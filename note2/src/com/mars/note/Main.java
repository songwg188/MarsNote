package com.mars.note;

import java.lang.reflect.Field;

import com.mars.note.fragment.CalendarFragment;
import com.mars.note.fragment.RecentRecordsFragment;
import com.mars.note.fragment.SearchFragment;
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
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.Scroller;

public class Main extends FragmentActivity implements FragmentCallBack {
	public static final String TAG = "Main";
	public static final int RECENTFRAGMENT = 1;
	public static final int SEARCHFRAGMENT = 0;
	public static final int CALENDARFRAGMENT = 2;
	public static final int SETTINGS = 3;
	public static final int REQUEST_VALIDATE = 4;
	private JazzyViewPager mViewPager;

	private FragmentPagerAdapter adapter;
	private Bitmap bm;
	// fragment part
	private FragmentManager mFragmentManager;
	private FragmentTransaction mFragmentTransaction;
	private RecentRecordsFragment mRecentRecordsFragment;
	private CalendarFragment mCalendarFragment;
	private SearchFragment mSearchFragment;
	public static int previousFragment = 0;
	private ViewPagerScroller scroller;
	private Field mScroller;
	private android.support.v4.widget.DrawerLayout mDrawerLayout;// 20141126 添加抽屉效果
	private boolean isSwitchFragment = false;
	private int changeTo;

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Logg.D(TAG+" onSaveInstanceState");
	}
	/*
	 * Bundle savedInstanceState 参数用于保存退出时的参数，如何保存见函数onSaveInstanceState
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Logg.D(TAG+" onCreate");
		
		validate();
		setContentView(R.layout.activity_main);
		initDrawerMenu();
		initFragments();
		mViewPager = (JazzyViewPager) this
				.findViewById(R.id.fragment_viewpager);
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

	private void validate() {
		Intent validate = new Intent(this,LoginActivity.class);
		Bundle bundle = new Bundle();
		bundle.putInt("mode",0);
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
			changeTo = SEARCHFRAGMENT;
			break;
		case R.id.recent:
			changeTo = RECENTFRAGMENT;

			break;
		case R.id.calendar:
			changeTo = CALENDARFRAGMENT;
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
		Logg.D(TAG+" onStart");
//		Logg.D("Editor TaskID "+getTaskId());
	}

	private void initFragments() {
		mFragmentManager = this.getSupportFragmentManager();
		mRecentRecordsFragment = new RecentRecordsFragment();
		mCalendarFragment = new CalendarFragment();
		mSearchFragment = new SearchFragment();

	}

	@Override
	public void onBackPressed() {
		switch (mViewPager.getCurrentItem()) {
		case SEARCHFRAGMENT:
			switchFragment(RECENTFRAGMENT);
			break;
		case RECENTFRAGMENT:
			if (mRecentRecordsFragment.onBackPressed()) {
			} else {
				super.onBackPressed();
				mCalendarFragment.isMainThreadAlive = false;
				// isMainThreadAlive = false;
			}
			break;
		case CALENDARFRAGMENT:
			switchFragment(RECENTFRAGMENT);
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
			if (mViewPager.getCurrentItem() == Main.RECENTFRAGMENT) {
				switch (Config.current_theme) {
				case 1:
					mRecentRecordsFragment.deleteSingleRecord();
					break;
				case 2:
					break;
				}
			} else if (mViewPager.getCurrentItem() == Main.CALENDARFRAGMENT) {
				mCalendarFragment.deleteSingleRecord();
				Config.search_needRefresh = true;
				Config.recent_needRefresh = true;
			}
			break;
		case R.id.settings:
			Intent intent = new Intent(this, NoteSettings.class);
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
		case RECENTFRAGMENT:
			return mRecentRecordsFragment;
		case CALENDARFRAGMENT:
			return mCalendarFragment;
		case SEARCHFRAGMENT:
			return mSearchFragment;
		}
		return null;
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		Logg.D(TAG + " onPause");
	}
	
	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		Logg.D(TAG + " onStop");
	}

	@Override
	public void onResume() {
		super.onResume();
		Logg.D(TAG + " onResume");
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == RECENTFRAGMENT) {
			Config.recent_needRefresh = true;
			mSearchFragment.isAddNoteReturn = true;
		} else if (requestCode == SEARCHFRAGMENT) {
			mSearchFragment.isAddNoteReturn = true;
			Config.recent_needRefresh = true;
		} else if (requestCode == CALENDARFRAGMENT) {
			Config.calendar_needRefresh = true;
			Config.recent_needRefresh = true;
		}else if(requestCode == REQUEST_VALIDATE){
			if(resultCode == RESULT_CANCELED){
				this.finish(); //20141202 验证失败则退出
			}
		}
	}

	public void addNewNote(View v) {
		if (mViewPager.getCurrentItem() == Main.CALENDARFRAGMENT) {
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
		case CALENDARFRAGMENT:

			mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);

			if (previousFragment == RECENTFRAGMENT) {

				try {
					scroller.setScrollDuration(1000);
					mScroller.set(mViewPager, scroller);

				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				}
			} else if (previousFragment == SEARCHFRAGMENT) {

				try {
					scroller.setScrollDuration(2000);
					mScroller.set(mViewPager, scroller);
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				}
			}
			this.mViewPager.setCurrentItem(CALENDARFRAGMENT);
			if (Config.calendar_needRefresh) {
				mCalendarFragment.onResume();
			}
			break;
		case RECENTFRAGMENT:

			mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);

			if (previousFragment == CALENDARFRAGMENT) {
				try {
					scroller.setScrollDuration(1000);
					mScroller.set(mViewPager, scroller);
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				}
			} else if (previousFragment == SEARCHFRAGMENT) {
				try {
					scroller.setScrollDuration(1000);
					mScroller.set(mViewPager, scroller);
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				}
			}
			this.mViewPager.setCurrentItem(RECENTFRAGMENT);
			if (Config.recent_needRefresh) {
				mRecentRecordsFragment.onResume();
			}

			break;
		case SEARCHFRAGMENT:
			//20141128 锁住抽屉防止打开键盘时的bug
			mDrawerLayout
					.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

			if (previousFragment == RECENTFRAGMENT) {
				try {
					scroller.setScrollDuration(1000);
					mScroller.set(mViewPager, scroller);

				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				}
			} else if (previousFragment == CALENDARFRAGMENT) {
				try {
					scroller.setScrollDuration(2000);
					mScroller.set(mViewPager, scroller);
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				}
			}
			this.mViewPager.setCurrentItem(SEARCHFRAGMENT);
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
			// return super.instantiateItem(container, position);
			return obj;
		}

		@Override
		public Fragment getItem(int position) {
			switch (position) {
			case SEARCHFRAGMENT:
				return mSearchFragment;
			case RECENTFRAGMENT:
				return mRecentRecordsFragment;
			case CALENDARFRAGMENT:
				return mCalendarFragment;
				// case SEARCHFRAGMENT_R:
				// return mSearchFragment;
			}
			return null;
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return 3;
		}
	}

	public class ViewPagerScroller extends Scroller {
		private int mScrollDuration = 1000;// speed 1000ms
		public ViewPagerScroller(Context context) {
			super(context);
		}

		@Override
		public void startScroll(int startX, int startY, int dx, int dy,
				int duration) {
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
		Logg.D(TAG+" onDestroy");
	}
	
	@Override
	protected void onRestart() {
		// TODO Auto-generated method stub
		super.onRestart();
		Logg.D(TAG+" onRestart");
	}
	

	@Override
	public void openDrawer() {
		// TODO Auto-generated method stub
		mDrawerLayout.openDrawer(Gravity.LEFT);
	}
}
