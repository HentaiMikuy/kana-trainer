const assert = require("node:assert/strict");
const speech = require("../speech.js");

function test(name, fn) {
  try {
    fn();
    console.log(`ok - ${name}`);
  } catch (error) {
    console.error(`not ok - ${name}`);
    throw error;
  }
}

function createSpeechRoot(voices = []) {
  const calls = {
    cancel: 0,
    spoken: [],
  };

  class FakeUtterance {
    constructor(text) {
      this.text = text;
      this.lang = "";
      this.rate = 1;
      this.pitch = 1;
      this.voice = null;
    }
  }

  return {
    calls,
    SpeechSynthesisUtterance: FakeUtterance,
    speechSynthesis: {
      cancel() {
        calls.cancel += 1;
      },
      getVoices() {
        return voices;
      },
      speak(utterance) {
        calls.spoken.push(utterance);
      },
    },
  };
}

test("detects speech synthesis support", () => {
  assert.equal(speech.canSpeak({}), false);
  assert.equal(
    speech.canSpeak({
      speechSynthesis: {},
      SpeechSynthesisUtterance: function SpeechSynthesisUtterance() {},
    }),
    true,
  );
});

test("selects the first Japanese voice", () => {
  const englishVoice = { lang: "en-US", name: "English" };
  const japaneseVoice = { lang: "ja-JP", name: "Japanese" };
  const root = createSpeechRoot([englishVoice, japaneseVoice]);

  assert.equal(speech.getJapaneseVoice(root), japaneseVoice);
});

test("reports unsupported speech without speaking", () => {
  let unsupportedCount = 0;

  const spoken = speech.speakKana(
    { hiragana: "し" },
    {
      root: {},
      onUnsupported() {
        unsupportedCount += 1;
      },
    },
  );

  assert.equal(spoken, false);
  assert.equal(unsupportedCount, 1);
});

test("speaks hiragana with Japanese utterance settings", () => {
  const voice = { lang: "ja-JP", name: "Japanese" };
  const root = createSpeechRoot([voice]);

  const spoken = speech.speakKana({ hiragana: "し" }, { root });

  assert.equal(spoken, true);
  assert.equal(root.calls.cancel, 1);
  assert.equal(root.calls.spoken.length, 1);

  const [utterance] = root.calls.spoken;
  assert.equal(utterance.text, "し");
  assert.equal(utterance.lang, "ja-JP");
  assert.equal(utterance.rate, 0.82);
  assert.equal(utterance.pitch, 1);
  assert.equal(utterance.voice, voice);
});

