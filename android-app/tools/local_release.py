#!/usr/bin/env python3
"""Create a permanent local signing key or build with it; never print passwords.

python tools/local_release.py create --java /path/to/java
python tools/local_release.py build --java /path/to/java [--version-code 10012]
The ignored .release-signing directory must be backed up separately.
"""
import argparse
import json
import os
from pathlib import Path
import secrets
import subprocess

ANDROID = Path(__file__).resolve().parents[1]
SIGNING = ANDROID / ".release-signing"


def native_path(path, java):
    path = path.resolve()
    if java.suffix == ".exe" and os.name != "nt":
        return subprocess.check_output(["wslpath", "-w", str(path)], text=True).strip()
    return str(path)


def native_environment(overrides, java):
    env = os.environ | overrides
    if java.suffix == ".exe" and os.name != "nt":
        env["WSLENV"] = ":".join(filter(None, [env.get("WSLENV", ""), *overrides]))
    return env


def create(java, resume=False):
    if SIGNING.exists():
        if not resume or (SIGNING / "kana-release.jks").exists():
            raise SystemExit("Signing directory already exists. Refusing to replace a permanent key.")
        config = json.loads((SIGNING / "credentials.json").read_text(encoding="utf-8"))
    else:
        SIGNING.mkdir(mode=0o700)
        config = dict(keyAlias="kana-release", storePassword=secrets.token_urlsafe(32),
                      keyPassword=secrets.token_urlsafe(32))
        credentials = SIGNING / "credentials.json"
        credentials.write_text(json.dumps(config, indent=2) + "\n", encoding="utf-8")
        credentials.chmod(0o600)
    keytool = java.with_name("keytool" + java.suffix)
    env = native_environment({"KANA_STORE_PASSWORD": config["storePassword"], "KANA_KEY_PASSWORD": config["keyPassword"]}, java)
    subprocess.run([str(keytool), "-genkeypair", "-storetype", "JKS", "-keystore",
        native_path(SIGNING / "kana-release.jks", java), "-alias", config["keyAlias"],
        "-storepass:env", "KANA_STORE_PASSWORD", "-keypass:env", "KANA_KEY_PASSWORD",
        "-keyalg", "RSA", "-keysize", "3072", "-sigalg", "SHA256withRSA",
        "-validity", "10000", "-dname", "CN=Kana Trainer, O=Kana Trainer", "-noprompt"],
        env=env, check=True)
    (SIGNING / "kana-release.jks").chmod(0o600)
    subprocess.run([str(keytool), "-exportcert", "-keystore", native_path(SIGNING / "kana-release.jks", java),
        "-alias", config["keyAlias"], "-storepass:env", "KANA_STORE_PASSWORD",
        "-file", native_path(SIGNING / "certificate.der", java)], env=env, check=True)
    print("Created permanent signing key. Back up android-app/.release-signing securely.")


def build(java, version_code):
    config = json.loads((SIGNING / "credentials.json").read_text(encoding="utf-8"))
    env = native_environment({"ANDROID_KEYSTORE_PATH": native_path(SIGNING / "kana-release.jks", java),
        "ANDROID_KEYSTORE_PASSWORD": config["storePassword"], "ANDROID_KEY_ALIAS": config["keyAlias"],
        "ANDROID_KEY_PASSWORD": config["keyPassword"]}, java)
    command = [str(java), "-classpath", "gradle/wrapper/gradle-wrapper.jar",
               "org.gradle.wrapper.GradleWrapperMain", ":app:assembleRelease", "--console=plain"]
    if version_code is not None:
        command.append(f"-PciVersionCode={version_code}")
    subprocess.run(command, cwd=ANDROID, env=env, check=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=["create", "build"])
    parser.add_argument("--java", type=Path, required=True)
    parser.add_argument("--version-code", type=int)
    parser.add_argument("--resume", action="store_true", help="Resume key creation only when no keystore exists")
    args = parser.parse_args()
    java = args.java.resolve()
    if args.action == "create":
        create(java, args.resume)
    else:
        build(java, args.version_code)
