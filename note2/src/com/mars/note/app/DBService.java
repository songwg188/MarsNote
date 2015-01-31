package com.mars.note.app;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import com.mars.note.api.ImageSpanInfo;
import com.mars.note.database.NoteDataBaseManager;
import com.mars.note.database.NoteRecord;
import com.mars.note.utils.Logg;
import com.mars.note.utils.PictureHelper;
import com.mars.note.utils.Util;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.IBinder;
import android.transition.Scene;
import android.util.DisplayMetrics;

/**
 * 用于更新数据库时执行大量耗时操作
 * @author mars
 * @date 2015-1-9 上午10:17:38
 * @version 1.1
 */
public class DBService extends Service {
	private boolean DBG = true;
	private Handler mHandler;
	private NoteDataBaseManager noteDBManager;
	private ThreadPoolExecutor executors;
	private int screenWidth;
	private ConcurrentHashMap<String, Boolean> mHaspMap; //记录写入的图片
	private ReentrantLock mLock;
	int count = 0;
	@Override
	public void onCreate() {
		mHandler = new Handler();
		noteDBManager = NoteApplication.getDbManager();
		if (DBG)
			Logg.S("DBService onCreate");
		executors = new ThreadPoolExecutor(12, 12, 0,
		        TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		mHaspMap = new ConcurrentHashMap<String, Boolean>();
		mLock = new ReentrantLock(true);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		boolean updateThumbnailsCount = intent.getBooleanExtra("updateThumbnailsCount", false);
		screenWidth = intent.getIntExtra("screenWidth", 0);
		if(screenWidth == 0)
			throw new IllegalStateException("screenWidth 0");
		if (DBG){
			Logg.S("DBService onStartCommand START_STICKY");
			Logg.S("DBService updateThumbnailsCount "+updateThumbnailsCount);
		}
		//该子线程等待写入任务的完成
		Thread updateTask = new Thread(){
			public void run() {
				long begin = System.currentTimeMillis();
				ArrayList<NoteRecord> allRecords = noteDBManager.queryAllRecords();
				Iterator<NoteRecord> iterator = allRecords.iterator();
//				noteDBManager.beginTransaction();
				while(iterator.hasNext()){
					final NoteRecord nr = iterator.next();
					if(nr.imageSpanInfos == null){
						continue;
					}
					//写入一条记录的所有图片到数据库
					Runnable runnable = new Runnable() {
						@Override
						public void run() {
							ArrayList<ImageSpanInfo> spanInfo = Util.getImageSpanInfoListFromBytes(nr.imageSpanInfos);
							Iterator<ImageSpanInfo> spanInfoIterator = spanInfo.iterator();
							while(spanInfoIterator.hasNext()){
								ImageSpanInfo isi = spanInfoIterator.next();
								String path = isi.path.substring(5, isi.path.length() - 5);
								mLock.lock();
								if(mHaspMap.get(path)==null){
									mHaspMap.put(path, true);
									mLock.unlock();
									Logg.S("path "+path+" need to be written");
									//第一次写入图片
									if (noteDBManager.isThumbnailExisted(path)) {
										if (DBG)
											Logg.S("path " + path + " existed in db ... Continue");
										// 跳到下一次循环
										continue;
									} else {
										if (DBG)
											Logg.S("path " + path + " load from local ...");
										if (path != null && (!path.equals("null")) && (!"".equals(path))) {
											Bitmap bm = PictureHelper.getImageFromPath(path, screenWidth * 0.7F, screenWidth * 0.7F, false, 100, DBService.this, Util.dpToPx(getResources(), 3), false);
											
											if(bm == null){
//												Logg.S("path " + path + " not found from local ... Continue");
												continue;
											}
											noteDBManager.addNewThumbnail(path, bm);
											count++;
//											Logg.S("path " + path + " inserted into db ");
										}else{
//											Logg.S("path error ... Continue");
											continue;
										}
									}
								}else{
									//已经写入，跳过循环
//									Logg.S("path "+path+" already written ... Continue");
									mLock.unlock();
									continue;
								}
							}
//							Logg.S("runnable done");
						}
					};
					executors.execute(runnable);
				}
				executors.shutdown(); //关闭后不能加入新线程，队列中的线程则依次执行完
			    while(executors.getPoolSize()!=0);
//			    noteDBManager.endTransaction();
//			    Logg.S("main thread end!");
				
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						Intent updateThumbnailsCountFinished = new Intent("com.mars.note.updateThumbnailsCount.finished");
						sendBroadcast(updateThumbnailsCountFinished);
						if (DBG){
							Logg.S("DBService sendBroadcast");
						}
					}
				});
//				Logg.S("using time "+(System.currentTimeMillis()-begin)+" ms ,interted count "+count);
			};
		};
		updateTask.start();
		return Service.START_STICKY;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onDestroy() {
		if (DBG)
			Logg.S("DBService onDestroy");
		super.onDestroy();
	}

}
