package com.mars.note;

import com.mars.note.views.GestureLockView;
import com.mars.note.views.GestureLockView.OnGestureFinishListener;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class LoginActivity extends Activity implements OnGestureFinishListener {
	private GestureLockView view;
	private TextView attention;
	private String pass1;
	private String pass2;
	private SharedPreferences shares;
	/*
	 * mode 0 ：从主界面进入 3 ：从widget进入 1 ：从设置进入，且未设置密码 2 ：从设置进入，已经设置密码
	 */
	private int mode = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		view = (GestureLockView) this.findViewById(R.id.gesturelockview);
		view.setOnGestureFinishListener(this);
		attention = (TextView) this.findViewById(R.id.attention);

		Bundle bundle = getIntent().getExtras();
		if (bundle == null) {
			throw new NullPointerException("bundle cant be null");
		}

		boolean createOrAlterPass = false;
		createOrAlterPass = bundle.getBoolean("createOrAlterPass", false);

		shares = this.getSharedPreferences("password", Activity.MODE_PRIVATE);
		boolean encrypted = shares.getBoolean("encrypted", false);
		
		if (createOrAlterPass) {

			if (!encrypted) {
				mode = 1;
				attention.setText(R.string.attention_new_pass);
			} else {
				mode = 2;
				attention.setText(R.string.attention_alter_pass);
				pass1 = shares.getString("password", null);
				if (pass1 == null || pass1.equals("")) {
					throw new NullPointerException("pass1 cant be null");
				}
			}
		} else {

			mode = bundle.getInt("mode");
			if (mode == 0) {
				if (encrypted) {
					attention.setText(R.string.attention_enter_pass);
					pass1 = shares.getString("password", null);
					if (pass1 == null || pass1.equals("")) {
						throw new NullPointerException("pass1 cant be null");
					} else {
					}
				} else {
					this.setResult(RESULT_OK);//先执行setResult，否则直接关闭
					this.finish();
				}
			}else if(mode == 3){
				if (encrypted) {
					attention.setText(R.string.attention_enter_pass);
					pass1 = shares.getString("password", null);
					if (pass1 == null || pass1.equals("")) {
						throw new NullPointerException("pass1 cant be null");
					} else {
					}
				} else {
					this.setResult(RESULT_OK);//先执行setResult，否则直接关闭
					this.finish();
				}
			}
		}
	}

	@Override
	public void OnGestureFinish(boolean success, String key) {
//		Log.d("mars", "key = " + key);
		if (mode == 1) {
			if (pass1 == null && key != null && !key.equals("")) {
				pass1 = key;
				attention.setText(R.string.attention_new_pass2);
				attention.setTextColor(Color.BLACK);
			} else if (pass1 != null && !pass1.equals("") && key != null
					&& !key.equals("")) {
				pass2 = key;

				if (pass1.equals(pass2)) {
					Toast.makeText(this, R.string.attention_success, 1000)
							.show();

					Editor editor = shares.edit();
					editor.putBoolean("encrypted", true);
					editor.putString("password", pass1);
					editor.commit();

					pass1 = null;
					pass2 = null;
					this.finish();
				} else {
					pass1 = null;
					pass2 = null;
					attention.setText(R.string.attention_error1);
					attention.setTextColor(Color.RED);
				}
			}
		} else if (mode == 2) {

			if (key != null && !key.equals("")) {
				pass2 = key;

				if (pass1.equals(pass2)) {
					Toast.makeText(this, R.string.attention_alter_pass2, 1000)
							.show();
					attention.setText(R.string.attention_new_pass);
					attention.setTextColor(Color.BLACK);
					Editor editor = shares.edit();
					editor.putBoolean("encrypted", false);
					editor.putString("password", "");
					editor.commit();
					mode = 1;
					pass1 = null;
					pass2 = null;
				} else {
					pass2 = null;
					attention.setText(R.string.attention_error1);
					attention.setTextColor(Color.RED);
				}
			}
		} else if (mode == 0) {
			if (key != null && !key.equals("")) {
				pass2 = key;

				if (pass1.equals(pass2)) {
					pass1 = null;
					pass2 = null;
					attention.setTextColor(Color.BLACK);
					attention.setText(R.string.attention_success2);
					
					this.setResult(RESULT_OK); //先执行setResult，否则直接关闭
					this.finish();
				} else {
					pass2 = null;
					attention.setText(R.string.attention_error1);
					attention.setTextColor(Color.RED);
				}
			}
		}else if (mode == 3) {
			if (key != null && !key.equals("")) {
				pass2 = key;

				if (pass1.equals(pass2)) {
					pass1 = null;
					pass2 = null;
					attention.setTextColor(Color.BLACK);
					attention.setText(R.string.attention_success2);
					
					this.setResult(RESULT_OK); //先执行setResult，否则直接关闭
					this.finish();
				} else {
					pass2 = null;
					attention.setText(R.string.attention_error1);
					attention.setTextColor(Color.RED);
				}
			}
		}

	}

}
