#!/usr/bin/env python3
"""Restore deduplicated source paths for an archived branch in a CI workspace."""
import argparse
import hashlib
import os
import re
import shutil
import subprocess
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

def historical_blob(expected_sha: str):
    """Read an archived duplicate from Git history when its canonical path changed."""
    result = subprocess.run(
        ["git", "cat-file", "blob", expected_sha],
        cwd=ROOT,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=False,
    )
    if result.returncode != 0:
        return None
    data = result.stdout
    actual = hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\0" + data).hexdigest()
    return data if actual == expected_sha else None

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
        target_matches = target.is_file() and blob_sha(target) == expected_sha
        recovered_blob = None
        if not target_matches:
            # Canonical paths can evolve after an archive is created. For DUPLICATE
            # entries, recover the immutable expected blob from repository history
            # rather than copying newer code into the archived branch.
            if status == "DUPLICATE":
                recovered_blob = historical_blob(expected_sha)
            if recovered_blob is None:
                raise ValueError(f"{manifest}:{line_no}: canonical content missing or corrupt and historical blob unavailable: {target_rel}")
        rows.append((status, expected_sha, source, target, mode, source_rel, line_no,
                     target_matches, recovered_blob))
    for status, expected_sha, source, target, mode, source_rel, line_no, target_matches, recovered_blob in rows:
        if status == "DUPLICATE" and not source.exists():
            source.parent.mkdir(parents=True, exist_ok=True)
            if target_matches:
                shutil.copyfile(target, source)
            else:
                source.write_bytes(recovered_blob)
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
