package cn.edu.zstu.login.activty;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import cn.edu.zstu.login.R;
import cn.edu.zstu.login.utils.URL;

public class LoginActivity extends Activity implements OnClickListener {

    private final static String TAG = "LoginActivity";
    private String mLoginURL;

    private static final int LOGIN_SUCCESS = 1;
    private static final int LOGIN_FAIL = 2;
    private static final int NAME_OR_PSW_ERROR = 3;
    private static final int TIME_OUT = 4;

    private EditText mLoginId;
    private EditText mLoginPassword;
    private Button mLoginBtn;
    private Button mLoginMissps;
    private Button mLoginNewUser;
    private Button mLoginChangePw;
    private String mIsId, mIsPs;
    private ProgressDialog mProDialog;
    private HttpClient mHttpClient;

    String userName = "";
    String passWord = "";
    String uuid = "";
    String token = "";

    private Button mSetUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_login);

        initView();

        Intent idIntent = super.getIntent();
        if (idIntent.hasExtra("myId")) {
            String Id = idIntent.getStringExtra("myId");
            mLoginId.setText(Id);
        } else {
            SharedPreferences preferences = getSharedPreferences("userInfo", Activity.MODE_PRIVATE);
            String account = preferences.getString("name", "");
            mLoginId.setText(account);
        }

        mProDialog = new ProgressDialog(this);
        mProDialog.setCancelable(true);
        mProDialog.setCanceledOnTouchOutside(false);
        mProDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                mHttpClient.getConnectionManager().shutdown();
            }
        });
    }

    // 控件的初始化
    private void initView() {
        mLoginId = (EditText) findViewById(R.id.loginId);
        mLoginPassword = (EditText) findViewById(R.id.loginPassword);
        mLoginBtn = (Button) findViewById(R.id.loginBtn);
        mLoginBtn.setOnClickListener(this);
        mLoginMissps = (Button) findViewById(R.id.loginMissps);
        mLoginMissps.setOnClickListener(this);
        mLoginNewUser = (Button) findViewById(R.id.loginNewUser);
        mLoginNewUser.setOnClickListener(this);
        mLoginChangePw = (Button) findViewById(R.id.loginChangePw);
        mLoginChangePw.setOnClickListener(this);

        mSetUrl = (Button) findViewById(R.id.loginBack);
        mSetUrl.setOnClickListener(this);
    }

    // 控件的点击事件
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.loginBtn:
                mIsId = mLoginId.getText().toString();
                mIsPs = mLoginPassword.getText().toString();
                if ((mIsId.equals("")) || (mIsPs.equals(""))) {
                    // Toast弹窗
                    Toast.makeText(LoginActivity.this, R.string.full_username_and_psw,
                            Toast.LENGTH_SHORT).show();
                } else {
                    mProDialog.setMessage(getString(R.string.login_ing));
                    mProDialog.show();

                    Log.i(TAG, "== call login() ==");
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            login();
                        }
                    }).start();
                }
                break;
            case R.id.loginMissps:
                Intent a = new Intent(LoginActivity.this,
                        FindPasswordActivity.class);
                startActivity(a);
                break;
            case R.id.loginNewUser:
                Intent i = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(i);
                break;
            case R.id.loginChangePw:
                Intent l = new Intent(LoginActivity.this, ChangePasswordActivity.class);
                startActivity(l);
                break;

            case R.id.loginBack:
                Intent s = new Intent(LoginActivity.this, SetURL.class);
                startActivity(s);
                break;

            default:
                break;
        }
    }

    /**
     * 处理POST请求的返回结果
     */
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case LOGIN_SUCCESS:
                    if (null != mProDialog) {
                        mProDialog.dismiss();
                    }
                    new AlertDialog.Builder(LoginActivity.this)
                            .setTitle(getString(R.string.hint))
                            .setMessage(R.string.login_success)
                            .setPositiveButton(getString(R.string.isture),
                                    new DialogInterface.OnClickListener() {

                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent i = new Intent(LoginActivity.this, MidActivity.class);
                                            LoginActivity.this.finish();
                                            startActivity(i);
                                        }
                                    }).show();

                    break;
                case LOGIN_FAIL:
                    if (null != mProDialog) {
                        mProDialog.dismiss();
                    }
                    Toast.makeText(LoginActivity.this, R.string.login_fail,
                            Toast.LENGTH_SHORT).show();
                    break;
                case NAME_OR_PSW_ERROR:
                    if (null != mProDialog) {
                        mProDialog.dismiss();
                    }
                    Toast.makeText(LoginActivity.this, R.string.user_psw_erro,
                            Toast.LENGTH_SHORT).show();
                    break;
                case TIME_OUT:
                    if (null != mProDialog) {
                        mProDialog.dismiss();
                    }
                    Toast.makeText(LoginActivity.this, getString(R.string.time_out), Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }

        ;
    };


    public void login() {

        Log.i(TAG, "==Login POST request");
        mLoginURL = URL.getURL(this) + "/sessions";
        Log.d(TAG, "[login()]: " + mLoginURL);
        // 获取用户名和密码
        userName = mLoginId.getText().toString();
        passWord = mLoginPassword.getText().toString();

        NameValuePair userNamePair = new BasicNameValuePair("email", userName);
        NameValuePair passWordPair = new BasicNameValuePair("password", passWord);

        List<NameValuePair> pairList = new ArrayList<NameValuePair>();
        pairList.add(userNamePair);
        pairList.add(passWordPair);

        try {
            HttpEntity requestHttpEntity = new UrlEncodedFormEntity(
                    pairList);
            // URL使用基本URL即可，其中不需要加参数
            HttpPost httpPost = new HttpPost(mLoginURL);
            // 将请求体内容加入请求中
            httpPost.setEntity(requestHttpEntity);
            // 需要客户端对象来发送请求
            mHttpClient = new DefaultHttpClient();
            mHttpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 30 * 1000);
            // 发送请求
            HttpResponse response = mHttpClient.execute(httpPost);
            // 返回响应结果
            responseResult(response);
        } catch (ConnectTimeoutException e) {
            Log.e(TAG, getString(R.string.time_out));
            sendUiMessage(TIME_OUT);
        } catch (Exception e) {
            sendUiMessage(LOGIN_FAIL);
            e.printStackTrace();
        }
    }

    /**
     * 显示响应结果
     *
     * @param response
     */
    private void responseResult(HttpResponse response) {
        if (null == response) {
            return;
        }

        HttpEntity httpEntity = response.getEntity();
        try {
            InputStream inputStream = httpEntity.getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    inputStream));
            String result = "";
            String line = "";
            while (null != (line = reader.readLine())) {
                result += line;
            }

            Log.i(TAG, "==Response Content from server: " + result);

            JSONObject jsonObject = new JSONObject(result);
            boolean isSuccessful = jsonObject.has("device");

            if (isSuccessful) {

                //TODO:拿到uuid和token
                JSONObject deviceValue = new JSONObject(jsonObject.getString("device"));
                uuid = deviceValue.getString("uuid");
                token = deviceValue.getString("token");
                Log.i(TAG, "== " + uuid + '\n' + "== " + token);

                //登录成功，保存用户名和密码到本地
                saveUser(userName, passWord);
                //保存session
                saveSession(uuid, token);
                sendUiMessage(LOGIN_SUCCESS);
            } else sendUiMessage(LOGIN_FAIL);
            Log.i(TAG, "==" + isSuccessful);
        } catch (JSONException e) {
            sendUiMessage(NAME_OR_PSW_ERROR);
            Log.e(TAG, getString(R.string.JSON_exception));
            e.printStackTrace();
        } catch (IOException e) {
            sendUiMessage(LOGIN_FAIL);
            Log.i(TAG, getString(R.string.login_fail));
            e.printStackTrace();
        }
    }

    public void sendUiMessage(int l) {
        Message msg = new Message();
        msg.what = l;
        mHandler.sendMessage(msg);
    }

    public void saveUser(String id, String pwd) {

        SharedPreferences preferences = getSharedPreferences("userInfo", Context.MODE_PRIVATE);
        //取到编辑器
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("name", id);
        editor.putString("password", pwd);
        //把数据提交给文件中
        editor.apply();
    }

    public void saveSession(String uuid, String token) {

        SharedPreferences preferences = getSharedPreferences("session", Context.MODE_PRIVATE);
        //取到编辑器
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("uuid", uuid);
        editor.putString("token", token);
        //把数据提交给文件中
        editor.apply();
    }
}
