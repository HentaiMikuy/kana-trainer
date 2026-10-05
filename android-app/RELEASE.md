# Android 正式发布与应用内更新

## 日常发布

源码仓库为私有仓库。APK 与更新信息发布到独立公开仓库 `HentaiMikuy/kana-trainer-releases`，
不公开源代码、私有提交日志或学习记录。完成首次签名与发布权限配置后，推送 `main` 中的 Android 代码会触发
`.github/workflows/android-release.yml`。仅 Web 文件变化不会发布 Android 包。
也可在 GitHub Actions 中手动运行 **Android release**（只允许 main）。

流程：单元测试 → 加载正式签名 → R8/资源压缩 → 校验 APK 签名 →
生成 `update.json` → 上传到草稿 Release → 全部成功后公开并设置为 latest。
失败时保留旧 latest。旧提交重跑不会重新发布；已经公开的同版本资产不会覆盖。

- `version.properties` 中的 `versionName` 是用户可见版本，如 `1.2.0`。
- 本机默认 `versionCode=3`。CI 的版本号为 `10000 + git rev-list --count HEAD`，从完整历史计算。
- `main` 必须保留线性增长的历史，不要重置或强推改写已发布历史。
  发布脚本也会拒绝将相同或更低版本替换为 latest；重建历史时需有意调整 CI 的基数。
- Release tag 为 `android-<versionCode>`；APK URL 固定到该 tag，不使用可变 latest APK 地址。
- 版本信息从构建产物 `output-metadata.json` 读取，最低系统版本由 `apkanalyzer` 读取。
- 同一 `versionName` 下可有多个构建；应用按 `versionCode` 判断更新，不比较显示字符串。

## 公开分发仓库与发布权限

1. 创建公开仓库 `HentaiMikuy/kana-trainer-releases`，勾选初始化 README。
2. 在源码仓库的 Actions Variables 设置 `ANDROID_RELEASE_REPOSITORY` 为该仓库全名。
   不设置时默认使用上面的名字。本机也可用 Gradle `-PupdateRepository=owner/repo` 覆盖。
3. 在 GitHub Settings → Developer settings → Personal access tokens → Fine-grained tokens 创建 Token，
   只选择公开分发仓库，授予 Repository permissions → Contents: Read and write。
   将 Token 保存为**私有源码仓库**的 Actions Secret `ANDROID_RELEASE_TOKEN`，不要写进 APK 或聊天。
   Token 过期前需轮换；正常 GitHub 推送凭据不会被复制到 CI。
4. 私有源码仓库的内置 `GITHUB_TOKEN` 仅用于读源码仓库，不能跨仓库发布。
   公开仓库需允许 Release 创建；若账户开启 immutable releases，请确保允许发布前准备草稿资产。

更新说明来自 `release-notes.md`，发布前编辑该文件。不会自动将私有提交日志公开。

## 首次签名配置

Android 覆盖更新必须使用相同包名及签名。正式签名丢失后，无法给已有安装覆盖更新。
本项目的 `release` 构建缺少签名时会直接失败，不会回退到 debug 签名。
本机测试仍可使用 `assemblePerformance`，该包使用本机 debug 签名。

已有正式证书时继续使用它，不要重新生成。没有正式证书时可执行：

```bash
python3 tools/local_release.py create --java /path/to/jdk/bin/java
python3 tools/local_release.py build --java /path/to/jdk/bin/java
```

WSL 可以把 `--java` 指向 Android Studio 的 `jbr/bin/java.exe`。
脚本生成 `.release-signing/kana-release.jks`、`credentials.json` 和公开证书 `certificate.der`。
目录已加入 `.gitignore`，创建命令拒绝覆盖已有目录。
**将整个目录备份到私有、安全且可恢复的位置；密码文件不要提交、发送到聊天或附在 Release 中。**

仓库 Settings → Secrets and variables → Actions 配置四项 Repository Secrets：

| Secret | 值 |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | 正式 JKS 文件的 Base64（不是文件路径） |
| `ANDROID_KEYSTORE_PASSWORD` | JKS 密码 |
| `ANDROID_KEY_ALIAS` | 签名别名（新建脚本默认为 `kana-release`） |
| `ANDROID_KEY_PASSWORD` | 私钥密码 |

使用 GitHub CLI 时，可将编码后的密钥和密码通过标准输入交给 `gh secret set`，
避免把它们放进 shell 命令历史。GitHub 只提供 Secret 名称，无法读回原始值。
工作流只在 main 上接触签名，签名文件写入临时目录并在结束时删除。
源码仓库需要启用 Actions；跨仓库 Release 写权限来自 `ANDROID_RELEASE_TOKEN`。

## 手机更新

在“记忆练习”页面打开“应用更新”，点击“检查更新”。页面展示当前版本、可用版本、
更新说明和包大小。确认下载后，Android DownloadManager 在后台下载，支持取消；
网络暂停时由系统重试。退出应用后再次打开更新页会恢复下载状态。

应用读取：

```text
https://github.com/HentaiMikuy/kana-trainer-releases/releases/latest/download/update.json
```

下载后及每次开始安装前，检查文件大小、SHA-256、包名、实际 versionCode、最低系统版本和签名。
只允许同签名且版本更高的 APK。证书轮换暂不支持。校验失败的下载会清理，可重新检查或下载。
安装通过限定到 `updates/` 目录的 FileProvider 分享只读 URI；首次安装需要允许“安装未知应用”，
最终确认交给 Android 系统。应用不申请共享存储权限、不静默安装、不主动弹出更新。
APK 存在应用私有的外部文件目录中；安装完成后再次进入更新页会清理旧下载。

404 表示尚无可用更新信息；超时、无法连接、损坏的 JSON、存储不足等会显示错误并允许重试。
GitHub 在部分网络下可能较慢，页面提供浏览器发布页入口。应用不内置 GitHub token，
因此分发仓库/发布资产必须公开；源码仓库可继续保持私有。

首次从此前的 debug/performance 包迁移：先导出学习记录，卸载旧包，手动安装首个正式包，
再导入记录。老版本没有更新模块，需要手动安装一次。后续正式版本可覆盖更新保留记录。
请勿将 debug 密钥上传为生产 Secret。

## 验证

```bash
./gradlew :app:testDebugUnitTest :app:assemblePerformance
python3 -m unittest discover -s tools/tests -v
```

发布前还应在真机上验证：检查更新、下载中断/恢复、退出并重开应用、取消后重下、
签名不一致的迁移提示、未知来源授权拒绝/允许、系统安装取消及成功覆盖后学习记录保留。
本机签名包输出为 `app/build/outputs/apk/release/app-release.apk`。
