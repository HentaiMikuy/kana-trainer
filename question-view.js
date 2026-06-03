(function (root, factory) {
  const api = factory(root);
  if (typeof module === "object" && module.exports) {
    module.exports = api;
  }
  root.KanaQuestionView = api;
})(typeof globalThis !== "undefined" ? globalThis : this, function (root) {
  function getPromptInputModePlaceholder(question) {
    if (!question) return "";
    return question.answerType === "romaji" ? "输入罗马音" : "输入假名";
  }

  function clearAnswerInput(elements) {
    elements.answerInput.value = "";
  }

  function setInputModeVisibility(elements, options) {
    const { answerMode, current, selectedAnswer } = options;
    const isInputMode = answerMode === "input";
    elements.inputAnswerArea.classList.toggle("hidden", !isInputMode);
    elements.optionsGrid.classList.toggle("hidden", isInputMode);
    elements.answerInput.disabled = !isInputMode || !current;
    elements.submitInputButton.disabled = !isInputMode || !current || selectedAnswer;
    elements.answerInput.placeholder = getPromptInputModePlaceholder(current);
  }

  function focusAnswerInput(elements, options) {
    const {
      answerMode,
      current,
      requestAnimationFrame = root.requestAnimationFrame?.bind(root) || ((callback) => callback()),
    } = options;

    if (answerMode !== "input" || !current) return;
    requestAnimationFrame(() => {
      elements.answerInput.focus();
      elements.answerInput.select();
    });
  }

  function renderEmptyQuestion(elements) {
    elements.promptText.textContent = "無";
    elements.promptText.classList.add("kana-text");
    elements.promptSub.textContent = "当前范围没有可练习的假名";
    elements.optionsGrid.innerHTML = "";
    elements.inputAnswerArea.classList.add("hidden");
    elements.feedback.textContent = "";
  }

  function renderOptions(elements, options) {
    const {
      answerType,
      createElement = root.document.createElement.bind(root.document),
      onChooseAnswer = () => {},
      optionValues,
    } = options;

    elements.optionsGrid.innerHTML = "";
    optionValues.forEach((option) => {
      const button = createElement("button");
      button.type = "button";
      button.className = "option-button";
      button.classList.toggle("kana-text", answerType === "kana");
      button.textContent = option;
      button.addEventListener("click", () => onChooseAnswer(option, button));
      elements.optionsGrid.append(button);
    });
  }

  function renderQuestion(elements, options) {
    const {
      answerMode,
      canSpeak,
      challengeEnabled,
      createElement,
      current,
      currentIndex,
      onChooseAnswer,
      optionValues,
      questionLimit,
      quizModeLabel,
      requestAnimationFrame,
    } = options;

    elements.resultReport.classList.add("hidden");
    elements.resultReport.innerHTML = "";
    elements.actionRow.classList.remove("hidden");
    elements.timerStrip.classList.toggle("hidden", !challengeEnabled);

    if (!current) {
      renderEmptyQuestion(elements);
      return;
    }

    elements.quizModeLabel.textContent = quizModeLabel;
    elements.questionCounter.textContent = `第 ${currentIndex + 1} / ${questionLimit} 题`;
    elements.promptText.textContent = current.prompt;
    elements.promptText.classList.toggle("kana-text", current.answerType === "romaji");
    elements.promptSub.textContent = current.helper;
    elements.feedback.textContent = "";
    elements.feedback.className = "feedback";
    elements.speakButton.disabled = !canSpeak;
    elements.speakButton.title = canSpeak ? "播放当前假名发音" : "当前浏览器不支持语音发音";
    elements.nextButton.disabled = true;
    elements.nextButton.textContent = currentIndex + 1 === questionLimit ? "查看结果" : "下一题";
    elements.answerInput.classList.remove("correct", "wrong");
    elements.answerInput.disabled = false;
    elements.submitInputButton.disabled = false;
    clearAnswerInput(elements);

    renderOptions(elements, {
      answerType: current.answerType,
      createElement,
      onChooseAnswer,
      optionValues,
    });

    setInputModeVisibility(elements, {
      answerMode,
      current,
      selectedAnswer: false,
    });
    elements.revealButton.disabled = false;
    focusAnswerInput(elements, {
      answerMode,
      current,
      requestAnimationFrame,
    });
  }

  function showAnswerResult(elements, options) {
    const {
      answerMode,
      current,
      isCorrect,
      selectedButton,
    } = options;

    if (answerMode === "choice") {
      [...elements.optionsGrid.children].forEach((button) => {
        button.disabled = true;
        if (button.textContent === current.answer) {
          button.classList.add("correct");
        } else if (button === selectedButton && !isCorrect) {
          button.classList.add("wrong");
        }
      });
    } else {
      elements.answerInput.disabled = true;
      elements.submitInputButton.disabled = true;
      elements.answerInput.classList.toggle("correct", isCorrect);
      elements.answerInput.classList.toggle("wrong", !isCorrect);
    }

    elements.nextButton.disabled = false;
  }

  function renderCompletionShell(elements, options) {
    const { answered, correct } = options;
    elements.progressBar.style.width = "100%";
    elements.promptText.textContent = "完成";
    elements.promptText.classList.remove("kana-text");
    elements.promptSub.textContent = `本轮完成：${correct} / ${answered} 题正确`;
    elements.optionsGrid.innerHTML = "";
    elements.inputAnswerArea.classList.add("hidden");
    elements.timerStrip.classList.add("hidden");
    elements.actionRow.classList.add("hidden");
    elements.feedback.textContent = "";
    elements.feedback.className = "feedback";
    elements.nextButton.disabled = true;
    elements.speakButton.disabled = true;
  }

  function createQuestionView(options) {
    const {
      createElement,
      elements,
      requestAnimationFrame,
    } = options;

    return {
      clearAnswerInput() {
        clearAnswerInput(elements);
      },
      focusAnswerInput(viewOptions) {
        focusAnswerInput(elements, {
          ...viewOptions,
          requestAnimationFrame,
        });
      },
      renderEmptyQuestion() {
        renderEmptyQuestion(elements);
      },
      renderQuestion(viewOptions) {
        renderQuestion(elements, {
          ...viewOptions,
          createElement,
          requestAnimationFrame,
        });
      },
      renderCompletionShell(viewOptions) {
        renderCompletionShell(elements, viewOptions);
      },
      setInputModeVisibility(viewOptions) {
        setInputModeVisibility(elements, viewOptions);
      },
      showAnswerResult(viewOptions) {
        showAnswerResult(elements, viewOptions);
      },
    };
  }

  return {
    clearAnswerInput,
    createQuestionView,
    focusAnswerInput,
    getPromptInputModePlaceholder,
    renderCompletionShell,
    renderEmptyQuestion,
    renderOptions,
    renderQuestion,
    setInputModeVisibility,
    showAnswerResult,
  };
});
