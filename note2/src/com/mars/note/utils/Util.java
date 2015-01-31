package com.mars.note.utils;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Iterator;

import com.mars.note.R;
import com.mars.note.api.ImageSpanInfo;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;

public class Util {

	public static int dpToPx(Resources res, float dp) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, res.getDisplayMetrics());
	}
	
	/**
	 * 从ArrayList<ImageSpanInfo>中得到图片地址集合
	 * @param list
	 * @return
	 */
	public static ArrayList<String> getPathsFromImageSpanInfo(ArrayList<ImageSpanInfo> list){
		ArrayList<String> paths = new ArrayList<String>();
		Iterator<ImageSpanInfo> iterator = list.iterator();
		while(iterator.hasNext()){
			String path = iterator.next().path;
			paths.add(path.substring(5, path.length() - 5));
		}
		
		return paths;
	}
	
	/**
	 * 过滤内容的图片字符串
	 * @param content
	 * @param list
	 * @return
	 */
	public static String filterContent(Context context,String content,ArrayList<ImageSpanInfo> list){
		StringBuffer filter = new StringBuffer(content);	
		for(ImageSpanInfo isi : list){
			int start = filter.indexOf(isi.path);
			int end = start + isi.path.length();
			filter.replace(start, end,context.getString(R.string.replaced_text));
		}
		return filter.toString();
	}
	
	/**
	 * 从二进制数组转换Arrayist对象
	 * 
	 * @param bytes
	 *            二进制数组
	 * @return ArrayList返回对象
	 */
	public static ArrayList<ImageSpanInfo> getImageSpanInfoListFromBytes(byte[] bytes) {
		if(bytes == null)
			throw new NullPointerException("bytes cant be null");
		ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(bytes);
		try {
			ObjectInputStream inputStream = new ObjectInputStream(
					arrayInputStream);
			ArrayList<ImageSpanInfo> list = (ArrayList<ImageSpanInfo>) inputStream
					.readObject();
			inputStream.close();
			arrayInputStream.close();
			return list;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

}
