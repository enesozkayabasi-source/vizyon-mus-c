"use strict";

const fs = require("node:fs");
const http = require("node:http");
const path = require("node:path");
const { initializeApp, cert, applicationDefault } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

const FIREBASE_PROJECT_ID =
  (process.env.FIREBASE_PROJECT_ID || "").trim() || "gen-lang-client-0103909720";
const FIRESTORE_DATABASE_ID =
  (process.env.FIRESTORE_DATABASE_ID || "").trim() ||
  "ai-studio-android-vizyonmu-95f9d1d0-40a5-423e-9c63-5985bfe0fd3c";
const SUPPORT_EMAIL =
  (process.env.SUPPORT_EMAIL || "").trim() || "enesozkayabasi@gmail.com";

const STALE_TOKEN_ERROR_CODES = new Set([
  "messaging/registration-token-not-registered",
  "messaging/invalid-registration-token"
]);

const TRANSIENT_ERROR_CODES = new Set([
  "messaging/internal-error",
  "messaging/server-unavailable",
  "messaging/unavailable",
  "messaging/unknown-error"
]);

const MAX_SEND_RETRIES = 3;
const RECONNECT_DELAY_MS = 5000;
const inFlightPaths = new Set();

function loadDotEnvIfPresent() {
  const envPath = path.join(__dirname, ".env");
  if (!fs.existsSync(envPath)) return;
  const raw = fs.readFileSync(envPath, "utf8");
  for (const line of raw.split(/\r?\n/)) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("#")) continue;
    const eqIndex = trimmed.indexOf("=");
    if (eqIndex <= 0) continue;
    const key = trimmed.slice(0, eqIndex).trim();
    let value = trimmed.slice(eqIndex + 1).trim();
    if (
      (value.startsWith('"') && value.endsWith('"')) ||
      (value.startsWith("'") && value.endsWith("'"))
    ) {
      value = value.slice(1, -1);
    }
    if (process.env[key] === undefined) {
      process.env[key] = value;
    }
  }
}

function resolveCredential() {
  const inlineJson = (process.env.FIREBASE_SERVICE_ACCOUNT_JSON || "").trim();
  if (inlineJson) {
    try {
      const decoded = inlineJson.startsWith("{")
        ? inlineJson
        : Buffer.from(inlineJson, "base64").toString("utf8");
      const parsed = JSON.parse(decoded);
      return { credential: cert(parsed), source: "FIREBASE_SERVICE_ACCOUNT_JSON" };
    } catch (err) {
      throw new Error(`FIREBASE_SERVICE_ACCOUNT_JSON gecersiz JSON iceriyor: ${err.message}`);
    }
  }

  const candidatePaths = [
    (process.env.GOOGLE_APPLICATION_CREDENTIALS || "").trim(),
    path.join(__dirname, "serviceAccountKey.json")
  ].filter(Boolean);

  for (const rawCandidate of candidatePaths) {
    const resolved = path.isAbsolute(rawCandidate)
      ? rawCandidate
      : path.resolve(__dirname, rawCandidate);
    if (fs.existsSync(resolved)) {
      const rawFile = fs.readFileSync(resolved, "utf8");
      const parsed = JSON.parse(rawFile);
      return { credential: cert(parsed), source: resolved };
    }
  }

  if (process.env.K_SERVICE || process.env.GOOGLE_CLOUD_PROJECT) {
    return { credential: applicationDefault(), source: "applicationDefault" };
  }

  throw new Error(
    "Service Account JSON anahtari bulunamadi. ./fcm-worker/serviceAccountKey.json ekleyin " +
      "veya FIREBASE_SERVICE_ACCOUNT_JSON / GOOGLE_APPLICATION_CREDENTIALS ortam degiskenini ayarlayin."
  );
}

function asCleanString(value, maxLen = 240) {
  if (typeof value !== "string") return "";
  return value.trim().slice(0, maxLen);
}

function asHttpAvatarUrl(value) {
  const clean = asCleanString(value, 512);
  if (clean.startsWith("https://") || clean.startsWith("http://")) {
    return clean;
  }
  return "";
}

function cleanDisplayNameOnly(senderName) {
  const cleaned = asCleanString(senderName, 80)
    .replace(/\s*\(@[^)]*\)/g, "")
    .replace(/@[A-Za-z0-9_.-]+/g, "")
    .trim();
  return cleaned || "Vizyon Dinleyicisi";
}

