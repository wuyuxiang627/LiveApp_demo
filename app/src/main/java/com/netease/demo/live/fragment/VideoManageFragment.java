package com.netease.demo.live.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;

import com.netease.demo.live.R;
import com.netease.demo.live.activity.VideoGalleryActivity;
import com.netease.demo.live.base.BaseFragment;
import com.netease.demo.live.server.DemoServerHttpClient;
import com.netease.demo.live.server.entity.AddVideoResponseEntity;
import com.netease.demo.live.server.entity.VideoInfoEntity;
import com.netease.demo.live.upload.adapter.UploadAdapter;
import com.netease.demo.live.upload.constant.UploadType;
import com.netease.demo.live.upload.controller.UploadController;
import com.netease.demo.live.upload.controller.UploadService;
import com.netease.demo.live.upload.controller.VideoGalleryController;
import com.netease.demo.live.upload.model.UploadDataAccessor;
import com.netease.demo.live.upload.model.VideoItem;
import com.netease.demo.live.util.network.NetworkUtils;
import com.netease.demo.live.widget.RecyclerViewEmptySupport;
import com.netease.nim.uikit.common.ui.recyclerview.adapter.BaseFetchLoadAdapter;
import com.netease.nim.uikit.common.ui.recyclerview.loadmore.MsgListFetchLoadMoreView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 点播：视频管理页面
 * Created by zhukkun on 3/6/17.
 */
public class VideoManageFragment extends BaseFragment implements UploadController.UploadUi {

    // data
    private boolean isUiInit;
    private List<VideoItem> items;
    private List<Long> remoteVidList;
    private List<String> localVideoIdList;
    private UploadDataAccessor dataAccessor;
    private HashMap<Long, VideoInfoEntity> videosFromServer;

    // view
    private RecyclerViewEmptySupport recyclerView;
    private UploadAdapter adapter;
    private Button btnSelectFile;
    private View divide_line;
    private ProgressBar loadingBar;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UploadController.getInstance().init(getContext());
        dataAccessor = UploadController.getInstance().getDataAccessor();
        UploadController.getInstance().loadVideoDataFromLocal(UploadType.VIDEO);

