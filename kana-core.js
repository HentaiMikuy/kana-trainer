(function (root, factory) {
  const api = factory();
  if (typeof module === "object" && module.exports) {
    module.exports = api;
  }
  root.KanaCore = api;
})(typeof globalThis !== "undefined" ? globalThis : this, function () {
  const BASE_ROWS = [
    {
      id: "a",
      label: "あ",
      items: [
        ["a", "あ", "ア"],
        ["i", "い", "イ"],
        ["u", "う", "ウ"],
        ["e", "え", "エ"],
        ["o", "お", "オ"],
      ],
    },
    {
      id: "k",
      label: "か",
      items: [
        ["ka", "か", "カ"],
        ["ki", "き", "キ"],
        ["ku", "く", "ク"],
        ["ke", "け", "ケ"],
        ["ko", "こ", "コ"],
      ],
    },
    {
      id: "s",
      label: "さ",
      items: [
        ["sa", "さ", "サ"],
        ["shi", "し", "シ"],
        ["su", "す", "ス"],
        ["se", "せ", "セ"],
        ["so", "そ", "ソ"],
      ],
    },
    {
      id: "t",
      label: "た",
      items: [
        ["ta", "た", "タ"],
        ["chi", "ち", "チ"],
        ["tsu", "つ", "ツ"],
        ["te", "て", "テ"],
        ["to", "と", "ト"],
      ],
    },
    {
      id: "n",
      label: "な",
      items: [
        ["na", "な", "ナ"],
        ["ni", "に", "ニ"],
        ["nu", "ぬ", "ヌ"],
        ["ne", "ね", "ネ"],
        ["no", "の", "ノ"],
      ],
    },
    {
      id: "h",
      label: "は",
      items: [
        ["ha", "は", "ハ"],
        ["hi", "ひ", "ヒ"],
        ["fu", "ふ", "フ"],
        ["he", "へ", "ヘ"],
        ["ho", "ほ", "ホ"],
      ],
    },
    {
      id: "m",
      label: "ま",
      items: [
        ["ma", "ま", "マ"],
        ["mi", "み", "ミ"],
        ["mu", "む", "ム"],
        ["me", "め", "メ"],
        ["mo", "も", "モ"],
      ],
    },
    {
      id: "y",
      label: "や",
      items: [
        ["ya", "や", "ヤ"],
        ["yu", "ゆ", "ユ"],
        ["yo", "よ", "ヨ"],
      ],
    },
    {
      id: "r",
      label: "ら",
      items: [
        ["ra", "ら", "ラ"],
        ["ri", "り", "リ"],
        ["ru", "る", "ル"],
        ["re", "れ", "レ"],
        ["ro", "ろ", "ロ"],
      ],
    },
    {
      id: "w",
      label: "わ",
      items: [
        ["wa", "わ", "ワ"],
        ["wo", "を", "ヲ"],
        ["n", "ん", "ン"],
      ],
    },
  ];

  const DAKUTEN_ITEMS = [
    ["ga", "が", "ガ", "k"],
    ["gi", "ぎ", "ギ", "k"],
    ["gu", "ぐ", "グ", "k"],
    ["ge", "げ", "ゲ", "k"],
    ["go", "ご", "ゴ", "k"],
    ["za", "ざ", "ザ", "s"],
    ["ji", "じ", "ジ", "s"],
    ["zu", "ず", "ズ", "s"],
    ["ze", "ぜ", "ゼ", "s"],
    ["zo", "ぞ", "ゾ", "s"],
    ["da", "だ", "ダ", "t"],
    ["ji/di", "ぢ", "ヂ", "t"],
    ["zu/du", "づ", "ヅ", "t"],
    ["de", "で", "デ", "t"],
    ["do", "ど", "ド", "t"],
    ["ba", "ば", "バ", "h"],
    ["bi", "び", "ビ", "h"],
    ["bu", "ぶ", "ブ", "h"],
    ["be", "べ", "ベ", "h"],
    ["bo", "ぼ", "ボ", "h"],
    ["pa", "ぱ", "パ", "h"],
    ["pi", "ぴ", "ピ", "h"],
    ["pu", "ぷ", "プ", "h"],
    ["pe", "ぺ", "ペ", "h"],
    ["po", "ぽ", "ポ", "h"],
  ];

  const SMALL_KANA_ITEMS = [
    ["kya", "きゃ", "キャ", "k"],
    ["kyu", "きゅ", "キュ", "k"],
    ["kyo", "きょ", "キョ", "k"],
    ["sha", "しゃ", "シャ", "s"],
    ["shu", "しゅ", "シュ", "s"],
    ["sho", "しょ", "ショ", "s"],
    ["cha", "ちゃ", "チャ", "t"],
    ["chu", "ちゅ", "チュ", "t"],
    ["cho", "ちょ", "チョ", "t"],
    ["nya", "にゃ", "ニャ", "n"],
    ["nyu", "にゅ", "ニュ", "n"],
    ["nyo", "にょ", "ニョ", "n"],
    ["hya", "ひゃ", "ヒャ", "h"],
    ["hyu", "ひゅ", "ヒュ", "h"],
    ["hyo", "ひょ", "ヒョ", "h"],
    ["mya", "みゃ", "ミャ", "m"],
    ["myu", "みゅ", "ミュ", "m"],
    ["myo", "みょ", "ミョ", "m"],
    ["rya", "りゃ", "リャ", "r"],
    ["ryu", "りゅ", "リュ", "r"],
    ["ryo", "りょ", "リョ", "r"],
    ["gya", "ぎゃ", "ギャ", "k"],
    ["gyu", "ぎゅ", "ギュ", "k"],
    ["gyo", "ぎょ", "ギョ", "k"],
    ["ja", "じゃ", "ジャ", "s"],
    ["ju", "じゅ", "ジュ", "s"],
    ["jo", "じょ", "ジョ", "s"],
    ["bya", "びゃ", "ビャ", "h"],
    ["byu", "びゅ", "ビュ", "h"],
    ["byo", "びょ", "ビョ", "h"],
    ["pya", "ぴゃ", "ピャ", "h"],
    ["pyu", "ぴゅ", "ピュ", "h"],
    ["pyo", "ぴょ", "ピョ", "h"],
  ];

  const CONFUSING_KANA_SETS = [
    {
      id: "shi-tsu",
      label: "シ / ツ",
      script: "katakana",
      items: ["shi", "tsu"],
    },
    {
      id: "so-n",
      label: "ソ / ン",
      script: "katakana",
      items: ["so", "n"],
    },
    {
      id: "no-me",
      label: "ノ / メ",
      script: "katakana",
      items: ["no", "me"],
    },
    {
      id: "nu-me",
      label: "ぬ / め",
      script: "hiragana",
      items: ["nu", "me"],
    },
    {
      id: "sa-chi",
      label: "さ / ち",
      script: "hiragana",
      items: ["sa", "chi"],
    },
    {
      id: "re-wa-ne",
      label: "れ / わ / ね",
      script: "hiragana",
      items: ["re", "wa", "ne"],
    },
  ];

  const RECOMMENDED_REVIEW_WEIGHT = 0.35;
  const REVIEW_WEIGHT_MULTIPLIER = 0.18;
  const REVIEW_STREAK_PENALTY = 0.12;
  const REVIEW_REST_BONUS = 0.18;
  const REVIEW_NEW_ITEM_BONUS = 0.08;

  function getItemKey(item) {
    return `${item.hiragana}|${item.katakana}|${item.romaji}`;
  }

  function toItem([romaji, hiragana, katakana, itemGroup], group = "base") {
    const resolvedGroup = itemGroup || group;
    return { romaji, hiragana, katakana, group: resolvedGroup };
  }

  function createRecord(item) {
    return {
      ...item,
      attempts: 0,
      correct: 0,
      misses: 0,
      masteredStreak: 0,
      lastPracticedAt: null,
    };
  }

  function getAllPracticeItems() {
    return [
      ...BASE_ROWS.flatMap((row) => row.items.map((item) => toItem(item, row.id))),
      ...DAKUTEN_ITEMS.map((item) => toItem(item, "dakuten")),
      ...SMALL_KANA_ITEMS.map((item) => toItem(item, "small")),
    ];
  }

  function getCanonicalPracticeItem(record) {
    if (!record?.hiragana || !record?.katakana || !record?.romaji) return null;
    const key = getItemKey(record);
    return getAllPracticeItems().find((item) => getItemKey(item) === key) || null;
  }

  function normalizeRecord(record, canonicalItem = null) {
    const item = canonicalItem || record;
    const rawAttempts = Math.max(Number(record.attempts) || 0, 0);
    const rawCorrect = Math.max(Number(record.correct) || 0, 0);
    const rawMisses = Math.max(Number(record.misses) || 0, 0);
    const attempts = Math.max(rawAttempts, rawCorrect + rawMisses);
    const correct = Math.min(rawCorrect, attempts);
    const misses = Math.min(rawMisses, Math.max(attempts - correct, 0));
    const masteredStreak = Math.min(
      Math.max(Number(record.masteredStreak) || 0, 0),
      correct,
    );
    return {
      romaji: item.romaji,
      hiragana: item.hiragana,
      katakana: item.katakana,
      group: item.group || "base",
      attempts,
      correct,
      misses,
      masteredStreak,
      lastPracticedAt: record.lastPracticedAt || null,
    };
  }

  function getRecordAccuracy(record) {
    const attempts = Math.max(Number(record?.attempts) || 0, 0);
    const correct = Math.max(Number(record?.correct) || 0, 0);
    return attempts === 0 ? 0 : Math.round((correct / attempts) * 100);
  }

  function isLongTermWeakRecord(record) {
    const misses = Math.max(Number(record?.misses) || 0, 0);
    if (!misses) return false;

    const accuracy = getRecordAccuracy(record);
    const masteredStreak = Math.max(Number(record?.masteredStreak) || 0, 0);
    return masteredStreak < 3 || accuracy < 75;
  }

  function normalizeAnswerText(value) {
    return String(value || "")
      .trim()
      .toLowerCase()
      .replace(/\s+/g, "")
      .replace(/[‐‑‒–—−]/g, "-");
  }

  function normalizeKanaText(value) {
    return String(value || "").trim();
  }

  function getAcceptedAnswers(question) {
    if (question.answerType === "romaji") {
      const variants = question.item.romaji.includes("/")
        ? question.item.romaji.split("/")
        : [question.item.romaji];
      return [...new Set(variants.map((variant) => normalizeAnswerText(variant)))];
    }

    return [question.item.hiragana, question.item.katakana].map(normalizeKanaText);
  }

  function isAnswerCorrect(question, userAnswer) {
    if (question.answerType === "romaji") {
      const normalized = normalizeAnswerText(userAnswer);
      return getAcceptedAnswers(question).includes(normalized);
    }

    return getAcceptedAnswers(question).includes(normalizeKanaText(userAnswer));
  }

  function getItemByRomaji(romaji) {
    return getAllPracticeItems().find((item) => item.romaji === romaji);
  }

  function getConfusingSetById(id) {
    return CONFUSING_KANA_SETS.find((set) => set.id === id) || null;
  }

  function getSelectedConfusingSets(selectedConfusingSet = "all") {
    return selectedConfusingSet === "all"
      ? CONFUSING_KANA_SETS
      : CONFUSING_KANA_SETS.filter((set) => set.id === selectedConfusingSet);
  }

  function withQuestionScript(item, script) {
    return script ? { ...item, forcedScript: script } : item;
  }

  function getConfusingPool(selectedConfusingSet = "all") {
    const sets = getSelectedConfusingSets(selectedConfusingSet);
    const keyedItems = new Map();

    sets.forEach((set) => {
      set.items.forEach((romaji) => {
        const item = getItemByRomaji(romaji);
        if (item) {
          keyedItems.set(`${set.id}:${getItemKey(item)}`, withQuestionScript(item, set.script));
        }
      });
    });

    return [...keyedItems.values()];
  }

  function getRowName(groupId) {
    if (groupId === "dakuten") return "浊音";
    if (groupId === "small") return "拗音";
    return BASE_ROWS.find((row) => row.id === groupId)?.label || groupId;
  }

  function getGroupLabel(groupId) {
    if (groupId === "dakuten") return "浊音组";
    if (groupId === "small") return "拗音组";
    return `${getRowName(groupId)}行`;
  }

  function sortByMissPriority(items) {
    return [...items].sort(
      (a, b) =>
        (b.misses || 0) - (a.misses || 0) ||
        (b.attempts || 0) - (a.attempts || 0) ||
        a.romaji.localeCompare(b.romaji),
    );
  }

  function getWeakRows(items) {
    const rows = new Map();

    items.forEach((item) => {
      const row = rows.get(item.group) || {
        id: item.group,
        label: getGroupLabel(item.group),
        misses: 0,
        count: 0,
      };
      row.misses += item.misses || 0;
      row.count += 1;
      rows.set(item.group, row);
    });

    return [...rows.values()].sort((a, b) => b.misses - a.misses || b.count - a.count);
  }

  function groupChartItemsByRow(items) {
    const rows = new Map();

    items.forEach((item) => {
      const row = rows.get(item.group) || {
        id: item.group,
        label: getGroupLabel(item.group),
        items: [],
      };
      row.items.push(item);
      rows.set(item.group, row);
    });

    return [...rows.values()];
  }

  function getChartSections() {
    const voicedDakuten = DAKUTEN_ITEMS.filter((item) => !String(item[0]).startsWith("p")).map(
      (item) => toItem(item, item[3]),
    );
    const semiDakuten = DAKUTEN_ITEMS.filter((item) => String(item[0]).startsWith("p")).map(
      (item) => toItem(item, item[3]),
    );

    return [
      {
        key: "base",
        title: "清音",
        items: BASE_ROWS.flatMap((row) => row.items.map((item) => toItem(item, row.id))),
      },
      {
        key: "dakuten",
        title: "浊音",
        items: voicedDakuten,
      },
      {
        key: "semi",
        title: "半浊音",
        items: semiDakuten,
      },
      {
        key: "small",
        title: "拗音",
        items: SMALL_KANA_ITEMS.map((item) => toItem(item, "small")),
      },
    ];
  }

  function calculateReviewWeight(item, record, now = Date.now()) {
    if (!record) {
      return 1 + REVIEW_NEW_ITEM_BONUS;
    }

    const attempts = Math.max(Number(record.attempts) || 0, 1);
    const misses = Math.max(Number(record.misses) || 0, 0);
    const masteredStreak = Math.max(Number(record.masteredStreak) || 0, 0);
    const missRate = misses / attempts;
    const effectiveMisses = misses / (1 + masteredStreak * 0.45);
    const missesWeight =
      1 + Math.min(effectiveMisses * REVIEW_WEIGHT_MULTIPLIER, 0.75) + missRate * 0.55;
    const streakPenalty = Math.min(masteredStreak * REVIEW_STREAK_PENALTY, 0.7);
    const lastPracticedTime = record.lastPracticedAt ? Date.parse(record.lastPracticedAt) : NaN;
    const restDays = Number.isNaN(lastPracticedTime)
      ? 0
      : Math.max((now - lastPracticedTime) / 86400000, 0);
    const restBonus = Math.min(restDays * REVIEW_REST_BONUS, 0.75);

    const weight = missesWeight + restBonus - streakPenalty;
    return Math.max(weight, RECOMMENDED_REVIEW_WEIGHT);
  }

  function shuffle(items, random = Math.random) {
    const result = [...items];
    for (let index = result.length - 1; index > 0; index -= 1) {
      const swapIndex = Math.floor(random() * (index + 1));
      [result[index], result[swapIndex]] = [result[swapIndex], result[index]];
    }
    return result;
  }

  return {
    BASE_ROWS,
    DAKUTEN_ITEMS,
    SMALL_KANA_ITEMS,
    CONFUSING_KANA_SETS,
    calculateReviewWeight,
    createRecord,
    getAcceptedAnswers,
    getAllPracticeItems,
    getCanonicalPracticeItem,
    getChartSections,
    getConfusingPool,
    getConfusingSetById,
    getGroupLabel,
    getItemByRomaji,
    getItemKey,
    getRecordAccuracy,
    getSelectedConfusingSets,
    getWeakRows,
    groupChartItemsByRow,
    isAnswerCorrect,
    isLongTermWeakRecord,
    normalizeAnswerText,
    normalizeKanaText,
    normalizeRecord,
    shuffle,
    sortByMissPriority,
    toItem,
    withQuestionScript,
  };
});
