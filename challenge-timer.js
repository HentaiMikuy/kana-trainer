(function (root, factory) {
  const api = factory(root);
  if (typeof module === "object" && module.exports) {
    module.exports = api;
  }
  root.KanaChallengeTimer = api;
})(typeof globalThis !== "undefined" ? globalThis : this, function (root) {
  function formatSeconds(seconds) {
    return `${Math.max(seconds, 0).toFixed(1)}s`;
  }

  function updateTimerDisplay(elements, remainingMs, totalMs) {
    const percent = totalMs ? Math.max(Math.min((remainingMs / totalMs) * 100, 100), 0) : 0;
    elements.timerRemaining.textContent = formatSeconds(remainingMs / 1000);
    elements.timerBar.style.width = `${percent}%`;
    elements.timerBar.classList.toggle("danger", percent <= 25);
  }

  function createChallengeTimer(options) {
    const {
      clearIntervalFn = root.clearInterval?.bind(root),
      elements,
      intervalMs = 100,
      now = () => Date.now(),
      onRecordTime = () => {},
      onTimeout = () => {},
      setIntervalFn = root.setInterval?.bind(root),
    } = options;
    let startTime = null;
    let timerId = null;

    function stop({ recordTime = false } = {}) {
      if (timerId !== null) {
        clearIntervalFn(timerId);
        timerId = null;
      }

      if (recordTime && startTime !== null) {
        onRecordTime(Math.max(now() - startTime, 0));
      }

      startTime = null;
    }

    function start({ current = null, enabled = false, limitSeconds }) {
      stop();
      startTime = now();

      const totalMs = limitSeconds * 1000;
      elements.timerStrip.classList.toggle("hidden", !enabled);
      updateTimerDisplay(elements, totalMs, totalMs);

      if (!enabled || !current) {
        return;
      }

      timerId = setIntervalFn(() => {
        const remainingMs = totalMs - (now() - startTime);
        updateTimerDisplay(elements, remainingMs, totalMs);

        if (remainingMs <= 0) {
          stop({ recordTime: true });
          onTimeout();
        }
      }, intervalMs);
    }

    return {
      start,
      stop,
      updateDisplay(limitSeconds, remainingMs = limitSeconds * 1000) {
        updateTimerDisplay(elements, remainingMs, limitSeconds * 1000);
      },
    };
  }

  return {
    createChallengeTimer,
    formatSeconds,
    updateTimerDisplay,
  };
});
