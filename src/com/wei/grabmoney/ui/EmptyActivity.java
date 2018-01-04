package com.wei.grabmoney.ui;

import android.app.Activity;
import android.os.Bundle;

import com.wei.grabmoney.R;
import com.wei.grabmoney.utils.Log;

/**
 * @author x-wei
 */
public class EmptyActivity extends Activity
{
    private final String TAG = getClass().getSimpleName();
    public static EmptyActivity sEmptyActivity = null;
    public static boolean isFirstStart = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_empty);
        Log.e(TAG, "--- onCreate ---" + MainActivity.mMainActivity);
        sEmptyActivity = this;
        finish();
        if (MainActivity.mMainActivity != null)
        {
            MainActivity.mMainActivity.finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sEmptyActivity = null;
        isFirstStart = false;
    }
}
