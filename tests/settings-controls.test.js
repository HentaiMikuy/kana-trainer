const assert = require("node:assert/strict");
const settings = require("../settings-controls.js");

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

  add(name) {
    const classes = new Set(this.element.className.split(/\s+/).filter(Boolean));
    classes.add(name);
    this.element.className = [...classes].join(" ");
  }

  remove(name) {
    const classes = new Set(this.element.className.split(/\s+/).filter(Boolean));
    classes.delete(name);
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
    this.dataset = {};
    this.eventListeners = new Map();
    this.parentBySelector = new Map();
    this.textContent = "";
    this.type = "";
    this.value = "";
    this.checked = false;
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

  closest(selector) {
    return this.parentBySelector.get(selector) || null;
  }

  get classList() {
    return new FakeClassList(this);
  }

  set innerHTML(value) {
    this.children = [];
    this.textContent = value;
  }
}

function createButton(dataset = {}) {
  const button = new FakeElement("button");
  button.dataset = dataset;
  return button;
}

function createElements() {
  const rowPicker = new FakeElement("div");
  const questionLimit = new FakeElement("input");
  const rowPickerScope = new FakeElement("div");
  const dakutenScope = new FakeElement("div");
  const smallKanaScope = new FakeElement("div");
  const questionLimitGroup = new FakeElement("div");
  const dakutenToggle = new FakeElement("input");
  const smallKanaToggle = new FakeElement("input");

  questionLimit.parentBySelector.set(".control-group", questionLimitGroup);
  rowPicker.parentBySelector.set(".normal-scope-control", rowPickerScope);
  dakutenToggle.parentBySelector.set(".normal-scope-control", dakutenScope);
  smallKanaToggle.parentBySelector.set(".normal-scope-control", smallKanaScope);

  return {
    answerModeButtons: [createButton({ answerMode: "choice" }), createButton({ answerMode: "input" })],
    challengeLimit: new FakeElement("input"),
    challengeLimitValue: new FakeElement("span"),
    challengeToggle: new FakeElement("input"),
    confusingControl: new FakeElement("div"),
    confusingSetButtons: [createButton({ confusingSet: "all" }), createButton({ confusingSet: "shi-tsu" })],
    dakutenToggle,
    glyphButtons: [createButton({ glyph: "print" }), createButton({ glyph: "hand" })],
    modeButtons: [createButton({ mode: "katakana" }), createButton({ mode: "hiragana" })],
    modeControl: new FakeElement("div"),
    practiceTypeButtons: [createButton({ practiceType: "normal" }), createButton({ practiceType: "confusing" })],
    pronunciationButtons: [
      createButton({ pronunciation: "manual" }),
      createButton({ pronunciation: "auto" }),
    ],
    questionLimit,
    rowPicker,
    smallKanaToggle,
    testParents: {
      dakutenScope,
      questionLimitGroup,
      rowPickerScope,
      smallKanaScope,
    },
  };
}

test("syncs active state and aria-pressed on segmented controls", () => {
  const buttons = [createButton({ mode: "katakana" }), createButton({ mode: "hiragana" })];

  settings.syncPressedButtons(buttons, (button) => button.dataset.mode === "hiragana");

  assert.equal(buttons[0].classList.contains("active"), false);
  assert.equal(buttons[0].getAttribute("aria-pressed"), "false");
  assert.equal(buttons[1].classList.contains("active"), true);
  assert.equal(buttons[1].getAttribute("aria-pressed"), "true");
});

test("syncs challenge controls and glyph document state", () => {
  const elements = createElements();
  const documentElement = { dataset: {} };

  settings.syncChallengeControls(elements, {
    challengeEnabled: true,
    challengeLimitSeconds: 20,
  });
  settings.syncGlyphControls(
    elements,
    {
      glyphStyle: "hand",
    },
    { documentElement },
  );

  assert.equal(elements.challengeToggle.checked, true);
  assert.equal(elements.challengeLimit.value, 20);
  assert.equal(elements.challengeLimitValue.textContent, 20);
  assert.equal(documentElement.dataset.glyph, "hand");
  assert.equal(elements.glyphButtons[1].classList.contains("active"), true);
});

test("syncs practice type visibility and selected confusing set", () => {
  const elements = createElements();

  settings.syncPracticeTypeControls(elements, {
    practiceType: "confusing",
    selectedConfusingSet: "shi-tsu",
  });

  assert.equal(elements.confusingControl.classList.contains("hidden"), false);
  assert.equal(elements.modeControl.classList.contains("hidden"), true);
  assert.equal(elements.testParents.rowPickerScope.classList.contains("hidden"), true);
  assert.equal(elements.testParents.dakutenScope.classList.contains("hidden"), true);
  assert.equal(elements.testParents.smallKanaScope.classList.contains("hidden"), true);
  assert.equal(elements.practiceTypeButtons[1].classList.contains("active"), true);
  assert.equal(elements.confusingSetButtons[1].getAttribute("aria-pressed"), "true");
});

test("renders selected rows and delegates row clicks", () => {
  const rowPicker = new FakeElement("div");
  const clickedRows = [];

  settings.renderRows({
    baseRows: [
      { id: "a", label: "あ行" },
      { id: "k", label: "か行" },
    ],
    createElement: (tagName) => new FakeElement(tagName),
    onToggleRow(row) {
      clickedRows.push(row.id);
    },
    rowPicker,
    selectedRows: new Set(["k"]),
  });

  assert.equal(rowPicker.children.length, 2);
  assert.equal(rowPicker.children[0].textContent, "あ行");
  assert.equal(rowPicker.children[0].classList.contains("active"), false);
  assert.equal(rowPicker.children[1].classList.contains("active"), true);
  assert.equal(rowPicker.children[1].getAttribute("aria-pressed"), "true");

  rowPicker.children[1].eventListeners.get("click")();
  assert.deepEqual(clickedRows, ["k"]);
});

test("creates a settings controller backed by current state", () => {
  const elements = createElements();
  const documentElement = { dataset: {} };
  const state = {
    answerMode: "input",
    challengeEnabled: false,
    challengeLimitSeconds: 15,
    glyphStyle: "print",
    mode: "hiragana",
    practiceType: "normal",
    pronunciationMode: "auto",
    selectedConfusingSet: "all",
    selectedRows: new Set(["a"]),
  };

  const controls = settings.createSettingsControls({
    baseRows: [{ id: "a", label: "あ行" }],
    createElement: (tagName) => new FakeElement(tagName),
    documentElement,
    elements,
    getState: () => state,
    onToggleRow() {},
  });

  controls.renderRows();
  controls.syncAnswerModeControls();
  controls.syncChallengeControls();
  controls.syncGlyphControls();
  controls.syncModeControls();
  controls.syncPracticeTypeControls();
  controls.syncPronunciationControls();

  assert.equal(elements.rowPicker.children.length, 1);
  assert.equal(elements.answerModeButtons[1].classList.contains("active"), true);
  assert.equal(elements.challengeToggle.checked, false);
  assert.equal(documentElement.dataset.glyph, "print");
  assert.equal(elements.modeButtons[1].classList.contains("active"), true);
  assert.equal(elements.practiceTypeButtons[0].classList.contains("active"), true);
  assert.equal(elements.pronunciationButtons[1].classList.contains("active"), true);
});
