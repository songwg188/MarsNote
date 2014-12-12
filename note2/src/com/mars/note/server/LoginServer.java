package com.mars.note.server;

import com.mars.note.R;
import com.mars.note.api.BaseActivity;
import com.mars.note.fragment.NoteSettingsMenu;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class LoginServer extends BaseActivity implements OnCheckedChangeListener, OnClickListener {
	CheckBox cbx;
	EditText user_name;
	EditText password;
	View showPwdLayout;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_login_server);
		cbx = (CheckBox)this.findViewById(R.id.show_pwd);
		cbx.setOnCheckedChangeListener(this);
		
		user_name = (EditText) this.findViewById(R.id.username);
		password = (EditText) this.findViewById(R.id.pwd);
		
		showPwdLayout = this.findViewById(R.id.showPwdLayout);
		showPwdLayout.setOnClickListener(this);
	};
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			this.onBackPressed();
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCheckedChanged(CompoundButton cb, boolean b) {
		if(cb.getId() == R.id.show_pwd){
//			android.util.Log.d("mars","status = " + b);
			if(b){
				password.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
			}else{
				password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
			}
		}
		
	}
	
	@Override
	public void onClick(View arg0) {
		if(arg0.getId() == R.id.showPwdLayout){
			boolean isChecked = cbx.isChecked();
			cbx.setChecked(!isChecked);
		}
		
	}
	
	public void register(View v){
		this.startActivity(new Intent(this,Register.class));						
	}
}
