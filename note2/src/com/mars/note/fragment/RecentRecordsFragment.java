package com.mars.note.fragment;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.mars.note.Editor;
import com.mars.note.Config;
import com.mars.note.FragmentCallBack;
import com.mars.note.Logg;
import com.mars.note.Main;
import com.mars.note.NoteApplication;
import com.mars.note.NoteSettings;
import com.mars.note.R;
import com.mars.note.database.NoteDataBaseManager;
import com.mars.note.database.NoteRecord;
import com.mars.note.fragment.GridViewPaperItem.CallBack;
import com.mars.note.utils.AnimationHelper;
import com.mars.note.utils.PictureHelper;
import com.mars.note.views.BounceListView;
import com.mars.note.views.JazzyViewPager;
import com.mars.note.views.JazzyViewPager.TransitionEffect;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.util.LruCache;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.ProgressBar;
import android.widget.Scroller;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class RecentRecordsFragment extends Fragment implements
		OnScrollListener, CallBack {
	public static final String TAG = "RecentRecordsFragment";
	private Activity mActivity;
//	private ViewGroup titleBar;
	private ImageButton titlebar_add_note_btn; // 添加新笔记按钮
	private ImageButton titlebar_overflow_options; // 显示菜单按钮
	private ImageButton titlebar_batch_delete_imgbtn; // 批量删除按钮
	private ImageButton bottom_batch_delete_btn; // 批量删除ui下，底部删除按钮
	private View bottom_layout;
	private CheckBox select_all_checkbox; // 选择全部复选框
	private BounceListView listView_myNote; // 20141124 增加阻尼效果
	private TextView textview_empty; // 空文本
	private ProgressBar mProgressBar;
	private JazzyViewPager mGridPager;
	
	// listener
	private OnCheckedChangeListener mNoteCheckBoxListener;
	private OnClickListener mOnClickListener;
	private NoteListAdapter noteAdapter;
	// db manager
	private NoteDataBaseManager noteDBManager;
	static List<NoteRecord> list_note_data; // 数据源
	private ArrayList<ListItemCheckBoxStatus> item_checkbox_status;
	private static LruCache<String, Bitmap> mBitmapCache;
	private int single_delete_position;
	public static Boolean isDeleteUIShown = false;
	private int currentPage = 1;
	private int records_count;
	private boolean isLoadMoreModel = false;
	private ProgressDialog mExecutingDialog;
	private boolean isFirstLoaded = true;
	private static final int queryPageNum = 20;
	private static final int MaxPageNum = 50;
	
	private PopupWindow pop;
	private View popView, root;
	private String[] date_title;
	private String[] time_title;
	private FragmentCallBack mCallBack;
	private boolean flag = true;
	private boolean delayToRefresh = false;
	public static final int thread_num = 12;
	private static ExecutorService executors;
	
	private FragmentStatePagerAdapter mGridPagerAdapter;
	private ArrayList<com.mars.note.fragment.GridViewPaperItemForBatchDelete> mGridViewBatchDeleteCache;
	private int screenHeight, screenWidth;
	private float DPI;
	private boolean firstResume = true; // 20111201 bug 第一次调整垃圾桶setY 7 导致下移

	private void setCallBack(FragmentCallBack mCallBack) {
		this.mCallBack = mCallBack;
	}

	@Override
	public void onAttach(Activity mActivity) {
		super.onAttach(mActivity);
		Logg.D(TAG + " onAttach");
		this.mActivity = mActivity;
		setCallBack((FragmentCallBack) mActivity);
		date_title = mActivity.getResources()
				.getStringArray(R.array.date_title);
		time_title = mActivity.getResources()
				.getStringArray(R.array.time_title);
	}

	@Override
	public void onDetach() {
		super.onDetach();
		Logg.D(TAG + " onDetach");
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Logg.D(TAG + " onCreate");

		initDBManager();
		initListener();
		initExecutingDialog();
		mBitmapCache = NoteApplication.getBitmapCache();
		executors = NoteApplication.getExecutors();
		if (executors == null) {
			executors = Executors
					.newFixedThreadPool(RecentRecordsFragment.thread_num);
			NoteApplication.setExecutors(executors);
		}

		DisplayMetrics dm = new DisplayMetrics();
		getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
		screenHeight = dm.heightPixels;
		screenWidth = dm.widthPixels;
		DPI = dm.density;

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Logg.D(TAG + " onCreateView");
		View v = inflater.inflate(R.layout.fragment_recent_records, container,
				false);
		mProgressBar = (ProgressBar) v.findViewById(R.id.loading_progress);
		initTitleBarView(v);
		initAutoRefreshListView(v);
		initGridPaper(v);
		initEmptyView(v);
		bottom_layout = v.findViewById(R.id.bottom);
		bottom_batch_delete_btn = (ImageButton) v
				.findViewById(R.id.bottom_batch_delete_btn);
		bindListener();

		root = v;

		return v;
	}

	private void initGridPaper(View v) {
		mGridPager = (JazzyViewPager) v.findViewById(R.id.note_grid_paper);
		mGridPager.setTouchable(true);
//		mGridPager.setSpringBack(true);
		mGridPager.setTransitionEffect(TransitionEffect.RotateDown);
		// mGridPager.setScrollable(true);
		Field mScroller = null;
		ViewPagerScroller scroller = null;
		try {
			mScroller = ViewPager.class.getDeclaredField("mScroller");
			mScroller.setAccessible(true);
			scroller = new ViewPagerScroller(mGridPager.getContext());
			mScroller.set(mGridPager, scroller);
		} catch (NoSuchFieldException e) {
		} catch (IllegalArgumentException e) {
		} catch (IllegalAccessException e) {
		}
		scroller.setScrollDuration(500);
	}

	private void bindListener() {
		titlebar_overflow_options.setOnClickListener(mOnClickListener);
		titlebar_add_note_btn.setOnClickListener(mOnClickListener);
		titlebar_batch_delete_imgbtn.setOnClickListener(mOnClickListener);
		bottom_batch_delete_btn.setOnClickListener(mOnClickListener);
		select_all_checkbox.setOnCheckedChangeListener(mNoteCheckBoxListener);
	}

	private void initDBManager() {
		noteDBManager = NoteApplication.getDbManager();
	}

	private void initExecutingDialog() {
		mExecutingDialog = new ProgressDialog(mActivity);
		mExecutingDialog
				.setMessage(getString(R.string.dialog_executing_message));
		mExecutingDialog.setCancelable(false);
	}

	private void initTitleBarView(View v) {
//		titleBar = (ViewGroup) v.findViewById(R.id.titlebar);
		titlebar_add_note_btn = (ImageButton) v
				.findViewById(R.id.titlebar_add_note_btn);
		titlebar_batch_delete_imgbtn = (ImageButton) v
				.findViewById(R.id.titlebar_batch_delete_btn);

		select_all_checkbox = (CheckBox) v
				.findViewById(R.id.select_all_checkBox);
		titlebar_overflow_options = (ImageButton) v
				.findViewById(R.id.titlebar_overflow_options);
		popView = LayoutInflater.from(mActivity).inflate(
				R.layout.popup_window_settings, null);
		ImageButton settings = (ImageButton) popView
				.findViewById(R.id.note_settings);
		settings.setOnClickListener(mOnClickListener);
		ImageButton calendar_fragment = (ImageButton) popView
				.findViewById(R.id.calendar_fragment);
		calendar_fragment.setOnClickListener(mOnClickListener);

		ImageButton search = (ImageButton) popView.findViewById(R.id.search);
		search.setOnClickListener(mOnClickListener);

		pop = new PopupWindow(popView, LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT, false);

		pop.setBackgroundDrawable(new BitmapDrawable());
		pop.setOutsideTouchable(true);
		pop.setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss() {
				AnimationHelper.runRotateAnim(titlebar_overflow_options, 180F,
						360F, 150);
			}
		});
		pop.setFocusable(true);
		pop.update();
	}

	private void initListener() {
		mNoteCheckBoxListener = new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (buttonView.getId() == R.id.select_all_checkBox) {
					switch (Config.current_theme) {
					case 1:
						if (isChecked) {
							Iterator<ListItemCheckBoxStatus> mIterator = item_checkbox_status
									.iterator();
							while (mIterator.hasNext()) {
								ListItemCheckBoxStatus itemBox = mIterator
										.next();
								itemBox.onChecked = true;
							}
						} else {
							Iterator<ListItemCheckBoxStatus> mIterator = item_checkbox_status
									.iterator();
							while (mIterator.hasNext()) {
								ListItemCheckBoxStatus itemBox = mIterator
										.next();
								itemBox.onChecked = false;
							}
						}
						break;
					case 2:
						if (isChecked) {
							if (mGridViewBatchDeleteCache != null) {
								Iterator<GridViewPaperItemForBatchDelete> mIterator = mGridViewBatchDeleteCache
										.iterator();
								while (mIterator.hasNext()) {
									GridViewPaperItemForBatchDelete item = mIterator
											.next();
									item.checked = true;
								}
							}
						} else {
							if (mGridViewBatchDeleteCache != null) {
								Iterator<GridViewPaperItemForBatchDelete> mIterator = mGridViewBatchDeleteCache
										.iterator();
								while (mIterator.hasNext()) {
									GridViewPaperItemForBatchDelete item = mIterator
											.next();
									item.checked = false;
								}
							}
						}
						break;
					}
					switch (Config.current_theme) {
					case 1:
						noteAdapter.notifyDataSetChanged();
						break;
					case 2:
						int index = mGridPager.getCurrentItem();
						if (mGridPagerAdapter != null) {
							mGridPager.setAdapter(mGridPagerAdapter);
							mGridPagerAdapter.notifyDataSetChanged();
							mGridPager.setCurrentItem(index);
						}
						break;
					}
				} else if (buttonView.getId() == R.id.checkbox) {
					int position = ((Integer) buttonView.getTag()).intValue();
					updateListItemCheckBoxStatus(position, isChecked);
				}
			}
		};
		mOnClickListener = new OnClickListener() {
			@Override
			public void onClick(final View v) {
				if (v.getId() == R.id.titlebar_add_note_btn) {
					Calendar cal = Calendar.getInstance();
					int totdayRecordCount = noteDBManager.getDayRecordCount(
							cal.get(Calendar.YEAR),
							cal.get(Calendar.MONTH) + 1,
							cal.get(Calendar.DAY_OF_MONTH));
					if (totdayRecordCount > Config.maxCountEachDay) {
						Toast.makeText(
								mActivity,
								mActivity.getText(R.string.toast_cant_add_note),
								1000).show();
					} else {
						final Intent addNote = new Intent(mActivity,
								Editor.class);
						addNote.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						cancelShowBatchDeleteUI();
						// titleBarOut();
						AnimationHelper.runScaleAnim(v, 1f, 1f, 250,
								new AnimatorListener() {

									@Override
									public void onAnimationCancel(Animator arg0) {
										// TODO Auto-generated method stub

									}

									@Override
									public void onAnimationEnd(Animator arg0) {
										startEditor(addNote);
										currentPage = 1;
										// listView_myNote.resetFooter();
										// v.setScaleX(1f);
										// v.setScaleY(1f);
									}

									@Override
									public void onAnimationRepeat(Animator arg0) {
										// TODO Auto-generated method stub
									}

									@Override
									public void onAnimationStart(Animator arg0) {
										// TODO Auto-generated method stub
									}
								});
					}
				} else if (v.getId() == R.id.titlebar_batch_delete_btn) {
					showBatchDeleteUI();
				} else if (v.getId() == R.id.bottom_batch_delete_btn) {
					batchDeleteNote();
				} else if (v.getId() == R.id.note_settings) {
					openSettingsActivity();

				} else if (v.getId() == R.id.titlebar_overflow_options) {

					mCallBack.openDrawer();

				} else if (v.getId() == R.id.calendar_fragment) {
					if (mCallBack == null) {
						throw new NullPointerException(
								"mCallBack can't be null");
					}
					cancelShowBatchDeleteUI();
					mCallBack.switchFragment(Main.CALENDARFRAGMENT);
					// nullpointer error
					if (pop.isShowing()) {
						pop.dismiss();
					}
				} else if (v.getId() == R.id.search) {
					if (mCallBack == null) {
						throw new NullPointerException(
								"mCallBack can't be null");
					}
					// titleBarOut();
					cancelShowBatchDeleteUI();
					mCallBack.switchFragment(Main.SEARCHFRAGMENT);
					if (pop.isShowing()) {
						pop.dismiss();
					}
				}
			}
		};
	}

	public void openSettingsActivity() {
		Intent intent = new Intent(mActivity, NoteSettings.class);
		cancelShowBatchDeleteUI();
		// titleBarOut();
		mActivity = this.getActivity();
		mActivity.startActivity(intent);
		mActivity.overridePendingTransition(android.R.anim.fade_in,
				android.R.anim.fade_out);
		if (pop.isShowing()) {
			pop.dismiss();
		}
	}

	private void startEditor(Intent intent) {
		mActivity.startActivity(intent);
		mActivity.overridePendingTransition(android.R.anim.fade_in,
				android.R.anim.fade_out);
	}

	private void initAutoRefreshListView(View v) {
		// //20141124 增加阻尼效果
		listView_myNote = (BounceListView) v.findViewById(R.id.note_list_view);
		LayoutInflater inflate = LayoutInflater.from(mActivity);
		View header = inflate.inflate(R.layout.note_list_item_header, null);
		listView_myNote.addHeaderView(header);
		listView_myNote.setOnScrollListener(this);

		// mActivity.registerForContextMenu(listView_myNote);
	}

	private void initEmptyView(View v) {
		textview_empty = (TextView) v.findViewById(R.id.empty_text);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		Logg.D(TAG + " onActivityCreated");
	}

	@Override
	public void onStart() {
		super.onStart();
		Logg.D(TAG + " onStart");

		if (isDeleteUIShown) {
			bottom_layout.setY(screenHeight - 380);
		} else {
			bottom_layout.setY(screenHeight);
		}
	}

	private class RefreshViewsTask extends AsyncTask<Integer, Integer, String> {
		@Override
		public void onPreExecute() {
			currentPage = 1;
		}

		@Override
		protected String doInBackground(Integer... arg0) {
			if (list_note_data != null) {
				list_note_data.clear();
			}
			switch (Config.current_theme) {
			case 1:
				list_note_data = noteDBManager.queryDividedPagerRecords(
						currentPage, 0, queryPageNum);
				break;
			case 2:
				break;
			}
			// clearImageList();
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			switch (Config.current_theme) {
			case 1:
				setListView();
				break;
			case 2:
				long begin = System.currentTimeMillis();
				setGridPaper();

				break;
			}
			mProgressBar.setVisibility(View.GONE);
			isFirstLoaded = false;
			com.mars.note.Config.recent_needRefresh = false;
			flag = true;
		}
	}

	@SuppressLint("NewApi")
	private void setGridPaper() {
		// long begin = System.currentTimeMillis();
		if (records_count == 0) {
			this.mGridPager.setVisibility(View.GONE);
		} else {
			this.mGridPager.setVisibility(View.VISIBLE);

			if (mGridViewBatchDeleteCache == null) {
				mGridViewBatchDeleteCache = new ArrayList<com.mars.note.fragment.GridViewPaperItemForBatchDelete>();
			}
			mGridViewBatchDeleteCache.clear();
			int pageNum = records_count / 6;
			int lastPageItems = 0;
			double result = (double) records_count / 6;
			if (result > pageNum) {
				lastPageItems = records_count - pageNum * 6;
			}

			ArrayList<NoteRecord> allData = (ArrayList<NoteRecord>) noteDBManager
					.queryAllRecords();
			for (int i = 0; i < pageNum; i++) {
				for (int j = 0; j < 6; j++) {
					int dataPos = i * 6 + j;
					NoteRecord nr = allData.get(dataPos);
					GridViewPaperItemForBatchDelete item = new GridViewPaperItemForBatchDelete();
					item.index = i;
					item.position = j;
					item.checked = false;
					item.id = nr.id;
					mGridViewBatchDeleteCache.add(item);
				}
			}
			if (lastPageItems > 0) {
				for (int i = 0; i < lastPageItems; i++) {
					int dataPos = i + pageNum * 6;
					NoteRecord nr = allData.get(dataPos);
					GridViewPaperItemForBatchDelete item = new GridViewPaperItemForBatchDelete();
					item.index = pageNum;
					item.position = i;
					item.checked = false;
					item.id = nr.id;
					mGridViewBatchDeleteCache.add(item);
				}
			}
			NoteApplication
					.setGridViewBatchDeleteCache(mGridViewBatchDeleteCache);
			mGridPagerAdapter = new GridPaperAdapter(this);
			mGridPager.setAdapter(mGridPagerAdapter);
			mGridPagerAdapter.notifyDataSetChanged();
			SharedPreferences pref = getActivity().getSharedPreferences(
					"effect", Context.MODE_PRIVATE);
			int pos = pref.getInt("effect_id", 0);
			setEffect(pos);
			mGridPager.setOnPageChangeListener(new OnPageChangeListener() {

				@Override
				public void onPageSelected(int pos) {
					// TODO Auto-generated method stub
//					mGridPager.setCurrentIndex(pos);
				}

				@Override
				public void onPageScrolled(int arg0, float arg1, int arg2) {
					// TODO Auto-generated method stub

				}

				@Override
				public void onPageScrollStateChanged(int arg0) {
					// TODO Auto-generated method stub

				}
			});
		}
	}

	private void setEffect(int i) {
		switch (i) {
		case 0:
			mGridPager.setTransitionEffect(TransitionEffect.Standard);
			break;
		case 1:
			mGridPager.setTransitionEffect(TransitionEffect.Tablet);
			break;
		case 2:
			mGridPager.setTransitionEffect(TransitionEffect.Alpha);

			break;
		case 3:
			mGridPager.setTransitionEffect(TransitionEffect.RotateDown);
			break;

		}
	}

	@SuppressLint("NewApi")
	private void setListView() {
		if (records_count > queryPageNum * currentPage) {
			isLoadMoreModel = true;
		} else {
			isLoadMoreModel = false;
		}
		listView_myNote.setAdapter(null);
		if (records_count == 0) {
			this.listView_myNote.setVisibility(View.INVISIBLE);
			//
		} else {
			this.listView_myNote.setVisibility(View.VISIBLE);

			if (item_checkbox_status == null) {
				item_checkbox_status = new ArrayList<ListItemCheckBoxStatus>();
			}
			item_checkbox_status.clear();
			for (int i = 0; i < list_note_data.size(); i++) {
				ListItemCheckBoxStatus item = new ListItemCheckBoxStatus();
				item.positon = i;
				item.onChecked = false;
				item_checkbox_status.add(item);
			}
			item_checkbox_status.trimToSize();
			// setHeaderAndFooter();
			// List<NoteRecord> mData = new ArrayList<NoteRecord>();
			// mData.addAll(list_note_data);
			noteAdapter = new NoteListAdapter(mActivity, list_note_data);
			listView_myNote.setAdapter(noteAdapter);
			// noteAdapter.notifyDataSetChanged();
		}
	}

	@SuppressLint("NewApi")
	private void titleBarIn() {
		// titlebar_overflow_options.setVisibility(View.VISIBLE);
		titlebar_add_note_btn.setScaleX(1f);
		titlebar_add_note_btn.setScaleY(1f);
		titlebar_add_note_btn.setAlpha(1f);
		if (records_count == 0) {
			if (titlebar_batch_delete_imgbtn != null) {
				titlebar_batch_delete_imgbtn.setVisibility(View.INVISIBLE);
				RelativeLayout.LayoutParams param = (RelativeLayout.LayoutParams) this.titlebar_add_note_btn
						.getLayoutParams();
				param.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
				titlebar_add_note_btn.setLayoutParams(param);
				titlebar_add_note_btn.setVisibility(View.VISIBLE);

			}
		} else {
			if (titlebar_batch_delete_imgbtn != null) {
				titlebar_batch_delete_imgbtn.setVisibility(View.VISIBLE);

				titlebar_batch_delete_imgbtn.setAlpha(1f);
				if (!isDeleteUIShown && !firstResume) {
					titlebar_batch_delete_imgbtn.setY(7);
				}// bug 垃圾桶不显示

				RelativeLayout.LayoutParams param = (RelativeLayout.LayoutParams) titlebar_add_note_btn
						.getLayoutParams();
				param.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT);
				param.addRule(RelativeLayout.LEFT_OF,
						R.id.titlebar_batch_delete_btn);

				titlebar_add_note_btn.setLayoutParams(param);
				titlebar_add_note_btn.setVisibility(View.VISIBLE);
			}
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		Logg.D(TAG + " onResume");
		if (pop != null && pop.isShowing()) {
			pop.dismiss();
		}
		if (flag) {
			flag = false;
			if (isFirstLoaded == true
					|| com.mars.note.Config.recent_needRefresh) {
				this.textview_empty.setVisibility(View.GONE);
				this.mProgressBar.setVisibility(View.VISIBLE);
				listView_myNote.setVisibility(View.GONE);
				mGridPager.setVisibility(View.GONE);
				records_count = noteDBManager.getMaxRecordsCount();

				if (records_count == 0) {
					this.mProgressBar.setVisibility(View.GONE);
					this.textview_empty.setVisibility(View.VISIBLE);
					if (Config.current_theme == 1) {
						setListView();
					} else if (Config.current_theme == 2) {
						setGridPaper();
					}
					flag = true;
				} else {
					this.textview_empty.setVisibility(View.INVISIBLE);
					new RefreshViewsTask().execute();
				}
			} else {
				flag = true;
			}
		}
		titleBarIn();
		firstResume = false;
	}

	@Override
	public void onPause() {
		super.onPause();
		Logg.D(TAG + " onPause");
	}

	@Override
	public void onStop() {
		super.onStop();
		Logg.D(TAG + " onStop");
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		Logg.D(TAG + " onDestroyView");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		isDeleteUIShown = false;
		System.gc();
		Logg.D(TAG + " onDestroy");
	}

	private class ViewHolder {
		int position;
		TextView date;
		TextView time;
		TextView title;
		TextView content;
		ViewGroup titleAndContent;
		ImageView img;
		CheckBox mCheckBox;
	}

	private class NoteListAdapter extends BaseAdapter {
		Context mContext;
		LayoutInflater inflate;
		List<NoteRecord> mData;

		public NoteListAdapter(Context context, List<NoteRecord> list) {
			mContext = context;
			inflate = LayoutInflater.from(context);
			mData = list;
		}

		@Override
		public boolean isEnabled(int position) {
			return false;
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return mData.size();
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return mData.get(position);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder = null;
			if (convertView == null) {
				convertView = inflate.inflate(R.layout.note_list_item, parent,
						false);
				holder = new ViewHolder();
				holder.date = (TextView) convertView.findViewById(R.id.date);
				holder.time = (TextView) convertView.findViewById(R.id.time);
				holder.title = (TextView) convertView.findViewById(R.id.title);
				holder.content = (TextView) convertView
						.findViewById(R.id.content);
				holder.titleAndContent = (ViewGroup) convertView
						.findViewById(R.id.title_and_content);
				holder.img = (ImageView) convertView
						.findViewById(R.id.note_listitem_img);
				holder.mCheckBox = (CheckBox) convertView
						.findViewById(R.id.checkbox);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			holder.img.setVisibility(View.INVISIBLE);
			holder.position = position;
			holder.mCheckBox.setTag(Integer.valueOf(position));
			if (isDeleteUIShown) {
				holder.mCheckBox.setVisibility(View.VISIBLE);
				int[] location = new int[2];
				holder.mCheckBox.getLocationOnScreen(location);
				int x = location[0];
				int y = location[1];
				ListItemCheckBoxStatus item = item_checkbox_status
						.get(position);
				holder.mCheckBox.setChecked(item.onChecked);
			} else {
				holder.mCheckBox.setVisibility(View.INVISIBLE);
			}
			final NoteRecord nr = (NoteRecord) getItem(position);
			holder.img.setTag(Integer.valueOf(position));
			final String path = nr.imgpath;
			if (path != null && (!path.equals("null"))) {
				LayoutParams param = holder.titleAndContent.getLayoutParams();
				if (mBitmapCache == null) {
					throw new NullPointerException("mBitmapCache cant be null");
				}
				param.width = 500;
				if (mBitmapCache.get(path) == null) {
					final ListItemImg item = new ListItemImg();
					item.position = position;
					final ImageView img = holder.img;
					img.setTag(Integer.valueOf(position));
					item.bm = mBitmapCache.get(path);
					if (item.bm != null) {
						img.setVisibility(View.VISIBLE);
						img.setImageBitmap(item.bm);
					} else {
						final Handler handler = new Handler() {
							@Override
							public void handleMessage(Message msg) {
								if (msg.what == 1) {
									if (((Integer) img.getTag()).intValue() == item.position) {
										img.setVisibility(View.VISIBLE);
										img.setImageBitmap(item.bm);
									}
								}
							}
						};
						Thread thread = new Thread() {
							@Override
							public void run() {
								if (path != null && (!path.equals("null"))
										&& (!"".equals(path))) {
									item.bm = PictureHelper.getCropImage(path,
											400, true, 100, mActivity, 7, true);
									mBitmapCache.put(path, item.bm);
								} else {
									item.bm = null;
								}
								Message msg = new Message();
								msg.what = 1;
								handler.sendMessage(msg);
							}
						};
						executors.execute(thread);
					}
				} else {
					if (mBitmapCache.get(path) != null) {
						holder.img.setImageBitmap(mBitmapCache.get(path));
						holder.img.setVisibility(View.VISIBLE);
					} else {
					}
				}
			} else {
				LayoutParams param0 = holder.img.getLayoutParams();
				holder.img.setVisibility(View.GONE);
				LayoutParams param1 = holder.titleAndContent.getLayoutParams();
				param1.width = 500 + param0.width;
			}
			long msTime = Long.parseLong(nr.time);
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(msTime);
			String nowDate = calendar.get(Calendar.YEAR) + date_title[0]
					+ (calendar.get(Calendar.MONTH) + 1) + date_title[1]
					+ calendar.get(Calendar.DAY_OF_MONTH) + date_title[2];
			String nowTime = calendar.get(Calendar.HOUR_OF_DAY) + time_title[0]
					+ calendar.get(Calendar.MINUTE) + time_title[1];
			String dayOfWeekText = getdayOfWeek(calendar
					.get(Calendar.DAY_OF_WEEK));
			holder.date.setText(nowDate);
			holder.time.setText(dayOfWeekText + "  " + nowTime);
			holder.title.setText(nr.title);
			holder.content.setText(nr.content);
			holder.mCheckBox.setOnCheckedChangeListener(mNoteCheckBoxListener);
			final CheckBox cb = holder.mCheckBox;
			convertView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					int position = ((ViewHolder) v.getTag()).position;
					if (isDeleteUIShown) {
						boolean isChecked = cb.isChecked();
						cb.setChecked(!isChecked);
						updateListItemCheckBoxStatus(position, !isChecked);
					} else {
						Intent editNote = new Intent(mActivity, Editor.class);
						String id = nr.id;
						editNote.putExtra("note_id", id);
						// titleBarOut();
						cancelShowBatchDeleteUI();
						startEditor(editNote);
					}
				}
			});
			final int pos = position;
			convertView.setOnLongClickListener(new OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					// single_delete_position = ((ViewHolder)
					// v.getTag()).position;
					showBatchDeleteUI();
					if (isDeleteUIShown) {
						boolean isChecked = cb.isChecked();
						cb.setChecked(!isChecked);
						updateListItemCheckBoxStatus(pos, !isChecked);
					}
					return true;
				}
			});
			return convertView;
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return 0;
		}
	}

	private class GridPaperAdapter extends FragmentStatePagerAdapter {
		public GridPaperAdapter(android.support.v4.app.FragmentManager fm) {
			super(fm);
			// TODO Auto-generated constructor stub
		}

		public GridPaperAdapter(android.support.v4.app.Fragment fragment) {
			super(fragment.getChildFragmentManager());
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			if (object != null) {
				return ((Fragment) object).getView() == view;
			} else {
				return false;
			}
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			Object obj = super.instantiateItem(container, position);
			mGridPager.setObjectForPosition(obj, position);
			// return super.instantiateItem(container, position);
			return obj;
		}

		@Override
		public Fragment getItem(int pos) {
			GridViewPaperItem gridPagerItem = new GridViewPaperItem();
			Bundle bundle = new Bundle();
			bundle.putInt("index", pos);
			gridPagerItem.setArguments(bundle);
			gridPagerItem.setCallBack(RecentRecordsFragment.this);
			// gridPagerItem.setLruCache(mGridPaperMemoryCache);
			return gridPagerItem;
		}

		@Override
		public int getCount() {
			int pageNum = records_count / 6;
			double result = (double) records_count / 6;
			if (result > pageNum) {
				pageNum++;
			}
//			mGridPager.setpagerCount(pageNum);
			return pageNum;
		}
	}

	private class ListItemImg {
		public int position;
		public Bitmap bm;
	}

	private class ListItemCheckBoxStatus {
		public int positon;
		public boolean onChecked;
	}

	public void showBatchDeleteUI() {
		if (Config.animation_finished && (isDeleteUIShown == false)) {
			isDeleteUIShown = true;
			AnimationHelper.runVerticalOutAnim(titlebar_batch_delete_imgbtn, 7,
					-200, 500);

			AnimationHelper.runVerticalInAnim(bottom_layout, screenHeight,
					screenHeight - 380, 300);

			select_all_checkbox.setVisibility(View.VISIBLE);
			select_all_checkbox.setChecked(false);
			select_all_checkbox
					.setOnCheckedChangeListener(mNoteCheckBoxListener);

			if (select_all_checkbox == null) {
				throw new NullPointerException("View is null ! ");
			}

			int[] location = new int[2];
			// select_all_checkbox.getLocationOnScreen(location);
			// int x = location[0];
			// int y = location[1];
			// Log.d("mars","select_all_checkbox X "+x);

			AnimationHelper.runHorizontalInAnim(select_all_checkbox,
					screenWidth, screenWidth - 153, 500); // getwidth =72 margin
															// = 25*3 total 147
			if (select_all_checkbox == null) {
				throw new NullPointerException("View is null!");
			}
			AnimationHelper.runVerticalOutAnim(titlebar_add_note_btn,
					titlebar_add_note_btn.getY(), -200, 500);

			switch (Config.current_theme) {
			case 1:
				noteAdapter.notifyDataSetChanged();
				break;
			case 2:
				break;
			}
		}
	}

	public void cancelShowBatchDeleteUI() {
		if (isDeleteUIShown && Config.animation_finished) {
			isDeleteUIShown = false;
			if (records_count == 0) {
				if (titlebar_batch_delete_imgbtn != null) {
					titlebar_batch_delete_imgbtn.setVisibility(View.INVISIBLE);
				}
			} else {
				titlebar_batch_delete_imgbtn.setVisibility(View.VISIBLE);
				AnimationHelper.runVerticalInAnim(titlebar_batch_delete_imgbtn,
						-200, 7, 500);
			}
			if (select_all_checkbox == null) {
				throw new NullPointerException("View is null ! ");
			}
			AnimationHelper.runHorizontalOutAnim(select_all_checkbox,
					screenWidth - 153, screenWidth, 500);
			AnimationHelper.runVerticalOutAnim(bottom_layout,
					screenHeight - 380, screenHeight, 500);
			titlebar_add_note_btn.setVisibility(View.VISIBLE);
			AnimationHelper.runVerticalInAnim(titlebar_add_note_btn, -200, 7,
					500);
			titlebar_overflow_options.setVisibility(View.VISIBLE);

			if (delayToRefresh) {
				// 当批量删除ui开启，滑动到底部后不会执行刷新，因此在撤销删除ui后执行
				currentPage++;
				List<NoteRecord> newNoteRecordList = noteDBManager
						.queryDividedPagerRecords(1, currentPage - 1,
								queryPageNum);
				list_note_data.addAll(newNoteRecordList);
				// 更新每个checkbox的信息
				item_checkbox_status.clear();
				for (int i = 0; i < list_note_data.size(); i++) {
					ListItemCheckBoxStatus item = new ListItemCheckBoxStatus();
					item.positon = i;
					item.onChecked = false;
					item_checkbox_status.add(item);
				}
				item_checkbox_status.trimToSize();

				noteAdapter.notifyDataSetChanged();

				records_count = noteDBManager.getMaxRecordsCount();
				if (records_count > queryPageNum * currentPage) {
					isLoadMoreModel = true;
				} else {
					isLoadMoreModel = false;
				}
				delayToRefresh = false;
			}
		}
	}

	private void updateListItemCheckBoxStatus(int position, boolean onChecked) {
		Iterator<ListItemCheckBoxStatus> it = item_checkbox_status.iterator();
		while (it.hasNext()) {
			ListItemCheckBoxStatus item = it.next();
			if (item.positon == position) {
				item.onChecked = onChecked;
				break;
			}
		}
	}

	public void batchDeleteNote() {
		// Long before = System.currentTimeMillis();
		final List<NoteRecord> deleteList = new ArrayList<NoteRecord>();
		switch (Config.current_theme) {
		case 1:
			Iterator<ListItemCheckBoxStatus> mIterator = item_checkbox_status
					.iterator();
			while (mIterator.hasNext()) {
				ListItemCheckBoxStatus itemBox = mIterator.next();
				int position = itemBox.positon;
				boolean isChecked = itemBox.onChecked;
				if (isChecked) {
					NoteRecord deleteItem = list_note_data.get(position);
					deleteList.add(deleteItem);
				}
			}
			break;
		case 2:
			Iterator<GridViewPaperItemForBatchDelete> it = mGridViewBatchDeleteCache
					.iterator();
			while (it.hasNext()) {
				GridViewPaperItemForBatchDelete item = it.next();
				if (item.checked == true) {
					NoteRecord deleteItem = new NoteRecord();
					deleteItem.id = item.id;
					deleteList.add(deleteItem);
				}
			}
			break;
		}
		if (deleteList.size() == 0) {
			Toast.makeText(mActivity, "没有选中任何记录", 1000).show();
		} else {
			AlertDialog.Builder mDialog = new AlertDialog.Builder(mActivity);
			mDialog.setMessage(mActivity
					.getString(R.string.dialog_batch_delete));
			mDialog.setPositiveButton(mActivity.getString(R.string.dialog_yes),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// noteDBManager.batchDeleteRecord(deleteList);
							showExectingDialog();
							new BatchDeleteTask(deleteList).execute();
						}
					});
			mDialog.setNegativeButton(mActivity.getString(R.string.dialog_no),
					null);
			mDialog.show();
		}
	}

	private void showExectingDialog() {
		mExecutingDialog.show();
	}

	private void dissmissExecutingDialog() {
		mExecutingDialog.dismiss();
	}

	private class BatchDeleteTask extends AsyncTask<Integer, Integer, String> {
		List<NoteRecord> mList;

		public BatchDeleteTask(List<NoteRecord> deleteList) {
			this.mList = deleteList;
		}

		@Override
		protected String doInBackground(Integer... arg0) {
			noteDBManager.batchDeleteRecord(mList);
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			currentPage = 1;
			// listView_myNote.resetFooter();
			dissmissExecutingDialog();
			Config.calendar_needRefresh = true;
			Config.search_needRefresh = true;
			Config.recent_needRefresh = true;
			listView_myNote.setVisibility(View.GONE);
			onResume();
			cancelShowBatchDeleteUI();
		}
	}

	public boolean onBackPressed() {
		if (RecentRecordsFragment.isDeleteUIShown) {
			this.cancelShowBatchDeleteUI();
			switch (Config.current_theme) {
			case 1:
				if (item_checkbox_status == null) {
					item_checkbox_status = new ArrayList<ListItemCheckBoxStatus>();
				}
				item_checkbox_status.clear();
				for (int i = 0; i < list_note_data.size(); i++) {
					ListItemCheckBoxStatus item = new ListItemCheckBoxStatus();
					item.positon = i;
					item.onChecked = false;
					item_checkbox_status.add(item);
				}
				item_checkbox_status.trimToSize();
				noteAdapter.notifyDataSetChanged();
				break;
			case 2:
				int index = mGridPager.getCurrentItem();
				setGridPaper();
				mGridPager.setCurrentItem(index);
				break;
			}
			return true;
		} else {
			return false;
		}
	}

	public void deleteSingleRecord() {
		NoteRecord deleteItem = list_note_data.get(single_delete_position);
		if (noteDBManager == null) {
			throw new NullPointerException("noteDBManager == null!");
		}
		Config.calendar_needRefresh = true;
		Config.search_needRefresh = true;
		noteDBManager.deleteRecord(deleteItem.id);
		currentPage = 1;
		Config.recent_needRefresh = true;
		this.onResume();
		if (records_count == 0) {
			this.cancelShowBatchDeleteUI();
			switch (Config.current_theme) {
			case 1:
				if (item_checkbox_status == null) {
					item_checkbox_status = new ArrayList<ListItemCheckBoxStatus>();
				}
				item_checkbox_status.clear();
				for (int i = 0; i < list_note_data.size(); i++) {
					ListItemCheckBoxStatus item = new ListItemCheckBoxStatus();
					item.positon = i;
					item.onChecked = false;
					item_checkbox_status.add(item);
				}
				noteAdapter.notifyDataSetChanged();
				break;
			case 2:
				int index = mGridPager.getCurrentItem();
				setGridPaper();
				mGridPager.setCurrentItem(index);
				break;
			}
		}
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

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		int lastItem = firstVisibleItem + visibleItemCount;
		if (Config.current_theme == 1) {

			if ((totalItemCount - lastItem) == 1) {
				if (currentPage < MaxPageNum && Config.loadmore_sync
						&& isLoadMoreModel) {
					if (!RecentRecordsFragment.isDeleteUIShown) {

						// 20141124
						// 不用子线程setadapter更新listview,改用在主线程直接更新数据源，用adapter的notifydatachanged
						// 这样做避免了setSelection 的直跳效果，避免子线程更新数据源的安全问题
						currentPage++;
						List<NoteRecord> newNoteRecordList = noteDBManager
								.queryDividedPagerRecords(1, currentPage - 1,
										queryPageNum);
						list_note_data.addAll(newNoteRecordList);
						// 更新每个checkbox的信息
						item_checkbox_status.clear();
						for (int i = 0; i < list_note_data.size(); i++) {
							ListItemCheckBoxStatus item = new ListItemCheckBoxStatus();
							item.positon = i;
							item.onChecked = false;
							item_checkbox_status.add(item);
						}
						item_checkbox_status.trimToSize();

						noteAdapter.notifyDataSetChanged();

						records_count = noteDBManager.getMaxRecordsCount();
						if (records_count > queryPageNum * currentPage) {
							isLoadMoreModel = true;
						} else {
							isLoadMoreModel = false;
						}
					} else {
						delayToRefresh = true;
					}
				}
			}
		}
	}

	@Override
	public void onScrollStateChanged(AbsListView arg0, int arg1) {
		// TODO Auto-generated method stub
	}

	@Override
	public void showDeleteUI() {
		this.showBatchDeleteUI();
	}

	// 自定义Scroller，用反射机制改变 Viewpager的滑动时间
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

	public void openDrawer() {
	}

	public void runDrawerAnim(float arg0) {
		titlebar_overflow_options.setRotation(arg0);
	}

	public void closeDrawer() {
	}

}
