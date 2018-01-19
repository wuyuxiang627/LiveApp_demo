package com.netease.demo.live.activity;

import android.Manifest;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.daimajia.numberprogressbar.NumberProgressBar;
import com.netease.LSMediaCapture.lsMessageHandler;
import com.netease.demo.live.R;
import com.netease.demo.live.shortvideo.MediaCaptureController;
import com.netease.demo.live.shortvideo.model.MediaCaptureOptions;
import com.netease.demo.live.shortvideo.model.ResolutionType;
import com.netease.demo.live.shortvideo.model.VideoCaptureParams;
import com.netease.demo.live.shortvideo.videoprocess.VideoProcessController;
import com.netease.demo.live.upload.model.VideoItem;
import com.netease.fulive.EffectAndFilterSelectAdapter;
import com.netease.fulive.FuVideoEffect;
import com.netease.nim.uikit.common.activity.UI;
import com.netease.nim.uikit.common.util.storage.StorageType;
import com.netease.nim.uikit.common.util.storage.StorageUtil;
import com.netease.nim.uikit.permission.MPermission;
import com.netease.nim.uikit.permission.annotation.OnMPermissionGranted;
import com.netease.transcoding.record.VideoCallback;
import com.netease.vcloud.video.render.NeteaseView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by connxun-16 on 2018/1/19.
 * https://github.com/Faceunity/FULiveDemoDroid
 */

