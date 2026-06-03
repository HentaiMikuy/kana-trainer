const assert = require("node:assert/strict");
const actions = require("../learning-data-actions.js");

const tests = [];

function test(name, fn) {
  tests.push({ name, fn });
}

async function run() {
  for (const { name, fn } of tests) {
    try {
      await fn();
      console.log(`ok - ${name}`);
    } catch (error) {
      console.error(`not ok - ${name}`);
      throw error;
    }
  }
}

test("downloads text files through injected browser APIs", () => {
  const appended = [];
  const createdBlobs = [];
  const revokedUrls = [];
  const timeoutDelays = [];
  const link = {
    clickCount: 0,
    download: "",
    href: "",
    rel: "",
    removed: false,
    click() {
      this.clickCount += 1;
    },
    remove() {
      this.removed = true;
    },
  };
  class FakeBlob {
    constructor(parts, options) {
      this.parts = parts;
      this.options = options;
      createdBlobs.push(this);
    }
  }

  const result = actions.downloadTextFile("records.json", "{\"records\":[]}\n", {
    BlobCtor: FakeBlob,
    URLApi: {
      createObjectURL(blob) {
        assert.equal(blob, createdBlobs[0]);
        return "blob:test-records";
      },
      revokeObjectURL(url) {
        revokedUrls.push(url);
      },
    },
    document: {
      body: {
        append(node) {
          appended.push(node);
        },
      },
      createElement(tagName) {
        assert.equal(tagName, "a");
        return link;
      },
    },
    setTimeoutFn(callback, delay) {
      timeoutDelays.push(delay);
      callback();
    },
  });

  assert.equal(createdBlobs.length, 1);
  assert.deepEqual(createdBlobs[0].parts, ["{\"records\":[]}\n"]);
  assert.deepEqual(createdBlobs[0].options, { type: actions.JSON_FILE_TYPE });
  assert.equal(link.href, "blob:test-records");
  assert.equal(link.download, "records.json");
  assert.equal(link.rel, "noopener");
  assert.equal(link.clickCount, 1);
  assert.equal(link.removed, true);
  assert.deepEqual(appended, [link]);
  assert.deepEqual(timeoutDelays, [0]);
  assert.deepEqual(revokedUrls, ["blob:test-records"]);
  assert.equal(result.blob, createdBlobs[0]);
  assert.equal(result.filename, "records.json");
  assert.equal(result.text, "{\"records\":[]}\n");
  assert.equal(result.url, "blob:test-records");
});

test("exports learning records with a stable payload, filename, and JSON text", () => {
  const records = new Map([["a|katakana", { romaji: "a", katakana: "ア" }]]);
  const payload = {
    records: [{ romaji: "a", katakana: "ア" }],
    schemaVersion: 1,
  };
  let downloadCall = null;

  const result = actions.exportLearningRecords(records, {
    buildExportData(source) {
      assert.equal(source, records);
      return payload;
    },
    download(filename, text, options) {
      downloadCall = { filename, options, text };
    },
    getExportFilename() {
      return "kana-records.json";
    },
  });

  assert.equal(downloadCall.filename, "kana-records.json");
  assert.equal(downloadCall.text, `${JSON.stringify(payload, null, 2)}\n`);
  assert.equal(downloadCall.options.getExportFilename(), "kana-records.json");
  assert.deepEqual(result, {
    filename: "kana-records.json",
    payload,
    text: `${JSON.stringify(payload, null, 2)}\n`,
  });
});

test("confirms before clearing learning records", () => {
  let confirmMessage = "";
  let clearCount = 0;

  const cancelled = actions.confirmAndClearLearningRecords({
    confirm(message) {
      confirmMessage = message;
      return false;
    },
    onClear() {
      clearCount += 1;
    },
  });

  assert.equal(cancelled, false);
  assert.equal(confirmMessage, actions.CLEAR_RECORDS_MESSAGE);
  assert.equal(clearCount, 0);

  const confirmed = actions.confirmAndClearLearningRecords({
    confirm(message) {
      confirmMessage = message;
      return true;
    },
    onClear() {
      clearCount += 1;
    },
  });

  assert.equal(confirmed, true);
  assert.equal(confirmMessage, actions.CLEAR_RECORDS_MESSAGE);
  assert.equal(clearCount, 1);
});

test("returns empty status when no import file is provided", async () => {
  let completeCount = 0;

  const result = await actions.handleImportDataFile(null, {
    onComplete() {
      completeCount += 1;
    },
  });

  assert.deepEqual(result, { status: "empty" });
  assert.equal(completeCount, 0);
});

test("reports invalid import files and completes cleanup", async () => {
  let confirmCount = 0;
  let completeCount = 0;
  let invalidCount = 0;

  const result = await actions.handleImportDataFile(
    {
      async text() {
        return "{invalid json";
      },
    },
    {
      confirm() {
        confirmCount += 1;
        return true;
      },
      onComplete() {
        completeCount += 1;
      },
      onInvalid() {
        invalidCount += 1;
      },
    },
  );

  assert.deepEqual(result, { status: "invalid" });
  assert.equal(confirmCount, 0);
  assert.equal(completeCount, 1);
  assert.equal(invalidCount, 1);
});

test("cancels valid imports before applying payloads", async () => {
  const payload = { records: [{ romaji: "a" }] };
  let completeCount = 0;
  let importCount = 0;

  const result = await actions.handleImportDataFile(
    {
      async text() {
        return JSON.stringify(payload);
      },
    },
    {
      confirm(message) {
        assert.equal(message, actions.IMPORT_CONFIRM_MESSAGE);
        return false;
      },
      onComplete() {
        completeCount += 1;
      },
      onImportPayload() {
        importCount += 1;
      },
    },
  );

  assert.deepEqual(result, {
    payload,
    status: "cancelled",
  });
  assert.equal(completeCount, 1);
  assert.equal(importCount, 0);
});

test("imports valid payloads after confirmation", async () => {
  const payload = { records: [{ romaji: "shi" }] };
  let completeCount = 0;
  let importedPayload = null;

  const result = await actions.handleImportDataFile(
    {
      async text() {
        return JSON.stringify(payload);
      },
    },
    {
      confirm() {
        return true;
      },
      onComplete() {
        completeCount += 1;
      },
      async onImportPayload(nextPayload) {
        importedPayload = nextPayload;
      },
    },
  );

  assert.deepEqual(result, {
    payload,
    status: "imported",
  });
  assert.deepEqual(importedPayload, payload);
  assert.equal(completeCount, 1);
});

test("does not hide import callback errors as invalid JSON", async () => {
  const payload = { records: [{ romaji: "tsu" }] };
  const expectedError = new Error("merge failed");
  let completeCount = 0;
  let invalidCount = 0;

  await assert.rejects(
    actions.handleImportDataFile(
      {
        async text() {
          return JSON.stringify(payload);
        },
      },
      {
        confirm() {
          return true;
        },
        onComplete() {
          completeCount += 1;
        },
        onImportPayload() {
          throw expectedError;
        },
        onInvalid() {
          invalidCount += 1;
        },
      },
    ),
    expectedError,
  );

  assert.equal(completeCount, 1);
  assert.equal(invalidCount, 0);
});

run();
