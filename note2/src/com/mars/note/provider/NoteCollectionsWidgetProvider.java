package com.mars.note.provider;

import java.util.Calendar;
import com.mars.note.R;
import com.mars.note.app.EditorActivity;
import com.mars.note.app.NoteApplication;
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

public class NoteCollectionsWidgetProvider extends AppWidgetProvider {
	private final String TAG = "NoteCollectionsWidgetProvider";
	private NoteDataBaseManager noteDBManager;
	private AppWidgetManager appWidgetManager;

	// run this when receive a broadcast
	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
//		Log.d(TAG, "onReceive" + " " + intent.getAction());
		if (appWidgetManager == null) {
			appWidgetManager = AppWidgetManager.getInstance(context);
		}
		if ("com.mars.note.widgetcollections.refresh"
				.equals(intent.getAction())) {
			int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(
					context, NoteCollectionsWidgetProvider.class));
			appWidgetManager.notifyAppWidgetViewDataChanged(ids,
					R.id.widget_stack_view);
		}
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
//		Log.d(TAG, "onUpdate");
		final int N = appWidgetIds.length;
//		Log.d(TAG, "onUpdate appWidgetIds.length = " + N);
		for (int i = 0; i < N; i++) {
			int appWidgetId = appWidgetIds[i];
			Log.i(TAG, "this is [" + appWidgetId + "] onUpdate!");
			// create a new Remote View to show in ListView
			RemoteViews views = new RemoteViews(context.getPackageName(),
					R.layout.widget_collections_layout);
			// bind the widget to a RemoteViewsService
			Intent intent = new Intent(context, MyRemoteViewsService.class);
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
			views.setRemoteAdapter(R.id.widget_stack_view, intent);
			views.setEmptyView(R.id.widget_stack_view,
					R.id.widget_stack_empty_text);
			Intent intent2 = new Intent(context, EditorActivity.class);
			intent2.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
			PendingIntent pIntent = PendingIntent.getActivity(context, 0,
					intent2, PendingIntent.FLAG_UPDATE_CURRENT);
			views.setPendingIntentTemplate(R.id.widget_stack_view, pIntent);
			appWidgetManager.updateAppWidget(appWidgetId, views);

		}
		super.onUpdate(context, appWidgetManager, appWidgetIds);
	}

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);
//		Log.d(TAG, "onDeleted");
	}

	@Override
	public void onDisabled(Context context) {
		super.onDisabled(context);
//		Log.d(TAG, "onDisabled");
	}

	@Override
	public void onEnabled(Context context) {
		super.onEnabled(context);
//		Log.d(TAG, "onEnabled");
	}
}
