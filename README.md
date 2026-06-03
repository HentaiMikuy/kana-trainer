# 五十音记忆测试

一个纯前端的日语假名练习工具，用于记忆平假名、片假名及其罗马音读法。项目不依赖构建工具、包管理器或后端服务，直接在浏览器中打开 `index.html` 即可使用。

## 当前实现概览

当前代码已经实现了一个完整可用的假名练习应用，核心能力集中在：

- 题库：清音、浊音、半浊音、拗音，以及常见易混淆假名分组。
- 练习模式：普通练习、易混淆专项、长期错题复习、本轮错题复习。
- 出题方向：假名到罗马音、罗马音到假名。
- 答题方式：选择题和手动输入。
- 学习记录：通过 `localStorage` 保存每个假名的练习次数、正确次数、错误次数、连续掌握次数和最近练习时间。
- 智能复习：普通练习会根据历史错题、掌握连续次数和久未练习时间进行加权抽样。
- 复盘反馈：顶部实时统计、完成页学习报告、长期薄弱项列表和带掌握状态的五十音速查表。
- 辅助体验：页面说明弹窗、限时挑战、平均用时统计、手动/自动发音、印刷体/手写体切换、速查/错题遮挡和移动端操作区优化。

项目目前是单页静态应用，运行时仍不需要构建流程。纯题库和基础记录规则集中在 `kana-core.js`，学习记录存储、导出 payload、导入合并和练习结果写入集中在 `learning-records.js`，出题队列和选项生成逻辑集中在 `quiz-engine.js`，完成页、错题列表、反馈和速查表渲染集中在 `ui-renderers.js`，发音能力集中在 `speech.js`，限时挑战集中在 `challenge-timer.js`，设置区控件同步集中在 `settings-controls.js`，题目区 DOM 渲染集中在 `question-view.js`，按钮和键盘事件绑定集中在 `app-events.js`，学习数据导出下载、导入文件解析和清空确认集中在 `learning-data-actions.js`，练习会话统计、进度和错题复习题量计算集中在 `practice-session.js`，练习标签、练习池、易混淆干扰项、选项和队列规划集中在 `practice-planner.js`，`app.js` 负责页面状态和跨模块流程编排。核心规则使用 Node 原生断言测试覆盖，不依赖第三方测试框架。

## 功能特性

- 支持片假名、平假名、混合三种普通练习范围。
- 支持「假名 → 罗马音」和「罗马音 → 假名」两种测试方向。
- 支持选择题和输入题两种答题方式。
- 输入模式下支持大小写宽容、空白清理，以及 `ji/di`、`zu/du` 等多写法罗马音。
- 支持易混淆专项训练：
  - `シ / ツ`
  - `ソ / ン`
  - `ノ / メ`
  - `ぬ / め`
  - `さ / ち`
  - `れ / わ / ね`
- 支持按五十音行选择练习范围。
- 支持选择是否包含浊音、半浊音和拗音。
- 支持每轮题量设置，范围为 10 到 50 题。
- 支持限时挑战，每题可设置 5 到 30 秒，超时自动判错。
- 顶部显示已答题数、正确率、连续答对数和平均答题用时。
- 每轮结束后生成学习报告，展示本轮正确率、答对/答错题数、平均用时、本轮错题和薄弱行。
- 支持从完成页直接进入「再练本轮错题」「重新随机一轮」「只练长期薄弱项」。
- 支持长期错题列表，并可一键只练长期错题。
- 支持将学习记录导出为 JSON，也支持从 JSON 导入记录。
- 内置五十音速查表，按清音、浊音、半浊音、拗音分区展示。
- 速查表会根据本地记录显示「新」「练」「弱」「稳」掌握状态。
- 支持点击速查表单元格播放发音。
- 支持对「五十音速查」和「容易忘的假名」内容执行「遮挡 / 显示」切换，答题时可通过模糊内容避免提前看到答案。
- 顶部标题右侧提供「?」页面说明按钮，弹窗介绍页面区域和主要功能。
- 支持浏览器语音合成，可手动播放或答题后自动播放日语发音。
- 支持印刷体和手写体字形显示切换。
- 支持键盘操作：
  - 选择题模式下按数字键 `1` 到 `8` 选择答案。
  - 输入模式下按 `Enter` 提交。
  - 答题完成后按 `Enter` 进入下一题或查看结果。
  - 页面说明弹窗打开时可按 `Esc` 关闭。

