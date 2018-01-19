package com.netease.demo.live.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.netease.demo.live.R;
import com.netease.demo.live.activity.QRCodeScanActivity;
import com.netease.demo.live.activity.VideoPlayerActivity;
import com.netease.demo.live.base.BaseFragment;
import com.netease.demo.live.util.sys.AndTools;
import com.netease.nim.uikit.common.ui.widget.ClearableEditTextWithIcon;

/**
 * Created by zhukkun on 3/6/17.
 */
public class VideoEnterFragment extends BaseFragment {
    public static final int REQUEST_CODE = 1213;

    private ClearableEditTextWithIcon editText;
    private Button btn_enter;
    private ImageView iv_scan;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_video_enter, null);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        editText = findView(R.id.edit_room_address);
        btn_enter = findView(R.id.btn_enter_room);
        iv_scan = findView(R.id.iv_scan);

        editText.addTextChangedListener(textWatcher);
        editText.setDeleteImage(R.drawable.btn_close_enter_room);

        iv_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), QRCodeScanActivity.class);
                startActivityForResult(intent, REQUEST_CODE);
            }
        });

        btn_enter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //check
                if(checkUrlValidate()) {
                    VideoPlayerActivity.startActivity(getContext(), editText.getText().toString());
                }
            }
        });
    }

    /**
     * 用户可自定义过滤规则
     * @return
     */
    private boolean checkUrlValidate() {
        String url = editText.getText().toString();
        if(url.startsWith("http://flv")|| url.startsWith("http://pullhls") || url.startsWith("rtmp://")){
            showToast("点播地址错误");
            return false;
        }
        if(url.endsWith(".mp4")|| url.endsWith(".flv") || url.endsWith(".m3u8")){
            return true;
        }
        showToast("点播地址错误");
        return false;
    }

    @Override
    public void onPause() {
        super.onPause();
        AndTools.hideIME(getActivity());
    }


    TextWatcher textWatcher = new TextWatcher() {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            if(s.length()>0){
                btn_enter.setEnabled(true);
                iv_scan.setVisibility(View.INVISIBLE);
            }else{
                btn_enter.setEnabled(false);
                iv_scan.setVisibility(View.VISIBLE);
            }
        }
    };


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE) {
            if (data == null || data.getExtras() == null || TextUtils.isEmpty(data.getExtras().getString("result"))) {
                return;
            }

            String result = data.getExtras().getString("result");
            if (editText != null) {
                editText.setText(result);
            }
            iv_scan.setVisibility(View.INVISIBLE);
        }
    }
}
