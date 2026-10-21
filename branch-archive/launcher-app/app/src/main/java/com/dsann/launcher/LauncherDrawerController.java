package com.dsann.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/** Builds the app-drawer view and keeps its adapter wiring in one place. */
final class LauncherDrawerController {
    interface Actions {
        boolean canLaunchApp(View sourceView);
        void launchApp(ResolveInfo app, View sourceView);
        void showAppOptions(ResolveInfo app, boolean onHome);
        int getColumns();
        void onDrawerDataChanged();
    }

    static final class BuiltDrawer {
        final LinearLayout screen;
        final GridView grid;
        final LauncherAppAdapter adapter;
        final Runnable refreshAppList;
        final CachedDrawerBody snapshotBody;
        BuiltDrawer(LinearLayout screen, GridView grid, LauncherAppAdapter adapter,
                    Runnable refreshAppList, CachedDrawerBody snapshotBody) {
            this.screen = screen;
            this.grid = grid;
            this.adapter = adapter;
            this.refreshAppList = refreshAppList;
            this.snapshotBody = snapshotBody;
        }
    }

    /**
     * Paints the last complete drawer viewport while visible icons are warming up.
     * The bitmap is drawn by dispatchDraw, not as a touch-blocking child, so the real GridView
     * continues to receive taps and long presses while the static view is shown.
     */
    static final class CachedDrawerBody extends FrameLayout {
        private static final long MAX_SNAPSHOT_PIXELS = 1200000L;
        private final Paint snapshotPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        private final Rect snapshotBounds = new Rect();
        private Bitmap cachedSnapshot;
        private boolean displayCachedSnapshot;
        private boolean capturingSnapshot;
        private long cachedSignature = Long.MIN_VALUE;
        private long cachedItemSignature = Long.MIN_VALUE;
        private int cachedFirstVisible = -1;
        private final java.io.File persistentSnapshotFile;
        private final java.io.File persistentSnapshotMetaFile;
        private static final java.util.concurrent.ExecutorService SNAPSHOT_DISK_EXECUTOR =
                java.util.concurrent.Executors.newSingleThreadExecutor(runnable -> {
                    Thread thread = new Thread(runnable, "DrawerSnapshotDiskCache");
                    thread.setDaemon(true);
                    return thread;
                });

        CachedDrawerBody(Activity activity) {
            super(activity);
            setWillNotDraw(false);
            java.io.File directory = new java.io.File(activity.getFilesDir(), "drawer-cache-v1");
            if (!directory.exists()) directory.mkdirs();
            persistentSnapshotFile = new java.io.File(directory, "viewport.png");
            persistentSnapshotMetaFile = new java.io.File(directory, "viewport.meta");
        }

        boolean isShowingCachedSnapshot() {
            return displayCachedSnapshot && cachedSnapshot != null && !cachedSnapshot.isRecycled();
        }

        void hideCachedSnapshot() {
            if (!displayCachedSnapshot) return;
            displayCachedSnapshot = false;
            invalidate();
        }

        private void clearMemorySnapshot() {
            displayCachedSnapshot = false;
            Bitmap previous = cachedSnapshot;
            cachedSnapshot = null;
            cachedSignature = Long.MIN_VALUE;
            cachedItemSignature = Long.MIN_VALUE;
            cachedFirstVisible = -1;
            if (previous != null && !previous.isRecycled()) previous.recycle();
        }

        void invalidateSnapshot() {
            // Keep the last disk snapshot. Its signature, viewport, and dimensions are checked
            // before reuse; stale files are replaced once a fresh, artwork-complete frame exists.
            clearMemorySnapshot();
            invalidate();
        }

        /**
         * Load the last complete drawer viewport from app-private internal storage. This runs
         * while the home screen is visible, not during the opening animation.
         */
        private long[] readPersistentSnapshotMetadata() {
            try (java.io.BufferedReader reader =
                         new java.io.BufferedReader(new java.io.FileReader(persistentSnapshotMetaFile))) {
                long[] values = new long[7];
                for (int i = 0; i < values.length; i++) {
                    String line = reader.readLine();
                    if (line == null) return null;
                    values[i] = Long.parseLong(line);
                }
                return values;
            } catch (java.io.IOException | RuntimeException ignored) {
                return null;
            }
        }

        private void adoptSnapshot(Bitmap decoded, long signature, int firstVisiblePosition,
                long itemSignature, int viewWidth, int viewHeight) {
            Bitmap previous = cachedSnapshot;
            cachedSnapshot = decoded;
            cachedSignature = signature;
            cachedItemSignature = itemSignature;
            cachedFirstVisible = firstVisiblePosition;
            snapshotBounds.set(0, 0, viewWidth, viewHeight);
            if (previous != null && previous != decoded && !previous.isRecycled()) {
                previous.recycle();
            }
            invalidate();
        }

