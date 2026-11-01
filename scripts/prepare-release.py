#!/usr/bin/env python3
"""Queue an already verified signed payload for publication by GitHub Actions."""
from pathlib import Path
import hashlib
import re
import subprocess
import sys

root = Path(__file__).resolve().parent.parent
version = sys.argv[1]
if not re.fullmatch(r"[0-9]+\.[0-9]+\.[0-9]+", version):
    raise SystemExit("Version must use major.minor.patch")

def git(*args, data=None):
    return subprocess.check_output(["git", "-C", str(root), *args], input=data).decode().strip()

apk = root / "app/build/outputs/apk/release/app-release.apk"
if not apk.is_file():
    raise SystemExit("Build and verify the release APK before preparing publication")
source = git("rev-parse", "HEAD")
files = {
    "FightForge.apk": apk.read_bytes(),
    "SHA256SUMS": (hashlib.sha256(apk.read_bytes()).hexdigest() + "  FightForge.apk\n").encode(),
    "SOURCE_COMMIT": (source + "\n").encode(),
}
entries = []
for name, content in files.items():
    blob = git("hash-object", "-w", "--stdin", data=content)
    entries.append(f"100644 blob {blob}\t{name}\n")
tree = git("mktree", data="".join(entries).encode())
commit = git("commit-tree", tree, "-m", f"Verified signed payload v{version}; no private keys")
subprocess.run(["git", "-C", str(root), "push", "origin", f"{commit}:refs/heads/release-artifacts/v{version}"], check=True)
subprocess.run(["git", "-C", str(root), "commit", "--allow-empty", "-m", f"[publish-release:v{version}] Publish verified signed APK"], check=True)
subprocess.run(["git", "-C", str(root), "push", "origin", "HEAD:main"], check=True)
print("Signed payload queued for verification and publication by GitHub Actions.")