public class TestActivity extends UI implements MediaCaptureController.MediaCaptureControllerCallback,
        View.OnClickListener, VideoProcessController.VideoProcessCallback {

    private NeteaseView videoView; //控件
    private Button btnStartStop; //开始或者停止
    private NumberProgressBar numberProgressBar; //进度

    //顶部布局
    private ImageView faceuBtn; //第三方美颜
    private ImageView switchBtn; //翻转摄像头
    //faceU 布局
    private RecyclerView effectRecyclerView; // 道具

    //第三方滤镜
    private FuVideoEffect mFuEffect; //FU的滤镜

    private MediaCaptureController mediaCaptureController; // 录制视频控制器
    private MediaCaptureOptions mediaCaptureOptions; // 视频录制参数配置
    private VideoCaptureParams videoCaptureParams; // 录制视频的参数（界面显示，用户操作配置的），分几段，时间等
    private List<String> videoPathList = new ArrayList<>(); // 录制的分段视频地址
    private String outputPath; // 拼接后的视频地址
    private VideoItem videoItem; // 拼接后的video
    private String displayName; // 视频名称

    private final int BASIC_PERMISSION_REQUEST_CODE = 100;

    /**
     * 代码流程
     * 1.检查权限
     * 2.获得权限-初始化界面
     * 3.显示预览界面
     * 4.美颜参数初始化
     * 5.设置美颜参数
     * 6.设置控件监听
     *
     * 4. 拍摄  暂停
     * 4.1 拍摄
     *
     * @param savedInstanceState
     */


    /**
     * 生命周期
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        //检查权限
        requestBasicPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    /**
     *开始
     */

    /**
     * 基本权限管理
     */
    private final String[] BASIC_PERMISSIONS = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.RECORD_AUDIO
    };

    /**
     * 检查权限
     */
    private void requestBasicPermission() {
        MPermission.printMPermissionResult(true, this, BASIC_PERMISSIONS);
        MPermission.with(TestActivity.this)
                .setRequestCode(BASIC_PERMISSION_REQUEST_CODE)
                .permissions(BASIC_PERMISSIONS)
                .request();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        MPermission.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    /**
     * 授权成功
     */
    @OnMPermissionGranted(BASIC_PERMISSION_REQUEST_CODE)
    public void onBasicPermissionSuccess() {
        Toast.makeText(this, "授权成功", Toast.LENGTH_SHORT).show();
        MPermission.printMPermissionResult(false, this, BASIC_PERMISSIONS);

        //初始化界面
        initView();
        //设置监听
        setListener();
        //视频参数
        initVideoParams();
        //视频录制参数配置
        initMediaCapture();


    }

    /**
     * 视频录制参数配置
     */
    private void initMediaCapture() {
        //视频录制参数
        mediaCaptureOptions = new MediaCaptureOptions();
        //创建视频文件
        initCaptureOptions();
        //短视频录制控制器
        mediaCaptureController = new MediaCaptureController(this, this, mediaCaptureOptions);
        // faceU要在startPreview之前初始化
//        fuLiveEffect();
        //创建本地视频预览模板
        mediaCaptureController.startPreview(videoView);
    }

    //FU的滤镜
    private void fuLiveEffect(){
        mediaCaptureController.getMediaRecord().setCaptureRawDataCB(new VideoCallback() {
            @Override
            public int onVideoCapture(byte[] data, int width, int height,int orientation) {
                //SDK回调的线程已经创建了GLContext
                if(mFuEffect == null){
                    mFuEffect = new FuVideoEffect();
                    mFuEffect.filterInit(TestActivity.this);
                }
                int result = mFuEffect.ifilterNV21Image(data, width, height);
                return result;
            }
        });
    }


    //创建视频文件-视频文件的尺寸-清晰度设置
    private void initCaptureOptions() {
        //创建视频文件
        mediaCaptureOptions.mFilePath = StorageUtil.getWritePath(System.currentTimeMillis() + ".mp4", StorageType.TYPE_VIDEO);
        videoPathList.add(mediaCaptureOptions.mFilePath);
        //视频文件的尺寸-清晰度设置
        mediaCaptureOptions.mVideoPreviewWidth = 720;
        mediaCaptureOptions.mVideoPreviewHeight = 1280;
        mediaCaptureOptions.resolutionType = ResolutionType.HD;

    }


    /**
     * 视频参数
     */
    private void initVideoParams() {
        videoCaptureParams = new VideoCaptureParams(3, 30*1000, ResolutionType.HD);
        videoCaptureParams.setResolutionType( ResolutionType.HD); 
    }

    /**
     * 设置监听器
     */
    private void setListener() {
        btnStartStop.setOnClickListener(this);
        faceuBtn.setOnClickListener(this);
        switchBtn.setOnClickListener(this);
    }

    /**
     *控件初始化
     */
    private void initView() {
        videoView = findView(R.id.camerasurfaceview); //显示控件
        btnStartStop = findView(R.id.btn_start_stop); //开始/暂停
        numberProgressBar = findView(R.id.number_progress_bar); //进度条
        faceuBtn = findView(R.id.test_faceu_btn); //第三方美颜
        switchBtn = findView(R.id.test_switch_btn);//翻转摄像头

        //美颜布局
        initFaceULayout();

    }

    /**
     * 第三方美颜
     */
    private void initFaceULayout() {
        effectRecyclerView = findView(R.id.effect_recycle_view);
        effectRecyclerView.setVisibility(View.VISIBLE);
        effectRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        EffectAndFilterSelectAdapter effectAndFilterSelectAdapter = new EffectAndFilterSelectAdapter(effectRecyclerView,
                EffectAndFilterSelectAdapter.VIEW_TYPE_EFFECT);
        effectAndFilterSelectAdapter.setOnItemSelectedListener(new EffectAndFilterSelectAdapter.OnItemSelectedListener() {
            @Override
            public void onItemSelected(int itemPosition) {
                if (mFuEffect != null) {
                    mFuEffect.onEffectItemSelected(itemPosition);
                }
            }
        });
        effectRecyclerView.setAdapter(effectAndFilterSelectAdapter);
        showOrHideFaceULayout(false);
    }

    /**
     * 美颜布局数据集合
     * @param show
     */
    private void showOrHideFaceULayout(boolean show) {
        ViewGroup vp = findView(R.id.faceu_layout);
        for (int i = 0; i < vp.getChildCount(); i++) {
            vp.getChildAt(i).setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }


    /**
     * 点击事件
     * @param view
     */
    @Override
    public void onClick(View view) {
        switch (view.getId())
        {
            case R.id.btn_start_stop:
                if(btnStartStop.getText().equals("开始")){
                    startRecording();
                    btnStartStop.setText("暂停");
                }else if(btnStartStop.getText().equals("暂停")){
                    stopRecordding();
                    btnStartStop.setText("继续");
                }else{

                }
                break;
            case R.id.test_faceu_btn:
                // faceu美颜布局显示
                showOrHideFaceULayout(true);
                break;
        }

    }

    /**
     * 开始录制
     */
    private void startRecording() {
        mediaCaptureController.startRecording();
    }

    /**
     * 停止录制
     */
    private void stopRecordding(){
        mediaCaptureController.stopRecording();
    }


    /**
     * 释放资源
     */
    private void doneRecording() {
        if (mediaCaptureController != null) {
            // 顺序不能错
            mediaCaptureController.stopRecording();
            mediaCaptureController.stopPreview();
            //释放美颜资源
            releaseFuEffect();

            mediaCaptureController.release();
        }
    }

    /**
     * 释放第三方美颜资源
     */
    private void releaseFuEffect(){
        if(mFuEffect != null){
            mediaCaptureController.getMediaRecord().postOnGLThread(new Runnable() {
                @Override
                public void run() {
                    mFuEffect.filterUnInit();
                    mFuEffect = null;
                }
            });
        }
    }



    /**
     * 预览设置完成
     */
    @Override
    public void onPreviewInited() {

    }

    /**
     * 设置预览画面大小
     */
    @Override
    public void setPreviewSize(int videoPreviewWidth, int videoPreviewHeight) {

    }

    /**
     * 获取画布SurfaceView
     */
    @Override
    public SurfaceView getSurfaceView() {
        return videoView;
    }

    /**
     * 可以开始录制了
     */
    @Override
    public void onStartRecording() {

    }

    /**
     * 资源释放完毕
     */
    @Override
    public void onRelease() {

    }

    /**
     * 异常
     */
    @Override
    public void onError(int code) {
        if (code == lsMessageHandler.MSG_AUDIO_RECORD_ERROR) {
            Toast.makeText(this, "录音模块异常", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "录制异常:" + code, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 视频处理
     */
    /**
     * 转码已完成
     */
    @Override
    public void onVideoProcessSuccess() {

    }

    @Override
    public void onVideoProcessFailed(int code) {

    }

    @Override
    public void onVideoSnapshotSuccess(Bitmap bitmap) {

    }

    @Override
    public void onVideoSnapshotFailed(int code) {

    }

    @Override
    public void onVideoProcessUpdate(int process, int total) {

    }
}
