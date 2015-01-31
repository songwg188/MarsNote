package com.mars.note.app;

import com.mars.note.R;
import com.mars.note.api.BaseActivity;
import com.mars.note.api.Config;
import com.mars.note.api.AlertDialogFactory;
import com.mars.note.utils.FileHelper;
import com.mars.note.views.BounceViewPager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

public class ThemeSettingsActivity extends BaseActivity implements OnClickListener {
	private String TAG = "ThemeSettings";
	private Activity mActivity;
	private BounceViewPager mViewPager;
	private PagerAdapter adpater;
	private View[] themeViews;
	private LayoutInflater inflater;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mActivity = this;
		this.setContentView(R.layout.activity_theme_settings);
		mViewPager = (BounceViewPager) this.findViewById(R.id.viewpager);
		mViewPager.setOffscreenPageLimit(3);
		mViewPager.setSpringBack(true);
		initThemes();
		adpater = new MyPagerAdapter();
		mViewPager.setAdapter(adpater);
	}

	private void initThemes() {
		Drawable d1 = this.getResources().getDrawable(R.drawable.theme1_preview);
		Drawable d2 = this.getResources().getDrawable(R.drawable.theme2_preview);

		themeViews = new View[2];
		View defaultThemeView = inflater.inflate(R.layout.view_pager_item, null);
		ImageView img = (ImageView) defaultThemeView.findViewById(R.id.theme_pic);
		TextView txt = (TextView) defaultThemeView.findViewById(R.id.theme_description);
		txt.setText(getString(R.string.theme1_title));
		img.setBackground(d1);
		defaultThemeView.setTag(1);
		defaultThemeView.setOnClickListener(this);
		View secondThemeView = inflater.inflate(R.layout.view_pager_item, null);
		ImageView img2 = (ImageView) secondThemeView.findViewById(R.id.theme_pic);
		TextView txt2 = (TextView) secondThemeView.findViewById(R.id.theme_description);
		txt2.setText(R.string.theme2_title);
		img2.setBackground(d2);
		secondThemeView.setTag(2);
		secondThemeView.setOnClickListener(this);
		themeViews[0] = defaultThemeView;
		themeViews[1] = secondThemeView;
	}

	@Override
	public void onResume() {
		super.onResume();
	}

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
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.theme_settings, menu);
		return true;
	}

	private class MyPagerAdapter extends PagerAdapter {
		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return themeViews.length;
		}

		@Override
		public boolean isViewFromObject(View v, Object o) {
			// TODO Auto-generated method stub
			return v == o;
		}

		@Override
		public void destroyItem(View v, int position, Object object) {
			((ViewPager) v).removeView(themeViews[position]);
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			container.addView(themeViews[position]);
			return themeViews[position];
		}
	}

	@Override
	public void onClick(View v) {
		final int id = (Integer) v.getTag();
		String message = null;
		switch (id) {
		case 1:
			message = this.getString(R.string.theme_dialog_title, this.getString(R.string.theme1_title));
			break;
		case 2:
			message = this.getString(R.string.theme_dialog_title, this.getString(R.string.theme2_title));
			break;
		}
		
		AlertDialogFactory.showAlertDialog(this, mViewPager, message, new AlertDialogFactory.DialogPositiveListner(){

			@Override
			public void onClick(View arg0) {
				super.onClick(arg0);
				switch (id) {
				case 1:
					Config.current_theme = 1;
					Config.recent_needRefresh = true;
					SharedPreferences pref = getSharedPreferences("theme", Context.MODE_PRIVATE);
					android.content.SharedPreferences.Editor editor = pref.edit();
					editor.putInt("theme_id", 1);
					editor.commit();
					finish();
					break;
				case 2:
					Config.current_theme = 2;
					Config.recent_needRefresh = true;
					SharedPreferences pref2 = getSharedPreferences("theme", Context.MODE_PRIVATE);
					android.content.SharedPreferences.Editor editor2 = pref2.edit();
					editor2.putInt("theme_id", 2);
					editor2.commit();
					finish();
					break;
				}
			}
		});

		// AlertDialog.Builder builder = new AlertDialog.Builder(this);
		//
		// builder.setPositiveButton(R.string.yes, new
		// DialogInterface.OnClickListener() {
		// @Override
		// public void onClick(DialogInterface arg0, int arg1) {
		// switch (id) {
		// case 1:
		// Config.current_theme = 1;
		// Config.recent_needRefresh = true;
		// SharedPreferences pref = getSharedPreferences("theme",
		// Context.MODE_PRIVATE);
		// android.content.SharedPreferences.Editor editor = pref.edit();
		// editor.putInt("theme_id", 1);
		// editor.commit();
		// finish();
		// break;
		// case 2:
		// Config.current_theme = 2;
		// Config.recent_needRefresh = true;
		// SharedPreferences pref2 = getSharedPreferences("theme",
		// Context.MODE_PRIVATE);
		// android.content.SharedPreferences.Editor editor2 = pref2.edit();
		// editor2.putInt("theme_id", 2);
		// editor2.commit();
		// finish();
		// break;
		// }
		// }
		// });
		// builder.setNegativeButton(R.string.no, null);
		// builder.show();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
}
