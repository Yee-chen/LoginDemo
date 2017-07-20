package cn.edu.zstu.login.activty;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.edu.zstu.login.R;
import cn.edu.zstu.login.utils.URL;

public class FindPasswordActivity extends Activity implements OnClickListener {

    private final static String TAG = "FindPasswordActivity";
    private String mGetForgetURL;
    private String mGetVerificationCodeURL;

    private Button mFindPasswordBack;
    private Button mFindPasswordBtn;
    private Button mGetMessage;
    private EditText mFindPasswId;
    private EditText mFindPasswAuth;
    private TextView mFindPasswordText;
    private TextView mFindPasswIdText;
    private String mIsPhone;
    private TimeCount mTimeCount;

    private ProgressDialog mProDialog;
    private HttpClient httpClient;
    private HttpClient httpClientVerify;

    private String userName = "";
    private String authCode = "";
    private String uuid;
    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_findpassword);
        initView();
        mTimeCount = new TimeCount(60000, 1000);

        mProDialog = new ProgressDialog(this);
        mProDialog.setCancelable(true);
        mProDialog.setCanceledOnTouchOutside(false);
        mProDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                httpClient.getConnectionManager().shutdown();
                httpClientVerify.getConnectionManager().shutdown();
            }
        });
    }

    /**
     * 控件的初始化
     */
    private void initView() {
        mFindPasswordBack = (Button) findViewById(R.id.findPasswordBack);
        mFindPasswordBack.setOnClickListener(this);
        mFindPasswordBtn = (Button) findViewById(R.id.findPasswordBtn);
        mFindPasswordBtn.setOnClickListener(this);
        mGetMessage = (Button) findViewById(R.id.getMessage);
        mGetMessage.setOnClickListener(this);
        mFindPasswordText = (TextView) findViewById(R.id.findPasswordText);
        mFindPasswordText.setOnClickListener(this);
        mFindPasswIdText = (TextView) findViewById(R.id.findPasswIdText);

        mFindPasswId = (EditText) findViewById(R.id.findPasswId);
        mFindPasswId.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus == false) {
                    isPhone();
                }
            }
        });
        mFindPasswAuth = (EditText) findViewById(R.id.findPasswAuth);
    }

    /**
     * 控件的点击事件
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.findPasswordBack:
                FindPasswordActivity.this.finish();
                break;
            case R.id.findPasswordText:
                FindPasswordActivity.this.finish();
                break;
            case R.id.getMessage:
                if (!mFindPasswId.getText().toString().isEmpty()) {
                    if (isPhone()) {
                        //TODO:获取验证码
                        Log.i(TAG, "==call getVerificationCode()");
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                getVerificationCode();
                            }
                        }).start();

                        mProDialog.setMessage(getString(R.string.getting_authcode));
                        mProDialog.show();

                    } else {
                        Toast.makeText(this, R.string.number_again, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(this, R.string.number_first, Toast.LENGTH_LONG).show();
                }
                break;

            case R.id.findPasswordBtn:
                if (!mIsPhone.isEmpty()) {
                    if (isPhone()) {
                        if (!mFindPasswAuth.getText().toString().isEmpty()) {
                            Log.i(TAG, "== call submit() ==");
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    confirm();
                                }
                            }).start();

                            mProDialog.setMessage(getString(R.string.please_wait));
                            mProDialog.show();
                        } else {
                            Toast.makeText(this, R.string.get_authcode_first, Toast.LENGTH_LONG).show();
                        }
                    }
                } else {
                    Toast.makeText(this, R.string.number_cannot_null, Toast.LENGTH_LONG).show();
                }
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
                case 1:
                    if (null != mProDialog) {
                        mProDialog.dismiss();
                    }
                    new AlertDialog.Builder(FindPasswordActivity.this)
                            .setTitle(getString(R.string.hint))
                            .setMessage(R.string.please_set_newpsw)
                            .setPositiveButton(getString(R.string.isture),
                                    new DialogInterface.OnClickListener() {

                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent i = new Intent(FindPasswordActivity.this, SetNewPasswordActivity.class);
                                            i.putExtra("user", mIsPhone);
                                            i.putExtra("uuid", uuid);
                                            i.putExtra("token", token);
                                            FindPasswordActivity.this.finish();
                                            startActivity(i);
                                        }
                                    }).show();

                    break;
                case 2:
                    if (null != mProDialog) {
                        mProDialog.dismiss();
                    }
                    Toast.makeText(FindPasswordActivity.this, R.string.confirm_fail, Toast.LENGTH_SHORT).show();
                    break;

                case 3:
                    if (null != mProDialog) {
                        mProDialog.dismiss();
                    }
                    Toast.makeText(FindPasswordActivity.this, R.string.user_not_register, Toast.LENGTH_SHORT).show();
                    break;

                case 4:
                    if (null != mProDialog) {
                        mProDialog.dismiss();
                    }
                    Toast.makeText(FindPasswordActivity.this, R.string.authcode_wrong, Toast.LENGTH_SHORT).show();
                    break;

                case 5:
                    if (null != mProDialog) {
                        mProDialog.dismiss();
                    }
                    mTimeCount.start();
                    Toast.makeText(FindPasswordActivity.this, R.string.wait_message, Toast.LENGTH_SHORT).show();
                    break;

                case 6:
                    if (null != mProDialog) {
                        mProDialog.dismiss();
                    }
                    Toast.makeText(FindPasswordActivity.this, R.string.get_authcode_fail, Toast.LENGTH_SHORT).show();
                    break;

                case 7:
                    if (null != mProDialog) {
                        mProDialog.dismiss();
                    }
                    break;
                default:
                    break;

                case 8:
                    if (null != mProDialog) {
                        mProDialog.dismiss();
                    }
                    Toast.makeText(FindPasswordActivity.this, getString(R.string.time_out), Toast.LENGTH_SHORT).show();
                    break;

                case 9:
                    if (null != mProDialog) {
                        mProDialog.dismiss();
                    }
                    Toast.makeText(FindPasswordActivity.this, "用户未注册", Toast.LENGTH_SHORT).show();
                    break;
            }
        }

        ;
    };

    public boolean isPhone() {
        mIsPhone = mFindPasswId.getText().toString();
        // 手机号码的正则判断
        Pattern pattern = Pattern.compile("^1[3,5,8]\\d{9}$");
        Matcher matcher = pattern.matcher(mIsPhone);

        if (matcher.find()) {
            mFindPasswIdText.setVisibility(View.INVISIBLE);
            return true;
        } else {
            if (mFindPasswId.length() != 0) {
                mFindPasswIdText.setVisibility(View.VISIBLE);
            }
            return false;
        }
    }

    public void getVerificationCode() {
        Log.i(TAG, "== getVerificationCode GET request ==");
        mGetVerificationCodeURL = URL.getURL(this) + "/forcode";
        String userName = mFindPasswId.getText().toString();
        String getUrl = mGetVerificationCodeURL + "?tel=" + userName;
        Log.e("getUrl", getUrl);

        try {
            // URL使用基本URL即可，其中不需要加参数
            HttpGet httpGet = new HttpGet(getUrl);
            // 需要客户端对象来发送请求
            httpClientVerify = new DefaultHttpClient();
            httpClientVerify.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 30 * 1000);
            // 发送请求
            //httpClient.execute(httpGet);
            HttpResponse response = httpClientVerify.execute(httpGet);
            // 返回响应结果
            getResponseResult(response);
        } catch (ConnectTimeoutException e) {
            Log.e(TAG, getString(R.string.time_out));
            sendUiMessage(8);
        } catch (Exception e) {
            sendUiMessage(2);
            e.printStackTrace();
        }
    }

    private void getResponseResult(HttpResponse response) {
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

            if (result.contains("not registered")) {
                sendUiMessage(9);
            } else {
                JSONObject jsonObject = new JSONObject(result);
                int status = jsonObject.getInt("status");
                Log.i(TAG, "== the status is: " + status);
                // TODO:处理返回结果
                if (status == 1) {
                    sendUiMessage(5);
                } else sendUiMessage(6);
            }
        } catch (JSONException e) {
            sendUiMessage(7);
            Log.e(TAG, getString(R.string.JSON_exception));
            e.printStackTrace();
        } catch (IOException e) {
            sendUiMessage(6);
            e.printStackTrace();
        }
    }

    public void confirm() {
        Log.i(TAG, "== submit POST request ==");
        mGetForgetURL = URL.getURL(this) + "/forgot";
        // 获取用户名和密码
        userName = mFindPasswId.getText().toString();
        authCode = mFindPasswAuth.getText().toString();

        NameValuePair userNamePair = new BasicNameValuePair("email", userName);
        NameValuePair authPair = new BasicNameValuePair("checkCode", authCode);

        List<NameValuePair> pairList = new ArrayList<NameValuePair>();
        pairList.add(userNamePair);
        pairList.add(authPair);

        try {
            HttpEntity requestHttpEntity = new UrlEncodedFormEntity(
                    pairList);
            // URL使用基本URL即可，其中不需要加参数
            HttpPost httpPost = new HttpPost(mGetForgetURL);
            // 将请求体内容加入请求中
            httpPost.setEntity(requestHttpEntity);
            // 需要客户端对象来发送请求
            httpClient = new DefaultHttpClient();
            httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 30 * 1000);
            // 发送请求
            HttpResponse response = httpClient.execute(httpPost);
            // 返回响应结果
            postForgetResult(response);
        } catch (ConnectTimeoutException e) {
            Log.e(TAG, getString(R.string.time_out));
            sendUiMessage(8);
        } catch (Exception e) {
            sendUiMessage(2);
            e.printStackTrace();
        }
    }

    /**
     * 显示响应结果
     *
     * @param response
     */
    private void postForgetResult(HttpResponse response) {
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
            boolean isSuccessful = jsonObject.has("uuid");


            if (isSuccessful) {
                sendUiMessage(1);
                uuid = jsonObject.getString("uuid");
                token = jsonObject.getString("token");

                Log.i(TAG, "== uuid = " + uuid);
                Log.i(TAG, "== token = " + token);


            } else if (result.equals("Device not found for email address")) {
                sendUiMessage(3);
            } else if (result.equals("forgot checkCode error")) {
                sendUiMessage(4);
            } else sendUiMessage(2);

        } catch (Exception e) {
            sendUiMessage(2);
            Log.i(TAG, getString(R.string.confirm_fail));
            e.printStackTrace();
        }
    }

    public void sendUiMessage(int result) {
        Message msg = new Message();
        msg.what = result;
        mHandler.sendMessage(msg);
    }

    class TimeCount extends CountDownTimer {

        public TimeCount(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {// 计时完毕
            mGetMessage.setText(R.string.get_authcode);
            mGetMessage.setClickable(true);
        }

        @Override
        public void onTick(long millisUntilFinished) {// 计时过程
            mGetMessage.setClickable(false);//防止重复点击
            mGetMessage.setText(millisUntilFinished / 1000 + "s");
        }
    }

}
