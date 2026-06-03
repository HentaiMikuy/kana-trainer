const assert = require("node:assert/strict");
const timerModule = require("../challenge-timer.js");

function test(name, fn) {
  try {
    fn();
    console.log(`ok - ${name}`);
  } catch (error) {
    console.error(`not ok - ${name}`);
    throw error;
  }
}

function createFakeElement() {
  const classes = new Set();

  return {
    classList: {
      contains(name) {
        return classes.has(name);
      },
      toggle(name, force) {
        if (force) {
          classes.add(name);
        } else {
          classes.delete(name);
        }
        return classes.has(name);
      },
    },
    style: {},
    textContent: "",
  };
}

function createTimerElements() {
  return {
    timerBar: createFakeElement(),
    timerRemaining: createFakeElement(),
    timerStrip: createFakeElement(),
  };
}

test("formats seconds and updates the timer display", () => {
  const elements = createTimerElements();

  assert.equal(timerModule.formatSeconds(-1), "0.0s");

  timerModule.updateTimerDisplay(elements, 2500, 10000);
  assert.equal(elements.timerRemaining.textContent, "2.5s");
  assert.equal(elements.timerBar.style.width, "25%");
  assert.equal(elements.timerBar.classList.contains("danger"), true);

  timerModule.updateTimerDisplay(elements, 12000, 10000);
  assert.equal(elements.timerBar.style.width, "100%");
  assert.equal(elements.timerBar.classList.contains("danger"), false);
});

test("starts disabled timers without scheduling an interval", () => {
  const elements = createTimerElements();
  let scheduledCount = 0;
  const timer = timerModule.createChallengeTimer({
    elements,
    now: () => 1000,
    setIntervalFn() {
      scheduledCount += 1;
      return 1;
    },
  });

  timer.start({
    current: { answer: "a" },
    enabled: false,
    limitSeconds: 15,
  });

  assert.equal(scheduledCount, 0);
  assert.equal(elements.timerRemaining.textContent, "15.0s");
  assert.equal(elements.timerStrip.classList.contains("hidden"), true);
});

test("records elapsed time and clears interval id zero on stop", () => {
  const elements = createTimerElements();
  const cleared = [];
  const recorded = [];
  let currentTime = 1000;

  const timer = timerModule.createChallengeTimer({
    clearIntervalFn(id) {
      cleared.push(id);
    },
    elements,
    now: () => currentTime,
    onRecordTime(elapsedMs) {
      recorded.push(elapsedMs);
    },
    setIntervalFn() {
      return 0;
    },
  });

  timer.start({
    current: { answer: "a" },
    enabled: true,
    limitSeconds: 2,
  });

  currentTime = 1350;
  timer.stop({ recordTime: true });

  assert.deepEqual(cleared, [0]);
  assert.deepEqual(recorded, [350]);
  assert.equal(elements.timerStrip.classList.contains("hidden"), false);
});

test("records elapsed time and calls timeout when time runs out", () => {
  const elements = createTimerElements();
  const cleared = [];
  const recorded = [];
  let currentTime = 0;
  let intervalCallback = null;
  let timeoutCount = 0;

  const timer = timerModule.createChallengeTimer({
    clearIntervalFn(id) {
      cleared.push(id);
    },
    elements,
    now: () => currentTime,
    onRecordTime(elapsedMs) {
      recorded.push(elapsedMs);
    },
    onTimeout() {
      timeoutCount += 1;
    },
    setIntervalFn(callback) {
      intervalCallback = callback;
      return 7;
    },
  });

  timer.start({
    current: { answer: "a" },
    enabled: true,
    limitSeconds: 1,
  });

  currentTime = 1100;
  intervalCallback();

  assert.deepEqual(cleared, [7]);
  assert.deepEqual(recorded, [1100]);
  assert.equal(timeoutCount, 1);
  assert.equal(elements.timerRemaining.textContent, "0.0s");
  assert.equal(elements.timerBar.style.width, "0%");
  assert.equal(elements.timerBar.classList.contains("danger"), true);
});

