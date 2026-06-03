const {
  BASE_ROWS,
  DAKUTEN_ITEMS,
  SMALL_KANA_ITEMS,
  getAllPracticeItems,
  getChartSections,
  getConfusingSetById,
  getItemByRomaji,
  getItemKey,
  getRecordAccuracy,
  getWeakRows,
  groupChartItemsByRow,
  isAnswerCorrect,
  isLongTermWeakRecord,
  normalizeKanaText,
  shuffle,
  sortByMissPriority,
  toItem,
  withQuestionScript,
} = window.KanaCore;

const calculateCoreReviewWeight = window.KanaCore.calculateReviewWeight;
const getCoreConfusingPool = window.KanaCore.getConfusingPool;
const getCoreSelectedConfusingSets = window.KanaCore.getSelectedConfusingSets;
const {
  buildQuestionQueue,
  getPracticePool,
  makeOptions: makeEngineOptions,
  makeQuestion: makeEngineQuestion,
} = window.KanaQuizEngine;
const {
  applyPracticeResult,
  createStorageAdapter,
  getLongTermMistakeRecords,
  mergeLearningRecords,
} = window.KanaLearningRecords;
const {
  renderChart: renderChartUi,
  renderCompletionReport: renderCompletionReportUi,
  renderWeakList: renderWeakListUi,
  setFeedback: setFeedbackUi,
} = window.KanaUiRenderers;
const {
  canSpeak: canSpeakCore,
  speakKana: speakKanaCore,
} = window.KanaSpeech;
const {
  createChallengeTimer,
  formatSeconds,
} = window.KanaChallengeTimer;
const {
  createSettingsControls,
} = window.KanaSettingsControls;
const {
  createQuestionView,
} = window.KanaQuestionView;
const {
  bindAppEvents,
} = window.KanaAppEvents;
const {
  confirmAndClearLearningRecords,
  exportLearningRecords: exportLearningRecordsAction,
  handleImportDataFile: handleImportDataFileAction,
} = window.KanaLearningDataActions;
const {
  advanceQuestion: advanceSessionQuestion,
  applyAnswerStats,
  createChoicePool,
  getAverageAnswerTime: getSessionAverageAnswerTime,
  getCompletionStats,
  getMistakeReviewQuestionLimit,
  getNextQuestionAction,
  getStatsViewModel,
  resetRoundStats: resetSessionRoundStats,
} = window.KanaPracticeSession;
const {
  createPracticePlanner,
} = window.KanaPracticePlanner;

function getBrowserStorage() {
  try {
    return window.localStorage;
  } catch {
    return null;
  }
}

const learningStorage = createStorageAdapter(getBrowserStorage());

const state = {
  mode: "katakana",
  direction: "kana-to-romaji",
  selectedRows: new Set(BASE_ROWS.map((row) => row.id)),
  configuredQuestionLimit: 20,
  questionLimit: 20,
  includeDakuten: true,
  includeSmallKana: false,
  practiceType: "normal",
  selectedConfusingSet: "all",
  challengeEnabled: false,
  challengeLimitSeconds: 15,
  glyphStyle: "print",
  pronunciationMode: "manual",
  answerMode: "choice",
  chartScript: "katakana",
  activeSessionType: "normal",
  pool: [],
  optionPool: [],
  queue: [],
  current: null,
  currentIndex: 0,
  answered: 0,
  correct: 0,
  streak: 0,
  totalAnswerTimeMs: 0,
  timeAnsweredCount: 0,
  mistakes: new Map(),
  roundMistakes: new Map(),
  records: new Map(),
  selectedAnswer: false,
};

const practicePlanner = createPracticePlanner({
  baseRows: BASE_ROWS,
  buildQuestionQueue,
  calculateReviewWeight: calculateCoreReviewWeight,
  dakutenItems: DAKUTEN_ITEMS,
  getConfusingPool: getCoreConfusingPool,
  getConfusingSetById,
  getItemByRomaji,
  getItemKey,
  getPracticePool,
  getPracticeRecord,
  getSelectedConfusingSets: getCoreSelectedConfusingSets,
  getState() {
    return state;
  },
  makeOptions: makeEngineOptions,
  makeQuestion: makeEngineQuestion,
  shuffle,
  smallKanaItems: SMALL_KANA_ITEMS,
  toItem,
  withQuestionScript,
});

