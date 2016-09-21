package com.greedlab.greedpatch;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.greedlab.patch.Greedpatch;

import java.io.File;

/**
 * Created by Bell on 16/9/20.
 */

public class MyApplication extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        File file = Greedpatch.getInstance(this).getPatchFile();
        if (file != null && file.exists()) {
            Log.d("greedpatch", "start patch");
        } else {
            Log.d("greedpatch", "no need to patch");
        }
    }
}
