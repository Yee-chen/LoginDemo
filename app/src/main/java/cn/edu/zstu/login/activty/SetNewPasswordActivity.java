package cn.edu.zstu.login.activty;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
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

public class SetNewPasswordActivity extends Activity implements View.OnClickListener, View.OnFocusChangeListener {
    private final static String TAG = "SetNewPasswordActivity";
    private String getSetNewPwURL;

    private Button newPwBack;
    private Button newPwBtn;
    private TextView newPwBackText;
    private EditText newPwId;
    private TextView newPwText;
    private EditText truePwNew;
    private TextView truePwNewText;
    private String isPassword;
    private String isPhone;
    private String uuid;
    private String token;

    private HttpClient httpClient;
    private ProgressDialog mProDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_new_password);

        initView();

        Intent info = super.getIntent();
        isPhone = info.getStringExtra("user");
        uuid = info.getStringExtra("uuid");
        token = info.getStringExtra("token");

        mProDialog = new ProgressDialog(this);
        mProDialog.setCancelable(true);
        mProDialog.setCanceledOnTouchOutside(false);
        mProDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                httpClient.getConnectionManager().shutdown();
            }
        });
    }

    /**
     * 控件的初始化
     */
    private void initView() {
        newPwBack = (Button) findViewById(R.id.newPwBack);
        // 点击事件的监听
        newPwBack.setOnClickListener(this);
        newPwBtn = (Button) findViewById(R.id.newPwBtn);
        newPwBtn.setOnClickListener(this);
        newPwBackText = (TextView) findViewById(R.id.newPwBackText);
        newPwBackText.setOnClickListener(this);

        newPwId = (EditText) findViewById(R.id.newPwId);
        // EditText焦点改变的监听
        newPwId.setOnFocusChangeListener(this);
        truePwNew = (EditText) findViewById(R.id.truePwNew);
        truePwNew.setOnFocusChangeListener(this);

        newPwText = (TextView) findViewById(R.id.newPwText);
        truePwNewText = (TextView) findViewById(R.id.truePwNewText);
    }

    /**
     * 控件的点击事件
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.newPwBack:
                // 关闭当前的Activity
                SetNewPasswordActivity.this.finish();
                break;
            case R.id.newPwBackText:
                SetNewPasswordActivity.this.finish();
                break;
            case R.id.newPwId:
                // EditText重新获取焦点
                newPwId.setFocusable(true);
                newPwId.setFocusableInTouchMode(true);
                newPwId.requestFocus();
                newPwId.findFocus();
                break;
            case R.id.truePwNew:
                // EditText重新获取焦点
                truePwNew.setFocusable(true);
                truePwNew.setFocusableInTouchMode(true);
                truePwNew.requestFocus();
                truePwNew.findFocus();
                break;
            case R.id.newPwBtn:
                // EditText失去焦点
                //newPwId.setFocusable(false);
                //truePwNew.setFocusable(false);

                if (isPassword()) {
                    if (isSame()) {
                        //TODO: 调用提交的接口
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                submit();
                            }
                        }).start();

                        mProDialog.setMessage(getString(R.string.please_wait));
                        mProDialog.show();

                    } else {
                        Toast.makeText(SetNewPasswordActivity.this, R.string.psw_not_same, Toast.LENGTH_SHORT).show();
                    }
                }
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
            case R.id.newPwId:
                if (hasFocus == false) {
                    isPassword();
                }
                break;
            case R.id.truePwNew:
                if (hasFocus == false) {
                    isSame();
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
                    new AlertDialog.Builder(SetNewPasswordActivity.this)
                            .setTitle(getString(R.string.hint))
                            .setMessage(R.string.reset_psw_success)
                            .setPositiveButton(getString(R.string.isture),
                                    new DialogInterface.OnClickListener() {

                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent i = new Intent(SetNewPasswordActivity.this, LoginActivity.class);
                                            i.putExtra("myId", isPhone);
                                            SetNewPasswordActivity.this.finish();
                                            startActivity(i);
                                        }
                                    }).show();

                    break;
                case 2:
                    if (null != mProDialog) {
                        mProDialog.dismiss();
                    }
                    Toast.makeText(SetNewPasswordActivity.this, R.string.reset_psw_fail, Toast.LENGTH_SHORT).show();
                    break;
                case 3:
                    if (null != mProDialog) {
                        mProDialog.dismiss();
                    }
                    Toast.makeText(SetNewPasswordActivity.this, getString(R.string.time_out), Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }

        ;
    };

    public boolean isPassword() {
        isPassword = newPwId.getText().toString();
        if (isPassword.length() < 6 || isPassword.length() > 20) {
            newPwText.setVisibility(View.VISIBLE);
            return false;
        } else {
            newPwText.setVisibility(View.INVISIBLE);
            return true;
        }
    }

    public boolean isSame() {
        if (truePwNew.getText().toString().equals(isPassword)) {
            truePwNewText.setVisibility(View.INVISIBLE);
            return true;
        } else {
            truePwNewText.setVisibility(View.VISIBLE);
            return false;
        }
    }

    //TODO::::::::::::::
    public void submit() {
        Log.i(TAG, "== set new pw POST request ==");
        getSetNewPwURL = URL.getURL(this) + "/reset";
        String newPw = newPwId.getText().toString();

        NameValuePair devicePair = new BasicNameValuePair("device", uuid);
        NameValuePair tokenPair = new BasicNameValuePair("token", token);
        NameValuePair newPwPair = new BasicNameValuePair("password", newPw);

        List<NameValuePair> pairList = new ArrayList<NameValuePair>();
        pairList.add(devicePair);
        pairList.add(tokenPair);
        pairList.add(newPwPair);

        try {
            HttpEntity requestHttpEntity = new UrlEncodedFormEntity(pairList);

            // URL使用基本URL即可，其中不需要加参数
            HttpPost httpPost = new HttpPost(getSetNewPwURL);
            // 将请求体内容加入请求中
            httpPost.setEntity(requestHttpEntity);
            // 需要客户端对象来发送请求
            httpClient = new DefaultHttpClient();
            httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 30 * 1000);
            // 发送请求
            HttpResponse response = httpClient.execute(httpPost);
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
                sendUiMeesage(1);
            } else sendUiMeesage(2);
        } catch (ConnectTimeoutException e) {
            Log.e(TAG, getString(R.string.time_out));
            sendUiMeesage(3);
        } catch (Exception e) {
            sendUiMeesage(2);
            e.printStackTrace();
        }
    }

    public void sendUiMeesage(int result) {
        Message msg = new Message();
        msg.what = result;
        mHandler.sendMessage(msg);
    }
}
