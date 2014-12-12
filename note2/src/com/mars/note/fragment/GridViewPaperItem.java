package com.mars.note.fragment;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

import com.mars.note.Editor;
import com.mars.note.Main;
import com.mars.note.NoteApplication;
import com.mars.note.R;
import com.mars.note.api.GridPaperItemImg;
import com.mars.note.api.GridViewPaperItemForBatchDelete;
import com.mars.note.api.Logg;
import com.mars.note.database.NoteDataBaseManager;
import com.mars.note.database.NoteRecord;
import com.mars.note.utils.PictureHelper;
import com.mars.note.views.MyGridView;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.app.Fragment;
import android.support.v4.util.LruCache;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.TextView;

/**
 * 表示GridView的每一页，是一个Fragment
 * 
 * @author mars
 */
public class GridViewPaperItem extends Fragment {
	private static final String TAG = "GridViewPaper";
	private Activity mActivity;
	private NoteDataBaseManager noteDBManager;
	private int index = -1;
	private MyGridView gridView;
	private TextView indexText;
	private BaseAdapter mAdapter;
	private List<NoteRecord> list_note_data;
	private ExecutorService executors;
	private LruCache<String, Bitmap> mBitmapCache;
	private ArrayList<com.mars.note.api.GridViewPaperItemForBatchDelete> mGridViewBatchDeleteCache;
	private CallBack mCallBack;
	private ConcurrentHashMap<String, Boolean> mConcurentMap;
	private ReentrantLock mLock;

	public void setCallBack(CallBack cb) {
		mCallBack = cb;
	}

	public GridViewPaperItem() {
	}

	public void setBitmapCache() throws NullPointerException {
		mBitmapCache = NoteApplication.getBitmapCache();
		if (mBitmapCache == null) {
			throw new NullPointerException("mBitmapCache cant be null");
		}
	}

	@Override
	public void onAttach(Activity mActivity) {
		super.onAttach(mActivity);
		this.mActivity = mActivity;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initDBManager();
		index = getArguments().getInt("index") + 1;
		setBitmapCache();
		executors = NoteApplication.getExecutors();
		mConcurentMap = new ConcurrentHashMap<String, Boolean>();// 保存线程工作状态
		mLock = new ReentrantLock(true);// 20141212 公平锁
		mGridViewBatchDeleteCache = NoteApplication
				.getmGridViewBatchDeleteCache();
	}

