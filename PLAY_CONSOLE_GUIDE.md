# Vizyon Music — Google Play Console & 7/24 Ücretsiz Bildirim Kurulum Rehberi

Bu dosya, **Google Play Console** başvuru formlarında ve **ücretsiz (0 TL)** kalıcı FCM push sunucusu kurulumunda sana kalan adımları en aza indirmek için hazırlandı. Aşağıdaki metinleri doğrudan kopyalayıp yapıştırabilirsin.

---

## 1. Uygulama Kimliği ve Sürüm Bilgileri (Otomatik Güncellendi)
- **Uygulama Adı (`app_name`)**: `Vizyon Music`
- **Paket Adı (`applicationId`)**: `com.vizyon.music`
- **Sürüm Kodu (`versionCode`)**: `2`
- **Sürüm Adı (`versionName`)**: `2.6.0`
- **Min / Target SDK**: `24` / `36`
- **Donanım Özelliği (`RECORD_AUDIO`)**: `required="false"` olarak işaretlendi; mikrofonsuz cihazlar da Play Store'da uygulamayı indirebilir.
- **Auto Backup (`backup_rules.xml` & `data_extraction_rules.xml`)**: Cihaza özel bildirim geçmişi ve FCM token senkron bayrakları yedekten hariç tutuldu.

---

## 2. 7/24 Ücretsiz Push Bildirim Sunucusu + Gizlilik Sayfaları (Tek Seferlik ~3 Dakika)

`fcm-worker/` klasörü artık tek bir Docker servisi içinde hem **uygulama kapalıyken anlık FCM HTTP v1 push bildirimlerini**, hem de Play Console'un zorunlu tuttuğu **HTTPS Gizlilik Politikası (`/privacy`) ve Hesap Silme (`/account-deletion`)** sayfalarını sunar.

