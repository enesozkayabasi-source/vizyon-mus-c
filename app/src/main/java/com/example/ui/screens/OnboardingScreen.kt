package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.model.AccentPalette
import com.example.model.AppLanguage
import com.example.model.AppThemeMode
import com.example.model.ChatPrivacy
import com.example.model.CornerDensity
import com.example.model.MusicCategory
import com.example.model.StoryCardTheme
import com.example.notifications.VizyonNotificationHelper
import com.example.ui.auth.onGoogleSignInClicked
import com.example.ui.components.AnimatedWaveformBackdrop
import com.example.ui.components.OnboardingStepMeta
import com.example.ui.components.SchemaStepperHeader
import java.util.Locale

@Composable
fun OnboardingScreen(
  initialDisplayName: String,
  initialAvatarPath: String = "",
  isSignedInWithGoogle: Boolean,
  initialThemeMode: AppThemeMode,
  initialAccentPalette: AccentPalette = AccentPalette.VIZYON_BLUE,
  initialCornerDensity: CornerDensity = CornerDensity.BALANCED,
  initialLanguage: AppLanguage,
  initialStoryTheme: StoryCardTheme = StoryCardTheme.NEON_WAVE,
  initialChatPrivacy: ChatPrivacy = ChatPrivacy.PUBLIC,
  onPickPhoto: (Uri) -> Unit = {},
  onThemePreviewChange: (AppThemeMode) -> Unit,
  onAccentPreviewChange: (AccentPalette) -> Unit = {},
  onCornerPreviewChange: (CornerDensity) -> Unit = {},
  onCompleteOnboarding: (
    displayName: String,
    handle: String,
    bio: String,
    favoriteGenres: List<String>,
    ageVerified: Boolean,
    preferredLanguage: AppLanguage,
    themeMode: AppThemeMode,
    shareActivity: Boolean,
    accentPalette: AccentPalette,
    cornerDensity: CornerDensity,
    defaultStoryTheme: StoryCardTheme,
    chatPrivacy: ChatPrivacy,
    passcode: String,
    rememberMe: Boolean,
    lockOnLaunch: Boolean
  ) -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { runCatching { CredentialManager.create(context) }.getOrNull() }

  var currentStep by remember { mutableIntStateOf(0) }
  val stepScrollState = rememberScrollState()
  var displayName by remember(initialDisplayName) { mutableStateOf(initialDisplayName) }
  var handle by remember(initialDisplayName) {
    val autoHandle =
      initialDisplayName
        .lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9_]"), "")
        .take(18)
    mutableStateOf(autoHandle)
  }
  var bio by remember { mutableStateOf("") }
  var previewAvatarPath by remember(initialAvatarPath) { mutableStateOf(initialAvatarPath) }
  var passcode by remember { mutableStateOf("") }
  var passcodeConfirm by remember { mutableStateOf("") }
  var rememberMe by remember { mutableStateOf(true) }
  var lockOnLaunch by remember { mutableStateOf(false) }
  var selectedStoryTheme by remember(initialStoryTheme) { mutableStateOf(initialStoryTheme) }
  var selectedChatPrivacy by remember(initialChatPrivacy) { mutableStateOf(initialChatPrivacy) }
  var selectedLanguage by remember(initialLanguage) { mutableStateOf(initialLanguage) }
  var ageVerified by remember { mutableStateOf(false) }
  val selectedGenres = remember {
    mutableStateListOf(
      MusicCategory.TURKISH_POP.label,
      MusicCategory.ANATOLIAN_ROCK.label,
      MusicCategory.POP.label
    )
  }
  var selectedTheme by remember(initialThemeMode) { mutableStateOf(initialThemeMode) }
  var selectedAccent by remember(initialAccentPalette) { mutableStateOf(initialAccentPalette) }
  var selectedCorner by remember(initialCornerDensity) { mutableStateOf(initialCornerDensity) }
  var shareActivity by remember { mutableStateOf(true) }
  var isNetworkAvailable by remember { mutableStateOf(checkActiveNetwork(context)) }
  var hasNotifPermission by remember {
    mutableStateOf(VizyonNotificationHelper.hasNotificationPermission(context))
  }
  var validationError by remember { mutableStateOf<String?>(null) }
  var googleConnecting by remember { mutableStateOf(false) }
  var googleAuthError by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(currentStep) {
    stepScrollState.scrollTo(0)
  }

  val photoPickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        previewAvatarPath = uri.toString()
        onPickPhoto(uri)
      }
    }

  val notifPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      hasNotifPermission = granted
    }

  BackHandler(enabled = currentStep > 0) {
    validationError = null
    currentStep -= 1
  }

  val steps = remember {
    listOf(
      OnboardingStepMeta("Kimlik", "Profil & Bulut Kurtarma"),
      OnboardingStepMeta("Tema & Stil", "Görünüm, Hikaye & DM"),
      OnboardingStepMeta("Tarzlar", "Dil & Müzik Türleri"),
      OnboardingStepMeta("Güvenlik", "Kasa, İzinler & Özet")
    )
  }
  val selectableGenres = remember {
    MusicCategory.entries.filter { it != MusicCategory.TRENDING }
  }

  val triggerGoogleAuth: () -> Unit = {
    val cm = credentialManager
    if (cm != null) {
      googleConnecting = true
      googleAuthError = null
      onGoogleSignInClicked(
        context = context,
        credentialManager = cm,
        onAuthSuccess = { googleConnecting = false },
        onAuthError = { err ->
          googleConnecting = false
          googleAuthError = err
        },
        scope = scope,
        onAuthCancelled = { googleConnecting = false }
      )
    }
  }

  Surface(
    color = MaterialTheme.colorScheme.background,
    contentColor = MaterialTheme.colorScheme.onBackground,
    modifier = Modifier.fillMaxSize().testTag("onboarding_screen")
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      AnimatedWaveformBackdrop(
        currentStep = currentStep,
        selectedGenreCount = selectedGenres.size
      )

      Column(
        modifier =
          Modifier.fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          if (currentStep > 0) {
            IconButton(
              onClick = {
                validationError = null
                currentStep -= 1
              }
            ) {
              Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Önceki adım")
            }
          } else {
            Spacer(modifier = Modifier.size(48.dp))
          }

          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "VİZYON STUDIO KURULUM",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "${steps[currentStep].headerTitle} • ${currentStep + 1}/${steps.size}",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold
            )
          }

          Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
            shape = RoundedCornerShape(999.dp),
            modifier =
              Modifier.border(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.30f),
                RoundedCornerShape(999.dp)
              )
          ) {
            Text(
              text = selectedLanguage.badge,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        SchemaStepperHeader(
          steps = steps,
          currentStep = currentStep
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.weight(1f).verticalScroll(stepScrollState)) {
          AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
              val forward = targetState >= initialState
              (slideInHorizontally(tween(260)) { if (forward) it / 4 else -it / 4 } +
                fadeIn(tween(240))) togetherWith
                (slideOutHorizontally(tween(200)) { if (forward) -it / 5 else it / 5 } +
                  fadeOut(tween(180)))
            },
            label = "onboarding_step"
          ) { step ->
            when (step) {
              0 -> {
                OnboardingStepOneIdentity(
                  displayName = displayName,
                  handle = handle,
                  bio = bio,
                  avatarPath = previewAvatarPath,
                  selectedCorner = selectedCorner,
                  isSignedInWithGoogle = isSignedInWithGoogle,
                  googleConnecting = googleConnecting,
                  googleAuthError = googleAuthError,
                  onPickPhotoClick = {
                    photoPickerLauncher.launch(
                      PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                  },
                  onDisplayNameChange = {
                    displayName = it
                    validationError = null
                  },
                  onHandleChange = {
                    handle = it
                    validationError = null
                  },
                  onBioChange = { bio = it },
                  onRestoreFromGoogleClick = triggerGoogleAuth
                )
              }

              1 -> {
                OnboardingStepTwoThemeAndStyle(
                  displayName = displayName,
                  handle = handle,
                  bio = bio,
                  avatarPath = previewAvatarPath,
                  selectedLanguage = selectedLanguage,
                  selectedTheme = selectedTheme,
                  selectedAccent = selectedAccent,
                  selectedCorner = selectedCorner,
                  selectedStoryTheme = selectedStoryTheme,
                  selectedChatPrivacy = selectedChatPrivacy,
                  selectedGenres = selectedGenres,
                  isSignedInWithGoogle = isSignedInWithGoogle,
                  ageVerified = ageVerified,
                  shareActivity = shareActivity,
                  passcodeConfigured = passcode.trim().length >= 4 && passcode == passcodeConfirm,
                  onSelectTheme = { mode ->
                    selectedTheme = mode
                    onThemePreviewChange(mode)
                  },
                  onSelectAccent = { palette ->
                    selectedAccent = palette
                    onAccentPreviewChange(palette)
                  },
                  onSelectCorner = { corner ->
                    selectedCorner = corner
                    onCornerPreviewChange(corner)
                  },
                  onSelectStoryTheme = { selectedStoryTheme = it },
                  onSelectChatPrivacy = { selectedChatPrivacy = it },
                  onShareActivityChange = { shareActivity = it }
                )
              }

              2 -> {
                OnboardingStepThreeLanguageAndGenres(
                  selectedLanguage = selectedLanguage,
                  selectableGenres = selectableGenres,
                  selectedGenres = selectedGenres,
                  onSelectLanguage = { selectedLanguage = it },
                  onToggleGenre = { genreLabel ->
                    if (genreLabel in selectedGenres) {
                      if (selectedGenres.size > 1) selectedGenres.remove(genreLabel)
                    } else if (selectedGenres.size < 8) {
                      selectedGenres.add(genreLabel)
                    }
                  }
                )
              }

              else -> {
                OnboardingStepFourSecurityAndSummary(
                  displayName = displayName,
                  handle = handle,
                  selectedTheme = selectedTheme,
                  selectedAccent = selectedAccent,
                  selectedStoryTheme = selectedStoryTheme,
                  selectedChatPrivacy = selectedChatPrivacy,
                  selectedLanguage = selectedLanguage,
                  selectedGenres = selectedGenres,
                  passcode = passcode,
                  passcodeConfirm = passcodeConfirm,
                  rememberMe = rememberMe,
                  lockOnLaunch = lockOnLaunch,
                  isNetworkAvailable = isNetworkAvailable,
                  ageVerified = ageVerified,
                  hasNotifPermission = hasNotifPermission,
                  isSignedInWithGoogle = isSignedInWithGoogle,
                  googleConnecting = googleConnecting,
                  googleAuthError = googleAuthError,
                  onPasscodeChange = {
                    passcode = it
                    validationError = null
                  },
                  onPasscodeConfirmChange = {
                    passcodeConfirm = it
                    validationError = null
                  },
                  onRememberMeChange = { rememberMe = it },
                  onLockOnLaunchChange = { lockOnLaunch = it },
                  onVerifyNetwork = { isNetworkAvailable = checkActiveNetwork(context) },
                  onAgeVerifiedChange = {
                    ageVerified = it
                    validationError = null
                  },
                  onRequestNotifPermission = {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                      notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                      hasNotifPermission = true
                    }
                  },
                  onGoogleAuthClick = triggerGoogleAuth
                )
              }
            }
          }
        }

        validationError?.let { err ->
          Spacer(modifier = Modifier.height(8.dp))
          Surface(
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
            shape = RoundedCornerShape(14.dp),
            modifier =
              Modifier.fillMaxWidth()
                .border(
                  1.dp,
                  MaterialTheme.colorScheme.error.copy(alpha = 0.40f),
                  RoundedCornerShape(14.dp)
                )
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
              Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = err,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
          color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
          shape = RoundedCornerShape(999.dp),
          modifier =
            Modifier.fillMaxWidth()
              .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
        ) {
          Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)
          ) {
            if (currentStep > 0) {
              TextButton(
                onClick = {
                  validationError = null
                  currentStep -= 1
                },
                modifier = Modifier.heightIn(min = 48.dp)
              ) {
                Text("Geri", fontWeight = FontWeight.SemiBold)
              }
            } else {
              Spacer(modifier = Modifier.width(8.dp))
            }

            if (currentStep < steps.lastIndex) {
              Button(
                onClick = {
                  if (currentStep == 0) {
                    val cleanName = displayName.trim()
                    val cleanHandle = handle.trim()
                    if (cleanName.length < 2) {
                      validationError = "Lütfen en az 2 karakterli bir görünen ad girin."
                      return@Button
                    }
                    if (cleanHandle.length < 2) {
                      validationError = "Lütfen en az 2 karakterli bir kullanıcı adı (@) girin."
                      return@Button
                    }
                  } else if (currentStep == 2 && selectedGenres.isEmpty()) {
                    validationError = "Lütfen en az 1 müzik tarzı seçin."
                    return@Button
                  }
                  validationError = null
                  currentStep += 1
                },
                shape = RoundedCornerShape(999.dp),
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                  ),
                modifier = Modifier.heightIn(min = 48.dp).testTag("onboarding_next_btn")
              ) {
                Text(
                  text = "Devam Et",
                  fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                  Icons.AutoMirrored.Rounded.ArrowForward,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp)
                )
              }
            } else {
              Button(
                onClick = {
                  val cleanPass = passcode.trim()
                  if (cleanPass.length < 4) {
                    validationError = "Lütfen en az 4 karakterli bir stüdyo güvenlik şifresi oluşturun."
                    return@Button
                  }
                  if (passcode != passcodeConfirm) {
                    validationError = "Girilen şifreler birbiriyle eşleşmiyor."
                    return@Button
                  }
                  if (!ageVerified) {
                    validationError = "Devam etmek için 13+ yaş güvenlik doğrulamasını onaylamalısınız."
                    return@Button
                  }
                  onCompleteOnboarding(
                    displayName.trim().ifBlank { "Vizyon Dinleyicisi" },
                    "@${handle.trim().ifBlank { "vizyon" }}",
                    bio.trim(),
                    selectedGenres.toList(),
                    ageVerified,
                    selectedLanguage,
                    selectedTheme,
                    shareActivity,
                    selectedAccent,
                    selectedCorner,
                    selectedStoryTheme,
                    selectedChatPrivacy,
                    passcode,
                    rememberMe,
                    lockOnLaunch
                  )
                },
                shape = RoundedCornerShape(999.dp),
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                  ),
                modifier = Modifier.heightIn(min = 48.dp).testTag("onboarding_complete_btn")
              ) {
                Icon(
                  Icons.Rounded.AutoAwesome,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Kurulumu Tamamla & Keşfet", fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }
  }
}