	private void initDBManager() {
		noteDBManager = NoteApplication.getDbManager();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_gridview_paper, container,
				false);
		gridView = (MyGridView) v.findViewById(R.id.grid_view);
		indexText = (TextView) v.findViewById(R.id.index);
		indexText.setText(getString(R.string.grid_pager_index, (index)));
		list_note_data = noteDBManager
				.queryDividedPagerRecords(1, index - 1, 6);
		mAdapter = new BaseAdapter() {
			LayoutInflater inflate = LayoutInflater.from(GridViewPaperItem.this
					.getActivity());

			@Override
			public int getCount() {
				return list_note_data.size();
			}

			@Override
			public NoteRecord getItem(int arg0) {
				// TODO Auto-generated method stub
				return list_note_data.get(arg0);
			}

			@Override
			public long getItemId(int arg0) {
				// TODO Auto-generated method stub
				return arg0;
			}

			@Override
			public View getView(int pos, View convertView, ViewGroup parent) {

				ViewHolder holder = null;
				if (convertView == null) {
					convertView = inflate.inflate(R.layout.gridview_item,
							parent, false);
					holder = new ViewHolder();
					holder.position = pos;
					holder.date = (TextView) convertView
							.findViewById(R.id.date);
					holder.title = (TextView) convertView
							.findViewById(R.id.title);
					holder.content = (TextView) convertView
							.findViewById(R.id.content);
					holder.img = (ImageView) convertView
							.findViewById(R.id.note_listitem_img);
					convertView.setTag(holder);
				} else {
					holder = (ViewHolder) convertView.getTag();
				}
				if (gridView.onMeasure) {
					return convertView;
				}
				final NoteRecord nr = getItem(pos);
				final View bg = convertView;
				if (RecentRecordsFragment.isDeleteUIShown) {
					int arrayPos = pos + (index - 1) * 6;
					GridViewPaperItemForBatchDelete item = mGridViewBatchDeleteCache
							.get(arrayPos);
					if (item.index == (index - 1) && item.position == pos) {
						if (item.checked) {
							bg.setBackgroundResource(R.drawable.rounded_rectangle_pressed_gray);
						} else {
							bg.setBackgroundResource(R.drawable.list_item_bg_normal);
						}
					}

				}
				convertView.setOnLongClickListener(new OnLongClickListener() {
					@Override
					public boolean onLongClick(View v) {
						if (!RecentRecordsFragment.isDeleteUIShown) {
							int position = ((ViewHolder) v.getTag()).position;
							int arrayPos = position + (index - 1) * 6;
							GridViewPaperItemForBatchDelete item = mGridViewBatchDeleteCache
									.get(arrayPos);
							if (item.index == (index - 1)
									&& item.position == position) {
								item.checked = !item.checked;
								if (item.checked) {
									bg.setBackgroundResource(R.drawable.rounded_rectangle_pressed_gray);
								} else {
									bg.setBackgroundResource(R.drawable.list_item_bg_normal);
								}
							}
							if (mCallBack != null) {
								mCallBack.showDeleteUI();
							}
						} else {
							int position = ((ViewHolder) v.getTag()).position;
							int arrayPos = position + (index - 1) * 6;
							GridViewPaperItemForBatchDelete item = mGridViewBatchDeleteCache
									.get(arrayPos);
							if (item.index == (index - 1)
									&& item.position == position) {
								item.checked = !item.checked;
								if (item.checked) {
									bg.setBackgroundResource(R.drawable.rounded_rectangle_pressed_gray);
								} else {
									bg.setBackgroundResource(R.drawable.list_item_bg_normal);
								}
							}
						}
						return true;
					}
				});
				convertView.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						int position = ((ViewHolder) v.getTag()).position;
						int arrayPos = position + (index - 1) * 6;

						if (RecentRecordsFragment.isDeleteUIShown) {
							GridViewPaperItemForBatchDelete item = mGridViewBatchDeleteCache
									.get(arrayPos);
							if (item.index == (index - 1)
									&& item.position == position) {
								item.checked = !item.checked;
								if (item.checked) {
									bg.setBackgroundResource(R.drawable.rounded_rectangle_pressed_gray);
								} else {
									bg.setBackgroundResource(R.drawable.list_item_bg_normal);
								}
							}

						} else {
							Intent editNote = new Intent(mActivity,
									Editor.class);
							String id = nr.id;
							editNote.putExtra("note_id", id);
							mActivity.startActivity(editNote);
						}
					}
				});
				long msTime = Long.parseLong(nr.time);
				Calendar calendar = Calendar.getInstance();
				calendar.setTimeInMillis(msTime);
				String nowDate = calendar.get(Calendar.YEAR) + "."
						+ (calendar.get(Calendar.MONTH) + 1) + "."
						+ calendar.get(Calendar.DAY_OF_MONTH);
				holder.date.setText(nowDate);
				holder.title.setText(nr.title);
				holder.content.setText(nr.content);
				final String path = nr.imgpath;
				if (parent.getChildCount() == pos) {
					if (path != null && (!path.equals("null"))) {
						// 20141125 有图的item content的行数设置为单行
						holder.content.setMaxLines(1);
						// 缓存在NoteApplication初始化
						if (mBitmapCache == null) {
							throw new NullPointerException(
									"mBitmapCache cant be null");
						}
						// 从主线程进入
						if (mBitmapCache.get(path) == null) {
							// 缓存中没有找到图片
							final GridPaperItemImg item = new GridPaperItemImg();
							item.index = index;
							item.position = pos;

							final ImageView img = holder.img;
							img.setTag(Integer.valueOf(pos));
							item.bm = mBitmapCache.get(path);
							if (item.bm != null) {
								// 如果碰巧有线程加载图片到缓存
								img.setVisibility(View.VISIBLE);
								img.setImageBitmap(item.bm);
							} else {
								// 初始化mConcurentMap
								if (mConcurentMap.get(path) == null)
									mConcurentMap.put(path, false);

								final Handler handler = new Handler() {
									@Override
									public void handleMessage(Message msg) {
										if (msg.what == 1) {
											if (((Integer) img.getTag())
													.intValue() == item.position) {
												if (item.bm == null) {
													return;
												}
												img.setVisibility(View.VISIBLE);
												img.setImageBitmap(item.bm);
												Logg.D(path + " is load new");
											}
										} else if (msg.what == 2) {
											if (((Integer) img.getTag())
													.intValue() == item.position) {
												if (item.bm == null) {
													return;
												}
												img.setVisibility(View.VISIBLE);
												img.setImageBitmap(item.bm);
												Logg.D(path
														+ " is load from cache");
											}
										}
									}
								};
								Thread thread = new Thread() {
									@Override
									public void run() {
										// 此处线程同步，第一个线程工作，其他等待
										mLock.lock();
										if (mConcurentMap.get(path) == false) {
											// Logg.D("mConcurentMap.get(path) = "+mConcurentMap.get(path));
											mConcurentMap.put(path, true);
											mLock.unlock();
//											Logg.D("mConcurentMap.get(path) = "
//													+ mConcurentMap.get(path));

											if (path != null
													&& (!path.equals("null"))
													&& (!"".equals(path))) {
												item.bm = PictureHelper
														.getCropImage(path,
																300, true, 100,
																mActivity, 7,
																true);
												mBitmapCache.put(path, item.bm);
												mConcurentMap.put(path, false);

											} else {
												item.bm = null;
												mConcurentMap.put(path, false);
											}
											Message msg = new Message();
											msg.what = 1;
											handler.sendMessage(msg);
										} else {
											mLock.unlock();
											while (true) {
												if (mConcurentMap.get(path) == false) {
													// Logg.D("other need break cycle");
													break;
												}
											}
											// Logg.D("other go out cycle");
											if (mBitmapCache.get(path) != null) {
												item.bm = mBitmapCache
														.get(path);
												Message msg = new Message();
												msg.what = 2;
												handler.sendMessage(msg);
											} else {
												throw new NullPointerException(
														"(mBitmapCache.get(path) == null!");
											}
										}
									}
								};
								executors.execute(thread);

							}
						} else {
							if (mBitmapCache.get(path) != null) {
								holder.img.setImageBitmap(mBitmapCache
										.get(path));
								holder.img.setVisibility(View.VISIBLE);
								Logg.D(path + " is from cache");
							} else {
								throw new NullPointerException(
										"(mBitmapCache.get(path) == null!");
							}
						}

					} else {
						holder.img.setVisibility(View.GONE);
						// 20141125 有图的item content的行数设置为多行
						holder.content.setMaxLines(6);
					}
				}
				// }
				return convertView;
			}

			class ViewHolder {
				int position;
				TextView date;
				TextView title;
				TextView content;
				ImageView img;
			}
		};
		gridView.setAdapter(mAdapter);
		return v;
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG, "onResume");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "onDestroy");
	}

	interface CallBack {
		public abstract void showDeleteUI();
	}
}
