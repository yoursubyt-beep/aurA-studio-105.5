const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const DATABASE_ID = "ai-studio-android-aurastud-8c2b93e0-6b56-45c0-8161-b237ea91bbd0";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

test("Unauthenticated user cannot read or write profiles, costumes, or storyboard scenes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore(DATABASE_ID);
  const ts = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("costumes").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("costumes").doc("cust_1").set({
      userId: ALICE_UID,
      title: "Sheer Ivory Look",
      prompt: "translucent ivory organza coat",
      category: "CUSTOM",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("storyboard_scenes").get());
});

test("Owner can create, update, list, and delete custom costumes with valid Kotlin payloads", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  const created = new Date(Date.now() - 5000);
  const costumeRef = aliceDb.collection("users").doc(ALICE_UID).collection("costumes").doc("cust_1");

  await assertSucceeds(
    costumeRef.set({
      userId: ALICE_UID,
      title: "Sheer Organza & Corset",
      prompt: "gossamer sheer silk organza over structured satin corset",
      category: "CUSTOM",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("costumes").get());

  await assertSucceeds(
    costumeRef.update({
      title: "Updated Sheer Organza",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject immutable createdAt mutation
  await assertFails(
    costumeRef.update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Reject ghost field / @DocumentId 'id' leakage
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("costumes").doc("cust_2").set({
      id: "cust_2",
      userId: ALICE_UID,
      title: "Ghost ID",
      prompt: "test prompt",
      category: "CUSTOM",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(costumeRef.delete());
});

test("Owner can manage profile and storyboard scenes while invalid inputs are rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  const ts = new Date(Date.now() - 2000);
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);

  await assertSucceeds(
    profileRef.set({
      userId: ALICE_UID,
      displayName: "Alice Creator",
      modelId: "gemini-nano-banana-2.1",
      autoSafeRefine: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    profileRef.update({
      modelId: "gemini-3-pro-image",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject unsupported modelId
  await assertFails(
    profileRef.update({
      modelId: "deprecated-model",
      updatedAt: new Date(Date.now() - 200),
    })
  );

  const sceneRef = profileRef.collection("storyboard_scenes").doc("scene_1");
  await assertSucceeds(
    sceneRef.set({
      userId: ALICE_UID,
      orderIndex: 1,
      wardrobeTitle: "Halter Cutaway",
      wardrobePrompt: "sleeveless halter cutaway top",
      prompt: "Editorial three-quarter framing",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Reject future timestamp
  await assertFails(
    profileRef.collection("storyboard_scenes").doc("scene_future").set({
      userId: ALICE_UID,
      orderIndex: 2,
      wardrobeTitle: "Future",
      wardrobePrompt: "future outfit",
      prompt: "future prompt",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Cross-user access is strictly denied across profiles, costumes, and storyboard scenes", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore(DATABASE_ID);
  const ts = new Date(Date.now() - 5000);

  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).collection("costumes").doc("bob_costume").set({
      userId: BOB_UID,
      title: "Bob Look",
      prompt: "linen resort coat",
      category: "CUSTOM",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("costumes").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("costumes").doc("bob_costume").get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("costumes").doc("bob_costume").update({
      title: "Hijacked",
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("costumes").doc("alice_injected").set({
      userId: ALICE_UID,
      title: "Injected",
      prompt: "injected prompt",
      category: "CUSTOM",
      createdAt: ts,
      updatedAt: ts,
    })
  );
});
