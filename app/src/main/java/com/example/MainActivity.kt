package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.audio.StudioSoundEngine
import com.example.data.firebase.VizyonFirestoreRepository
import com.example.data.local.AppearancePreferences
import com.example.data.local.VizyonDatabase
import com.example.data.remote.HybridMusicRepository
import com.example.model.AppAppearanceConfig
import com.example.notifications.BackgroundNotificationJobService
import com.example.notifications.VizyonNotificationHelper
import com.example.player.YouTubeForegroundPlayerManager
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.components.VizyonLaunchSplash
import com.example.ui.navigation.VizyonMainShell
import com.example.ui.screens.OnboardingScreen
import com.example.ui.theme.VizyonMusicTheme
import com.example.ui.viewmodel.VizyonMainViewModel
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { firebaseAuth -> trySend(firebaseAuth.currentUser) }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
  private var pendingLaunchIntent by mutableStateOf<Intent?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    pendingLaunchIntent = intent
    if (FirebaseApp.getApps(applicationContext).isEmpty()) {
      FirebaseApp.initializeApp(applicationContext)
    }
    VizyonNotificationHelper.ensureChannel(applicationContext)
    BackgroundNotificationJobService.schedule(applicationContext)
    StudioSoundEngine.warmup(applicationContext)
    enableEdgeToEdge()
    setContent {
      VizyonMusicAppRoot(
        launchIntent = pendingLaunchIntent,
        onLaunchIntentConsumed = { pendingLaunchIntent = null }
      )
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    pendingLaunchIntent = intent
  }
}

private enum class AppRootRoute {
  SPLASH,
  ONBOARDING,
  MAIN
}

@Composable
fun VizyonMusicAppRoot(
  launchIntent: Intent? = null,
  onLaunchIntentConsumed: () -> Unit = {}
) {
  val context = LocalContext.current
  remember(context) {
    if (FirebaseApp.getApps(context.applicationContext).isEmpty()) {
      FirebaseApp.initializeApp(context.applicationContext)
    }
    true
  }
  val appearancePrefs = remember(context) { AppearancePreferences(context) }
  val appearanceOrNull by appearancePrefs.configFlow.collectAsStateWithLifecycle(initialValue = null)
  val activeAppearance = appearanceOrNull ?: AppAppearanceConfig()

  val auth = remember { Firebase.auth }
  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  val credentialManager = remember(context) { runCatching { CredentialManager.create(context) }.getOrNull() }
  val startupScope = rememberCoroutineScope()

  LaunchedEffect(Unit) {
    if (credentialManager != null && auth.currentUser == null) {
      attemptAutoSignIn(
        context = context,
        credentialManager = credentialManager,
        onAuthSuccess = {},
        onUnauthenticated = {},
        scope = startupScope
      )
    }
  }

  VizyonMusicTheme(appearance = activeAppearance) {
    val uid = currentUser?.uid
    val sessionKey = uid ?: "guest_session"
    key(sessionKey) {
      val mainViewModel: VizyonMainViewModel =
        viewModel(
          key = sessionKey,
          factory =
            viewModelFactory {
              initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                if (FirebaseApp.getApps(app).isEmpty()) {
                  FirebaseApp.initializeApp(app)
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val firestoreRepo = VizyonFirestoreRepository(db, Firebase.auth)
                val hybridRepo = HybridMusicRepository()
                val roomDb = VizyonDatabase.getInstance(app)
                val playerManager = YouTubeForegroundPlayerManager()
                VizyonMainViewModel(
                  currentUserId = uid,
                  firestoreRepo = firestoreRepo,
                  localLibraryDao = roomDb.localLibraryDao(),
                  hybridMusicRepo = hybridRepo,
                  playerManager = playerManager,
                  appearancePrefs = appearancePrefs,
                  appContext = app.applicationContext
                )
              }
            }
        )

      // Foreground-only policy enforcement: pause YouTube IFrame when app leaves foreground
      val lifecycleOwner = LocalLifecycleOwner.current
      DisposableEffect(lifecycleOwner, mainViewModel) {
        val observer = LifecycleEventObserver { _, event ->
          if (event == Lifecycle.Event.ON_RESUME) {
            mainViewModel.onHostResumed()
          } else if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
            mainViewModel.pauseForBackground()
          }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
          lifecycleOwner.lifecycle.removeObserver(observer)
        }
      }

      LaunchedEffect(launchIntent, mainViewModel) {
        if (launchIntent != null) {
          mainViewModel.handleNotificationIntent(launchIntent)
          onLaunchIntentConsumed()
        }
      }

      val rootRoute =
        when (val loaded = appearanceOrNull) {
          null -> AppRootRoute.SPLASH
          else -> if (!loaded.onboardingCompleted) AppRootRoute.ONBOARDING else AppRootRoute.MAIN
        }

      Crossfade(
        targetState = rootRoute,
        animationSpec = tween(durationMillis = 220),
        label = "app_root_route"
      ) { route ->
        when (route) {
          AppRootRoute.SPLASH -> {
            VizyonLaunchSplash()
          }
          AppRootRoute.ONBOARDING -> {
            OnboardingScreen(
              initialDisplayName = currentUser?.displayName.orEmpty(),
              initialAvatarPath = activeAppearance.cloudAvatarDataUrl.ifBlank { activeAppearance.localAvatarPath },
              isSignedInWithGoogle = !uid.isNullOrBlank(),
              initialThemeMode = activeAppearance.themeMode,
              initialAccentPalette = activeAppearance.accentPalette,
              initialCornerDensity = activeAppearance.cornerDensity,
              initialLanguage = activeAppearance.preferredLanguage,
              initialStoryTheme = activeAppearance.defaultStoryTheme,
              initialChatPrivacy = activeAppearance.chatPrivacy,
              onPickPhoto = { uri -> mainViewModel.updateProfilePhotoFromUri(context, uri) },
              onThemePreviewChange = { mainViewModel.updateThemeMode(it) },
              onAccentPreviewChange = { mainViewModel.updateAccentPalette(it) },
              onCornerPreviewChange = { mainViewModel.updateCornerDensity(it) },
              onCompleteOnboarding = {
                name,
                handle,
                bio,
                genres,
                ageOk,
                lang,
                theme,
                shareAct,
                accent,
                corner,
                storyTheme,
                chatPrivacy,
                passcode,
                rememberMe,
                lockOnLaunch ->
                mainViewModel.completeOnboarding(
                  displayName = name,
                  handle = handle,
                  bio = bio,
                  favoriteGenres = genres,
                  ageVerified = ageOk,
                  preferredLanguage = lang,
                  themeMode = theme,
                  shareActivity = shareAct,
                  accentPalette = accent,
                  cornerDensity = corner,
                  defaultStoryTheme = storyTheme,
                  chatPrivacy = chatPrivacy,
                  passcode = passcode,
                  rememberMe = rememberMe,
                  lockOnLaunch = lockOnLaunch
                )
              }
            )
          }
          AppRootRoute.MAIN -> {
            VizyonMainShell(
              currentUserId = uid,
              firebaseUserEmail = currentUser?.email,
              viewModel = mainViewModel,
              onSignedOut = {}
            )
          }
        }
      }
    }
  }
}
