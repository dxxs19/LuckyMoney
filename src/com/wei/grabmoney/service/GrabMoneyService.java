package com.wei.grabmoney.service;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Vibrator;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.wei.grabmoney.R;
import com.wei.grabmoney.ui.EmptyActivity;
import com.wei.grabmoney.ui.MainActivity;
import com.wei.grabmoney.utils.Log;
import com.wei.grabmoney.utils.SharedPreUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


/**
 * @author x-wei
 */
public class GrabMoneyService extends AccessibilityService {
    private static final String TAG = "GrabMoneyService";
    private static final String WX_PKG = "com.tencent.mm";
    private final String QQ_PKG = "com.tencent.mobileqq";
    private static final String WX_ID_PREFIX = WX_PKG + ":id/";

    /**
     * "开"按钮的id
     */
    private static String OPENBTN_ID = ""; //WX_ID_PREFIX + "c2i";

    private static final int PERIOD_TIME = 3000;
    private static final String WEIXIN_CLASSNAME = "com.tencent.mm.ui.LauncherUI";
    private static final String WEIXIN_LUCKYMONEYRECEIVEUI_OLD = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI";
    private static final String WEIXIN_LUCKYMONEYRECEIVEUI = "com.tencent.mm.plugin.luckymoney.ui.En_fba4b94f";
    private static final String WEIXIN_MONEY_TEXT = "[微信红包]";
    // 红包详情界面(已领该红包)
    private static final String WEIXIN_LUCKYMONEYDETAILUI = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI";
    private static final String GETMONEY_TEXT = "领取红包";
    private static final String CHECKMONEY_TEXT = "查看红包";
    private static final String FAILT_TEXT = "手慢了";
    private static final String TIMEOUT_TEXT = "该红包已超过";

    // QQ钱包界面
    private static final String QQ_WALLET = "cooperation.qwallet.plugin.QWalletPluginProxyActivity";
    private static final String QQ_CHAT = "com.tencent.mobileqq.activity.SplashActivity";
    private static final String QQ_TIPS = "点击拆开";
    private static final String QQ_COMMAND = "口令红包";
    private static final String QQ_CLICK_COMMAND = "点击输入口令";
    private static final String QQ_MONEY_TEXT = "[QQ红包]";

    private String className = "";

    private KeyguardManager keyguardManager;
    private KeyguardManager.KeyguardLock keyguardLock;
    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;
    private Vibrator vibrator;

    private SharedPreUtils mSharedPreUtils;

