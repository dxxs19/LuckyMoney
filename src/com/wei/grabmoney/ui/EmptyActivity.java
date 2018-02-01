package com.wei.grabmoney.ui;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;

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
    static Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_empty);
        Log.e(TAG, "--- onCreate ---" + MainActivity.mMainActivity);
        sEmptyActivity = this;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
                if (MainActivity.mMainActivity != null)
                {
                    MainActivity.mMainActivity.finish();
                }
            }
        }, 200);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sEmptyActivity = null;
        isFirstStart = false;
    }
}
