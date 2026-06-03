(function (root, factory) {
  const api = factory(root);
  if (typeof module === "object" && module.exports) {
    module.exports = api;
  }
  root.KanaSettingsControls = api;
})(typeof globalThis !== "undefined" ? globalThis : this, function (root) {
  function syncPressedButtons(buttons, isActive) {
    buttons.forEach((button) => {
      const active = isActive(button);
      button.classList.toggle("active", active);
      button.setAttribute("aria-pressed", String(active));
    });
  }

  function syncChallengeControls(elements, state) {
    elements.challengeToggle.checked = state.challengeEnabled;
    elements.challengeLimit.value = state.challengeLimitSeconds;
    elements.challengeLimitValue.textContent = state.challengeLimitSeconds;
  }

  function syncModeControls(elements, state) {
    syncPressedButtons(elements.modeButtons, (button) => button.dataset.mode === state.mode);
  }

  function syncGlyphControls(elements, state, options = {}) {
    const documentElement = options.documentElement || root.document?.documentElement;
    if (documentElement) {
      documentElement.dataset.glyph = state.glyphStyle;
    }
    syncPressedButtons(elements.glyphButtons, (button) => button.dataset.glyph === state.glyphStyle);
  }

  function syncPronunciationControls(elements, state) {
    syncPressedButtons(
      elements.pronunciationButtons,
      (button) => button.dataset.pronunciation === state.pronunciationMode,
    );
  }

  function syncAnswerModeControls(elements, state) {
    syncPressedButtons(
      elements.answerModeButtons,
      (button) => button.dataset.answerMode === state.answerMode,
    );
  }

  function syncPracticeTypeControls(elements, state) {
    const isConfusing = state.practiceType === "confusing";
    elements.confusingControl.classList.toggle("hidden", !isConfusing);
    elements.questionLimit.closest(".control-group")?.classList.remove("hidden");
    elements.modeControl?.classList.toggle("hidden", isConfusing);
    elements.rowPicker.closest(".normal-scope-control")?.classList.toggle("hidden", isConfusing);
    elements.dakutenToggle.closest(".normal-scope-control")?.classList.toggle("hidden", isConfusing);
    elements.smallKanaToggle.closest(".normal-scope-control")?.classList.toggle("hidden", isConfusing);
    syncPressedButtons(
      elements.practiceTypeButtons,
      (button) => button.dataset.practiceType === state.practiceType,
    );
    syncPressedButtons(
      elements.confusingSetButtons,
      (button) => button.dataset.confusingSet === state.selectedConfusingSet,
    );
  }

  function renderRows(options) {
    const {
      baseRows,
      createElement = root.document.createElement.bind(root.document),
      onToggleRow = () => {},
      rowPicker,
      selectedRows,
    } = options;

    rowPicker.innerHTML = "";
    baseRows.forEach((row) => {
      const selected = selectedRows.has(row.id);
      const button = createElement("button");
      button.type = "button";
      button.className = `row-chip ${selected ? "active" : ""}`;
      button.classList.add("kana-text");
      button.textContent = row.label;
      button.setAttribute("aria-pressed", String(selected));
      button.addEventListener("click", () => onToggleRow(row));
      rowPicker.append(button);
    });
  }

  function createSettingsControls(options) {
    const {
      baseRows,
      createElement,
      documentElement = root.document?.documentElement,
      elements,
      getState,
      onToggleRow,
    } = options;

    return {
      renderRows() {
        renderRows({
          baseRows,
          createElement,
          onToggleRow,
          rowPicker: elements.rowPicker,
          selectedRows: getState().selectedRows,
        });
      },
      syncAnswerModeControls() {
        syncAnswerModeControls(elements, getState());
      },
      syncChallengeControls() {
        syncChallengeControls(elements, getState());
      },
      syncGlyphControls() {
        syncGlyphControls(elements, getState(), { documentElement });
      },
      syncModeControls() {
        syncModeControls(elements, getState());
      },
      syncPracticeTypeControls() {
        syncPracticeTypeControls(elements, getState());
      },
      syncPronunciationControls() {
        syncPronunciationControls(elements, getState());
      },
    };
  }

  return {
    createSettingsControls,
    renderRows,
    syncAnswerModeControls,
    syncChallengeControls,
    syncGlyphControls,
    syncModeControls,
    syncPracticeTypeControls,
    syncPronunciationControls,
    syncPressedButtons,
  };
});
