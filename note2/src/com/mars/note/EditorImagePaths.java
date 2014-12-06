package com.mars.note;

import java.io.Serializable;
import java.util.LinkedList;

public class EditorImagePaths implements Serializable {

	/**
	 * Author mars Date 20141205 Description 作为序列化对象，存储每个record的图片位置集合的信息，
	 * 将会转换为二进制保存到数据库中。 如果从数据库中读取，也应该还原为此类。
	 */
	private static final long serialVersionUID = 1L;
	private LinkedList<String> mPaths; //使用LinkedList，即链表，能够快速响应删除增加操作

	public EditorImagePaths() {
		mPaths = new LinkedList<String>();
	}

	public LinkedList<String> getPaths() {
		return mPaths;
	}

	public void addPath(String path) {
		mPaths.add(path);
	}
	
	public void removePath(String path){
		//LinkedList的remove(object)中的判断依据是，如果obj1.equals(obj2),remove(obj1)
		//对于String类型适用
		mPaths.remove(path);
	}
	
	public int getSize(){
		return mPaths.size();
	}
	
	public void clearPaths(){
		mPaths.clear();
	}
}
