package com.mars.note.database;
import com.mars.note.api.Logg;

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
				+ " ( " + NoteDBField.TITLE + " VARCHAR, " 
						+ NoteDBField.ID + " INTEGER PRIMARY KEY, " 
						+ NoteDBField.CONTENT + " VARCHAR, "
						+ NoteDBField.TIME + " VARCHAR," 
						+ NoteDBField.YEAR + " VARCHAR,"
						+ NoteDBField.MONTH + " VARCHAR,"
						+ NoteDBField.DAY + " VARCHAR," 
						+ NoteDBField.HOUR + " VARCHAR," 
						+ NoteDBField.MINUTE + " VARCHAR,"
						+ NoteDBField.IMGPATH + " VARCHAR," 
						+ NoteDBField.SECOND  + " VARCHAR)"
		);
		db.execSQL("CREATE TABLE IF NOT EXISTS "
				+ NoteDBField.WIDGETS_TABLE_NAME + " ( "
				+ NoteDBField.WIDGETS_ID + " VARCHAR PRIMARY KEY, "
				+ NoteDBField.NOTES_ID + " VARCHAR)");
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if(newVersion == 2 && oldVersion == 1){
			Logg.D("update 1 to 2");
			db.beginTransaction();
			db.execSQL("ALTER TABLE "+NoteDBField.TABLE_NAME+
					" ADD COLUMN "+NoteDBField.IMGPATHS+" BLOB;");
			Logg.D("ALTER TABLE "+NoteDBField.TABLE_NAME+
					" ADD COLUMN "+NoteDBField.IMGPATHS+" BLOB;");
			db.setTransactionSuccessful();
			db.endTransaction();
			Logg.D("update db success");
		}
		if(newVersion == 3 && oldVersion == 2){
			Logg.D("update 2 to 3"); 
//			1、修改原表的名称
			db.beginTransaction();
			db.execSQL("ALTER TABLE "+NoteDBField.TABLE_NAME+
					" RENAME TO temp");
//			2、新建修改字段后的表  
			db.execSQL("CREATE TABLE IF NOT EXISTS " + NoteDBField.TABLE_NAME
					+ " ( " + NoteDBField.TITLE + " VARCHAR, " 
							+ NoteDBField.ID + " INTEGER PRIMARY KEY, " 
							+ NoteDBField.CONTENT + " VARCHAR, "
							+ NoteDBField.TIME + " VARCHAR," 
							+ NoteDBField.YEAR + " VARCHAR,"
							+ NoteDBField.MONTH + " VARCHAR,"
							+ NoteDBField.DAY + " VARCHAR," 
							+ NoteDBField.HOUR + " VARCHAR," 
							+ NoteDBField.MINUTE + " VARCHAR,"
							+ NoteDBField.IMGPATH + " VARCHAR," 
							+ NoteDBField.SECOND  + " VARCHAR)"
			);
			db.execSQL("ALTER TABLE "
							+ NoteDBField.TABLE_NAME + " ADD COLUMN "
							+ NoteDBField.IMAGESPANINFOS+" BLOB;");
			
//			3、从旧表中查询出数据 并插入新表
//			INSERT INTO table SELECT ID,Username FROM tableOld;
			db.execSQL("INSERT INTO "+NoteDBField.TABLE_NAME+" SELECT "+
					NoteDBField.TITLE+"," +
					NoteDBField.ID+"," +
					NoteDBField.CONTENT+"," +
					NoteDBField.TIME+"," +
					NoteDBField.YEAR+"," +
					NoteDBField.MONTH+"," +
					NoteDBField.DAY+"," +
					NoteDBField.HOUR+"," +
					NoteDBField.MINUTE+"," +
					NoteDBField.IMGPATH+"," +
					NoteDBField.SECOND +"," +
					NoteDBField.IMGPATHS +
					" FROM temp;");
//			4、删除旧表
//			DROP TABLE tableOld;
			db.execSQL("DROP TABLE temp");
			db.setTransactionSuccessful();
			db.endTransaction();
			Logg.D("update db success");
		}  
		if(newVersion == 3 && oldVersion == 1){
			db.beginTransaction(); 
			Logg.D("update 1 to 3");
			db.execSQL("ALTER TABLE "+NoteDBField.TABLE_NAME+
					" ADD COLUMN "+NoteDBField.IMAGESPANINFOS+" BLOB;");
			Logg.D("ALTER TABLE "+NoteDBField.TABLE_NAME+
					" ADD COLUMN "+NoteDBField.IMAGESPANINFOS+" BLOB;");
			db.setTransactionSuccessful();
			db.endTransaction();
			Logg.D("update db success");
		}
	}
}
