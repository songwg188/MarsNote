package com.mars.note.fragment2;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.locks.ReentrantLock;

import com.mars.note.R;
import com.mars.note.api.Config;
import com.mars.note.api.GridPaperItemImg;
import com.mars.note.api.GridViewPaperItemForBatchDelete;
import com.mars.note.api.ImageSpanInfo;
import com.mars.note.app.EditorActivity;
import com.mars.note.app.MarsNoteActivity;
import com.mars.note.app.NoteApplication;
import com.mars.note.database.NoteDataBaseManager;
import com.mars.note.database.NoteRecord;
import com.mars.note.fragment.RecentFragment;
import com.mars.note.utils.Logg;
import com.mars.note.utils.PictureHelper;
import com.mars.note.utils.Util;
import com.mars.note.views.BounceListView;
import com.mars.note.views.MyGridView;

import android.app.Activity;
import android.content.Context;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.TextView;

public class GridViewItemFragment extends BaseFragment {
	private static final String TAG = "GridViewPaper";
	private static final boolean DEBUG = true;
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
	private ReentrantLock mLock;// 同步锁

	public GridViewItemFragment() {
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
		mConcurentMap = NoteApplication.getmConcurentMap();// 保存线程工作状态
		mLock = NoteApplication.getmLock();// 20141212 公平锁
		mGridViewBatchDeleteCache = RecentFragment.getGridViewBatchDeleteCache();
	}