## 使用方式

直接用浏览器打开项目中的 `index.html` 文件即可：

```text
index.html
```

也可以在当前目录启动一个本地静态服务：

```bash
python3 -m http.server 8000
```

然后访问：

```text
http://localhost:8000
```

## 测试方式

项目没有包管理器依赖，核心逻辑测试可以直接用 Node 运行：

```bash
node tests/kana-core.test.js
node tests/learning-records.test.js
node tests/quiz-engine.test.js
node tests/ui-renderers.test.js
node tests/speech.test.js
node tests/challenge-timer.test.js
node tests/settings-controls.test.js
node tests/question-view.test.js
node tests/app-events.test.js
node tests/learning-data-actions.test.js
node tests/practice-session.test.js
node tests/practice-planner.test.js
```

也可以单独做 JavaScript 语法检查：

```bash
node --check kana-core.js
node --check learning-records.js
node --check quiz-engine.js
node --check ui-renderers.js
node --check speech.js
node --check challenge-timer.js
node --check settings-controls.js
node --check question-view.js
node --check app-events.js
node --check learning-data-actions.js
node --check practice-session.js
node --check practice-planner.js
node --check app.js
node --check tests/kana-core.test.js
node --check tests/learning-records.test.js
node --check tests/quiz-engine.test.js
node --check tests/ui-renderers.test.js
node --check tests/speech.test.js
node --check tests/challenge-timer.test.js
node --check tests/settings-controls.test.js
node --check tests/question-view.test.js
node --check tests/app-events.test.js
node --check tests/learning-data-actions.test.js
node --check tests/practice-session.test.js
node --check tests/practice-planner.test.js
```

## 操作流程

1. 在「练习设置」中选择测试字符、字形显示、发音方式、测试方向、答题方式和练习类型。
2. 普通练习下，可选择五十音行，并决定是否包含浊音、半浊音和拗音。
3. 易混淆专项下，可选择全部易混淆分组或单独分组。
4. 通过题量滑块设置本轮题数。
5. 如需限时答题，开启「限时挑战」并设置每题秒数。
6. 点击「开始新一轮」生成练习。
7. 答题后可查看反馈，点击「下一题」继续。
8. 练习结束后在学习报告中查看错题、薄弱行，并选择下一步复习入口。
9. 答题时如果不希望看到提示，可点击「五十音速查」或「容易忘的假名」中的「遮挡」按钮模糊对应内容，再点击「显示」恢复。
10. 可点击标题右侧的「?」查看页面说明，说明弹窗支持关闭按钮、点击遮罩和 `Esc` 关闭。
11. 可通过「导出」「导入」迁移当前浏览器里的学习记录。
12. 如需重置长期记录，点击「清空记录」删除当前浏览器保存的数据。

## 文件结构

```text
.
├── index.html           # 页面结构和主要界面元素
├── styles.css           # 页面布局、视觉样式、响应式适配和状态样式
├── kana-core.js         # 题库、答案兼容、记录规范化、复习权重和纯逻辑工具
├── learning-records.js  # 学习记录存储、导入导出、结果写入和薄弱项筛选
├── quiz-engine.js       # 练习池、题目队列、题目生成和选项生成逻辑
├── ui-renderers.js      # 完成页、错题列表、反馈和速查表 DOM 渲染
├── speech.js            # 浏览器语音合成能力检测、日语语音选择和播放
├── challenge-timer.js   # 限时挑战倒计时、显示更新、耗时记录和超时回调
├── settings-controls.js # 设置区控件同步、分段按钮状态和行选择渲染
├── question-view.js     # 题目区、选项、输入状态和完成态 DOM 渲染
├── app-events.js        # 按钮、表单和键盘事件绑定与回调分发
├── learning-data-actions.js # 学习数据导出下载、导入解析和清空确认
├── practice-session.js  # 练习会话统计、进度和错题复习题量计算
├── practice-planner.js  # 练习标签、练习池、干扰项、选项和队列规划
├── app.js               # 页面状态和跨模块流程编排
├── tests/
│   ├── kana-core.test.js       # 题库、基础记录和答案规则测试
│   ├── learning-records.test.js # 学习记录导入导出、存储和写入测试
│   ├── quiz-engine.test.js     # 出题队列和选项生成测试
│   ├── ui-renderers.test.js    # 反馈、错题列表和速查表渲染测试
│   ├── speech.test.js          # 语音能力检测、voice 选择和播放参数测试
│   ├── challenge-timer.test.js # 倒计时显示、耗时记录和超时测试
│   ├── settings-controls.test.js # 设置控件状态同步和行选择渲染测试
│   ├── question-view.test.js   # 题目区渲染、选项点击、输入状态和完成态测试
│   ├── app-events.test.js      # 控件事件、导入代理和键盘快捷键测试
│   ├── learning-data-actions.test.js # 学习数据动作、下载和导入状态测试
│   ├── practice-session.test.js # 练习会话统计、进度和复习题量测试
│   └── practice-planner.test.js # 练习规划、干扰项和队列上下文测试
├── FEATURE_ROADMAP.md   # 功能规划和已完成状态记录
└── README.md            # 项目说明文档
```

