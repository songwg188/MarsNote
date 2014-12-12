package com.mars.note.api;

import java.util.LinkedList;


import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

/**
 * author mars
 * date 20141205
 * description 帮助Editor缓存，保存图片地址
 */
public class EditorHelper {
	public static LruCache<String, Bitmap> mIMGCache; //Editor的图片缓存

	public EditorHelper() {
		initBitmapCache();
	}

	public static EditorHelper newInstance() {
		return new EditorHelper();
	}

	private void initBitmapCache() {
		if (mIMGCache == null) {
			int maxMemory = (int) Runtime.getRuntime().maxMemory();
			int mCacheSize = maxMemory / 4;
			 Logg.D("mCacheSize = "+mCacheSize);
			mIMGCache = new LruCache<String, Bitmap>(mCacheSize) {
				@Override
				protected int sizeOf(String key, Bitmap bm) {
					if (bm != null) {
//						Logg.D("bm.getRowBytes() ="+bm.getRowBytes());
//						Logg.D("bm.getWidth() ="+bm.getWidth());
//						Logg.D("bm.getHeight() ="+bm.getHeight());
//						Logg.D("bm.getDensity() ="+bm.getDensity());
//						Logg.D("ARGB 8888 = 4");
//						Logg.D("usage size = "+(bm.getRowBytes() * bm.getHeight()));
//						Logg.D("left size = "+(mIMGCache.maxSize()-
//								(mIMGCache.size()+bm.getRowBytes() * bm.getHeight())));
						
						return bm.getRowBytes() * bm.getHeight();
					}
					return 0;
				}
			};
		}
	}
	
	public void addImage(String path,Bitmap bm){
		mIMGCache.put(path, bm);
	}
	
	public void removeImage(String path){
		mIMGCache.remove(path);
	}
	
	public Bitmap getImage(String path){
		return mIMGCache.get(path);
	}
	
	public void clearImageCache(){
		mIMGCache.evictAll();
	}
	
}
