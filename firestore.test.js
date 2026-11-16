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

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfile(uid) {
  const now = new Date(Date.now() - 1000);
  return {
    userId: uid,
    displayName: "Gatinho Player",
    highScore: 0,
    totalCoins: 0,
    totalRuns: 0,
    scoreMultiplier: 1,
    selectedSkin: "ginger",
    unlockedSkins: ["ginger"],
    magnetLevel: 1,
    jetpackLevel: 1,
    missionTier: 1,
    missionCoinsWithMagnet: 0,
    missionJetpackFlights: 0,
    missionTotalMeters: 0,
    missionTrainsPassed: 0,
    createdAt: now,
    updatedAt: now,
  };
}

function validRun(uid) {
  return {
    userId: uid,
    score: 1250,
    coinsCollected: 42,
    distanceMeters: 310,
    magnetCoins: 15,
    jetpackMeters: 120,
    skinId: "ginger",
    createdAt: new Date(Date.now() - 1000),
  };
}

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

test("Unauthenticated user cannot read or write user profiles or runs", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID)));
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("runs").get());
});

test("Authenticated owner can create, read, and update own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);
  await assertSucceeds(profileRef.set(validProfile(ALICE_UID)));
  await assertSucceeds(profileRef.get());
  await assertSucceeds(profileRef.update({
    highScore: 5000,
    totalCoins: 120,
    updatedAt: new Date(Date.now() - 1000),
  }));
});

test("Cross-user access to another user's profile or runs is rejected", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(BOB_UID).set(validProfile(BOB_UID));
    await ctx.firestore().collection("users").doc(BOB_UID).collection("runs").doc("run_1").set(validRun(BOB_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).update({ totalCoins: 9999, updatedAt: new Date(Date.now() - 1000) }));
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("runs").get());
});

test("Shadow update with ghost field or invalid skin is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);
  await assertSucceeds(profileRef.set(validProfile(ALICE_UID)));
  await assertFails(profileRef.update({ isAdmin: true, updatedAt: new Date(Date.now() - 1000) }));
  await assertFails(profileRef.update({ selectedSkin: "hacker_cat", updatedAt: new Date(Date.now() - 1000) }));
});

test("Owner can create and list runs, cannot update runs", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const runRef = aliceDb.collection("users").doc(ALICE_UID).collection("runs").doc("run_abc");
  await assertSucceeds(runRef.set(validRun(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("runs").get());
  await assertFails(runRef.update({ score: 999999 }));
});