## 代码实现说明

### 页面结构

`index.html` 是应用唯一入口，主要分为四块：

- 顶部标题和统计区：展示页面标题、页面说明入口、已答题数、正确率、连对数和平均用时。
- 左侧设置区：控制练习模式、题量、范围、限时、导入导出等。
- 右侧练习区：第一行左侧展示答题卡，右侧展示五十音速查；第二行横向展示长期错题和薄弱项。
- 页面说明弹窗：标题右侧「?」按钮打开，用于介绍页面区域和主要功能。

页面会依次加载 `kana-core.js`、`learning-records.js`、`quiz-engine.js`、`ui-renderers.js`、`speech.js`、`challenge-timer.js`、`settings-controls.js`、`question-view.js`、`app-events.js`、`learning-data-actions.js`、`practice-session.js`、`practice-planner.js` 和 `app.js`。页面通过原生 DOM 节点 ID 与 `app.js` 绑定，没有使用框架。

### 样式实现

`styles.css` 负责完整视觉层：

- 使用 CSS 变量维护背景、文本、强调色、正确/错误状态和假名字体。
- 通过 `:root[data-glyph="hand"]` 切换手写体字体栈。
- 使用 CSS Grid 和 Flex 实现设置面板、题卡、统计条、速查表、长期错题区和移动端布局。
- 桌面端练习区采用答题卡与五十音速查并排、容易忘的假名横向铺满的布局；窄屏下恢复单列布局。
- 通过 `.privacy-blurred` 为速查表和容易忘的假名提供柔和模糊遮挡效果，并禁用遮挡内容的点击交互。
- 通过 `.modal-backdrop`、`.modal-panel`、`.help-grid` 实现页面说明弹窗和移动端单列说明布局。
- 小屏幕下将答题操作区设置为 sticky，方便移动端连续答题。

### 应用状态

`app.js` 中的 `state` 保存所有运行时状态，包括：

- 当前练习配置：字符模式、方向、答题方式、题量、选中行、是否包含浊音/拗音、是否限时等。
- 当前练习数据：题库池、选项池、题目队列、当前题、当前题序号。
- 本轮统计：已答数、正确数、连对数、总答题时间、本轮错题。
- 长期记录：从 `localStorage` 读取并保存在 `records` 和 `mistakes` 中。

状态更新后通过渲染函数同步到页面。

### 核心模块

`kana-core.js` 是可在浏览器和 Node 中复用的纯逻辑模块，负责：

- 内置题库：清音、浊音、半浊音、拗音、易混淆分组。
- 学习记录：创建记录、规范化导入记录、计算正确率、判断长期薄弱项。
- 答案规则：罗马音多写法兼容、平假名/片假名答案兼容。
- 题库辅助：生成完整题库、易混淆专项池、速查表分区和行分组。
- 复习策略：计算普通练习加权复习权重。
- 工具函数：无偏洗牌、错题排序等。

`learning-records.js` 是可在浏览器和 Node 中复用的学习记录模块，负责：

- 从 `localStorage` 加载、保存和清空学习记录，并在存储不可用时降级。
- 生成导出 payload 和文件名。
- 规范化导入记录，只接受当前题库中存在的假名。
- 合并导入记录、写入单次练习结果、筛选长期薄弱项。

`quiz-engine.js` 是可在浏览器和 Node 中复用的出题模块，负责：

- 普通练习池生成。
- 平假名、片假名和混合模式下的题目生成。
- 选择题选项生成、去重和易混淆优先干扰项。
- 按复习权重生成固定题量的题目队列。

`ui-renderers.js` 是浏览器 DOM 渲染模块，负责：

