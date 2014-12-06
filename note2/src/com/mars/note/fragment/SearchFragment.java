package com.mars.note.fragment;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.mars.note.Config;
import com.mars.note.Editor;
import com.mars.note.FragmentCallBack;
import com.mars.note.Main;
import com.mars.note.NoteApplication;
import com.mars.note.R;
import com.mars.note.database.NoteDataBaseManager;
import com.mars.note.database.NoteRecord;
import com.mars.note.utils.AnimationHelper;
import com.mars.note.utils.PictureHelper;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v4.app.Fragment;
import android.support.v4.util.LruCache;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

public class SearchFragment extends Fragment implements OnClickListener,
		SearchView.OnQueryTextListener {
	private static final String TAG = "SearchFragment";
	private Activity mActivity;
	private NoteDataBaseManager noteDBManager;
	private SearchView mSearchView;
	private TextView txt;
	private ListView searchList;
	private BaseAdapter searchListAdapter;
	private List<NoteRecord> result_list;
	private String[] date_title;
	private String[] time_title;
	public boolean isAddNoteReturn = false;
	private String query;
	private static LruCache<String, Bitmap> mBitmapCache;
	private ExecutorService executors;
	private View root;

	@Override
	public void onAttach(Activity mActivity) {
		super.onAttach(mActivity);
		this.mActivity = mActivity;
		setCallBack((FragmentCallBack)mActivity);
		date_title = mActivity.getResources()
				.getStringArray(R.array.date_title);
		time_title = mActivity.getResources()
				.getStringArray(R.array.time_title);
	}

	FragmentCallBack mCallBack;

	public void setCallBack(FragmentCallBack mCallBack) {
		this.mCallBack = mCallBack;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initDBManager();
		mBitmapCache = NoteApplication.getBitmapCache();
		// mGridPaperMemoryCache = NoteApplication.getGridPaperMemoryCache();
		executors = NoteApplication.getExecutors();
		if (executors == null) {
			executors = Executors
					.newFixedThreadPool(RecentRecordsFragment.thread_num);
			NoteApplication.setExecutors(executors);
		}
	}

	private void initDBManager() {
		noteDBManager = NoteApplication.getDbManager();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_search, container, false);
		mSearchView = (SearchView) v.findViewById(R.id.searchView);
		mSearchView.onActionViewExpanded();
		mSearchView.clearFocus();
		mSearchView.setOnQueryTextListener(this);
		txt = (TextView) mSearchView.findViewById(mActivity.getResources()
				.getIdentifier("android:id/search_src_text", null, null));
		txt.setTextColor(Color.DKGRAY);
		searchList = (ListView) v.findViewById(R.id.search_list);
		root = v;
		return v;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mSearchView != null) {
			mSearchView.clearFocus();
		}
		if (query != null && query.length() != 0 || Config.search_needRefresh) {
			result_list = noteDBManager.querySelectedRecords(query);
			searchListAdapter = new SearchListAdapter(result_list);
			searchList.setAdapter(searchListAdapter);
			Config.search_needRefresh = false;
		}
		isAddNoteReturn = false;
//		Log.d(TAG, "onResume");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
//		Log.d(TAG, "onDestroy");
	}

	@Override
	public void onClick(View v) {
		if (v.getTag() != null) {
			int position = ((Integer) v.getTag()).intValue();
			NoteRecord nr = result_list.get(position);
			Intent editNote = new Intent(mActivity, Editor.class);
			String id = nr.id;
			editNote.putExtra("note_id", id);
			mActivity.startActivity(editNote);
		}
	}

	private class SearchListAdapter extends BaseAdapter {
		LayoutInflater inflater = LayoutInflater.from(getActivity());
		List<NoteRecord> list;

		public SearchListAdapter(List<NoteRecord> result_list) {
			list = result_list;
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return list.size();
		}

		@Override
		public Object getItem(int arg0) {
			// TODO Auto-generated method stub
			return list.get(arg0);
		}

		@Override
		public long getItemId(int arg0) {
			// TODO Auto-generated method stub
			return arg0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder = null;
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.search_list_item,
						parent, false);
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
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			holder.img.setVisibility(View.GONE);
			holder.position = position;

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
			// convertView.setTag(Integer.valueOf(position));
			convertView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent editNote = new Intent(mActivity, Editor.class);
					String id = nr.id;
					editNote.putExtra("note_id", id);
					mActivity.startActivity(editNote);
				}
			});
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

	@Override
	public boolean onQueryTextChange(String arg0) {
		query = arg0;
		refreshListView();
		return false;
	}
	
	private void refreshListView(){
		if (query != null && query.length() != 0 && !query.equals("")) {
			result_list = noteDBManager.querySelectedRecords(query);
			searchListAdapter = new SearchListAdapter(result_list);
			searchList.setAdapter(searchListAdapter);
		} else {
			result_list.clear();
			searchListAdapter = new SearchListAdapter(result_list);
			searchList.setAdapter(searchListAdapter);
		}
	}

	@Override
	public boolean onQueryTextSubmit(String arg0) {
		query = arg0;
		refreshListView();
		return false;
	}

	private class ViewHolder {
		int position;
		TextView date;
		TextView time;
		TextView title;
		TextView content;
		ViewGroup titleAndContent;
		ImageView img;
	}

	private class ListItemImg {
		public int position;
		public Bitmap bm;
	}
}
