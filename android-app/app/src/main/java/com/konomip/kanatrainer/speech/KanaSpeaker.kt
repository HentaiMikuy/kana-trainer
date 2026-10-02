package com.konomip.kanatrainer.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * 日语发音封装，对应 Web 版 speech.js：
 * 使用平假名文本、ja-JP 语言和固定语速 0.82 播放假名读音。
 * 引擎初始化完成前的播放请求会先暂存，初始化完成后补播。
 */
class KanaSpeaker(context: Context) {

    private var tts: TextToSpeech? = null
    private var pendingText: String? = null

    private val _available = MutableStateFlow(false)

    /** TTS 引擎是否初始化成功；不可用时调用方应禁用发音入口。 */
    val available: StateFlow<Boolean> = _available.asStateFlow()

    private val initListener = TextToSpeech.OnInitListener { status ->
        _available.value = status == TextToSpeech.SUCCESS
        val text = pendingText
        pendingText = null
        if (_available.value && text != null) {
            speak(text)
        }
    }

    init {
        tts = TextToSpeech(context.applicationContext, initListener)
    }

    fun speak(hiraganaText: String) {
        if (!_available.value) {
            pendingText = hiraganaText
            return
        }
        val engine = tts ?: return
        engine.stop()
        engine.language = Locale.JAPAN
        engine.setSpeechRate(SPEECH_RATE)
        engine.setPitch(PITCH)
        engine.speak(hiraganaText, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        _available.value = false
    }

    companion object {
        const val SPEECH_RATE = 0.82f
        const val PITCH = 1.0f
        private const val UTTERANCE_ID = "kana-speech"
    }
}
