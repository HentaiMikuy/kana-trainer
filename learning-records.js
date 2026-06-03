(function (root, factory) {
  const core =
    typeof require === "function" && typeof module === "object" && module.exports
      ? require("./kana-core.js")
      : root.KanaCore;
  const api = factory(core);
  if (typeof module === "object" && module.exports) {
    module.exports = api;
  }
  root.KanaLearningRecords = api;
})(typeof globalThis !== "undefined" ? globalThis : this, function (core) {
  if (!core) {
    throw new Error("KanaLearningRecords requires KanaCore");
  }

  const STORAGE_KEY = "kana-trainer-learning-records:v1";
  const EXPORT_SCHEMA_VERSION = 1;

  function toRecordArray(records) {
    if (records instanceof Map) return [...records.values()];
    return Array.isArray(records) ? records : [];
  }

  function normalizeLearningRecords(source) {
    const records = Array.isArray(source)
      ? source
      : Array.isArray(source?.records)
        ? source.records
        : [];

    return records
      .filter((record) => record && typeof record === "object")
      .map((record) => {
        const canonicalItem = core.getCanonicalPracticeItem(record);
        return canonicalItem ? core.normalizeRecord(record, canonicalItem) : null;
      })
      .filter(Boolean);
  }

  function recordsToMap(records) {
    return new Map(
      normalizeLearningRecords(records).map((record) => [core.getItemKey(record), record]),
    );
  }

  function getLongTermMistakeRecords(records) {
    return new Map(
      [...records.entries()]
        .filter(([, record]) => core.isLongTermWeakRecord(record))
        .map(([key, record]) => [key, record]),
    );
  }

  function buildLearningExportData(records, now = new Date()) {
    return {
      schemaVersion: EXPORT_SCHEMA_VERSION,
      exportedAt: now.toISOString(),
      records: toRecordArray(records),
    };
  }

  function getLearningExportFilename(now = new Date()) {
    const stamp = now.toISOString().replace(/[:.]/g, "-");
    return `kana-learning-records-${stamp}.json`;
  }

  function mergeLearningRecords(currentRecords, payload) {
    const importedRecords = normalizeLearningRecords(payload);
    const records = new Map(currentRecords);

    importedRecords.forEach((record) => {
      records.set(core.getItemKey(record), record);
    });

    return {
      records,
      importedCount: importedRecords.length,
      importedRecords,
    };
  }

  function applyPracticeResult(records, item, isCorrect, { now = new Date() } = {}) {
    const key = core.getItemKey(item);
    const existing = records.get(key) || core.createRecord(item);
    const record = core.normalizeRecord(existing);

    record.attempts += 1;
    record.lastPracticedAt = now.toISOString();

    if (isCorrect) {
      record.correct += 1;
      record.masteredStreak += 1;
    } else {
      record.misses += 1;
      record.masteredStreak = 0;
    }

    const nextRecords = new Map(records);
    nextRecords.set(key, record);

    return {
      records: nextRecords,
      record,
      key,
    };
  }

  function createStorageAdapter(storage, key = STORAGE_KEY) {
    let storageAvailable = null;

    function canStore() {
      if (storageAvailable !== null) return storageAvailable;

      try {
        const testKey = `${key}:test`;
        storage.setItem(testKey, "1");
        storage.removeItem(testKey);
        storageAvailable = true;
      } catch {
        storageAvailable = false;
      }

      return storageAvailable;
    }

    function load() {
      if (!canStore()) return new Map();

      try {
        const stored = JSON.parse(storage.getItem(key) || "[]");
        return recordsToMap(stored);
      } catch {
        return new Map();
      }
    }

    function save(records) {
      if (!canStore()) return false;

      try {
        storage.setItem(key, JSON.stringify(toRecordArray(records)));
        return true;
      } catch {
        return false;
      }
    }

    function clear() {
      if (!canStore()) return false;

      try {
        storage.removeItem(key);
        return true;
      } catch {
        return false;
      }
    }

    return {
      canStore,
      clear,
      key,
      load,
      save,
    };
  }

  return {
    EXPORT_SCHEMA_VERSION,
    STORAGE_KEY,
    applyPracticeResult,
    buildLearningExportData,
    createStorageAdapter,
    getLearningExportFilename,
    getLongTermMistakeRecords,
    mergeLearningRecords,
    normalizeLearningRecords,
    recordsToMap,
  };
});
