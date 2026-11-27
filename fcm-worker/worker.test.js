"use strict";

const http = require("node:http");
const test = require("node:test");
const assert = require("node:assert/strict");
const {
  channelForNotificationType,
  defaultTargetTypeForType,
  cleanDisplayNameOnly,
  sanitizeNotificationBody,
  createWorkerTelemetry,
  processNotificationSnapshot,
  startHttpServer
} = require("./index");

function createFakeFirestore(initialDocs) {
  const store = new Map(Object.entries(initialDocs));

  function makeDocRef(docPath) {
    const parts = docPath.split("/");
    return {
      path: docPath,
      id: parts[parts.length - 1],
      parent: {
        parent: parts.length >= 4 ? { id: parts[parts.length - 3] } : null
      },
      async get() {
        const data = store.get(docPath);
        return {
          exists: data !== undefined,
          id: parts[parts.length - 1],
          ref: this,
          data: () => (data ? { ...data } : undefined)
        };
      },
      async update(patch) {
        const existing = store.get(docPath);
        if (!existing) throw new Error(`Missing doc ${docPath}`);
        const next = { ...existing };
        for (const [k, v] of Object.entries(patch)) {
          if (v && v.constructor && v.constructor.name === "DeleteTransform") {
            delete next[k];
          } else {
            next[k] = v;
          }
        }
        store.set(docPath, next);
      }
    };
  }

  return {
    store,
    makeDocRef,
    collection(name) {
      return {
        doc(id) {
          return makeDocRef(`${name}/${id}`);
        }
      };
    },
    async runTransaction(fn) {
      const updates = [];
      const tx = {
        async get(ref) {
          return ref.get();
        },
        update(ref, patch) {
          updates.push([ref, patch]);
        }
      };
      const result = await fn(tx);
      for (const [ref, patch] of updates) {
        await ref.update(patch);
      }
      return result;
    }
  };
}

function httpGet(port, pathName) {
  return new Promise((resolve, reject) => {
    http
      .get(`http://127.0.0.1:${port}${pathName}`, (res) => {
        let body = "";
        res.on("data", (chunk) => {
          body += chunk;
        });
        res.on("end", () => {
          resolve({ statusCode: res.statusCode, headers: res.headers, body });
        });
      })
      .on("error", reject);
  });
}

test("channelForNotificationType maps all Vizyon channels to v6 IDs", () => {
  assert.equal(channelForNotificationType("FOLLOW"), "vizyon_social_follows_v6");
  assert.equal(channelForNotificationType("FOLLOW_REQUEST"), "vizyon_social_follows_v6");
  assert.equal(channelForNotificationType("STORY"), "vizyon_social_stories_v6");
  assert.equal(channelForNotificationType("LIKE"), "vizyon_social_reactions_v6");
  assert.equal(channelForNotificationType("COMMENT"), "vizyon_social_reactions_v6");
  assert.equal(channelForNotificationType("REPOST"), "vizyon_social_reactions_v6");
  assert.equal(channelForNotificationType("MESSAGE"), "vizyon_social_messages_v6");
  assert.equal(channelForNotificationType("VOICE_MESSAGE"), "vizyon_social_messages_v6");
  assert.equal(channelForNotificationType("MESSAGE_REQUEST"), "vizyon_social_messages_v6");
});

test("processNotificationSnapshot delivers Notification+Data payload once via atomic sent claim", async () => {
  const notifPath = "users/u_recv_1/notifications/notif_101";
  const db = createFakeFirestore({
    "users/u_recv_1": {
      fcmToken: "fcm_tok_valid_123",
      fcmPushEnabled: true
    },
    [notifPath]: {
      recipientId: "u_recv_1",
      senderId: "u_sender_1",
      senderName: "Deniz (@deniz)",
      senderHandle: "@deniz",
      type: "MESSAGE",
      trackId: "tr_1",
      trackTitle: "Firuze",
      message: "Deniz (@deniz): Bunu dinle",
      targetType: "CHAT",
      targetId: "u_sender_1",
      read: false,
      sent: false
    }
  });

  const sentPayloads = [];
  const messaging = {
    async send(payload) {
      sentPayloads.push(payload);
      return "projects/gen-lang-client-0103909720/messages/msg_101";
    }
  };
  const telemetry = createWorkerTelemetry("gen-lang-client-0103909720", "db_1");

  const snap = await db.makeDocRef(notifPath).get();
  await processNotificationSnapshot(db, messaging, snap, telemetry);
  await processNotificationSnapshot(db, messaging, snap, telemetry);

  assert.equal(sentPayloads.length, 1);
  assert.equal(telemetry.stats.sentCount, 1);
  const p = sentPayloads[0];
  assert.equal(p.token, "fcm_tok_valid_123");
  assert.equal(p.notification.title, "Deniz");
  assert.equal(p.notification.body, "Bunu dinle (♪ Firuze)");
  assert.equal(p.android.priority, "high");
  assert.equal(p.android.notification.channelId, "vizyon_social_messages_v6");
  assert.equal(p.android.notification.visibility, "public");
  assert.equal(p.android.notification.priority, "max");
  assert.equal(p.data.notificationId, "notif_101");
  assert.equal(p.data.targetType, "CHAT");
  assert.equal(p.data.targetId, "u_sender_1");
  assert.equal(p.data.trackId, "tr_1");

  const storedNotif = db.store.get(notifPath);
  assert.equal(storedNotif.sent, true);
  assert.equal(storedNotif.fcmMessageId, "projects/gen-lang-client-0103909720/messages/msg_101");
});

