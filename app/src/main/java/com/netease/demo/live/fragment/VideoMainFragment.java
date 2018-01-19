package com.netease.demo.live.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.netease.demo.live.R;
import com.netease.demo.live.base.BaseFragment;

/**
 * Created by zhukkun on 3/6/17.
 */
public class VideoMainFragment extends BaseFragment {

    public static final String TAG_VIDEO_MANAGE = VideoManageFragment.class.getSimpleName();
    public static final String TAG_VIDEO_ENTER = VideoEnterFragment.class.getSimpleName();

    private String currentFragment = TAG_VIDEO_MANAGE;

    View subTabLeft, subTabRight;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return  inflater.inflate(R.layout.fragment_video_main, null);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        subTabLeft = findView(R.id.subtab_left);
        subTabRight = findView(R.id.subtab_right);

        subTabLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentFragment = TAG_VIDEO_MANAGE;
                switchFragment();

            }
        });

        subTabRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentFragment = TAG_VIDEO_ENTER;
                switchFragment();

            }
        });

        switchFragment();
    }

    private void switchFragment(){

        subTabLeft.setEnabled(currentFragment.equals(TAG_VIDEO_ENTER));
        subTabRight.setEnabled(currentFragment.equals(TAG_VIDEO_MANAGE));

        String fragmentTag = currentFragment;
        FragmentManager fragmentManager = getChildFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag(fragmentTag);
        if(fragment == null){
            if(fragmentTag.equals(TAG_VIDEO_MANAGE)){
                fragment = new VideoManageFragment();
            }else{
                fragment = new VideoEnterFragment();
            }
        }

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.video_main_container, fragment, fragmentTag);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        FragmentManager fragmentManager = getChildFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag(currentFragment);
        if(fragment!=null){
            fragment.onActivityResult(requestCode, resultCode, data);
        }

    }

    boolean nextResumeFragmentToVideoManage;
    /**
     * 设置下次恢复此Fragment时,显示的子fragment为视频管理页
     * 用于响应用户点击上传通知时,展示视频管理页
     */
    public void setNextResumeFragmentToVideoManage(){
        nextResumeFragmentToVideoManage = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        if(nextResumeFragmentToVideoManage) {
            currentFragment = TAG_VIDEO_MANAGE;
            switchFragment();
            nextResumeFragmentToVideoManage = false;
        }
    }
}
