package com.netease.demo.live.activity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.netease.demo.live.R;
import com.netease.demo.live.base.BaseActivity;
import com.netease.demo.live.upload.adapter.VideoGalleryAdapter;
import com.netease.demo.live.upload.controller.VideoGalleryController;
import com.netease.demo.live.upload.model.LineContainer;

import java.util.List;

/**
 * Created by zhukkun on 2/24/17.
 */
public class VideoGalleryActivity extends BaseActivity implements VideoGalleryController.VideoSelectUi, VideoGalleryAdapter.GalleryUiDelegate {

    public static final String EXTRAL_SELECTED_LIST = "SELECTED_LIST";
    public static final int EXTRA_REQUEST_CODE = 100;

    RecyclerView recyclerView;
    VideoGalleryAdapter adapter;
    VideoGalleryController controller;

    private TextView tv_count;
    private TextView tv_hint;
    private TextView tv_next;
    private TextView tv_title;
    private View back;

    boolean isInit;

    public static void startActivityForResult(Activity context, int largestSelectCount, long filterTime) {
        Intent intent = new Intent(context, VideoGalleryActivity.class);
        intent.putExtra(VideoGalleryController.EXTRAL_LARGEST_COUNT, largestSelectCount);
        intent.putExtra(VideoGalleryController.EXTRA_FILTER_TIME, filterTime);
        context.startActivityForResult(intent, EXTRA_REQUEST_CODE);
    }

    @Override
    protected void handleIntent(Intent intent) {
        controller.handleIntent(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        controller = new VideoGalleryController(this);
        controller.init(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        controller.suspend();
    }

    @Override
    protected void onResume() {
        super.onResume();
        controller.attachUi(this);
        isInit = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        controller.detachUi(this);
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_video_select;
    }

    @Override
    protected void initView() {

        tv_count = findView(R.id.tv_count);
        tv_hint = findView(R.id.tv_hint);
        tv_next = findView(R.id.tv_next);
        tv_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra(EXTRAL_SELECTED_LIST, controller.getDataAccessor().getSelectedArray());
                setResult(RESULT_OK, intent);
                finish();
            }
        });
        tv_title = findView(R.id.tv_title);
        back = findView(R.id.iv_back);

        tv_title.setText("选择视频");
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        initRecyclerView();
    }

    private void initRecyclerView() {
        recyclerView = findView(R.id.recycler_view);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter = new VideoGalleryAdapter(this, controller.getDataAccessor(), this));
    }

    @Override
    public boolean isUiInit() {
        return isInit;
    }

    @Override
    public void populateVideoData(List<LineContainer> datas) {
        adapter.setDatas(datas);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void initBottomBar(int selected_count, int largest_count) {
        updateBottomBarState(selected_count, largest_count);
        tv_hint.setText("最多可选择" + largest_count + "个视频");
    }

    public void updateBottomBarState(int selected_count, int largest_count) {
        tv_next.setEnabled(selected_count >= 1);
        tv_count.setText(selected_count + "/" + largest_count);
    }

    @Override
    public void onSelectCountChanged(int selected, int total) {
        updateBottomBarState(selected, total);
    }

    @Override
    public void showMoreThan1GConfirm(final Runnable confirmRunnable) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("建议选择1G以下的视频上传,您选择了1G以上的视频.");
        builder.setPositiveButton("继续上传",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        confirmRunnable.run();
                    }
                });
        builder.setNegativeButton(R.string.cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        builder.show();
    }

    @Override
    public void showMoreThan5GConfirm() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("单个视频不可超过5G, 请重新选择");
        builder.setPositiveButton("重新选择视频",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        builder.show();
    }
}
