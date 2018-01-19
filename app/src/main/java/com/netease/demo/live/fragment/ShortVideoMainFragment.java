package com.netease.demo.live.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.netease.demo.live.R;
import com.netease.demo.live.activity.TestActivity;
import com.netease.demo.live.activity.VideoDetailInfoActivity;
import com.netease.demo.live.activity.VideoShootActivity;
import com.netease.demo.live.base.BaseFragment;
import com.netease.demo.live.server.DemoServerHttpClient;
import com.netease.demo.live.server.entity.AddVideoResponseEntity;
import com.netease.demo.live.server.entity.VideoInfoEntity;
import com.netease.demo.live.shortvideo.UploadState;
import com.netease.demo.live.shortvideo.VideoAdapter;
import com.netease.demo.live.upload.constant.UploadType;
import com.netease.demo.live.upload.controller.UploadController;
import com.netease.demo.live.upload.model.VideoItem;
import com.netease.demo.live.util.file.FileUtil;
import com.netease.demo.live.widget.RecyclerViewEmptySupport;
import com.netease.nim.uikit.common.ui.dialog.CustomAlertDialog;
import com.netease.nim.uikit.common.ui.dialog.EasyAlertDialog;
import com.netease.nim.uikit.common.ui.dialog.EasyAlertDialogHelper;
import com.netease.nim.uikit.common.ui.recyclerview.adapter.BaseFetchLoadAdapter;
import com.netease.nim.uikit.common.ui.recyclerview.listener.OnItemClickListener;
import com.netease.nim.uikit.common.ui.recyclerview.loadmore.MsgListFetchLoadMoreView;
import com.netease.nim.uikit.common.util.storage.StorageType;
import com.netease.nim.uikit.common.util.storage.StorageUtil;
import com.netease.nim.uikit.common.util.sys.NetworkUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ShortVideoMainFragment extends BaseFragment implements View.OnClickListener, UploadController.UploadUi, VideoAdapter.EventListener {
    // constant
    private static final int VIDEO_LIMIT = 10;
    // view
    private ImageView takeVideoImage; // 开始拍摄按钮
    private RecyclerViewEmptySupport videoListView; // 视频列表控件

    // data
    private int videoCount; // 已经上传服务器的视频数量
    private VideoAdapter videoAdapter;
    private List<VideoItem> items; // 视频item列表
    private HashMap<Long, VideoInfoEntity> videosFromServer;

    private interface FetchVideoListener {
        void onFetchVideoDone();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_short_video_main, null);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        findViews();
        setListener();
        initData();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        UploadController.getInstance().suspend();
    }

    private void findViews() {

        takeVideoImage = findView(R.id.take_video_image);
        videoListView = findView(R.id.video_list);

        // adapter
        items = new ArrayList<>();
        videoAdapter = new VideoAdapter(videoListView, R.layout.video_item_layout, items);
        videoAdapter.setEventListener(this);
        videoAdapter.setFetchMoreView(new MsgListFetchLoadMoreView());
        videoAdapter.setOnFetchMoreListener(new BaseFetchLoadAdapter.RequestFetchMoreListener() {
            @Override
            public void onFetchMoreRequested() {
                getVideoList(null);
            }
        });
        videoListView.addOnItemTouchListener(new OnItemClickListener<VideoAdapter>() {
            @Override
            public void onItemClick(VideoAdapter adapter, View view, int position) {
                VideoInfoEntity entity = adapter.getItem(position).getEntity();
                if (entity != null) {
                    if (TextUtils.isEmpty(entity.getSnapshotUrl())) {
                        entity.setSnapshotUrl(adapter.getItem(position).getUriString());
                    }
                    VideoDetailInfoActivity.startActivity(getActivity(), adapter.getItem(position).getEntity(),
                            adapter.getItem(position).getState(), false);
                }
            }

            @Override
            public void onItemLongClick(VideoAdapter adapter, View view, int position) {
                onNormalLongClick(position);
            }
        });
        videoListView.setLayoutManager(new LinearLayoutManager(getContext()));
        videoListView.setEmptyView(findView(R.id.list_empty));
        videoListView.setAdapter(videoAdapter);
    }

    private void setListener() {
        takeVideoImage.setOnClickListener(this);
    }

    private void initData() {
        UploadController.getInstance().init(getContext());
        UploadController.getInstance().attachUi(this);
        videosFromServer = new HashMap<>();
        getVideoList(null);
        UploadController.getInstance().loadVideoDataFromLocal(UploadType.SHORT_VIDEO);
        // 删除thumb文件夹里面的缓存图片
        FileUtil.delete(new File(StorageUtil.getDirectoryByDirType(StorageType.TYPE_THUMB_IMAGE)));
    }

    private void getVideoList(final FetchVideoListener fetchVideoListener) {
        DemoServerHttpClient.getInstance().videoInfoGet(null, UploadType.SHORT_VIDEO, new DemoServerHttpClient.DemoServerHttpCallback<List<VideoInfoEntity>>() {
            @Override
            public void onSuccess(List<VideoInfoEntity> entities) {
                List<VideoInfoEntity> videoInfoEntities = new ArrayList<>();
                List<VideoItem> videoItems = new ArrayList<>();
                for (VideoInfoEntity videoInfoEntity : entities) {
                    if (videosFromServer.containsKey(videoInfoEntity.getVid())) {
                        continue;
                    }
                    videoInfoEntities.add(videoInfoEntity);
                    VideoItem videoItem = new VideoItem();
                    videoItem.setEntity(videoInfoEntity);
                    videoItems.add(videoItem);
                    videosFromServer.put(videoInfoEntity.getVid(), videoInfoEntity);
                }

                // 顶部加载
                if (videoInfoEntities.size() <= 0) {
                    videoAdapter.fetchMoreEnd(true);
                } else {
                    videoCount = videoInfoEntities.size();
                    videoAdapter.fetchMoreComplete(videoListView, videoItems);
                }
                if (fetchVideoListener != null) {
                    fetchVideoListener.onFetchVideoDone();
                }
            }

            @Override
            public void onFailed(int code, String errorMsg) {
            }
        });
    }

    /**
     * 确认是否可以开始拍摄视频
     */
    private void checkTakeVideo() {
        if (videoCount >= VIDEO_LIMIT) {
            showVideoCountDialog();
        } else {
//            startActivity(new Intent(getActivity(), TestActivity.class));
            startActivity(new Intent(getActivity(), VideoShootActivity.class));
//            VideoShootActivity.startActivityForResult(getActivity());
        }
    }

    /**
     * 视频数量已达上限
     */
    private void showVideoCountDialog() {
        EasyAlertDialogHelper.OnDialogActionListener listener = new EasyAlertDialogHelper.OnDialogActionListener() {

            @Override
            public void doCancelAction() {
                // go to delete
            }

            @Override
            public void doOkAction() {
                // go on
//                startActivity(new Intent(getActivity(), TestActivity.class));
                startActivity(new Intent(getActivity(), VideoShootActivity.class));
            }
        };
        final EasyAlertDialog dialog = EasyAlertDialogHelper.createOkCancelDiolag(getActivity(),
                getString(R.string.video_reach_limit),
                getString(R.string.video_reach_limit_content),
                getString(R.string.continue_shooting), getString(R.string.go_delete_video), false, listener);
        dialog.show();
    }

    // 长按
    private void onNormalLongClick(final int position) {
        CustomAlertDialog alertDialog = new CustomAlertDialog(getActivity());
        alertDialog.setCancelable(true);
        alertDialog.setCanceledOnTouchOutside(true);

        alertDialog.addItem(getString(R.string.delete), new CustomAlertDialog.onSeparateItemClickListener() {

            @Override
            public void onClick() {
                showDeleteConfirm(position, videoAdapter.getItem(position).getEntity());
            }
        });
        alertDialog.show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.take_video_image:
                checkTakeVideo();
                break;
        }
    }

    private void uploadFile(final VideoItem videoItem) {
        if (!NetworkUtil.isNetAvailable(getActivity())) {
            EasyAlertDialogHelper.showOneButtonDiolag(getActivity(), null, getString(R.string.network_is_not_available),
                    getString(R.string.i_know), false, null);
        }

        doRealUpload(videoItem);
    }

    /**
     * 调用上传接口，上传视频
     */
    private void doRealUpload(VideoItem videoItem) {
        videoItem.setType(UploadType.SHORT_VIDEO);
        videoItem.setState(UploadState.STATE_WAIT);
        List<VideoItem> videoItemList = new ArrayList<>(1);
        videoItemList.add(videoItem);
        UploadController.getInstance().uploadLocalItem(videoItemList, UploadType.SHORT_VIDEO, true);
    }

    private int getItemIndex(String id) {
        for (int i = 0; i < items.size(); i++) {
            VideoItem videoItem = items.get(i);
            if (TextUtils.equals(videoItem.getId(), id)) {
                return i;
            }
        }

        return -1;
    }

    private int getItemIndexByVid(long vid) {
        for (int i = 0; i < items.size(); i++) {
            VideoItem videoItem = items.get(i);
            if (videoItem.getEntity() != null && videoItem.getEntity().getVid() == vid) {
                return i;
            }
        }
        return -1;
    }

    /**
     * ************************ Upload ui ************************
     */

    @Override
    public boolean isUiInit() {
        return false;
    }

    @Override
    public void updateAllItems() {

    }

    @Override
    public void updateUploadState(int state) {
        if (state == UploadState.STATE_UPLOAD_COMPLETE) {
            videoCount++;
        }
    }

    @Override
    public void updateItemProgress(String id, int progress, int state) {
        int index = getItemIndex(id);
        if (index >= 0) {
            videoAdapter.putProgress(id, progress);
            videoAdapter.notifyDataItemChanged(index);
        }
    }

    @Override
    public void onDataSetChanged(List<VideoItem> data) {
        for (VideoItem item : data) {
            if (items.contains(item)) {
                continue;
            }
            items.add(0, item);
        }
        videoAdapter.notifyDataSetChanged();
    }

    @Override
    public void onAddVideoResult(int code, String id, AddVideoResponseEntity addVideoResponseEntity) {
        if (code == 200) {
            int index = getItemIndex(id);
            if (index >= 0 && index < items.size() && items.get(index) != null) {
                VideoItem videoItem = items.get(index);
                videoItem.setVid(addVideoResponseEntity.getVid());
                videoItem.setEntity(addVideoResponseEntity.getVideoInfoEntity());
                // 删除上传成功的文件
                FileUtil.deleteFile(videoItem.getFilePath());
            }
            videoAdapter.notifyDataItemChanged(index);
        }
    }

    /**
     * ************************ adapter listener *********************
     */

    @Override
    public void onRetryUpload(VideoItem videoItem) {
        uploadFile(videoItem);
    }

    @Override
    public void onVideoDeleted(int position, VideoItem videoItem) {
        showDeleteConfirm(position, videoItem.getEntity());
    }

    private void showDeleteConfirm(final int position, final VideoInfoEntity videoInfoEntity) {
        EasyAlertDialogHelper.OnDialogActionListener listener = new EasyAlertDialogHelper.OnDialogActionListener() {

            @Override
            public void doCancelAction() {
            }

            @Override
            public void doOkAction() {
                showNetworkDialog(position, videoInfoEntity);
            }
        };

        final EasyAlertDialog dialog = EasyAlertDialogHelper.createOkCancelDiolag(getActivity(), null,
                getString(R.string.delete_confirm_tip), false, listener);
        dialog.show();
    }

    private void showNetworkDialog(int position, VideoInfoEntity videoInfoEntity) {
        if (NetworkUtil.isNetAvailable(getActivity())) {
            doRealDelete(position, videoInfoEntity);
        } else {
            EasyAlertDialogHelper.showOneButtonDiolag(getActivity(), null, getString(R.string.network_is_not_available),
                    getString(R.string.i_know), true, null);
        }
    }

    private void doRealDelete(final int position, VideoInfoEntity videoInfoEntity) {
        if (videoInfoEntity == null || videoInfoEntity.getVid() == 0) {
            return;
        }
        DemoServerHttpClient.getInstance().videoDelete(videoInfoEntity.getVid(), null, new DemoServerHttpClient.DemoServerHttpCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                videoCount--;
                videoAdapter.remove(position);
                videoAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailed(int code, String errorMsg) {

            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case VideoShootActivity.REQUEST_CODE:
                    VideoItem videoItem = (VideoItem) data.getSerializableExtra(VideoShootActivity.EXTRA_VIDEO_ITEM);
                    uploadFile(videoItem);
                    break;
                case VideoDetailInfoActivity.REQUEST_CODE:
                    VideoInfoEntity infoEntity = (VideoInfoEntity) data.getSerializableExtra(VideoDetailInfoActivity.EXTRA_VIDEO_ENTITY);
                    doRealDelete(getItemIndexByVid(infoEntity.getVid()), infoEntity);
                    break;
            }
        }
    }
}
