const assert = require("node:assert/strict");
const { createPracticePlanner } = require("../practice-planner.js");

function test(name, fn) {
  try {
    fn();
    console.log(`ok - ${name}`);
  } catch (error) {
    console.error(`not ok - ${name}`);
    throw error;
  }
}

function createState(overrides = {}) {
  return {
    activeSessionType: "normal",
    direction: "kana-to-romaji",
    includeDakuten: false,
    includeSmallKana: false,
    mode: "katakana",
    optionPool: [],
    pool: [],
    practiceType: "normal",
    questionLimit: 3,
    selectedConfusingSet: "all",
    selectedRows: new Set(["a"]),
    ...overrides,
  };
}

function createHarness(initialState = {}) {
  const state = createState(initialState);
  const calls = [];
  const items = new Map([
    ["a", { romaji: "a", script: "base" }],
    ["i", { romaji: "i", script: "base" }],
    ["shi", { romaji: "shi", script: "base" }],
    ["tsu", { romaji: "tsu", script: "base" }],
    ["missing", null],
  ]);

  const planner = createPracticePlanner({
    baseRows: [{ id: "a", items: [["a", "あ", "ア"]] }],
    buildQuestionQueue(pool, options) {
      calls.push(["buildQuestionQueue", pool, options.questionLimit]);
      return pool.slice(0, options.questionLimit).map((item) => ({
        item,
        prompt: `q:${item.romaji}`,
        weight: options.getWeight(item),
        ...options.makeQuestion(item),
      }));
    },
    calculateReviewWeight(item, record) {
      calls.push(["calculateReviewWeight", item.romaji, record?.attempts || 0]);
      return record?.attempts || 1;
    },
    dakutenItems: [["ga", "が", "ガ", "a"]],
    getConfusingPool(selectedConfusingSet) {
      calls.push(["getConfusingPool", selectedConfusingSet]);
      return [{ romaji: "shi" }, { romaji: "tsu" }];
    },
    getConfusingSetById(id) {
      return id === "shi-tsu" ? { label: "シ / ツ" } : null;
    },
    getItemByRomaji(romaji) {
      return items.get(romaji);
    },
    getItemKey(item) {
      return item.romaji;
    },
    getPracticePool(options) {
      calls.push(["getPracticePool", options]);
      return [
        { romaji: "a", source: "base" },
        ...(options.includeDakuten ? [{ romaji: "ga", source: "dakuten" }] : []),
        ...(options.includeSmallKana ? [{ romaji: "kya", source: "small" }] : []),
      ];
    },
    getPracticeRecord(item) {
      return item.romaji === "shi" ? { attempts: 4 } : null;
    },
    getSelectedConfusingSets(selectedConfusingSet) {
      calls.push(["getSelectedConfusingSets", selectedConfusingSet]);
      return [
        { id: "shi-tsu", items: ["shi", "tsu", "missing"], script: "katakana" },
        { id: "a-i", items: ["a", "i"], script: "hiragana" },
      ];
    },
    getState() {
      return state;
    },
    makeOptions(question, options) {
      calls.push([
        "makeOptions",
        question.item.romaji,
        options.optionPool.map((item) => item.romaji),
        options.priorityDistractors.map((item) => `${item.romaji}:${item.forcedScript}`),
      ]);
      return [question.answer, ...options.priorityDistractors.map((item) => item.romaji)];
    },
    makeQuestion(item, options) {
      calls.push(["makeQuestion", item.romaji, options]);
      return {
        answer: item.romaji,
        answerType: options.direction === "kana-to-romaji" ? "romaji" : "kana",
        helper: options.modeLabel,
      };
    },
    shuffle(items) {
      return [...items];
    },
    smallKanaItems: [["kya", "きゃ", "キャ", "a"]],
    toItem(item, group) {
      return { group, romaji: item[0] };
    },
    withQuestionScript(item, script) {
      return { ...item, forcedScript: script };
    },
  });

  return {
    calls,
    planner,
    state,
  };
}

