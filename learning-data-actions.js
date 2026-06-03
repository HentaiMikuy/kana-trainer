(function (root, factory) {
  const learningRecords =
    typeof require === "function" && typeof module === "object" && module.exports
      ? require("./learning-records.js")
      : root.KanaLearningRecords;
  const api = factory(root, learningRecords);
  if (typeof module === "object" && module.exports) {
    module.exports = api;
  }
  root.KanaLearningDataActions = api;
})(typeof globalThis !== "undefined" ? globalThis : this, function (root, learningRecords) {
  if (!learningRecords) {
    throw new Error("KanaLearningDataActions requires KanaLearningRecords");
  }

  const CLEAR_RECORDS_MESSAGE = "确定清空本地学习记录吗？错题和历史正确率都会被移除。";
  const IMPORT_CONFIRM_MESSAGE = "导入学习数据会合并并覆盖同一假名的历史记录，是否继续？";
  const JSON_FILE_TYPE = "application/json;charset=utf-8";

  function downloadTextFile(filename, text, options = {}) {
    const {
      BlobCtor = root.Blob,
      URLApi = root.URL,
      document = root.document,
      setTimeoutFn = root.setTimeout?.bind(root) || ((callback) => callback()),
    } = options;

    const blob = new BlobCtor([text], { type: JSON_FILE_TYPE });
    const url = URLApi.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = filename;
    link.rel = "noopener";
    document.body.append(link);
    link.click();
    link.remove();
    setTimeoutFn(() => URLApi.revokeObjectURL(url), 0);

    return {
      blob,
      filename,
      text,
      url,
    };
  }

  function exportLearningRecords(records, options = {}) {
    const {
      buildExportData = learningRecords.buildLearningExportData,
      download = downloadTextFile,
      getExportFilename = learningRecords.getLearningExportFilename,
      stringify = JSON.stringify,
    } = options;
    const payload = buildExportData(records);
    const filename = getExportFilename();
    const text = `${stringify(payload, null, 2)}\n`;

    download(filename, text, options);

    return {
      filename,
      payload,
      text,
    };
  }

  function confirmAndClearLearningRecords(options = {}) {
    const {
      confirm = root.confirm?.bind(root) || (() => true),
      message = CLEAR_RECORDS_MESSAGE,
      onClear = () => {},
    } = options;

    if (!confirm(message)) {
      return false;
    }

    onClear();
    return true;
  }

  async function handleImportDataFile(file, options = {}) {
    if (!file) {
      return {
        status: "empty",
      };
    }

    const {
      confirm = root.confirm?.bind(root) || (() => true),
      confirmMessage = IMPORT_CONFIRM_MESSAGE,
      onComplete = () => {},
      onImportPayload = () => {},
      onInvalid = () => {},
    } = options;

    try {
      let payload;

      try {
        const text = await file.text();
        payload = JSON.parse(text);
      } catch {
        onInvalid();
        return {
          status: "invalid",
        };
      }

      if (!confirm(confirmMessage)) {
        return {
          payload,
          status: "cancelled",
        };
      }

      await onImportPayload(payload);
      return {
        payload,
        status: "imported",
      };
    } finally {
      onComplete();
    }
  }

  return {
    CLEAR_RECORDS_MESSAGE,
    IMPORT_CONFIRM_MESSAGE,
    JSON_FILE_TYPE,
    confirmAndClearLearningRecords,
    downloadTextFile,
    exportLearningRecords,
    handleImportDataFile,
  };
});
