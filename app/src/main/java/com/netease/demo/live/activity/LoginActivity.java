package com.netease.demo.live.activity;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.netease.demo.live.DemoCache;
import com.netease.demo.live.R;
import com.netease.demo.live.nim.config.perference.Preferences;
import com.netease.demo.live.server.DemoServerHttpClient;
import com.netease.demo.live.util.ScreenUtil;
import com.netease.demo.live.util.file.AssetCopyer;
import com.netease.nim.uikit.common.activity.UI;
import com.netease.nim.uikit.common.ui.dialog.DialogMaker;
import com.netease.nim.uikit.common.ui.dialog.EasyAlertDialogHelper;
import com.netease.nim.uikit.common.ui.widget.ClearableEditTextWithIcon;
import com.netease.nim.uikit.common.util.string.MD5;
import com.netease.nim.uikit.common.util.sys.NetworkUtil;
import com.netease.nim.uikit.permission.MPermission;
import com.netease.nim.uikit.permission.annotation.OnMPermissionDenied;
import com.netease.nim.uikit.permission.annotation.OnMPermissionGranted;
import com.netease.nimlib.sdk.AbortableFuture;
import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.RequestCallback;
import com.netease.nimlib.sdk.auth.AuthService;
import com.netease.nimlib.sdk.auth.ClientType;
import com.netease.nimlib.sdk.auth.LoginInfo;

import java.io.IOException;

/**
 * 登录/注册界面
 */
public class LoginActivity extends UI implements OnKeyListener {

    private static final String TAG = LoginActivity.class.getSimpleName();
    private static final String KICK_OUT = "KICK_OUT";
    private final int BASIC_PERMISSION_REQUEST_CODE = 110;

    private TextView loginRegisterBtn;  // 注册/登录 完成按钮
    private TextView switchModeBtn;  // 注册/登录切换按钮

    private ImageView switchRegister;//验证码和账号登录切换按钮

    private ImageView phoneLogin;//手机验证码登录

    private ClearableEditTextWithIcon loginAccountEdit;
    private ClearableEditTextWithIcon loginPasswordEdit;

    private ClearableEditTextWithIcon registerAccountEdit;
    private ClearableEditTextWithIcon registerNickNameEdit;
    private ClearableEditTextWithIcon registerPasswordEdit;
    private ClearableEditTextWithIcon registerPhoneNumberEdit;
    private ClearableEditTextWithIcon registerVerifyNumberEdit;

    private ClearableEditTextWithIcon loginPhonenumber;
    private ClearableEditTextWithIcon loginVerifynumber;
    private TextView loginCode;
    private TextView phoneVerifyLogin;

    private TextView registerGetCode; // 注册界面，获取验证码

    private ViewGroup loginRelativeLayout;

    private View loginLayout;
    private View registerLayout;
    private View loginPhoneLayout;

    private LinearLayout rootView;
    private View iv_logo;
    private TextView tv_title;

    private CheckBox rememberPwdCheck; // 记住账号和密码

    // data
    private float screenHeight;
    private AbortableFuture<LoginInfo> loginRequest;
    private boolean registerMode = false; // 注册模式
    private boolean loginMode = false;// 验证码模式
    private boolean loginPanelInited = false;//登录面板是否初始化
    private boolean registerPanelInited = false; // 注册面板是否初始化
    private int registerCountDownTime = 60;
    private int loginCountDownTime = 60;

    public static void start(Context context) {
        start(context, false);
    }

    public static void start(Context context, boolean kickOut) {
        Intent intent = new Intent(context, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(KICK_OUT, kickOut);
        context.startActivity(intent);
    }

    @Override
    protected boolean displayHomeAsUpEnabled() {
        return false;
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);
        requestBasicPermission();
        onParseIntent();
        initTitleView();
        initLoginRegisterBtn();
        setupLoginPanel();
        setupRegisterPanel();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getHandler().removeCallbacksAndMessages(null);
    }