        /**
         * Load the exact, current drawer viewport from app-private internal storage.
         * A cache miss deliberately preserves any in-memory complete frame: it can remain
         * visible while a new theme or icon list is prepared, then be swapped atomically.
         */
        boolean loadPersistentSnapshot(long signature, int firstVisiblePosition) {
            int viewWidth = getWidth();
            int viewHeight = getHeight();
            if (viewWidth <= 0 || viewHeight <= 0) return false;
            if (cachedSnapshot != null && !cachedSnapshot.isRecycled()
                    && cachedSignature == signature
                    && cachedFirstVisible == firstVisiblePosition) return true;
            if (!persistentSnapshotFile.isFile() || !persistentSnapshotMetaFile.isFile()) {
                return false;
            }

            long[] metadata = readPersistentSnapshotMetadata();
            if (metadata == null || metadata[0] != signature
                    || metadata[1] != firstVisiblePosition
                    || metadata[2] != viewWidth || metadata[3] != viewHeight
                    || metadata[4] <= 0 || metadata[5] <= 0
                    || metadata[4] * metadata[5] > MAX_SNAPSHOT_PIXELS) {
                return false;
            }

            Bitmap decoded = null;
            try {
                decoded = BitmapFactory.decodeFile(persistentSnapshotFile.getAbsolutePath());
                if (decoded == null || decoded.getWidth() != (int) metadata[4]
                        || decoded.getHeight() != (int) metadata[5]) {
                    if (decoded != null && !decoded.isRecycled()) decoded.recycle();
                    return false;
                }
            } catch (RuntimeException | OutOfMemoryError ignored) {
                if (decoded != null && !decoded.isRecycled()) decoded.recycle();
                return false;
            }

            adoptSnapshot(decoded, metadata[0], (int) metadata[1],
                    metadata[6], viewWidth, viewHeight);
            return true;
        }

        /**
         * Display the last stored image as a temporary visual placeholder when only icon
         * appearance changed. The ordered app identity signature and viewport must still match,
         * so installs, removals, search results, and scrolling never get mismatched hit targets.
         */
        boolean showLastPersistentSnapshot(long itemSignature, int firstVisiblePosition) {
            int viewWidth = getWidth();
            int viewHeight = getHeight();
            if (viewWidth <= 0 || viewHeight <= 0) return false;
            if (cachedSnapshot != null && !cachedSnapshot.isRecycled()
                    && cachedItemSignature == itemSignature
                    && cachedFirstVisible == firstVisiblePosition) {
                displayCachedSnapshot = true;
                invalidate();
                return true;
            }
            if (!persistentSnapshotFile.isFile() || !persistentSnapshotMetaFile.isFile()) {
                return false;
            }

            long[] metadata = readPersistentSnapshotMetadata();
            if (metadata == null || metadata[1] != firstVisiblePosition
                    || metadata[2] != viewWidth || metadata[3] != viewHeight
                    || metadata[4] <= 0 || metadata[5] <= 0
                    || metadata[4] * metadata[5] > MAX_SNAPSHOT_PIXELS
                    || metadata[6] != itemSignature) {
                return false;
            }

            Bitmap decoded = null;
            try {
                decoded = BitmapFactory.decodeFile(persistentSnapshotFile.getAbsolutePath());
                if (decoded == null || decoded.getWidth() != (int) metadata[4]
                        || decoded.getHeight() != (int) metadata[5]) {
                    if (decoded != null && !decoded.isRecycled()) decoded.recycle();
                    return false;
                }
            } catch (RuntimeException | OutOfMemoryError ignored) {
                if (decoded != null && !decoded.isRecycled()) decoded.recycle();
                return false;
            }

            adoptSnapshot(decoded, metadata[0], (int) metadata[1],
                    metadata[6], viewWidth, viewHeight);
            displayCachedSnapshot = true;
            invalidate();
            return true;
        }

        boolean showCachedSnapshot(long signature, int firstVisiblePosition) {
            if (cachedSnapshot == null || cachedSnapshot.isRecycled()
                    || cachedSignature != signature
                    || cachedFirstVisible != firstVisiblePosition) {
                if (!loadPersistentSnapshot(signature, firstVisiblePosition)) return false;
            }
            if (!displayCachedSnapshot) {
                displayCachedSnapshot = true;
                invalidate();
            }
            return true;
        }

