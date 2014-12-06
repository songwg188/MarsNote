package com.mars.note.fragment;

import java.util.Calendar;
import java.util.List;
import com.mars.note.Editor;
import com.mars.note.Config;
import com.mars.note.FragmentCallBack;
import com.mars.note.Main;
import com.mars.note.NoteApplication;
import com.mars.note.NoteSettings;
import com.mars.note.R;
import com.mars.note.database.NoteDataBaseManager;
import com.mars.note.database.NoteRecord;
import com.mars.note.utils.PictureHelper;
import com.mars.note.views.BounceViewPager;
import com.mars.note.views.DataAlterDialog;
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

public class CalendarFragment extends Fragment implements OnClickListener,
		ViewPager.OnPageChangeListener, CalendarItem.CallBack {
	private static final String TAG = "CalendarFragment";
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
	private View root;
	private FragmentCallBack mCallBack;

	@Override
	public void onAttach(Activity mActivity) {
		super.onAttach(mActivity);
		Log.d(TAG, "onAttach");
		this.mActivity = mActivity;
		setCallBack((FragmentCallBack)mActivity);
		date_title = mActivity.getResources()
				.getStringArray(R.array.date_title);
		time_title = mActivity.getResources()
				.getStringArray(R.array.time_title);
	}

	public void setCallBack(FragmentCallBack mCallBack) {
		this.mCallBack = mCallBack;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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
		mViewPagerAdapter = new CalendarPagerAdapter(CalendarFragment.this,
				sync);

		mBitmapCache = NoteApplication.getBitmapCache();
	}

	@Override
	public void onStart() {
		super.onStart();
		if (sync) {
			refreshSelectedDayData();
		}
		Log.d(TAG, "onStart");
	}

	private void initDBManager() {
		noteDBManager = NoteApplication.getDbManager();
	}

	private void initListener() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
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
		calendar_title.setText(calendar_year + dateTitle[0] + calendar_month
				+ dateTitle[1]);
		mProgressBar = (ProgressBar) v
				.findViewById(R.id.progress_loading_viewflow);
		// initViewFlow(v);
		initContentPager(v);
		root = v;
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
				index_of_data.setText((pos + 1) + " / "
						+ selected_day_datalist.size());
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
		Log.d(TAG, "onResume");
		if (sync && Config.calendar_needRefresh) {
			refreshSelectedDayData();
		}
	}

	private class refreshContentPagerTask extends
			AsyncTask<Integer, Integer, String> {
		@Override
		public void onPreExecute() {
			sync = false;
//			mContentPager.setVisibility(View.GONE);
//			index_of_data.setVisibility(View.GONE);
//			add_new_note.setVisibility(View.GONE);
//			emptyContent.setVisibility(View.GONE);
//			mProgressBar.setVisibility(View.VISIBLE);
			mViewPagerAdapter = new CalendarPagerAdapter(CalendarFragment.this,
					sync);
			mViewPager.setAdapter(mViewPagerAdapter);
			mViewPager.setCurrentItem(viewPagerIndex);
			calendar_title.setText(calendar_year + dateTitle[0]
					+ calendar_month + dateTitle[1]);
			mViewPager.setOnPageChangeListener(CalendarFragment.this);
		}

		@Override
		protected String doInBackground(Integer... arg0) {
			if (selected_day_datalist != null) {
				selected_day_datalist.clear(); // GC?
			}
			selected_day_datalist = noteDBManager.querySelectedRecords(
					calendar_selected_year, calendar_selected_month,
					calendar_selected_day);

//			System.gc();
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			if (isMainThreadAlive) {
				int dayCount = noteDBManager.getDayRecordCount(
						calendar_selected_year, calendar_selected_month,
						calendar_selected_day);
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
						mContentAdapter = new ContentAdapter(mActivity,
								selected_day_datalist);
						mContentPager.setAdapter(mContentAdapter);
						mContentPager.setCurrentItem(0);
						index_of_data.setText("1 / "
								+ selected_day_datalist.size());
						index_of_data.setVisibility(View.VISIBLE);
					}
//					mProgressBar.setVisibility(View.GONE);
					Config.calendar_needRefresh = false;
					sync = true;
					mViewPagerAdapter = new CalendarPagerAdapter(
							CalendarFragment.this, sync);
					mViewPager.setAdapter(mViewPagerAdapter);
					mViewPager.setCurrentItem(viewPagerIndex);
					calendar_title.setText(calendar_year + dateTitle[0]
							+ calendar_month + dateTitle[1]);
					mViewPager.setOnPageChangeListener(CalendarFragment.this);
				}
			}
		}
	}

	//刷新内容版面
	public void refreshSelectedDayData() {
		new refreshContentPagerTask().execute();
	}

	private class ListItemImg {
		public int position;
		public Bitmap bm;
	}

	@Override
	public void onClick(View view) {
		if (view.getId() == R.id.calendar_title) {
			final DataAlterDialog mChangeDateDialog = new DataAlterDialog(
					this.mActivity, null, calendar_selected_year,
					calendar_selected_month - 1, 1);
			final DatePicker mDatePicker = mChangeDateDialog.getDatePicker();
			mChangeDateDialog.setBtnListener(
					new android.view.View.OnClickListener() {
						@Override
						public void onClick(View v) {
							Calendar cal = Calendar.getInstance();
							calendar_year = mDatePicker.getYear();
							calendar_month = mDatePicker.getMonth() + 1;
							viewPagerIndex = (calendar_year - 1900) * 12
									+ calendar_month - 1;
							mViewPager.setCurrentItem(viewPagerIndex);
							calendar_title.setText(calendar_year + dateTitle[0]
									+ calendar_month + dateTitle[1]);
							mChangeDateDialog.dismiss();
						}
					}).show();
		} else if (view.getId() == R.id.note_settings) {
			Intent intent = new Intent(mActivity, NoteSettings.class);
			mActivity.startActivity(intent);
			if (pop.isShowing()) {
				pop.dismiss();
			}
		} 
//		else if (view.getId() == R.id.search) {
//			mCallBack.switchFragment(Main.SEARCHFRAGMENT);
//			if (pop.isShowing()) {
//				pop.dismiss();
//			}
//		}
		else {
			int position = ((Integer) view.getTag()).intValue();
			NoteRecord nr = selected_day_datalist.get(position);
			String id = nr.id;
			Intent editNote = new Intent(mActivity, Editor.class);
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
		NoteRecord deleteItem = this.selected_day_datalist
				.get(single_delete_position);
		noteDBManager.deleteRecord(deleteItem.id);
		Config.calendar_needRefresh = true;
		this.onResume();
	}

	public void addNewNote(View v) {
		AlertDialog.Builder myDialog = new AlertDialog.Builder(mActivity);
		myDialog.setMessage(R.string.add_new_note_title);
		myDialog.setPositiveButton(mActivity.getString(R.string.yes),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						Intent editNote = new Intent(mActivity, Editor.class);
						int year = calendar_selected_year;
						int month = calendar_selected_month;
						int day = calendar_selected_day;
						editNote.putExtra("add_from_calendar", true);
						editNote.putExtra("year", year);
						editNote.putExtra("month", month);
						editNote.putExtra("day", day);
						mActivity.startActivityForResult(editNote,
								Main.CALENDARFRAGMENT);
					}
				});
		myDialog.setNegativeButton(mActivity.getString(R.string.no), null);
		myDialog.show();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "onDestroy");
	}

	private class CalendarPagerAdapter extends FragmentStatePagerAdapter {
		private static final int YEAR_MAX = 2100;
		private static final int YEAR_MIN = 1900;
		private boolean sync; //同步刷新动作，在内容刷新时，日历不能点击

		public CalendarPagerAdapter(FragmentManager fm) {
			super(fm);
			// TODO Auto-generated constructor stub
		}

		public CalendarPagerAdapter(android.support.v4.app.Fragment fragment,
				boolean b) {
			super(fragment.getChildFragmentManager());
			sync = b;
		}

		@Override
		public Fragment getItem(int pos) {
			CalendarItem ci = new CalendarItem();
			Bundle bundle = new Bundle();
			bundle.putInt("pos", pos);
			bundle.putInt("calendar_selected_year", calendar_selected_year);
			bundle.putInt("calendar_selected_month", calendar_selected_month);
			bundle.putInt("calendar_selected_day", calendar_selected_day);
			bundle.putBoolean("sync", sync);
			ci.setArguments(bundle);//不要用构造方法传递参数 ，用getArguments，见onCreate
			//这里用Fragment实现回调，因为嵌套的Fragment生命周期时小于本Fragment的
			ci.setCalendarCallBack(CalendarFragment.this);
			return ci;
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
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
		calendar_title.setText(calendar_year + dateTitle[0] + calendar_month
				+ dateTitle[1]);
		viewPagerIndex = pos;
	}

	@Override
	public void onPageSelected(int pos) {
	}

	/*
	 * 日历View点击的回调事件，通知Fragment点击了哪个时期
	 * @see com.mars.note.fragment.CalendarItem.CallBack#onChangeSelectedDate(int, int, int)
	 */
	@Override
	public void onChangeSelectedDate(int year, int month, int day) {
		//如果点击的日期一样，不做处理
		if (calendar_selected_year == year && calendar_selected_month == month
				&& calendar_selected_day == day) {
			return;
		}
		//点击的日期不一样，需要重新赋值
		this.calendar_selected_year = year;
		this.calendar_selected_month = month;
		this.calendar_selected_day = day;
		if (calendar_year != calendar_selected_year
				|| calendar_selected_month != calendar_month) {
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
			mInflater = (LayoutInflater) ctx
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
			TextView content = (TextView) convertView
					.findViewById(R.id.content);
			date.setTextColor(Color.DKGRAY);
			time.setTextColor(Color.DKGRAY);
			title.setTextColor(Color.DKGRAY);
			content.setTextColor(Color.DKGRAY);
			ImageView img = (ImageView) convertView
					.findViewById(R.id.note_listitem_img);
			img.setVisibility(View.INVISIBLE);
			String path = nr.imgpath;
			if (path != null && (!path.equals("null")) && (!"".equals(path))) {
				img.setVisibility(View.VISIBLE);
				if (mBitmapCache.get(path) == null) {
					new LoadSingleImageTask(position, path, img).execute();
				} else {
					if (mBitmapCache.get(path) != null) {
						img.setImageBitmap(mBitmapCache.get(path));
						img.setVisibility(View.VISIBLE);
					} else {
					}
				}
			} else {
				img.setVisibility(View.GONE);
			}
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(Long.parseLong(nr.time));
			String nowDate = calendar.get(Calendar.YEAR) + date_title[0]
					+ (calendar.get(Calendar.MONTH) + 1) + date_title[1]
					+ calendar.get(Calendar.DAY_OF_MONTH) + date_title[2];
			String nowTime = calendar.get(Calendar.HOUR_OF_DAY) + time_title[0]
					+ calendar.get(Calendar.MINUTE) + time_title[1];
			String dayOfWeekText = getdayOfWeek(calendar
					.get(Calendar.DAY_OF_WEEK));
			date.setText(nowDate);
			time.setText(dayOfWeekText + "  " + nowTime);
			title.setText(nr.title);
			content.setText(nr.content);
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
		Bitmap bm;
		ImageView imageView;
		ListItemImg item;

		LoadSingleImageTask(int position, String path, ImageView v) {
			this.path = path;
			this.imageView = v;
			item = new ListItemImg();
			item.position = position;
		}

		@Override
		protected String doInBackground(String... arg0) {
			if (path != null && (!path.equals("null")) && (!"".equals(path))) {
				bm = PictureHelper.getCropImage(path, 400, true, 100,
						mActivity, 7, true);
				item.bm = bm;
			} else {
				bm = null;
				item.bm = bm;
			}
			mBitmapCache.put(path, item.bm);
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			imageView.setVisibility(View.VISIBLE);
			imageView.setImageBitmap(bm);
		}
	}

	@Override
	public void onStop() {
		Log.d(TAG, "onStop");
		super.onStop();
	}

	@Override
	public void onPause() {
		Log.d(TAG, "onPause");
		super.onPause();
	}

	@Override
	public void onDestroyView() {
		Log.d(TAG, "onDestroyView");
		super.onDestroyView();
	}
}
