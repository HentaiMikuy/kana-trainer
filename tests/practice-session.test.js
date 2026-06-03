const assert = require("node:assert/strict");
const session = require("../practice-session.js");

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
    answered: 0,
    correct: 0,
    currentIndex: 0,
    questionLimit: 10,
    roundMistakes: new Map([["shi", { romaji: "shi" }]]),
    selectedAnswer: false,
    streak: 0,
    timeAnsweredCount: 0,
    totalAnswerTimeMs: 0,
    ...overrides,
  };
}

test("builds stats view models from round state", () => {
  const state = createState({
    answered: 3,
    correct: 2,
    currentIndex: 2,
    questionLimit: 10,
    selectedAnswer: true,
    streak: 1,
    timeAnsweredCount: 2,
    totalAnswerTimeMs: 3500,
  });

  assert.equal(session.calculateAccuracy(3, 2), 67);
  assert.deepEqual(
    session.getStatsViewModel(state, {
      formatSeconds(seconds) {
        return `${seconds.toFixed(2)}s`;
      },
    }),
    {
      accuracy: 67,
      answered: 3,
      averageTime: "1.75s",
      progressPercent: 30,
      streak: 1,
    },
  );
});

test("returns stable average time and progress edge cases", () => {
  const state = createState({
    questionLimit: 0,
    timeAnsweredCount: 0,
    totalAnswerTimeMs: 4000,
  });

  assert.equal(session.getAverageAnswerTime(state), "0.0s");
  assert.equal(session.getProgressPercent(state), 0);
});

test("applies answer stats for correct and incorrect answers", () => {
  const state = createState({
    answered: 1,
    correct: 1,
    streak: 1,
  });

  assert.deepEqual(session.applyAnswerStats(state, true), {
    answered: 2,
    correct: 2,
    streak: 2,
  });
  assert.equal(state.selectedAnswer, true);

  assert.deepEqual(session.applyAnswerStats(state, false), {
    answered: 3,
    correct: 2,
    streak: 0,
  });
});

test("resets round stats while keeping long-lived configuration", () => {
  const state = createState({
    answered: 5,
    correct: 3,
    streak: 2,
    timeAnsweredCount: 4,
    totalAnswerTimeMs: 9000,
  });

  session.resetRoundStats(state);

  assert.equal(state.answered, 0);
  assert.equal(state.correct, 0);
  assert.equal(state.streak, 0);
  assert.equal(state.timeAnsweredCount, 0);
  assert.equal(state.totalAnswerTimeMs, 0);
  assert.equal(state.roundMistakes.size, 0);
  assert.equal(state.questionLimit, 10);
});

test("calculates completion stats", () => {
  assert.deepEqual(
    session.getCompletionStats(createState({ answered: 8, correct: 6 })),
    {
      accuracy: 75,
      wrongCount: 2,
    },
  );
  assert.deepEqual(
    session.getCompletionStats(createState({ answered: 0, correct: 0 })),
    {
      accuracy: 0,
      wrongCount: 0,
    },
  );
});

test("calculates mistake review question limits", () => {
  assert.equal(session.getMistakeReviewQuestionLimit([]), 0);
  assert.equal(session.getMistakeReviewQuestionLimit([1]), 10);
  assert.equal(session.getMistakeReviewQuestionLimit(new Array(12)), 24);
  assert.equal(session.getMistakeReviewQuestionLimit(new Array(40)), 50);
  assert.equal(session.getMistakeReviewQuestionLimit(3, { min: 4, max: 9, multiplier: 3 }), 9);
});

test("creates de-duplicated choice pools and lets review items override base items", () => {
  const baseA = { romaji: "a", source: "base" };
  const reviewA = { romaji: "a", source: "review" };
  const reviewShi = { romaji: "shi", source: "review" };
  const pool = session.createChoicePool([reviewA, reviewShi], {
    allPracticeItems: [baseA, { romaji: "i", source: "base" }],
    getItemKey(item) {
      return item.romaji;
    },
  });

  assert.deepEqual(pool, [
    reviewA,
    { romaji: "i", source: "base" },
    reviewShi,
  ]);
});

test("decides next-question actions and advances state", () => {
  assert.equal(
    session.getNextQuestionAction(createState({ selectedAnswer: false })),
    "blocked",
  );
  assert.equal(
    session.getNextQuestionAction(createState({ currentIndex: 2, questionLimit: 5, selectedAnswer: true })),
    "advance",
  );
  assert.equal(
    session.getNextQuestionAction(createState({ currentIndex: 4, questionLimit: 5, selectedAnswer: true })),
    "finish",
  );

  const state = createState({ currentIndex: 1 });
  assert.equal(session.advanceQuestion(state), 2);
  assert.equal(state.currentIndex, 2);
});
