const assert = require("node:assert/strict");
const ui = require("../ui-renderers.js");

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
}

class FakeElement {
  constructor(tagName) {
    this.tagName = tagName.toUpperCase();
    this.attributes = new Map();
    this.children = [];
    this.className = "";
    this.dataset = {};
    this.eventListeners = new Map();
    this.textContent = "";
    this.type = "";
  }

  append(...nodes) {
    nodes.forEach((node) => {
      this.children.push(typeof node === "string" ? new FakeTextNode(node) : node);
    });
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

  get classList() {
    return new FakeClassList(this);
  }

  set innerHTML(value) {
    this.children = [];
    this.textContent = value;
  }
}

class FakeTextNode {
  constructor(text) {
    this.textContent = text;
  }
}

function installFakeDocument() {
  global.document = {
    createElement(tagName) {
      return new FakeElement(tagName);
    },
    createTextNode(text) {
      return new FakeTextNode(text);
    },
  };
}

test("renders kana answers inside feedback spans", () => {
  installFakeDocument();
  const target = new FakeElement("div");

  ui.setFeedback(target, {
    answer: "シ",
    className: "feedback wrong",
    isKanaAnswer: true,
    text: "正确答案是 ",
  });

  assert.equal(target.className, "feedback wrong");
  assert.equal(target.children[0].textContent, "正确答案是 ");
  assert.equal(target.children[1].tagName, "SPAN");
  assert.equal(target.children[1].className, "kana-text");
  assert.equal(target.children[1].textContent, "シ");
});

test("renders weak-list empty and populated states", () => {
  installFakeDocument();
  const target = new FakeElement("div");

  ui.renderWeakList(target, {
    getRecordAccuracy: () => 50,
    mistakes: [],
  });

  assert.equal(target.children.length, 1);
  assert.equal(target.children[0].className, "empty-state");

  ui.renderWeakList(target, {
    getRecordAccuracy: () => 50,
    mistakes: [
      {
        romaji: "shi",
        hiragana: "し",
        katakana: "シ",
        misses: 2,
      },
    ],
  });

  assert.equal(target.children.length, 1);
  assert.equal(target.children[0].className, "weak-card");
  assert.equal(target.children[0].children[0].textContent, "シ");
});

test("renders chart cells with mastery metadata and click handlers", () => {
  installFakeDocument();
  const target = new FakeElement("div");
  const toggleButton = new FakeElement("button");
  const item = {
    romaji: "shi",
    hiragana: "し",
    katakana: "シ",
    group: "s",
  };
  let spoken = null;

  ui.renderChart({
    chartScript: "katakana",
    getMasteryState: () => ({
      bucket: "weak",
      detail: "50% · 2 错",
      label: "弱",
    }),
    groupChartItemsByRow: (items) => [
      {
        id: "s",
        label: "さ行",
        items,
      },
    ],
    onSpeak: (clickedItem) => {
      spoken = clickedItem;
    },
    sections: [
      {
        key: "base",
        title: "清音",
        items: [item],
      },
    ],
    target,
    toggleButton,
  });

  const cell = target.children[0].children[1].children[0].children[1].children[0];
  assert.equal(toggleButton.textContent, "片假名");
  assert.equal(cell.dataset.mastery, "weak");
  assert.equal(cell.getAttribute("aria-label"), "播放 シ 的发音，罗马音 shi，50% · 2 错");

  cell.eventListeners.get("click")();
  assert.equal(spoken, item);
});