    private final String[] CLASSARRAYS = new String[]{WEIXIN_CLASSNAME, WEIXIN_LUCKYMONEYRECEIVEUI_OLD, WEIXIN_LUCKYMONEYRECEIVEUI, WEIXIN_LUCKYMONEYDETAILUI,
            QQ_WALLET, QQ_CHAT};
    private final List<String> CLASSLISTS = Arrays.asList(CLASSARRAYS);
    private boolean hasOpenBtn = false;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        final int eventType = event.getEventType();
        Log.e(TAG, "eventType = " + eventType);
        final String tempClass = event.getClassName().toString();
        Log.e(TAG, "tempClass : " + tempClass);
        switch (eventType) {
            // 监听通知栏消息,如果有“【微信红包】”4个字，则跳转到微信聊天界面，开始抢红包
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                dealNotificationEven(event);
                break;

            case AccessibilityEvent.TYPE_VIEW_FOCUSED:
                break;

            // 如果当前界面是微信聊天界面，则进行以下操作
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                className = event.getClassName().toString();
            case AccessibilityEvent.TYPE_VIEW_SCROLLED:
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                if (!CLASSLISTS.contains(className)) {
                    break;
                }
                // 以下是抢微信红包
                if (className.equals(WEIXIN_CLASSNAME)) {
                    // 如果是聊天界面，则搜索有没有红包，有则点击红包
                    getMoney();
                }
                else if (className.equals(WEIXIN_LUCKYMONEYRECEIVEUI) || className.equals(WEIXIN_LUCKYMONEYRECEIVEUI_OLD))
                {
                    List<AccessibilityNodeInfo> infos = getOpenButtons();
                    Log.e(TAG, "has Open ? " + (infos != null) + ", size : " + (infos != null ? infos.size() : 0));
                    if (infos == null)
                    {   // 没有"开",android 7.0+需要这个策略
                        if (EmptyActivity.isFirstStart)
                        {  /// TODO
                            if (Build.VERSION.SDK_INT > 23)
                            {
                                Intent intent = new Intent(this, EmptyActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        }
                    }
                    else if (infos.size() > 0)
                    {   // 点击 "开"
                        hasOpenBtn = true;
                        for (AccessibilityNodeInfo nodeInfo:infos) {
                            nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        }
                    }
                } else if (className.equals(WEIXIN_LUCKYMONEYDETAILUI)) {
                    if (hasOpenBtn)
                    {// 自动点开的，要自动关掉
                        performGlobalAction(GLOBAL_ACTION_BACK);
                        hasOpenBtn = false;
                    }
                }
                // 以下是抢QQ红包
                else if (className.equals(QQ_CHAT)) {
                    getQQMoney();
                } else if (className.equals(QQ_WALLET)) {
                    performGlobalAction(GLOBAL_ACTION_BACK);
                }
                break;

            default:
        }
    }

    /**
     * 处理通知栏信息
     *
     * @param event
     */
    private void dealNotificationEven(AccessibilityEvent event) {
        List<CharSequence> texts = event.getText();
        if (!texts.isEmpty()) {
            for (CharSequence text : texts) {
                String content = text.toString();
                Log.e(TAG, "content : " + content);
                if (content.contains(WEIXIN_MONEY_TEXT) || content.contains(QQ_MONEY_TEXT)) {
                    // 判断屏幕是否处于锁屏状态
                    if (keyguardManager.inKeyguardRestrictedInputMode()) {
                        Log.e(TAG, "屏幕处于锁屏状态！");
                        unlockAndVib(true);
                    }

                    // 模拟打开通知栏信息
                    if (event.getParcelableData() != null && event.getParcelableData() instanceof Notification) {
                        Notification notification = (Notification) event.getParcelableData();
                        PendingIntent pendingIntent = notification.contentIntent;
                        try {
                            pendingIntent.send();
                        } catch (PendingIntent.CanceledException e) {
                            Log.e(TAG, e.getMessage());
                        }
                    }
                }
            }
        }
    }

    /**
     * 获取“开”按钮，
     *
     * @return
     */
    private List<AccessibilityNodeInfo> getOpenButtons()
    {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo == null)
        {
            return null;
        }
        SharedPreferences sharedPreferences = getSharedPreferences("mark", MODE_PRIVATE);
        String openId = (sharedPreferences == null) ? "" : sharedPreferences.getString("openId", "c2i");
        OPENBTN_ID = openId.contains(WX_ID_PREFIX) ? openId : (WX_ID_PREFIX + openId);
        Log.e(TAG, "OPENBTN_ID : " + OPENBTN_ID);
        List<AccessibilityNodeInfo> nodes = nodeInfo.findAccessibilityNodeInfosByViewId(OPENBTN_ID);
        if (nodes.size() > 0) {
            return nodes;
        }
        return null;
    }

    /**
     * 找到可领取的红包并模拟点击
     */
    private void getMoney() {
        AccessibilityNodeInfo rootInActiveWindow = getRootInActiveWindow();
        if (null != rootInActiveWindow)
        {
            List<AccessibilityNodeInfo> targetNodeList = rootInActiveWindow.findAccessibilityNodeInfosByText(GETMONEY_TEXT);
            if (targetNodeList == null)
            {
                targetNodeList = rootInActiveWindow.findAccessibilityNodeInfosByText(CHECKMONEY_TEXT);
            }
            if (null != targetNodeList && targetNodeList.size() > 0)
            {
                SharedPreferences sharedPreferences = getSharedPreferences("mark", MODE_PRIVATE);
                String mark = (sharedPreferences == null) ? "" : sharedPreferences.getString("mark_work", "");
                for (AccessibilityNodeInfo nodeInfo:targetNodeList)
                {
                    if (isNodeCanOpen(nodeInfo, mark)) {
                        Log.e(TAG, "luckMoneyInfo.isNodeCanOpen");
                        clickWallet(nodeInfo);
                    }
                }
            }
        }
    }

