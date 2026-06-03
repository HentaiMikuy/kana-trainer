(function (root, factory) {
  const api = factory();
  if (typeof module === "object" && module.exports) {
    module.exports = api;
  }
  root.KanaPracticeSession = api;
})(typeof globalThis !== "undefined" ? globalThis : this, function () {
  function calculateAccuracy(answered, correct) {
    return answered === 0 ? 0 : Math.round((correct / answered) * 100);
  }

  function getAverageAnswerTime(state, options = {}) {
    const {
      formatSeconds = (seconds) => `${seconds.toFixed(1)}s`,
    } = options;

    if (!state.timeAnsweredCount) return "0.0s";
    return formatSeconds(state.totalAnswerTimeMs / state.timeAnsweredCount / 1000);
  }

  function getCompletedQuestionCount(state) {
    return state.selectedAnswer ? state.currentIndex + 1 : state.currentIndex;
  }

  function getProgressPercent(state) {
    const questionLimit = Math.max(Number(state.questionLimit) || 0, 0);
    if (!questionLimit) return 0;
    return (getCompletedQuestionCount(state) / questionLimit) * 100;
  }

  function getStatsViewModel(state, options = {}) {
    return {
      accuracy: calculateAccuracy(state.answered, state.correct),
      answered: state.answered,
      averageTime: getAverageAnswerTime(state, options),
      progressPercent: getProgressPercent(state),
      streak: state.streak,
    };
  }

  function applyAnswerStats(state, isCorrect) {
    state.selectedAnswer = true;
    state.answered += 1;

    if (isCorrect) {
      state.correct += 1;
      state.streak += 1;
    } else {
      state.streak = 0;
    }

    return {
      answered: state.answered,
      correct: state.correct,
      streak: state.streak,
    };
  }

  function resetRoundStats(state) {
    state.answered = 0;
    state.correct = 0;
    state.streak = 0;
    state.totalAnswerTimeMs = 0;
    state.timeAnsweredCount = 0;
    state.roundMistakes.clear();
    return state;
  }

  function getCompletionStats(state) {
    return {
      accuracy: calculateAccuracy(state.answered, state.correct),
      wrongCount: Math.max(state.answered - state.correct, 0),
    };
  }

  function getMistakeReviewQuestionLimit(itemsOrCount, options = {}) {
    const {
      max = 50,
      min = 10,
      multiplier = 2,
    } = options;
    const count = Array.isArray(itemsOrCount)
      ? itemsOrCount.length
      : Math.max(Number(itemsOrCount) || 0, 0);

    if (!count) return 0;
    return Math.min(Math.max(count * multiplier, min), max);
  }

  function createChoicePool(items, options = {}) {
    const {
      allPracticeItems = [],
      getItemKey = (item) => item?.romaji,
    } = options;
    const keyedItems = new Map();

    [...allPracticeItems, ...items]
      .filter(Boolean)
      .forEach((item) => keyedItems.set(getItemKey(item), item));

    return [...keyedItems.values()];
  }

  function getNextQuestionAction(state) {
    if (!state.selectedAnswer) return "blocked";
    return state.currentIndex + 1 >= state.questionLimit ? "finish" : "advance";
  }

  function advanceQuestion(state) {
    state.currentIndex += 1;
    return state.currentIndex;
  }

  return {
    advanceQuestion,
    applyAnswerStats,
    calculateAccuracy,
    createChoicePool,
    getAverageAnswerTime,
    getCompletedQuestionCount,
    getCompletionStats,
    getMistakeReviewQuestionLimit,
    getNextQuestionAction,
    getProgressPercent,
    getStatsViewModel,
    resetRoundStats,
  };
});