        items = new ArrayList<>();
        remoteVidList = new ArrayList<>();
        localVideoIdList = new ArrayList<>();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        UploadController.getInstance().suspend();
        items.clear();
        remoteVidList.clear();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_upload, null);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (items != null) {
            items.clear();
        }
        remoteVidList.clear();
        localVideoIdList.clear();
        videosFromServer = new HashMap<>();
        dataAccessor.getCloudItemList().clear();
        items.addAll(0, dataAccessor.getLocalItemList());

        btnSelectFile = findView(R.id.btn_select_file);
        btnSelectFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnSelectFile.setEnabled(false);
                //每次点击进入视频选择页前, 先请求服务端已有的总量
                if (!NetworkUtils.isNetworkConnected(false)) {
                    btnSelectFile.setEnabled(true);
                    showToast("当前网络不可用,请检查网络");
                    return;
                }
                getCloudVideoData(true);
            }
        });
        loadingBar = findView(R.id.loading_bar);
        divide_line = findView(R.id.video_divide_line);
        initRecyclerView();
        UploadController.getInstance().attachUi(this);
        isUiInit = true;
        getCloudVideoData(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        UploadController.getInstance().detachUi(this);
    }

    private void initRecyclerView() {
        recyclerView = findView(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setEmptyView(findView(R.id.list_empty));

        adapter = new UploadAdapter(recyclerView, items, UploadController.getInstance());
        recyclerView.setAdapter(adapter);
        adapter.attachRecyclerView(recyclerView);
        adapter.setFetchMoreView(new MsgListFetchLoadMoreView());
        adapter.setOnFetchMoreListener(new BaseFetchLoadAdapter.RequestFetchMoreListener() {
            @Override
            public void onFetchMoreRequested() {
                getCloudVideoData(false);
            }
        });
    }

    /**
     * 从云端获取数据
     */
    private void getCloudVideoData(final boolean startGallery) {
        showLoading(true);

        DemoServerHttpClient.getInstance().videoInfoGet(null, UploadType.VIDEO, new DemoServerHttpClient.DemoServerHttpCallback<List<VideoInfoEntity>>() {
            @Override
            public void onSuccess(List<VideoInfoEntity> entities) {
                showLoading(false);
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
                dataAccessor.setCloudVideoItems(videoItems);

                // 顶部加载
                if (videoInfoEntities.size() <= 0) {
                    adapter.fetchMoreEnd(true);
                    startTranscodingQuery();
                } else {
                    adapter.fetchMoreComplete(recyclerView, videoItems);
                }

                if (startGallery) {
                    canStartGallery(dataAccessor.getTotalCount());
                }
            }

            @Override
            public void onFailed(int code, String errorMsg) {
                showLoading(false);
                if (startGallery)
                    canStartGallery(dataAccessor.getTotalCount());
            }
        });
    }

    /**
     * 如果云端有正在转码的视频，开启定时查询转码状态
     */
    private void startTranscodingQuery() {
        for (VideoItem item : items) {
            if (item.getState() == UploadService.STATE_TRANSCODEING) {
                UploadController.getInstance().getDataAccessor().addTransCodingList(item);
            }
        }
        if (UploadController.getInstance().getDataAccessor().getTranscodingVidList().size() > 0) {
            UploadController.getInstance().startTranscodeQuery();
        }
    }

    private void canStartGallery(int totalCount) {
        if (totalCount >= VideoGalleryController.DEFAULT_LARGEST_COUNT) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setMessage("您最多可上传" + VideoGalleryController.DEFAULT_LARGEST_COUNT + "个视频体验demo");
            builder.setPositiveButton("我知道了",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            builder.show();
        } else {
            VideoGalleryActivity.startActivityForResult(getActivity(),
                    VideoGalleryController.DEFAULT_LARGEST_COUNT - UploadController.getInstance().getDataAccessor().getTotalCount(),
                    -1);
        }
        btnSelectFile.setEnabled(true);
    }

    public void showLoading(boolean show) {
        if (show) {
            loadingBar.setVisibility(View.VISIBLE);
        } else {
            loadingBar.setVisibility(View.INVISIBLE);
            btnSelectFile.setEnabled(true);
        }
    }


    @Override
    public void onDataSetChanged(List<VideoItem> data) {
        for (VideoItem item : data) {
            long vid = item.getEntity() == null ? 0 : item.getEntity().getVid();
            if (localVideoIdList.contains(item.getId())
                    || remoteVidList.contains(vid)) {
                continue;
            }
            items.add(0, item);
            if (!TextUtils.isEmpty(item.getId())) {
                localVideoIdList.add(item.getId());
            }
            if (vid != 0) {
                remoteVidList.add(vid);
            }
        }
        adapter.notifyDataSetChanged();

        if (data.size() >= VideoGalleryController.DEFAULT_LARGEST_COUNT) {
            btnSelectFile.setSelected(true);
        } else {
            btnSelectFile.setSelected(false);
        }

        if (data.size() > 0) {
            divide_line.setVisibility(View.VISIBLE);
        } else {
            divide_line.setVisibility(View.GONE);
        }
    }

    @Override
    public void onAddVideoResult(int code, String id, AddVideoResponseEntity addVideoResponseEntity) {
        // 添加到应用服务器成功，等待转码
        if (code == 200) {
            videosFromServer.put(addVideoResponseEntity.getVideoInfoEntity().getVid(), addVideoResponseEntity.getVideoInfoEntity());
        }
    }

    @Override
    public void updateAllItems() {
        adapter.notifyDataSetChanged();
    }

    @Override
    public void updateUploadState(int state) {
        switch (state) {
            case UploadService.STATE_UPLOAD_COMPLETE:
                break;
            case UploadService.STATE_UPLOADING:
                break;
        }
    }

    @Override
    public void updateItemProgress(String id, int progress, int state) {
        int index = getItemIndex(id);
        if (index >= 0) {
            adapter.notifyDataItemChanged(index);
        }
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

    @Override
    public Context getContext() {
        return getActivity();
    }


    @Override
    public boolean isUiInit() {
        return isUiInit;
    }

    /**
     * implement of UploadUi
     **/


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        List<VideoItem> selectedItems = (ArrayList<VideoItem>) data.getSerializableExtra(VideoGalleryActivity.EXTRAL_SELECTED_LIST);
        if (selectedItems != null && selectedItems.size() > 0) {
            UploadController.getInstance().uploadLocalItem(selectedItems, UploadType.VIDEO, true);
        }
    }

}