function defaultBodyForType(type, trackTitle) {
  switch (type) {
    case "FOLLOW":
      return "Seni takip etmeye başladı";
    case "FOLLOW_REQUEST":
      return "Sana takip isteği gönderdi";
    case "LIKE":
      return "Paylaştığın parçayı beğendi";
    case "COMMENT":
      return "Paylaşımına yorum yaptı";
    case "REPOST":
      return "Paylaşımını yeniden paylaştı";
    case "STORY":
      return "Yeni bir müzik hikayesi paylaştı";
    case "MESSAGE_REQUEST":
      return "Sana bir mesaj isteği gönderdi";
    case "VOICE_MESSAGE":
      return "Sana bir sesli mesaj gönderdi";
    case "MESSAGE":
    default:
      return trackTitle ? "Sana bir şarkı önerdi" : "Sana yeni bir mesaj gönderdi";
  }
}

function sanitizeNotificationBody(notif, displayName, type, trackTitle) {
  let body = asCleanString(notif.message, 240).replace(/\s*\(@[^)]*\)/g, "").trim();
  const handle = asCleanString(notif.senderHandle, 30);
  if (handle) {
    body = body.split(handle).join("").trim();
  }
  body = body.replace(/\s{2,}/g, " ").trim();

  const lowerBody = body.toLowerCase();
  const lowerName = displayName.toLowerCase();
  if (lowerBody.startsWith(`${lowerName}:`)) {
    body = body.slice(displayName.length + 1).trim();
  } else if (lowerBody.startsWith(`${lowerName},`)) {
    body = body.slice(displayName.length + 1).trim();
  } else if (lowerBody.startsWith(`${lowerName} `)) {
    body = body.slice(displayName.length + 1).trim();
  }

  if (!body) {
    body = defaultBodyForType(type, trackTitle);
  }
  return trackTitle ? `${body} (♪ ${trackTitle})`.slice(0, 240) : body.slice(0, 240);
}

function channelForNotificationType(rawType) {
  const type = asCleanString(rawType, 40).toUpperCase();
  if (type === "FOLLOW" || type === "FOLLOW_REQUEST") {
    return "vizyon_social_follows_v6";
  }
  if (type === "STORY") {
    return "vizyon_social_stories_v6";
  }
  if (type === "LIKE" || type === "COMMENT" || type === "REPOST") {
    return "vizyon_social_reactions_v6";
  }
  return "vizyon_social_messages_v6";
}

function defaultTargetTypeForType(rawType) {
  const type = asCleanString(rawType, 40).toUpperCase();
  if (type === "FOLLOW") return "PROFILE";
  if (type === "FOLLOW_REQUEST" || type === "MESSAGE_REQUEST") return "REQUESTS";
  if (type === "LIKE" || type === "COMMENT" || type === "REPOST") return "POST";
  if (type === "STORY") return "STORY";
  return "CHAT";
}

function extractRecipientId(docRef, notif) {
  const fromDoc = asCleanString(notif.recipientId, 128);
  if (fromDoc) return fromDoc;
  const userDocRef = docRef.parent && docRef.parent.parent;
  return userDocRef ? asCleanString(userDocRef.id, 128) : "";
}

function extractSentTimeMs(notif) {
  if (notif.createdAt && typeof notif.createdAt.toMillis === "function") {
    return String(notif.createdAt.toMillis());
  }
  if (typeof notif.createdAt === "number" && notif.createdAt > 0) {
    return String(notif.createdAt);
  }
  return String(Date.now());
}

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function isTransientFcmError(err) {
  const code = err && err.code ? String(err.code) : "";
  if (TRANSIENT_ERROR_CODES.has(code)) return true;
  const msg = err && err.message ? String(err.message) : "";
  return /ECONNRESET|ETIMEDOUT|EAI_AGAIN|socket hang up/i.test(msg);
}

