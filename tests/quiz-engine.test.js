const assert = require("node:assert/strict");
const core = require("../kana-core.js");
const engine = require("../quiz-engine.js");

function test(name, fn) {
  try {
    fn();
    console.log(`ok - ${name}`);
  } catch (error) {
    console.error(`not ok - ${name}`);
    throw error;
  }
}

function identityShuffle(items) {
  return [...items];
}

test("builds a practice pool from selected rows and enabled extras", () => {
  const pool = engine.getPracticePool({
    baseRows: core.BASE_ROWS,
    dakutenItems: core.DAKUTEN_ITEMS,
    smallKanaItems: core.SMALL_KANA_ITEMS,
    selectedRows: new Set(["k"]),
    includeDakuten: true,
    includeSmallKana: true,
    toItem: core.toItem,
  });

  assert.equal(pool.length, 16);
  assert.deepEqual(
    pool.map((item) => item.romaji),
    [
      "ka",
      "ki",
      "ku",
      "ke",
      "ko",
      "ga",
      "gi",
      "gu",
      "ge",
      "go",
      "kya",
      "kyu",
      "kyo",
      "gya",
      "gyu",
      "gyo",
    ],
  );
});

test("makes questions with forced scripts and session labels", () => {
  const item = core.withQuestionScript(core.getItemByRomaji("nu"), "hiragana");
  const question = engine.makeQuestion(item, {
    activeSessionType: "confusing",
    direction: "romaji-to-kana",
    mode: "katakana",
    modeLabel: "假名",
  });

  assert.equal(question.prompt, "nu");
  assert.equal(question.answer, "ぬ");
  assert.equal(question.answerType, "kana");
  assert.equal(question.answerScript, "hiragana");
  assert.equal(question.helper, "易混淆专项：选择对应的假名");
});

test("prioritizes confusing distractors and removes duplicate options", () => {
  const question = engine.makeQuestion(core.getItemByRomaji("shi"), {
    direction: "romaji-to-kana",
    mode: "katakana",
  });
  const options = engine.makeOptions(question, {
    optionPool: core.getAllPracticeItems(),
    priorityDistractors: [core.withQuestionScript(core.getItemByRomaji("tsu"), "katakana")],
    shuffle: identityShuffle,
  });

  assert.equal(options.length, 8);
  assert.equal(new Set(options).size, options.length);
  assert.equal(options[0], "シ");
  assert.equal(options[1], "ツ");
});

test("builds a weighted question queue to the requested limit", () => {
  const pool = [core.getItemByRomaji("a"), core.getItemByRomaji("i")];
  const queue = engine.buildQuestionQueue(pool, {
    questionLimit: 5,
    getWeight: () => 1,
    makeQuestion: (item) => engine.makeQuestion(item, { mode: "katakana" }),
    random: () => 0,
    shuffle: identityShuffle,
  });

  assert.equal(queue.length, 5);
  assert.deepEqual(
    queue.map((question) => question.answer),
    ["a", "i", "a", "i", "a"],
  );
});
