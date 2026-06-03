const assert = require("node:assert/strict");
const appEvents = require("../app-events.js");

function test(name, fn) {
  try {
    fn();
    console.log(`ok - ${name}`);
  } catch (error) {
    console.error(`not ok - ${name}`);
    throw error;
  }
}

class FakeElement {
  constructor(dataset = {}) {
    this.dataset = dataset;
    this.disabled = false;
    this.eventListeners = new Map();
    this.files = [];
    this.value = "";
    this.checked = false;
    this.children = [];
    this.clickCount = 0;
    this.closestSelector = null;
  }

  addEventListener(type, listener) {
    this.eventListeners.set(type, listener);
  }

  click() {
    this.clickCount += 1;
    this.eventListeners.get("click")?.({ target: this });
  }

  closest(selector) {
    return this.closestSelector === selector ? this : null;
  }
}

function createElements() {
  return {
    answerInput: new FakeElement(),
    answerModeButtons: [new FakeElement({ answerMode: "choice" }), new FakeElement({ answerMode: "input" })],
    challengeLimit: new FakeElement(),
    challengeToggle: new FakeElement(),
    clearRecordsButton: new FakeElement(),
    confusingSetButtons: [new FakeElement({ confusingSet: "all" }), new FakeElement({ confusingSet: "shi-tsu" })],
    dakutenToggle: new FakeElement(),
    directionSelect: new FakeElement(),
    exportDataButton: new FakeElement(),
    glyphButtons: [new FakeElement({ glyph: "print" }), new FakeElement({ glyph: "hand" })],
    importDataButton: new FakeElement(),
    importDataInput: new FakeElement(),
    modeButtons: [new FakeElement({ mode: "katakana" }), new FakeElement({ mode: "hiragana" })],
    newQuizButton: new FakeElement(),
    nextButton: new FakeElement(),
    optionsGrid: new FakeElement(),
    practiceTypeButtons: [new FakeElement({ practiceType: "normal" }), new FakeElement({ practiceType: "confusing" })],
    pronunciationButtons: [new FakeElement({ pronunciation: "manual" }), new FakeElement({ pronunciation: "auto" })],
    questionLimit: new FakeElement(),
    resetButton: new FakeElement(),
    revealButton: new FakeElement(),
    reviewMistakesButton: new FakeElement(),
    smallKanaToggle: new FakeElement(),
    speakButton: new FakeElement(),
    submitInputButton: new FakeElement(),
    toggleChartButton: new FakeElement(),
  };
}

function createCallbacks(log) {
  const names = [
    "onAnswerModeChange",
    "onChallengeEnabledChange",
    "onChallengeLimitChange",
    "onChallengeLimitInput",
    "onClearRecords",
    "onConfusingSetChange",
    "onDakutenChange",
    "onDirectionChange",
    "onExportData",
    "onGlyphChange",
    "onImportDataFile",
    "onModeChange",
    "onNextQuestion",
    "onPracticeTypeChange",
    "onPronunciationChange",
    "onQuestionLimitChange",
    "onQuestionLimitInput",
    "onResetQuiz",
    "onRevealAnswer",
    "onReviewMistakes",
    "onSmallKanaChange",
    "onSpeak",
    "onSubmitInputAnswer",
    "onToggleChart",
  ];

  return Object.fromEntries(
    names.map((name) => [
      name,
      (...args) => {
        log.push([name, ...args]);
      },
    ]),
  );
}

function createRoot() {
  return {
    Element: FakeElement,
    eventListeners: new Map(),
    addEventListener(type, listener) {
      this.eventListeners.set(type, listener);
    },
  };
}

function trigger(element, type, event = { target: element }) {
  element.eventListeners.get(type)(event);
}

function createKeyEvent(key, target) {
  return {
    key,
    preventDefaultCount: 0,
    target,
    preventDefault() {
      this.preventDefaultCount += 1;
    },
  };
}

