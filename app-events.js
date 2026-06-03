(function (root, factory) {
  const api = factory(root);
  if (typeof module === "object" && module.exports) {
    module.exports = api;
  }
  root.KanaAppEvents = api;
})(typeof globalThis !== "undefined" ? globalThis : this, function (root) {
  function isKeyboardShortcutBlocked(target, options = {}) {
    const {
      answerInput,
      ElementConstructor = root.Element,
    } = options;

    if (!ElementConstructor || !(target instanceof ElementConstructor)) return false;
    if (target === answerInput) return false;
    return Boolean(target.closest("button, input, select, textarea, [contenteditable='true']"));
  }

  function bindButtonGroup(buttons, eventName, handler) {
    buttons.forEach((button) => {
      button.addEventListener(eventName, () => handler(button));
    });
  }

  function bindAction(element, eventName, handler) {
    element.addEventListener(eventName, () => handler());
  }

  function bindAppEvents(options) {
    const {
      callbacks,
      elements,
      getState,
      root: targetRoot = root,
    } = options;

    bindButtonGroup(elements.modeButtons, "click", (button) => {
      callbacks.onModeChange(button.dataset.mode);
    });

    bindButtonGroup(elements.glyphButtons, "click", (button) => {
      callbacks.onGlyphChange(button.dataset.glyph);
    });

    bindButtonGroup(elements.pronunciationButtons, "click", (button) => {
      callbacks.onPronunciationChange(button.dataset.pronunciation);
    });

    bindButtonGroup(elements.answerModeButtons, "click", (button) => {
      callbacks.onAnswerModeChange(button.dataset.answerMode);
    });

    bindButtonGroup(elements.practiceTypeButtons, "click", (button) => {
      callbacks.onPracticeTypeChange(button.dataset.practiceType);
    });

    bindButtonGroup(elements.confusingSetButtons, "click", (button) => {
      callbacks.onConfusingSetChange(button.dataset.confusingSet);
    });

    elements.directionSelect.addEventListener("change", (event) => {
      callbacks.onDirectionChange(event.target.value);
    });

    elements.questionLimit.addEventListener("input", (event) => {
      callbacks.onQuestionLimitInput(Number(event.target.value));
    });
    elements.questionLimit.addEventListener("change", () => {
      callbacks.onQuestionLimitChange();
    });

    elements.challengeToggle.addEventListener("change", (event) => {
      callbacks.onChallengeEnabledChange(event.target.checked);
    });

    elements.challengeLimit.addEventListener("input", (event) => {
      callbacks.onChallengeLimitInput(Number(event.target.value));
    });
    elements.challengeLimit.addEventListener("change", () => {
      callbacks.onChallengeLimitChange();
    });

    elements.dakutenToggle.addEventListener("change", (event) => {
      callbacks.onDakutenChange(event.target.checked);
    });

    elements.smallKanaToggle.addEventListener("change", (event) => {
      callbacks.onSmallKanaChange(event.target.checked);
    });

    bindAction(elements.newQuizButton, "click", callbacks.onResetQuiz);
    bindAction(elements.resetButton, "click", callbacks.onResetQuiz);
    bindAction(elements.revealButton, "click", callbacks.onRevealAnswer);
    bindAction(elements.speakButton, "click", callbacks.onSpeak);
    bindAction(elements.submitInputButton, "click", callbacks.onSubmitInputAnswer);
    bindAction(elements.nextButton, "click", callbacks.onNextQuestion);
    bindAction(elements.reviewMistakesButton, "click", callbacks.onReviewMistakes);
    bindAction(elements.clearRecordsButton, "click", callbacks.onClearRecords);
    bindAction(elements.exportDataButton, "click", callbacks.onExportData);

    elements.importDataButton.addEventListener("click", () => {
      elements.importDataInput.click();
    });
    elements.importDataInput.addEventListener("change", (event) => {
      const [file] = event.target.files || [];
      callbacks.onImportDataFile(file);
    });

    bindAction(elements.toggleChartButton, "click", callbacks.onToggleChart);

    targetRoot.addEventListener("keydown", (event) => {
      if (
        isKeyboardShortcutBlocked(event.target, {
          answerInput: elements.answerInput,
          ElementConstructor: targetRoot.Element,
        })
      ) {
        return;
      }

      const state = getState();

      if (event.key === "Enter" && state.answerMode === "input") {
        event.preventDefault();
        if (state.selectedAnswer && !elements.nextButton.disabled) {
          callbacks.onNextQuestion();
        } else {
          callbacks.onSubmitInputAnswer();
        }
        return;
      }

      const number = Number(event.key);
      if (
        state.answerMode === "choice" &&
        number >= 1 &&
        number <= elements.optionsGrid.children.length &&
        !state.selectedAnswer
      ) {
        elements.optionsGrid.children[number - 1].click();
      }

      if (event.key === "Enter" && !elements.nextButton.disabled) {
        callbacks.onNextQuestion();
      }
    });
  }

  return {
    bindAppEvents,
    isKeyboardShortcutBlocked,
  };
});
