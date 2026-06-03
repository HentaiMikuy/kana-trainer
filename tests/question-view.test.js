const assert = require("node:assert/strict");
const questionView = require("../question-view.js");

function test(name, fn) {
  try {
    fn();
    console.log(`ok - ${name}`);
  } catch (error) {
    console.error(`not ok - ${name}`);
    throw error;
  }
}

class FakeClassList {
  constructor(element) {
    this.element = element;
  }

  add(...names) {
    const classes = new Set(this.element.className.split(/\s+/).filter(Boolean));
    names.forEach((name) => classes.add(name));
    this.element.className = [...classes].join(" ");
  }

  remove(...names) {
    const classes = new Set(this.element.className.split(/\s+/).filter(Boolean));
    names.forEach((name) => classes.delete(name));
    this.element.className = [...classes].join(" ");
  }

  contains(name) {
    return this.element.className.split(/\s+/).filter(Boolean).includes(name);
  }

  toggle(name, force) {
    const shouldAdd = force === undefined ? !this.contains(name) : Boolean(force);
    if (shouldAdd) {
      this.add(name);
    } else {
      this.remove(name);
    }
    return shouldAdd;
  }
}

class FakeElement {
  constructor(tagName = "div") {
    this.tagName = tagName.toUpperCase();
    this.attributes = new Map();
    this.children = [];
    this.className = "";
    this.disabled = false;
    this.eventListeners = new Map();
    this.focusCount = 0;
    this.selectCount = 0;
    this.style = {};
    this.textContent = "";
    this.title = "";
    this.type = "";
    this.value = "";
  }

  append(...nodes) {
    this.children.push(...nodes);
  }

  addEventListener(type, listener) {
    this.eventListeners.set(type, listener);
  }

  setAttribute(name, value) {
    this.attributes.set(name, String(value));
  }

  getAttribute(name) {
    return this.attributes.get(name) || null;
  }

  focus() {
    this.focusCount += 1;
  }

  select() {
    this.selectCount += 1;
  }

  get classList() {
    return new FakeClassList(this);
  }

  set innerHTML(value) {
    this.children = [];
    this.textContent = value;
  }
}

function createElements() {
  return {
    actionRow: new FakeElement("div"),
    answerInput: new FakeElement("input"),
    feedback: new FakeElement("div"),
    inputAnswerArea: new FakeElement("div"),
    nextButton: new FakeElement("button"),
    optionsGrid: new FakeElement("div"),
    progressBar: new FakeElement("span"),
    promptSub: new FakeElement("div"),
    promptText: new FakeElement("div"),
    questionCounter: new FakeElement("span"),
    quizModeLabel: new FakeElement("span"),
    resultReport: new FakeElement("div"),
    revealButton: new FakeElement("button"),
    speakButton: new FakeElement("button"),
    submitInputButton: new FakeElement("button"),
    timerStrip: new FakeElement("div"),
  };
}

function createQuestion(overrides = {}) {
  return {
    answer: "shi",
    answerType: "romaji",
    helper: "选择对应的罗马音",
    prompt: "シ",
    ...overrides,
  };
}

test("renders empty question state", () => {
  const elements = createElements();

  questionView.renderQuestion(elements, {
    answerMode: "choice",
    canSpeak: false,
    challengeEnabled: false,
    current: null,
    currentIndex: 0,
    optionValues: [],
    questionLimit: 20,
    quizModeLabel: "片假名",
  });

  assert.equal(elements.promptText.textContent, "無");
  assert.equal(elements.promptText.classList.contains("kana-text"), true);
  assert.equal(elements.promptSub.textContent, "当前范围没有可练习的假名");
  assert.equal(elements.optionsGrid.children.length, 0);
  assert.equal(elements.inputAnswerArea.classList.contains("hidden"), true);
});