const elements = {
  answeredCount: document.querySelector("#answeredCount"),
  accuracyRate: document.querySelector("#accuracyRate"),
  streakCount: document.querySelector("#streakCount"),
  averageTimeCount: document.querySelector("#averageTimeCount"),
  progressBar: document.querySelector("#progressBar"),
  quizModeLabel: document.querySelector("#quizModeLabel"),
  questionCounter: document.querySelector("#questionCounter"),
  promptText: document.querySelector("#promptText"),
  promptSub: document.querySelector("#promptSub"),
  optionsGrid: document.querySelector("#optionsGrid"),
  inputAnswerArea: document.querySelector("#inputAnswerArea"),
  answerInput: document.querySelector("#answerInput"),
  submitInputButton: document.querySelector("#submitInputButton"),
  resultReport: document.querySelector("#resultReport"),
  feedback: document.querySelector("#feedback"),
  actionRow: document.querySelector(".action-row"),
  openHelpButton: document.querySelector("#openHelpButton"),
  closeHelpButton: document.querySelector("#closeHelpButton"),
  helpDialog: document.querySelector("#helpDialog"),
  helpDialogPanel: document.querySelector("#helpDialogPanel"),
  speakButton: document.querySelector("#speakButton"),
  nextButton: document.querySelector("#nextButton"),
  revealButton: document.querySelector("#revealButton"),
  newQuizButton: document.querySelector("#newQuizButton"),
  resetButton: document.querySelector("#resetButton"),
  modeButtons: [...document.querySelectorAll("button[data-mode]")],
  modeControl: document.querySelector("[data-mode]")?.closest(".normal-scope-control"),
  directionSelect: document.querySelector("#directionSelect"),
  questionLimit: document.querySelector("#questionLimit"),
  questionLimitValue: document.querySelector("#questionLimitValue"),
  rowPicker: document.querySelector("#rowPicker"),
  dakutenToggle: document.querySelector("#dakutenToggle"),
  smallKanaToggle: document.querySelector("#smallKanaToggle"),
  glyphButtons: [...document.querySelectorAll("button[data-glyph]")],
  pronunciationButtons: [...document.querySelectorAll("button[data-pronunciation]")],
  answerModeButtons: [...document.querySelectorAll("button[data-answer-mode]")],
  challengeToggle: document.querySelector("#challengeToggle"),
  challengeControl: document.querySelector("#challengeControl"),
  challengeLimit: document.querySelector("#challengeLimit"),
  challengeLimitValue: document.querySelector("#challengeLimitValue"),
  timerStrip: document.querySelector("#timerStrip"),
  timerRemaining: document.querySelector("#timerRemaining"),
  timerBar: document.querySelector("#timerBar"),
  confusingControl: document.querySelector("#confusingControl"),
  exportDataButton: document.querySelector("#exportDataButton"),
  importDataButton: document.querySelector("#importDataButton"),
  importDataInput: document.querySelector("#importDataInput"),
  practiceTypeButtons: [...document.querySelectorAll("[data-practice-type]")],
  confusingSetButtons: [...document.querySelectorAll("[data-confusing-set]")],
  weakList: document.querySelector("#weakList"),
  toggleWeakPrivacyButton: document.querySelector("#toggleWeakPrivacyButton"),
  reviewMistakesButton: document.querySelector("#reviewMistakesButton"),
  clearRecordsButton: document.querySelector("#clearRecordsButton"),
  kanaChart: document.querySelector("#kanaChart"),
  toggleChartPrivacyButton: document.querySelector("#toggleChartPrivacyButton"),
  toggleChartButton: document.querySelector("#toggleChartButton"),
};

const challengeTimer = createChallengeTimer({
  elements: {
    timerBar: elements.timerBar,
    timerRemaining: elements.timerRemaining,
    timerStrip: elements.timerStrip,
  },
  onRecordTime(elapsedMs) {
    state.totalAnswerTimeMs += elapsedMs;
    state.timeAnsweredCount += 1;
  },
  onTimeout: handleQuestionTimeout,
});

const settingsControls = createSettingsControls({
  baseRows: BASE_ROWS,
  elements,
  getState() {
    return state;
  },
  onToggleRow: handleToggleRow,
});