- 反馈文案和假名答案高亮。
- 完成页学习报告、错题列表和薄弱行展示。
- 长期错题列表。
- 五十音速查表、掌握状态标记和发音点击绑定。

`speech.js` 是可在浏览器和 Node 中复用的发音模块，负责：

- 检测 Web Speech API 是否可用。
- 选择系统或浏览器提供的日语语音。
- 使用平假名文本、`ja-JP` 语言和固定语速播放假名读音。
- 在不支持语音合成时向页面层返回失败状态。

`challenge-timer.js` 是可在浏览器和 Node 中复用的限时模块，负责：

- 每题开始和停止时维护内部计时状态。
- 更新倒计时文字、进度条宽度和危险状态。
- 在答题完成时回传本题耗时。
- 在倒计时归零时触发超时回调。

`settings-controls.js` 是浏览器 DOM 同步模块，负责：

- 设置区分段按钮 active 状态和 `aria-pressed` 同步。
- 题量、限时挑战、字形、发音方式、答题方式和练习类型控件同步。
- 普通练习范围行按钮渲染和点击回调转发。
- 易混淆专项模式下普通范围控件的显隐切换。

`question-view.js` 是浏览器 DOM 渲染模块，负责：

- 当前题题干、提示、题号和模式标签渲染。
- 选择题选项按钮渲染和点击回调转发。
- 输入模式显隐、placeholder、聚焦和提交后状态。
- 答题后的正确/错误状态标记。
- 练习完成时题卡外壳状态同步。

`app-events.js` 是浏览器事件绑定模块，负责：

- 设置区按钮、滑块、开关和选择框事件绑定。
- 题目操作按钮、数据导入导出按钮、速查表切换按钮和遮挡按钮事件绑定。
- 页面说明弹窗的打开、关闭、遮罩点击关闭和 `Esc` 关闭事件绑定。
- 输入模式 `Enter`、选择题数字键、答题后 `Enter` 和弹窗 `Esc` 的键盘快捷键分发。
- 屏蔽输入控件、按钮、下拉框和可编辑元素内的全局快捷键。

`learning-data-actions.js` 是浏览器学习数据动作模块，负责：

- 导出学习记录时生成 JSON 文本并触发浏览器下载。
- 导入学习记录时读取文件、解析 JSON、执行确认并把 payload 交回页面层合并。
- 清空学习记录前执行确认，并把真实清空动作交回页面层处理。
- 通过依赖注入封装 `Blob`、`URL`、`document`、`confirm` 等浏览器副作用，方便 Node 测试覆盖。

`practice-session.js` 是可在浏览器和 Node 中复用的练习会话模块，负责：

- 计算本轮正确率、进度条比例和平均答题用时。
- 答题完成后推进已答数、正确数、连对数和已选答案状态。
- 重置本轮统计、生成完成页统计和决定下一题动作。
- 计算错题复习题量，并为错题复习生成去重后的选择题候选池。

`practice-planner.js` 是可在浏览器和 Node 中复用的练习规划模块，负责：

- 根据当前模式生成练习标签和题目模式标签。
- 根据普通/易混淆设置选择练习池。
- 为易混淆专项生成优先干扰项。
- 将当前会话上下文传给题目、选项和队列生成逻辑。

`tests/kana-core.test.js`、`tests/learning-records.test.js`、`tests/quiz-engine.test.js`、`tests/ui-renderers.test.js`、`tests/speech.test.js`、`tests/challenge-timer.test.js`、`tests/settings-controls.test.js`、`tests/question-view.test.js`、`tests/app-events.test.js`、`tests/learning-data-actions.test.js`、`tests/practice-session.test.js` 和 `tests/practice-planner.test.js` 直接引用这些模块，覆盖题库数量、答案兼容、导入规范化、长期错题退出规则、复习权重、易混淆池、学习记录存储降级、练习池、题目生成、选项生成、队列长度、基础 DOM 渲染、发音参数、限时倒计时、设置控件同步、题目区渲染、按钮事件绑定、页面说明弹窗关闭路径、学习数据下载、导入状态、确认分支、会话统计、复习题量和练习规划上下文等核心行为。

### 出题流程

核心出题流程如下：

