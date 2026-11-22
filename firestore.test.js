const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const CHARLIE_UID = "charlie_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

const SAMPLE_AVATAR_DATA_URL = "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8U";

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users are denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("favorites").get());
  await assertFails(unauthDb.collection("activity_feed").where("visibility", "==", "PUBLIC").get());
  await assertFails(unauthDb.collection("public_profiles").where("discoverable", "==", true).get());
  await assertFails(unauthDb.collection("chats").where("participantIds", "array-contains", ALICE_UID).get());
});

test("User profile supports vault passcode backup, data-url avatar, and enforces strict owner isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      handle: "@alice",
      bio: "Gece müzikleri",
      photoUrl: SAMPLE_AVATAR_DATA_URL,
      favoriteGenres: ["Türkçe Pop", "Anadolu Rock", "Elektronik"],
      shareActivity: true,
      chatPrivacy: "PUBLIC",
      defaultStoryTheme: "AMOLED_GOLD",
      securityPasscodeHash: "abcdef1234567890",
      securityPasscodeSalt: "salt99",
      rememberMe: true,
      lockOnLaunch: false,
      preferredThemeMode: "NIGHT_AMOLED",
      preferredAccentPalette: "VIZYON_BLUE",
      preferredCornerDensity: "BALANCED",
      preferredLanguage: "TURKISH",
      onboardingCompleted: true,
      recentTracks: [
        {
          trackId: "tr_1",
          title: "Kış Güneşi",
          artistId: "art_tarkan",
          artistName: "Tarkan",
          source: "HYBRID",
          playedAtMillis: Date.now() - 1000,
        },
      ],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Cross-user read & write must fail (including passcode hash protection)
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Favorites create + list + @DocumentId 'id' rejection
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("favorites").doc("yt_abc123").set({
      trackId: "yt_abc123",
      title: "Midnight Echoes",
      artistId: "art_1",
      artistName: "Neon Pulse",
      albumName: "Nocturne",
      durationMs: 210000,
      source: "YOUTUBE",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("favorites").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("favorites").get());

  // Reject extra @DocumentId 'id' field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("favorites").doc("yt_bad").set({
      id: "yt_bad",
      trackId: "yt_bad",
      title: "Invalid",
      artistId: "art_1",
      artistName: "Neon Pulse",
      durationMs: 120000,
      source: "YOUTUBE",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Legacy downloads collection rejects new client writes, allows owner read/delete on existing doc
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("downloads").doc("sp_track1").set({
      trackId: "sp_track1",
      title: "Emerald Horizon",
      artistId: "art_2",
      artistName: "Vizyon Collective",
      durationMs: 185000,
      source: "YOUTUBE",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).collection("downloads").doc("sp_track1").set({
      trackId: "sp_track1",
      title: "Emerald Horizon",
      artistId: "art_2",
      artistName: "Vizyon Collective",
      durationMs: 185000,
      source: "YOUTUBE",
      createdAt: ts,
      updatedAt: ts,
    });
  });
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("downloads").doc("sp_track1").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("downloads").doc("sp_track1").delete());

  // Customizable playlist + tracks subcollection
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("playlists").doc("pl_night").set({
      userId: ALICE_UID,
      title: "Gece Sürüşü",
      description: "AMOLED gece modunda dinlenecekler",
      accentColorHex: "#1ED760",
      coverStyle: "CHROME",
      sharedPublic: true,
      trackCount: 1,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("playlists").doc("pl_night")
      .collection("tracks").doc("sp_track1").set({
        trackId: "sp_track1",
        title: "Emerald Horizon",
        artistId: "art_2",
        artistName: "Vizyon Collective",
        durationMs: 185000,
        source: "YOUTUBE",
        createdAt: ts,
        updatedAt: ts,
      })
  );
});

