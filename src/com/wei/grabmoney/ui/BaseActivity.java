package com.wei.grabmoney.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.wei.grabmoney.R;

/**
 * 其它Activity的父类
 */
public class BaseActivity extends Activity
{
    protected String TAG = getClass().getSimpleName();
    protected Context mContext;
    private LinearLayout viewGroup;
    protected LinearLayout titleCustom;
    protected LinearLayout container;
    protected RelativeLayout titleContainer;
    protected LinearLayout headMid;
    protected View back;
    protected TextView title;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mContext = this;
        initBaseView();
    }

    private void initBaseView()
    {
        viewGroup = (LinearLayout) View.inflate(this, R.layout.title_bar, null);
        titleCustom = (LinearLayout) viewGroup.findViewById(R.id.title_custom);
        titleContainer = (RelativeLayout) viewGroup.findViewById(R.id.title_container);
        container = (LinearLayout) viewGroup.findViewById(R.id.container);
        headMid = (LinearLayout) viewGroup.findViewById(R.id.ll_head);
        back = viewGroup.findViewById(R.id.title_back);
        back.setVisibility(View.VISIBLE);
        title = (TextView) viewGroup.findViewById(R.id.title);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }

    @Override
    public void setContentView(int layoutResID) {
        View contentView = View.inflate(this, layoutResID, null);
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, -1);
        contentView.setLayoutParams(layoutParams);
        container.addView(contentView);
        super.setContentView(viewGroup);
    }

    public void hideTitle() {
        titleContainer.setVisibility(View.GONE);
        //title.setVisibility(View.GONE);
    }

    public void setCustomTitle(int resid) {
        title.setText(resid);
        title.setVisibility(View.VISIBLE);
    }

    public void setCustomTitle(String text) {
        title.setText(text);
        title.setVisibility(View.VISIBLE);
    }


    protected void showMsg(String msg)
    {
        Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
    }

    protected void showAlertDialog(String msg, String negativeTxt, String positiveTxt, final View.OnClickListener clickListener)
    {
        new AlertDialog.Builder(mContext)
                .setTitle("提示")
                .setMessage(msg)
                .setNegativeButton(negativeTxt, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setPositiveButton(positiveTxt, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        if (null != clickListener)
                        {
                            clickListener.onClick(null);
                        }
                        dialog.dismiss();
                    }
                }).show();
    }
}
