package com.mars.note.api;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;

/*
 * 统一ActionBar配置 20141208
 */
public class BaseActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initActionbar();
	}

	private void initActionbar() {
		getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY); // 20141202
		this.getActionBar().setDisplayHomeAsUpEnabled(true);
		this.getActionBar().setDisplayShowHomeEnabled(false);
	}
}
