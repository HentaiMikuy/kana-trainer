(function (root, factory) {
  const api = factory();
  if (typeof module === "object" && module.exports) {
    module.exports = api;
  }
  root.KanaPracticePlanner = api;
})(typeof globalThis !== "undefined" ? globalThis : this, function () {
  function createPracticePlanner(options) {
    const {
      baseRows,
      buildQuestionQueue,
      calculateReviewWeight,
      dakutenItems,
      getConfusingPool: buildConfusingPool,
      getConfusingSetById,
      getItemByRomaji,
      getItemKey,
      getPracticePool: buildPracticePool,
      getPracticeRecord,
      getSelectedConfusingSets: selectConfusingSets,
      getState,
      makeOptions: buildOptions,
      makeQuestion: buildQuestion,
      shuffle,
      smallKanaItems,
      toItem,
      withQuestionScript,
    } = options;

    function getConfusingSetLabel(selectedConfusingSet = getState().selectedConfusingSet) {
      if (selectedConfusingSet === "all") return "全部易混淆";
      return getConfusingSetById(selectedConfusingSet)?.label || "易混淆专项";
    }

    function getModeLabel(state = getState()) {
      if (state.practiceType === "confusing") return "假名";
      if (state.mode === "katakana") return "片假名";
      if (state.mode === "hiragana") return "平假名";
      return "混合";
    }

    function getPracticeLabel(state = getState()) {
      if (state.activeSessionType === "mistakes") return "错题复习";
      if (state.activeSessionType === "confusing") {
        return `易混淆 · ${getConfusingSetLabel(state.selectedConfusingSet)}`;
      }
      return getModeLabel(state);
    }

    function getSelectedConfusingSets(selectedConfusingSet = getState().selectedConfusingSet) {
      return selectConfusingSets(selectedConfusingSet);
    }

    function getConfusingPool(selectedConfusingSet = getState().selectedConfusingSet) {
      return buildConfusingPool(selectedConfusingSet);
    }

    function getPool(state = getState()) {
      return buildPracticePool({
        baseRows,
        dakutenItems,
        includeDakuten: state.includeDakuten,
        includeSmallKana: state.includeSmallKana,
        selectedRows: state.selectedRows,
        smallKanaItems,
        toItem,
      });
    }

    function getActivePool(state = getState()) {
      return state.practiceType === "confusing"
        ? getConfusingPool(state.selectedConfusingSet)
        : getPool(state);
    }

    function getConfusingDistractors(question, state = getState()) {
      if (state.activeSessionType !== "confusing") return [];

      return getSelectedConfusingSets(state.selectedConfusingSet)
        .filter((set) => set.items.includes(question.item.romaji))
        .flatMap((set) =>
          set.items
            .map((romaji) => getItemByRomaji(romaji))
            .filter(Boolean)
            .filter((item) => getItemKey(item) !== getItemKey(question.item))
            .map((item) => withQuestionScript(item, set.script)),
        );
    }

    function getReviewWeight(item) {
      return calculateReviewWeight(item, getPracticeRecord(item));
    }

    function makeQuestion(item, state = getState()) {
      return buildQuestion(item, {
        activeSessionType: state.activeSessionType,
        direction: state.direction,
        mode: state.mode,
        modeLabel: getModeLabel(state),
      });
    }

    function makeOptions(question, state = getState()) {
      const optionPool = state.optionPool.length ? state.optionPool : state.pool;
      return buildOptions(question, {
        optionPool,
        priorityDistractors: getConfusingDistractors(question, state),
        shuffle,
      });
    }

    function buildQueue(pool = getState().pool, state = getState()) {
      return buildQuestionQueue(pool, {
        getWeight: getReviewWeight,
        makeQuestion: (item) => makeQuestion(item, state),
        questionLimit: state.questionLimit,
        shuffle,
      });
    }

    return {
      buildQueue,
      getActivePool,
      getConfusingDistractors,
      getConfusingPool,
      getConfusingSetLabel,
      getModeLabel,
      getPool,
      getPracticeLabel,
      getReviewWeight,
      getSelectedConfusingSets,
      makeOptions,
      makeQuestion,
    };
  }

  return {
    createPracticePlanner,
  };
});