const questionView = createQuestionView({
  elements,
  requestAnimationFrame: window.requestAnimationFrame?.bind(window),
});

function assertRequiredElements() {
  const missing = Object.entries(elements)
    .filter(([, element]) => !Array.isArray(element) && !element)
    .map(([key]) => key);

  if (missing.length) {
    throw new Error(`Missing required DOM elements: ${missing.join(", ")}`);
  }
}

function loadLearningRecords() {
  state.records = learningStorage.load();
  syncMistakesFromRecords();
}

function saveLearningRecords() {
  if (!learningStorage.canStore()) return;

  if (!learningStorage.save(state.records)) {
    setFeedback("本地学习记录保存失败，当前练习仍可继续。", "feedback");
  }
}

function clearLearningRecords() {
  confirmAndClearLearningRecords({
    confirm: window.confirm.bind(window),
    onClear() {
      state.records.clear();
      state.mistakes.clear();
      state.roundMistakes.clear();
      learningStorage.clear();
      renderWeakList();
      renderChart();
      setFeedback("本地学习记录已清空。", "feedback");
    },
  });
}

function exportLearningRecords() {
  exportLearningRecordsAction(state.records);
  setFeedback("学习记录已导出。", "feedback");
}

function importLearningRecordsFromPayload(payload) {
  const result = mergeLearningRecords(state.records, payload);

  if (!result.importedCount) {
    setFeedback("没有找到可导入的学习记录。", "feedback");
    return;
  }

  state.records = result.records;
  syncMistakesFromRecords();
  saveLearningRecords();
  renderWeakList();
  renderChart();
  setFeedback(`已导入 ${result.importedCount} 条学习记录。`, "feedback");
}

async function handleImportDataFile(file) {
  await handleImportDataFileAction(file, {
    confirm: window.confirm.bind(window),
    onComplete() {
      elements.importDataInput.value = "";
    },
    onImportPayload: importLearningRecordsFromPayload,
    onInvalid() {
      setFeedback("导入失败，请选择有效的 JSON 学习数据文件。", "feedback");
    },
  });
}

function syncMistakesFromRecords() {
  state.mistakes = getLongTermMistakeRecords(state.records);
}

function rememberRoundMistake(item, answerScript = null) {
  const key = getItemKey(item);
  const existing = state.roundMistakes.get(key);
  state.roundMistakes.set(key, {
    ...item,
    answerScript: answerScript || existing?.answerScript || item.forcedScript || null,
    misses: existing ? existing.misses + 1 : 1,
  });
}

function recordPracticeResult(item, isCorrect, metadata = {}) {
  const result = applyPracticeResult(state.records, item, isCorrect);
  state.records = result.records;

  if (!isCorrect) {
    rememberRoundMistake(item, metadata.answerScript);
  }
  syncMistakesFromRecords();
  saveLearningRecords();
}

function getPracticeRecord(item) {
  return state.records.get(getItemKey(item)) || null;
}

function getItemMasteryState(item) {
  const record = getPracticeRecord(item);
  if (!record) {
    return {
      bucket: "new",
      label: "新",
      detail: "尚未练习",
    };
  }

  const attempts = Math.max(Number(record.attempts) || 0, 0);
  const misses = Math.max(Number(record.misses) || 0, 0);
  const accuracy = getRecordAccuracy(record);

  if (isLongTermWeakRecord(record)) {
    return {
      bucket: "weak",
      label: "弱",
      detail: `${accuracy}% · ${misses} 错`,
    };
  }

  if (record.masteredStreak >= 3 && accuracy >= 90) {
    return {
      bucket: "steady",
      label: "稳",
      detail: `${accuracy}% · 连对 ${record.masteredStreak}`,
    };
  }

  return {
    bucket: "practice",
    label: "练",
    detail: `${accuracy}% · ${attempts} 次`,
  };
}

function getAverageAnswerTime() {
  return getSessionAverageAnswerTime(state, { formatSeconds });
}

function stopQuestionTimer({ recordTime = false } = {}) {
  challengeTimer.stop({ recordTime });
}

function handleQuestionTimeout() {
  if (state.selectedAnswer || !state.current) return;
  setFeedback("时间到。正确答案是 ", "feedback wrong", state.current.answer);
  completeAnswer("", null, { forceIncorrect: true, keepFeedback: true });
}

