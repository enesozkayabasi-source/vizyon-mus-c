const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const logger = require("firebase-functions/logger");

initializeApp();

const FIRESTORE_DATABASE_ID = "ai-studio-android-vizyonmu-95f9d1d0-40a5-423e-9c63-5985bfe0fd3c";
const STALE_TOKEN_ERROR_CODES = new Set([
  "messaging/registration-token-not-registered",
  "messaging/invalid-registration-token"
]);

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
  if (type === "MESSAGE" || type === "VOICE_MESSAGE" || type === "MESSAGE_REQUEST") {
    return "vizyon_social_messages_v6";
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

/**
 * Triggers whenever a notification document is created under users/{recipientId}/notifications/{notificationId}
 * and sends a high-priority DATA-ONLY FCM payload so Android wakes VizyonFirebaseMessagingService
 * even when the app is completely closed/terminated.
 */
exports.onNotificationCreatedSendFcm = onDocumentCreated(
  {
    document: "users/{recipientId}/notifications/{notificationId}",
    database: FIRESTORE_DATABASE_ID,
    region: "europe-west1",
    retry: false
  },
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) return;

    const recipientId = asCleanString(event.params.recipientId, 128);
    const notificationId = asCleanString(event.params.notificationId, 128);
    if (!recipientId || !notificationId) return;

    const notif = snapshot.data() || {};
    if (notif.read === true) return;

    const senderId = asCleanString(notif.senderId, 128);
    if (senderId && senderId === recipientId) return;

    const db = getFirestore(FIRESTORE_DATABASE_ID);
    const userRef = db.collection("users").doc(recipientId);
    const userSnap = await userRef.get();
    if (!userSnap.exists) {
      logger.info("Skipped FCM push: recipient user doc missing", { recipientId, notificationId });
      return;
    }

    const user = userSnap.data() || {};
    if (user.fcmPushEnabled === false) {
      logger.info("Skipped FCM push: fcmPushEnabled is false", { recipientId, notificationId });
      return;
    }

    const fcmToken = asCleanString(user.fcmToken, 512);
    if (!fcmToken) {
      logger.info("Skipped FCM push: recipient fcmToken is empty", { recipientId, notificationId });
      return;
    }

    const type = asCleanString(notif.type, 40) || "MESSAGE";
    const targetType = asCleanString(notif.targetType, 40) || defaultTargetTypeForType(type);
    const channelId = channelForNotificationType(type);

    const sentTimeMs =
      notif.createdAt && typeof notif.createdAt.toMillis === "function"
        ? String(notif.createdAt.toMillis())
        : typeof notif.createdAt === "number" && notif.createdAt > 0
          ? String(notif.createdAt)
          : String(Date.now());

    const payload = {
      token: fcmToken,
      android: {
        priority: "high",
        ttl: 2419200000
      },
      data: {
        notificationId,
        recipientId,
        senderId,
        senderName: asCleanString(notif.senderName, 50) || "Vizyon Dinleyicisi",
        senderHandle: asCleanString(notif.senderHandle, 30),
        senderAvatarUrl: asHttpAvatarUrl(notif.senderAvatarUrl),
        type,
        channelId,
        message: asCleanString(notif.message, 240),
        trackId: asCleanString(notif.trackId, 128),
        trackTitle: asCleanString(notif.trackTitle, 160),
        targetType,
        targetId: asCleanString(notif.targetId, 128),
        sentTimeMs
      }
    };

    try {
      const messageId = await getMessaging().send(payload);
      logger.info("Sent data-only FCM push", { recipientId, notificationId, channelId, messageId });
    } catch (err) {
      const code = err && err.code ? String(err.code) : "";
      logger.warn("Failed to deliver FCM push", { recipientId, notificationId, code, err: String(err) });
      if (STALE_TOKEN_ERROR_CODES.has(code)) {
        const latestSnap = await userRef.get();
        const latestToken = latestSnap.exists ? asCleanString((latestSnap.data() || {}).fcmToken, 512) : "";
        if (latestToken === fcmToken) {
          await userRef.update({
            fcmToken: "",
            fcmPushEnabled: false,
            updatedAt: FieldValue.serverTimestamp()
          });
        }
      }
    }
  }
);
