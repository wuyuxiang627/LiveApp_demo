package com.netease.demo.live.activity;


import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.TextView;

import com.netease.demo.live.R;
import com.netease.demo.live.base.BaseActivity;
import com.netease.demo.live.fragment.LiveEnterFragment;
import com.netease.demo.live.fragment.ShortVideoMainFragment;
import com.netease.demo.live.fragment.VideoMainFragment;
import com.netease.demo.live.nim.config.perference.Preferences;
import com.netease.demo.live.server.DemoServerHttpClient;
import com.netease.demo.live.upload.model.UploadTotalDataAccessor;


public class MainActivity extends BaseActivity {
    public static final String TAG_LIVE_FRAGMENT = LiveEnterFragment.class.getSimpleName();
    public static final String TAG_VIDEO_FRAGMENT = VideoMainFragment.class.getSimpleName();
    public static final String TAG_SHORT_VIDEO_FRAGMENT = ShortVideoMainFragment.class.getSimpleName();
    public static final String EXTRA_FROM_UPLOAD_NOTIFY = "extra_from_upload_notify"; //由上传通知启动

    public String currentFragment = TAG_LIVE_FRAGMENT;

    View tab_live, tab_upload;
    TextView tabShortVideo;
    View btn_login_out;

    public static void start(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void handleIntent(Intent intent) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        switchFragment();
    }

    boolean isFromUploadNotify;
    boolean saveBackStack;

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        //若用户点击 通知栏显示的上传通知,启动MainActivity, 则切换至视频上传Fragment
        isFromUploadNotify = intent.getBooleanExtra(EXTRA_FROM_UPLOAD_NOTIFY, false);
        if (isFromUploadNotify) {
            saveBackStack = true;
            currentFragment = TAG_VIDEO_FRAGMENT;
            switchFragment();
        }
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_main;
    }

    @Override
    protected void initView() {
        tab_live = findView(R.id.btn_tab_live);
        tab_upload = findView(R.id.btn_tab_upload);
        tabShortVideo = findView(R.id.btn_tab_short_video);
        btn_login_out = findView(R.id.btn_login_out);
        clickView();
    }

    private void clickView() {

        tab_live.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentFragment = TAG_LIVE_FRAGMENT;
                switchFragment();
            }
        });

        tab_upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentFragment = TAG_VIDEO_FRAGMENT;
                switchFragment();
            }
        });

        tabShortVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentFragment = TAG_SHORT_VIDEO_FRAGMENT;
                switchFragment();
            }
        });

        btn_login_out.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

    }

    private void switchFragment() {
        tab_live.setEnabled(!currentFragment.equals(TAG_LIVE_FRAGMENT));
        tab_upload.setEnabled(!currentFragment.equals(TAG_VIDEO_FRAGMENT));
        tabShortVideo.setEnabled(!currentFragment.equals(TAG_SHORT_VIDEO_FRAGMENT));

        String fragmentTag = currentFragment;
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag(fragmentTag);
        if (fragment == null) {
            if (fragmentTag.equals(TAG_LIVE_FRAGMENT)) {
                fragment = new LiveEnterFragment();
            } else if (fragmentTag.equals(TAG_SHORT_VIDEO_FRAGMENT)) {
                fragment = new ShortVideoMainFragment();
            } else {
                fragment = new VideoMainFragment();
            }
        }

        if (isFromUploadNotify) {
            //若由上传通知点击进入, 则启动点播时,默认切到上传管理子Fragment
            ((VideoMainFragment) fragment).setNextResumeFragmentToVideoManage();
            isFromUploadNotify = false;
        }

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.main_container, fragment, fragmentTag);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    boolean confirmed = false;

    @Override
    public void onBackPressed() {

        if (!confirmed) {
            showConfirmDialog(null, "确定退出该账号?", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    confirmed = true;
                    UploadTotalDataAccessor.getInstance().clear();
                    DemoServerHttpClient.getInstance().logout();
                    Preferences.saveLoginState(false);
                    onBackPressed();
                }
            }, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
        } else {
            //清空Activity管理的缓存栈
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            LoginActivity.start(this);
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag(currentFragment);
        if (fragment != null) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }

    }
}
