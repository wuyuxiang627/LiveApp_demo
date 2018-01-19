package com.netease.demo.live.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.netease.demo.live.R;
import com.netease.demo.live.base.BaseActivity;
import com.netease.demo.live.server.entity.VideoInfoEntity;
import com.netease.demo.live.upload.VideoUtils;
import com.netease.demo.live.upload.adapter.TranscodeVideoListAdapter;
import com.netease.demo.live.upload.controller.UploadService;
import com.netease.nim.uikit.common.ui.dialog.EasyAlertDialog;
import com.netease.nim.uikit.common.ui.dialog.EasyAlertDialogHelper;
import com.netease.nim.uikit.common.util.sys.NetworkUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhukkun on 3/13/17.
 */
public class VideoDetailInfoActivity extends BaseActivity {

    public static final String EXTRA_VIDEO_ENTITY = "video_entity";
    public static final String EXTRA_VIDEO_STATE = "video_state";
    public static final String EXTRA_VIDEO_NEED_TRANSCODE = "need_transcode";
    public static final int REQUEST_CODE = 1001;

    // view
    View tv_back;
    TextView tv_title;
    ScrollView scrollView;
    ImageView iv_thumb;
    TextView tv_video_title;
    TextView tv_source_formate, tv_source_size, tv_source_url;
    TextView tv_play, tv_share, tv_delete;
    ListView listView;
    LinearLayout transcodeLayout;

    // data
    VideoInfoEntity videoInfoEntity;
    List<TranscodeVideoListAdapter.TranscodeEntity> transcodeEntityList = new ArrayList<>();
    boolean needTranscode;

    public static void startActivity(Context context, VideoInfoEntity entity,
                                     int state, boolean needTranscode){
        Intent intent = new Intent(context, VideoDetailInfoActivity.class);
        intent.putExtra(EXTRA_VIDEO_ENTITY, entity);
        intent.putExtra(EXTRA_VIDEO_STATE, state);
        intent.putExtra(EXTRA_VIDEO_NEED_TRANSCODE, needTranscode);
        ((Activity)context).startActivityForResult(intent, REQUEST_CODE);
    }

    @Override
    protected void handleIntent(Intent intent) {
        videoInfoEntity = (VideoInfoEntity) intent.getSerializableExtra(EXTRA_VIDEO_ENTITY);
        int state = intent.getIntExtra(EXTRA_VIDEO_STATE, UploadService.STATE_TRANSCODEING);
        TranscodeVideoListAdapter.TranscodeEntity entity = new TranscodeVideoListAdapter.TranscodeEntity();
        entity.setTitle("高清MP4"); //显示的文案与 服务端转码格式文案不一致.若要删除某种格式,以服务端转码格式值为准
        entity.setFormat(3);
        entity.setUrl(videoInfoEntity.getShdMp4Url());
        entity.setSize(videoInfoEntity.getShdMp4Size());
        entity.setState(state);
        entity.setVid(videoInfoEntity.getVid());
        addToListIfNeed(entity);

        entity = new TranscodeVideoListAdapter.TranscodeEntity();
        entity.setTitle("标清FLV");
        entity.setFormat(5);
        entity.setUrl(videoInfoEntity.getHdFlvUrl());
        entity.setSize(videoInfoEntity.getHdFlvSize());
        entity.setState(state);
        entity.setVid(videoInfoEntity.getVid());
        addToListIfNeed(entity);

        entity = new TranscodeVideoListAdapter.TranscodeEntity();
        entity.setTitle("流畅HLS");
        entity.setFormat(7);
        entity.setUrl(videoInfoEntity.getSdHlsUrl());
        entity.setSize(videoInfoEntity.getSdHlsSize());
        entity.setState(state);
        entity.setVid(videoInfoEntity.getVid());
        addToListIfNeed(entity);
        needTranscode = intent.getBooleanExtra(EXTRA_VIDEO_NEED_TRANSCODE, false);
    }

