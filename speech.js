(function (root, factory) {
  const api = factory(root);
  if (typeof module === "object" && module.exports) {
    module.exports = api;
  }
  root.KanaSpeech = api;
})(typeof globalThis !== "undefined" ? globalThis : this, function (root) {
  function canSpeak(target = root) {
    return (
      Boolean(target) &&
      "speechSynthesis" in target &&
      "SpeechSynthesisUtterance" in target
    );
  }

  function getJapaneseVoice(target = root) {
    if (!canSpeak(target)) return null;
    return (
      target.speechSynthesis
        .getVoices()
        .find((voice) => voice.lang.toLowerCase().startsWith("ja")) || null
    );
  }

  function speakKana(item, options = {}) {
    const {
      onUnsupported = null,
      root: target = root,
      showUnsupported = true,
    } = options;

    if (!item || !canSpeak(target)) {
      if (showUnsupported && onUnsupported) {
        onUnsupported();
      }
      return false;
    }

    target.speechSynthesis.cancel();
    const utterance = new target.SpeechSynthesisUtterance(item.hiragana);
    utterance.lang = "ja-JP";
    utterance.rate = 0.82;
    utterance.pitch = 1;

    const voice = getJapaneseVoice(target);
    if (voice) {
      utterance.voice = voice;
    }

    target.speechSynthesis.speak(utterance);
    return true;
  }

  return {
    canSpeak,
    getJapaneseVoice,
    speakKana,
  };
});
