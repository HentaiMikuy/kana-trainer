package com.konomip.kanatrainer.data

import kotlin.math.floor
import kotlin.random.Random

/** 假名显示脚本，对应 Web 版的 "hiragana" / "katakana" 字符串。 */
enum class Script(val id: String) {
    HIRAGANA("hiragana"),
    KATAKANA("katakana");

    companion object {
        fun fromId(id: String): Script? = entries.firstOrNull { it.id == id }
    }
}

/** 题库中的单个假名，字段与 Web 版 KanaCore.toItem 产物一致。 */
data class KanaItem(
    val romaji: String,
    val hiragana: String,
    val katakana: String,
    val group: String,
    val forcedScript: Script? = null,
) {
    /** 与 Web 版 getItemKey 保持一致：`hiragana|katakana|romaji`。 */
    val key: String get() = "$hiragana|$katakana|$romaji"
}

data class BaseRow(val id: String, val label: String, val items: List<RawKana>)

data class ConfusingSet(
    val id: String,
    val label: String,
    val script: Script,
    val items: List<String>,
)

/** 题库原始条目；group 非空时表示浊音/拗音所属的行字母。 */
data class RawKana(
    val romaji: String,
    val hiragana: String,
    val katakana: String,
    val group: String? = null,
)

data class WeakRow(val id: String, val label: String, val misses: Int, val count: Int)

data class ChartRowGroup(val id: String, val label: String, val items: List<KanaItem>)

data class ChartSection(val key: String, val title: String, val items: List<KanaItem>)

/** 本轮错题条目，对应 Web 版 rememberRoundMistake 写入的结构。 */
data class RoundMistake(
    val item: KanaItem,
    val answerScript: Script? = null,
    val misses: Int = 1,
)

/** 题库与纯逻辑工具，迁移自 kana-core.js。 */
object KanaData {

    val BASE_ROWS: List<BaseRow> = listOf(
        BaseRow(
            "a", "あ",
            listOf(
                RawKana("a", "あ", "ア"),
                RawKana("i", "い", "イ"),
                RawKana("u", "う", "ウ"),
                RawKana("e", "え", "エ"),
                RawKana("o", "お", "オ"),
            ),
        ),
        BaseRow(
            "k", "か",
            listOf(
                RawKana("ka", "か", "カ"),
                RawKana("ki", "き", "キ"),
                RawKana("ku", "く", "ク"),
                RawKana("ke", "け", "ケ"),
                RawKana("ko", "こ", "コ"),
            ),
        ),
        BaseRow(
            "s", "さ",
            listOf(
                RawKana("sa", "さ", "サ"),
                RawKana("shi", "し", "シ"),
                RawKana("su", "す", "ス"),
                RawKana("se", "せ", "セ"),
                RawKana("so", "そ", "ソ"),
            ),
        ),
        BaseRow(
            "t", "た",
            listOf(
                RawKana("ta", "た", "タ"),
                RawKana("chi", "ち", "チ"),
                RawKana("tsu", "つ", "ツ"),
                RawKana("te", "て", "テ"),
                RawKana("to", "と", "ト"),
            ),
        ),
        BaseRow(
            "n", "な",
            listOf(
                RawKana("na", "な", "ナ"),
                RawKana("ni", "に", "ニ"),
                RawKana("nu", "ぬ", "ヌ"),
                RawKana("ne", "ね", "ネ"),
                RawKana("no", "の", "ノ"),
            ),
        ),
        BaseRow(
            "h", "は",
            listOf(
                RawKana("ha", "は", "ハ"),
                RawKana("hi", "ひ", "ヒ"),
                RawKana("fu", "ふ", "フ"),
                RawKana("he", "へ", "ヘ"),
                RawKana("ho", "ほ", "ホ"),
            ),
        ),
        BaseRow(
            "m", "ま",
            listOf(
                RawKana("ma", "ま", "マ"),
                RawKana("mi", "み", "ミ"),
                RawKana("mu", "む", "ム"),
                RawKana("me", "め", "メ"),
                RawKana("mo", "も", "モ"),
            ),
        ),
        BaseRow(
            "y", "や",
            listOf(
                RawKana("ya", "や", "ヤ"),
                RawKana("yu", "ゆ", "ユ"),
                RawKana("yo", "よ", "ヨ"),
            ),
        ),
        BaseRow(
            "r", "ら",
            listOf(
                RawKana("ra", "ら", "ラ"),
                RawKana("ri", "り", "リ"),
                RawKana("ru", "る", "ル"),
                RawKana("re", "れ", "レ"),
                RawKana("ro", "ろ", "ロ"),
            ),
        ),
        BaseRow(
            "w", "わ",
            listOf(
                RawKana("wa", "わ", "ワ"),
                RawKana("wo", "を", "ヲ"),
                RawKana("n", "ん", "ン"),
            ),
        ),
    )

