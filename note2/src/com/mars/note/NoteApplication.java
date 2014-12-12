package com.mars.note;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

import com.mars.note.api.Config;
import com.mars.note.api.CrashHandler;
import com.mars.note.api.GridPaperItemImg;
import com.mars.note.api.Logg;
import com.mars.note.database.NoteDataBaseManager;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.util.Log;

public class NoteApplication extends Application {
	private static NoteDataBaseManager dbManager;
	private static LruCache<String, Bitmap> mBitmapCache;
	private static ArrayList<com.mars.note.api.GridViewPaperItemForBatchDelete> mGridViewBatchDeleteCache;
	
	//创建一个可重用固定线程数的线程池，以共享的无界队列方式来运行这些线程
	private static ExecutorService executors;
	
	/**
	 * 考虑这种情形，在RecentRecordsFragment中，如果ListView或GridView中同一页有多个Item用的相同的图片
	 * 那么第一次加载图片时可能有这种情形，第一个线程去加载图片还未将图片加载到mBitmapCache，第二个线程也去请求图片，因为
	 * mBitmapCache没有图片，会去加载，这样导致了多个线程加载同一图片
	 * 
	 * 尝试，用ConcurrentMap<String , Boolean>同步线程，第一String代表图片的地址，第二个Boolean
	 * 表示是否有线程在加载这个地址的图片 True表示正在加载，False表示没有加载，或者已经加载完了
	 * 如果一个线程去读一张图，mBitmapCache没有这张图，接下来去找ConcurrentMap对应地址的Boolean，
	 * 如果是ConcurrentMap没有这个键值对或者Boolean是false就去读这张图并设置True，读完图片设置Boolean False
	 * 如果是True就进入无线循环不断读Boolean直到False去mBitmapCache加载图片
	 */

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
	
	public static ExecutorService getExecutors() {
		return executors;
	}

	public static void setExecutors(ExecutorService executors) {
		NoteApplication.executors = executors;
	}

	public static void setGridViewBatchDeleteCache(
			ArrayList<com.mars.note.api.GridViewPaperItemForBatchDelete> mGridViewBatchDeleteCache) {
		NoteApplication.mGridViewBatchDeleteCache = mGridViewBatchDeleteCache;
	}

	public static ArrayList<com.mars.note.api.GridViewPaperItemForBatchDelete> getmGridViewBatchDeleteCache() {
		return mGridViewBatchDeleteCache;
	}

	public static void closeDB() {
		dbManager.closeDB();
	}
	
	public static void openDB() {
		dbManager.openDB();
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
	public void onTerminate() {
		super.onTerminate();
		Logg.D("application onTerminate()");
		NoteApplication.clearBitmapCache();
		NoteApplication.closeDB();
	}
}