        private void persistSnapshot(Bitmap source, long signature, int firstVisiblePosition,
                long itemSignature, int viewWidth, int viewHeight) {
            final Bitmap copy;
            try {
                copy = source.copy(Bitmap.Config.ARGB_8888, false);
            } catch (OutOfMemoryError | RuntimeException ignored) {
                return;
            }
            if (copy == null) return;
            final int bitmapWidth = copy.getWidth();
            final int bitmapHeight = copy.getHeight();
            SNAPSHOT_DISK_EXECUTOR.execute(() -> {
                java.io.File imageTemp = new java.io.File(
                        persistentSnapshotFile.getAbsolutePath() + ".tmp");
                java.io.File metaTemp = new java.io.File(
                        persistentSnapshotMetaFile.getAbsolutePath() + ".tmp");
                try {
                    java.io.File parent = persistentSnapshotFile.getParentFile();
                    if (parent != null && !parent.exists() && !parent.mkdirs()) return;
                    try (java.io.FileOutputStream output = new java.io.FileOutputStream(imageTemp)) {
                        if (!copy.compress(Bitmap.CompressFormat.PNG, 100, output)) return;
                        output.flush();
                        output.getFD().sync();
                    }
                    try (java.io.BufferedWriter writer =
                                 new java.io.BufferedWriter(new java.io.FileWriter(metaTemp))) {
                        writer.write(Long.toString(signature)); writer.newLine();
                        writer.write(Integer.toString(firstVisiblePosition)); writer.newLine();
                        writer.write(Integer.toString(viewWidth)); writer.newLine();
                        writer.write(Integer.toString(viewHeight)); writer.newLine();
                        writer.write(Integer.toString(bitmapWidth)); writer.newLine();
                        writer.write(Integer.toString(bitmapHeight)); writer.newLine();
                        writer.write(Long.toString(itemSignature)); writer.newLine();
                        writer.flush();
                    }
                    // Both files live in the same private directory. A metadata mismatch makes
                    // an interrupted replacement a cache miss, never a wrong-screen display.
                    if (persistentSnapshotFile.exists() && !persistentSnapshotFile.delete()) return;
                    if (!imageTemp.renameTo(persistentSnapshotFile)) return;
                    if (persistentSnapshotMetaFile.exists()
                            && !persistentSnapshotMetaFile.delete()) return;
                    metaTemp.renameTo(persistentSnapshotMetaFile);
                } catch (java.io.IOException | RuntimeException ignored) {
                    imageTemp.delete();
                    metaTemp.delete();
                } finally {
                    if (!copy.isRecycled()) copy.recycle();
                }
            });
        }

        void captureSnapshot(long signature, int firstVisiblePosition, long itemSignature) {
            int width = getWidth();
            int height = getHeight();
            if (width <= 0 || height <= 0) return;

            long area = (long) width * (long) height;
            float scale = area > MAX_SNAPSHOT_PIXELS
                    ? (float) Math.sqrt((double) MAX_SNAPSHOT_PIXELS / (double) area) : 1f;
            int bitmapWidth = Math.max(1, Math.round(width * scale));
            int bitmapHeight = Math.max(1, Math.round(height * scale));
            Bitmap next = null;
            try {
                next = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(next);
                canvas.scale(bitmapWidth / (float) width, bitmapHeight / (float) height);
                capturingSnapshot = true;
                // Force live child rendering even when a previous cached frame is showing.
                super.draw(canvas);
            } catch (OutOfMemoryError | RuntimeException ignored) {
                if (next != null && !next.isRecycled()) next.recycle();
                return;
            } finally {
                capturingSnapshot = false;
            }

            Bitmap previous = cachedSnapshot;
            cachedSnapshot = next;
            cachedSignature = signature;
            cachedItemSignature = itemSignature;
            cachedFirstVisible = firstVisiblePosition;
            displayCachedSnapshot = false;
            snapshotBounds.set(0, 0, width, height);
            if (previous != null && previous != next && !previous.isRecycled()) previous.recycle();
            persistSnapshot(next, signature, firstVisiblePosition, itemSignature, width, height);
            invalidate();
        }

        @Override protected void dispatchDraw(Canvas canvas) {
            Bitmap current = cachedSnapshot;
            if (!capturingSnapshot && displayCachedSnapshot
                    && current != null && !current.isRecycled()) {
                snapshotBounds.set(0, 0, getWidth(), getHeight());
                canvas.drawBitmap(current, null, snapshotBounds, snapshotPaint);
                return;
            }
            super.dispatchDraw(canvas);
        }