function startQuestionTimer() {
  challengeTimer.start({
    current: state.current,
    enabled: state.challengeEnabled,
    limitSeconds: state.challengeLimitSeconds,
  });
}

function getQuestionLabel(question) {
  return question.answerType === "romaji" ? "罗马音" : "假名";
}

function focusAnswerInput() {
  questionView.focusAnswerInput({
    answerMode: state.answerMode,
    current: state.current,
  });
}

function submitInputAnswer() {
  if (state.answerMode !== "input" || state.selectedAnswer || !state.current) return;

  const answer = elements.answerInput.value;
  if (!normalizeKanaText(answer)) {
    setFeedback(`请输入${getQuestionLabel(state.current)}。`, "feedback");
    focusAnswerInput();
    return;
  }

  completeAnswer(answer);
}

function revealCurrentAnswer() {
  completeAnswer("", null, { forceIncorrect: true });
}

function completeAnswer(answer, selectedButton = null, options = {}) {
  if (state.selectedAnswer || !state.current) return;

  stopQuestionTimer({ recordTime: true });
  const isCorrect = options.forceIncorrect ? false : isAnswerCorrect(state.current, answer);
  applyAnswerStats(state, isCorrect);

  if (isCorrect) {
    if (!options.keepFeedback) {
      setFeedback("答对了。", "feedback right");
    }
  } else {
    if (!options.keepFeedback) {
      setFeedback("正确答案是 ", "feedback wrong", state.current.answer);
    }
  }

  recordPracticeResult(state.current.item, isCorrect, {
    answerScript: state.current.answerScript,
  });

  questionView.showAnswerResult({
    answerMode: state.answerMode,
    current: state.current,
    isCorrect,
    selectedButton,
  });
  speakAfterAnswer();
  updateStats();
  renderWeakList();
  renderChart();
}

function canSpeak() {
  return canSpeakCore(window);
}

function speakKana(item = state.current?.item, { showUnsupported = true } = {}) {
  return speakKanaCore(item, {
    root: window,
    showUnsupported,
    onUnsupported() {
      setFeedback("当前浏览器不支持语音发音。", "feedback");
    },
  });
}

function speakAfterAnswer() {
  if (state.pronunciationMode === "auto") {
    speakKana(state.current?.item, { showUnsupported: false });
  }
}

function buildQueue(pool = state.pool) {
  state.queue = practicePlanner.buildQueue(pool);
  state.currentIndex = 0;
}

function updateStats() {
  const stats = getStatsViewModel(state, { formatSeconds });
  elements.answeredCount.textContent = stats.answered;
  elements.accuracyRate.textContent = `${stats.accuracy}%`;
  elements.streakCount.textContent = stats.streak;
  elements.averageTimeCount.textContent = stats.averageTime;
  elements.progressBar.style.width = `${stats.progressPercent}%`;
}

function handleToggleRow(row) {
  if (state.selectedRows.has(row.id) && state.selectedRows.size > 1) {
    state.selectedRows.delete(row.id);
  } else {
    state.selectedRows.add(row.id);
  }
  settingsControls.renderRows();
  startQuiz(null, { resetRound: true });
}

function renderQuestion() {
  settingsControls.syncPracticeTypeControls();
  settingsControls.syncChallengeControls();
  elements.challengeControl.classList.toggle("hidden", !state.challengeEnabled);

  if (!state.queue.length) {
    state.current = null;
    questionView.renderQuestion({
      answerMode: state.answerMode,
      canSpeak: canSpeak(),
      challengeEnabled: state.challengeEnabled,
      current: null,
      currentIndex: state.currentIndex,
      onChooseAnswer: chooseAnswer,
      optionValues: [],
      questionLimit: state.questionLimit,
      quizModeLabel: practicePlanner.getPracticeLabel(),
    });
    return;
  }

  state.current = state.queue[state.currentIndex];
  state.selectedAnswer = false;

  const options = practicePlanner.makeOptions(state.current);
  questionView.renderQuestion({
    answerMode: state.answerMode,
    canSpeak: canSpeak(),
    challengeEnabled: state.challengeEnabled,
    current: state.current,
    currentIndex: state.currentIndex,
    onChooseAnswer: chooseAnswer,
    optionValues: options,
    questionLimit: state.questionLimit,
    quizModeLabel: practicePlanner.getPracticeLabel(),
  });
  startQuestionTimer();
  updateStats();
}

