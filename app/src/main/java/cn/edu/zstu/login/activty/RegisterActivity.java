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

public class RegisterActivity extends Activity implements OnClickListener,
		OnFocusChangeListener {

	private final static String TAG = "RegisterActivity";
	private String registerURL;
	private String getVerificationCodeURL;

	private Button registerBack;
	private Button registerBtn;
	private Button getMessage;
	private EditText registerId;
	private EditText registerPassword;
	private EditText registerAuth;
	private TextView registerBackText;
	private TextView registerIdText;
	private TextView registerPwText;
	private String isPhone, isPassword;
	private TimeCount timeCount;

	private HttpClient httpClient;
	private HttpClient httpClientVerify;
	private ProgressDialog mProDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_register);
		initView();
		timeCount = new TimeCount(60000, 1000);

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

	private void initView() {
		registerBack = (Button) findViewById(R.id.registerBack);
		registerBack.setOnClickListener(this);
		registerBtn = (Button) findViewById(R.id.registerBtn);
		registerBtn.setOnClickListener(this);

		/////////////////////////////////////////////////
		getMessage = (Button) findViewById(R.id.getMessage);
		getMessage.setOnClickListener(this);

		registerBackText = (TextView) findViewById(R.id.registerBackText);
		registerBackText.setOnClickListener(this);

		registerId = (EditText) findViewById(R.id.registerId);
		registerId.setOnFocusChangeListener(this);
		registerPassword = (EditText) findViewById(R.id.registerPassword);
		registerPassword.setOnFocusChangeListener(this);
		registerAuth = (EditText) findViewById(R.id.registerAuth);
		registerAuth.setOnFocusChangeListener(this);
		registerAuth.setOnClickListener(this);

		registerIdText = (TextView) findViewById(R.id.registerIdText);
		registerPwText = (TextView) findViewById(R.id.registerPwText);
		//registerAuthText = (TextView) findViewById(R.id.registerAuthText);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.registerBack:
				RegisterActivity.this.finish();
				break;
			case R.id.registerBackText:
				RegisterActivity.this.finish();
				break;
			case R.id.registerAuth:
				registerAuth.setFocusable(true);
				registerAuth.setFocusableInTouchMode(true);
				registerAuth.requestFocus();
				registerAuth.findFocus();
				break;

			case R.id.getMessage:
				if (!registerId.getText().toString().isEmpty()) {
					if (isPhone()) {
						//TODO:获取验证码
						Log.i(TAG, "==call getVerificationCode()");
						new Thread(new Runnable() {
							@Override
							public void run() {
								//TODO:放到服务器返回结果成功的时候触发
								//timeCount.start();
								getVerificationCode();
							}
						}).start();

						mProDialog.setMessage(getString(R.string.getting_authcode));
						mProDialog.show();

					}
				} else {
					Toast.makeText(this, getString(R.string.number_cannot_null), Toast.LENGTH_LONG).show();
				}
				break;

			case R.id.registerBtn:

				if (isPhone()) {
					if (isPassword()) {
						if (!isPhone.isEmpty()) {
							Log.i(TAG, "== call register() ==");
							new Thread(new Runnable() {
								@Override
								public void run() {
									register();
								}
							}).start();

							mProDialog.setMessage(getString(R.string.register_ing));
							mProDialog.show();

						} else {
							Toast.makeText(this, getString(R.string.get_authcode_first), Toast.LENGTH_LONG).show();
						}
					}
				}
				break;
			default:
				break;
		}

	}

	@Override
	public void onFocusChange(View v, boolean hasFocus) {
		switch (v.getId()) {
			case R.id.registerId:
				if (hasFocus == false) {
					isPhone();
				}
				break;
			case R.id.registerPassword:
				if (hasFocus == false) {
					isPassword();
				}
				break;
			default:
				break;
		}
	}

	/**
	 *  处理POST请求的返回结果
	 */
	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case 1:
					if (null != mProDialog) {
						mProDialog.dismiss();
					}
					new AlertDialog.Builder(RegisterActivity.this)
							.setTitle(getString(R.string.hint))
							.setMessage(R.string.register_success)
							.setPositiveButton(getString(R.string.isture),
									new DialogInterface.OnClickListener() {

										@Override
										public void onClick(DialogInterface dialog, int which) {
											Intent i = new Intent(RegisterActivity.this, LoginActivity.class);
											i.putExtra("myId", isPhone);
											RegisterActivity.this.finish();
											startActivity(i);
										}
									}).show();
					break;

				case 2:
					if (null != mProDialog) {
						mProDialog.dismiss();
					}
					Toast.makeText(RegisterActivity.this, R.string.register_fail, Toast.LENGTH_SHORT).show();
					break;

				case 3:
					if (null != mProDialog) {
						mProDialog.dismiss();
					}
					timeCount.start();
					Toast.makeText(RegisterActivity.this, getString(R.string.wait_message), Toast.LENGTH_SHORT).show();
					break;

				case 4:
					if (null != mProDialog) {
						mProDialog.dismiss();
					}
					Toast.makeText(RegisterActivity.this, getString(R.string.get_authcode_fail), Toast.LENGTH_SHORT).show();
					break;
				case 5:
					if (null != mProDialog) {
						mProDialog.dismiss();
					}
					Toast.makeText(RegisterActivity.this, R.string.register_fail, Toast.LENGTH_SHORT).show();
					break;
				case 6:
					if (null != mProDialog) {
						mProDialog.dismiss();
					}
					Toast.makeText(RegisterActivity.this, getString(R.string.time_out), Toast.LENGTH_SHORT).show();
					break;
				case 7:
					if (null != mProDialog) {
						mProDialog.dismiss();
					}
					Toast.makeText(RegisterActivity.this, "验证码错误", Toast.LENGTH_SHORT).show();
					break;
				case 8:
					if (null != mProDialog) {
						mProDialog.dismiss();
					}
					Toast.makeText(RegisterActivity.this, "用户已存在", Toast.LENGTH_SHORT).show();
					break;
				default:
					break;
			}
		};
	};

	public boolean isPhone() {
		isPhone = registerId.getText().toString();
		// 手机号码的正则判断
		Pattern pattern = Pattern.compile("^((1[3,5,8][0-9])|(14[5,7])|(17[0,6,7,8]))\\d{8}$");

		Matcher matcher = pattern.matcher(isPhone);

		if (matcher.find()) {
			registerIdText.setVisibility(View.INVISIBLE);
			return true;
		} else {
			if (registerId.length() != 0) {
				registerIdText.setVisibility(View.VISIBLE);
			}
			return false;
		}
	}

	public boolean isPassword() {
		isPassword = registerPassword.getText().toString();
		if (isPassword.length() < 6 || isPassword.length() > 20) {
			registerPwText.setVisibility(View.VISIBLE);
			return false;
		} else {
			registerPwText.setVisibility(View.INVISIBLE);
			return true;
		}
	}

	public void register() {

		Log.i(TAG, "==register POST request");
		registerURL = URL.getURL(this) + "/devices";
		// 获取用户名和密码
		String userName = registerId.getText().toString();
		String passWord = registerPassword.getText().toString();
		String auth = registerAuth.getText().toString();

		NameValuePair userNamePair = new BasicNameValuePair("email", userName);
		NameValuePair passWordPair = new BasicNameValuePair("password", passWord);
		NameValuePair checkCodePair = new BasicNameValuePair("checkCode", auth);

		List<NameValuePair> pairList = new ArrayList<NameValuePair>();
		pairList.add(userNamePair);
		pairList.add(passWordPair);
		pairList.add(checkCodePair);

		try {

			HttpEntity requestHttpEntity = new UrlEncodedFormEntity(pairList);
			// URL使用基本URL即可，其中不需要加参数
			HttpPost httpPost = new HttpPost(registerURL);
			// 将请求体内容加入请求中
			httpPost.setEntity(requestHttpEntity);
			// 需要客户端对象来发送请求
			httpClient = new DefaultHttpClient();
			httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 60 * 1000);
			// 发送请求
			HttpResponse response = httpClient.execute(httpPost);
			// 返回响应结果
			postResponseResult(response);
		} catch (ConnectTimeoutException e) {
			Log.e(TAG, getString(R.string.time_out));
			sendUiMeesage(6);
		} catch (Exception e) {
			Log.i(TAG, getString(R.string.register_fail));
			sendUiMeesage(2);
			e.printStackTrace();
		}
	}

	/**
	 * 显示响应结果
	 * @param response
	 */
	private void postResponseResult(HttpResponse response)
	{
		if (null == response) {
			return;
		}

		HttpEntity httpEntity = response.getEntity();
		try {
			InputStream inputStream = httpEntity.getContent();
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			String result = "";
			String line = "";
			while (null != (line = reader.readLine())) {
				result += line;
			}

			Log.i(TAG, "==Response Content from server: " + result);

			if (result.equals("checkCode wrong")) {
				sendUiMeesage(7);
			} else {
				JSONObject jsonObject = new JSONObject(result);
				if (jsonObject.has("error")) {
					if (jsonObject.getString("error").equals("Unable to create user")) {
						sendUiMeesage(8);
					} else sendUiMeesage(2);
				} else {
					boolean isSuccessful = jsonObject.has("device");
					if (isSuccessful) {

						JSONObject deviceValue = new JSONObject(jsonObject.getString("device"));
						String uuid = deviceValue.getString("uuid");
						String token = deviceValue.getString("token");

						Log.i(TAG, "==" + uuid);
						Log.i(TAG, "==" + token);

						sendUiMeesage(1);
					} else sendUiMeesage(2);

					Log.i(TAG, "==" + isSuccessful);
				}
			}
		} catch (JSONException e) {
			sendUiMeesage(5);
			Log.e(TAG, getString(R.string.JSON_exception));
			e.printStackTrace();
		} catch (IOException e) {
			sendUiMeesage(2);
			e.printStackTrace();
		}
	}

	public void sendUiMeesage(int result) {
		Message msg = new Message();
		msg.what = result;
		mHandler.sendMessage(msg);
	}

	public void getVerificationCode() {
		Log.i(TAG, "== getVerificationCode GET request ==");
		getVerificationCodeURL = URL.getURL(this) + "/auth";
		String userName = registerId.getText().toString();
        String getUrl = getVerificationCodeURL + "?tel=" + userName;
        Log.e("getUrl", getUrl);

		try {
			// URL使用基本URL即可，其中不需要加参数
			HttpGet httpGet = new HttpGet(getUrl);
			// 需要客户端对象来发送请求
			httpClientVerify = new DefaultHttpClient();
			httpClientVerify.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 30 * 1000);
			// 发送请求
			HttpResponse response = httpClientVerify.execute(httpGet);
			// 返回响应结果
			getResponseResult(response);
		} catch (ConnectTimeoutException e) {
			Log.e(TAG, getString(R.string.time_out));
			sendUiMeesage(6);
		} catch (Exception e) {
			Log.i(TAG, getString(R.string.get_authcode_fail));
			sendUiMeesage(4);
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

			JSONObject jsonObject = new JSONObject(result);
			int status = jsonObject.getInt("status");
			Log.i(TAG, "== the status is: " + status);
			// TODO:处理返回结果
			if (status == 1) {
				sendUiMeesage(3);
			} else sendUiMeesage(4);
		} catch (JSONException e) {
			sendUiMeesage(5);
			Log.e(TAG, getString(R.string.JSON_exception));
			e.printStackTrace();
		} catch (IOException e) {
			sendUiMeesage(4);
			e.printStackTrace();
		}
	}

	class TimeCount extends CountDownTimer {

		public TimeCount(long millisInFuture, long countDownInterval) {
			super(millisInFuture, countDownInterval);
		}

		@Override
		public void onFinish() {// 计时完毕
			getMessage.setText(getString(R.string.get_authcode));
			getMessage.setClickable(true);
		}

		@Override
		public void onTick(long millisUntilFinished) {// 计时过程
			getMessage.setClickable(false);//防止重复点击
			getMessage.setText(millisUntilFinished / 1000 + "s");
		}
	}
}
