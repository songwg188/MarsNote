package com.mars.note.provider;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.mars.note.Editor;
import com.mars.note.NoteApplication;
import com.mars.note.R;
import com.mars.note.database.NoteDataBaseManager;
import com.mars.note.database.NoteRecord;
import com.mars.note.fragment.RecentRecordsFragment;
import com.mars.note.utils.PictureHelper;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService.RemoteViewsFactory;

public class MyRemoteViewsFactory implements RemoteViewsFactory {
	final String TAG = "MyRemoteViewsFactory";
	public Context context;
	public Intent intent;
	private int widgetID;
	List<NoteRecord> datas;
	NoteDataBaseManager noteDBManager;
	private static LruCache<String, Bitmap> mBitmapCache;

	public MyRemoteViewsFactory(Context applicationContext, Intent intent) {
		context = applicationContext;
		this.intent = intent;
		widgetID = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
				AppWidgetManager.INVALID_APPWIDGET_ID);

	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return datas.size();
	}

	@Override
	public long getItemId(int pos) {
		// TODO Auto-generated method stub
		return pos;
	}

	@Override
	public RemoteViews getLoadingView() {
		return null;
	}

	@Override
	public RemoteViews getViewAt(int pos) {
		RemoteViews views = new RemoteViews(context.getPackageName(),
				R.layout.widget_layout);
		if (datas.size() != 0) {
			NoteRecord nr = datas.get(pos);
			String title = nr.title;
			String content = nr.content;
			String path = nr.imgpath;
			Bitmap bm = null;

			if (mBitmapCache == null) {
				throw new NullPointerException("mBitmapCache cant be null");
			}
			if (path != null && (!path.equals("null")) && (!"".equals(path))) {
				if (mBitmapCache.get(path) == null) {

					bm = PictureHelper.getCropImage(path, 400, true, 100,
							context, 7, true);
					mBitmapCache.put(path, bm);

				} else {
					if (mBitmapCache.get(path) != null) {
						bm = mBitmapCache.get(path);
					} else {
						// Log.d("test3", "item.bm has been recycled");
					}
				}
			}

			if (bm != null) {
				views.setViewVisibility(R.id.img, View.VISIBLE);
				views.setImageViewBitmap(R.id.img, bm);
				views.setTextViewText(R.id.content, content);
				views.setViewVisibility(R.id.content, View.VISIBLE);
				views.setViewVisibility(R.id.content2, View.GONE);
			} else {
				views.setTextViewText(R.id.content2, content);
				views.setViewVisibility(R.id.img, View.GONE);//20141126 服用conertview导致图片被重用
				views.setViewVisibility(R.id.content2, View.VISIBLE);
				views.setViewVisibility(R.id.content, View.GONE);
			}

			views.setTextViewText(R.id.title, title);
			long msTime = Long.parseLong(nr.time);
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(msTime);
			String date = calendar.get(Calendar.YEAR) + "."
					+ (calendar.get(Calendar.MONTH) + 1) + "."
					+ calendar.get(Calendar.DAY_OF_MONTH);
			views.setTextViewText(R.id.date, date);

			Intent intent = new Intent(context, Editor.class);
			intent.putExtra("note_id", nr.id);
			views.setOnClickFillInIntent(R.id.root, intent);
			// to fill PendingIntent in NoteCollectionsWidgetProvider
		}

		return views;
	}

	@Override
	public int getViewTypeCount() {
		// TODO Auto-generated method stub
		return 1;
	}

	@Override
	public boolean hasStableIds() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onCreate() {
		noteDBManager = NoteApplication.getDbManager();
		mBitmapCache = NoteApplication.getBitmapCache();

	}

	@Override
	public void onDataSetChanged() {
		datas = noteDBManager.queryDividedPagerRecords(1, 0, 10);
	}

	@Override
	public void onDestroy() {
		datas = null;
	}

}