test("renders choice question and delegates option clicks", () => {
  const elements = createElements();
  const chosen = [];

  questionView.renderQuestion(elements, {
    answerMode: "choice",
    canSpeak: true,
    challengeEnabled: true,
    createElement: (tagName) => new FakeElement(tagName),
    current: createQuestion(),
    currentIndex: 1,
    onChooseAnswer(answer, button) {
      chosen.push([answer, button]);
    },
    optionValues: ["shi", "tsu"],
    questionLimit: 20,
    quizModeLabel: "片假名",
  });

  assert.equal(elements.quizModeLabel.textContent, "片假名");
  assert.equal(elements.questionCounter.textContent, "第 2 / 20 题");
  assert.equal(elements.promptText.textContent, "シ");
  assert.equal(elements.promptText.classList.contains("kana-text"), true);
  assert.equal(elements.speakButton.disabled, false);
  assert.equal(elements.nextButton.disabled, true);
  assert.equal(elements.optionsGrid.children.length, 2);
  assert.equal(elements.optionsGrid.children[0].textContent, "shi");
  assert.equal(elements.inputAnswerArea.classList.contains("hidden"), true);

  elements.optionsGrid.children[1].eventListeners.get("click")();
  assert.equal(chosen[0][0], "tsu");
  assert.equal(chosen[0][1], elements.optionsGrid.children[1]);
});

test("renders input question and focuses the answer input", () => {
  const elements = createElements();
  let animationRequested = false;

  questionView.renderQuestion(elements, {
    answerMode: "input",
    canSpeak: false,
    challengeEnabled: false,
    createElement: (tagName) => new FakeElement(tagName),
    current: createQuestion({
      answer: "シ",
      answerType: "kana",
      prompt: "shi",
    }),
    currentIndex: 0,
    optionValues: ["シ", "ツ"],
    questionLimit: 10,
    quizModeLabel: "片假名",
    requestAnimationFrame(callback) {
      animationRequested = true;
      callback();
    },
  });

  assert.equal(elements.optionsGrid.classList.contains("hidden"), true);
  assert.equal(elements.inputAnswerArea.classList.contains("hidden"), false);
  assert.equal(elements.answerInput.placeholder, "输入假名");
  assert.equal(elements.answerInput.focusCount, 1);
  assert.equal(elements.answerInput.selectCount, 1);
  assert.equal(animationRequested, true);
});

test("marks answer results for choice and input modes", () => {
  const elements = createElements();
  const correctButton = new FakeElement("button");
  const wrongButton = new FakeElement("button");
  correctButton.textContent = "shi";
  wrongButton.textContent = "tsu";
  elements.optionsGrid.children = [correctButton, wrongButton];

  questionView.showAnswerResult(elements, {
    answerMode: "choice",
    current: createQuestion(),
    isCorrect: false,
    selectedButton: wrongButton,
  });

  assert.equal(correctButton.disabled, true);
  assert.equal(wrongButton.disabled, true);
  assert.equal(correctButton.classList.contains("correct"), true);
  assert.equal(wrongButton.classList.contains("wrong"), true);
  assert.equal(elements.nextButton.disabled, false);

  questionView.showAnswerResult(elements, {
    answerMode: "input",
    current: createQuestion(),
    isCorrect: true,
  });

  assert.equal(elements.answerInput.disabled, true);
  assert.equal(elements.submitInputButton.disabled, true);
  assert.equal(elements.answerInput.classList.contains("correct"), true);
});

test("renders completion shell", () => {
  const elements = createElements();

  questionView.renderCompletionShell(elements, {
    answered: 20,
    correct: 18,
  });

  assert.equal(elements.progressBar.style.width, "100%");
  assert.equal(elements.promptText.textContent, "完成");
  assert.equal(elements.promptSub.textContent, "本轮完成：18 / 20 题正确");
  assert.equal(elements.inputAnswerArea.classList.contains("hidden"), true);
  assert.equal(elements.timerStrip.classList.contains("hidden"), true);
  assert.equal(elements.actionRow.classList.contains("hidden"), true);
  assert.equal(elements.nextButton.disabled, true);
  assert.equal(elements.speakButton.disabled, true);
});

