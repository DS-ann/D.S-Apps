package com.dsann.launcher;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

/**
 * Uses Android's accessibility global action to open Quick Settings.
 * The user must explicitly enable this service once in system Accessibility settings.
 */
public final class NotificationShadeAccessibilityService extends AccessibilityService {
    private static NotificationShadeAccessibilityService instance;

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        // No screen content is inspected or collected.
    }

    @Override public void onInterrupt() {
        // Nothing to interrupt.
    }

    @Override public void onDestroy() {
        if (instance == this) instance = null;
        super.onDestroy();
    }

    static boolean openQuickSettings() {
        NotificationShadeAccessibilityService service = instance;
        return service != null
                && service.performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS);
    }
}