test("builds mode and practice labels", () => {
  const { planner, state } = createHarness();

  assert.equal(planner.getModeLabel(), "片假名");
  state.mode = "hiragana";
  assert.equal(planner.getModeLabel(), "平假名");
  state.mode = "mixed";
  assert.equal(planner.getModeLabel(), "混合");

  state.practiceType = "confusing";
  assert.equal(planner.getModeLabel(), "假名");

  state.activeSessionType = "mistakes";
  assert.equal(planner.getPracticeLabel(), "错题复习");

  state.activeSessionType = "confusing";
  state.selectedConfusingSet = "shi-tsu";
  assert.equal(planner.getPracticeLabel(), "易混淆 · シ / ツ");
  assert.equal(planner.getConfusingSetLabel("unknown"), "易混淆专项");
  assert.equal(planner.getConfusingSetLabel("all"), "全部易混淆");
});

test("builds normal and confusing practice pools", () => {
  const { calls, planner, state } = createHarness({
    includeDakuten: true,
    includeSmallKana: true,
  });

  assert.deepEqual(planner.getPool().map((item) => item.romaji), ["a", "ga", "kya"]);
  assert.equal(calls[0][0], "getPracticePool");
  assert.equal(calls[0][1].includeDakuten, true);
  assert.equal(calls[0][1].includeSmallKana, true);
  assert.equal(calls[0][1].selectedRows, state.selectedRows);

  state.practiceType = "confusing";
  state.selectedConfusingSet = "shi-tsu";
  assert.deepEqual(planner.getActivePool().map((item) => item.romaji), ["shi", "tsu"]);
});

test("builds confusing distractors only for matching confusing sessions", () => {
  const { planner, state } = createHarness({
    activeSessionType: "normal",
    selectedConfusingSet: "shi-tsu",
  });
  const question = { item: { romaji: "shi" } };

  assert.deepEqual(planner.getConfusingDistractors(question), []);

  state.activeSessionType = "confusing";
  assert.deepEqual(planner.getConfusingDistractors(question), [
    { forcedScript: "katakana", romaji: "tsu", script: "base" },
  ]);
});

test("makes questions with current session context", () => {
  const { calls, planner, state } = createHarness({
    activeSessionType: "mistakes",
    direction: "romaji-to-kana",
    mode: "hiragana",
  });

  const question = planner.makeQuestion({ romaji: "shi" });

  assert.equal(question.answerType, "kana");
  assert.equal(question.helper, "平假名");
  assert.deepEqual(calls.at(-1), [
    "makeQuestion",
    "shi",
    {
      activeSessionType: "mistakes",
      direction: "romaji-to-kana",
      mode: "hiragana",
      modeLabel: "平假名",
    },
  ]);

  state.practiceType = "confusing";
  assert.equal(planner.makeQuestion({ romaji: "tsu" }).helper, "假名");
});

test("makes options with explicit option pool and confusing distractors", () => {
  const { calls, planner, state } = createHarness({
    activeSessionType: "confusing",
    optionPool: [{ romaji: "custom" }],
    pool: [{ romaji: "fallback" }],
    selectedConfusingSet: "shi-tsu",
  });

  const options = planner.makeOptions({
    answer: "shi",
    item: { romaji: "shi" },
  });

  assert.deepEqual(options, ["shi", "tsu"]);
  assert.deepEqual(calls.at(-1), [
    "makeOptions",
    "shi",
    ["custom"],
    ["tsu:katakana"],
  ]);

  state.optionPool = [];
  planner.makeOptions({ answer: "a", item: { romaji: "a" } });
  assert.deepEqual(calls.at(-1)[2], ["fallback"]);
});

test("builds weighted queues through injected queue builder", () => {
  const { calls, planner, state } = createHarness({
    activeSessionType: "normal",
    questionLimit: 2,
  });
  const pool = [{ romaji: "shi" }, { romaji: "a" }, { romaji: "tsu" }];

  const queue = planner.buildQueue(pool);

  assert.equal(queue.length, 2);
  assert.deepEqual(queue.map((question) => question.item.romaji), ["shi", "a"]);
  assert.deepEqual(queue.map((question) => question.weight), [4, 1]);
  assert.deepEqual(calls[0], ["buildQuestionQueue", pool, 2]);
  assert.deepEqual(
    calls.filter((call) => call[0] === "calculateReviewWeight"),
    [
      ["calculateReviewWeight", "shi", 4],
      ["calculateReviewWeight", "a", 0],
    ],
  );

  state.mode = "hiragana";
  assert.equal(planner.buildQueue([{ romaji: "a" }])[0].helper, "平假名");
});
