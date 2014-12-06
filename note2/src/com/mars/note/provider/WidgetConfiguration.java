package com.mars.note.provider;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.mars.note.Editor;
import com.mars.note.LoginActivity;
import com.mars.note.NoteApplication;
import com.mars.note.R;
import com.mars.note.database.NoteDataBaseManager;
import com.mars.note.database.NoteRecord;
import com.mars.note.fragment.RecentRecordsFragment;
import com.mars.note.fragment.SearchFragment;
import com.mars.note.utils.PictureHelper;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.SearchView;
import android.widget.TextView;

public class WidgetConfiguration extends Activity implements
		View.OnClickListener, SearchView.OnQueryTextListener {
	int appWidgetId;
	View relate;
	View addNew;
	TextView txt;
	ListView lv;
	SearchView searchView;
	String query;
	List<NoteRecord> result_list;
	BaseAdapter searchListAdapter;
	String[] date_title;
	String[] time_title;
	NoteDataBaseManager noteDBManager;
	OnClickListener onClickListener;
	private static LruCache<String, Bitmap> mBitmapCache;
	private ExecutorService executors;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.widget_configuration);
		relate = this.findViewById(R.id.relate);
		relate.setOnClickListener(this);
		addNew = this.findViewById(R.id.add_new);
		addNew.setOnClickListener(this);
		searchView = (SearchView) this.findViewById(R.id.searchView);
		lv = (ListView) this.findViewById(R.id.search_list);
		result_list = new ArrayList<NoteRecord>();
		this.setResult(this.RESULT_CANCELED);
		Intent intent = this.getIntent();
		Bundle extras = intent.getExtras();
		if (extras != null) {
			this.appWidgetId = extras.getInt(
					AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
		}
		if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
			finish();
		}
		mBitmapCache = NoteApplication.getBitmapCache();
		// mGridPaperMemoryCache = NoteApplication.getGridPaperMemoryCache();
		executors = NoteApplication.getExecutors();
		if (executors == null) {
			executors = Executors
					.newFixedThreadPool(RecentRecordsFragment.thread_num);
			NoteApplication.setExecutors(executors);
		}
	}
	
	private void validate() {
		Intent validate = new Intent(this,LoginActivity.class);
		Bundle bundle = new Bundle();
		bundle.putInt("mode",0);
		validate.putExtras(bundle);
		this.startActivityForResult(validate, 0);		
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == 0){
			if(resultCode == RESULT_CANCELED){
				this.finish(); //20141202 验证失败则退出
			}else if(resultCode == RESULT_OK){
				relate();
			}
		}
	}
	
	private void relate(){
		noteDBManager = NoteApplication.getDbManager();
		date_title = this.getResources().getStringArray(R.array.date_title);
		time_title = this.getResources().getStringArray(R.array.time_title);
		addNew.setVisibility(View.GONE);
		relate.setVisibility(View.GONE);
		searchView.setVisibility(View.VISIBLE);
		lv.setVisibility(View.VISIBLE);
		searchView.onActionViewExpanded();
		searchView.clearFocus();
		searchView.setOnQueryTextListener(this);
		txt = (TextView) searchView.findViewById(this.getResources()
				.getIdentifier("android:id/search_src_text", null, null));
		txt.setTextColor(Color.DKGRAY);
		View bg = (View) searchView.getParent();
		bg.setBackgroundResource(R.drawable.bg);
		result_list.clear();
		result_list = noteDBManager.queryAllRecords();
		searchListAdapter = new SearchListAdapter(result_list);
		lv.setAdapter(searchListAdapter);
		onClickListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (v.getTag() != null) {
					final ViewHolder holder = (ViewHolder) v.getTag();
					AlertDialog.Builder builder = new AlertDialog.Builder(
							WidgetConfiguration.this);
					builder.setMessage(R.string.widget_relate_to_others_dialog_message);
					builder.setPositiveButton(R.string.yes,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface arg0,
										int arg1) {
									noteDBManager.addWidgetRelation(
											String.valueOf(appWidgetId),
											holder.id);
									Intent i2 = new Intent();
									i2.putExtra(
											AppWidgetManager.EXTRA_APPWIDGET_ID,
											appWidgetId);
									setResult(Activity.RESULT_OK, i2);
									Intent intent = new Intent(
											"com.mars.note.widget.relate");
									intent.putExtra(
											AppWidgetManager.EXTRA_APPWIDGET_ID,
											appWidgetId);
									WidgetConfiguration.this
											.sendBroadcast(intent);
									finish();
								}
							});
					builder.setNegativeButton(R.string.no, null);
					builder.show();
				}
			}
		};
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.add_new:
			Intent i1 = new Intent();
			i1.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
			setResult(WidgetConfiguration.this.RESULT_OK, i1);
			finish();
			break;
		case R.id.relate:
			validate();
			break;
		}
	}

	@Override
	public boolean onQueryTextChange(String arg0) {
		query = arg0;
		if (query != null && query.length() != 0) {
			result_list = noteDBManager.querySelectedRecords(query);
			searchListAdapter = new SearchListAdapter(result_list);
			lv.setAdapter(searchListAdapter);
		} else {
			result_list.clear();
			result_list = noteDBManager.queryAllRecords();
			searchListAdapter = new SearchListAdapter(result_list);
			lv.setAdapter(searchListAdapter);
		}
		return false;
	}

	@Override
	public boolean onQueryTextSubmit(String arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	private class SearchListAdapter extends BaseAdapter {
		LayoutInflater inflater = LayoutInflater.from(WidgetConfiguration.this);
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
			holder.id = nr.id;
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
											400, true, 100,
											WidgetConfiguration.this, 7, true);
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
						// Log.d("test", "executors.execute");
					}
				} else {
					if (mBitmapCache.get(path) != null) {
						holder.img.setImageBitmap(mBitmapCache.get(path));
						holder.img.setVisibility(View.VISIBLE);
					} else {
						// Log.d("test3", "item.bm has been recycled");
					}
				}
			} else {
				LayoutParams param0 = holder.img.getLayoutParams();
				holder.img.setVisibility(View.GONE);
				LayoutParams param1 = holder.titleAndContent.getLayoutParams();
				// Log.d("param", "param.width = " + param.width);
				param1.width = 500 + param0.width;
				// img.setLayoutParams(null);
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

			convertView.setOnClickListener(onClickListener);
			// convertView.setTag(nr);
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

	private class ViewHolder {
		int position;
		TextView date;
		TextView time;
		TextView title;
		TextView content;
		ViewGroup titleAndContent;
		ImageView img;
		String id;
	}

	private class ListItemImg {
		public int position;
		public Bitmap bm;
	}
}
