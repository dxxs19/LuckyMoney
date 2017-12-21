package com.wei.grabmoney.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.wei.grabmoney.R;
import com.wei.grabmoney.utils.Log;
import com.wei.grabmoney.utils.SharedPreUtils;

/**
 * @author WEI
 */
public class MainActivity extends BaseActivity implements TextWatcher, SeekBar.OnSeekBarChangeListener, CompoundButton.OnCheckedChangeListener {
    private final String TAG = "MainActivity";
    private TextView currentStateTxt, delayTimeTxt;
    private EditText contentTxt;
    private final Intent mAccessibleIntent =
            new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
    public static final String MARK_WORK = "mark_work", DELAY_TIME = "delay_time", FASTEST_CHECKED = "fastest_checked";
    private SharedPreUtils mSharedPreUtils;
    private SeekBar mSeekBar;
    private final float MAX_TIME = 2000;
    private Button startServerBtn = null;
    private RadioButton fastestChk, manualChk;
    private RadioButton voice_vibrate, rdoBtn_voice, rdoBtn_vibrate, rdoBtn_nothing;
    public final static String VOICE_VIBRATE = "voice_vibrate";
    public final static String VOICE = "voice";
    public final static String VIBRATE = "vibrate";
    public final static String NOTHING = "nothing";
    private int mProgree;
    public static MainActivity mMainActivity = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        setData();
        mMainActivity = this;
    }

    private void initView() {
        setContentView(R.layout.activity_main);
        back.setVisibility(View.GONE);
        setCustomTitle("自动抢红包");
        currentStateTxt = (TextView) findViewById(R.id.txt_currentState);
        contentTxt = (EditText) findViewById(R.id.content);
        mSeekBar = (SeekBar) findViewById(R.id.delay_seekBar);
        fastestChk = (RadioButton) findViewById(R.id.chk_fastest);
        manualChk = (RadioButton) findViewById(R.id.chk_manual);
        fastestChk.setOnCheckedChangeListener(this);
        manualChk.setOnCheckedChangeListener(this);
        mSeekBar.setOnSeekBarChangeListener(this);
        delayTimeTxt = (TextView) findViewById(R.id.txt_delayTime);
        startServerBtn = (Button) findViewById(R.id.btn_start);
        voice_vibrate = (RadioButton) findViewById(R.id.voice_vibrate);
        rdoBtn_voice = (RadioButton) findViewById(R.id.rdoBtn_voice);
        rdoBtn_vibrate = (RadioButton) findViewById(R.id.rdoBtn_vibrate);
        rdoBtn_nothing = (RadioButton) findViewById(R.id.rdoBtn_nothing);

        voice_vibrate.setOnCheckedChangeListener(this);
        rdoBtn_voice.setOnCheckedChangeListener(this);
        rdoBtn_vibrate.setOnCheckedChangeListener(this);
        rdoBtn_nothing.setOnCheckedChangeListener(this);
    }

    private void setData() {
        mSharedPreUtils = new SharedPreUtils(mContext);
        String mark = mSharedPreUtils.getString(MARK_WORK, "");
        contentTxt.setText(mark);
        contentTxt.setSelection(mark.length());
        contentTxt.addTextChangedListener(this);

        float delayTime = mSharedPreUtils.getFloat(DELAY_TIME, 0);
        float rate = delayTime / MAX_TIME;
        mSeekBar.setProgress((int) (rate * 100));
        delayTimeTxt.setText(delayTime / 1000 + " 秒 ");

        String value = mSharedPreUtils.getString("alarm", NOTHING);
        if (value.equals(MainActivity.NOTHING)) {
            rdoBtn_nothing.setChecked(true);
        } else if (value.equals(MainActivity.VOICE_VIBRATE)) {
            voice_vibrate.setChecked(true);
        } else if (value.equals(MainActivity.VOICE)) {
            rdoBtn_voice.setChecked(true);
        } else if (value.equals(MainActivity.VIBRATE)) {
            rdoBtn_vibrate.setChecked(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateServiceStatus();
    }

    private void updateServiceStatus() {
        boolean serviceActive;
        serviceActive = isServiceActive(getPackageName() + "/" + getPackageName() + ".service.GrabMoneyService");
        currentStateTxt.setText(serviceActive ? "正在抢红包......" : "服务未启动");
        currentStateTxt.setTextColor(serviceActive ? Color.GREEN : Color.GRAY);
        if (serviceActive) {
            startServerBtn.setText("停止服务");
        } else {
            startServerBtn.setText("开启服务");
        }
    }

    /**
     * 检测服务是否已开启
     *
     * @param service
     * @return
     */
    private boolean isServiceActive(String service) {
        int ok = 0;
        try {
            ok = Settings.Secure.getInt(getApplicationContext().getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
        }
        TextUtils.SimpleStringSplitter ms = new TextUtils.SimpleStringSplitter(':');
        if (ok == 1) {
            String settingValue = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                ms.setString(settingValue);
                while (ms.hasNext()) {
                    String accessibilityService = ms.next();
                    if (accessibilityService.equalsIgnoreCase(service)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 开启服务
     *
     * @param view
     */
    public void startService(View view) {
        try {
            mAccessibleIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(mAccessibleIntent);
        } catch (Exception e) {

        }
    }

    public void stopService(View view) {
        stopService(mAccessibleIntent);
        updateServiceStatus();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        if (!TextUtils.isEmpty(s)) {
            String mark = s.toString();
            contentTxt.setSelection(mark.length());
            saveMark(mark);
        } else {
            saveMark("");
        }
    }

    private void saveMark(String mark) {
        mSharedPreUtils.putString(MARK_WORK, mark);
    }



    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            this.mProgree = progress;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        Log.d(TAG, "--- onStartTrackingTouch ---");
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        Log.d(TAG, "--- onStopTrackingTouch ---");
        float delayTime = mProgree * MAX_TIME / 100;
        Log.e(TAG, "延迟了：" + delayTime + "毫秒");
        mSharedPreUtils.putFloat(DELAY_TIME, delayTime);
        delayTimeTxt.setText(delayTime / 1000 + " 秒 ");
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.chk_fastest:
                if (isChecked) {
                    mSharedPreUtils.putBoolean(FASTEST_CHECKED, true);
                }
                break;

            case R.id.chk_manual:
                if (isChecked) {
                    mSharedPreUtils.putBoolean(FASTEST_CHECKED, false);
                }
                break;

            case R.id.voice_vibrate:
                if (isChecked) {
                    saveAlarm(VOICE_VIBRATE);
                }
                break;

            case R.id.rdoBtn_voice:
                if (isChecked) {
                    saveAlarm(VOICE);
                }
                break;

            case R.id.rdoBtn_vibrate:
                if (isChecked) {
                    saveAlarm(VIBRATE);
                }
                break;

            case R.id.rdoBtn_nothing:
                if (isChecked) {
                    saveAlarm(NOTHING);
                }
                break;

            default:
        }
    }

    private void saveAlarm(String value) {
        mSharedPreUtils.putString("alarm", value);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMainActivity = null;
    }
}
