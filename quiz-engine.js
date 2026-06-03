(function (root, factory) {
  const api = factory();
  if (typeof module === "object" && module.exports) {
    module.exports = api;
  }
  root.KanaQuizEngine = api;
})(typeof globalThis !== "undefined" ? globalThis : this, function () {
  function toSelectedRowSet(selectedRows) {
    return selectedRows instanceof Set ? selectedRows : new Set(selectedRows || []);
  }

  function defaultShuffle(items, random = Math.random) {
    const result = [...items];
    for (let index = result.length - 1; index > 0; index -= 1) {
      const swapIndex = Math.floor(random() * (index + 1));
      [result[index], result[swapIndex]] = [result[swapIndex], result[index]];
    }
    return result;
  }

  function getPracticePool({
    baseRows,
    dakutenItems = [],
    smallKanaItems = [],
    selectedRows,
    includeDakuten = false,
    includeSmallKana = false,
    toItem,
  }) {
    const selectedRowSet = toSelectedRowSet(selectedRows);
    const baseItems = baseRows
      .filter((row) => selectedRowSet.has(row.id))
      .flatMap((row) => row.items.map((item) => toItem(item, row.id)));
    const extras = [];

    if (includeDakuten) {
      extras.push(
        ...dakutenItems
          .filter((item) => selectedRowSet.has(item[3]))
          .map((item) => toItem(item, "dakuten")),
      );
    }

    if (includeSmallKana) {
      extras.push(
        ...smallKanaItems
          .filter((item) => selectedRowSet.has(item[3]))
          .map((item) => toItem(item, "small")),
      );
    }

    return [...baseItems, ...extras];
  }

  function getKanaChoice(item, { mode = "katakana", random = Math.random } = {}) {
    if (item.forcedScript === "hiragana") return { kana: item.hiragana, script: "hiragana" };
    if (item.forcedScript === "katakana") return { kana: item.katakana, script: "katakana" };
    if (mode === "hiragana") return { kana: item.hiragana, script: "hiragana" };
    if (mode === "katakana") return { kana: item.katakana, script: "katakana" };
    return random() > 0.5
      ? { kana: item.katakana, script: "katakana" }
      : { kana: item.hiragana, script: "hiragana" };
  }

  function makeQuestion(item, options = {}) {
    const {
      direction = "kana-to-romaji",
      mode = "katakana",
      activeSessionType = "normal",
      modeLabel = "假名",
      random = Math.random,
    } = options;
    const kanaChoice = getKanaChoice(item, { mode, random });
    const helperPrefix =
      activeSessionType === "confusing"
        ? "易混淆专项："
        : activeSessionType === "mistakes"
          ? "错题复习："
          : "";

    if (direction === "kana-to-romaji") {
      return {
        item,
        prompt: kanaChoice.kana,
        answer: item.romaji,
        answerType: "romaji",
        answerScript: kanaChoice.script,
        helper: `${helperPrefix}选择对应的罗马音`,
      };
    }

    return {
      item,
      prompt: item.romaji,
      answer: kanaChoice.kana,
      answerType: "kana",
      answerScript: kanaChoice.script,
      helper: `${helperPrefix}选择对应的${modeLabel}`,
    };
  }

  function getOptionValue(item, question) {
    if (question.answerType === "romaji") return item.romaji;
    const script = item.forcedScript || question.answerScript;
    return script === "hiragana" ? item.hiragana : item.katakana;
  }

  function makeOptions(question, options = {}) {
    const {
      optionPool = [],
      priorityDistractors = [],
      maxOptions = 8,
      random = Math.random,
      shuffle = defaultShuffle,
    } = options;
    const priorityCandidates = priorityDistractors
      .map((item) => getOptionValue(item, question))
      .filter((item) => item !== question.answer);
    const generalCandidates = optionPool
      .filter(
        (item) =>
          item.romaji !== question.item.romaji ||
          getOptionValue(item, question) !== question.answer,
      )
      .map((item) => getOptionValue(item, question));
    const uniqueCandidates = [
      ...new Set([...priorityCandidates, ...shuffle(generalCandidates, random)]),
    ].filter((item) => item !== question.answer);

    return shuffle([question.answer, ...uniqueCandidates.slice(0, maxOptions - 1)], random);
  }

  function weightedShuffle(items, getWeight, random = Math.random, shuffle = defaultShuffle) {
    const candidates = items.map((item) => ({
      item,
      weight: Math.max(Number(getWeight(item)) || 0, 0),
    }));
    const result = [];

    while (candidates.length) {
      const totalWeight = candidates.reduce((sum, candidate) => sum + candidate.weight, 0);
      if (totalWeight <= 0) {
        result.push(...shuffle(candidates.map((candidate) => candidate.item), random));
        break;
      }

      let cursor = random() * totalWeight;
      const selectedIndex = candidates.findIndex((candidate) => {
        cursor -= candidate.weight;
        return cursor <= 0;
      });
      const index = selectedIndex === -1 ? candidates.length - 1 : selectedIndex;
      const [selected] = candidates.splice(index, 1);
      result.push(selected.item);
    }

    return result;
  }

  function buildQuestionQueue(pool, options = {}) {
    const {
      questionLimit = 0,
      getWeight = () => 1,
      makeQuestion: makeQuestionForItem = (item) => makeQuestion(item, options),
      random = Math.random,
      shuffle = defaultShuffle,
    } = options;
    const limit = Math.max(Number(questionLimit) || 0, 0);
    const rounds = [];

    if (!pool.length || !limit) return [];

    while (rounds.length < limit) {
      rounds.push(...weightedShuffle(pool, getWeight, random, shuffle));
    }

    return rounds.slice(0, limit).map(makeQuestionForItem);
  }

  return {
    buildQuestionQueue,
    getKanaChoice,
    getOptionValue,
    getPracticePool,
    makeOptions,
    makeQuestion,
    weightedShuffle,
  };
});