test("processNotificationSnapshot delivers legacy notification missing sent field once and stamps sent=true", async () => {
  const notifPath = "users/u_legacy_recv/notifications/notif_legacy_1";
  const db = createFakeFirestore({
    "users/u_legacy_recv": {
      fcmToken: "legacy_apk_recipient_tok",
      fcmPushEnabled: true
    },
    [notifPath]: {
      recipientId: "u_legacy_recv",
      senderId: "u_legacy_sender",
      senderName: "Mert",
      senderHandle: "@mert",
      type: "FOLLOW",
      message: "Seni takip etmeye başladı!",
      targetType: "PROFILE",
      targetId: "u_legacy_sender",
      read: false
      // Note: no 'sent' field (legacy APK behavior)
    }
  });

  const sentPayloads = [];
  const messaging = {
    async send(payload) {
      sentPayloads.push(payload);
      return "projects/gen-lang-client-0103909720/messages/msg_legacy_1";
    }
  };

  const snap = await db.makeDocRef(notifPath).get();
  await processNotificationSnapshot(db, messaging, snap);
  await processNotificationSnapshot(db, messaging, snap);

  assert.equal(sentPayloads.length, 1);
  assert.equal(sentPayloads[0].android.notification.channelId, "vizyon_social_follows_v6");
  assert.equal(db.store.get(notifPath).sent, true);
});

test("processNotificationSnapshot cleans up stale FCM token cleanly", async () => {
  const notifPath = "users/u_stale/notifications/notif_stale_1";
  const db = createFakeFirestore({
    "users/u_stale": {
      fcmToken: "stale_token_xyz",
      fcmPushEnabled: true
    },
    [notifPath]: {
      recipientId: "u_stale",
      senderId: "u_sender_2",
      senderName: "Ece",
      type: "LIKE",
      message: "Beğendi",
      read: false,
      sent: false
    }
  });

  const messaging = {
    async send() {
      const err = new Error("Requested entity was not found.");
      err.code = "messaging/registration-token-not-registered";
      throw err;
    }
  };

  const snap = await db.makeDocRef(notifPath).get();
  await processNotificationSnapshot(db, messaging, snap);

  const userDoc = db.store.get("users/u_stale");
  assert.equal(userDoc.fcmToken, "");
  assert.equal(userDoc.fcmPushEnabled, false);
  const notifDoc = db.store.get(notifPath);
  assert.equal(notifDoc.sent, true);
  assert.equal(notifDoc.fcmError, "messaging/registration-token-not-registered");
});

test("cleanDisplayNameOnly and sanitizeNotificationBody strip handles cleanly", () => {
  assert.equal(cleanDisplayNameOnly("Can Demir (@can)"), "Can Demir");
  assert.equal(
    sanitizeNotificationBody(
      { message: "Can Demir (@can): Harika parça!", senderHandle: "@can" },
      "Can Demir",
      "MESSAGE",
      ""
    ),
    "Harika parça!"
  );
  assert.equal(defaultTargetTypeForType("FOLLOW_REQUEST"), "REQUESTS");
});

test("HTTP server exposes /health JSON and Play Console /privacy & /account-deletion pages", async () => {
  const telemetry = createWorkerTelemetry("gen-lang-client-0103909720", "db_1");
  telemetry.listeners.sentQuery = "listening";
  telemetry.listeners.legacyReadQuery = "listening";
  const server = startHttpServer(telemetry, { port: 0, supportEmail: "destek@vizyon.app" });

  try {
    if (!server.listening) {
      await new Promise((resolve) => server.once("listening", resolve));
    }
    const port = server.address().port;
    const healthRes = await httpGet(port, "/health");
    assert.equal(healthRes.statusCode, 200);
    const parsed = JSON.parse(healthRes.body);
    assert.equal(parsed.ok, true);
    assert.equal(parsed.status, "listening");

    const privacyRes = await httpGet(port, "/privacy");
    assert.equal(privacyRes.statusCode, 200);
    assert.match(privacyRes.body, /Gizlilik Politikası/);
    assert.match(privacyRes.body, /destek@vizyon\.app/);

    const delRes = await httpGet(port, "/account-deletion");
    assert.equal(delRes.statusCode, 200);
    assert.match(delRes.body, /Hesap ve Veri Silme/);
    assert.match(delRes.body, /com\.vizyon\.music/);
  } finally {
    await new Promise((resolve) => server.close(resolve));
  }
});
