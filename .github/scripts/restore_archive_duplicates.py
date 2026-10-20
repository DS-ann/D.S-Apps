#!/usr/bin/env python3
"""Restore deduplicated source paths for an archived branch in a CI workspace."""
import argparse
import hashlib
import os
import re
import shutil
import sys
from pathlib import Path, PurePosixPath

ROOT = Path.cwd().resolve()
BRANCH_PATTERN = re.compile(r"^[A-Za-z0-9._/-]+$")

def safe_path(relative: str) -> Path:
    pure = PurePosixPath(relative)
    if pure.is_absolute() or not pure.parts or any(part in ("", ".", "..") for part in pure.parts):
        raise ValueError(f"Unsafe relative path in archive manifest: {relative!r}")
    result = (ROOT / Path(*pure.parts)).resolve()
    if result != ROOT and ROOT not in result.parents:
        raise ValueError(f"Path escapes repository root: {relative!r}")
    return result

def blob_sha(path: Path) -> str:
    data = path.read_bytes()
    return hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\0" + data).hexdigest()

def apply_mode(path: Path, mode: str) -> None:
    if not re.fullmatch(r"100[0-7]{3}", mode):
        raise ValueError(f"Unsupported Git file mode: {mode!r}")
    os.chmod(path, int(mode[-3:], 8))

def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--branch", required=True, help="Archived branch name")
    args = parser.parse_args()
    branch = args.branch
    if not BRANCH_PATTERN.fullmatch(branch) or ".." in PurePosixPath(branch).parts:
        parser.error("Invalid branch name")
    manifest = safe_path(f"branch-archive/{branch}/ARCHIVE-MANIFEST.tsv")
    if not manifest.is_file():
        raise FileNotFoundError(f"Archive manifest not found: {manifest.relative_to(ROOT)}")
    rows = []
    for line_no, line in enumerate(manifest.read_text(encoding="utf-8").splitlines(), 1):
        if not line or line.startswith("#"):
            continue
        fields = line.split("\t")
        if len(fields) != 5:
            raise ValueError(f"{manifest}:{line_no}: expected five tab-separated fields")
        status, expected_sha, source_rel, target_rel, mode = fields
        if status not in ("ARCHIVED", "DUPLICATE"):
            raise ValueError(f"{manifest}:{line_no}: unknown status {status!r}")
        source = safe_path(f"branch-archive/{branch}/{source_rel}")
        target = safe_path(target_rel)
        if not re.fullmatch(r"[0-9a-f]{40}", expected_sha):
            raise ValueError(f"{manifest}:{line_no}: invalid Git blob SHA")
        if not target.is_file() or blob_sha(target) != expected_sha:
            raise ValueError(f"{manifest}:{line_no}: canonical content missing or corrupt: {target_rel}")
        rows.append((status, expected_sha, source, target, mode, source_rel, line_no))
    for status, expected_sha, source, target, mode, source_rel, line_no in rows:
        if status == "DUPLICATE" and not source.exists():
            source.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(target, source)
        if not source.is_file() or blob_sha(source) != expected_sha:
            raise ValueError(f"{manifest}:{line_no}: source content failed SHA verification: {source_rel}")
        apply_mode(source, mode)
    print(f"Verified and restored {len(rows)} source paths for branch '{branch}'.")
    return 0

if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exc:
        print(f"Archive restoration failed: {exc}", file=sys.stderr)
        raise SystemExit(1)
