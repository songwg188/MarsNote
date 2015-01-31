package com.mars.note.fragment;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import com.mars.note.R;
import com.mars.note.api.Config;
import com.mars.note.api.FragmentCallBack;
import com.mars.note.api.FragmentFactory;
import com.mars.note.api.ImageSpanInfo;
import com.mars.note.api.AlertDialogFactory;
import com.mars.note.app.BackUpActivity;
import com.mars.note.app.EditorActivity;
import com.mars.note.app.MarsNoteActivity;
import com.mars.note.app.NoteApplication;
import com.mars.note.app.NoteSettingsActivity;
import com.mars.note.database.NoteDataBaseManager;
import com.mars.note.database.NoteRecord;
import com.mars.note.fragment2.CalendarItemFragment;
import com.mars.note.utils.FileHelper;
import com.mars.note.utils.Logg;
import com.mars.note.utils.PictureHelper;
import com.mars.note.utils.Util;
import com.mars.note.views.BounceViewPager;
import com.mars.note.views.DateAlterDialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.util.LruCache;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * 显示日历
 * 
 * @author mars
 * 
 */
public class CalendarFragment extends BaseFragment implements OnClickListener, ViewPager.OnPageChangeListener, CalendarItemFragment.CallBack {
	private static final String TAG = "CalendarFragment";
	private static final boolean DEBUG = false;
	private Activity mActivity;
	private NoteDataBaseManager noteDBManager;
	private TextView index_of_data;
	private TextView calendar_title;
	private TextView emptyContent;
	private ImageButton add_new_note;
	private BounceViewPager mViewPager;
	private FragmentStatePagerAdapter mViewPagerAdapter;
	private String[] dateTitle;
	private BounceViewPager mContentPager;
	private PagerAdapter mContentAdapter;
	private List<NoteRecord> selected_day_datalist;
	private static LruCache<String, Bitmap> mBitmapCache;
	private ProgressBar mProgressBar;
	private int single_delete_position;
	private String[] date_title;
	private String[] time_title;
	private PopupWindow pop;
	private View popView;
	private int calendar_year;
	private int calendar_month;
	private int calendar_selected_year;
	private int calendar_selected_month;
	private int calendar_selected_day;
	private int viewPagerIndex;
	private boolean sync = true;
	public boolean isMainThreadAlive = true;
	private View rootView;
	private ConcurrentHashMap<String, Boolean> mConcurentMap;
	private ReentrantLock mLock;// 同步锁

	public static BaseFragment getInstance() {
		return new CalendarFragment();
	}

	@Override
	public void onAttach(Activity mActivity) {
		super.onAttach(mActivity);
		if (DEBUG)
			Log.d(TAG, "onAttach");
		this.mActivity = mActivity;
		date_title = mActivity.getResources().getStringArray(R.array.date_title);
		time_title = mActivity.getResources().getStringArray(R.array.time_title);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (DEBUG)
			Log.d(TAG, "onCreate");
		initDBManager();
		initListener();
		dateTitle = mActivity.getResources().getStringArray(R.array.date_title);
		Calendar cal = Calendar.getInstance();
		this.calendar_year = cal.get(Calendar.YEAR);
		this.calendar_month = cal.get(Calendar.MONTH) + 1;
		calendar_selected_year = calendar_year;
		calendar_selected_month = calendar_month;
		calendar_selected_day = cal.get(Calendar.DAY_OF_MONTH);
		viewPagerIndex = (calendar_year - 1900) * 12 + calendar_month - 1;
		mViewPagerAdapter = new CalendarPagerAdapter(CalendarFragment.this, sync);

		mBitmapCache = NoteApplication.getBitmapCache();
		mConcurentMap = NoteApplication.getmConcurentMap();// 保存线程工作状态
		mLock = NoteApplication.getmLock();// 20141212 公平锁
	}

	@Override
	public void onStart() {
		super.onStart();
		if (sync) {
			refreshSelectedDayData();
		}
		if (DEBUG)
			Log.d(TAG, "onStart");
	}

	private void initDBManager() {
		noteDBManager = NoteApplication.getDbManager();
	}