### A) Render (Önerilen Ücretsiz Docker Web Service) veya Hugging Face Spaces Kurulumu
1. Bu depoyu GitHub hesabına gönder (veya `fcm-worker/` klasörünü Render'a bağla).
   - **ÖNEMLİ**: `serviceAccountKey.json` dosyasını ASLA Git'e ekleme! Render üzerinde **Environment Variables** bölümünden vereceksin.
2. [Render](https://render.com) -> **New +** -> **Web Service** (veya **Blueprint** ile `fcm-worker/render.yaml`):
   - **Root Directory**: `fcm-worker`
   - **Runtime**: `Docker`
   - **Instance Type**: `Free`
   - **Health Check Path**: `/health`
3. Render **Environment Variables** (Ortam Değişkenleri) kısmına aşağıdakileri ekle:
   - `FIREBASE_SERVICE_ACCOUNT_JSON`: Firebase Console -> Project Settings -> Service Accounts -> *Generate new private key* ile indirdiğin JSON içeriğinin tamamını yapıştır.
   - `PRIVACY_CONTACT_EMAIL`: `enesozkayabasi@gmail.com`
   - `FIRESTORE_DATABASE_ID`: `ai-studio-android-vizyonmu-95f9d1d0-40a5-423e-9c63-5985bfe0fd3c`
4. Servis ayağa kalkınca sana `https://vizyon-music-fcm-worker.onrender.com` benzeri bir HTTPS URL verecek.
   - **Render Free Uyku Önleme (İsteğe Bağlı)**: [UptimeRobot](https://uptimerobot.com) veya [cron-job.org](https://cron-job.org) üzerinden her 10 dakikada bir `https://<SENIN-URL>/health` adresine ücretsiz HTTP GET ping atarak servisi 7/24 uyanık tutabilirsin (0 TL).

### B) Firestore İndekslerini Tek Seferlik Oluşturma
Dual bildirim sorgusu (`sent == false` ve eski APK uyumluluğu için `read == false`) collectionGroup indeksleri gerektirir. İlk çalışmada terminalde (veya Render loglarında) `FAILED_PRECONDITION: The query requires an index` linki çıkarsa o linke 1 kez tıklayıp **Create Index** de; ya da yerel bilgisayarında tek komutla yükle:
```bash
firebase deploy --only firestore:indexes --project gen-lang-client-0103909720
```

---

## 3. Google Play Console Mağaza Girişi (Kopyala & Yapıştır)

### Kısa Açıklama (En fazla 80 karakter — Türkçe)
```text
Keşfet, paylaş, dinle: Senkron şarkı sözleri, müzik hikayeleri ve anlık sohbet.
```

### Tam Açıklama (Türkçe)
```text
Vizyon Music; Türkçe ve global müzik keşfini, senkronize şarkı sözlerini, 24 saatlik müzik hikayelerini ve dinleyiciler arası gerçek zamanlı sohbeti tek bir modern stüdyoda birleştirir.

Öne Çıkan Özellikler:
• Keşfet ve Ara: iTunes müzik kataloğu ve resmi YouTube IFrame oynatıcı entegrasyonu ile sevdiğin parçaları ve sanatçıları keşfet.
• Gerçek Zamanlı Şarkı Sözleri: Dinlediğin parçayla eşzamanlı akan şarkı sözlerini takip et.
• Müzik Hikayeleri: Favori parçanı özel kart temalarıyla 24 saatlik görsel hikayeler olarak paylaş.
• Birebir Sohbet ve Sesli Mesaj: Takipçilerinle parça öner, metin veya sesli mesaj gönder.
• Akıllı Bildirim Kanalları: Takip istekleri, beğeniler, hikayeler ve mesajlar için özelleştirilmiş anlık bildirimler.
• Google Bulut Yedeği & Kişiselleştirme: AMOLED koyu mod, özel vurgu renkleri ve bulutta korunan profil tercihleri.
```

### Short Description (English, max 80 chars)
```text
Discover tracks, synced lyrics, 24h music stories, and real-time listener chat.
```

---

## 4. Play Console Politika & Beyan Cevapları

### A) Gizlilik Politikası & Hesap Silme URL'leri
Render servisin yayınlandıktan sonra (`https://<SENIN-URL>` yerine kendi adresini yaz):
- **Privacy Policy URL (Gizlilik Politikası)**: `https://<SENIN-URL>/privacy`
- **Account Deletion URL (Hesap Silme Sayfası)**: `https://<SENIN-URL>/account-deletion`
- **Geliştirici İletişim E-postası**: `enesozkayabasi@gmail.com`

### B) Veri Güvenliği (Data Safety Form)
Play Console -> App content -> Data safety bölümünde seçilecek cevaplar:
- **Does your app collect or share any of the required user data types?** -> **Yes**
- **Is all of the user data collected by your app encrypted in transit?** -> **Yes** (HTTPS / TLS)
- **Do you provide a way for users to request that their data is deleted?** -> **Yes** (Uygulama içi Ayarlar -> Hesabı Kalıcı Sil + yukarıdaki Web Account Deletion URL)
- **Toplanan Veri Türleri**:
  1. **Personal info -> Name, Email address, User IDs**:
     - Amaç: *App functionality, Account management* (Google ile Giriş, profil ve bulut yedeği)
     - Kullanıcı seçimine bağlı mı?: Hesap oluşturma sırasında toplanır (Google Sign-In).
     - Başka taraflarla paylaşılıyor mu?: **No** (Üçüncü taraflara satılmaz/paylaşılmaz).
  2. **Photos and videos -> Photos**:
     - Amaç: *App functionality* (Kullanıcının Fotoğraf Seçici ile seçtiği profil fotoğrafı).
  3. **Audio -> Voice or sound recordings**:
     - Amaç: *App functionality* (Yalnızca kullanıcı doğrudan sesli mesaj kaydetmeye basarsa kullanılır).
  4. **Messages -> Other in-app messages**:
     - Amaç: *App functionality* (Dinleyiciler arası doğrudan sohbet mesajları ve hikaye notları).
  5. **Device or other IDs**:
     - Amaç: *App functionality* (FCM anlık bildirim token'ı, yalnızca bildirim teslimi için).

### C) İzin Beyanları (App Content -> Sensitive Permissions)
- **Fotoğraf / Medya Erişimi (`READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE`)**:
  - Uygulama **Android Photo Picker (`PickVisualMedia`)** kullanır; geniş depolama izni talep ETMEZ. Play Console'da galeri izni formu açılmaz.
- **Mikrofon (`RECORD_AUDIO`)**:
  - Yalnızca kullanıcı sohbet ekranında "Sesli Mesaj Kaydet" butonuna bastığında runtime izin istenir. Arka planda dinleme yapılmaz.
- **Bildirim (`POST_NOTIFICATIONS`)**:
  - Android 13+ cihazlarda yalnızca kullanıcı bildirim almak istediğinde runtime olarak istenir.

### D) Uygulama Erişimi (App Access) & Yaş Sınırı (Content Rating)
- **App Access**: Google Sign-In giriş ekranı bulunduğundan **"All or some functionality is restricted"** seçilip inceleme ekibi için bir test Google hesabı bilgisi verilebilir (veya misafir keşif akışı varsa not olarak belirtilebilir).
- **Content Rating (IARC)**: Sosyal sohbet / kullanıcı içeriği (UGC) içerdiği için anket sırasında **"Users interact / share content"** -> **Yes** (Raporlama ve Engelleme mekanizması uygulama içinde mevcuttur) olarak işaretle; uygulama içi 13+ yaş doğrulaması vardır (`12+` / `Teen`).
- **Reklamlar (Ads)**: Uygulama içinde reklam SDK'sı yoktur -> **"No, my app does not contain ads"**.

---

## 5. Yayın Öncesi AAB Alma Komutu
Bilgisayarında imzalı Play Store paketi (`.aab`) üretmek için:
```bash
gradle :app:bundleRelease
```
Çıktı dosyası: `app/build/outputs/bundle/release/app-release.aab` -> Google Play Console **Production** veya **Internal Testing** kanalına yükle.
