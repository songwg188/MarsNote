package com.mars.note.provider;

import java.util.Calendar;
import com.mars.note.Editor;
import com.mars.note.NoteApplication;
import com.mars.note.R;
import com.mars.note.database.NoteDataBaseManager;
import com.mars.note.database.NoteRecord;
import com.mars.note.utils.PictureHelper;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

public class NoteWidgetProvider extends AppWidgetProvider {
	final String TAG = "NoteWidgetProvider";
	NoteDataBaseManager noteDBManager;

	// run this when receive a broadcast
	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		AppWidgetManager appWidgetManager = AppWidgetManager
				.getInstance(context);
//		Log.d("widget", "onReceive" + " " + intent.getAction());
		if (noteDBManager == null) {
			noteDBManager = NoteApplication.getDbManager();
		}
		if ("com.mars.note.widget.refresh".equals(intent.getAction())) {
			int widgetID = intent.getIntExtra(
					AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
			if (widgetID != -1) {
//				Log.d("widget", "onReceive " + " widgetID = " + widgetID);
				String noteId = noteDBManager
						.querySingleRecordIDByWidgetID(String.valueOf(widgetID));
//				Log.d("widget", "noteId = " + noteId);
				NoteRecord nr = noteDBManager.querySingleRecord(noteId);
				if (nr != null) {
					String title = nr.title;
					String content = nr.content;
					String path = nr.imgpath;
					Bitmap bm = null;
					if (path != null && (!path.equals("null"))
							&& (!"".equals(path))) {
						bm = PictureHelper.getCropImage(path, 400, true, 100,
								context, 7, true);
					}
					RemoteViews views = new RemoteViews(
							context.getPackageName(), R.layout.widget_layout);
					views.setViewVisibility(R.id.img, View.GONE);
					if (bm != null) {
						views.setViewVisibility(R.id.img, View.VISIBLE);
						views.setImageViewBitmap(R.id.img, bm);
						views.setTextViewText(R.id.content, content);
						views.setViewVisibility(R.id.content, View.VISIBLE);
						views.setViewVisibility(R.id.content2, View.GONE);
					} else {
						views.setTextViewText(R.id.content2, content);
						views.setViewVisibility(R.id.content2, View.VISIBLE);
						views.setViewVisibility(R.id.content, View.GONE);
					}
					views.setTextViewText(R.id.title, title);
					// views.setTextViewText(R.id.content, content);
					long msTime = Long.parseLong(nr.time);
					Calendar calendar = Calendar.getInstance();
					calendar.setTimeInMillis(msTime);
					String date = calendar.get(Calendar.YEAR) + "."
							+ (calendar.get(Calendar.MONTH) + 1) + "."
							+ calendar.get(Calendar.DAY_OF_MONTH);
					views.setTextViewText(R.id.date, date);
					appWidgetManager.updateAppWidget(widgetID, views);
				}
			}
		} else if ("com.mars.note.widget.delete".equals(intent.getAction())) {
			String widgetID = intent
					.getStringExtra(AppWidgetManager.EXTRA_APPWIDGET_ID);
//			Log.d("widget", "widgetID = " + widgetID);
			if (widgetID != null) {
//				Log.d("widget", "widgetID = " + widgetID);
				RemoteViews views = new RemoteViews(context.getPackageName(),
						R.layout.widget_layout);
				views.setViewVisibility(R.id.content2, View.VISIBLE);
				views.setViewVisibility(R.id.content, View.GONE);
				views.setViewVisibility(R.id.img, View.GONE);
				views.setTextViewText(R.id.date, "");
				views.setTextViewText(R.id.title, "");
				views.setTextViewText(R.id.content2,
						context.getString(R.string.widget_empty_title));
				appWidgetManager.updateAppWidget(Integer.parseInt(widgetID),
						views);
			}
		} else if ("com.mars.note.widget.clearall".equals(intent.getAction())) {
			int[] widgetIDs = appWidgetManager
					.getAppWidgetIds(new ComponentName(context,
							NoteWidgetProvider.class));
			RemoteViews views = new RemoteViews(context.getPackageName(),
					R.layout.widget_layout);
			views.setViewVisibility(R.id.content2, View.VISIBLE);
			views.setViewVisibility(R.id.content, View.GONE);
			views.setViewVisibility(R.id.img, View.GONE);
			views.setTextViewText(R.id.date, "");
			views.setTextViewText(R.id.title, "");
			views.setTextViewText(R.id.content2,
					context.getString(R.string.widget_empty_title));
			appWidgetManager.updateAppWidget(widgetIDs, views);
		} else if ("com.mars.note.widget.relate".equals(intent.getAction())) {
			int widgetID = intent.getIntExtra(
					AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
			if (widgetID != -1) {
//				Log.d("widget", "onReceive " + " widgetID = " + widgetID);
				String noteId = noteDBManager
						.querySingleRecordIDByWidgetID(String.valueOf(widgetID));
//				Log.d("widget", "noteId = " + noteId);
				NoteRecord nr = noteDBManager.querySingleRecord(noteId);
				String title = nr.title;
				String content = nr.content;
				String path = nr.imgpath;
				Bitmap bm = null;
				if (path != null && (!path.equals("null"))
						&& (!"".equals(path))) {
					bm = PictureHelper.getCropImage(path, 400, true, 100,
							context, 7, true);
				}
				RemoteViews views = new RemoteViews(context.getPackageName(),
						R.layout.widget_layout);
				views.setViewVisibility(R.id.img, View.GONE);
				if (bm != null) {
					views.setViewVisibility(R.id.img, View.VISIBLE);
					views.setImageViewBitmap(R.id.img, bm);
					views.setTextViewText(R.id.content, content);
					views.setViewVisibility(R.id.content, View.VISIBLE);
					views.setViewVisibility(R.id.content2, View.GONE);
				} else {
					views.setTextViewText(R.id.content2, content);
					views.setViewVisibility(R.id.content2, View.VISIBLE);
					views.setViewVisibility(R.id.content, View.GONE);
				}
				Intent intentR = new Intent(context, Editor.class);
				intentR.setAction(TAG + widgetID); // setAction to make intent
													// unique or all widgets
													// will use one intent
				intentR.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);
				PendingIntent pendingIntent = PendingIntent.getActivity(
						context, 0, intentR, 0);
				views.setTextViewText(R.id.title, title);
				// views.setTextViewText(R.id.content, content);
				long msTime = Long.parseLong(nr.time);
				Calendar calendar = Calendar.getInstance();
				calendar.setTimeInMillis(msTime);
				String date = calendar.get(Calendar.YEAR) + "."
						+ (calendar.get(Calendar.MONTH) + 1) + "."
						+ calendar.get(Calendar.DAY_OF_MONTH);
				views.setTextViewText(R.id.date, date);
				views.setOnClickPendingIntent(R.id.root, pendingIntent);
				appWidgetManager.updateAppWidget(widgetID, views);
			}
		}
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		Log.d(TAG, "onUpdate");
		if (noteDBManager == null) {
			noteDBManager = NoteApplication.getDbManager();
		}
		final int N = appWidgetIds.length;
//		Log.d(TAG, "onUpdate appWidgetIds.length = " + N);
		for (int i = 0; i < N; i++) {
			int appWidgetId = appWidgetIds[i];
			Log.i(TAG, "this is [" + appWidgetId + "] onUpdate!");
			RemoteViews views = new RemoteViews(context.getPackageName(),
					R.layout.widget_layout);
			views.setViewVisibility(R.id.img, View.GONE);
			String noteId = noteDBManager.querySingleRecordIDByWidgetID(String
					.valueOf(appWidgetId));
			if (noteId != null) { // if has relation
				NoteRecord nr = noteDBManager.querySingleRecord(noteId);
				String title = nr.title;
				String content = nr.content;
				String path = nr.imgpath;
				Bitmap bm = null;
				if (path != null && (!path.equals("null"))
						&& (!"".equals(path))) {
					bm = PictureHelper.getCropImage(path, 400, true, 100,
							context, 7, true);
				}
				if (bm != null) {
					views.setViewVisibility(R.id.img, View.VISIBLE);
					views.setImageViewBitmap(R.id.img, bm);
					views.setTextViewText(R.id.content, content);
					views.setViewVisibility(R.id.content, View.VISIBLE);
					views.setViewVisibility(R.id.content2, View.GONE);
				} else {
					views.setTextViewText(R.id.content2, content);
					views.setViewVisibility(R.id.content2, View.VISIBLE);
					views.setViewVisibility(R.id.content, View.GONE);
				}
				views.setTextViewText(R.id.title, title);
				long msTime = Long.parseLong(nr.time);
				Calendar calendar = Calendar.getInstance();
				calendar.setTimeInMillis(msTime);
				String date = calendar.get(Calendar.YEAR) + "/"
						+ (calendar.get(Calendar.MONTH) + 1) + "/"
						+ calendar.get(Calendar.DAY_OF_MONTH);
				views.setTextViewText(R.id.date, date);
			}
			Intent intent = new Intent(context, Editor.class);
			intent.setAction(TAG + appWidgetId); // setAction to make intent
													// unique or all widgets
													// will use one intent
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
			PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
					intent, 0);
			views.setOnClickPendingIntent(R.id.root, pendingIntent);
			appWidgetManager.updateAppWidget(appWidgetId, views);
		}
		super.onUpdate(context, appWidgetManager, appWidgetIds);
	}

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);
		Log.d(TAG, "onDeleted");
		if (noteDBManager == null) {
			noteDBManager = NoteApplication.getDbManager();
		}
		final int N = appWidgetIds.length;
//		Log.d(TAG, "onDeleted appWidgetIds.length = " + N);
		for (int i = 0; i < N; i++) {
			int appWidgetId = appWidgetIds[i];
			Log.i(TAG, "this is [" + appWidgetId + "] onDelete!");
			noteDBManager.deleteWidgetRelation(String.valueOf(appWidgetId));
		}
	}

	@Override
	public void onDisabled(Context context) {
		super.onDisabled(context);
		Log.d(TAG, "onDisabled");
	}

	@Override
	public void onEnabled(Context context) {
		super.onEnabled(context);
		Log.d(TAG, "onEnabled");
	}
}