test("binds control changes to typed callbacks", () => {
  const elements = createElements();
  const root = createRoot();
  const log = [];

  appEvents.bindAppEvents({
    callbacks: createCallbacks(log),
    elements,
    getState: () => ({ answerMode: "choice", selectedAnswer: false }),
    root,
  });

  elements.modeButtons[1].click();
  elements.glyphButtons[1].click();
  elements.answerModeButtons[1].click();
  elements.practiceTypeButtons[1].click();
  elements.confusingSetButtons[1].click();

  elements.directionSelect.value = "romaji-to-kana";
  trigger(elements.directionSelect, "change");

  elements.questionLimit.value = "35";
  trigger(elements.questionLimit, "input");
  trigger(elements.questionLimit, "change");

  elements.challengeToggle.checked = true;
  trigger(elements.challengeToggle, "change");

  elements.challengeLimit.value = "25";
  trigger(elements.challengeLimit, "input");
  trigger(elements.challengeLimit, "change");

  elements.dakutenToggle.checked = false;
  trigger(elements.dakutenToggle, "change");

  elements.smallKanaToggle.checked = true;
  trigger(elements.smallKanaToggle, "change");

  assert.deepEqual(log, [
    ["onModeChange", "hiragana"],
    ["onGlyphChange", "hand"],
    ["onAnswerModeChange", "input"],
    ["onPracticeTypeChange", "confusing"],
    ["onConfusingSetChange", "shi-tsu"],
    ["onDirectionChange", "romaji-to-kana"],
    ["onQuestionLimitInput", 35],
    ["onQuestionLimitChange"],
    ["onChallengeEnabledChange", true],
    ["onChallengeLimitInput", 25],
    ["onChallengeLimitChange"],
    ["onDakutenChange", false],
    ["onSmallKanaChange", true],
  ]);
});

test("binds action buttons and import file delegation", () => {
  const elements = createElements();
  const root = createRoot();
  const log = [];
  const file = { name: "records.json" };

  appEvents.bindAppEvents({
    callbacks: createCallbacks(log),
    elements,
    getState: () => ({ answerMode: "choice", selectedAnswer: false }),
    root,
  });

  elements.newQuizButton.click();
  elements.resetButton.click();
  elements.revealButton.click();
  elements.speakButton.click();
  elements.submitInputButton.click();
  elements.nextButton.click();
  elements.reviewMistakesButton.click();
  elements.clearRecordsButton.click();
  elements.exportDataButton.click();
  elements.importDataButton.click();
  elements.importDataInput.files = [file];
  trigger(elements.importDataInput, "change");
  elements.toggleChartButton.click();

  assert.equal(elements.importDataInput.clickCount, 1);
  assert.deepEqual(log, [
    ["onResetQuiz"],
    ["onResetQuiz"],
    ["onRevealAnswer"],
    ["onSpeak"],
    ["onSubmitInputAnswer"],
    ["onNextQuestion"],
    ["onReviewMistakes"],
    ["onClearRecords"],
    ["onExportData"],
    ["onImportDataFile", file],
    ["onToggleChart"],
  ]);
});

test("handles keyboard shortcuts for input and choice modes", () => {
  const elements = createElements();
  const root = createRoot();
  const log = [];
  const optionOne = new FakeElement();
  const optionTwo = new FakeElement();
  elements.optionsGrid.children = [optionOne, optionTwo];

  let state = {
    answerMode: "input",
    selectedAnswer: false,
  };

  appEvents.bindAppEvents({
    callbacks: createCallbacks(log),
    elements,
    getState: () => state,
    root,
  });

  const submitEvent = createKeyEvent("Enter", elements.answerInput);
  root.eventListeners.get("keydown")(submitEvent);

  state = {
    answerMode: "input",
    selectedAnswer: true,
  };
  elements.nextButton.disabled = false;
  const nextEvent = createKeyEvent("Enter", elements.answerInput);
  root.eventListeners.get("keydown")(nextEvent);

  state = {
    answerMode: "choice",
    selectedAnswer: false,
  };
  root.eventListeners.get("keydown")(createKeyEvent("2", elements.answerInput));

  assert.equal(submitEvent.preventDefaultCount, 1);
  assert.equal(nextEvent.preventDefaultCount, 1);
  assert.equal(optionTwo.clickCount, 1);
  assert.deepEqual(log, [
    ["onSubmitInputAnswer"],
    ["onNextQuestion"],
  ]);
});

test("blocks keyboard shortcuts inside non-answer controls", () => {
  const elements = createElements();
  const root = createRoot();
  const log = [];
  const blockedTarget = new FakeElement();
  blockedTarget.closestSelector = "button, input, select, textarea, [contenteditable='true']";

  appEvents.bindAppEvents({
    callbacks: createCallbacks(log),
    elements,
    getState: () => ({ answerMode: "input", selectedAnswer: false }),
    root,
  });

  root.eventListeners.get("keydown")(createKeyEvent("Enter", blockedTarget));

  assert.deepEqual(log, []);
  assert.equal(
    appEvents.isKeyboardShortcutBlocked(blockedTarget, {
      answerInput: elements.answerInput,
      ElementConstructor: FakeElement,
    }),
    true,
  );
  assert.equal(
    appEvents.isKeyboardShortcutBlocked(elements.answerInput, {
      answerInput: elements.answerInput,
      ElementConstructor: FakeElement,
    }),
    false,
  );
});