function extractConsoleUrl(message) {
  if (typeof message !== "string") return "";
  const match = message.match(/https:\/\/console\.firebase\.google\.com\/[^\s"']+/);
  return match ? match[0] : "";
}

function createWorkerTelemetry(projectId, databaseId) {
  return {
    startedAt: new Date().toISOString(),
    projectId,
    databaseId,
    credentialSource: "pending",
    listeners: {
      sentQuery: "initializing",
      legacyReadQuery: "initializing"
    },
    stats: {
      sentCount: 0,
      skippedCount: 0,
      errorCount: 0,
      lastSentAt: null,
      lastError: null
    },
    indexUrls: []
  };
}

async function claimNotificationAtomically(db, notifRef) {
  return db.runTransaction(async (tx) => {
    const freshSnap = await tx.get(notifRef);
    if (!freshSnap.exists) {
      return { shouldSend: false, reason: "missing" };
    }
    const data = freshSnap.data() || {};
    if (data.sent === true) {
      return { shouldSend: false, reason: "already_sent" };
    }
    tx.update(notifRef, {
      sent: true,
      sentAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp()
    });
    if (data.read === true) {
      return { shouldSend: false, reason: "already_read" };
    }
    return { shouldSend: true, notif: data };
  });
}

async function sendWithRetry(messaging, payload) {
  let lastError;
  for (let attempt = 1; attempt <= MAX_SEND_RETRIES; attempt++) {
    try {
      return await messaging.send(payload);
    } catch (err) {
      lastError = err;
      if (!isTransientFcmError(err) || attempt === MAX_SEND_RETRIES) {
        throw err;
      }
      await delay(1000 * attempt);
    }
  }
  throw lastError;
}

async function processNotificationSnapshot(db, messaging, docSnap, telemetry = null) {
  const notifRef = docSnap.ref;
  const docPath = notifRef.path;
  if (inFlightPaths.has(docPath)) return;
  inFlightPaths.add(docPath);

  try {
    const claim = await claimNotificationAtomically(db, notifRef);
    if (!claim.shouldSend) {
      if (telemetry && claim.reason !== "already_sent") {
        telemetry.stats.skippedCount += 1;
      }
      return;
    }

    const notif = claim.notif;
    const notificationId = asCleanString(docSnap.id, 128);
    const recipientId = extractRecipientId(notifRef, notif);
    const senderId = asCleanString(notif.senderId, 128);

    if (!recipientId || !notificationId) {
      if (telemetry) telemetry.stats.skippedCount += 1;
      return;
    }
    if (senderId && senderId === recipientId) {
      if (telemetry) telemetry.stats.skippedCount += 1;
      return;
    }

    const userRef = db.collection("users").doc(recipientId);
    const userSnap = await userRef.get();
    if (!userSnap.exists) {
      console.warn(`[SKIP] Alici kullanici belgesi bulunamadi: recipientId=${recipientId}`);
      if (telemetry) telemetry.stats.skippedCount += 1;
      await notifRef.update({ fcmError: "recipient_not_found" });
      return;
    }

    const user = userSnap.data() || {};
    if (user.fcmPushEnabled === false) {
      console.log(`[SKIP] Push kapatilmis: recipientId=${recipientId}`);
      if (telemetry) telemetry.stats.skippedCount += 1;
      return;
    }

    const fcmToken = asCleanString(user.fcmToken, 512);
    if (!fcmToken) {
      console.log(`[SKIP] Alici FCM token bos: recipientId=${recipientId}`);
      if (telemetry) telemetry.stats.skippedCount += 1;
      await notifRef.update({ fcmError: "missing_fcm_token" });
      return;
    }

    const type = (asCleanString(notif.type, 40) || "MESSAGE").toUpperCase();
    const trackId = asCleanString(notif.trackId, 128);
    const trackTitle = asCleanString(notif.trackTitle, 160);
    const targetType = asCleanString(notif.targetType, 40) || defaultTargetTypeForType(type);
    const targetId = asCleanString(notif.targetId, 128);
    const channelId = channelForNotificationType(type);
    const displayName = cleanDisplayNameOnly(notif.senderName);
    const bodyText = sanitizeNotificationBody(notif, displayName, type, trackTitle);
    const sentTimeMs = extractSentTimeMs(notif);

    const payload = {
      token: fcmToken,
      notification: {
        title: displayName,
        body: bodyText
      },
      android: {
        priority: "high",
        ttl: 2419200000,
        notification: {
          channelId,
          sound: "vizyon_notify_chime",
          icon: "ic_notif_vizyon",
          color: "#2979FF",
          priority: "max",
          defaultVibrateTimings: true,
          visibility: "public",
          tag: `notif_${notificationId}`
        }
      },
      data: {
        notificationId,
        recipientId,
        senderId,
        senderName: displayName,
        senderHandle: asCleanString(notif.senderHandle, 30),
        senderAvatarUrl: asHttpAvatarUrl(notif.senderAvatarUrl),
        type,
        channelId,
        message: bodyText,
        trackId,
        trackTitle,
        targetType,
        targetId,
        sentTimeMs
      }
    };

    try {
      const fcmMessageId = await sendWithRetry(messaging, payload);
      await notifRef.update({
        fcmMessageId,
        fcmError: FieldValue.delete()
      });
      if (telemetry) {
        telemetry.stats.sentCount += 1;
        telemetry.stats.lastSentAt = new Date().toISOString();
      }
      console.log(
        `[SENT] recipient=${recipientId} notif=${notificationId} channel=${channelId} msgId=${fcmMessageId}`
      );
    } catch (err) {
      const code = err && err.code ? String(err.code) : "unknown_error";
      if (telemetry) {
        telemetry.stats.errorCount += 1;
        telemetry.stats.lastError = `${code}: ${err.message || err}`;
      }
      console.error(
        `[ERROR] FCM gonderilemedi recipient=${recipientId} notif=${notificationId} code=${code}:`,
        err.message || err
      );
      await notifRef.update({ fcmError: code }).catch(() => {});

      if (STALE_TOKEN_ERROR_CODES.has(code)) {
        const latestUserSnap = await userRef.get();
        const latestToken = latestUserSnap.exists
          ? asCleanString((latestUserSnap.data() || {}).fcmToken, 512)
          : "";
        if (latestToken === fcmToken) {
          await userRef.update({
            fcmToken: "",
            fcmPushEnabled: false,
            updatedAt: FieldValue.serverTimestamp()
          });
          console.warn(`[CLEANUP] Gecersiz fcmToken temizlendi: recipientId=${recipientId}`);
        }
      }
    }
  } catch (err) {
    if (telemetry) {
      telemetry.stats.errorCount += 1;
      telemetry.stats.lastError = err.message || String(err);
    }
    console.error(`[ERROR] Belge islenirken hata olustu (${docPath}):`, err);
  } finally {
    inFlightPaths.delete(docPath);
  }
}

function escapeHtml(str) {
  return String(str || "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function renderLegalHtml({ title, subtitle, bodyHtml }) {
  return `<!DOCTYPE html>
<html lang="tr">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>${escapeHtml(title)} — Vizyon Music</title>
  <style>
    :root { color-scheme: dark; }
    body {
      margin: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
      background: #070B14; color: #EAF0FF; line-height: 1.65;
    }
    main { max-width: 780px; margin: 0 auto; padding: 36px 20px 64px; }
    .card {
      background: #0F172A; border: 1px solid rgba(255,255,255,0.12);
      border-radius: 20px; padding: 26px; margin-top: 18px;
    }
    h1 { margin: 0 0 6px; font-size: 28px; color: #FFFFFF; }
    h2 { margin: 22px 0 8px; font-size: 18px; color: #60A5FA; }
    .badge {
      display: inline-block; padding: 4px 12px; border-radius: 999px;
      background: rgba(41,121,255,0.18); color: #93C5FD; font-size: 12px; font-weight: 700;
    }
    a { color: #60A5FA; }
    ul { padding-left: 20px; }
    nav { margin-top: 20px; display: flex; gap: 16px; flex-wrap: wrap; font-size: 14px; }
  </style>
</head>
<body>
  <main>
    <span class="badge">VIZYON MUSIC (com.vizyon.music)</span>
    <h1>${escapeHtml(title)}</h1>
    <p style="color:#94A3B8;margin-top:0">${escapeHtml(subtitle)}</p>
    <div class="card">${bodyHtml}</div>
    <nav>
      <a href="/privacy">Gizlilik Politikası / Privacy Policy</a>
      <a href="/account-deletion">Hesap Silme / Account Deletion</a>
      <a href="/terms">Kullanım Koşulları / Terms</a>
      <a href="/health">Servis Durumu (/health)</a>
    </nav>
  </main>
</body>
</html>`;
}

function buildPrivacyHtml(supportEmail) {
  return renderLegalHtml({
    title: "Gizlilik Politikası / Privacy Policy",
    subtitle: "Son güncelleme / Last updated: 27 Kasım 2026 • Paket: com.vizyon.music",
    bodyHtml: `
      <h2>1. Toplanan Veriler ve Amaçları / Data Collected</h2>
      <ul>
        <li><strong>Google Kimlik Bilgileri (İsteğe Bağlı):</strong> Bulut yedekleme ve sosyal özellikler için Google ile giriş yaptığınızda adınız, e-posta adresiniz ve profil fotoğrafınız Firebase Authentication ve Firestore üzerinde yalnızca hesabınızı çalıştırmak için işlenir.</li>
        <li><strong>Kullanıcı İçeriği:</strong> Oluşturduğunuz çalma listeleri, müzik hikayeleri, yorumlar ve birebir metin/sesli mesajlar yalnızca sosyal akış ve sohbet teslimatı için saklanır.</li>
        <li><strong>Cihaz Bildirim Anahtarı (FCM Token):</strong> Anlık bildirimleri açık tuttuğunuzda uygulama kapalıyken bildirim ulaştırmak için cihaz bildirim token'ı saklanır; bildirimleri kapattığınızda veya çıkış yaptığınızda temizlenir.</li>
      </ul>
      <h2>2. Uygulama İzinleri / Permissions</h2>
      <ul>
        <li><strong>Fotoğraf Seçimi (Sıfır Depolama İzni):</strong> Profil fotoğrafı Android Fotoğraf Seçici (Photo Picker) ile seçilir; <code>READ_MEDIA_IMAGES</code> veya <code>READ_EXTERNAL_STORAGE</code> izni istenmez.</li>
        <li><strong>Mikrofon (<code>RECORD_AUDIO</code>):</strong> Yalnızca sohbette Sesli Mesaj kaydetmeye bastığınızda çalışır; kayıt en fazla 30 saniyedir ve onayınız olmadan gönderilmez.</li>
        <li><strong>Bildirimler (<code>POST_NOTIFICATIONS</code>):</strong> Yalnızca takip, beğeni, yorum ve mesaj bildirimlerini göstermek için kullanılır.</li>
      </ul>
      <h2>3. Veri Paylaşımı ve Güvenlik / Data Sharing & Security</h2>
      <p>Reklam SDK'sı, analitik takipçisi veya veri satışı yoktur. Tüm aktarım HTTPS/TLS üzerinden şifreli yapılır; stüdyo kasa şifresi SHA-256 + rastgele tuz ile özetlenir.</p>
      <h2>4. Hesap ve Veri Silme / Account & Data Deletion</h2>
      <p>Tüm verilerinizi uygulama içinden tek dokunuşla (<em>Profil → Ayarlar → Hesabı Kalıcı Sil</em>) veya <a href="/account-deletion">Hesap Silme Sayfası</a> üzerinden kalıcı olarak silebilirsiniz. İletişim: <a href="mailto:${escapeHtml(supportEmail)}">${escapeHtml(supportEmail)}</a>.</p>
    `
  });
}

function buildAccountDeletionHtml(supportEmail) {
  return renderLegalHtml({
    title: "Hesap ve Veri Silme / Account Deletion",
    subtitle: "Vizyon Music (com.vizyon.music) kullanıcı verilerini kalıcı silme yönergesi",
    bodyHtml: `
      <h2>1. Uygulama İçinden Anında Kalıcı Silme (Önerilen)</h2>
      <ol>
        <li>Vizyon Music uygulamasını açın ve alt menüden <strong>Profil</strong> sekmesine geçin.</li>
        <li><strong>Profil Ayarları</strong> → <strong>Hesabı Kalıcı Sil</strong> ekranını açın.</li>
        <li>Güvenlik için Google hesabınızla yeniden doğrulamayı onaylayın ve <strong>Hesabı ve Tüm Verileri Kalıcı Sil</strong> düğmesine dokunun.</li>
      </ol>
      <h2>2. E-posta ile Silme Talebi / Web Request</h2>
      <p>Uygulamaya erişiminiz yoksa, kayıtlı Google e-posta adresinizden <strong>"Vizyon Music Hesap Silme Talebi"</strong> konu başlığıyla <a href="mailto:${escapeHtml(supportEmail)}">${escapeHtml(supportEmail)}</a> adresine e-posta gönderebilirsiniz. Talepler en geç 7 gün içinde işlenir.</p>
      <h2>3. Kalıcı Olarak Silinen Veriler / Deleted Data Scope</h2>
      <ul>
        <li>Özel ve herkese açık profil kaydınız, profil fotoğrafınız ve FCM bildirim token'ınız.</li>
        <li>Favorileriniz, çalma listeleriniz, takip/takipçi listeleriniz, istekleriniz ve bildirim kutunuz.</li>
        <li>Paylaştığınız hikayeler, akış gönderileriniz ve doğrudan sohbet mesajlarınız.</li>
      </ul>
      <p>Silme işlemi sonrası bu verilerin hiçbiri sunucularda tutulmaz veya yedeklenmez.</p>
    `
  });
}

function buildTermsHtml(supportEmail) {
  return renderLegalHtml({
    title: "Kullanım Koşulları / Terms of Service",
    subtitle: "Vizyon Music müzik keşif ve sosyal topluluk platformu",
    bodyHtml: `
      <p>Vizyon Music, resmi YouTube IFrame oynatıcı ve iTunes Search API meta verileri üzerinden yasal müzik keşfi ve sosyal paylaşım sunar.</p>
      <ul>
        <li>Kullanıcılar paylaştıkları yorum, hikaye ve mesajlardan sorumludur; spam, taciz veya uygunsuz içerikler uygulama içindeki Engelle ve Şikayet Et araçlarıyla yönetilir.</li>
        <li>Destek ve geri bildirim için: <a href="mailto:${escapeHtml(supportEmail)}">${escapeHtml(supportEmail)}</a>.</li>
      </ul>
    `
  });
}

function startHttpServer(telemetry, options = {}) {
  const port =
    options.port !== undefined
      ? Number(options.port)
      : Number(process.env.PORT || 8080);
  const supportEmail = options.supportEmail || SUPPORT_EMAIL;

  const server = http.createServer((req, res) => {
    const rawUrl = req.url || "/";
    const pathname = rawUrl.split("?")[0];

    if (pathname === "/privacy" || pathname === "/privacy-policy") {
      res.writeHead(200, { "Content-Type": "text/html; charset=utf-8" });
      res.end(buildPrivacyHtml(supportEmail));
      return;
    }
    if (pathname === "/account-deletion") {
      res.writeHead(200, { "Content-Type": "text/html; charset=utf-8" });
      res.end(buildAccountDeletionHtml(supportEmail));
      return;
    }
    if (pathname === "/terms") {
      res.writeHead(200, { "Content-Type": "text/html; charset=utf-8" });
      res.end(buildTermsHtml(supportEmail));
      return;
    }
    if (pathname === "/" || pathname === "/health") {
      const listenerHealthy = telemetry.listeners.sentQuery === "listening";
      res.writeHead(200, { "Content-Type": "application/json; charset=utf-8" });
      res.end(
        JSON.stringify(
          {
            ok: listenerHealthy,
            status: listenerHealthy ? "listening" : telemetry.listeners.sentQuery,
            ...telemetry,
            timestamp: new Date().toISOString()
          },
          null,
          2
        )
      );
      return;
    }

    res.writeHead(404, { "Content-Type": "application/json; charset=utf-8" });
    res.end(JSON.stringify({ ok: false, error: "Not Found" }));
  });

  server.listen(port, "0.0.0.0", () => {
    const addr = server.address();
    const boundPort = typeof addr === "object" && addr ? addr.port : port;
    console.log(`[HTTP] Saglik & Play Console sayfalari aktif: http://0.0.0.0:${boundPort}/health`);
  });

  return server;
}

function attachManagedListener({
  name,
  query,
  shouldProcessDoc,
  db,
  messaging,
  telemetry,
  isShuttingDown
}) {
  let unsubscribe = null;
  let reconnectTimer = null;

  const attach = () => {
    if (isShuttingDown()) return;
    telemetry.listeners[name] = "connecting";
    unsubscribe = query.onSnapshot(
      (snapshot) => {
        telemetry.listeners[name] = "listening";
        for (const change of snapshot.docChanges()) {
          if (change.type === "added" || change.type === "modified") {
            const docData = change.doc.data() || {};
            if (shouldProcessDoc(docData)) {
              void processNotificationSnapshot(db, messaging, change.doc, telemetry);
            }
          }
        }
      },
      (err) => {
        telemetry.listeners[name] = `error:${err && err.code !== undefined ? err.code : "unknown"}`;
        telemetry.stats.lastError = `[${name}] ${err.message || err}`;
        console.error(`[LISTENER ERROR:${name}]`, err.message || err);
        if (err && err.code === 9) {
          const url = extractConsoleUrl(err.message || "");
          if (url && !telemetry.indexUrls.includes(url)) {
            telemetry.indexUrls.push(url);
          }
          console.error(
            `[INDEX GEREKLI:${name}] Firestore collectionGroup('notifications') indeksi eksik. ` +
              (url ? `Tek tikla olustur: ${url}` : "`firebase deploy --only firestore:indexes` calistirin.")
          );
        }
        if (unsubscribe) {
          unsubscribe();
          unsubscribe = null;
        }
        if (!isShuttingDown()) {
          reconnectTimer = setTimeout(attach, RECONNECT_DELAY_MS);
        }
      }
    );
  };

  attach();

  return () => {
    if (reconnectTimer) clearTimeout(reconnectTimer);
    if (unsubscribe) unsubscribe();
  };
}

function startWorker() {
  loadDotEnvIfPresent();
  const telemetry = createWorkerTelemetry(FIREBASE_PROJECT_ID, FIRESTORE_DATABASE_ID);
  const { credential, source } = resolveCredential();
  telemetry.credentialSource = source;

  const app = initializeApp({
    credential,
    projectId: FIREBASE_PROJECT_ID
  });

  const db = getFirestore(app, FIRESTORE_DATABASE_ID);
  const messaging = getMessaging(app);

  console.log("==================================================");
  console.log("Vizyon Muzik Harici FCM Worker Baslatildi");
  console.log(`Firebase Project : ${FIREBASE_PROJECT_ID}`);
  console.log(`Firestore DB     : ${FIRESTORE_DATABASE_ID}`);
  console.log(`Credential Source: ${source}`);
  console.log("Dinleyici 1      : collectionGroup('notifications').where('sent', '==', false)");
  console.log("Dinleyici 2      : collectionGroup('notifications').where('read', '==', false) [eski APK]");
  console.log("==================================================");

  const httpServer = startHttpServer(telemetry);
  let shuttingDown = false;
  const isShuttingDown = () => shuttingDown;

  const stopSentListener = attachManagedListener({
    name: "sentQuery",
    query: db.collectionGroup("notifications").where("sent", "==", false),
    shouldProcessDoc: (data) => data.sent !== true,
    db,
    messaging,
    telemetry,
    isShuttingDown
  });

  // Backward compatibility for legacy APKs that write read: false without the 'sent' field
  const stopLegacyReadListener = attachManagedListener({
    name: "legacyReadQuery",
    query: db.collectionGroup("notifications").where("read", "==", false),
    shouldProcessDoc: (data) => !Object.prototype.hasOwnProperty.call(data, "sent"),
    db,
    messaging,
    telemetry,
    isShuttingDown
  });

  const shutdown = (signal) => {
    if (shuttingDown) return;
    shuttingDown = true;
    console.log(`\n[SHUTDOWN] ${signal} alindi, Firestore dinleyicileri ve HTTP sunucusu kapatiliyor...`);
    stopSentListener();
    stopLegacyReadListener();
    httpServer.close(() => {
      process.exit(0);
    });
    setTimeout(() => process.exit(0), 1500).unref();
  };

  process.on("SIGINT", () => shutdown("SIGINT"));
  process.on("SIGTERM", () => shutdown("SIGTERM"));
}

if (require.main === module) {
  startWorker();
}

module.exports = {
  channelForNotificationType,
  defaultTargetTypeForType,
  cleanDisplayNameOnly,
  sanitizeNotificationBody,
  extractRecipientId,
  extractConsoleUrl,
  createWorkerTelemetry,
  claimNotificationAtomically,
  processNotificationSnapshot,
  startHttpServer
};
