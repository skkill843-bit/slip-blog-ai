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

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validArticlePayload(uid, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    topicTitle: "AI Agents Transforming Everyday Workflows in 2026",
    category: "Technology & AI",
    seoTitle: "How AI Agents Are Quietly Changing Everyday Workflows in 2026",
    metaDescription:
      "A practical look at how autonomous AI agents are reshaping work, creative writing, and software without the hype.",
    language: "English",
    targetWordCount: 2000,
    actualWordCount: 2045,
    tone: "Natural Human",
    plainTextContent: "# How AI Agents Are Quietly Changing Everyday Workflows\n\nFull article body...",
    bloggerHtmlContent: "<article class=\"slip-blog-article\"><h1>Title</h1></article>",
    heroImageAlt: "Minimalist desk with floating digital workflow cards",
    heroImageCaption: "Modern workflows increasingly blend human editorial judgement with smart automation.",
    inlineImageAlt: "Diagram of structured article creation stages",
    inlineImageCaption: "Key stages in an AI-assisted editorial workflow.",
    sources: ["MIT Technology Review | https://example.com/ai-agents"],
    createdAt: now,
    updatedAt: now,
    ...overrides,
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

test("Unauthenticated user cannot read or write articles", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("articles").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("articles")
      .doc("art_1")
      .set(validArticlePayload(ALICE_UID))
  );
});

test("Authenticated owner: valid Kotlin-mirrored create, list, partial update, and delete succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("articles")
    .doc("article_001");

  await assertSucceeds(ref.set(validArticlePayload(ALICE_UID)));

  // Empty or populated list query on /users/{uid}/articles
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("articles")
      .orderBy("createdAt", "desc")
      .get()
  );

  // Partial update with updated image and HTML while preserving createdAt & userId
  await assertSucceeds(
    ref.update({
      heroImageBase64: "abc123base64payload",
      bloggerHtmlContent: "<article class=\"slip-blog-article\"><h1>Updated</h1></article>",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(ref.delete());
});

test("Cross-user isolation: Bob cannot read, create, update, or delete Alice's articles", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("articles")
      .doc("alice_art")
      .set(validArticlePayload(ALICE_UID));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceCollection = bobDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("articles");

  await assertFails(aliceCollection.get());
  await assertFails(aliceCollection.doc("alice_art").get());
  await assertFails(
    aliceCollection.doc("bob_inject").set(validArticlePayload(BOB_UID))
  );
  await assertFails(
    aliceCollection.doc("alice_art").update({
      seoTitle: "Hijacked",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(aliceCollection.doc("alice_art").delete());
});

test("Schema validation rejects ghost fields, @DocumentId 'id', invalid enums, and future timestamps", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const col = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("articles");

  // Reject @DocumentId 'id' field
  await assertFails(
    col.doc("art_id").set(validArticlePayload(ALICE_UID, { id: "art_id" }))
  );

  // Reject ghost field
  await assertFails(
    col
      .doc("art_ghost")
      .set(validArticlePayload(ALICE_UID, { isAdminApproved: true }))
  );

  // Reject invalid language enum
  await assertFails(
    col
      .doc("art_lang")
      .set(validArticlePayload(ALICE_UID, { language: "French" }))
  );

  // Reject future timestamp
  await assertFails(
    col
      .doc("art_future")
      .set(
        validArticlePayload(ALICE_UID, {
          createdAt: new Date(Date.now() + 60000),
        })
      )
  );

  // Reject immutable createdAt mutation on update
  await assertSucceeds(col.doc("art_ok").set(validArticlePayload(ALICE_UID)));
  await assertFails(
    col.doc("art_ok").update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );
});