        @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
            super.onSizeChanged(width, height, oldWidth, oldHeight);
            // GONE briefly causes some Android versions to report a zero-sized pass.
            // Keep the last complete image across that pass; a real non-zero size change
            // (rotation/resolution change) invalidates it and a new one is captured at Home.
            if (cachedSnapshot != null && width > 0 && height > 0
                    && oldWidth > 0 && oldHeight > 0
                    && (width != oldWidth || height != oldHeight)) {
                invalidateSnapshot();
            }
            snapshotBounds.set(0, 0, width, height);
        }
    }

    private LauncherDrawerController() {}

    static BuiltDrawer build(Activity activity, FrameLayout root, SharedPreferences prefs,
                             PackageManager packageManager, List<ResolveInfo> allApps,
                             List<ResolveInfo> drawerApps, Drawable background, int spacing,
                             String iconSizeKey, String fontSizeKey, String showLabelsKey,
                             Actions actions) {
        LinearLayout screen = new LinearLayout(activity);
        // INVISIBLE still participates in layout, allowing GridView cells to warm up in the
        // background before the first drawer opening; transition helpers later use GONE.
        screen.setVisibility(View.INVISIBLE);
        screen.setOrientation(LinearLayout.VERTICAL);
        int side = LauncherUiUtils.dp(activity, 18);
        screen.setPadding(side, side, side, LauncherUiUtils.dp(activity, 8));
        screen.setBackground(background);
        FrameLayout headerRow = new FrameLayout(activity);
        LinearLayout.LayoutParams headerParams =
                new LinearLayout.LayoutParams(-1, LauncherUiUtils.dp(activity, 48));
        headerParams.bottomMargin = LauncherUiUtils.dp(activity, 4);
        TextView title = LauncherUiUtils.text(activity, "My Apps", 28,
                android.graphics.Color.WHITE, true);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(android.graphics.Typeface.create("cursive", android.graphics.Typeface.NORMAL));
        title.setLetterSpacing(0.015f);
        headerRow.addView(title, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        final TextView searchButton = LauncherUiUtils.text(activity, "⌕", 27,
                android.graphics.Color.WHITE, true);
        searchButton.setGravity(Gravity.CENTER);
        searchButton.setContentDescription("Search apps");
        searchButton.setBackgroundResource(android.R.drawable.list_selector_background);
        searchButton.setPadding(0, 0, 0, 0);
        headerRow.addView(searchButton, new FrameLayout.LayoutParams(
                LauncherUiUtils.dp(activity, 44), LauncherUiUtils.dp(activity, 44),
                Gravity.RIGHT | Gravity.CENTER_VERTICAL));
        screen.addView(headerRow, headerParams);

        CachedDrawerBody drawerBody = new CachedDrawerBody(activity);
        GridView grid = new GridView(activity);
        grid.setNumColumns(actions.getColumns());
        grid.setVerticalSpacing(LauncherUiUtils.dp(activity, spacing));
        grid.setHorizontalSpacing(LauncherUiUtils.dp(activity, spacing / 2));
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        // GridView already reuses child cells. Its legacy scrolling bitmap cache adds
        // per-cell allocations and memory pressure; icon artwork conversion now runs off
        // the UI thread, so keep native scrolling/render-node reuse instead.
        grid.setScrollingCacheEnabled(false);
        grid.setSmoothScrollbarEnabled(true);
        grid.setCacheColorHint(android.graphics.Color.TRANSPARENT);
        grid.setSelector(android.R.color.transparent);
        final Runnable[] closeSearchRef = new Runnable[1];
        LauncherAppAdapter adapter = new LauncherAppAdapter(activity, prefs, packageManager, allApps, drawerApps, null,
                true, iconSizeKey, fontSizeKey, showLabelsKey, new LauncherAppAdapter.Actions() {
                    @Override public void onAppClick(ResolveInfo app, View sourceView) {
                        if (!actions.canLaunchApp(sourceView)) return;
                        if (closeSearchRef[0] != null) closeSearchRef[0].run();
                        actions.launchApp(app, sourceView);
                    }
                    @Override public void onAppLongClick(ResolveInfo app, boolean onHome) {
                        actions.showAppOptions(app, onHome);
                    }
                });
        adapter.setOnDataChangedListener(() -> {
            drawerBody.invalidateSnapshot();
            actions.onDrawerDataChanged();
        });
        grid.setAdapter(adapter);
        grid.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState != AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                    // A scroll changes the cached viewport even if it is not currently displayed.
                    drawerBody.invalidateSnapshot();
                }
            }
            @Override public void onScroll(AbsListView view, int firstVisibleItem,
                    int visibleItemCount, int totalItemCount) {}
        });
        drawerBody.addView(grid, new FrameLayout.LayoutParams(-1, -1));
        screen.addView(drawerBody, new LinearLayout.LayoutParams(-1, 0, 1f));

        // Keep the inline query field immediately under the header so it remains visible
        // above the keyboard on Android 6 even when adjustResize is ineffective.
        LinearLayout searchRow = new LinearLayout(activity);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams searchRowParams =
                new LinearLayout.LayoutParams(-1, LauncherUiUtils.dp(activity, 42));
        searchRowParams.bottomMargin = LauncherUiUtils.dp(activity, 2);
        searchRow.setVisibility(View.GONE);
        screen.addView(searchRow, 1, searchRowParams);

        final EditText[] activeQuery = new EditText[1];

        // allApps is the stable, freshly re-queried source owned by MainActivity. Do not
        // retain a startup snapshot here: newly installed or re-enabled apps must survive
        // closing search, even when the drawer was built before they became available.
        Runnable refreshAppList = () -> {
            String needle = activeQuery[0] == null ? ""
                    : activeQuery[0].getText().toString().trim().toLowerCase(java.util.Locale.ROOT);
            drawerApps.clear();
            String ownPackage = activity.getPackageName();
            for (ResolveInfo app : allApps) {
                if (app == null || app.activityInfo == null
                        || ownPackage.equals(app.activityInfo.packageName)) continue;
                String label = adapter.cachedLabel(app);
                if (needle.isEmpty()
                        || label.toLowerCase(java.util.Locale.ROOT).contains(needle)) {
                    drawerApps.add(app);
                }
            }
            adapter.notifyDataSetChanged();
            grid.setSelection(0);
        };
        Runnable restoreApps = () -> refreshAppList.run();
        Runnable closeSearch = () -> {
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager) activity.getSystemService(
                            Activity.INPUT_METHOD_SERVICE);
            if (imm != null && activeQuery[0] != null) {
                imm.hideSoftInputFromWindow(activeQuery[0].getWindowToken(), 0);
            }
            activeQuery[0] = null;
            searchRow.removeAllViews();
            searchRow.setVisibility(View.GONE);
            restoreApps.run();
        };
        closeSearchRef[0] = closeSearch;

        searchButton.setOnClickListener(v -> {
            if (activeQuery[0] != null) return;
            searchRow.setVisibility(View.VISIBLE);
            searchRow.removeAllViews();
            EditText queryInput = new EditText(activity);
            activeQuery[0] = queryInput;
            queryInput.setSingleLine(true);
            queryInput.setHint("Search apps");
            queryInput.setTextSize(16);
            queryInput.setTextColor(android.graphics.Color.WHITE);
            queryInput.setHintTextColor(android.graphics.Color.LTGRAY);
            queryInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                    | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            queryInput.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
            queryInput.setPadding(LauncherUiUtils.dp(activity, 12), 0,
                    LauncherUiUtils.dp(activity, 8), 0);
            queryInput.setBackground(LauncherUiUtils.roundRect(activity,
                    new int[]{android.graphics.Color.rgb(35, 42, 61), android.graphics.Color.rgb(24, 30, 46)},
                    LauncherUiUtils.dp(activity, 12)));
            searchRow.addView(queryInput, new LinearLayout.LayoutParams(0, -1, 1f));

            TextView closeButton = LauncherUiUtils.text(activity, "×", 25,
                    android.graphics.Color.WHITE, true);
            closeButton.setGravity(Gravity.CENTER);
            closeButton.setContentDescription("Close app search");
            closeButton.setBackgroundResource(android.R.drawable.list_selector_background);
            searchRow.addView(closeButton, new LinearLayout.LayoutParams(
                    LauncherUiUtils.dp(activity, 42), -1));
            closeButton.setOnClickListener(closeView -> closeSearch.run());

            queryInput.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    refreshAppList.run();
                }
                @Override public void afterTextChanged(Editable s) {}
            });
            queryInput.setOnEditorActionListener((field, actionId, event) -> {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
                        && !drawerApps.isEmpty()) {
                    ResolveInfo match = drawerApps.get(0);
                    closeSearch.run();
                    actions.launchApp(match, null);
                    return true;
                }
                return false;
            });
            queryInput.requestFocus();
            queryInput.post(() -> {
                android.view.inputmethod.InputMethodManager imm =
                        (android.view.inputmethod.InputMethodManager) activity.getSystemService(
                                Activity.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(queryInput,
                        android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            });
        });
        root.addView(screen, new FrameLayout.LayoutParams(-1, -1));
        return new BuiltDrawer(screen, grid, adapter, refreshAppList, drawerBody);
    }
}
