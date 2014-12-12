package com.mars.note.database;

public class NoteRecord {
	public String title;
	public String content;
	public String id;
	public String time;
	public String year;
	public String month;
	public String day;
	public String hour;
	public String minute;
	public String second;
	public String imgpath;
	public byte[] imageSpanInfos; //20141211 ArrayList<ImageSpan>的序列化数组
}
