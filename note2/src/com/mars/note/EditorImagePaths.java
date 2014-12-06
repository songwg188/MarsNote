package com.mars.note;

import java.io.Serializable;
import java.util.LinkedList;

public class EditorImagePaths implements Serializable {

	/**
	 * Author mars Date 20141205 Description ��Ϊ���л����󣬴洢ÿ��record��ͼƬλ�ü��ϵ���Ϣ��
	 * ����ת��Ϊ�����Ʊ��浽���ݿ��С� ��������ݿ��ж�ȡ��ҲӦ�û�ԭΪ���ࡣ
	 */
	private static final long serialVersionUID = 1L;
	private LinkedList<String> mPaths; //ʹ��LinkedList���������ܹ�������Ӧɾ�����Ӳ���

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
		//LinkedList��remove(object)�е��ж������ǣ����obj1.equals(obj2),remove(obj1)
		//����String��������
		mPaths.remove(path);
	}
	
	public int getSize(){
		return mPaths.size();
	}
	
	public void clearPaths(){
		mPaths.clear();
	}
}
