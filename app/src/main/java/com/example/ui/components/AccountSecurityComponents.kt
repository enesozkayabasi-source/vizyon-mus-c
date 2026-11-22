package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.GppMaybe
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import java.util.Locale

@Composable
fun FirebaseAuthAccountCard(
  isSignedIn: Boolean,
  displayName: String,
  handle: String,
  firebaseUid: String?,
  firebaseEmail: String?,
  ageVerified: Boolean,
  isConnecting: Boolean,
  authError: String?,
  onSignInOrRegisterClick: () -> Unit,
  onSignOutClick: () -> Unit
) {
  GlassStudioCard(
    highlighted = isSignedIn,
    cornerRadius = 22.dp,
    modifier = Modifier.fillMaxWidth().testTag("profile_auth_card")
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Icon(
            imageVector = if (isSignedIn) Icons.Rounded.VerifiedUser else Icons.Rounded.AccountCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(26.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text =
              if (isSignedIn) "Bulut Kayıt Hesabı"
              else "Bulut Kayıt (Google İsteğe Bağlı)",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
          )
        }
        Surface(
          color =
            if (isSignedIn) MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
          shape = RoundedCornerShape(999.dp)
        ) {
          Text(
            text = if (isSignedIn) "Kayıtlı ✓" else "Cihaz Modu",
            style = MaterialTheme.typography.labelSmall,
            color =
              if (isSignedIn) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
          )
        }
      }

      if (isSignedIn) {
        Surface(
          color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
          shape = MaterialTheme.shapes.medium,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Text(
              text = "${displayName.ifBlank { "Vizyon Dinleyicisi" }} (${handle.ifBlank { "@vizyon" }})",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            if (!firebaseEmail.isNullOrBlank()) {
              Text(
                text = "Kayıtlı E-posta: $firebaseEmail",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            val maskedUid =
              if (!firebaseUid.isNullOrBlank() && firebaseUid.length > 10) {
                "${firebaseUid.take(6)}…${firebaseUid.takeLast(4)}"
              } else {
                firebaseUid.orEmpty()
              }
            Text(
              text = "Bulut UID: $maskedUid • ${if (ageVerified) "13+ Onaylı" else "Eşitleme Aktif"}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary
            )
          }
        }

        StudioGlassButton(
          text = "Güvenli Çıkış Yap",
          icon = Icons.AutoMirrored.Rounded.Logout,
          onClick = onSignOutClick,
          modifier = Modifier.fillMaxWidth().testTag("sign_out_btn")
        )
      } else {
        Text(
          text =
            "Tüm kişisel ayarlarını, profil fotoğrafını ve müziklerini Google olmadan da özgürce yönetebilirsin. Google hesabı yalnızca diğer kullanıcılarla sosyal eşitleme ve bulut kaydı için gereklidir.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        StudioGradientCtaButton(
          text = if (isConnecting) "Kayıt Bağlanıyor..." else "Google ile Kayıt Ol / Bağlan",
          icon = Icons.Rounded.Lock,
          isLoading = isConnecting,
          enabled = !isConnecting,
          onClick = onSignInOrRegisterClick,
          modifier = Modifier.fillMaxWidth().testTag("profile_google_auth_btn")
        )
        if (!authError.isNullOrBlank()) {
          Text(
            text = authError,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
          )
        }
      }
    }
  }
}

@Composable
fun AccountDeletionSecurityCard(
  isSignedIn: Boolean,
  ageVerified: Boolean,
  firebaseEmail: String?,
  isDeletingAccount: Boolean,
  onOpenDeleteWall: () -> Unit
) {
  GlassStudioCard(
    highlighted = false,
    cornerRadius = 22.dp,
    modifier =
      Modifier.fillMaxWidth()
        .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.42f), RoundedCornerShape(22.dp))
        .testTag("account_security_card")
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Security, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Hesap Güvenliği & Silme",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.error,
          fontWeight = FontWeight.Bold
        )
      }
      Text(
        text =
          buildString {
            if (ageVerified) append("• 13+ Güvenlik Duvarı: Onaylı\n")
            if (isSignedIn) {
              append("• Bulut Kimlik Duvarı: Aktif (${firebaseEmail ?: "Google"})\n")
              append("• Hesap silme öncesi onay kutusu, SİL güvenlik kelimesi ve güncel Google doğrulaması istenir.")
            } else {
              append("• Yerel profil, avatar, favoriler ve çalma listeleri silinmeden önce çok aşamalı onay uygulanır.")
            }
          },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      StudioGlassButton(
        text = if (isDeletingAccount) "Hesap ve Veriler Siliniyor..." else "Hesabımı Kalıcı Sil",
        icon = Icons.Rounded.DeleteForever,
        enabled = !isDeletingAccount,
        tintColor = MaterialTheme.colorScheme.error,
        borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.09f),
        onClick = onOpenDeleteWall,
        modifier = Modifier.fillMaxWidth().testTag("open_delete_account_dialog_btn")
      )
    }
  }
}

