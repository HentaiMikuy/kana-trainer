#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""从 Wikimedia Commons 下载五十音真人录音（CC 授权），供 Kana Trainer 离线使用。

用法（需要 Python 3.8+，仅标准库，需要联网；墙内请先设置代理环境变量）：

    set HTTPS_PROXY=http://127.0.0.1:7890
    python android-app/tools/fetch_kana_audio.py            # 下载全部缺失的音频
    python android-app/tools/fetch_kana_audio.py --dry-run  # 只检查覆盖情况，不下载

产物：
    android-app/app/src/main/assets/audio/kana/<罗马音>.ogg   # 应用运行时读取
    android-app/app/src/main/assets/audio/ATTRIBUTION.md      # 署名清单（随 APK 分发）

说明：
- 文件名按假名的主罗马音命名（"ji/di" 取 ji），与应用内 KanaItem.audioKey 一致；
- 主音源是 Wikimedia Commons 上 Hakatanoshio117117 的平假名录音集（PD-self 公有领域，
  单一录音者、音色统一），覆盖全部清音、浊音、半浊音；拗音（きゃ 等）该集缺失，
  会自动列入最后的未找到清单，运行时回退系统 TTS；
- 先用 API 批量解析文件的 CDN 直链（upload.wikimedia.org），再逐个从 CDN 下载，
  避免反复请求 commons.wikimedia.org 触发 429 限流；