    /**
     * 找到可领取的QQ红包并模拟点击
     */
    private void getQQMoney() {
        AccessibilityNodeInfo rootInActiveWindow = getRootInActiveWindow();
        if (null != rootInActiveWindow) {
            // 普通红包
            List<AccessibilityNodeInfo> targetNodes = rootInActiveWindow.findAccessibilityNodeInfosByText(QQ_TIPS);
            if (null != targetNodes && targetNodes.size() > 0) {
                for (AccessibilityNodeInfo node : targetNodes) {
                    if (isCanGet(node)) {
                        float delayTime = mSharedPreUtils.getFloat(MainActivity.DELAY_TIME, 0);
                        try {
                            Thread.sleep((long) delayTime);
                            clickWallet(node);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            // 口令红包
            List<AccessibilityNodeInfo> cmdNodes = rootInActiveWindow.findAccessibilityNodeInfosByText(QQ_COMMAND);
            if (null != cmdNodes && cmdNodes.size() > 0) {
                List<AccessibilityNodeInfo> sendBtns = rootInActiveWindow.findAccessibilityNodeInfosByViewId("com.tencent.mobileqq:id/fun_btn");
                AccessibilityNodeInfo sendBtn = (sendBtns == null ? null : sendBtns.get(0));

                for (AccessibilityNodeInfo cmdNode : cmdNodes) {
                    String cmdText = cmdNode.getText().toString();
                    if (cmdText.equals(QQ_COMMAND)) {
                        clickWallet(cmdNode);
                    }
                }

                List<AccessibilityNodeInfo> clickCmdNodes = rootInActiveWindow.findAccessibilityNodeInfosByText(QQ_CLICK_COMMAND);
                if (null != clickCmdNodes && clickCmdNodes.size() > 0) {
                    for (AccessibilityNodeInfo clickCmdNode : clickCmdNodes) {
                        clickWallet(clickCmdNode);
                        if (sendBtn != null && sendBtn.isEnabled()) {
                            float delayTime = mSharedPreUtils.getFloat(MainActivity.DELAY_TIME, 0);
                            try {
                                Thread.sleep((long) delayTime);
                                sendBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
    }

    private void clickWallet(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo parent = node.getParent();
        while (null != parent) {
            if (parent.isClickable()) {
                Log.e(TAG, "--- clickWallet ---");
                parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                return;
            }
            parent = parent.getParent();
        }
    }

    private boolean isCanGet(AccessibilityNodeInfo node) {
        boolean isCan = true;
        SharedPreferences sharedPreferences = getSharedPreferences("mark", MODE_PRIVATE);
        String mark = (sharedPreferences == null) ? "" : sharedPreferences.getString("mark_work", "");
        if ("".equals(mark)) {
            isCan = true;
        } else {
            AccessibilityNodeInfo parent = node.getParent();
            String content = parent.getChild(0).getText().toString();
//            Log.e(TAG, "content = " + content);
            String[] works = mark.split("，");
            for (String work : works) {
                if (content.contains(work)) {
                    isCan = false;
                    break;
                }
            }
        }
        return isCan;
    }

    private final String TARGET_SYMBOL = "，";
    public boolean isNodeCanOpen(AccessibilityNodeInfo node, String mark)
    {
        try
        {
            AccessibilityNodeInfo nodeParent = node.getParent();
            if (!"android.widget.LinearLayout".equals(nodeParent.getClassName()))
            {
                return false;
            }
            String content = nodeParent.getChild(0).getText().toString(); // 获取文字红包内容
            if (!"".equals(mark))
            {
                if (mark.contains(TARGET_SYMBOL))
                {
                    String[] arrs = mark.split(TARGET_SYMBOL);
                    for (String arr:arrs)
                    {
                        if (content.contains(arr))
                        {
                            // 含关键字
                            return false;
                        }
                    }
                }
            }
            return true;
        }
        catch (Exception e)
        {
            Log.e("异常：", e.getMessage()  + "");
            return false;
        }
    }

    /**
     * 打开红包
     */
    private void openMoney() {
        // 点击繁体的“开”。现在新版本微信红包是点“开”来领取
        List<AccessibilityNodeInfo> info = getOpenButtons();
        if (null == info) {
            // 如果没有“开”可点，则直接关闭当前窗口，回到微信聊天界面
            if (!className.equals(WEIXIN_CLASSNAME) && !className.equals(WEIXIN_LUCKYMONEYDETAILUI)) {
                try {
                    Thread.sleep(1000);
                    Log.e(TAG, "没有'开'按钮");
//                    closeUI(RECEIVEUI_CLOSEBTN_ID);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } else {
            Log.e(TAG, "有'开'按钮");
//            info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
    }

    @Override
    public void onInterrupt() {
        // TODO Auto-generated method stub
        showMsg("抢红包服务已被中断！！！");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        keyguardLock = keyguardManager.newKeyguardLock("unLock");
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        mSharedPreUtils = new SharedPreUtils(this);
        initSoundPool();
        showMsg("服务已开启！！！");

//        setServiceToForeground();
    }

    private SoundPool mSoundPool;
    private HashMap<Integer, Integer> soundMap = new HashMap<>();

    private void initSoundPool() {
        mSoundPool = new SoundPool(1, AudioManager.STREAM_SYSTEM, 5);
        soundMap.put(1, mSoundPool.load(this, R.raw.hongbao_arrived, 1));
    }

    /**
     * 解锁并震动3s
     */
    private void unlockAndVib(boolean b) {
        if (b) {
            String value = mSharedPreUtils.getString("alarm", "");
            if (value.equals(MainActivity.NOTHING)) {
                return;
            }

            // 如果是锁屏状态，则解锁
            keyguardLock.disableKeyguard();
            if (!powerManager.isScreenOn()) {
                wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "bright");
                // 点亮屏幕
                wakeLock.acquire();
                Log.e(TAG, "亮屏");
            }

            if (value.equals(MainActivity.VOICE_VIBRATE)) {
                mSoundPool.play(soundMap.get(1), 1, 1, 0, 0, 1);
                vibrator.vibrate(PERIOD_TIME * 2);
            } else if (value.equals(MainActivity.VOICE)) {
                mSoundPool.play(soundMap.get(1), 1, 1, 0, 0, 1);
            } else if (value.equals(MainActivity.VIBRATE)) {
                vibrator.vibrate(PERIOD_TIME * 2);
            }
//            // 播放系统自带声音
//            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//            Ringtone r = RingtoneManager.getRingtone(this, notification);
//            r.play();

        }

        if (wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
            Log.e(TAG, "释放锁");
        }
    }

    private void showMsg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }

    protected long lastClickTime, lastClickTime2, lastClickTime3;

    /**
     * 判断控件在短时间内是否被连续点击了两次
     *
     * @return
     */
    public boolean isFastDoubleClick(int flag) {
        long timeD = 0l;
        long time = System.currentTimeMillis();

        switch (flag) {
            case 1:
                timeD = time - lastClickTime;
                lastClickTime = time;
                break;

            case 2:
                timeD = time - lastClickTime2;
                lastClickTime2 = time;
                break;

            case 3:
                timeD = time - lastClickTime3;
                lastClickTime3 = time;
                break;
        }

        if (0 < timeD && timeD < PERIOD_TIME) {
            return true;
        }
        return false;
    }

    //
    private void setServiceToForeground() {
        Intent mAccessibleIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, mAccessibleIntent, 0);
        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(pendingIntent)
                .setContentTitle(getText(R.string.app_name))
                .setContentText("正在抢红包...");
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify(103, builder.build());
        startForeground(103, builder.build());
    }
}