	private void initListener() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_calendar, container, false);
		mViewPager = (BounceViewPager) v.findViewById(R.id.calendar_pager);
		mViewPager.setAdapter(mViewPagerAdapter);
		mViewPager.setCurrentItem(viewPagerIndex);
		mViewPager.setOnPageChangeListener(this);
		mViewPager.setOffscreenPageLimit(3); // cache 2
		index_of_data = (TextView) v.findViewById(R.id.index_of_data);
		emptyContent = (TextView) v.findViewById(R.id.content_empty);
		add_new_note = (ImageButton) v.findViewById(R.id.add_new_note);
		calendar_title = (TextView) v.findViewById(R.id.calendar_title);
		calendar_title.setOnClickListener(this);
		calendar_title.setText(calendar_year + dateTitle[0] + calendar_month + dateTitle[1]);
		mProgressBar = (ProgressBar) v.findViewById(R.id.progress_loading_viewflow);
		// initViewFlow(v);
		initContentPager(v);
		rootView = v;
		return v;
	}

	private void initContentPager(View v) {
		mContentPager = (BounceViewPager) v.findViewById(R.id.content_pager);
		mContentPager.setOnPageChangeListener(new OnPageChangeListener() {
			@Override
			public void onPageScrollStateChanged(int arg0) {
				Config.contentPagerScrollState = arg0;
			}

			@Override
			public void onPageScrolled(int pos, float arg1, int arg2) {
				index_of_data.setText((pos + 1) + " / " + selected_day_datalist.size());
			}

			@Override
			public void onPageSelected(int arg0) {
				mContentPager.setCurrentIndex(arg0);
			}
		});
		mContentPager.setSpringBack(true);
		mActivity.registerForContextMenu(mContentPager);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (DEBUG)
			Log.d(TAG, "onResume");
		if (sync && Config.calendar_needRefresh) {
			refreshSelectedDayData();
		}

	}

	// 刷新内容版面
	public void refreshSelectedDayData() {
		// new refreshContentPagerTask().execute();
		sync = false;
		mViewPagerAdapter = new CalendarPagerAdapter(CalendarFragment.this, sync);
		mViewPager.setAdapter(mViewPagerAdapter);
		mViewPager.setCurrentItem(viewPagerIndex);
		calendar_title.setText(calendar_year + dateTitle[0] + calendar_month + dateTitle[1]);
		mViewPager.setOnPageChangeListener(CalendarFragment.this);

		if (selected_day_datalist != null) {
			selected_day_datalist.clear();
		}
		selected_day_datalist = noteDBManager.querySelectedRecords(calendar_selected_year, calendar_selected_month, calendar_selected_day);

		if (isMainThreadAlive) {
			int dayCount = noteDBManager.getDayRecordCount(calendar_selected_year, calendar_selected_month, calendar_selected_day);
			if (dayCount != selected_day_datalist.size()) {
				sync = true;
				onResume();
			} else {
				if (selected_day_datalist.size() == 0) {
					emptyContent.setVisibility(View.VISIBLE);
					add_new_note.setVisibility(View.VISIBLE);
					mContentPager.setVisibility(View.GONE);
					index_of_data.setVisibility(View.GONE);
				} else {
					emptyContent.setVisibility(View.GONE);
					add_new_note.setVisibility(View.GONE);
					mContentPager.setVisibility(View.VISIBLE);
					mContentAdapter = new ContentAdapter(mActivity, selected_day_datalist);
					mContentPager.setAdapter(mContentAdapter);
					mContentPager.setCurrentItem(0);
					index_of_data.setText("1 / " + selected_day_datalist.size());
					index_of_data.setVisibility(View.VISIBLE);
				}
				// mProgressBar.setVisibility(View.GONE);
				Config.calendar_needRefresh = false;
				sync = true;
				mViewPagerAdapter = new CalendarPagerAdapter(CalendarFragment.this, sync);
				mViewPager.setAdapter(mViewPagerAdapter);
				mViewPager.setCurrentItem(viewPagerIndex);
				calendar_title.setText(calendar_year + dateTitle[0] + calendar_month + dateTitle[1]);
				mViewPager.setOnPageChangeListener(CalendarFragment.this);
			}
		}
	}

	private class ListItemImg {
		public int position;
		public Bitmap bm;
	}

	@Override
	public void onClick(View view) {
		if (view.getId() == R.id.calendar_title) {
			final DateAlterDialog mChangeDateDialog = new DateAlterDialog(this.mActivity, null, calendar_selected_year, calendar_selected_month - 1, 1);
			final DatePicker mDatePicker = mChangeDateDialog.getDatePicker();
			mChangeDateDialog.setBtnListener(new android.view.View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Calendar cal = Calendar.getInstance();
					calendar_year = mDatePicker.getYear();
					calendar_month = mDatePicker.getMonth() + 1;
					viewPagerIndex = (calendar_year - 1900) * 12 + calendar_month - 1;
					mViewPager.setCurrentItem(viewPagerIndex);
					calendar_title.setText(calendar_year + dateTitle[0] + calendar_month + dateTitle[1]);
					mChangeDateDialog.dismiss();
				}
			}).show();
		} else if (view.getId() == R.id.note_settings) {
			Intent intent = new Intent(mActivity, NoteSettingsActivity.class);
			mActivity.startActivity(intent);
			if (pop.isShowing()) {
				pop.dismiss();
			}
		} else {
			int position = ((Integer) view.getTag()).intValue();
			NoteRecord nr = selected_day_datalist.get(position);
			String id = nr.id;
			Intent editNote = new Intent(mActivity, EditorActivity.class);
			editNote.putExtra("note_id", id);
			mActivity.startActivity(editNote);
		}
	}

	private OnLongClickListener mOnLongClickListener = new OnLongClickListener() {
		@Override
		public boolean onLongClick(View v) {
			single_delete_position = ((Integer) v.getTag()).intValue();
			return false;
		}
	};

	public void deleteSingleRecord() {
		NoteRecord deleteItem = this.selected_day_datalist.get(single_delete_position);
		noteDBManager.deleteRecord(deleteItem);
		Config.calendar_needRefresh = true;
		this.onResume();
	}

	public void addNewNote(View v) {
		// AlertDialog.Builder myDialog = new AlertDialog.Builder(mActivity);
		// myDialog.setMessage(R.string.add_new_note_title);
		// myDialog.setPositiveButton(mActivity.getString(R.string.yes), new
		// DialogInterface.OnClickListener() {
		// @Override
		// public void onClick(DialogInterface arg0, int arg1) {
		// Intent editNote = new Intent(mActivity, EditorActivity.class);
		// int year = calendar_selected_year;
		// int month = calendar_selected_month;
		// int day = calendar_selected_day;
		// editNote.putExtra("add_from_calendar", true);
		// editNote.putExtra("year", year);
		// editNote.putExtra("month", month);
		// editNote.putExtra("day", day);
		// mActivity.startActivityForResult(editNote,
		// FragmentFactory.CALENDARFRAGMENT);
		// }
		// });
		// myDialog.setNegativeButton(mActivity.getString(R.string.no), null);
		// myDialog.show();

		AlertDialogFactory.showAlertDialog(mActivity, rootView, mActivity.getString(R.string.add_new_note_title),
				new AlertDialogFactory.DialogPositiveListner() {

					@Override
					public void onClick(View arg0) {
						super.onClick(arg0);
						Intent editNote = new Intent(mActivity, EditorActivity.class);
						int year = calendar_selected_year;
						int month = calendar_selected_month;
						int day = calendar_selected_day;
						editNote.putExtra("add_from_calendar", true);
						editNote.putExtra("year", year);
						editNote.putExtra("month", month);
						editNote.putExtra("day", day);
						mActivity.startActivityForResult(editNote, FragmentFactory.CALENDARFRAGMENT);
					}
				});
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (DEBUG)
			Log.d(TAG, "onDestroy");
	}

	private class CalendarPagerAdapter extends FragmentStatePagerAdapter {
		private static final int YEAR_MAX = 2100;
		private static final int YEAR_MIN = 1900;
		private boolean sync; // 同步刷新动作，在内容刷新时，日历不能点击

		public CalendarPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		public CalendarPagerAdapter(android.support.v4.app.Fragment fragment, boolean b) {
			super(fragment.getChildFragmentManager());
			sync = b;
		}

		@Override
		public Fragment getItem(int pos) {
			CalendarItemFragment ci = (CalendarItemFragment) FragmentFactory.newCalendarItem();
			Bundle bundle = new Bundle();
			bundle.putInt("pos", pos);
			bundle.putInt("calendar_selected_year", calendar_selected_year);
			bundle.putInt("calendar_selected_month", calendar_selected_month);
			bundle.putInt("calendar_selected_day", calendar_selected_day);
			bundle.putBoolean("sync", sync);
			// 不要用构造方法传递参数 ，用getArguments，见onCreate
			// 这里用Fragment实现回调，因为嵌套的Fragment生命周期时小于本Fragment的
			ci.setArguments(bundle);
			ci.setCallBack(CalendarFragment.this);
			return ci;
		}

		@Override
		public int getCount() {
			return (YEAR_MAX - YEAR_MIN) * 12;
		}
	}

	@Override
	public void onPageScrollStateChanged(int pos) {
	}

	@Override
	public void onPageScrolled(int pos, float arg1, int arg2) {
		calendar_year = 1900 + (pos / 12);
		calendar_month = pos % 12 + 1;
		calendar_title.setText(calendar_year + dateTitle[0] + calendar_month + dateTitle[1]);
		viewPagerIndex = pos;
	}

	@Override
	public void onPageSelected(int pos) {
	}

	/*
	 * 日历View点击的回调事件，通知Fragment点击了哪个时期
	 * 
	 * @see
	 * com.mars.note.fragment.CalendarItem.CallBack#onChangeSelectedDate(int,
	 * int, int)
	 */
	@Override
	public void onChangeSelectedDate(int year, int month, int day) {
		// 如果点击的日期一样，不做处理
		if (calendar_selected_year == year && calendar_selected_month == month && calendar_selected_day == day) {
			return;
		}
		// 点击的日期不一样，需要重新赋值
		this.calendar_selected_year = year;
		this.calendar_selected_month = month;
		this.calendar_selected_day = day;
		if (calendar_year != calendar_selected_year || calendar_selected_month != calendar_month) {
			calendar_year = calendar_selected_year;
			calendar_month = calendar_selected_month;
			viewPagerIndex = (calendar_year - 1900) * 12 + calendar_month - 1;
		}

		if (sync) {
			refreshSelectedDayData();
		}
	}

	public class ContentAdapter extends PagerAdapter {
		List<NoteRecord> datalist;
		private LayoutInflater mInflater = null;

		public ContentAdapter(Context ctx, List<NoteRecord> list) {
			datalist = list;
			mInflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return datalist.size();
		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1) {
			// TODO Auto-generated method stub
			return arg0 == arg1;
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object o) {
			container.removeView((View) o);
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			View convertView = mInflater.inflate(R.layout.viewflow_item, null);
			convertView.setTag(Integer.valueOf(position));
			convertView.setOnClickListener(CalendarFragment.this);
			convertView.setOnLongClickListener(mOnLongClickListener);
			NoteRecord nr = datalist.get(position);
			TextView date = (TextView) convertView.findViewById(R.id.date);
			TextView time = (TextView) convertView.findViewById(R.id.time);
			TextView title = (TextView) convertView.findViewById(R.id.title);
			TextView content = (TextView) convertView.findViewById(R.id.content);
			date.setTextColor(Color.DKGRAY);
			time.setTextColor(Color.DKGRAY);
			title.setTextColor(Color.DKGRAY);
			content.setTextColor(Color.DKGRAY);
			ImageView img = (ImageView) convertView.findViewById(R.id.note_listitem_img);
			img.setVisibility(View.INVISIBLE);
			String path = nr.imgpath;
			if (path != null && (!path.equals("null")) && (!"".equals(path))) {
				img.setVisibility(View.VISIBLE);
				if (mBitmapCache.get(path) == null) {
					mLock.lock();
					if (mConcurentMap.get(path) == null)
						mConcurentMap.put(path, false);
					mLock.unlock();
					if (DEBUG)
						Logg.I("instantiateItem pos = " + position);
					new LoadSingleImageTask(position, path, img, nr).execute();
				} else {
					if (mBitmapCache.get(path) != null) {
						img.setImageBitmap(mBitmapCache.get(path));
						img.setVisibility(View.VISIBLE);
					}
				}
			} else {
				img.setVisibility(View.GONE);
			}
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(Long.parseLong(nr.time));
			String nowDate = calendar.get(Calendar.YEAR) + date_title[0] + (calendar.get(Calendar.MONTH) + 1) + date_title[1]
					+ calendar.get(Calendar.DAY_OF_MONTH) + date_title[2];
			String nowTime = calendar.get(Calendar.HOUR_OF_DAY) + time_title[0] + calendar.get(Calendar.MINUTE) + time_title[1];
			String dayOfWeekText = getdayOfWeek(calendar.get(Calendar.DAY_OF_WEEK));
			date.setText(nowDate);
			time.setText(dayOfWeekText + "  " + nowTime);
			title.setText(nr.title);

			// 20141215 过滤图片字符串 start
			String contentStr = nr.content;
			byte[] data = nr.imageSpanInfos;
			if (data != null) {
				ArrayList<ImageSpanInfo> imageSpanInfoList = Util.getImageSpanInfoListFromBytes(data);
				contentStr = Util.filterContent(mActivity, contentStr, imageSpanInfoList);
			}
			content.setText(contentStr);
			// 20141215 过滤图片字符串 end
			container.addView(convertView);
			return convertView;
		}

		private String getdayOfWeek(int dayOfWeek) {
			switch (dayOfWeek) {
			case 1:
				return getString(R.string.sunday);
			case 2:
				return getString(R.string.monday);
			case 3:
				return getString(R.string.tuesday);
			case 4:
				return getString(R.string.wednesday);
			case 5:
				return getString(R.string.thursday);
			case 6:
				return getString(R.string.friday);
			case 7:
				return getString(R.string.saturday);
			default:
				return null;
			}
		}
	}

	private class LoadSingleImageTask extends AsyncTask<String, String, String> {
		String path;
		ImageView imageView;
		ListItemImg item;
		NoteRecord nr;

		LoadSingleImageTask(int position, String path, ImageView v, NoteRecord nr) {
			this.path = path;
			this.imageView = v;
			item = new ListItemImg();
			item.position = position;
			this.nr = nr;
		}

		@Override
		protected String doInBackground(String... arg0) {
			if (mBitmapCache.get(path) == null) {
				mLock.lock();
				if (mConcurentMap.get(path) == null)
					mConcurentMap.put(path, false);
				if (mConcurentMap.get(path) == false) {
					if (DEBUG)
						Logg.D("mConcurentMap.get(path) = " + mConcurentMap.get(path));
					mConcurentMap.put(path, true);
					mLock.unlock();
					if (path != null && (!path.equals("null")) && (!"".equals(path))) {
						if (Config.DB_SAVE_MODE) {
							// 从数据库读图
							item.bm = noteDBManager.getCroppedImage(path);
							if (item.bm == null)
								throw new NullPointerException("bm null error");
						} else {
							item.bm = PictureHelper.getCropImage(path, getResources().getDimension(R.dimen.listview_image_width),
									getResources().getDimension(R.dimen.listview_image_height), true, 100, mActivity, 7, true);
						}
						if (DEBUG)
							Logg.I("Calendar " + path + " is load new");
					} else {
						item.bm = null;
					}
					if (item.bm != null) {
						mBitmapCache.put(path, item.bm);
					} else {
						// 20141216 预览图已经删除 需要更新数据库 并刷新start
						if (DEBUG)
							Logg.I("Calendar " + " img not exsited");
						noteDBManager.deleteImagePath(nr);
						mConcurentMap.put(path, false);
						// 20141216 预览图已经删除 需要更新数据库 并刷新end
					}
					mConcurentMap.put(path, false);
				} else {
					mLock.unlock();
					while (true) {
						if (mConcurentMap.get(path) == false) {
							break;
						}
					}
					if (DEBUG)
						Logg.I("Calendar " + "other go out cycle");
				}
			} else {
				if (DEBUG)
					Logg.I("Calendar " + path + " is load from cache");
			}
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			if (mBitmapCache.get(path) != null) {
				imageView.setVisibility(View.VISIBLE);
				imageView.setImageBitmap(mBitmapCache.get(path));
			} else {
				if (DEBUG)
					Logg.I("Calendar " + " img not exsited");
			}
		}
	}

	@Override
	public void onStop() {
		if (DEBUG)
			Log.d(TAG, "onStop");
		super.onStop();
	}

	@Override
	public void onPause() {
		if (DEBUG)
			Log.d(TAG, "onPause");
		super.onPause();
	}

	@Override
	public void onDestroyView() {
		if (DEBUG)
			Log.d(TAG, "onDestroyView");
		super.onDestroyView();
	}
}
