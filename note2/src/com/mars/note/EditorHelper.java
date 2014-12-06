package com.mars.note;

import java.util.LinkedList;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

/*
 * author mars
 * date 20141205
 * description 帮助Editor缓存图片，保存图片地址等等,使用于新功能：图文并排
 */
public class EditorHelper {
	public static LruCache<String, Bitmap> mIMGCache; //图片缓存
	EditorImagePaths mEditorImagePaths; //地址集合

	public EditorHelper() {
		mEditorImagePaths = new EditorImagePaths();
		initBitmapCache();
	}

	public static EditorHelper newInstance() {
		return new EditorHelper();
	}

	private void initBitmapCache() {
		if (mIMGCache == null) {
			int maxMemory = (int) Runtime.getRuntime().maxMemory();
			int mCacheSize = maxMemory / 8;
			// Logg.D("maxMemory = "+maxMemory);
			// Logg.D("mCacheSize = "+mCacheSize);
			mIMGCache = new LruCache<String, Bitmap>(mCacheSize) {
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
	
	public void addPath(String path){
		mEditorImagePaths.addPath(path);
	}
	
	public void removePath(String path){
		mEditorImagePaths.removePath(path);
	}
	
	public int getPathsSize(){
		return mEditorImagePaths.getSize();
	}
	
	public void clearPaths(){
		mEditorImagePaths.clearPaths();
	}
	
}
