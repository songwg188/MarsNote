package com.mars.note.fragment;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.locks.ReentrantLock;

import com.mars.note.R;
import com.mars.note.R.drawable;
import com.mars.note.api.Config;
import com.mars.note.api.FragmentCallBack;
import com.mars.note.api.FragmentFactory;
import com.mars.note.api.GridViewPaperItemForBatchDelete;
import com.mars.note.api.ImageSpanInfo;
import com.mars.note.api.AlertDialogFactory;
import com.mars.note.api.ProgressDialogFactory;
import com.mars.note.app.BackUpActivity;
import com.mars.note.app.EditorActivity;
import com.mars.note.app.MarsNoteActivity;
import com.mars.note.app.NoteApplication;
import com.mars.note.app.NoteSettingsActivity;
import com.mars.note.database.NoteDataBaseManager;
import com.mars.note.database.NoteRecord;
import com.mars.note.fragment2.GridViewItemFragment;
import com.mars.note.fragment2.GridViewItemFragment.CallBack;
import com.mars.note.utils.AnimationHelper;
import com.mars.note.utils.DragViewUtil;
import com.mars.note.utils.FileHelper;
import com.mars.note.utils.Logg;
import com.mars.note.utils.PictureHelper;
import com.mars.note.utils.Util;
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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Filter;
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

/**
 * 显示最近的记录
 * 
 * @author mars
 */
public class RecentFragment extends BaseFragment implements OnScrollListener/*, CallBack*/ {
	public static final String TAG = "RecentRecordsFragment";
	private boolean DEBUG = false;
	private Activity mActivity;

	private ImageButton titlebar_add_note_btn; // 添加新笔记按钮
	private ImageButton titlebar_overflow_options; // 显示菜单按钮
	private ImageButton titlebar_batch_delete_imgbtn; // 批量删除按钮
	public static ImageButton bottom_batch_delete_btn; // 批量删除ui下，底部删除按钮
	public static View BOTTOM_LAYOUT; // 底部布局
	private CheckBox select_all_checkbox; // 选择全部复选框
	private BounceListView listView_myNote; // 20141124 增加阻尼效果
	private TextView textview_empty; // 空文本
	// private ProgressBar mProgressBar;
	private ViewGroup titleBar;
	private JazzyViewPager mGridPager; // 格子视图

	private OnCheckedChangeListener mNoteCheckBoxListener;
	private OnClickListener mOnClickListener;
	private NoteListAdapter noteAdapter;

	private NoteDataBaseManager noteDBManager;
	private List<NoteRecord> list_note_data; // 数据源
	private ArrayList<ListItemCheckBoxStatus> item_checkbox_status;
	private LruCache<String, Bitmap> mBitmapCache;
	private int single_delete_position;
	public static Boolean isDeleteUIShown = false;
	private int currentPage = 1;
	private int records_count;
	private boolean isLoadMoreModel = false;
	private boolean isFirstLoaded = true;
	private static final int queryPageNum = 20;
	private static final int MaxPageNum = 50;

	private PopupWindow pop;
	private View popView, rootView;
	private String[] date_title;
	private String[] time_title;
	private boolean flag = true;
	private boolean delayToRefresh = false;
	public static final int thread_num = 12;
	private ExecutorService executors;

	private FragmentStatePagerAdapter mGridPagerAdapter;
	// 单例模式 用于记录GridView将要删除的Item的位置
	private static ArrayList<com.mars.note.api.GridViewPaperItemForBatchDelete> mGridViewBatchDeleteCache;
	private int screenHeight, screenWidth;
	private float DPI;
	private boolean firstResume = true; // 20141201 bug 第一次调整垃圾桶setY 7 导致下移
	private ConcurrentHashMap<String, Boolean> mConcurentMap;
	private ReentrantLock mLock;

	// 拖拽工具类
	private DragViewUtil dragViewUtil;
	// 拖拽时被隐藏的View
	private View primaryView;
	// 拖拽时跟随手指的View
	private View dragView;
	private boolean isInDeleteArea = false;

	Handler handler;

	public static BaseFragment getInstance() {
		return new RecentFragment();
	}

