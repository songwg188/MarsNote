package com.mars.note.database;
import com.mars.note.Logg;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
public class NoteDatabaseHelper extends SQLiteOpenHelper {
	public NoteDatabaseHelper(Context context, String name,
			CursorFactory factory, int version) {
		super(context, name, factory, version);
		// TODO Auto-generated constructor stub
	}
	public NoteDatabaseHelper(Context context) {
		this(context, NoteDBField.DBNAME, null, NoteDBField.VERSION);
	}
	@Override
	public void onCreate(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		db.execSQL("CREATE TABLE IF NOT EXISTS " + NoteDBField.TABLE_NAME
				+ " ( " + NoteDBField.TITLE + " VARCHAR, " + NoteDBField.ID
				+ " INTEGER PRIMARY KEY, " + NoteDBField.CONTENT + " VARCHAR, "
				+ NoteDBField.TIME + " VARCHAR," + NoteDBField.YEAR
				+ " VARCHAR," + NoteDBField.MONTH + " VARCHAR,"
				+ NoteDBField.DAY + " VARCHAR," + NoteDBField.HOUR
				+ " VARCHAR," + NoteDBField.MINUTE + " VARCHAR,"
				+ NoteDBField.IMGPATH + " VARCHAR," + NoteDBField.SECOND
				+ " VARCHAR)"
		);
		db.execSQL("CREATE TABLE IF NOT EXISTS "
				+ NoteDBField.WIDGETS_TABLE_NAME + " ( "
				+ NoteDBField.WIDGETS_ID + " VARCHAR PRIMARY KEY, "
				+ NoteDBField.NOTES_ID + " VARCHAR)");
	}
	
	//第一次更新版本，新版本2，增加一个字段，该字段将储存图片位置集合的二进制信息
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		//增加一个保存图片集合的字段
		if(newVersion == 2 && oldVersion == 1){
			Logg.D("update 1 to 2");
			db.execSQL("ALTER TABLE "+NoteDBField.TABLE_NAME+
					" ADD COLUMN "+NoteDBField.IMGPATHS+" BLOB;");
			Logg.D("ALTER TABLE "+NoteDBField.TABLE_NAME+
					" ADD COLUMN "+NoteDBField.IMGPATHS+" BLOB;");
		}
	}
}
