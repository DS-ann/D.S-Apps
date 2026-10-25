package com.dsann.launcher;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

/**
 * Intentionally empty destination for the D.S Launcher app-drawer icon.
 * It is a normal Activity, separate from the HOME/launcher interface.
 */
public final class BlankActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View blankPage = new View(this);
        blankPage.setBackgroundColor(Color.BLACK);
        setContentView(blankPage);
    }
}
