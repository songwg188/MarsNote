package com.mars.note;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.concurrent.ExecutorService;

import com.mars.note.database.NoteDataBaseManager;
import com.mars.note.fragment.GridPaperItemImg;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.util.Log;

public class NoteApplication extends Application {
	private static NoteDataBaseManager dbManager;
	private static LruCache<String, Bitmap> mBitmapCache;
	private static ArrayList<com.mars.note.fragment.GridViewPaperItemForBatchDelete> mGridViewBatchDeleteCache;
	private static ExecutorService executors;

	public static ExecutorService getExecutors() {
		return executors;
	}

	public static void setExecutors(ExecutorService executors) {
		NoteApplication.executors = executors;
	}

	public static void setGridViewBatchDeleteCache(
			ArrayList<com.mars.note.fragment.GridViewPaperItemForBatchDelete> mGridViewBatchDeleteCache) {
		NoteApplication.mGridViewBatchDeleteCache = mGridViewBatchDeleteCache;
	}

	public static ArrayList<com.mars.note.fragment.GridViewPaperItemForBatchDelete> getmGridViewBatchDeleteCache() {
		return mGridViewBatchDeleteCache;
	}

	public static void closeDB() {
		dbManager.closeDB();
	}

	public static NoteDataBaseManager getDbManager() {
		return dbManager;
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		NoteApplication.clearBitmapCache();
		initBitmapCache();

	}

	public static LruCache<String, Bitmap> getBitmapCache() {
		return mBitmapCache;
	}

	public static void clearBitmapCache() {
		if (mBitmapCache != null) {
			mBitmapCache.evictAll();
			mBitmapCache = null;
		}
		System.gc();
	}

	public void initBitmapCache() {

		if (mBitmapCache == null) {
			int maxMemory = (int) Runtime.getRuntime().maxMemory();
			int mCacheSize = maxMemory / 8;
			// Logg.D("maxMemory = "+maxMemory); //about 129mb
			// Logg.D("mCacheSize = "+mCacheSize); //about 16mb
			mBitmapCache = new LruCache<String, Bitmap>(mCacheSize) {
				@Override
				protected int sizeOf(String key, Bitmap value) {
					if (value != null) {
						return value.getRowBytes() * value.getHeight();
					}
					return 0;
				}
			};
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Logg.D("application create");
		dbManager = new NoteDataBaseManager(this);
		CrashHandler crashHandler = CrashHandler.getInstance();
		crashHandler.init(getApplicationContext());
		initBitmapCache();
		SharedPreferences pref = this.getSharedPreferences("theme",
				Context.MODE_PRIVATE);
		Config.current_theme = pref.getInt("theme_id", 1);
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
		Logg.D("application onTerminate()");
		NoteApplication.clearBitmapCache();
		NoteApplication.closeDB();

	}
}
