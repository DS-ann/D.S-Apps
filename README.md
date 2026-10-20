# D.S-Apps

This repository consolidates historical project branches while keeping project variants separate.

## Project snapshots

- `branch-archive/main/` — the former main tree and its files.
- `branch-archive/battery-monitor/` — Battery Monitor snapshot.
- `branch-archive/battery-monitor-tab-android16/` — Android 16 tablet Battery Monitor snapshot.
- `branch-archive/fix/auto-call-audio/` — automatic call-audio fixes snapshot.
- `branch-archive/launcher-app/` — D.S Launcher snapshot.
- `branch-archive/test-call-bluetooth-audio-route/` — Bluetooth audio-routing test snapshot.

Identical Git blob contents are stored once. Each snapshot has an `ARCHIVE-MANIFEST.tsv` recording original paths, SHA values, canonical storage paths, and file modes. Build workflows restore duplicate paths in the runner before compiling.

## GitHub Actions

All workflows are centralized in `.github/workflows/`. Build workflows target the matching `branch-archive/<branch-name>/` project tree. Firmware inspection and the AirBeats repair workflow remain available as manually started workflows.

Archive integrity and old-branch cleanup are handled by `archive-verify-delete-branches.yml`. The `main` branch remains the default branch.
