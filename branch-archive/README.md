# Branch archive

All six project snapshots are represented below, one folder per branch name.

The repository root is intentionally small: all original project source trees and files from `main` are under `branch-archive/main/`; source snapshots for other branches are under their matching `branch-archive/<branch-name>/` paths. Active GitHub Actions workflows are centralized in the root `.github/workflows/` directory.

## Snapshots and deduplication

| Branch | Source commit | Source files (workflows excluded) | Unique content stored | Duplicate paths mapped |
|---|---|---:|---:|---:|
| `main` | `f319d324e09347191209df1333297d59a40e5952` | 315 | 313 | 2 |
| `battery-monitor` | `0656e6c9efddc226ecf12d88b82042d20c7555f3` | 326 | 15 | 311 |
| `battery-monitor-tab-android16` | `ec544a6b072b5ae895ef49a44f8e62a7411e8096` | 324 | 8 | 316 |
| `fix/auto-call-audio` | `0c9a11699d2a6984492d900e26f86bf4b37fc780` | 23 | 13 | 10 |
| `launcher-app` | `c45491aec532d850fafab0643e2208e17cfd03d9` | 29 | 29 | 0 |
| `test-call-bluetooth-audio-route` | `0ef2f5634d102f8c82d0863957a9a1080617c15e` | 307 | 2 | 305 |

For duplicate blobs, the original path is recorded in the branch's `ARCHIVE-MANIFEST.tsv`; the identical contents are stored once at the listed canonical path.

Before compiling, the corresponding workflow executes `.github/scripts/restore_archive_duplicates.py --branch '<branch-name>'`. It materializes duplicate paths in the temporary Actions runner and verifies every file against its Git blob SHA. Those restored copies are not committed to the repository.

These are deduplicated Git snapshots; CI restores missing duplicate paths before treating each snapshot as a complete build tree.
