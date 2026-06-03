const assert = require("node:assert/strict");
const core = require("../kana-core.js");

function test(name, fn) {
  try {
    fn();
    console.log(`ok - ${name}`);
  } catch (error) {
    console.error(`not ok - ${name}`);
    throw error;
  }
}

test("builds the complete practice item list", () => {
  const items = core.getAllPracticeItems();

  assert.equal(items.length, 104);
  assert.deepEqual(core.getItemByRomaji("shi"), {
    romaji: "shi",
    hiragana: "し",
    katakana: "シ",
    group: "s",
  });
});

test("accepts romaji variants and both kana scripts", () => {
  const variantQuestion = {
    answerType: "romaji",
    item: core.getItemByRomaji("ji/di"),
  };
  const kanaQuestion = {
    answerType: "kana",
    item: core.getItemByRomaji("shi"),
  };

  assert.deepEqual(core.getAcceptedAnswers(variantQuestion), ["ji", "di"]);
  assert.equal(core.isAnswerCorrect(variantQuestion, " DI "), true);
  assert.equal(core.isAnswerCorrect(kanaQuestion, "し"), true);
  assert.equal(core.isAnswerCorrect(kanaQuestion, "シ"), true);
  assert.equal(core.isAnswerCorrect(kanaQuestion, "ツ"), false);
});

test("normalizes imported records against canonical kana data", () => {
  const imported = {
    romaji: "shi",
    hiragana: "し",
    katakana: "シ",
    group: "wrong",
    attempts: 1,
    correct: 2,
    misses: 1,
    masteredStreak: 10,
    lastPracticedAt: "2026-06-02T00:00:00.000Z",
  };
  const canonical = core.getCanonicalPracticeItem(imported);
  const normalized = core.normalizeRecord(imported, canonical);

  assert.equal(normalized.group, "s");
  assert.equal(normalized.attempts, 3);
  assert.equal(normalized.correct, 2);
  assert.equal(normalized.misses, 1);
  assert.equal(normalized.masteredStreak, 2);
});

test("keeps only currently weak records in the long-term mistake pool", () => {
  assert.equal(
    core.isLongTermWeakRecord({
      attempts: 5,
      correct: 4,
      misses: 1,
      masteredStreak: 3,
    }),
    false,
  );
  assert.equal(
    core.isLongTermWeakRecord({
      attempts: 3,
      correct: 2,
      misses: 1,
      masteredStreak: 2,
    }),
    true,
  );
  assert.equal(
    core.isLongTermWeakRecord({
      attempts: 3,
      correct: 3,
      misses: 0,
      masteredStreak: 3,
    }),
    false,
  );
});

test("weights weak records above mastered records", () => {
  const item = core.getItemByRomaji("shi");
  const now = Date.parse("2026-06-02T00:00:00.000Z");
  const weakWeight = core.calculateReviewWeight(
    item,
    {
      attempts: 4,
      correct: 1,
      misses: 3,
      masteredStreak: 0,
      lastPracticedAt: "2026-06-02T00:00:00.000Z",
    },
    now,
  );
  const masteredWeight = core.calculateReviewWeight(
    item,
    {
      attempts: 10,
      correct: 8,
      misses: 2,
      masteredStreak: 5,
      lastPracticedAt: "2026-06-02T00:00:00.000Z",
    },
    now,
  );

  assert.ok(weakWeight > masteredWeight);
  assert.ok(core.calculateReviewWeight(item, null, now) > 1);
});

test("builds confusing pools with the expected script", () => {
  const pool = core.getConfusingPool("nu-me");

  assert.deepEqual(
    pool.map((item) => item.romaji).sort(),
    ["me", "nu"],
  );
  assert.equal(pool.every((item) => item.forcedScript === "hiragana"), true);
});

test("shuffles without mutating the input", () => {
  const source = [1, 2, 3, 4];
  const shuffled = core.shuffle(source, () => 0);

  assert.deepEqual(source, [1, 2, 3, 4]);
  assert.deepEqual(shuffled, [2, 3, 4, 1]);
});
