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
import android.widget.Button;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import cn.edu.zstu.login.R;
import cn.edu.zstu.login.utils.URL;

public class MidActivity extends Activity {

    private static final String TAG = "MidActivity";

    private static final int LOGOUT_SUCCESS = 1;
    private static final int LOGOUT_FAIL = 2;
    private static final int TIME_OUT = 3;
    private static final int LOGOUT_ERR = 4;
    private static final int TO_LOGIN = 5;

    private Button mButton;
    private ProgressDialog mProDialog;
    private HttpClient httpClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mid);

        mProDialog = new ProgressDialog(this);
        mProDialog.setCancelable(true);
        mProDialog.setCanceledOnTouchOutside(false);
        mProDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                httpClient.getConnectionManager().shutdown();
            }
        });

        mButton = (Button) findViewById(R.id.button);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mProDialog.setMessage(getString(R.string.login_ing));
                mProDialog.show();

                Log.i("Logout", "== call logout() ==");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        logout();
                    }
                }).start();
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                isLogin();
            }
        }).start();
    }

    /**
     * 处理GET请求的返回结果
     */
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case LOGOUT_SUCCESS:
                    if (null != mProDialog) {
                        mProDialog.dismiss();
                    }
                    new AlertDialog.Builder(MidActivity.this)
                            .setTitle(getString(R.string.hint))
                            .setMessage(R.string.logout_success)
                            .setPositiveButton(getString(R.string.isture),
                                    new DialogInterface.OnClickListener() {

                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent i = new Intent(MidActivity.this, LoginActivity.class);
                                            MidActivity.this.finish();
                                            startActivity(i);
                                        }
                                    }).show();

                    break;
                case LOGOUT_FAIL:
                    if (null != mProDialog) {
                        mProDialog.dismiss();
                    }
                    Toast.makeText(MidActivity.this, R.string.logout_fail,
                            Toast.LENGTH_SHORT).show();
                    break;
                case TIME_OUT:
                    if (null != mProDialog) {
                        mProDialog.dismiss();
                    }
                    Toast.makeText(MidActivity.this, R.string.time_out,
                            Toast.LENGTH_SHORT).show();
                    break;

                case LOGOUT_ERR:
                    if (null != mProDialog) {
                        mProDialog.dismiss();
                    }
                    Toast.makeText(MidActivity.this, R.string.JSON_exception,
                            Toast.LENGTH_SHORT).show();
                    break;

                case TO_LOGIN:
                    Intent i = new Intent(MidActivity.this, LoginActivity.class);
                    MidActivity.this.finish();
                    startActivity(i);

                default:
                    break;
            }
        }

        ;
    };

    public void logout() {
        Log.i("Logout", "== logout GET request ==");
        SharedPreferences preferences = getSharedPreferences("session", Context.MODE_PRIVATE);
        String uuid = preferences.getString("uuid", "");
        String token = preferences.getString("token", "");
        String logoutURL = URL.getURL(this) + "/logout?uuid=" + uuid + "&token=" + token;
        Log.i("Logout", logoutURL);

        try {
            // URL使用基本URL即可，其中不需要加参数
            HttpGet httpGet = new HttpGet(logoutURL);
            // 需要客户端对象来发送请求
            httpClient = new DefaultHttpClient();
            httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 30 * 1000);
            // 发送请求
            HttpResponse response = httpClient.execute(httpGet);
            // 返回响应结果
            getResponseResult(response);
        } catch (ConnectTimeoutException e) {
            Log.e("Logout", getString(R.string.time_out));
            sendUiMessage(TIME_OUT);
        } catch (Exception e) {
            Log.i("Logout", getString(R.string.logout_fail));
            sendUiMessage(LOGOUT_FAIL);
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
            Log.i("Logout", "==Response Content from server: " + result);

            JSONObject jsonObject = new JSONObject(result);
            String status = jsonObject.getString("status");
            Log.i("Logout", "== the status is: " + status);
            // TODO:处理返回结果
            if (status.equals("failure")) {
                sendUiMessage(LOGOUT_FAIL);
            } else if (status.equals("succeed")) sendUiMessage(LOGOUT_SUCCESS);
        } catch (JSONException e) {
            sendUiMessage(LOGOUT_ERR);
            Log.e("Logout", getString(R.string.JSON_exception));
            e.printStackTrace();
        } catch (IOException e) {
            sendUiMessage(LOGOUT_FAIL);
            e.printStackTrace();
        }
    }

    public void sendUiMessage(int l) {
        Message msg = new Message();
        msg.what = l;
        mHandler.sendMessage(msg);
    }

    public void isLogin() {
        Log.i(TAG, "==isLogin GET request");

        SharedPreferences preferences = getSharedPreferences("session", Activity.MODE_PRIVATE);
        String uuid = preferences.getString("uuid", "");
        String token = preferences.getString("token", "");
        if ((uuid.equals("")) && (token.equals(""))) {
            sendUiMessage(TO_LOGIN);
        } else {

            Log.i(TAG, "== uuid =" + uuid + '\n' + "== token =" + token);
            String url = URL.getURL(this) + "?uuid=" + uuid + "&token=" + token;
            HttpGet httpGet = new HttpGet(url);
            HttpClient httpClient = new DefaultHttpClient();
            httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 10 * 1000);

            try {
                HttpResponse response = httpClient.execute(httpGet);
                responseResult(response);
            } catch (Exception e) {
                sendUiMessage(TO_LOGIN);
                e.printStackTrace();
            }
        }
    }

    /**
     * Display the response.
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
            String line;
            while (null != (line = reader.readLine())) {
                result += line;
            }

            JSONObject jsonObject = new JSONObject(result);
            String status = jsonObject.getString("status");
            if (status.equals("online")) {
                //sendUiMessage(ONLINE);
                Log.e(TAG, "is online");
//            } else sendUiMessage(OFFLINE);
            } else {
                sendUiMessage(TO_LOGIN);
                Log.e(TAG, "is offline");
            }
            Log.i(TAG, "==Response Content from server: " + result);
        } catch (Exception e) {
            sendUiMessage(TO_LOGIN);
            Log.e(TAG, "exception");
            e.printStackTrace();
        }
    }
}