@Composable
fun DeleteAccountSecurityWallDialog(
  isSignedIn: Boolean,
  displayName: String,
  handle: String,
  firebaseEmail: String?,
  isDeletingAccount: Boolean,
  onDismiss: () -> Unit,
  onConfirmDelete: () -> Unit
) {
  var ackChecked by remember { mutableStateOf(false) }
  var confirmDeleteText by remember { mutableStateOf("") }

  val cleanConfirm = confirmDeleteText.trim().uppercase(Locale.forLanguageTag("tr-TR"))
  val isKeywordValid = cleanConfirm == "SİL" || cleanConfirm == "SIL"
  val canProceed = ackChecked && isKeywordValid && !isDeletingAccount

  BackHandler(enabled = !isDeletingAccount) { onDismiss() }

  Box(
    contentAlignment = Alignment.Center,
    modifier =
      Modifier.fillMaxSize()
        .zIndex(20f)
        .background(Color.Black.copy(alpha = 0.74f))
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null
        ) { if (!isDeletingAccount) onDismiss() }
        .padding(20.dp)
  ) {
    GlassStudioCard(
      highlighted = true,
      cornerRadius = 26.dp,
      modifier =
        Modifier.fillMaxWidth()
          .widthIn(max = 460.dp)
          .border(1.2.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.55f), RoundedCornerShape(26.dp))
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
          ) {}
          .testTag("delete_account_dialog")
    ) {
      Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Rounded.GppMaybe, contentDescription = null, tint = MaterialTheme.colorScheme.error)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Hesap Silme Güvenlik Duvarı",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold
          )
        }

        Surface(
          color = MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
          shape = MaterialTheme.shapes.small,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = "Silinecek Hesap: ${displayName.ifBlank { "Vizyon Dinleyicisi" }} (${handle.ifBlank { "@vizyon" }})",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold
            )
            if (isSignedIn && !firebaseEmail.isNullOrBlank()) {
              Text(
                text = "Bulut Hesabı: $firebaseEmail",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
              )
            }
            Text(
              text = "• Profil, avatar, favoriler, çalma listeleri, sosyal akış, bildirimler ve oturum kalıcı olarak silinir.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier =
            Modifier.fillMaxWidth()
              .clip(MaterialTheme.shapes.small)
              .clickable { ackChecked = !ackChecked }
              .padding(vertical = 8.dp, horizontal = 4.dp)
              .testTag("delete_account_ack_checkbox")
        ) {
          Checkbox(checked = ackChecked, onCheckedChange = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Bu işlemin geri alınamayacağını ve tüm verilerimin kalıcı olarak silineceğini onaylıyorum.",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
          )
        }

        Text(
          text = "Doğrulama için aşağıdaki kutuya SİL yazın:",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold
        )
        OutlinedTextField(
          value = confirmDeleteText,
          onValueChange = { if (it.length <= 10) confirmDeleteText = it },
          placeholder = { Text("SİL") },
          singleLine = true,
          isError = confirmDeleteText.isNotBlank() && !isKeywordValid,
          modifier = Modifier.fillMaxWidth().testTag("delete_account_confirm_input")
        )

        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
          TextButton(onClick = onDismiss, enabled = !isDeletingAccount) { Text("Vazgeç") }
          Spacer(modifier = Modifier.width(8.dp))
          StudioGradientCtaButton(
            text = if (isSignedIn) "Doğrula ve Sil" else "Kalıcı Sil",
            enabled = canProceed,
            isLoading = isDeletingAccount,
            gradientColors = listOf(MaterialTheme.colorScheme.error, Color(0xFF991B1B)),
            onClick = onConfirmDelete,
            modifier = Modifier.testTag("confirm_delete_account_btn")
          )
        }
      }
    }
  }
}