function startMistakeReview(items) {
  if (!items.length) {
    elements.feedback.textContent = "目前还没有错题。";
    elements.feedback.className = "feedback";
    return false;
  }

  state.questionLimit = getMistakeReviewQuestionLimit(items);
  state.pool = createChoicePool(items, {
    allPracticeItems: getAllPracticeItems(),
    getItemKey,
  });
  state.optionPool = state.pool;
  state.activeSessionType = "mistakes";
  state.currentIndex = 0;
  resetRoundStats();
  buildQueue(items);
  renderQuestion();
  return true;
}

function renderCompletionReport({ accuracy, wrongCount }) {
  const roundMistakes = sortByMissPriority([...state.roundMistakes.values()]);
  const weakRows = getWeakRows(roundMistakes);
  const longTermMistakes = sortByMissPriority([...state.mistakes.values()]);

  renderCompletionReportUi(elements.resultReport, {
    accuracy,
    averageTime: getAverageAnswerTime(),
    chartScript: state.chartScript,
    correctCount: state.correct,
    displayOptions: {
      chartScript: state.chartScript,
      fallbackScript: state.mode,
    },
    longTermMistakes,
    onResetQuiz: resetQuiz,
    onReviewLongTermMistakes: startMistakeReview,
    onReviewRoundMistakes: startMistakeReview,
    roundMistakes,
    weakRows,
    wrongCount,
  });
}

function setFeedback(text, className = "feedback", answer = "") {
  setFeedbackUi(elements.feedback, {
    answer,
    className,
    isKanaAnswer: Boolean(answer && state.current?.answerType === "kana"),
    text,
  });
}

function chooseAnswer(answer, selectedButton) {
  completeAnswer(answer, selectedButton);
}

function revealAnswer() {
  if (state.selectedAnswer || !state.current) return;
  revealCurrentAnswer();
}

function nextQuestion() {
  const action = getNextQuestionAction(state);

  if (action === "blocked") return;
  if (action === "finish") {
    finishQuiz();
    return;
  }

  advanceSessionQuestion(state);
  renderQuestion();
}

function finishQuiz() {
  stopQuestionTimer();
  const { accuracy, wrongCount } = getCompletionStats(state);
  questionView.renderCompletionShell({
    answered: state.answered,
    correct: state.correct,
  });
  renderCompletionReport({ accuracy, wrongCount });
}

function renderWeakList() {
  const mistakes = [...state.mistakes.values()]
    .sort((a, b) => b.misses - a.misses || b.attempts - a.attempts)
    .slice(0, 8);

  renderWeakListUi(elements.weakList, {
    displayOptions: {
      chartScript: state.chartScript,
      fallbackScript: state.mode,
    },
    getRecordAccuracy,
    mistakes,
  });
}

function renderChart() {
  renderChartUi({
    chartScript: state.chartScript,
    getMasteryState: getItemMasteryState,
    groupChartItemsByRow,
    onSpeak: speakKana,
    sections: getChartSections(),
    target: elements.kanaChart,
    toggleButton: elements.toggleChartButton,
  });
}

function togglePanelPrivacy(target, button) {
  const isBlurred = !target.classList.contains("privacy-blurred");
  target.classList.toggle("privacy-blurred", isBlurred);
  target.inert = isBlurred;
  target.setAttribute("aria-hidden", String(isBlurred));
  button.textContent = isBlurred ? "显示" : "遮挡";
  button.setAttribute("aria-pressed", String(isBlurred));
}

function openHelpDialog() {
  elements.helpDialog.classList.remove("hidden");
  elements.openHelpButton.setAttribute("aria-expanded", "true");
  elements.helpDialogPanel.focus();
}

function closeHelpDialog() {
  elements.helpDialog.classList.add("hidden");
  elements.openHelpButton.setAttribute("aria-expanded", "false");
  elements.openHelpButton.focus();
}

function resetRoundStats() {
  stopQuestionTimer();
  resetSessionRoundStats(state);
  updateStats();
}

