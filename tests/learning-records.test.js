const assert = require("node:assert/strict");
const core = require("../kana-core.js");
const records = require("../learning-records.js");

function test(name, fn) {
  try {
    fn();
    console.log(`ok - ${name}`);
  } catch (error) {
    console.error(`not ok - ${name}`);
    throw error;
  }
}

function createMemoryStorage() {
  const data = new Map();
  return {
    getItem(key) {
      return data.has(key) ? data.get(key) : null;
    },
    removeItem(key) {
      data.delete(key);
    },
    setItem(key, value) {
      data.set(key, String(value));
    },
  };
}

test("normalizes import payloads and drops unknown records", () => {
  const payload = {
    records: [
      {
        romaji: "shi",
        hiragana: "し",
        katakana: "シ",
        group: "wrong",
        attempts: 1,
        correct: 2,
        misses: 1,
        masteredStreak: 9,
      },
      {
        romaji: "missing",
        hiragana: "x",
        katakana: "x",
      },
    ],
  };
  const normalized = records.normalizeLearningRecords(payload);

  assert.equal(normalized.length, 1);
  assert.equal(normalized[0].group, "s");
  assert.equal(normalized[0].attempts, 3);
  assert.equal(normalized[0].masteredStreak, 2);
});

test("merges imported records over existing records", () => {
  const item = core.getItemByRomaji("shi");
  const existing = core.normalizeRecord({
    ...item,
    attempts: 1,
    correct: 1,
    misses: 0,
  });
  const current = new Map([[core.getItemKey(existing), existing]]);
  const result = records.mergeLearningRecords(current, {
    records: [
      {
        ...item,
        attempts: 5,
        correct: 2,
        misses: 3,
      },
    ],
  });

  assert.equal(result.importedCount, 1);
  assert.equal(result.records.size, 1);
  assert.equal(result.records.get(core.getItemKey(item)).attempts, 5);
});

test("applies practice results without mutating the input map", () => {
  const item = core.getItemByRomaji("a");
  const source = new Map();
  const result = records.applyPracticeResult(source, item, false, {
    now: new Date("2026-06-03T00:00:00.000Z"),
  });
  const record = result.records.get(core.getItemKey(item));

  assert.equal(source.size, 0);
  assert.equal(record.attempts, 1);
  assert.equal(record.correct, 0);
  assert.equal(record.misses, 1);
  assert.equal(record.masteredStreak, 0);
  assert.equal(record.lastPracticedAt, "2026-06-03T00:00:00.000Z");
});

test("builds export payloads and filenames with stable timestamps", () => {
  const item = core.getItemByRomaji("a");
  const map = new Map([[core.getItemKey(item), core.createRecord(item)]]);
  const now = new Date("2026-06-03T01:02:03.004Z");
  const payload = records.buildLearningExportData(map, now);

  assert.equal(payload.schemaVersion, 1);
  assert.equal(payload.exportedAt, "2026-06-03T01:02:03.004Z");
  assert.equal(payload.records.length, 1);
  assert.equal(records.getLearningExportFilename(now), "kana-learning-records-2026-06-03T01-02-03-004Z.json");
});

test("storage adapter saves, loads, clears, and degrades when storage is unavailable", () => {
  const item = core.getItemByRomaji("a");
  const storage = createMemoryStorage();
  const adapter = records.createStorageAdapter(storage, "test-records");
  const map = new Map([[core.getItemKey(item), core.createRecord(item)]]);

  assert.equal(adapter.canStore(), true);
  assert.equal(adapter.save(map), true);
  assert.equal(adapter.load().size, 1);
  assert.equal(adapter.clear(), true);
  assert.equal(adapter.load().size, 0);

  const unavailable = records.createStorageAdapter(null, "test-records");
  assert.equal(unavailable.canStore(), false);
  assert.equal(unavailable.save(map), false);
  assert.equal(unavailable.load().size, 0);
});