    /**
     * 初始化顶部布局
     */
    private void initTitleView() {
        rootView = findView(R.id.login_root);
        tv_title = findView(R.id.tv_title);
        iv_logo = findView(R.id.iv_logo);

        screenHeight = ScreenUtil.getDisplayHeight();


        rootView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (bottom < oldBottom && (bottom - top) < (screenHeight * 2 / 3)) {
                    //键盘展开
                    iv_logo.setVisibility(View.GONE);
                    tv_title.setVisibility(View.VISIBLE);
                } else if (bottom > oldBottom && (bottom - top) > (screenHeight * 2 / 3)) {
                    //键盘缩起
                    iv_logo.setVisibility(View.VISIBLE);
                    tv_title.setVisibility(View.GONE);
                }
            }
        });


    }

    /**
     * 基本权限管理
     */
    private void requestBasicPermission() {
        MPermission.with(LoginActivity.this)
                .setRequestCode(BASIC_PERMISSION_REQUEST_CODE)
                .permissions(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                .request();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        MPermission.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    private boolean bWritePermission;

    @OnMPermissionGranted(BASIC_PERMISSION_REQUEST_CODE)
    public void onBasicPermissionSuccess() {
        //Toast.makeText(this, "授权成功", Toast.LENGTH_SHORT).show();
        bWritePermission = true;
    }

    @OnMPermissionDenied(BASIC_PERMISSION_REQUEST_CODE)
    public void onBasicPermissionFailed() {
        //Toast.makeText(this, "授权失败", Toast.LENGTH_SHORT).show();
        bWritePermission = false;
    }

    private void onParseIntent() {
        if (getIntent().getBooleanExtra(KICK_OUT, false)) {
            int type = NIMClient.getService(AuthService.class).getKickedClientType();
            String client;
            switch (type) {
                case ClientType.Web:
                    client = "网页端";
                    break;
                case ClientType.Windows:
                    client = "电脑端";
                    break;
                case ClientType.REST:
                    client = "服务端";
                    break;
                default:
                    client = "移动端";
                    break;
            }
            EasyAlertDialogHelper.showOneButtonDiolag(LoginActivity.this, getString(R.string.kickout_notify),
                    String.format(getString(R.string.kickout_content), client), getString(R.string.ok), true, null);
        }
    }

    /**
     * 初始化按钮
     */
    private void initLoginRegisterBtn() {
        loginRegisterBtn = findView(R.id.btn_login_register);
        loginRegisterBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (registerMode) {
                    register();
                } else {
                    //fakeLoginTest(); // 假登录代码示例
                    if (!checkWritePermission()) {
                        return;
                    }
                    login();
                }
            }
        });

        registerGetCode = findView(R.id.get_code);
        registerGetCode.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!NetworkUtil.isNetAvailable(LoginActivity.this)) {
                    Toast.makeText(LoginActivity.this, R.string.net_broken, Toast.LENGTH_SHORT).show();
                    return;
                }
                String phone = registerPhoneNumberEdit.getText().toString();
                if (checkPhoneValidate(phone)) {
                    DemoServerHttpClient.getInstance().fetchRegisterVerifyCode(phone, new DemoServerHttpClient.DemoServerHttpCallback<String>() {
                        @Override
                        public void onSuccess(String s) {
                            Toast.makeText(LoginActivity.this, "获取手机验证码成功", Toast.LENGTH_SHORT).show();
                            getHandler().post(registerCountDownRunnable);
                        }

                        @Override
                        public void onFailed(int code, String errorMsg) {
                            Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    /**
     * 检查手机号码
     * @param phone
     * @return
     */
    private boolean checkPhoneValidate(String phone) {
        if (TextUtils.isEmpty(phone)) {
            Toast.makeText(LoginActivity.this, "请输入手机号码", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (phone.length() != 11) {
            Toast.makeText(LoginActivity.this, "手机号码格式错误", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }


    private Runnable registerCountDownRunnable = new Runnable() {
        @Override
        public void run() {
            registerGetCode.setEnabled(false);
            registerCountDownTime--;
            registerGetCode.setText(registerCountDownTime + "s");
            if (registerCountDownTime > 0) {
                getHandler().postDelayed(registerCountDownRunnable, 1000);
            } else {
                registerCountDownTime = 60;
                registerGetCode.setEnabled(true);
                registerGetCode.setText("重新获取");
            }
        }
    };

    private Runnable loginCountDownRunnable = new Runnable() {
        @Override
        public void run() {
            loginCode.setEnabled(false);
            loginCountDownTime--;
            loginCode.setText(loginCountDownTime + "s");
            if (loginCountDownTime > 0) {
                getHandler().postDelayed(loginCountDownRunnable, 1000);
            } else {
                loginCountDownTime = 60;
                loginCode.setEnabled(true);
                loginCode.setText("重新获取");
            }
        }
    };

    /**
     * 登录面板
     */
    private void setupLoginPanel() {
        loginAccountEdit = findView(R.id.edit_login_account);
        loginPasswordEdit = findView(R.id.edit_login_password);
        rememberPwdCheck = findView(R.id.remember_pwd);
        rememberPwdCheck.setChecked(Preferences.getRememberAccountToken());

        loginAccountEdit.setIconResource(R.drawable.user_account_icon);
        loginPasswordEdit.setIconResource(R.drawable.user_pwd_lock_icon);

        loginAccountEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(32)});
        loginPasswordEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(32)});
        loginAccountEdit.addTextChangedListener(textWatcher);
        loginPasswordEdit.addTextChangedListener(textWatcher);
        loginPasswordEdit.setOnKeyListener(this);

        if (rememberPwdCheck.isChecked()) {
            String account = Preferences.getUserAccount();
            loginAccountEdit.setText(account);
            String token = Preferences.getUserToken();
            loginPasswordEdit.setText(token);
        }

        switchRegister = findView(R.id.phone_login);

        loginRelativeLayout = findView(R.id.login_relative_layout);

        switchRegister.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                switchLogin();
            }
        });
    }

    /**
     * 注册面板
     */
    private void setupRegisterPanel() {
        loginLayout = findView(R.id.login_layout);
        registerLayout = findView(R.id.register_layout);
        switchModeBtn = findView(R.id.register_login_tip);

        loginPhoneLayout = findView(R.id.login_phone_layout);

        switchModeBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                switchMode();
            }
        });


    }

    private TextWatcher textWatcher = new TextWatcher() {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            // 更新右上角按钮状态
            if (!registerMode) {
                if (loginRegisterBtn.getText().equals("登录")) {
                    // 账号登录模式
                    boolean isEnable = loginAccountEdit.getText().length() > 0
                            && loginPasswordEdit.getText().length() > 0;
                    loginRegisterBtn.setEnabled(isEnable);
                } else {
                    boolean isEnable = loginPhonenumber.getText().length() > 0
                            && phoneVerifyLogin.getText().length() > 0;
                    loginRegisterBtn.setEnabled(isEnable);
                }
            }
        }
    };

    /**
     * ***************************************** 登录 **************************************
     */

    /**
     * 登录应用服务器
     */
    private void login() {
        if (!NetworkUtil.isNetAvailable(this)) {
            Toast.makeText(this, R.string.net_broken, Toast.LENGTH_SHORT).show();
            return;
        }
        DialogMaker.showProgressDialog(this, null, getString(R.string.logining), true, new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (loginRequest != null) {
                    loginRequest.abort();
                    onLoginDone();
                }
            }
        }).setCanceledOnTouchOutside(false);

        if (loginRegisterBtn.getText().toString().equals("登录")) {
            // 账号密码登录
            final String account = loginAccountEdit.getEditableText().toString().toLowerCase();
            final String token = loginPasswordEdit.getEditableText().toString();

            DemoServerHttpClient.getInstance().login(account, token, new DemoServerHttpClient.DemoServerHttpCallback<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    DemoCache.setAccount(account);
                    saveLoginInfo(account, token);

                    loginNim(account, MD5.getStringMD5(token));
                }

                @Override
                public void onFailed(int code, String errorMsg) {
                    onLoginDone();
                    Toast.makeText(LoginActivity.this, "登录失败: " + errorMsg, Toast.LENGTH_SHORT).show();
                }
            });
        } else  {
            // 手机验证码登录
            final String phone = loginPhonenumber.getText().toString();
            final String verify = loginVerifynumber.getText().toString();

            DemoServerHttpClient.getInstance().phoneLogin(phone, verify, new DemoServerHttpClient.DemoServerHttpCallback<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    loginNim(DemoCache.getAccount(), DemoCache.getToken());
                }

                @Override
                public void onFailed(int code, String errorMsg) {
                    onLoginDone();

                    Toast.makeText(LoginActivity.this, "登录失败: " + errorMsg, Toast.LENGTH_SHORT).show();
                }
            });
        }


    }

    private void loginNim(final String account, final String token) {
        NIMClient.getService(AuthService.class).login(new LoginInfo(account, token)).setCallback(new RequestCallback() {
            @Override
            public void onSuccess(Object o) {
                onLoginDone();

                onLoginDoneInit(account, token);
            }

            @Override
            public void onFailed(int i) {
                onLoginDone();

                Toast.makeText(LoginActivity.this, "登录失败: " + i, Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onException(Throwable throwable) {
                onLoginDone();
            }
        });
    }

    private void onLoginDoneInit(String account, String token) {
        initAsset();

        // 进入主界面
        MainActivity.start(LoginActivity.this);
        finish();
    }

    private boolean checkWritePermission() {
        if (bWritePermission) {
            return true;
        } else {
            requestBasicPermission();
            Toast.makeText(LoginActivity.this, "授权读写存储卡权限后,才能正常使用", Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private void onLoginDone() {
        loginRequest = null;
        DialogMaker.dismissProgressDialog();
    }

    private void initAsset() {
        AssetCopyer assetCopyer = new AssetCopyer(this);
        try {
            assetCopyer.copy();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveLoginInfo(final String account, final String token) {
        Preferences.saveUserAccount(account);
        Preferences.saveUserToken(token);
        Preferences.saveVodToken(DemoCache.getVodtoken());
        Preferences.saveRememberAccountToken(rememberPwdCheck.isChecked());
        Preferences.saveLoginState(true);
    }

    private String readAppKey() {
        try {
            ApplicationInfo appInfo = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            if (appInfo != null) {
                return appInfo.metaData.getString("com.netease.nim.appKey");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * ***************************************** 注册 **************************************
     */

    private void register() {
        if (!registerMode || !registerPanelInited) {
            return;
        }

        if (!checkRegisterContentValid()) {
            return;
        }

        if (!NetworkUtil.isNetAvailable(LoginActivity.this)) {
            Toast.makeText(LoginActivity.this, R.string.network_is_not_available, Toast.LENGTH_SHORT).show();
            return;
        }

        DialogMaker.showProgressDialog(this, getString(R.string.registering), false);

        // 注册流程
        final String account = registerAccountEdit.getText().toString();
        final String nickName = registerNickNameEdit.getText().toString();
        final String password = registerPasswordEdit.getText().toString();
        final String phone = registerPhoneNumberEdit.getText().toString();
        final String verifyCode = registerVerifyNumberEdit.getText().toString();

        DemoServerHttpClient.getInstance().register(account, nickName, password, phone, verifyCode, new DemoServerHttpClient.DemoServerHttpCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(LoginActivity.this, R.string.register_success, Toast.LENGTH_SHORT).show();
                switchMode();  // 切换回登录
                loginAccountEdit.setText(account);
                loginPasswordEdit.setText(password);

                registerAccountEdit.setText("");
                registerNickNameEdit.setText("");
                registerPasswordEdit.setText("");

                DialogMaker.dismissProgressDialog();
            }

            @Override
            public void onFailed(int code, String errorMsg) {
                Toast.makeText(LoginActivity.this, getString(R.string.register_failed, String.valueOf(code), errorMsg), Toast.LENGTH_SHORT)
                        .show();

                DialogMaker.dismissProgressDialog();
            }
        });
    }

    private boolean checkRegisterContentValid() {
        if (!registerMode || !registerPanelInited) {
            return false;
        }

        // 帐号检查
        String account = registerAccountEdit.getText().toString().trim();
        if (account.length() <= 0 || account.length() > 20) {
            Toast.makeText(this, R.string.register_account_tip, Toast.LENGTH_SHORT).show();

            return false;
        }

        // 昵称检查
        String nick = registerNickNameEdit.getText().toString().trim();
        if (!nick.matches("^[\\u4E00-\\u9FA5A-Za-z0-9]{1,10}$")) {
            Toast.makeText(this, R.string.register_nick_name_tip, Toast.LENGTH_SHORT).show();
            return false;
        }

        // 密码检查
        String password = registerPasswordEdit.getText().toString().trim();
        if (password.length() < 6 || password.length() > 20) {
            Toast.makeText(this, R.string.register_password_tip, Toast.LENGTH_SHORT).show();

            return false;
        }

        return true;
    }

    /**
     * ***************************************** 验证码登录以及账号登录切换 **************************************
     */

    private void switchLogin() {

        loginMode = !loginMode;

        if (!loginPanelInited) {
            loginPhonenumber = findView(R.id.login_phonenumber);
            loginCode = findView(R.id.login_code);
            loginVerifynumber = findView(R.id.login_verifynumber);
            phoneVerifyLogin = findView(R.id.phone_verify_login);
            phoneLogin = findView(R.id.phone_login);

            loginPhonenumber.setIconResource(R.drawable.phone_number);
            loginVerifynumber.setIconResource(R.drawable.verify_number);

            loginPhonenumber.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});
            loginVerifynumber.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});

            loginPhonenumber.addTextChangedListener(textWatcher);
            loginVerifynumber.addTextChangedListener(textWatcher);

            loginPanelInited = true;

            loginCode.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    String phone = loginPhonenumber.getText().toString();
                    if(checkPhoneValidate(phone)) {
                        DemoServerHttpClient.getInstance().fetchLoginVerifyCode(phone, new DemoServerHttpClient.DemoServerHttpCallback<String>() {
                            @Override
                            public void onSuccess(String s) {
                                getHandler().post(loginCountDownRunnable);
                                Toast.makeText(LoginActivity.this, "获取手机登录验证码成功", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onFailed(int code, String errorMsg) {
                                if (code == 908) {
                                    showRegisterTip();
                                }
                                Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            });
        }

        loginLayout.setVisibility(loginMode ? View.GONE : View.VISIBLE);
        loginPhoneLayout.setVisibility(loginMode ? View.VISIBLE : View.GONE);
        loginRegisterBtn.setText(loginMode ? "验证并登录" : "登录");
        phoneVerifyLogin.setText(loginMode ? "账号登录" : "手机验证码登录");
        phoneLogin.setBackgroundResource(loginMode ? R.drawable.account_login : R.drawable.phone_login);

    }

    private void showRegisterTip() {
        EasyAlertDialogHelper.createOkCancelDiolag(this, null, getString(R.string.phone_is_unregister),
                getString(R.string.quick_register), getString(R.string.cancel), true, new EasyAlertDialogHelper.OnDialogActionListener() {
                    @Override
                    public void doCancelAction() {

                    }

                    @Override
                    public void doOkAction() {
                        switchMode();
                    }
                }).show();
    }

    /**
     * ***************************************** 注册/登录切换 **************************************
     */
    private void switchMode() {
        registerMode = !registerMode;

        if (registerMode && !registerPanelInited) {
            registerAccountEdit = findView(R.id.edit_register_account);
            registerNickNameEdit = findView(R.id.edit_register_nickname);
            registerPasswordEdit = findView(R.id.edit_register_password);
            registerPhoneNumberEdit = findView(R.id.edit_register_phonenumber);
            registerVerifyNumberEdit = findView(R.id.edit_register_verifynumber);

            registerAccountEdit.setIconResource(R.drawable.user_account_icon);
            registerNickNameEdit.setIconResource(R.drawable.nick_name_icon);
            registerPasswordEdit.setIconResource(R.drawable.user_pwd_lock_icon);
            registerPhoneNumberEdit.setIconResource(R.drawable.phone_number);
            registerVerifyNumberEdit.setIconResource(R.drawable.verify_number);

            registerAccountEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});
            registerNickNameEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10)});
            registerPasswordEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});
            registerPhoneNumberEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});
            registerVerifyNumberEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});

            registerAccountEdit.addTextChangedListener(textWatcher);
            registerNickNameEdit.addTextChangedListener(textWatcher);
            registerPasswordEdit.addTextChangedListener(textWatcher);
            registerPhoneNumberEdit.addTextChangedListener(textWatcher);
            registerVerifyNumberEdit.addTextChangedListener(textWatcher);

            registerPanelInited = true;
        }

        setTitle(registerMode ? R.string.register : R.string.login);
        loginLayout.setVisibility(registerMode ? View.GONE : View.VISIBLE);
        loginPhoneLayout.setVisibility(View.GONE);
        registerLayout.setVisibility(registerMode ? View.VISIBLE : View.GONE);
        loginRegisterBtn.setText(registerMode ? "注册" : "登录");
        switchModeBtn.setText(registerMode ? R.string.login_has_account : R.string.register_quickly);
        loginRelativeLayout.setVisibility(registerMode ? View.GONE : View.VISIBLE);
        if (registerMode) {
            loginRegisterBtn.setEnabled(true);
        } else {
            boolean isEnable = loginAccountEdit.getText().length() > 0
                    && loginPasswordEdit.getText().length() > 0;
            loginRegisterBtn.setEnabled(isEnable);
            loginMode = true;
            switchLogin();
        }
    }
}