    private void addToListIfNeed(TranscodeVideoListAdapter.TranscodeEntity entity) {
        if(entity.getState() == UploadService.STATE_TRANSCODE_COMPLETE){
            if(entity.getUrl()!=null){
                transcodeEntityList.add(entity);
            }
        }else{
            transcodeEntityList.add(entity);
        }
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_video_detail_info;
    }

    @Override
    protected void initView() {
        tv_back = findView(R.id.iv_back);
        tv_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        scrollView = findView(R.id.scroll_view);
        tv_title = findView(R.id.tv_title);
        tv_title.setText("视频详情");

        iv_thumb = findView(R.id.iv_thumb);
        tv_video_title = findView(R.id.tv_video_title);
        listView = findView(R.id.transcode_video_list);

        tv_source_formate = findView(R.id.tv_format);
        tv_source_size = findView(R.id.tv_size);
        tv_source_url = findView(R.id.tv_info);

        tv_play = findView(R.id.tv_play);
        tv_share = findView(R.id.tv_share);
        tv_share.setText(needTranscode ? R.string.share : R.string.copy);
        tv_delete = findView(R.id.tv_delete);
        tv_delete.setVisibility(needTranscode ? View.GONE : View.VISIBLE);

        transcodeLayout = findView(R.id.transcode_layout);
        transcodeLayout.setVisibility(needTranscode ? View.VISIBLE : View.GONE);

        tv_share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VideoUtils.shareUrl(videoInfoEntity.getOrigUrl());
            }
        });

        tv_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!checkUrlValidate())return;
                VideoPlayerActivity.startActivity(VideoDetailInfoActivity.this, videoInfoEntity.getOrigUrl());
            }
        });

        tv_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteConfirm();
            }
        });

        populateData();
    }

    /**
     * 播放器格式过滤
     * @return
     */
    private boolean checkUrlValidate() {
        String url = videoInfoEntity.getOrigUrl();
        if(url.endsWith(".mp4")|| url.endsWith(".flv") || url.endsWith(".m3u8")){
            return true;
        }
        showToast("播放器不支持该格式播放");
        return false;
    }

    private void populateData() {
        Glide.with(this).load(videoInfoEntity.getSnapshotUrl()).placeholder(R.drawable.video_thumb).into(iv_thumb);
        tv_video_title.setText(videoInfoEntity.getVideoName());
        listView.setAdapter(new TranscodeVideoListAdapter(this, R.layout.item_layout_video_info, transcodeEntityList));

        tv_source_formate.setText(VideoUtils.getVideoFormate(videoInfoEntity.getOrigUrl()));
        tv_source_url.setText(videoInfoEntity.getOrigUrl());
        tv_source_size.setText(VideoUtils.getFormateSize(videoInfoEntity.getInitialSize()));
        scrollView.smoothScrollTo(0, 0);
    }

    private void showDeleteConfirm() {
        EasyAlertDialogHelper.OnDialogActionListener listener = new EasyAlertDialogHelper.OnDialogActionListener() {

            @Override
            public void doCancelAction() {
            }

            @Override
            public void doOkAction() {
                showNetworkDialog();
            }
        };

        final EasyAlertDialog dialog = EasyAlertDialogHelper.createOkCancelDiolag(VideoDetailInfoActivity.this, null,
                getString(R.string.delete_confirm_tip), false, listener);
        dialog.show();
    }

    private void showNetworkDialog() {
        if (NetworkUtil.isNetAvailable(this)) {
            Intent intent = new Intent();
            intent.putExtra(EXTRA_VIDEO_ENTITY, videoInfoEntity);
            setResult(Activity.RESULT_OK, intent);
            finish();
        } else {
            EasyAlertDialogHelper.showOneButtonDiolag(this, null, getString(R.string.network_is_not_available),
                    getString(R.string.i_know), false, null);
        }
    }
}
