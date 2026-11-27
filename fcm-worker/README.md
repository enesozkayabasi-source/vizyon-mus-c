# Vizyon Müzik — 0 TL Bulut FCM İşçisi & Play Console Yasal Sayfalar

Bu Docker tabanlı Node.js servisi, Firebase **Spark (ücretsiz)** planında `users/{recipientId}/notifications/{notificationId}` koleksiyon grubunu gerçek zamanlı dinleyerek uygulama tamamen kapalıyken dahi yüksek öncelikli **FCM HTTP v1 (`notification + data`)** bildirimlerini gönderir.

Ayrıca Google Play Console için zorunlu olan herkese açık HTTPS sayfalarını aynı servis içinde barındırır:
- `/health` → Canlı dinleyici durumu, gönderim/hata sayaçları ve varsa eksik indeks bağlantısı
- `/privacy` (ve `/privacy-policy`) → Google Play **Privacy Policy URL**
- `/account-deletion` → Google Play **Account Deletion URL**
- `/terms` → Kullanım Koşulları

## 3 Adımda 0 TL Kalıcı Bulut Kurulumu (Bilgisayar Açık Kalmadan)

1. **Firebase Service Account JSON Al:**
   - Firebase Console → Project Settings (`gen-lang-client-0103909720`) → **Service accounts** → **Generate new private key**.
   - İndirdiğin JSON içeriğinin tamamını kopyala (veya tek satır yap).

2. **Ücretsiz Docker Web Servisini Aç (Render veya Hugging Face Spaces):**
   - **Render Free Tier**: `render.yaml` veya **New Web Service (Docker, Root Directory: `fcm-worker`)** seç → Environment Variables içine `FIREBASE_SERVICE_ACCOUNT_JSON` (Service Account JSON metni) ekle → Deploy.
   - **Hugging Face Spaces (Kredi Kartsız 0 TL)**: Yeni Docker Space oluştur → `fcm-worker/` içindeki `Dockerfile`, `package.json`, `index.js` dosyalarını yükle → Space **Settings → Variables & Secrets** altına `FIREBASE_SERVICE_ACCOUNT_JSON` Secret'ını ve `PORT=7860` ekle.

3. **Tek Seferlik İndeks ve Doğrulama:**
   - Servisin adresinde `/health` sayfasını aç. `listeners.sentQuery: "listening"` görünüyorsa hazırdır!
   - Eğer `indexUrls` dizisinde bir `console.firebase.google.com/...` bağlantısı görünürse o linke bir kez tıklayıp **Create Index** de (veya yerelde `firebase deploy --only firestore:indexes` çalıştır).
   - Oluşan `https://<senin-servis-adresin>/privacy` ve `https://<senin-servis-adresin>/account-deletion` linklerini doğrudan Google Play Console'a yapıştır.

## Yerel Test

```bash
cd fcm-worker
npm install
npm test
npm run check
```
