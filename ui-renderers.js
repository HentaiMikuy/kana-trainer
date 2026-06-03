(function (root, factory) {
  const api = factory(root);
  if (typeof module === "object" && module.exports) {
    module.exports = api;
  }
  root.KanaUiRenderers = api;
})(typeof globalThis !== "undefined" ? globalThis : this, function (root) {
  function getDocument() {
    if (!root.document) {
      throw new Error("KanaUiRenderers requires a DOM document");
    }
    return root.document;
  }

  function getDisplayScript(item, fallbackScript = "katakana", chartScript = "katakana") {
    if (item?.forcedScript === "hiragana" || item?.forcedScript === "katakana") {
      return item.forcedScript;
    }
    if (fallbackScript === "hiragana" || fallbackScript === "katakana") {
      return fallbackScript;
    }
    return chartScript;
  }

  function getKanaDisplay(item, options = {}) {
    const script = getDisplayScript(item, options.fallbackScript, options.chartScript);
    const primary = script === "hiragana" ? item.hiragana : item.katakana;
    const secondary = script === "hiragana" ? item.katakana : item.hiragana;
    return { primary, secondary, script };
  }

  function setFeedback(target, { text, className = "feedback", answer = "", isKanaAnswer = false }) {
    const doc = getDocument();
    target.textContent = "";
    target.className = className;

    target.append(doc.createTextNode(text));

    if (answer) {
      const answerNode = doc.createElement("span");
      answerNode.textContent = answer;
      if (isKanaAnswer) {
        answerNode.className = "kana-text";
      }
      target.append(answerNode);
    }
  }

  function createMetric(value, label) {
    const doc = getDocument();
    const metric = doc.createElement("div");
    metric.className = "result-metric";

    const valueNode = doc.createElement("strong");
    valueNode.textContent = value;

    const labelNode = doc.createElement("span");
    labelNode.textContent = label;

    metric.append(valueNode, labelNode);
    return metric;
  }

  function createKanaBadge(item, displayOptions = {}) {
    const doc = getDocument();
    const badge = doc.createElement("div");
    badge.className = "result-kana-badge";
    const display = getKanaDisplay(item, {
      ...displayOptions,
      fallbackScript: item.forcedScript || item.answerScript || displayOptions.fallbackScript,
    });

    const kana = doc.createElement("strong");
    kana.className = "kana-text";
    kana.textContent = display.primary;

    const reading = doc.createElement("span");
    const secondary = doc.createElement("span");
    secondary.className = "kana-text";
    secondary.textContent = display.secondary;
    reading.append(secondary, ` / ${item.romaji}`);

    const misses = doc.createElement("small");
    misses.textContent = `${item.misses || 0} 次错误`;

    badge.append(kana, reading, misses);
    return badge;
  }

  function createRowBadge(row) {
    const doc = getDocument();
    const badge = doc.createElement("div");
    badge.className = "result-row-badge";

    const label = doc.createElement("strong");
    label.textContent = row.label;

    const summary = doc.createElement("span");
    summary.textContent = `${row.misses} 次错误 · ${row.count} 个假名`;

    badge.append(label, summary);
    return badge;
  }

  function createReportSection(titleText, body) {
    const doc = getDocument();
    const section = doc.createElement("section");
    section.className = "result-section";

    const title = doc.createElement("h3");
    title.textContent = titleText;

    section.append(title, body);
    return section;
  }

  function createReportButton(text, className, onClick, disabled = false) {
    const doc = getDocument();
    const button = doc.createElement("button");
    button.type = "button";
    button.className = className;
    button.textContent = text;
    button.disabled = disabled;
    if (!disabled) {
      button.addEventListener("click", onClick);
    }
    return button;
  }

  function createEmptyBlock(text) {
    const doc = getDocument();
    const empty = doc.createElement("div");
    empty.className = "result-empty";
    empty.textContent = text;
    return empty;
  }

  function renderCompletionReport(target, options) {
    const doc = getDocument();
    const {
      accuracy,
      averageTime,
      correctCount,
      displayOptions = {},
      longTermMistakes,
      onResetQuiz,
      onReviewLongTermMistakes,
      onReviewRoundMistakes,
      roundMistakes,
      weakRows,
      wrongCount,
    } = options;
    const hasRoundMistakes = roundMistakes.length > 0;
    const hasLongTermMistakes = longTermMistakes.length > 0;

    target.innerHTML = "";
    target.classList.remove("hidden");

    const summary = doc.createElement("div");
    summary.className = "result-summary";
    summary.append(
      createMetric(`${accuracy}%`, "本轮正确率"),
      createMetric(`${correctCount}`, "答对题数"),
      createMetric(`${wrongCount}`, "答错题数"),
      createMetric(averageTime, "平均用时"),
    );

    const message = doc.createElement("p");
    message.className = hasRoundMistakes ? "result-message wrong" : "result-message right";
    message.textContent = hasRoundMistakes
      ? "优先复习下面这些本轮出错的假名。"
      : "这一轮没有错题，可以继续扩大范围或切换输入模式巩固。";

    const sections = doc.createElement("div");
    sections.className = "result-sections";

    const mistakeList = doc.createElement("div");
    mistakeList.className = "result-kana-list";
    if (hasRoundMistakes) {
      roundMistakes.slice(0, 6).forEach((item) => {
        mistakeList.append(createKanaBadge(item, displayOptions));
      });
    } else {
      mistakeList.append(createEmptyBlock("本轮没有需要优先复习的错题。"));
    }
    sections.append(createReportSection("本轮错得最多", mistakeList));

    const rowList = doc.createElement("div");
    rowList.className = "result-row-list";
    if (weakRows.length) {
      weakRows.slice(0, 4).forEach((row) => rowList.append(createRowBadge(row)));
    } else {
      rowList.append(createEmptyBlock("本轮没有明显薄弱行。"));
    }
    sections.append(createReportSection("薄弱行", rowList));

    const actions = doc.createElement("div");
    actions.className = "result-actions";
    actions.append(
      createReportButton(
        hasRoundMistakes ? "再练本轮错题" : "本轮无错题",
        "next-button",
        () => onReviewRoundMistakes(roundMistakes),
        !hasRoundMistakes,
      ),
      createReportButton("重新随机一轮", "ghost-button", onResetQuiz),
      createReportButton(
        hasLongTermMistakes ? "只练长期薄弱项" : "暂无长期薄弱项",
        "ghost-button",
        () => onReviewLongTermMistakes(longTermMistakes),
        !hasLongTermMistakes,
      ),
    );

    target.append(summary, message, sections, actions);
  }

  function renderWeakList(target, { displayOptions = {}, getRecordAccuracy, mistakes }) {
    const doc = getDocument();
    target.innerHTML = "";

    if (!mistakes.length) {
      const empty = doc.createElement("div");
      empty.className = "empty-state";
      empty.textContent = "错题会显示在这里";
      target.append(empty);
      return;
    }

    mistakes.forEach((item) => {
      const accuracy = getRecordAccuracy(item);
      const display = getKanaDisplay(item, displayOptions);
      const card = doc.createElement("div");
      card.className = "weak-card";

      const primaryKana = doc.createElement("strong");
      primaryKana.className = "kana-text";
      primaryKana.textContent = display.primary;

      const reading = doc.createElement("span");
      const secondaryKana = doc.createElement("span");
      secondaryKana.className = "kana-text";
      secondaryKana.textContent = display.secondary;
      reading.append(secondaryKana, ` / ${item.romaji}`);

      const summary = doc.createElement("small");
      summary.textContent = `${item.misses} 错 · ${accuracy}%`;

      card.append(primaryKana, reading, summary);
      target.append(card);
    });
  }

  function getChartSectionSummary(items, getMasteryState) {
    const counts = {
      new: 0,
      weak: 0,
      practice: 0,
      steady: 0,
    };

    items.forEach((item) => {
      const mastery = getMasteryState(item);
      counts[mastery.bucket] += 1;
    });

    const parts = [];
    if (counts.new) parts.push(`新 ${counts.new}`);
    if (counts.practice) parts.push(`练 ${counts.practice}`);
    if (counts.weak) parts.push(`弱 ${counts.weak}`);
    if (counts.steady) parts.push(`稳 ${counts.steady}`);

    return parts.length ? parts.join(" · ") : "暂无记录";
  }

  function renderChart(options) {
    const doc = getDocument();
    const {
      chartScript,
      getMasteryState,
      groupChartItemsByRow,
      onSpeak,
      sections,
      target,
      toggleButton,
    } = options;
    target.innerHTML = "";

    sections.forEach((section) => {
      const block = doc.createElement("section");
      block.className = "chart-section";
      block.dataset.section = section.key;

      const head = doc.createElement("div");
      head.className = "chart-section-head";

      const title = doc.createElement("h3");
      title.textContent = section.title;

      const summary = doc.createElement("span");
      summary.className = "chart-summary";
      summary.textContent = getChartSectionSummary(section.items, getMasteryState);

      head.append(title, summary);

      const body = doc.createElement("div");
      body.className = "chart-section-body";

      groupChartItemsByRow(section.items).forEach((row) => {
        const rowWrap = doc.createElement("div");
        rowWrap.className = "chart-row";

        const rowLabel = doc.createElement("strong");
        rowLabel.className = "chart-row-label";
        rowLabel.textContent = row.label;

        const grid = doc.createElement("div");
        grid.className = "chart-grid";

        row.items.forEach((item) => {
          const cell = doc.createElement("button");
          const mastery = getMasteryState(item);
          const kana = chartScript === "katakana" ? item.katakana : item.hiragana;
          cell.className = "kana-cell";
          cell.type = "button";
          cell.dataset.mastery = mastery.bucket;
          cell.setAttribute(
            "aria-label",
            `播放 ${kana} 的发音，罗马音 ${item.romaji}，${mastery.detail}`,
          );
          cell.title = `${item.hiragana} / ${item.katakana} · ${item.romaji} · ${mastery.detail}`;

          const kanaNode = doc.createElement("strong");
          kanaNode.className = "kana-text";
          kanaNode.textContent = kana;

          const romajiNode = doc.createElement("span");
          romajiNode.textContent = item.romaji;

          const statusNode = doc.createElement("small");
          statusNode.className = "chart-status";
          statusNode.textContent = mastery.label;

          cell.append(kanaNode, romajiNode, statusNode);
          cell.addEventListener("click", () => onSpeak(item));
          grid.append(cell);
        });

        rowWrap.append(rowLabel, grid);
        body.append(rowWrap);
      });

      block.append(head, body);
      target.append(block);
    });

    toggleButton.textContent = chartScript === "katakana" ? "片假名" : "平假名";
  }

  return {
    getKanaDisplay,
    renderChart,
    renderCompletionReport,
    renderWeakList,
    setFeedback,
  };
});
