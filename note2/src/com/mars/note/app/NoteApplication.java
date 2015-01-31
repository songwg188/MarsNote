package com.mars.note.app;

import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

import com.mars.note.api.Config;
import com.mars.note.api.CrashHandler;
import com.mars.note.database.NoteDataBaseManager;
import com.mars.note.utils.Logg;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

public class NoteApplication extends Application {
	// 单例模式 被整个app共用
	private static NoteDataBaseManager dbManager;
	// 单例模式，被RecentFragment,SearchFragment,CalendarFragment共享
	private static LruCache<String, Bitmap> mBitmapCache;
	// 单例模式 创建一个可重用固定线程数的线程池，以共享的无界队列方式来运行这些线程
	private static ExecutorService executors;
	private static Application mApplication;
	//记录图片是否正在读取到缓存
	private static ConcurrentHashMap<String, Boolean> mConcurentMap;
	private static ReentrantLock mLock;

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
		mApplication = this;
		CrashHandler crashHandler = CrashHandler.getInstance();
		crashHandler.init(getApplicationContext());
		SharedPreferences pref = this.getSharedPreferences("theme",
				Context.MODE_PRIVATE);
		Config.current_theme = pref.getInt("theme_id", 1);
		
		mConcurentMap = new ConcurrentHashMap<String, Boolean>();// 保存线程工作状态
		mLock = new ReentrantLock(true);// 20141212 公平锁
	}

	/**
	 * @return the mLock
	 */
	public static ReentrantLock getmLock() {
		return mLock;
	}

	/**
	 * @param mLock the mLock to set
	 */
	public static void setmLock(ReentrantLock mLock) {
		NoteApplication.mLock = mLock;
	}

	/**
	 * @return the mConcurentMap
	 */
	public static ConcurrentHashMap<String, Boolean> getmConcurentMap() {
		return mConcurentMap;
	}

	/**
	 * @param mConcurentMap the mConcurentMap to set
	 */
	public static void setmConcurentMap(ConcurrentHashMap<String, Boolean> mConcurentMap) {
		NoteApplication.mConcurentMap = mConcurentMap;
	}

	public static ExecutorService getExecutors() {
		if (executors == null) {
			executors = Executors.newFixedThreadPool(EditorActivity.thread_num);
		}
		return executors;
	}

	public static void closeDB() {
		if (dbManager == null) {
			dbManager = new NoteDataBaseManager(mApplication);
		}
		dbManager.closeDB();
	}

	public static void openDB() {
		if (dbManager == null) {
			dbManager = new NoteDataBaseManager(mApplication);
		}
		dbManager.openDB();
	}

	public static NoteDataBaseManager getDbManager() {
		if (dbManager == null) {
			dbManager = new NoteDataBaseManager(mApplication);
		}
		return dbManager;
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		clearBitmapCache();
		if (mBitmapCache == null) {
			int maxMemory = (int) Runtime.getRuntime().maxMemory();
			int mCacheSize = maxMemory / 8;
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

	public static LruCache<String, Bitmap> getBitmapCache() {
		if (mBitmapCache == null) {
			int maxMemory = (int) Runtime.getRuntime().maxMemory();
			int mCacheSize = maxMemory / 4;
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
		return mBitmapCache;
	}

	public static void clearBitmapCache() {
		if (mBitmapCache != null) {
			mBitmapCache.evictAll();
			mBitmapCache = null;
		}
		System.gc();
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
		NoteApplication.clearBitmapCache();
		NoteApplication.closeDB();
	}
}