test("Public profiles, data-URL avatars, bi-directional following/followers, and safe reactions pass rules", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date(Date.now() - 3000);

  await assertSucceeds(
    aliceDb.collection("public_profiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      handle: "@alice",
      photoUrl: SAMPLE_AVATAR_DATA_URL,
      discoverable: true,
      chatPrivacy: "PUBLIC",
      defaultStoryTheme: "NEON_WAVE",
      followerCount: 1,
      followingCount: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Filtered discovery query succeeds; unfiltered query fails
  await assertSucceeds(bobDb.collection("public_profiles").where("discoverable", "==", true).get());
  await assertFails(bobDb.collection("public_profiles").get());

  // Bi-directional follow: Bob writes to bob/following/alice and alice/followers/bob and increments Alice followerCount +1
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).collection("following").doc(ALICE_UID).set({
      targetUserId: ALICE_UID,
      displayName: "Alice",
      handle: "@alice",
      photoUrl: SAMPLE_AVATAR_DATA_URL,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    bobDb.collection("users").doc(ALICE_UID).collection("followers").doc(BOB_UID).set({
      targetUserId: BOB_UID,
      displayName: "Bob",
      handle: "@bob",
      photoUrl: SAMPLE_AVATAR_DATA_URL,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    bobDb.collection("public_profiles").doc(ALICE_UID).update({
      followerCount: 2,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  // Charlie cannot spoof Bob in Alice's followers or jump followerCount by +5
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  await assertFails(
    charlieDb.collection("users").doc(ALICE_UID).collection("followers").doc(BOB_UID).set({
      targetUserId: BOB_UID,
      displayName: "Spoof",
      handle: "@spoof",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    charlieDb.collection("public_profiles").doc(ALICE_UID).update({
      followerCount: 7,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Signed-in users can read followers & following lists
  await assertSucceeds(charlieDb.collection("users").doc(ALICE_UID).collection("followers").get());
  await assertSucceeds(charlieDb.collection("users").doc(BOB_UID).collection("following").get());

  // Activity feed create + data-URL avatar + filtered query + like increment
  await assertSucceeds(
    aliceDb.collection("activity_feed").doc("act_1").set({
      userId: ALICE_UID,
      userName: "Alice",
      userHandle: "@alice",
      userAvatarUrl: SAMPLE_AVATAR_DATA_URL,
      actionType: "SHARED_TRACK",
      trackId: "dz_999",
      trackTitle: "Starlight Synth",
      artistId: "art_9",
      artistName: "Aurora",
      source: "HYBRID",
      note: "Bu geçiş harika!",
      visibility: "PUBLIC",
      likesCount: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(bobDb.collection("activity_feed").where("visibility", "==", "PUBLIC").get());
  await assertFails(bobDb.collection("activity_feed").get());

  await assertSucceeds(
    bobDb.collection("activity_feed").doc("act_1").update({
      likesCount: 1,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    bobDb.collection("activity_feed").doc("act_1").update({
      likesCount: 5,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Direct messages (/chats and /messages) and MESSAGE notifications enforce participant-only isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = new Date(Date.now() - 2000);
  const chatId = "dm_alice_123__bob_456";

  // Alice starts DM thread with Bob
  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).set({
      chatId,
      participantIds: [ALICE_UID, BOB_UID],
      participantNames: { [ALICE_UID]: "Alice", [BOB_UID]: "Bob" },
      participantHandles: { [ALICE_UID]: "@alice", [BOB_UID]: "@bob" },
      participantAvatars: { [ALICE_UID]: SAMPLE_AVATAR_DATA_URL, [BOB_UID]: "" },
      lastMessageText: "Selam Bob, bu parçayı dinledin mi?",
      lastSenderId: ALICE_UID,
      lastTrackTitle: "Gülpembe",
      lastMessageAtMillis: Date.now() - 1000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Alice sends a track-attached message
  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).collection("messages").doc("msg_1").set({
      chatId,
      participantIds: [ALICE_UID, BOB_UID],
      senderId: ALICE_UID,
      senderName: "Alice",
      senderHandle: "@alice",
      senderAvatarUrl: SAMPLE_AVATAR_DATA_URL,
      text: "Selam Bob, bu parçayı dinledin mi?",
      trackId: "tr_gulpembe",
      trackTitle: "Gülpembe",
      artistName: "Barış Manço",
      source: "YOUTUBE",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Bob can query chats and messages
  await assertSucceeds(bobDb.collection("chats").where("participantIds", "array-contains", BOB_UID).get());
  await assertSucceeds(
    bobDb.collection("chats").doc(chatId).collection("messages")
      .where("participantIds", "array-contains", BOB_UID).get()
  );

  // Bob updates thread & replies
  await assertSucceeds(
    bobDb.collection("chats").doc(chatId).update({
      lastMessageText: "Evet harika!",
      lastSenderId: BOB_UID,
      lastMessageAtMillis: Date.now() - 500,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Charlie cannot read, list, or inject into Alice-Bob chat
  await assertFails(charlieDb.collection("chats").doc(chatId).get());
  await assertFails(
    charlieDb.collection("chats").doc(chatId).collection("messages")
      .where("participantIds", "array-contains", CHARLIE_UID).get()
  );
  await assertFails(
    charlieDb.collection("chats").doc(chatId).collection("messages").doc("msg_evil").set({
      chatId,
      participantIds: [ALICE_UID, BOB_UID],
      senderId: CHARLIE_UID,
      senderName: "Charlie",
      senderHandle: "@charlie",
      text: "Intruder!",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Alice sends MESSAGE notification to Bob
  await assertSucceeds(
    aliceDb.collection("users").doc(BOB_UID).collection("notifications").doc("notif_msg_1").set({
      recipientId: BOB_UID,
      senderId: ALICE_UID,
      senderName: "Alice",
      senderHandle: "@alice",
      senderAvatarUrl: SAMPLE_AVATAR_DATA_URL,
      type: "MESSAGE",
      trackId: "tr_gulpembe",
      trackTitle: "Gülpembe",
      targetType: "CHAT",
      targetId: ALICE_UID,
      message: "Alice (@alice) sana mesaj gönderdi: Selam Bob...",
      read: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("PublicProfile recentTracks (max 5), /stories collection, and STORY deep-link notifications pass rules", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date(Date.now() - 2000);

  const recentTracks = [1, 2, 3, 4, 5].map((i) => ({
    trackId: `tr_${i}`,
    title: `Track ${i}`,
    artistId: `art_${i}`,
    artistName: `Artist ${i}`,
    artworkUrl: "https://example.com/art.jpg",
    youtubeVideoId: "",
    source: "HYBRID",
    playedAtMillis: Date.now() - i * 1000,
  }));

  await assertSucceeds(
    aliceDb.collection("public_profiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      handle: "@alice",
      discoverable: true,
      recentTracks,
      followerCount: 2,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Exceeding 5 recentTracks must fail
  await assertFails(
    aliceDb.collection("public_profiles").doc(ALICE_UID).update({
      recentTracks: [...recentTracks, { trackId: "tr_6", title: "Track 6" }],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Stories: Alice publishes PUBLIC story with data-url avatar
  await assertSucceeds(
    aliceDb.collection("stories").doc("story_1").set({
      userId: ALICE_UID,
      userName: "Alice",
      userHandle: "@alice",
      userAvatarUrl: SAMPLE_AVATAR_DATA_URL,
      trackId: "tr_1",
      trackTitle: "Track 1",
      artistId: "art_1",
      artistName: "Artist 1",
      source: "HYBRID",
      note: "Gece hikayesi",
      cardTheme: "AMOLED_GOLD",
      visibility: "PUBLIC",
      expiresAtMillis: Date.now() + 86400000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(bobDb.collection("stories").where("visibility", "==", "PUBLIC").get());
  await assertFails(bobDb.collection("stories").get());
  await assertFails(bobDb.collection("stories").doc("story_1").delete());
  await assertSucceeds(aliceDb.collection("stories").doc("story_1").delete());
});