    val DAKUTEN_ITEMS: List<RawKana> = listOf(
        RawKana("ga", "が", "ガ", "k"),
        RawKana("gi", "ぎ", "ギ", "k"),
        RawKana("gu", "ぐ", "グ", "k"),
        RawKana("ge", "げ", "ゲ", "k"),
        RawKana("go", "ご", "ゴ", "k"),
        RawKana("za", "ざ", "ザ", "s"),
        RawKana("ji", "じ", "ジ", "s"),
        RawKana("zu", "ず", "ズ", "s"),
        RawKana("ze", "ぜ", "ゼ", "s"),
        RawKana("zo", "ぞ", "ゾ", "s"),
        RawKana("da", "だ", "ダ", "t"),
        RawKana("ji/di", "ぢ", "ヂ", "t"),
        RawKana("zu/du", "づ", "ヅ", "t"),
        RawKana("de", "で", "デ", "t"),
        RawKana("do", "ど", "ド", "t"),
        RawKana("ba", "ば", "バ", "h"),
        RawKana("bi", "び", "ビ", "h"),
        RawKana("bu", "ぶ", "ブ", "h"),
        RawKana("be", "べ", "ベ", "h"),
        RawKana("bo", "ぼ", "ボ", "h"),
        RawKana("pa", "ぱ", "パ", "h"),
        RawKana("pi", "ぴ", "ピ", "h"),
        RawKana("pu", "ぷ", "プ", "h"),
        RawKana("pe", "ぺ", "ペ", "h"),
        RawKana("po", "ぽ", "ポ", "h"),
    )

    val SMALL_KANA_ITEMS: List<RawKana> = listOf(
        RawKana("kya", "きゃ", "キャ", "k"),
        RawKana("kyu", "きゅ", "キュ", "k"),
        RawKana("kyo", "きょ", "キョ", "k"),
        RawKana("sha", "しゃ", "シャ", "s"),
        RawKana("shu", "しゅ", "シュ", "s"),
        RawKana("sho", "しょ", "ショ", "s"),
        RawKana("cha", "ちゃ", "チャ", "t"),
        RawKana("chu", "ちゅ", "チュ", "t"),
        RawKana("cho", "ちょ", "チョ", "t"),
        RawKana("nya", "にゃ", "ニャ", "n"),
        RawKana("nyu", "にゅ", "ニュ", "n"),
        RawKana("nyo", "にょ", "ニョ", "n"),
        RawKana("hya", "ひゃ", "ヒャ", "h"),
        RawKana("hyu", "ひゅ", "ヒュ", "h"),
        RawKana("hyo", "ひょ", "ヒョ", "h"),
        RawKana("mya", "みゃ", "ミャ", "m"),
        RawKana("myu", "みゅ", "ミュ", "m"),
        RawKana("myo", "みょ", "ミョ", "m"),
        RawKana("rya", "りゃ", "リャ", "r"),
        RawKana("ryu", "りゅ", "リュ", "r"),
        RawKana("ryo", "りょ", "リョ", "r"),
        RawKana("gya", "ぎゃ", "ギャ", "k"),
        RawKana("gyu", "ぎゅ", "ギュ", "k"),
        RawKana("gyo", "ぎょ", "ギョ", "k"),
        RawKana("ja", "じゃ", "ジャ", "s"),
        RawKana("ju", "じゅ", "ジュ", "s"),
        RawKana("jo", "じょ", "ジョ", "s"),
        RawKana("bya", "びゃ", "ビャ", "h"),
        RawKana("byu", "びゅ", "ビュ", "h"),
        RawKana("byo", "びょ", "ビョ", "h"),
        RawKana("pya", "ぴゃ", "ピャ", "h"),
        RawKana("pyu", "ぴゅ", "ピュ", "h"),
        RawKana("pyo", "ぴょ", "ピョ", "h"),
    )

    val CONFUSING_KANA_SETS: List<ConfusingSet> = listOf(
        ConfusingSet("shi-tsu", "シ / ツ", Script.KATAKANA, listOf("shi", "tsu")),
        ConfusingSet("so-n", "ソ / ン", Script.KATAKANA, listOf("so", "n")),
        ConfusingSet("no-me", "ノ / メ", Script.KATAKANA, listOf("no", "me")),
        ConfusingSet("nu-me", "ぬ / め", Script.HIRAGANA, listOf("nu", "me")),
        ConfusingSet("sa-chi", "さ / ち", Script.HIRAGANA, listOf("sa", "chi")),
        ConfusingSet("re-wa-ne", "れ / わ / ね", Script.HIRAGANA, listOf("re", "wa", "ne")),
    )

    /** 与 Web 版 toItem 一致：行内条目继承行 id，浊音/拗音条目自带 group。 */
    fun toItem(raw: RawKana, group: String = "base"): KanaItem =
        KanaItem(
            romaji = raw.romaji,
            hiragana = raw.hiragana,
            katakana = raw.katakana,
            group = raw.group ?: group,
        )

    fun withQuestionScript(item: KanaItem, script: Script): KanaItem =
        item.copy(forcedScript = script)