1. `quiz-engine.js` 的 `getPracticePool()` 根据普通练习设置生成题库。
2. `kana-core.js` 的 `getConfusingPool()` 根据易混淆分组生成专项题库。
3. `kana-core.js` 的 `calculateReviewWeight()` 根据历史记录计算复习权重。
4. `quiz-engine.js` 的 `buildQuestionQueue()` 根据权重和当前会话题量生成题目队列。
5. `quiz-engine.js` 的 `makeQuestion()` 根据测试方向生成题干、答案和提示文案。
6. `quiz-engine.js` 的 `makeOptions()` 为选择题生成正确答案和干扰项。

错题复习会调用 `startMistakeReview()`，直接使用本轮错题或长期错题作为出题范围，并自动将当前会话题量限制在 10 到 50 题之间，不覆盖练习设置里的题量滑块配置。

### 判题与统计

选择题和输入题最终都会进入 `completeAnswer()`：

- 判断答案是否正确。
- 更新本轮已答数、正确数、连续答对数和平均用时。
- 将结果写入长期学习记录。
- 错题会进入本轮错题和长期薄弱项。
- 页面同步显示正确/错误状态、反馈文案、下一题按钮、顶部统计和速查表掌握状态。

「看答案」和限时超时都会按错误处理并写入记录。学习记录写入由 `learning-records.js` 处理，页面层只负责同步统计和渲染反馈。

### 学习记录

学习记录保存在当前浏览器的 `localStorage` 中，键名为：

```text
kana-trainer-learning-records:v1
```

每条记录包含：

```json
{
  "romaji": "shi",
  "hiragana": "し",
  "katakana": "シ",
  "group": "s",
  "attempts": 3,
  "correct": 2,
  "misses": 1,
  "masteredStreak": 1,
  "lastPracticedAt": "2026-06-02T00:00:00.000Z"
}
```

说明：

- 刷新页面不会清空历史记录。
- 点击「开始新一轮」只重置当前轮统计，不清空长期学习记录。
- 点击「清空记录」会删除本地保存的错题和历史正确率。
- 导出文件为 JSON，包含 `schemaVersion`、`exportedAt` 和 `records`。
- 导入时只接受当前题库中存在的假名记录。
- 导入会合并到当前记录，同一假名以导入文件中的记录为准。
- 如果浏览器禁用或限制 `localStorage`，当前练习仍可继续，但刷新后不会保留学习记录。

### 发音能力

发音基于浏览器 Web Speech API：

- 如果浏览器支持 `speechSynthesis` 和 `SpeechSynthesisUtterance`，可以播放当前假名读音。
- 系统或浏览器存在日语语音时，会优先使用 `ja` 语言语音。
- 播放文本使用平假名，语言设置为 `ja-JP`。
- 如果浏览器不支持语音合成，练习和统计功能仍可正常使用。

## 浏览器和环境要求

- 不需要安装依赖。
- 浏览器使用不需要 Node.js 或构建工具。
- 运行测试需要本机安装 Node.js。
- 需要现代浏览器支持原生 JavaScript、CSS Grid、`localStorage`。
- 发音功能依赖浏览器和系统语音能力，不保证所有环境都有日语语音。
- 学习记录只保存在当前浏览器和当前站点上下文中，不会自动跨设备同步。

## 已知限制

- 当前自动化测试覆盖核心规则、学习记录、出题、基础渲染、发音模块、限时模块、设置控件同步、题目区渲染、事件绑定、学习数据动作、练习会话统计和练习规划，完整浏览器交互仍主要依赖手动验证。
- `app.js` 仍承担页面状态和跨模块流程编排，部分练习流程编排还可以继续拆分。
- 数据只存储在浏览器本地，没有账号、云同步或服务端备份。
- 题库范围固定在当前代码内，暂不支持用户自定义假名或词汇。
- 语音播放质量取决于浏览器和操作系统提供的语音包。
- 导入学习记录时会覆盖同一假名的历史记录，没有逐条冲突确认。

## 后续可扩展方向

- 增加浏览器端交互测试，覆盖切换模式、答题、导入导出和移动端布局。
- 继续收窄 `app.js`，优先抽出练习流程编排或状态更新协调逻辑。
- 增加学习趋势图和按日期筛选记录。
- 增加更多易混淆专项或自定义专项分组。
- 支持更多罗马音体系兼容规则。
- 增加学习记录云同步或多设备迁移能力。
- 为移动端进一步优化输入模式和触控反馈。

## 相关文档

- [FEATURE_ROADMAP.md](FEATURE_ROADMAP.md)：记录功能规划、已完成状态和后续候选优化。
