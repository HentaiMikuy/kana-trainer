# 假名音频来源

当前 `kana/*.ogg` 为日语合成语音，不是真人录音或 Wikimedia Commons 音频。

- 声音：Microsoft `ja-JP-NanamiNeural`（Nanami 女声）。
- 生成工具：edge-tts 7.2.8；生成日期：2026-10-05。
- 参数：语速 -10%，音高默认；每个文件只合成一个假名。
- 使用已选定的 10 个试听样本，补齐其余读音；102 个文件覆盖 104 个题库条目。
- 「じ／ぢ」「ず／づ」共用现代标准日语读音；「を」按现代标准 /o/ 生成。
- 「は」「へ」的合成输入使用「ハ」「ヘ」，避免被解释为助词。
- 「い」使用句末标点输入「い。」重新合成，以缩短单独读音的拖尾（处理后约 0.41 秒）。
- 与试听版相同的音量及首尾空白处理，编码为 24kHz 单声道 Ogg Vorbis。

生成与处理脚本：`android-app/tools/generate_nanami_audio.py`。
输入文本、参数、时长及文件 SHA-256：`android-app/tools/nanami-audio-manifest.json`。
原 Wikimedia 音频及其署名清单已备份到
`android-app/audio-preview/backups/wiki-before-nanami.zip`，不再随当前应用分发。

本文件记录来源与处理方式，不将合成音频声明为 Wikimedia 的 Public Domain 授权。