	@Override
	public void onAttach(Activity mActivity) {
		super.onAttach(mActivity);
		Logg.D(TAG + " onAttach");
		this.mActivity = mActivity;
		date_title = mActivity.getResources().getStringArray(R.array.date_title);
		time_title = mActivity.getResources().getStringArray(R.array.time_title);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initDBManager();
		initListener();
		handler = new Handler();
		mBitmapCache = NoteApplication.getBitmapCache();
		mConcurentMap = NoteApplication.getmConcurentMap();// 保存线程工作状态
		mLock = NoteApplication.getmLock();// 20141212 公平锁

		executors = NoteApplication.getExecutors();
		if (executors == null) {
			Logg.E("executors null error");
			throw new NullPointerException("executors == null");
		}

		DisplayMetrics dm = new DisplayMetrics();
		getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
		screenHeight = dm.heightPixels;
		screenWidth = dm.widthPixels;
		DPI = dm.density;

		dragViewUtil = new DragViewUtil(mActivity);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_recent_records, container, false);
		// mProgressBar = (ProgressBar) v.findViewById(R.id.loading_progress);
		initTitleBarView(v);
		initAutoRefreshListView(v);
		initGridPaper(v);
		initEmptyView(v);
		BOTTOM_LAYOUT = v.findViewById(R.id.bottom);
		bottom_batch_delete_btn = (ImageButton) v.findViewById(R.id.bottom_batch_delete_btn);
		bindListener();
		rootView = v;
		return v;
	}

	private void initGridPaper(View v) {
		mGridPager = (JazzyViewPager) v.findViewById(R.id.note_grid_paper);
		mGridPager.setTouchable(true);
		mGridPager.setSpringBack(true); // 回弹开关
		mGridPager.setTransitionEffect(TransitionEffect.RotateDown);
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

	private void initTitleBarView(View v) {
		titleBar = (ViewGroup) v.findViewById(R.id.titlebar);
		titlebar_add_note_btn = (ImageButton) v.findViewById(R.id.titlebar_add_note_btn);
		titlebar_batch_delete_imgbtn = (ImageButton) v.findViewById(R.id.titlebar_batch_delete_btn);

		select_all_checkbox = (CheckBox) v.findViewById(R.id.select_all_checkBox);
		titlebar_overflow_options = (ImageButton) v.findViewById(R.id.titlebar_overflow_options);
		popView = LayoutInflater.from(mActivity).inflate(R.layout.popup_window_settings, null);
		ImageButton settings = (ImageButton) popView.findViewById(R.id.note_settings);
		settings.setOnClickListener(mOnClickListener);
		ImageButton calendar_fragment = (ImageButton) popView.findViewById(R.id.calendar_fragment);
		calendar_fragment.setOnClickListener(mOnClickListener);

		ImageButton search = (ImageButton) popView.findViewById(R.id.search);
		search.setOnClickListener(mOnClickListener);

		pop = new PopupWindow(popView, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, false);
		pop.setBackgroundDrawable(new BitmapDrawable());
		pop.setOutsideTouchable(true);
		pop.setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss() {
				AnimationHelper.runRotateAnim(titlebar_overflow_options, 180F, 360F, 150);
			}
		});
		pop.setFocusable(true);
		pop.update();
	}

	private void initListener() {
		mNoteCheckBoxListener = new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				// 全选checkbox
				if (buttonView.getId() == R.id.select_all_checkBox) {
					switch (Config.current_theme) {
					case 1:
						if (isChecked) {
							Iterator<ListItemCheckBoxStatus> mIterator = item_checkbox_status.iterator();
							while (mIterator.hasNext()) {
								ListItemCheckBoxStatus itemBox = mIterator.next();
								itemBox.onChecked = true;
							}
						} else {
							Iterator<ListItemCheckBoxStatus> mIterator = item_checkbox_status.iterator();
							while (mIterator.hasNext()) {
								ListItemCheckBoxStatus itemBox = mIterator.next();
								itemBox.onChecked = false;
							}
						}
						break;
					case 2:
						if (isChecked) {
							if (mGridViewBatchDeleteCache != null) {
								Iterator<GridViewPaperItemForBatchDelete> mIterator = mGridViewBatchDeleteCache.iterator();
								while (mIterator.hasNext()) {
									GridViewPaperItemForBatchDelete item = mIterator.next();
									item.checked = true;
								}
							}
						} else {
							if (mGridViewBatchDeleteCache != null) {
								Iterator<GridViewPaperItemForBatchDelete> mIterator = mGridViewBatchDeleteCache.iterator();
								while (mIterator.hasNext()) {
									GridViewPaperItemForBatchDelete item = mIterator.next();
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
					int totdayRecordCount = noteDBManager
							.getDayRecordCount(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
					if (totdayRecordCount > Config.maxCountEachDay) {
						Toast.makeText(mActivity, mActivity.getText(R.string.toast_cant_add_note), 1000).show();
					} else {
						final Intent addNote = new Intent(mActivity, EditorActivity.class);
						addNote.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						cancelShowBatchDeleteUI();
						// titleBarOut();
						AnimationHelper.runScaleAnim(v, 1f, 1f, 250, new AnimatorListener() {
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
						throw new NullPointerException("mCallBack can't be null");
					}
					cancelShowBatchDeleteUI();
					mCallBack.switchFragment(FragmentFactory.CALENDARFRAGMENT);
					// nullpointer error
					if (pop.isShowing()) {
						pop.dismiss();
					}
				} else if (v.getId() == R.id.search) {
					if (mCallBack == null) {
						throw new NullPointerException("mCallBack can't be null");
					}
					// titleBarOut();
					cancelShowBatchDeleteUI();
					mCallBack.switchFragment(FragmentFactory.SEARCHFRAGMENT);
					if (pop.isShowing()) {
						pop.dismiss();
					}
				}
			}
		};
	}

	public void openSettingsActivity() {
		Intent intent = new Intent(mActivity, NoteSettingsActivity.class);
		cancelShowBatchDeleteUI();
		// titleBarOut();
		mActivity = this.getActivity();
		mActivity.startActivity(intent);
		mActivity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
		if (pop.isShowing()) {
			pop.dismiss();
		}
	}

	private void startEditor(Intent intent) {
		mActivity.startActivity(intent);
		mActivity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
	}

	private int oldX, oldY;

	private void initAutoRefreshListView(View v) {
		// //20141124 增加阻尼效果
		listView_myNote = (BounceListView) v.findViewById(R.id.note_list_view);
		LayoutInflater inflate = LayoutInflater.from(mActivity);
		View header = inflate.inflate(R.layout.note_list_item_header, null);
		listView_myNote.addHeaderView(header);
		listView_myNote.setOnScrollListener(this);
		listView_myNote.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {

				if (dragView != null && primaryView != null) {
					switch (event.getAction()) {
					case MotionEvent.ACTION_MOVE:
						dragViewUtil.drag((int) event.getRawX() - oldX, (int) event.getRawY() - oldY, primaryView, dragView);
						int moveX = (int) event.getRawX();
						int moveY = (int) event.getRawY();
						int[] location = new int[2];
						BOTTOM_LAYOUT.getLocationOnScreen(location);
						// Logg.D("BOTTOM_LAYOUT getY " + location[1] +
						// ", getY " + moveY);
						if (moveX > RecentFragment.BOTTOM_LAYOUT.getLeft() && moveX < RecentFragment.BOTTOM_LAYOUT.getRight()
								&& moveY > (location[1]-Util.dpToPx(getResources(), 5))) {
							dragView.setBackgroundResource(R.drawable.list_item_bg_deleted);
							bottom_batch_delete_btn.setBackgroundResource(R.drawable.list_item_bg_pressed);
							isInDeleteArea = true;
							// Logg.D("in delete area");
						} else {
							dragView.setBackgroundResource(R.drawable.list_item_bg_pressed);
							bottom_batch_delete_btn.setBackgroundResource(R.drawable.list_item_bg_normal);
							isInDeleteArea = false;
							// Logg.D("out of delete area");
						}
						return true;
					case MotionEvent.ACTION_UP:
						// Logg.D("onTouch ACTION_UP oldX " + oldX + " oldY " +
						// oldY);
						if (primaryView != null) {
							primaryView.setVisibility(View.VISIBLE);
							primaryView = null;
							dragViewUtil.stopDrag(dragView);
							if (isInDeleteArea) {
								batchDeleteNote();
							}
						}
						break;
					}
				}
				return false;
			}
		});
		// mActivity.registerForContextMenu(listView_myNote);
	}

	private void initEmptyView(View v) {
		textview_empty = (TextView) v.findViewById(R.id.empty_text);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onStart() {
		super.onStart();
		if (isDeleteUIShown) {
			BOTTOM_LAYOUT.setY(screenHeight - (getResources().getDimensionPixelSize(R.dimen.bottom_height)));
		} else {
			BOTTOM_LAYOUT.setY(screenHeight);
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
				list_note_data = noteDBManager.queryDividedPagerRecords(currentPage, 0, queryPageNum);
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
				setGridPaper();

				break;
			}
			// mProgressBar.setVisibility(View.GONE);
			isFirstLoaded = false;
			com.mars.note.api.Config.recent_needRefresh = false;
			flag = true;
		}
	}

	private void setGridPaper() {
		if (records_count == 0) {
			this.mGridPager.setVisibility(View.GONE);
		} else {
			this.mGridPager.setVisibility(View.VISIBLE);

			if (mGridViewBatchDeleteCache == null) {
				mGridViewBatchDeleteCache = new ArrayList<com.mars.note.api.GridViewPaperItemForBatchDelete>();
			}
			mGridViewBatchDeleteCache.clear();
			int pageNum = records_count / 6;
			int lastPageItems = 0;
			double result = (double) records_count / 6;
			if (result > pageNum) {
				lastPageItems = records_count - pageNum * 6;
			}

			ArrayList<NoteRecord> allData = (ArrayList<NoteRecord>) noteDBManager.queryAllRecords();
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

			mGridPagerAdapter = new GridPaperAdapter(this);
			mGridPager.setAdapter(mGridPagerAdapter);
			mGridPagerAdapter.notifyDataSetChanged();
			SharedPreferences pref = getActivity().getSharedPreferences("effect", Context.MODE_PRIVATE);
			int pos = pref.getInt("effect_id", 0);
			setEffect(pos);
			mGridPager.setOnPageChangeListener(new OnPageChangeListener() {

				@Override
				public void onPageSelected(int pos) {
					mGridPager.setCurrentIndex(pos);
				}

				@Override
				public void onPageScrolled(int arg0, float arg1, int arg2) {
				}

				@Override
				public void onPageScrollStateChanged(int arg0) {
				}
			});
		}
	}

	public static ArrayList<GridViewPaperItemForBatchDelete> getGridViewBatchDeleteCache() {
		return mGridViewBatchDeleteCache;
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
		case 4:
			mGridPager.setTransitionEffect(TransitionEffect.ZoomIn);
			break;
		case 5:
			mGridPager.setTransitionEffect(TransitionEffect.Accordion);
			break;

		}
	}

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
			noteAdapter = new NoteListAdapter(mActivity, list_note_data);
			listView_myNote.setAdapter(noteAdapter);
			// noteAdapter.notifyDataSetChanged();
		}
	}

	private void titleBarIn() {
		// titlebar_overflow_options.setVisibility(View.VISIBLE);
		titlebar_add_note_btn.setScaleX(1f);
		titlebar_add_note_btn.setScaleY(1f);
		titlebar_add_note_btn.setAlpha(1f);
		if (records_count == 0) {
			if (titlebar_batch_delete_imgbtn != null) {
				titlebar_batch_delete_imgbtn.setVisibility(View.INVISIBLE);
				RelativeLayout.LayoutParams param = (RelativeLayout.LayoutParams) this.titlebar_add_note_btn.getLayoutParams();
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

				RelativeLayout.LayoutParams param = (RelativeLayout.LayoutParams) titlebar_add_note_btn.getLayoutParams();
				param.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
				// param.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT);
				// 需要api最低17 用上述方法替换
				param.addRule(RelativeLayout.LEFT_OF, R.id.titlebar_batch_delete_btn);

				titlebar_add_note_btn.setLayoutParams(param);
				titlebar_add_note_btn.setVisibility(View.VISIBLE);
			}
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (pop != null && pop.isShowing()) {
			pop.dismiss();
		}
		if (flag) {
			flag = false;
			if (isFirstLoaded == true || com.mars.note.api.Config.recent_needRefresh) {
				this.textview_empty.setVisibility(View.GONE);
				// this.mProgressBar.setVisibility(View.VISIBLE);
				listView_myNote.setVisibility(View.GONE);
				mGridPager.setVisibility(View.GONE);
				records_count = noteDBManager.getMaxRecordsCount();

				if (records_count == 0) {
					// this.mProgressBar.setVisibility(View.GONE);
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
		IntentFilter mFilter = new IntentFilter();
		mFilter.addAction(BATCH_DELETE);
		mActivity.registerReceiver(mBroadcastReceiver, mFilter);
	}

	@Override
	public void onPause() {
		mActivity.unregisterReceiver(mBroadcastReceiver);
		super.onPause();
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		isDeleteUIShown = false;
		System.gc();
	}

	private class ViewHolder {
		int position;
		TextView date;
		TextView time;
		TextView title;
		TextView content;
		ViewGroup titleAndContent;
		ImageView img;
		CheckBox checkBox;
	}

	private class NoteListAdapter extends BaseAdapter {
		Context mContext;
		LayoutInflater inflate;
		List<NoteRecord> mData;

		OnLongClickListener mOnLongClickListener;
		OnClickListener mOnClickListener;
		StringBuilder date, time;
		Calendar calendar;

		public NoteListAdapter(Context context, List<NoteRecord> list) {
			mContext = context;
			inflate = LayoutInflater.from(context);
			mData = list;
			date = new StringBuilder();
			time = new StringBuilder();
			calendar = Calendar.getInstance();
		}

		@Override
		public boolean isEnabled(int position) {
			return false;
		}

		@Override
		public int getCount() {
			return mData.size();
		}

		private void caculateViewLocation(View v) {
			// int[] location = new int[2];
			// v.getLocationOnScreen(location);
			// int x = location[0];
			// int y = location[1];
			// Logg.D("caculateViewLocation x " + x);
			// Logg.D("caculateViewLocation y " + y);
			// Logg.D("screen width " + screenWidth);
			// Logg.D("screen height " + screenHeight);
			Logg.D("caculateViewLocation x " + v.getX());
			Logg.D("caculateViewLocation y " + v.getY());

		}

		@Override
		public NoteRecord getItem(int position) {
			return mData.get(position);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder = null;
			if (convertView == null) {
				convertView = inflate.inflate(R.layout.note_list_item, parent, false);
				holder = new ViewHolder();
				holder.date = (TextView) convertView.findViewById(R.id.date);
				holder.time = (TextView) convertView.findViewById(R.id.time);
				holder.title = (TextView) convertView.findViewById(R.id.title);
				holder.content = (TextView) convertView.findViewById(R.id.content);
				holder.titleAndContent = (ViewGroup) convertView.findViewById(R.id.title_and_content);
				holder.img = (ImageView) convertView.findViewById(R.id.note_listitem_img);
				holder.checkBox = (CheckBox) convertView.findViewById(R.id.checkbox);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			holder.img.setVisibility(View.INVISIBLE);
			holder.position = position;
			holder.checkBox.setTag(Integer.valueOf(position));
			if (isDeleteUIShown) {
				holder.checkBox.setVisibility(View.VISIBLE);
				ListItemCheckBoxStatus item = item_checkbox_status.get(position);
				holder.checkBox.setChecked(item.onChecked);
			} else {
				holder.checkBox.setVisibility(View.INVISIBLE);
			}
			final NoteRecord nr = getItem(position);
			// 异步加载图片
			holder.img.setTag(Integer.valueOf(position));
			final String path = nr.imgpath;
			if (path != null && (!path.equals("null")) && (!"".equals(path))) {
				LayoutParams param = holder.titleAndContent.getLayoutParams();
				if (mBitmapCache == null) {
					throw new NullPointerException("mBitmapCache cant be null");
				}
				param.width = getResources().getDimensionPixelSize(R.dimen.title_and_content_width);
				if (mBitmapCache.get(path) == null) {
					final ListItem item = new ListItem();
					item.position = position;
					final ImageView img = holder.img;
					final ViewGroup titleAndContent = holder.titleAndContent;
					img.setTag(Integer.valueOf(position));
					item.bm = mBitmapCache.get(path);
					if (item.bm != null) {
						img.setVisibility(View.VISIBLE);
						img.setImageBitmap(item.bm);
					} else {
						if (mConcurentMap.get(path) == null)
							mConcurentMap.put(path, false);
						// 20141215 Runnable对将交给线程池内部的线程
						Runnable loadIMG = new Runnable() {
							@Override
							public void run() {
								// Lock mConcurentMap
								mLock.lock();
								if (mConcurentMap.get(path) == false) {
									mConcurentMap.put(path, true);
									// UnLock mConcurentMap
									mLock.unlock();
									if (path != null && (!path.equals("null")) && (!"".equals(path))) {
										if (Config.DB_SAVE_MODE) {
											// 从数据库读图
											item.bm = noteDBManager.getCroppedImage(path);
											if (item.bm == null)
												throw new NullPointerException("bm null error");
											// 旧的数据库需要此步骤
											// item.bm =
											// PictureHelper.getCropImage(path,
											// getResources().getDimension(R.dimen.listview_image_width),
											// getResources().getDimension(R.dimen.listview_image_height),
											// true, 100, mActivity, 7, true);
										} else {
											item.bm = PictureHelper.getCropImage(path, getResources().getDimension(R.dimen.listview_image_width),
													getResources().getDimension(R.dimen.listview_image_height), true, 100, mActivity, 7, true);
										}
										if (item.bm != null) {
											mBitmapCache.put(path, item.bm);
										} else {
											// 20141216 预览图已经删除 需要更新数据库 并刷新start
											noteDBManager.deleteImagePath(nr);
											mConcurentMap.put(path, false);
											Logg.I("img not exsited");
											// 20141216 预览图已经删除 需要更新数据库 并刷新end
										}
										mConcurentMap.put(path, false);
									} else {
										item.bm = null;
										mConcurentMap.put(path, false);
									}
									handler.post(new Runnable() {
										@Override
										public void run() {
											if (((Integer) img.getTag()).intValue() == item.position) {
												img.setVisibility(View.VISIBLE);
												// 20141219 如果读不到图片则改变文本宽度 start
												if (item.bm == null) {
													Logg.I(path + " not existed");
													LayoutParams param0 = img.getLayoutParams();
													img.setVisibility(View.GONE);
													LayoutParams param1 = titleAndContent.getLayoutParams();
													param1.width = getResources().getDimensionPixelSize(R.dimen.title_and_content_width) + param0.width;
													return;
												}
												// 20141219 如果读不到图片则改变文本宽度 end
												img.setImageBitmap(item.bm);
												if (DEBUG)
													Logg.I(path + " is load new");
											}
										}
									});
								} else {
									mLock.unlock();
									while (true) {
										if (mConcurentMap.get(path) == false) {
											break;
										}
									}
									Logg.I("other go out cycle");
									if (mBitmapCache.get(path) != null) {
										item.bm = mBitmapCache.get(path);
										handler.post(new Runnable() {
											@Override
											public void run() {
												if (((Integer) img.getTag()).intValue() == item.position) {
													img.setVisibility(View.VISIBLE);
													if (item.bm == null) {
														throw new NullPointerException("item.bm==null");
													}
													img.setImageBitmap(item.bm);
													Logg.I(path + " is load from cache");
												}
											}
										});
									} else {
										Logg.I("img not exsited");
									}
								}
							}
						};
						try {
							executors.execute(loadIMG);
							// 20141219 bug
							// java.util.concurrent.RejectedExecutionException
						} catch (RejectedExecutionException e) {
							e.printStackTrace();
						}
					}
				} else {
					if (mBitmapCache.get(path) != null) {
						holder.img.setImageBitmap(mBitmapCache.get(path));
						holder.img.setVisibility(View.VISIBLE);
						// Logg.D(path + " is from cache");
					} else {
						throw new NullPointerException("(mBitmapCache.get(path) == null!");
					}
				}
			} else {
				LayoutParams param0 = holder.img.getLayoutParams();
				holder.img.setVisibility(View.GONE);
				LayoutParams param1 = holder.titleAndContent.getLayoutParams();
				param1.width = getResources().getDimensionPixelSize(R.dimen.title_and_content_width) + param0.width;
			}

			// 20141214 bug 这里会有小卡顿 begin
			// 此bug是布局过于复杂引起，改用LinearLayout代替RelativeLayout
			calendar.setTimeInMillis(Long.parseLong(nr.time));
			// StringBuilder非线程安全，这里是主线程在跑 StringBuffer 线程安全
			date.setLength(0);
			time.setLength(0);
			date.append(calendar.get(Calendar.YEAR)).append(date_title[0]).append((calendar.get(Calendar.MONTH) + 1)).append(date_title[1])
					.append(calendar.get(Calendar.DAY_OF_MONTH)).append(date_title[2]);
			time.append(calendar.get(Calendar.HOUR_OF_DAY)).append(time_title[0]).append(calendar.get(Calendar.MINUTE)).append(time_title[1]).append("  ")
					.append(getdayOfWeek(calendar.get(Calendar.DAY_OF_WEEK)));
			holder.date.setText(date.toString());
			holder.time.setText(time.toString());
			// 20141214 bug 这里会有小卡顿 end
			holder.title.setText(nr.title);
			// 20141215 过滤图片字符串 start
			String content = nr.content;
			byte[] data = nr.imageSpanInfos;
			if (data != null) {
				ArrayList<ImageSpanInfo> imageSpanInfoList = Util.getImageSpanInfoListFromBytes(data);
				content = Util.filterContent(mActivity, content, imageSpanInfoList);
			}
			holder.content.setText(content);
			// if(holder.content.getLineCount() > 4){
			// int lineEndIndex = holder.content.getLayout().getLineEnd(3);
			// String text = holder.content.getText().subSequence(0,
			// lineEndIndex - 3) + "...";
			// holder.content.setText(text);
			// }
			// 20141215 过滤图片字符串 end

			// 设置长按 点击 checkbox的监听器
			setListener(convertView);
			return convertView;
		}

		/**
		 * 设置长按 点击 checkbox的监听器
		 * 
		 * @param convertView
		 * @param nr
		 */
		private void setListener(final View convertView) {
			// 更新CheckBox状态
			CheckBox checkBox = ((ViewHolder) convertView.getTag()).checkBox;
			checkBox.setOnCheckedChangeListener(mNoteCheckBoxListener);

			// 长按监听
			// 复用会出现乱
			convertView.setOnLongClickListener(new OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					Logg.I("pos = " + ((ViewHolder) v.getTag()).position);
					if (isDeleteUIShown) {
						CheckBox cb = ((ViewHolder) v.getTag()).checkBox;
						boolean isChecked = cb.isChecked();
						cb.setChecked(!isChecked);
						updateListItemCheckBoxStatus(((ViewHolder) convertView.getTag()).position, !isChecked);
						v.setVisibility(View.VISIBLE);
					} else {
						showBatchDeleteUI();
						CheckBox cb = ((ViewHolder) v.getTag()).checkBox;
						cb.setChecked(true);
						updateListItemCheckBoxStatus(((ViewHolder) convertView.getTag()).position, true);
						// 拖拽删除开始
						primaryView = v;
						primaryView.setVisibility(View.INVISIBLE);
						updateDragView(primaryView);

						dragViewUtil.startDrag(primaryView, dragView, getTitleBarHeight());
						// caculateViewLocation(BOTTOM_LAYOUT);
					}
					return true;
				}

				private void updateDragView(View primaryView) {
					dragView = LayoutInflater.from(mActivity).inflate(R.layout.note_list_item, null, false);
					dragView.setBackgroundResource(R.drawable.list_item_bg_pressed);
					dragView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

					ViewHolder primaryHolder = (ViewHolder) primaryView.getTag();
					ViewHolder dragHolder = new ViewHolder();
					dragHolder.date = (TextView) dragView.findViewById(R.id.date);
					dragHolder.time = (TextView) dragView.findViewById(R.id.time);
					dragHolder.title = (TextView) dragView.findViewById(R.id.title);
					dragHolder.content = (TextView) dragView.findViewById(R.id.content);
					dragHolder.titleAndContent = (ViewGroup) dragView.findViewById(R.id.title_and_content);
					dragHolder.img = (ImageView) dragView.findViewById(R.id.note_listitem_img);
					dragHolder.checkBox = (CheckBox) dragView.findViewById(R.id.checkbox);

					dragHolder.date.setText(primaryHolder.date.getText());
					dragHolder.time.setText(primaryHolder.time.getText());
					dragHolder.title.setText(primaryHolder.title.getText());
					dragHolder.content.setText(primaryHolder.content.getText());
					dragHolder.titleAndContent.getLayoutParams().width = primaryHolder.titleAndContent.getLayoutParams().width;
					dragHolder.titleAndContent.setVisibility(primaryHolder.titleAndContent.getVisibility());

					// Bitmap bm = mBitmapCache.get(path);
					dragHolder.img.setBackground(primaryHolder.img.getDrawable());
					dragHolder.img.setVisibility(primaryHolder.img.getVisibility());
					dragHolder.checkBox.setChecked(true);
					dragHolder.checkBox.setVisibility(View.VISIBLE);
				}
			});

			if (mOnClickListener == null)
				mOnClickListener = new OnClickListener() {
					@Override
					public void onClick(View v) {
						int position = ((ViewHolder) v.getTag()).position;
						if (isDeleteUIShown) {
							CheckBox cb = ((ViewHolder) v.getTag()).checkBox;
							boolean isChecked = cb.isChecked();
							cb.setChecked(!isChecked);
							updateListItemCheckBoxStatus(position, !isChecked);
						} else {
							Intent editNote = new Intent(mActivity, EditorActivity.class);
							String id = getItem(position).id;
							editNote.putExtra("note_id", id);
							cancelShowBatchDeleteUI();
							startEditor(editNote);
						}
					}
				};
			convertView.setOnClickListener(mOnClickListener);

			convertView.setOnTouchListener(new OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					if (event.getAction() == MotionEvent.ACTION_DOWN) {
						oldX = (int) event.getRawX();
						oldY = (int) event.getRawY();
						Logg.D("oldX " + oldX + " oldY " + oldY);
					}
					return false;
				}
			});
		}

		@Override
		public long getItemId(int position) {
			return position;
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
			GridViewItemFragment gridPagerItem = (GridViewItemFragment) FragmentFactory.newGridViewItem();
			Bundle bundle = new Bundle();
			bundle.putInt("index", pos);
			gridPagerItem.setArguments(bundle);
			//这里有概率性，callback nullpointer
//			gridPagerItem.setCallBack(RecentFragment.this);
			return gridPagerItem;
		}

		@Override
		public int getCount() {
			int pageNum = records_count / 6;
			double result = (double) records_count / 6;
			if (result > pageNum) {
				pageNum++;
			}
			mGridPager.setpagerCount(pageNum);
			return pageNum;
		}
	}

	private class ListItem {

		public int position;
		public Bitmap bm;
		// 20141215 尝试异步加载字符串
		String date;
		String time;
	}

	private class ListItemCheckBoxStatus {
		public int positon;
		public boolean onChecked;
	}

	public void showBatchDeleteUI() {
		if (Config.animation_finished && (isDeleteUIShown == false)) {
			isDeleteUIShown = true;
			AnimationHelper.runVerticalOutAnim(titlebar_batch_delete_imgbtn, 7, -200, 500);

			AnimationHelper.runVerticalInAnim(BOTTOM_LAYOUT, screenHeight, screenHeight - (getResources().getDimensionPixelSize(R.dimen.bottom_height)), 300);

			select_all_checkbox.setVisibility(View.VISIBLE);
			select_all_checkbox.setChecked(false);
			select_all_checkbox.setOnCheckedChangeListener(mNoteCheckBoxListener);

			if (select_all_checkbox == null) {
				Logg.E("select_all_checkbox null error");
				select_all_checkbox = (CheckBox) rootView.findViewById(R.id.select_all_checkBox);
				// throw new NullPointerException("View is null ! ");
			}

			AnimationHelper.runHorizontalInAnim(
					select_all_checkbox,
					screenWidth,
					screenWidth
							- (getResources().getDimensionPixelSize(R.dimen.checkbox_marginRight) + getResources()
									.getDimensionPixelSize(R.dimen.checkbox_width)), 500);
			if (select_all_checkbox == null) {
				throw new NullPointerException("View is null!");
			}
			AnimationHelper.runVerticalOutAnim(titlebar_add_note_btn, titlebar_add_note_btn.getY(), -200, 500);

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
				AnimationHelper.runVerticalInAnim(titlebar_batch_delete_imgbtn, -200, 7, 500);
			}
			if (select_all_checkbox == null) {
				throw new NullPointerException("View is null ! ");
			}
			AnimationHelper.runHorizontalOutAnim(select_all_checkbox, screenWidth
					- (getResources().getDimensionPixelSize(R.dimen.checkbox_marginRight) + getResources().getDimensionPixelSize(R.dimen.checkbox_width)),
					screenWidth, 500);
			AnimationHelper.runVerticalOutAnim(BOTTOM_LAYOUT, screenHeight - (getResources().getDimensionPixelSize(R.dimen.bottom_height)), screenHeight, 500);
			titlebar_add_note_btn.setVisibility(View.VISIBLE);
			AnimationHelper.runVerticalInAnim(titlebar_add_note_btn, -200, 7, 500);
			titlebar_overflow_options.setVisibility(View.VISIBLE);

			if (delayToRefresh) {
				// 当批量删除ui开启，滑动到底部后不会执行刷新，因此在撤销删除ui后执行
				currentPage++;
				List<NoteRecord> newNoteRecordList = noteDBManager.queryDividedPagerRecords(1, currentPage - 1, queryPageNum);
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
			Iterator<ListItemCheckBoxStatus> mIterator = item_checkbox_status.iterator();
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
			Iterator<GridViewPaperItemForBatchDelete> it = mGridViewBatchDeleteCache.iterator();
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
			AlertDialogFactory.showAlertDialog(mActivity, rootView, mActivity.getString(R.string.dialog_batch_delete),
					new AlertDialogFactory.DialogPositiveListner() {

						@Override
						public void onClick(View arg0) {
							super.onClick(arg0);
							showExectingDialog();
							new BatchDeleteTask(deleteList).execute();
						}
					});
		}
	}

	Runnable showExectingDialog = new Runnable() {
		@Override
		public void run() {
			if (rootView == null) {
				Logg.E("rootView null error");
			}
			if (mActivity == null) {
				Logg.E("mActivity null error");
			}
			ProgressDialogFactory.showProgressDialog(mActivity, rootView);
		}
	};

	private void showExectingDialog() {

		try {
			handler.postDelayed(showExectingDialog, 500);
		} catch (NullPointerException e) {
			if (handler == null) {
				Logg.E("handler null error");
			}
			if (showExectingDialog == null) {
				Logg.E("showExectingDialog null error");
			}
		}
	}

	private void dissmissExecutingDialog() {
		handler.removeCallbacks(showExectingDialog);
		ProgressDialogFactory.dismissProgressDialog();
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
		if (RecentFragment.isDeleteUIShown) {
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
		noteDBManager.deleteRecord(deleteItem);
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
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		int lastItem = firstVisibleItem + visibleItemCount;
		if (Config.current_theme == 1) {
			if ((totalItemCount - lastItem) == 1) {
				if (currentPage < MaxPageNum && Config.loadmore_sync && isLoadMoreModel) {
					if (!RecentFragment.isDeleteUIShown) {
						// 20141124
						// 不用子线程setadapter更新listview,改用在主线程直接更新数据源，用adapter的notifydatachanged
						// 这样做避免了setSelection 的直跳效果，避免子线程更新数据源的安全问题
						currentPage++;
						List<NoteRecord> newNoteRecordList = noteDBManager.queryDividedPagerRecords(1, currentPage - 1, queryPageNum);
						list_note_data.addAll(newNoteRecordList);
						Logg.D("onScroll addAll ");
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

//	@Override
//	public void showDeleteUI() {
//		this.showBatchDeleteUI();
//	}

	// 自定义Scroller，用反射机制改变 Viewpager的滑动时间
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

	public void openDrawer() {
	}

	public void runDrawerAnim(float arg0) {
		if (titlebar_overflow_options == null) {
			Logg.E("titlebar_overflow_options null error");
			initTitleBarView(rootView);
		}
		titlebar_overflow_options.setRotation(arg0);
	}

	public void closeDrawer() {
	}

	@Override
	public void onDetach() {
		super.onDetach();
	}

	public static final String BATCH_DELETE = "com.mars.note.fragment.RecentFragment.batch_delete";

	BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(BATCH_DELETE)) {
				batchDeleteNote();
			}
		}
	};

	public int getTitleBarHeight() {
		// TODO Auto-generated method stub
		return titleBar.getHeight();
	}

}