	private void initDBManager() {
		noteDBManager = NoteApplication.getDbManager();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_gridview_paper, container, false);
		gridView = (MyGridView) v.findViewById(R.id.grid_view);
		indexText = (TextView) v.findViewById(R.id.index);
		indexText.setText(getString(R.string.grid_pager_index, (index)));
		list_note_data = noteDBManager.queryDividedPagerRecords(1, index - 1, 6);
		mAdapter = new BaseAdapter() {
			LayoutInflater inflate = LayoutInflater.from(GridViewItemFragment.this.getActivity());
			private Handler handler = new Handler();

			@Override
			public int getCount() {
				return list_note_data.size();
			}

			@Override
			public NoteRecord getItem(int arg0) {
				return list_note_data.get(arg0);
			}

			@Override
			public long getItemId(int arg0) {
				return arg0;
			}

			private void setListener(final View convertView, final NoteRecord nr) {
				// 长按监听
				OnLongClickListener mOnLongClickListener = new OnLongClickListener() {
					@Override
					public boolean onLongClick(View v) {
						if (!RecentFragment.isDeleteUIShown) {
							int position = ((ViewHolder) v.getTag()).position;
							int arrayPos = position + (index - 1) * 6;
							GridViewPaperItemForBatchDelete item = mGridViewBatchDeleteCache.get(arrayPos);
							if (item.index == (index - 1) && item.position == position) {
								item.checked = !item.checked;
								if (item.checked) {
									convertView.setBackgroundResource(R.drawable.rounded_rectangle_pressed_gray);
								} else {
									convertView.setBackgroundResource(R.drawable.list_item_bg_normal);
								}
							}
							if (mCallBack != null) {
								mCallBack.showDeleteUI();
							}
						} else {
							int position = ((ViewHolder) v.getTag()).position;
							int arrayPos = position + (index - 1) * 6;
							GridViewPaperItemForBatchDelete item = mGridViewBatchDeleteCache.get(arrayPos);
							if (item.index == (index - 1) && item.position == position) {
								item.checked = !item.checked;
								if (item.checked) {
									convertView.setBackgroundResource(R.drawable.rounded_rectangle_pressed_gray);
								} else {
									convertView.setBackgroundResource(R.drawable.list_item_bg_normal);
								}
							}
						}
						return true;
					}
				};
				convertView.setOnLongClickListener(mOnLongClickListener);
				// 点击监听
				OnClickListener mOnClickListener = new OnClickListener() {
					@Override
					public void onClick(View v) {
						int position = ((ViewHolder) v.getTag()).position;
						int arrayPos = position + (index - 1) * 6;

						if (RecentFragment.isDeleteUIShown) {
							GridViewPaperItemForBatchDelete item = mGridViewBatchDeleteCache.get(arrayPos);
							if (item.index == (index - 1) && item.position == position) {
								item.checked = !item.checked;
								if (item.checked) {
									convertView.setBackgroundResource(R.drawable.rounded_rectangle_pressed_gray);
								} else {
									convertView.setBackgroundResource(R.drawable.list_item_bg_normal);
								}
							}
						} else {
							Intent editNote = new Intent(mActivity, EditorActivity.class);
							String id = nr.id;
							editNote.putExtra("note_id", id);
							mActivity.startActivity(editNote);
						}
					}
				};
				convertView.setOnClickListener(mOnClickListener);
			}

			@Override
			public View getView(int pos, View convertView, ViewGroup parent) {

				ViewHolder holder = null;
				if (convertView == null) {
					convertView = inflate.inflate(R.layout.gridview_item, parent, false);
					holder = new ViewHolder();
					holder.position = pos;
					holder.date = (TextView) convertView.findViewById(R.id.date);
					holder.title = (TextView) convertView.findViewById(R.id.title);
					holder.content = (TextView) convertView.findViewById(R.id.content);
					holder.img = (ImageView) convertView.findViewById(R.id.note_listitem_img);
					holder.imgs = (BounceListView) convertView.findViewById(R.id.note_listitem_imgs);
					convertView.setTag(holder);
				} else {
					// debug 中没有发现holder被复用的情况，因此
					// holder.postion不用重新设置，注意和ListView复用ConvertView不同
					holder = (ViewHolder) convertView.getTag();
				}
				if (gridView.onMeasure) {
					return convertView;
				}
				final NoteRecord nr = getItem(pos);
				final View bg = convertView;
				if (RecentFragment.isDeleteUIShown) {
					int arrayPos = pos + (index - 1) * 6;
					GridViewPaperItemForBatchDelete item = mGridViewBatchDeleteCache.get(arrayPos);
					if (item.index == (index - 1) && item.position == pos) {
						if (item.checked) {
							bg.setBackgroundResource(R.drawable.rounded_rectangle_pressed_gray);
						} else {
							bg.setBackgroundResource(R.drawable.list_item_bg_normal);
						}
					}
				}
				setListener(convertView, nr);
				long msTime = Long.parseLong(nr.time);
				Calendar calendar = Calendar.getInstance();
				calendar.setTimeInMillis(msTime);
				String nowDate = calendar.get(Calendar.YEAR) + "." + (calendar.get(Calendar.MONTH) + 1) + "." + calendar.get(Calendar.DAY_OF_MONTH);
				holder.date.setText(nowDate);

				holder.title.setVisibility(View.GONE);
				holder.content.setVisibility(View.GONE);

				byte[] data = nr.imageSpanInfos;
				if (data != null) {
					/*
					 * 存在图片的情况： 1、有图 3、图片本地已被删除
					 */
					ArrayList<ImageSpanInfo> imageSpanInfoList = Util.getImageSpanInfoListFromBytes(data);
					
					if (imageSpanInfoList.size() > 0) {
						// 1、只有一张图的情况
						holder.title.setVisibility(View.VISIBLE);
						holder.content.setVisibility(View.VISIBLE);
						holder.title.setText(nr.title);
						String content = nr.content;
						content = Util.filterContent(mActivity,content, imageSpanInfoList);
						holder.content.setText(content);

						final String path = nr.imgpath;
						if (parent.getChildCount() == pos) {
							if (path != null && (!path.equals("null")) && (!"".equals(path))) {
								// 20141125 有图的item content的行数设置为单行
								holder.content.setMaxLines(1);
								// 缓存在NoteApplication初始化
								if (mBitmapCache == null) {
									throw new NullPointerException("mBitmapCache cant be null");
								}
								// 从主线程进入
								if (mBitmapCache.get(path) == null) {
									// 缓存中没有找到图片
									final GridPaperItemImg item = new GridPaperItemImg();
									item.index = index;
									item.position = pos;

									final ImageView img = holder.img;
									final TextView contentV = holder.content;
									img.setTag(Integer.valueOf(pos));
									item.bm = mBitmapCache.get(path);
									if (item.bm != null) {
										// 如果碰巧有线程加载图片到缓存
										img.setVisibility(View.VISIBLE);
										img.setImageBitmap(item.bm);
									} else {
										// 初始化键值对
										mLock.lock();
										if (mConcurentMap.get(path) == null)
											mConcurentMap.put(path, false);
										mLock.unlock();

										// 20141215
										// 只需要runnable对象，将交给线程池内部的线程
										Runnable runnable = new Runnable() {
											@Override
											public void run() {
												// 此处线程同步，第一个线程工作，其他等待
												mLock.lock();
												if (mConcurentMap.get(path) == false) {
													mConcurentMap.put(path, true);
													mLock.unlock();
													if (path != null && (!path.equals("null")) && (!"".equals(path))) {
														if (Config.DB_SAVE_MODE) {
															//从数据库读图
															item.bm = noteDBManager.getCroppedImage(path);
															if(item.bm == null)
																throw new NullPointerException("bm null error");
														} else {
															item.bm = PictureHelper.getCropImage(path, getResources().getDimension(R.dimen.listview_image_width),
																	getResources().getDimension(R.dimen.listview_image_height), true, 100, mActivity, 7, true);
														}
														if (item.bm != null) {
															mBitmapCache.put(path, item.bm);
														} else {
															// 20141216
															// 预览图已经删除
															// 需要更新数据库 并刷新
															// start
															if (DEBUG)
																Logg.I("GridView img not exsited");
															noteDBManager.deleteImagePath(nr);
															mConcurentMap.put(path, false);
															// 20141216
															// 预览图已经删除
															// 需要更新数据库 并刷新
															// end
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
																// 20141219
																// 如果图片读不到就隐藏imageview，加多textview行数
																if (item.bm == null) {
																	img.setVisibility(View.GONE);
																	// 20141125
																	// 有图的item
																	// content的行数设置为多行
																	contentV.setMaxLines(6);
																	return;
																}
																// 20141219
																// 如果图片读不到就隐藏imageview，加多textview行数
																img.setVisibility(View.VISIBLE);
																img.setImageBitmap(item.bm);
																if (DEBUG)
																	Logg.I("GridView" + path + " is load new");
															} else {
																throw new IllegalStateException("((Integer) img.getTag()).intValue() != item.position");
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
																	if (item.bm == null) {
																		throw new NullPointerException("item.bm==null");
																	}
																	img.setVisibility(View.VISIBLE);
																	img.setImageBitmap(item.bm);
																	if (DEBUG)
																		Logg.I("GridView " + path + " is load from cache");
																} else {
																	throw new IllegalStateException("((Integer) img.getTag()).intValue() != item.position");
																}
															}
														});
													} else {
														if (DEBUG)
															Logg.I("GridView img not exsited");
													}
												}
											}
										};
										try {
											executors.execute(runnable);
										} catch (RejectedExecutionException e) {
											e.printStackTrace();
										}

									}
								} else {
									if (mBitmapCache.get(path) != null) {
										holder.img.setImageBitmap(mBitmapCache.get(path));
										holder.img.setVisibility(View.VISIBLE);
										if (DEBUG)
											Logg.I(path + " is from cache");
									} else {
										throw new NullPointerException("(mBitmapCache.get(path) == null!");
									}
								}

							} else {
								holder.img.setVisibility(View.GONE);
								// 20141125 无图的item content的行数设置为多行
								holder.content.setMaxLines(6);
							}
						}
					}
				} else {
					// 无图的情况
					holder.title.setVisibility(View.VISIBLE);
					holder.content.setVisibility(View.VISIBLE);
					holder.title.setText(nr.title);
					// 20141215 过滤图片字符串 start
					String content = nr.content;
					holder.content.setText(content);
					// 20141125 无图的item content的行数设置为多行
					holder.content.setMaxLines(6);
				}

				return convertView;
			}

			class ViewHolder {
				int position;
				TextView date;
				TextView title;
				TextView content;
				ImageView img;
				BounceListView imgs;
			}
		};
		gridView.setAdapter(mAdapter);
		return v;
	}

	@Override
	public void onResume() {
		super.onResume();
		if(DEBUG)
		Log.d(TAG, "onResume");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if(DEBUG)
		Log.d(TAG, "onDestroy");
	}

	public interface CallBack extends com.mars.note.fragment2.BaseFragment.CallBack {
		public abstract void showDeleteUI();
	}

	@Override
	public void setCallBack(com.mars.note.fragment2.BaseFragment.CallBack cb) {
		mCallBack = (CallBack) cb;
	}
}
