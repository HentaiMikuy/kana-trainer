package com.konomip.kanatrainer.speech

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.util.Locale

/**
 * 日语发音封装：
 *
 * 1. 优先播放 assets/audio/kana/ 下的 Nanami 合成音频（按罗马音命名，如 ka.ogg；
 *    "ji/di" 这类多写法罗马音取斜杠前的主写法）；
 * 2. 录音文件缺失或加载失败时回退到系统 TTS（平假名文本、ja-JP、语速 0.82），
 *    行为与 Web 版 speech.js 一致；
 * 3. TTS 引擎初始化完成前的 TTS 请求会先暂存，初始化完成后补播。
 *    录音播放不依赖 TTS，即使设备没有日语语音包也能发音。
 */
class KanaSpeaker(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var pendingText: String? = null

    private var soundPool: SoundPool? = null
    private val loadedSounds = HashMap<String, Int>()
    private val pendingLoads = HashMap<Int, String>()
    private val missingAudio = HashSet<String>()

    private val _available = MutableStateFlow(false)

    /** 是否具备发音能力（录音存在或 TTS 可用）；不可用时调用方应禁用发音入口。 */
    val available: StateFlow<Boolean> = _available.asStateFlow()

    private val hasAudioAssets: Boolean by lazy {
        runCatching {
            context.assets.list(AUDIO_DIR)?.isNotEmpty() == true
        }.getOrDefault(false)
    }

    private val initListener = TextToSpeech.OnInitListener { status ->
        ttsReady = status == TextToSpeech.SUCCESS
        _available.value = ttsReady || hasAudioAssets
        val text = pendingText
        pendingText = null
        if (ttsReady && text != null) {
            speakWithTts(text)
        }
    }

    init {
        if (hasAudioAssets) {
            _available.value = true
        }
        tts = TextToSpeech(context.applicationContext, initListener)
    }

    fun speak(hiraganaText: String, audioKey: String? = null) {
        if (audioKey != null && tryPlayRecording(audioKey)) return
        if (!ttsReady) {
            pendingText = hiraganaText
            return
        }
        speakWithTts(hiraganaText)
    }

    // ---------- 内置音频（SoundPool） ----------

    /** 返回 true 表示录音已在播放或正在加载（加载完成后会自动补播）。 */
    private fun tryPlayRecording(key: String): Boolean {
        val pool = ensureSoundPool()

        loadedSounds[key]?.let { soundId ->
            pool.play(soundId, 1f, 1f, 1, 0, 1f)
            return true
        }
        if (key in missingAudio || pendingLoads.containsValue(key)) {
            return key !in missingAudio
        }

        return try {
            val afd = context.assets.openFd("$AUDIO_DIR/$key.ogg")
            val soundId = pool.load(afd, 1)
            afd.close()
            pendingLoads[soundId] = key
            true
        } catch (e: IOException) {
            missingAudio += key
            false
        }
    }

    private fun ensureSoundPool(): SoundPool {
        soundPool?.let { return it }
        val pool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            .build()
        pool.setOnLoadCompleteListener { _, soundId, status ->
            val key = pendingLoads.remove(soundId) ?: return@setOnLoadCompleteListener
            if (status == 0) {
                loadedSounds[key] = soundId
                soundPool?.play(soundId, 1f, 1f, 1, 0, 1f)
            } else {
                Log.w(TAG, "音频加载失败: $key (status=$status)")
                missingAudio += key
            }
        }
        soundPool = pool
        return pool
    }

    // ---------- 系统 TTS ----------

    private fun speakWithTts(hiraganaText: String) {
        val engine = tts ?: return
        if (engine.isLanguageAvailable(Locale.JAPAN) < TextToSpeech.LANG_AVAILABLE) {
            // 设备没有日语语音包时静默跳过，交由录音覆盖常见设备
            return
        }
        engine.stop()
        engine.language = Locale.JAPAN
        engine.setSpeechRate(SPEECH_RATE)
        engine.setPitch(PITCH)
        engine.speak(hiraganaText, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    fun stop() {
        soundPool?.autoPause()
        tts?.stop()
    }

    fun shutdown() {
        soundPool?.release()
        soundPool = null
        loadedSounds.clear()
        pendingLoads.clear()
        tts?.stop()
        tts?.shutdown()
        tts = null
        ttsReady = false
        _available.value = false
    }

    companion object {
        const val SPEECH_RATE = 0.82f
        const val PITCH = 1.0f
        private const val AUDIO_DIR = "audio/kana"
        private const val UTTERANCE_ID = "kana-speech"
        private const val TAG = "KanaSpeaker"
    }
}
