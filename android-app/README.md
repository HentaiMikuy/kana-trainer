# 五十音记忆测试 · Android 版

`../`（仓库根目录）中纯前端 Web 版的**原生 Android 迁移重构**：全部业务逻辑与 UI 用
Kotlin + Jetpack Compose 重写，不再依赖 WebView。学习记录的导出/导入 JSON 与 Web 版
互通，可以在浏览器和手机之间迁移学习进度。

## 功能对照

Web 版已实现的能力在安卓版中全部保留：

| 能力 | Web 版实现 | Android 版实现 |
| --- | --- | --- |
| 题库 | `kana-core.js`（清音/浊音/半浊音/拗音/易混淆分组，104 项） | `data/KanaData.kt` |
| 判题规则 | 大小写宽容、`ji/di`、`zu/du` 多写法 | `logic/AnswerRules.kt` |
| 学习记录 | `localStorage` + `learning-records.js` | JSON 文件（`filesDir`）+ `data/LearningRecords.kt`、`RecordStore.kt` |
| 出题引擎 | `quiz-engine.js`（练习池、加权队列、选项生成） | `logic/QuizEngine.kt` |
| 练习规划 | `practice-planner.js`（标签、易混淆干扰项） | `logic/PracticePlanner.kt` |
| 会话统计 | `practice-session.js`（正确率、平均用时、错题复习题量） | `logic/PracticeSession.kt` |
| 智能复习权重 | `calculateReviewWeight()` | `logic/ReviewWeight.kt` |
| 限时挑战 | `challenge-timer.js`（倒计时、超时判错） | Compose `LaunchedEffect` 倒计时（`QuizScreen.kt`） |
| 发音 | Web Speech API（`speech.js`） | Android 原生 `TextToSpeech`，ja-JP、语速 0.82（`speech/KanaSpeaker.kt`） |
| 页面 UI | `index.html` + `styles.css` + DOM 渲染模块 | Jetpack Compose（Material 3，配色复刻 Web 版） |
| 数据导入导出 | 浏览器下载/文件选择 | SAF（`CreateDocument` / `OpenDocument`），payload 格式与 Web 版一致 |

练习模式、出题方向、答题方式、题量与限时设置、五十音行选择、易混淆专项、错题复习、
学习报告、速查表掌握状态（新/练/弱/稳）、遮挡、字形切换（印刷体/手写体）等全部可用。

## 与 Web 版的行为差异

- **导航形态**：Web 版单页并排展示设置与答题区；Android 版按移动端习惯拆分为
  设置 / 答题 / 速查表 / 错题本四个页面，系统返回键可逐级返回。
- **字形显示**：Web 版手写体依赖系统字体（Klee One 等）；Android 版手写体使用系统
  衬线字体近似楷书效果，未捆绑字体文件以控制包体积。
- **遮挡效果**：Web 版用 CSS `blur`；Android 版用低不透明度（12%）呈现，兼容
  API 26–30（`Modifier.blur` 需 API 31+）。
- **存储位置**：Web 版存 `localStorage`；Android 版存应用私有目录
  `files/kana-trainer-learning-records-v1.json`（同为记录数组 JSON）。
- 其余判题、统计、权重、错题规则与 Web 版逐行对应，并有单元测试固定行为。

## 构建要求

- JDK 17+（Android Studio 自带 JBR 即可）
- Android SDK Platform 37、Build-Tools（`local.properties` 指向 SDK，或设置 `ANDROID_HOME`）
- 无需手动安装 Gradle，使用项目自带 wrapper（Gradle 9.7 + AGP 9.3 + Kotlin 2.3）

```bash
# 运行单元测试（48 个，覆盖题库/判题/出题/记录/权重/会话/规划/JSON 编解码）
./gradlew :app:testDebugUnitTest

# 构建调试 APK
./gradlew :app:assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

macOS 上若默认 JDK 版本不合适，可显式指定 Android Studio 的 JBR：

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDebug
```

安装到设备：`adb install -r app/build/outputs/apk/debug/app-debug.apk`。

## 目录结构

```text
android-app/
├── app/src/main/java/com/konomip/kanatrainer/
│   ├── MainActivity.kt            # 单 Activity 入口 + 页面导航 + Snackbar
│   ├── data/
│   │   ├── KanaData.kt            # 题库、易混淆分组、速查表分区、洗牌（自 kana-core.js）
│   │   ├── LearningRecords.kt     # 记录规范化、长期薄弱项、导入合并、导出 payload
│   │   ├── RecordJson.kt          # kotlinx.serialization 编解码（兼容 Web 导出格式）
│   │   └── RecordStore.kt         # 学习记录文件存储（原子写入、失败降级）
│   ├── logic/
│   │   ├── TrainerSettings.kt     # 练习设置模型与枚举（默认值同 Web 版初始 state）
│   │   ├── AnswerRules.kt         # 答案兼容规则（多写法、大小写、空白）
│   │   ├── QuizEngine.kt          # 练习池、题干、选项、加权队列
│   │   ├── ReviewWeight.kt        # 智能复习权重
│   │   ├── PracticeSession.kt     # 会话统计、进度、错题复习题量
│   │   └── PracticePlanner.kt     # 练习标签、练习池选择、易混淆干扰项
│   ├── speech/KanaSpeaker.kt      # 原生 TTS 封装
│   └── ui/
│       ├── KanaTrainerViewModel.kt # 页面状态与流程编排（自 app.js）
│       ├── theme/Theme.kt          # 配色复刻 Web 版 styles.css 设计变量
│       ├── components/Common.kt    # 统计条、设置控件等共享组件
│       ├── help/KanaHelpDialog.kt  # 页面说明弹窗
│       └── screens/                # Settings / Quiz（含学习报告）/ Chart / Weak
└── app/src/test/java/com/konomip/kanatrainer/
    ├── data/                       # KanaDataTest、LearningRecordsTest、RecordJsonTest
    └── logic/                      # AnswerRules、QuizEngine、ReviewWeight、
                                    # PracticeSession、PracticePlanner 测试
```

## 测试与既有 JS 测试的对应

`tests/*.test.js` 中的核心断言已移植为 JUnit 测试：题库数量（104）、答案多写法、
导入规范化与未知记录剔除、长期薄弱项规则、练习结果写入与连对重置、导出 payload 与
文件名时间戳、练习池行过滤、选项去重与易混淆干扰项、加权队列出题、错题复习题量
clamp、复习权重上下界与空记录加成等。UI 渲染与浏览器事件类 JS 测试不适用于原生
UI，由模拟器手工冒烟测试覆盖（安装、答题、判题反馈、错题本、速查表、学习报告）。

## 已知限制

- 手写体为衬线近似，未捆绑 Klee One 等字体。
- 遮挡在 Android 11 及以下用半透明而非高斯模糊实现。
- 设置项暂不持久化（与 Web 版一致，仅学习记录持久化）。
- 发音依赖设备日语 TTS 数据；未安装时发音按钮禁用，其余功能不受影响。