@Composable
fun ReauthenticateAccountDialog(
  firebaseEmail: String?,
  isReauthenticating: Boolean,
  reauthError: String?,
  onDismiss: () -> Unit,
  onConfirmReauth: () -> Unit
) {
  BackHandler(enabled = !isReauthenticating) { onDismiss() }

  Box(
    contentAlignment = Alignment.Center,
    modifier =
      Modifier.fillMaxSize()
        .zIndex(20f)
        .background(Color.Black.copy(alpha = 0.74f))
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null
        ) { if (!isReauthenticating) onDismiss() }
        .padding(20.dp)
  ) {
    GlassStudioCard(
      highlighted = true,
      cornerRadius = 26.dp,
      modifier =
        Modifier.fillMaxWidth()
          .widthIn(max = 440.dp)
          .border(1.2.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.55f), RoundedCornerShape(26.dp))
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
          ) {}
          .testTag("reauth_delete_dialog")
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Son Güvenlik Duvarı: Doğrulama",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
        }
        Text(
          text = "Güvenlik politikası gereği hesap silmeden önce Google hesabınızla güncel doğrulama yapmanız gerekir.",
          style = MaterialTheme.typography.bodySmall
        )
        if (!firebaseEmail.isNullOrBlank()) {
          Text(
            text = "Doğrulanacak hesap: $firebaseEmail",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
        }
        if (!reauthError.isNullOrBlank()) {
          Text(
            text = reauthError,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
          )
        }
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
          TextButton(onClick = onDismiss, enabled = !isReauthenticating) { Text("Vazgeç") }
          Spacer(modifier = Modifier.width(8.dp))
          StudioGradientCtaButton(
            text = if (isReauthenticating) "Doğrulanıyor..." else "Google ile Doğrula ve Sil",
            enabled = !isReauthenticating,
            isLoading = isReauthenticating,
            gradientColors = listOf(MaterialTheme.colorScheme.error, Color(0xFF991B1B)),
            onClick = onConfirmReauth,
            modifier = Modifier.testTag("confirm_reauth_delete_btn")
          )
        }
      }
    }
  }
}

@Composable
fun VaultUnlockDialog(
  userName: String,
  errorMessage: String?,
  onUnlockSubmit: (String) -> Unit
) {
  var passcodeInput by remember { mutableStateOf("") }

  Box(
    contentAlignment = Alignment.Center,
    modifier =
      Modifier.fillMaxSize()
        .zIndex(30f)
        .background(Color(0xEE040710))
        .padding(20.dp)
        .testTag("vault_unlock_dialog")
  ) {
    GlassStudioCard(
      highlighted = true,
      cornerRadius = 26.dp,
      modifier =
        Modifier.fillMaxWidth()
          .widthIn(max = 420.dp)
          .border(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f), RoundedCornerShape(26.dp))
    ) {
      Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(22.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            Icons.Rounded.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(26.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = "Stüdyo Güvenlik Kilidi",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Tekrar hoş geldin, ${userName.ifBlank { "Dinleyici" }}",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary
            )
          }
        }
        Text(
          text = "Profilini, özel mesajlarını ve yedekli verilerini açmak için stüdyo şifreni gir.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
          value = passcodeInput,
          onValueChange = { if (it.length <= 24) passcodeInput = it },
          label = { Text("Stüdyo Şifresi") },
          singleLine = true,
          visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().testTag("vault_unlock_input")
        )
        if (!errorMessage.isNullOrBlank()) {
          Text(
            text = errorMessage,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.SemiBold
          )
        }
        StudioGradientCtaButton(
          text = "Kilidi Aç",
          icon = Icons.Rounded.VerifiedUser,
          enabled = passcodeInput.trim().length >= 4,
          onClick = { onUnlockSubmit(passcodeInput) },
          modifier = Modifier.fillMaxWidth().testTag("vault_unlock_btn")
        )
      }
    }
  }
}

