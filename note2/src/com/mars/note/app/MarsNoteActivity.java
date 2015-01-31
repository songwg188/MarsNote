package com.mars.note.app;

import java.lang.reflect.Field;

import com.mars.note.R;
import com.mars.note.api.Config;
import com.mars.note.api.FragmentCallBack;
import com.mars.note.api.FragmentFactory;
import com.mars.note.fragment.CalendarFragment;
import com.mars.note.fragment.RecentFragment;
import com.mars.note.fragment.SearchFragment;
import com.mars.note.utils.Logg;
import com.mars.note.views.JazzyViewPager;
import com.mars.note.views.JazzyViewPager.TransitionEffect;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.util.DisplayMetrics;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.PopupWindow;
import android.widget.Scroller;
import android.widget.TextView;
import android.widget.Toast;

public class MarsNoteActivity extends FragmentActivity implements FragmentCallBack {
	public static final String TAG = "MarsNoteActivity";

	public static final int SETTINGS = 3;
	public static final int REQUEST_VALIDATE = 4;
	private JazzyViewPager mViewPager;

	private FragmentPagerAdapter adapter;
	private Bitmap bm;
	// fragment part
	private FragmentManager mFragmentManager;
	private RecentFragment mRecentRecordsFragment;
	private CalendarFragment mCalendarFragment;
	private SearchFragment mSearchFragment;
	public static int previousFragment = 0;
	private ViewPagerScroller scroller;
	private Field mScroller;
	private android.support.v4.widget.DrawerLayout mDrawerLayout;// 20141126
																	// 添加抽屉效果
	private boolean isSwitchFragment = false;
	private int changeTo;
	PopupWindow dialog;

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Logg.S("Main onSaveInstanceState");
		super.onSaveInstanceState(outState);
	}

	/**
	 * Bundle savedInstanceState 参数用于保存退出时的参数，如何保存见函数onSaveInstanceState
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// getScreenParam();
		validate();
		setContentView(R.layout.activity_main);
		initDrawerMenu();
		initFragments();
		mViewPager = (JazzyViewPager) this.findViewById(R.id.fragment_viewpager);
		mViewPager.setTouchable(false);
		mViewPager.setTransitionEffect(TransitionEffect.Stack);
		try {
			mScroller = null;
			mScroller = ViewPager.class.getDeclaredField("mScroller");
			mScroller.setAccessible(true);
			scroller = new ViewPagerScroller(mViewPager.getContext());
			mScroller.set(mViewPager, scroller);
		} catch (NoSuchFieldException e) {
		} catch (IllegalArgumentException e) {
		} catch (IllegalAccessException e) {
		}
		mViewPager.setOffscreenPageLimit(3); // cache 2
		adapter = new MyFragmentAdapter(mFragmentManager);
		mViewPager.setAdapter(adapter);
		mViewPager.setCurrentItem(1);

	}

	private void getScreenParam() {
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		int screenWidth = dm.widthPixels;
		int screenHeight = dm.heightPixels;
		float density = dm.density; // 屏幕密度（0.75 / 1.0 / 1.5）
		int densityDpi = dm.densityDpi; // 屏幕密度DPI（120 / 160 / 240）
//		Logg.I("screenWidth = " + screenWidth);
//		Logg.I("screenHeight = " + screenHeight);
//		Logg.I("density = " + density);
//		Logg.I("densityDpi = " + densityDpi);
	}

	private void validate() {
		Intent validate = new Intent(this, LoginActivity.class);
		Bundle bundle = new Bundle();
		bundle.putInt("mode", 0);
		validate.putExtras(bundle);
		this.startActivityForResult(validate, REQUEST_VALIDATE);
	}

	// 添加抽屉效果
	private void initDrawerMenu() {
		mDrawerLayout = (DrawerLayout) this.findViewById(R.id.drawer_layout);
		mDrawerLayout.setDrawerListener(new DrawerListener() {
			@Override
			public void onDrawerStateChanged(int arg0) {
			}

			@Override
			public void onDrawerSlide(View arg0, float arg1) {
				mRecentRecordsFragment.runDrawerAnim(arg1 * 360);
			}

			@Override
			public void onDrawerOpened(View arg0) {
			}

			@Override
			public void onDrawerClosed(View arg0) {
				if (isSwitchFragment) {
					isSwitchFragment = false;
					if (changeTo < SETTINGS) {
						switchFragment(changeTo);
					} else {
						mRecentRecordsFragment.openSettingsActivity();
					}
				}
			}
		});
	}

	public void toChangeFragment(View v) {
		isSwitchFragment = true;
		mRecentRecordsFragment.onBackPressed();
		switch (v.getId()) {
		case R.id.search:
			changeTo = FragmentFactory.SEARCHFRAGMENT;
			break;
		case R.id.recent:
			changeTo = FragmentFactory.RECENTFRAGMENT;

			break;
		case R.id.calendar:
			changeTo = FragmentFactory.CALENDARFRAGMENT;
			break;
		case R.id.settings:
			changeTo = SETTINGS;
			break;
		default:
			break;
		}
		mDrawerLayout.closeDrawers();
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	private void initFragments() {
		mFragmentManager = this.getSupportFragmentManager();
		mRecentRecordsFragment = (RecentFragment) FragmentFactory.newBaseFragmentInstance(FragmentFactory.RECENTFRAGMENT);
		mCalendarFragment = (CalendarFragment) FragmentFactory.newBaseFragmentInstance(FragmentFactory.CALENDARFRAGMENT);
		mSearchFragment = (SearchFragment) FragmentFactory.newBaseFragmentInstance(FragmentFactory.SEARCHFRAGMENT);

	}

	@Override
	public void onBackPressed() {
		switch (mViewPager.getCurrentItem()) {
		case FragmentFactory.SEARCHFRAGMENT:
			switchFragment(FragmentFactory.RECENTFRAGMENT);
			break;
		case FragmentFactory.RECENTFRAGMENT:
			if (mRecentRecordsFragment.onBackPressed()) {
			} else {
				super.onBackPressed();
				mCalendarFragment.isMainThreadAlive = false;
			}
			break;
		case FragmentFactory.CALENDARFRAGMENT:
			switchFragment(FragmentFactory.RECENTFRAGMENT);
			break;
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo mi) {
		getMenuInflater().inflate(R.menu.context_menu_note_activity, menu);
		super.onCreateContextMenu(menu, v, mi);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.delete:
			if (mViewPager.getCurrentItem() == FragmentFactory.RECENTFRAGMENT) {
				switch (Config.current_theme) {
				case 1:
					mRecentRecordsFragment.deleteSingleRecord();
					break;
				case 2:
					break;
				}
			} else if (mViewPager.getCurrentItem() == FragmentFactory.CALENDARFRAGMENT) {
				mCalendarFragment.deleteSingleRecord();
				Config.search_needRefresh = true;
				Config.recent_needRefresh = true;
			}
			break;
		case R.id.settings:
			Intent intent = new Intent(this, NoteSettingsActivity.class);
			this.startActivity(intent);
			break;
		default:
			break;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	public Fragment getCurrentFragment() {
		switch (mViewPager.getCurrentItem()) {
		case FragmentFactory.RECENTFRAGMENT:
			return mRecentRecordsFragment;
		case FragmentFactory.CALENDARFRAGMENT:
			return mCalendarFragment;
		case FragmentFactory.SEARCHFRAGMENT:
			return mSearchFragment;
		}
		return null;
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == FragmentFactory.RECENTFRAGMENT) {
			Config.recent_needRefresh = true;
			mSearchFragment.isAddNoteReturn = true;
		} else if (requestCode == FragmentFactory.SEARCHFRAGMENT) {
			mSearchFragment.isAddNoteReturn = true;
			Config.recent_needRefresh = true;
		} else if (requestCode == FragmentFactory.CALENDARFRAGMENT) {
			Config.calendar_needRefresh = true;
			Config.recent_needRefresh = true;
		} else if (requestCode == REQUEST_VALIDATE) {
			if (resultCode == RESULT_CANCELED) {
				this.finish(); // 20141202 验证失败则退出
			}
		}
	}

	public void addNewNote(View v) {
		if (mViewPager.getCurrentItem() == FragmentFactory.CALENDARFRAGMENT) {
			mCalendarFragment.addNewNote(v);
		}
	}

	@Override
	public void switchFragment(int to) {
		previousFragment = mViewPager.getCurrentItem();
		if (previousFragment == to) {
			return;
		}

		switch (to) {
		case FragmentFactory.CALENDARFRAGMENT:
			mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
			if (previousFragment == FragmentFactory.RECENTFRAGMENT) {
				try {
					scroller.setScrollDuration(1000);
					mScroller.set(mViewPager, scroller);

				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				}
			} else if (previousFragment == FragmentFactory.SEARCHFRAGMENT) {
				try {
					scroller.setScrollDuration(2000);
					mScroller.set(mViewPager, scroller);
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				}
			}
			this.mViewPager.setCurrentItem(FragmentFactory.CALENDARFRAGMENT);
			if (Config.calendar_needRefresh) {
				mCalendarFragment.onResume();
			}
			break;
		case FragmentFactory.RECENTFRAGMENT:

			mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);

			if (previousFragment == FragmentFactory.CALENDARFRAGMENT) {
				try {
					scroller.setScrollDuration(1000);
					mScroller.set(mViewPager, scroller);
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				}
			} else if (previousFragment == FragmentFactory.SEARCHFRAGMENT) {
				try {
					scroller.setScrollDuration(1000);
					mScroller.set(mViewPager, scroller);
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				}
			}
			this.mViewPager.setCurrentItem(FragmentFactory.RECENTFRAGMENT);
			if (Config.recent_needRefresh) {
				mRecentRecordsFragment.onResume();
			}
			break;
		case FragmentFactory.SEARCHFRAGMENT:
			// 20141128 锁住抽屉防止打开键盘时的bug
			mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
			if (previousFragment == FragmentFactory.RECENTFRAGMENT) {
				try {
					scroller.setScrollDuration(1000);
					mScroller.set(mViewPager, scroller);

				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				}
			} else if (previousFragment == FragmentFactory.CALENDARFRAGMENT) {
				try {
					scroller.setScrollDuration(2000);
					mScroller.set(mViewPager, scroller);
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				}
			}
			this.mViewPager.setCurrentItem(FragmentFactory.SEARCHFRAGMENT);
			if (Config.search_needRefresh) {
				mSearchFragment.onResume();
			}
			break;
		}
	}

	private class MyFragmentAdapter extends FragmentPagerAdapter {
		public MyFragmentAdapter(android.support.v4.app.FragmentManager fm) {
			super(fm);
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			Object obj = super.instantiateItem(container, position);
			mViewPager.setObjectForPosition(obj, position);
			return obj;
		}

		@Override
		public Fragment getItem(int position) {
			switch (position) {
			case FragmentFactory.SEARCHFRAGMENT:
				return mSearchFragment;
			case FragmentFactory.RECENTFRAGMENT:
				return mRecentRecordsFragment;
			case FragmentFactory.CALENDARFRAGMENT:
				return mCalendarFragment;
			}
			return null;
		}

		@Override
		public int getCount() {
			return 3;
		}
	}

	public class ViewPagerScroller extends Scroller {
		private int mScrollDuration = 1000;// speed 1000ms

		public ViewPagerScroller(Context context) {
			super(context);
		}

		@Override
		public void startScroll(int startX, int startY, int dx, int dy, int duration) {
			super.startScroll(startX, startY, dx, dy, mScrollDuration);
		}

		@Override
		public void startScroll(int startX, int startY, int dx, int dy) {
			super.startScroll(startX, startY, dx, dy, mScrollDuration);
		}

		public void setScrollDuration(int duration) {
			mScrollDuration = duration;
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onRestart() {
		super.onRestart();
	}

	@Override
	public void openDrawer() {
		mDrawerLayout.openDrawer(Gravity.LEFT);
	}

	public void openInfo(View v) {
		Toast.makeText(this,R.string.product_info , 3000).show();
//		if (dialog == null){
//			View dialogView = LayoutInflater.from(this).inflate(R.layout.info_dialog, null, false);
//			TextView title = (TextView) dialogView.findViewById(R.id.text);
//			title.setText(R.string.product_info);
//			dialog = MarsDialogFactory.getPopupWindow(this, dialogView);
//		}
//		
//		if(dialog.isShowing())
//			return;
//		else
//			dialog.showAtLocation(mDrawerLayout, Gravity.CENTER, 0, 0);
	}
}