- 每个文件的实际授权以 Commons 文件页为准，脚本会写入 ATTRIBUTION.md，发布前请人工复核；
- 未找到的假名不报错终止，最后统一列出——这些假名在应用内会自动回退到系统 TTS。
"""

import argparse
import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

USER_AGENT = "KanaTrainerAudioFetcher/1.0 (kana-trainer app; python-urllib; offline audio collection)"
API = "https://commons.wikimedia.org/w/api.php"

# 限速参数（可用环境变量覆盖）：共享代理出口容易被 Wikimedia 限流，慢速匀速最稳
DOWNLOAD_INTERVAL = float(os.environ.get("KANA_DOWNLOAD_INTERVAL", "1.0"))
RETRY_WAIT_SECONDS = float(os.environ.get("KANA_RETRY_WAIT", "15"))

# (audioKey, 平假名, 片假名)；audioKey 与 KanaItem.audioKey 一致
KANA = [
    # 清音
    ("a", "あ", "ア"), ("i", "い", "イ"), ("u", "う", "ウ"), ("e", "え", "エ"), ("o", "お", "オ"),
    ("ka", "か", "カ"), ("ki", "き", "キ"), ("ku", "く", "ク"), ("ke", "け", "ケ"), ("ko", "こ", "コ"),
    ("sa", "さ", "サ"), ("shi", "し", "シ"), ("su", "す", "ス"), ("se", "せ", "セ"), ("so", "そ", "ソ"),
    ("ta", "た", "タ"), ("chi", "ち", "チ"), ("tsu", "つ", "ツ"), ("te", "て", "テ"), ("to", "と", "ト"),
    ("na", "な", "ナ"), ("ni", "に", "ニ"), ("nu", "ぬ", "ヌ"), ("ne", "ね", "ネ"), ("no", "の", "ノ"),
    ("ha", "は", "ハ"), ("hi", "ひ", "ヒ"), ("fu", "ふ", "フ"), ("he", "へ", "ヘ"), ("ho", "ほ", "ホ"),
    ("ma", "ま", "マ"), ("mi", "み", "ミ"), ("mu", "む", "ム"), ("me", "め", "メ"), ("mo", "も", "モ"),
    ("ya", "や", "ヤ"), ("yu", "ゆ", "ユ"), ("yo", "よ", "ヨ"),
    ("ra", "ら", "ラ"), ("ri", "り", "リ"), ("ru", "る", "ル"), ("re", "れ", "レ"), ("ro", "ろ", "ロ"),
    ("wa", "わ", "ワ"), ("wo", "を", "ヲ"), ("n", "ん", "ン"),
    # 浊音（ぢ/づ 与 じ/ず 同音，共用 ji/zu 文件）
    ("ga", "が", "ガ"), ("gi", "ぎ", "ギ"), ("gu", "ぐ", "グ"), ("ge", "げ", "ゲ"), ("go", "ご", "ゴ"),
    ("za", "ざ", "ザ"), ("ji", "じ", "ジ"), ("zu", "ず", "ズ"), ("ze", "ぜ", "ゼ"), ("zo", "ぞ", "ゾ"),
    ("da", "だ", "ダ"), ("de", "で", "デ"), ("do", "ど", "ド"),
    ("ba", "ば", "バ"), ("bi", "び", "ビ"), ("bu", "ぶ", "ブ"), ("be", "べ", "ベ"), ("bo", "ぼ", "ボ"),
    # 半浊音
    ("pa", "ぱ", "パ"), ("pi", "ぴ", "ピ"), ("pu", "ぷ", "プ"), ("pe", "ぺ", "ペ"), ("po", "ぽ", "ポ"),
    # 拗音
    ("kya", "きゃ", "キャ"), ("kyu", "きゅ", "キュ"), ("kyo", "きょ", "キョ"),
    ("sha", "しゃ", "シャ"), ("shu", "しゅ", "シュ"), ("sho", "しょ", "ショ"),
    ("cha", "ちゃ", "チャ"), ("chu", "ちゅ", "チュ"), ("cho", "ちょ", "チョ"),
    ("nya", "にゃ", "ニャ"), ("nyu", "にゅ", "ニュ"), ("nyo", "にょ", "ニョ"),
    ("hya", "ひゃ", "ヒャ"), ("hyu", "ひゅ", "ヒュ"), ("hyo", "ひょ", "ヒョ"),
    ("mya", "みゃ", "ミャ"), ("myu", "みゅ", "ミュ"), ("myo", "みょ", "ミョ"),
    ("rya", "りゃ", "リャ"), ("ryu", "りゅ", "リュ"), ("ryo", "りょ", "リョ"),
    ("gya", "ぎゃ", "ギャ"), ("gyu", "ぎゅ", "ギュ"), ("gyo", "ぎょ", "ギョ"),
    ("ja", "じゃ", "ジャ"), ("ju", "じゅ", "ジュ"), ("jo", "じょ", "ジョ"),
    ("bya", "びゃ", "ビャ"), ("byu", "びゅ", "ビュ"), ("byo", "びょ", "ビョ"),
    ("pya", "ぴゃ", "ピャ"), ("pyu", "ぴゅ", "ピュ"), ("pyo", "ぴょ", "ピョ"),
]

# Hakatanoshio117117 的平假名录音集（PD-self 公有领域）中的特殊命名：
# 元音/ん 用大写，hu/ti/zi 是旧式罗马音拼写，其余为 "Japanese <小写罗马音>.ogg"
SPECIAL_SOURCE_NAMES = {
    "a": "Ja-A.oga",
    "e": "Ja-E.oga",
    "i": "Japanese I.ogg",
    "o": "Japanese O.ogg",
    "u": "Japanese U.ogg",
    "n": "Japanese N.ogg",
    "fu": "Japanese hu.ogg",
    "chi": "Japanese ti.ogg",
    "ji": "Japanese zi.ogg",
}

OUT_DIR = Path(__file__).resolve().parents[1] / "app" / "src" / "main" / "assets" / "audio" / "kana"
ATTRIBUTION = OUT_DIR.parent / "ATTRIBUTION.md"


def candidate_filenames(key, hira, kata):
    """按命中率从高到低给出候选 Commons 文件名。"""
    names = []
    special = SPECIAL_SOURCE_NAMES.get(key)
    if special:
        names.append(special)
    names.append(f"Japanese {key}.ogg")
    names.append(f"Japanese {key.capitalize()}.ogg")
    names.append(f"Ja-{key.capitalize()}.oga")
    names.append(f"Ja-{hira}.oga")
    names.append(f"Ja-{kata}.oga")
    # 去重保持顺序
    return list(dict.fromkeys(names))


def http_get(url, binary=False):
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(req, timeout=30) as resp:
        data = resp.read()
        return data if binary else data.decode("utf-8")


def api_query(params):
    url = API + "?" + urllib.parse.urlencode(params)
    return json.loads(http_get(url))


def resolve_urls(filenames):
    """批量解析文件的 CDN 直链（upload.wikimedia.org），返回 {文件名: 直链}。"""
    urls = {}
    for i in range(0, len(filenames), 40):
        batch = filenames[i:i + 40]
        params = {
            "action": "query",
            "format": "json",
            "prop": "imageinfo",
            "iiprop": "url",
            "titles": "|".join("File:" + f for f in batch),
        }
        for attempt in range(3):
            try:
                payload = api_query(params)
                for page in payload.get("query", {}).get("pages", {}).values():
                    if "missing" in page:
                        continue
                    title = page.get("title", "")
                    info = page.get("imageinfo")
                    if title.startswith("File:") and info:
                        urls[title[len("File:"):]] = info[0].get("url")
                break
            except urllib.error.HTTPError as e:
                if e.code == 429 and attempt < 2:
                    wait = 10 * (attempt + 1)
                    print(f"  ~ 批量解析限流，{wait}s 后重试", file=sys.stderr)
                    time.sleep(wait)
                    continue
                print(f"  ! 批量解析失败: HTTP {e.code}", file=sys.stderr)
                break
            except Exception as e:
                print(f"  ! 批量解析失败: {e}", file=sys.stderr)
                break
        time.sleep(1.0)
    return urls


def download_file(url):
    """从 CDN 下载文件内容。429 时限速等待后重试同一文件（等待额度恢复），失败返回 None。"""
    for attempt in range(10):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
            with urllib.request.urlopen(req, timeout=30) as resp:
                return resp.read()
        except urllib.error.HTTPError as e:
            if e.code == 429 and attempt < 9:
                wait = RETRY_WAIT_SECONDS * (1 + attempt // 2)
                print(f"  ~ 下载限流，{wait:.0f}s 后重试（第 {attempt + 1} 次）", file=sys.stderr)
                time.sleep(wait)
                continue
            print(f"  ! 下载失败: HTTP {e.code}", file=sys.stderr)
            return None
        except Exception as e:
            print(f"  ! 下载失败: {e}", file=sys.stderr)
            return None
    return None


def query_file_metadata(filename):
    """返回 (license_short, artist) 或 None。"""
    params = {
        "action": "query",
        "format": "json",
        "prop": "imageinfo",
        "iiprop": "extmetadata",
        "titles": "File:" + filename,
    }
    try:
        payload = api_query(params)
    except Exception:
        return None
    pages = payload.get("query", {}).get("pages", {})
    for page in pages.values():
        info = (page.get("imageinfo") or [{}])[0]
        meta = info.get("extmetadata") or {}
        license_short = (meta.get("LicenseShortName") or {}).get("value", "未知")
        artist = (meta.get("Artist") or {}).get("value", "未知")
        # 去掉 HTML 标签
        for ch in ("<a", "</a>", "<i>", "</i>", "<b>", "</b>", "<span>", "</span>"):
            artist = artist.split(ch)[0] if ch in artist else artist
        return license_short, artist
    return None


def main():
    parser = argparse.ArgumentParser(description="下载五十音真人录音（Wikimedia Commons）")
    parser.add_argument("--dry-run", action="store_true", help="只检查覆盖情况，不下载")
    parser.add_argument("--only", help="只处理指定罗马音，逗号分隔（如 a,ka,kya）")
    args = parser.parse_args()

    targets = KANA
    if args.only:
        wanted = {k.strip() for k in args.only.split(",")}
        targets = [t for t in KANA if t[0] in wanted]

    if not args.dry_run:
        OUT_DIR.mkdir(parents=True, exist_ok=True)

    downloaded = []
    skipped = []
    missing = []
    attributions = []

    todo = []
    for key, hira, kata in targets:
        out_file = OUT_DIR / f"{key}.ogg"
        if out_file.exists() and not args.dry_run:
            skipped.append(key)
        else:
            todo.append((key, hira, kata))

    # 第一阶段：批量解析全部候选文件的 CDN 直链
    probe = []
    for key, hira, kata in todo:
        for filename in candidate_filenames(key, hira, kata):
            probe.append((key, filename))
    resolved = resolve_urls([f for _, f in probe])
    hits = {}
    for key, filename in probe:
        if key not in hits and filename in resolved:
            hits[key] = (filename, resolved[filename])

    # 第二阶段：逐个从 CDN 下载
    for key, hira, kata in todo:
        hit = hits.get(key)
        if not hit:
            missing.append(f"{key} ({hira})")
            continue
        filename, url = hit
        print(f"[{hira}] {key} -> {filename}")
        if args.dry_run:
            downloaded.append(key)
            continue
        data = download_file(url)
        if data is None:
            missing.append(f"{key} ({hira}, 下载失败)")
            continue
        (OUT_DIR / f"{key}.ogg").write_bytes(data)
        downloaded.append(key)
        print(f"  已保存 ({len(data) // 1024} KB)")
        meta = query_file_metadata(filename) or ("未知", "未知")
        attributions.append((key, hira, kata, filename, meta[0], meta[1]))
        time.sleep(DOWNLOAD_INTERVAL)

    if attributions and not args.dry_run:
        lines = [
            "# 五十音发声音频署名",
            "",
            "以下音频来自 Wikimedia Commons，由对应上传者以所列授权条款发布。",
            "应用内发音同时提供系统 TTS 回退；本清单仅覆盖随包分发的录音文件。",
            "",
            "| 假名 | 文件 | 来源（Commons） | 授权 | 作者 |",
            "| --- | --- | --- | --- | --- |",
        ]
        for key, hira, kata, filename, license_short, artist in attributions:
            url = "https://commons.wikimedia.org/wiki/File:" + urllib.parse.quote(filename)
            lines.append(f"| {hira} / {kata} | {key}.ogg | [{filename}]({url}) | {license_short} | {artist} |")
        lines.append("")
        ATTRIBUTION.write_text("\n".join(lines), encoding="utf-8")

    print()
    print(f"完成：新下载 {len(downloaded)}，已存在跳过 {len(skipped)}，未找到 {len(missing)}")
    if missing:
        print("未找到（运行时将回退系统 TTS）：")
        for item in missing:
            print(f"  - {item}")


if __name__ == "__main__":
    main()