function startQuiz(poolOverride = null, options = {}) {
  if (options.resetRound) {
    resetRoundStats();
  }

  settingsControls.syncPracticeTypeControls();
  settingsControls.syncChallengeControls();
  state.pool = poolOverride || practicePlanner.getActivePool();
  state.optionPool = state.practiceType === "confusing" ? getAllPracticeItems() : state.pool;
  state.activeSessionType = state.practiceType;
  state.questionLimit = state.configuredQuestionLimit;
  elements.questionLimit.value = state.configuredQuestionLimit;
  elements.questionLimitValue.textContent = state.configuredQuestionLimit;

  if (!state.pool.length) return;

  buildQueue(state.pool);
  state.currentIndex = 0;
  renderQuestion();
}

function resetQuiz() {
  resetRoundStats();
  startQuiz();
  renderWeakList();
}

function bindEvents() {
  bindAppEvents({
    elements,
    getState() {
      return state;
    },
    root: window,
    callbacks: {
      onModeChange(mode) {
        state.mode = mode;
        settingsControls.syncModeControls();
        startQuiz(null, { resetRound: true });
      },
      onGlyphChange(glyphStyle) {
        state.glyphStyle = glyphStyle;
        settingsControls.syncGlyphControls();
      },
      onPronunciationChange(pronunciationMode) {
        state.pronunciationMode = pronunciationMode;
        settingsControls.syncPronunciationControls();
      },
      onAnswerModeChange(answerMode) {
        state.answerMode = answerMode;
        settingsControls.syncAnswerModeControls();
        startQuiz(null, { resetRound: true });
      },
      onPracticeTypeChange(practiceType) {
        state.practiceType = practiceType;
        settingsControls.syncPracticeTypeControls();
        startQuiz(null, { resetRound: true });
      },
      onConfusingSetChange(confusingSet) {
        state.selectedConfusingSet = confusingSet;
        settingsControls.syncPracticeTypeControls();
        startQuiz(null, { resetRound: true });
      },
      onDirectionChange(direction) {
        state.direction = direction;
        startQuiz(null, { resetRound: true });
      },
      onQuestionLimitInput(questionLimit) {
        state.configuredQuestionLimit = questionLimit;
        elements.questionLimitValue.textContent = state.configuredQuestionLimit;
      },
      onQuestionLimitChange() {
        startQuiz(null, { resetRound: true });
      },
      onChallengeEnabledChange(challengeEnabled) {
        state.challengeEnabled = challengeEnabled;
        settingsControls.syncChallengeControls();
        startQuiz(null, { resetRound: true });
      },
      onChallengeLimitInput(limitSeconds) {
        state.challengeLimitSeconds = limitSeconds;
        settingsControls.syncChallengeControls();
      },
      onChallengeLimitChange() {
        startQuiz(null, { resetRound: true });
      },
      onDakutenChange(includeDakuten) {
        state.includeDakuten = includeDakuten;
        startQuiz(null, { resetRound: true });
      },
      onSmallKanaChange(includeSmallKana) {
        state.includeSmallKana = includeSmallKana;
        startQuiz(null, { resetRound: true });
      },
      onResetQuiz: resetQuiz,
      onRevealAnswer: revealAnswer,
      onSpeak() {
        speakKana();
      },
      onSubmitInputAnswer: submitInputAnswer,
      onNextQuestion: nextQuestion,
      onReviewMistakes() {
        startMistakeReview([...state.mistakes.values()]);
      },
      onClearRecords: clearLearningRecords,
      onExportData: exportLearningRecords,
      onImportDataFile: handleImportDataFile,
      onOpenHelp: openHelpDialog,
      onCloseHelp: closeHelpDialog,
      onToggleWeakPrivacy() {
        togglePanelPrivacy(elements.weakList, elements.toggleWeakPrivacyButton);
      },
      onToggleChartPrivacy() {
        togglePanelPrivacy(elements.kanaChart, elements.toggleChartPrivacyButton);
      },
      onToggleChart() {
        state.chartScript = state.chartScript === "katakana" ? "hiragana" : "katakana";
        renderChart();
      },
    },
  });
}

function init() {
  assertRequiredElements();
  loadLearningRecords();
  settingsControls.renderRows();
  renderWeakList();
  renderChart();
  settingsControls.syncModeControls();
  settingsControls.syncPracticeTypeControls();
  settingsControls.syncGlyphControls();
  settingsControls.syncPronunciationControls();
  settingsControls.syncAnswerModeControls();
  settingsControls.syncChallengeControls();
  bindEvents();
  startQuiz();
}

init();