    fun getAllPracticeItems(): List<KanaItem> =
        BASE_ROWS.flatMap { row -> row.items.map { toItem(it, row.id) } } +
            DAKUTEN_ITEMS.map { toItem(it, "dakuten") } +
            SMALL_KANA_ITEMS.map { toItem(it, "small") }

    fun getCanonicalPracticeItem(record: LearningRecord): KanaItem? {
        val key = "${record.hiragana}|${record.katakana}|${record.romaji}"
        return getAllPracticeItems().firstOrNull { it.key == key }
    }

    fun getItemByRomaji(romaji: String): KanaItem? =
        getAllPracticeItems().firstOrNull { it.romaji == romaji }

    fun getConfusingSetById(id: String): ConfusingSet? =
        CONFUSING_KANA_SETS.firstOrNull { it.id == id }

    fun getSelectedConfusingSets(selectedConfusingSet: String = "all"): List<ConfusingSet> =
        if (selectedConfusingSet == "all") {
            CONFUSING_KANA_SETS
        } else {
            CONFUSING_KANA_SETS.filter { it.id == selectedConfusingSet }
        }

    fun getConfusingPool(selectedConfusingSet: String = "all"): List<KanaItem> {
        val keyedItems = LinkedHashMap<String, KanaItem>()
        getSelectedConfusingSets(selectedConfusingSet).forEach { set ->
            set.items.forEach { romaji ->
                getItemByRomaji(romaji)?.let { item ->
                    keyedItems["${set.id}:${item.key}"] = withQuestionScript(item, set.script)
                }
            }
        }
        return keyedItems.values.toList()
    }

    fun getRowName(groupId: String): String = when (groupId) {
        "dakuten" -> "浊音"
        "small" -> "拗音"
        else -> BASE_ROWS.firstOrNull { it.id == groupId }?.label ?: groupId
    }

    fun getGroupLabel(groupId: String): String = when (groupId) {
        "dakuten" -> "浊音组"
        "small" -> "拗音组"
        else -> "${getRowName(groupId)}行"
    }

    fun getWeakRows(mistakes: List<RoundMistake>): List<WeakRow> {
        val rows = LinkedHashMap<String, WeakRow>()
        mistakes.forEach { mistake ->
            val item = mistake.item
            val existing = rows[item.group]
            rows[item.group] = if (existing == null) {
                WeakRow(item.group, getGroupLabel(item.group), mistake.misses, 1)
            } else {
                existing.copy(misses = existing.misses + mistake.misses, count = existing.count + 1)
            }
        }
        return rows.values.sortedWith(
            compareByDescending<WeakRow> { it.misses }.thenByDescending { it.count },
        )
    }

    fun groupChartItemsByRow(items: List<KanaItem>): List<ChartRowGroup> {
        val rows = LinkedHashMap<String, ChartRowGroup>()
        items.forEach { item ->
            val existing = rows[item.group]
            rows[item.group] = if (existing == null) {
                ChartRowGroup(item.group, getGroupLabel(item.group), listOf(item))
            } else {
                existing.copy(items = existing.items + item)
            }
        }
        return rows.values.toList()
    }

    fun getChartSections(): List<ChartSection> {
        val voicedDakuten = DAKUTEN_ITEMS
            .filter { !it.romaji.startsWith("p") }
            .map { toItem(it, it.group ?: "base") }
        val semiDakuten = DAKUTEN_ITEMS
            .filter { it.romaji.startsWith("p") }
            .map { toItem(it, it.group ?: "base") }

        return listOf(
            ChartSection(
                "base",
                "清音",
                BASE_ROWS.flatMap { row -> row.items.map { toItem(it, row.id) } },
            ),
            ChartSection("dakuten", "浊音", voicedDakuten),
            ChartSection("semi", "半浊音", semiDakuten),
            ChartSection("small", "拗音", SMALL_KANA_ITEMS.map { toItem(it, "small") }),
        )
    }

    /** 错题优先排序：misses 多在前，其次 attempts 多，最后按 romaji 升序。 */
    fun sortByMissPriorityRecords(items: List<LearningRecord>): List<LearningRecord> =
        items.sortedWith(
            compareByDescending<LearningRecord> { it.misses }
                .thenByDescending { it.attempts }
                .thenBy { it.romaji },
        )

    fun sortByMissPriorityRoundMistakes(items: List<RoundMistake>): List<RoundMistake> =
        items.sortedWith(
            compareByDescending<RoundMistake> { it.misses }
                .thenBy { it.item.romaji },
        )

    /** 无偏洗牌，与 Web 版 KanaCore.shuffle 保持同一算法，不修改输入列表。 */
    fun <T> shuffle(items: List<T>, random: Random = Random.Default): List<T> {
        val result = items.toMutableList()
        for (index in result.size - 1 downTo 1) {
            val swapIndex = floor(random.nextDouble() * (index + 1)).toInt()
            val temp = result[index]
            result[index] = result[swapIndex]
            result[swapIndex] = temp
        }
        return result
    }
}
