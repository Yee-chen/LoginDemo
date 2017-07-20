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
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import cn.edu.zstu.login.R;
import cn.edu.zstu.login.utils.URL;

public class ChangePasswordActivity extends Activity implements OnClickListener, OnFocusChangeListener {
    private static final String TAG = "ChangePasswordActivity";
    private String mChangePswURL;

    private Button mChangePwBack;
    private Button mChangePwBtn;
    private TextView mChangePwBackText;
    private TextView mChangePwText;
    private EditText mChangePw;
    private TextView mChangePwNewText;
    private EditText mChangePwNew;
    private ProgressDialog mProDialog;
    private HttpClient mHttpClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_changepassword);

        initView();

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

    /**
     * 控件的初始化
     */
    private void initView() {
        mChangePwBack = (Button) findViewById(R.id.changePwBack);
        // 点击事件的监听
        mChangePwBack.setOnClickListener(this);
        mChangePwBtn = (Button) findViewById(R.id.changePwBtn);
        mChangePwBtn.setOnClickListener(this);
        mChangePwBackText = (TextView) findViewById(R.id.changePwBackText);
        mChangePwBackText.setOnClickListener(this);

        // EditText焦点改变的监听
        mChangePw = (EditText) findViewById(R.id.changePw);
        mChangePw.setOnFocusChangeListener(this);
        mChangePwNew = (EditText) findViewById(R.id.changePwNew);
        mChangePwNew.setOnFocusChangeListener(this);
        mChangePwNew.setOnClickListener(this);

        mChangePwText = (TextView) findViewById(R.id.changePwText);
        mChangePwNewText = (TextView) findViewById(R.id.changePwNewText);
    }

    /**
     * 控件的点击事件
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.changePwBack:
                // 关闭当前的Activity
                ChangePasswordActivity.this.finish();
                break;
            case R.id.changePwBackText:
                ChangePasswordActivity.this.finish();
                break;
            case R.id.changePwNew:
                // EditText重新获取焦点
                mChangePwNew.setFocusable(true);
                mChangePwNew.setFocusableInTouchMode(true);
                mChangePwNew.requestFocus();
                mChangePwNew.findFocus();
                break;
            case R.id.changePwBtn:
                // EditText失去焦点
                //changePwNew.setFocusable(false);
                Log.i(TAG, "==call changePw() ==");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        changePw();
                    }
                }).start();

                mProDialog.setMessage(getString(R.string.please_wait));
                mProDialog.show();

                break;
            default:
                break;
        }
    }

    /**
     * 焦点监听时间
     *
     * @param v        对应的控件
     * @param hasFocus 是否点击
     */
    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        switch (v.getId()) {
            case R.id.changePw:
                if (hasFocus == false) {
                    isPassword(mChangePw.getText().toString(), mChangePwText);
                }
                break;
            case R.id.changePwNew:
                if (hasFocus == false) {
                    isPassword(mChangePwNew.getText().toString(), mChangePwNewText);
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
                    new AlertDialog.Builder(ChangePasswordActivity.this)
                            .setTitle(getString(R.string.hint))
                            .setMessage(getString(R.string.change_psw_success))
                            .setPositiveButton(getString(R.string.isture),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent i = new Intent(ChangePasswordActivity.this, LoginActivity.class);
                                            //i.putExtra("myId", isPhone);
                                            ChangePasswordActivity.this.finish();
                                            startActivity(i);
                                        }
                                    }).show();

                    break;
                case 2:
                    if (null != mProDialog) {
                        mProDialog.dismiss();
                    }
                    Toast.makeText(ChangePasswordActivity.this, R.string.oldpsw_erro, Toast.LENGTH_SHORT).show();
                    break;
                case 3:
                    if (null != mProDialog) {
                        mProDialog.dismiss();
                    }
                    Toast.makeText(ChangePasswordActivity.this, getString(R.string.time_out), Toast.LENGTH_SHORT).show();
                    break;

                case 4:
                    if (null != mProDialog) {
                        mProDialog.dismiss();
                    }
                    Toast.makeText(ChangePasswordActivity.this, getString(R.string.change_psw_fail), Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    };

    public boolean isPassword(String password, TextView textView) {
        if (password.length() < 6 || password.length() > 20) {
            textView.setVisibility(View.VISIBLE);
            return false;
        } else {
            textView.setVisibility(View.INVISIBLE);
            return true;
        }
    }

    public void changePw() {

        Log.i(TAG, "==register POST request ==");
        mChangePswURL = URL.getURL(this) + "/modify";
        // 获取用户名和密码
        SharedPreferences namePreferences = getSharedPreferences("userInfo", Context.MODE_PRIVATE);
        String isPhone = namePreferences.getString("name", "");
        SharedPreferences preferences = getSharedPreferences("session", Context.MODE_PRIVATE);
        String uuid = preferences.getString("uuid", "");
        String token = preferences.getString("token", "");

        String oldPsw = mChangePw.getText().toString();
        String newPsw = mChangePwNew.getText().toString();

        Log.i(TAG, "== user  = " + isPhone + "\n" +
                   "== uui   = " + uuid + "\n" +
                   "== token = " + token + "\n" +
                   "== oldPsw= " + oldPsw + "\n" +
                   "== newPsw= " + newPsw);

        NameValuePair userPair = new BasicNameValuePair("tel", isPhone);
        NameValuePair uuidPair = new BasicNameValuePair("device", uuid);
        NameValuePair tokenPair = new BasicNameValuePair("token", token);
        NameValuePair oldPswPair = new BasicNameValuePair("oldPassword", oldPsw);
        NameValuePair newPswPair = new BasicNameValuePair("password", newPsw);

        List<NameValuePair> pairList = new ArrayList<NameValuePair>();
        pairList.add(userPair);
        pairList.add(uuidPair);
        pairList.add(tokenPair);
        pairList.add(oldPswPair);
        pairList.add(newPswPair);

        try {
            HttpEntity requestHttpEntity = new UrlEncodedFormEntity(pairList);
            // URL使用基本URL即可，其中不需要加参数
            HttpPost httpPost = new HttpPost(mChangePswURL);
            // 将请求体内容加入请求中
            httpPost.setEntity(requestHttpEntity);
            // 需要客户端对象来发送请求
            mHttpClient = new DefaultHttpClient();
            mHttpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 30 * 1000);
            // 发送请求
            HttpResponse response = mHttpClient.execute(httpPost);
            // 返回响应结果
            if (null == response) {
                return;
            }
            HttpEntity httpEntity = response.getEntity();
            InputStream inputStream = httpEntity.getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String result = "";
            String line = "";
            while (null != (line = reader.readLine())) {
                result += line;
            }
            Log.i(TAG, "==Response Content from server: " + result);

            if (result.equals("Created")) {
                sendUiMessage(1);
            } else if (result.equals("oldPassword error")) {
                sendUiMessage(2);
            } else sendUiMessage(4);
        } catch (ConnectTimeoutException e) {
            Log.e(TAG, getString(R.string.time_out));
            sendUiMessage(3);
        } catch (Exception e) {
            Log.i(TAG, getString(R.string.change_psw_fail));
            sendUiMessage(2);
            e.printStackTrace();
        }
    }

    public void sendUiMessage(int result) {
        Message msg = new Message();
        msg.what = result;
        mHandler.sendMessage(msg);
    }
}
