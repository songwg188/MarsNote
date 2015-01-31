package com.mars.note.database;

import com.mars.note.api.Config;
import com.mars.note.app.BackUpActivity;
import com.mars.note.app.DBService;
import com.mars.note.utils.Logg;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class NoteDatabaseHelper extends SQLiteOpenHelper {
	private static final boolean DEBUG = true;
	Intent serviceIntent;
	private Context mContext;

	public NoteDatabaseHelper(Context context, String name, CursorFactory factory, int version) {
		super(context, name, factory, version);
	}

	public NoteDatabaseHelper(Context context) {
		this(context, NoteDBField.DBNAME, null, NoteDBField.VERSION);
		serviceIntent  = new Intent(context,DBService.class);
		serviceIntent.putExtra("updateThumbnailsCount", true);
		mContext = context;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// version 3
		// record 记录
		db.beginTransaction();
		db.execSQL("CREATE TABLE IF NOT EXISTS " + NoteDBField.TABLE_NAME + " ( " + NoteDBField.TITLE + " VARCHAR, " + NoteDBField.ID
				+ " INTEGER PRIMARY KEY, " + NoteDBField.CONTENT + " VARCHAR, " + NoteDBField.TIME + " VARCHAR," + NoteDBField.YEAR + " VARCHAR,"
				+ NoteDBField.MONTH + " VARCHAR," + NoteDBField.DAY + " VARCHAR," + NoteDBField.HOUR + " VARCHAR," + NoteDBField.MINUTE + " VARCHAR,"
				+ NoteDBField.SECOND + " VARCHAR," + NoteDBField.IMGPATH + " VARCHAR," + NoteDBField.IMAGESPANINFOS + " BLOB )");
		// 实现widget记录
		db.execSQL("CREATE TABLE IF NOT EXISTS " + NoteDBField.WIDGETS_TABLE_NAME + " ( " + NoteDBField.WIDGETS_ID + " VARCHAR PRIMARY KEY, "
				+ NoteDBField.NOTES_ID + " VARCHAR)");

		// 实现剪裁图表
		db.execSQL("CREATE TABLE IF NOT EXISTS " + NoteDBField.CROPPED_IMGS_TABLE_NAME + " ( " + NoteDBField.IMGPATH + " VARCHAR PRIMARY KEY, "
				+ NoteDBField.IMAGE + " BLOB)");
		if (DEBUG)
			Logg.I("cropped imgs table created at first time");

		// 实现缩略图表
		db.execSQL("CREATE TABLE IF NOT EXISTS " + NoteDBField.THUMBNAILS_TABLE_NAME + " ( " + NoteDBField.IMGPATH + " VARCHAR PRIMARY KEY, "
				+ NoteDBField.IMAGE + " BLOB," + NoteDBField.THUMBNAILS_COUNT + " )");
		if (DEBUG)
			Logg.I("thumbnails table created at first time");
		db.setTransactionSuccessful();
		db.endTransaction();
		if (DEBUG)
			Logg.I("db created succeed");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (DEBUG)
			Logg.I("oldVersion " + oldVersion + " newVersion " + newVersion);
		if (newVersion == 2 && oldVersion == 1) {
			Logg.D("update 1 to 2");
			db.beginTransaction();
			db.execSQL("ALTER TABLE " + NoteDBField.TABLE_NAME + " ADD COLUMN " + NoteDBField.IMGPATHS + " BLOB;");
			Logg.D("ALTER TABLE " + NoteDBField.TABLE_NAME + " ADD COLUMN " + NoteDBField.IMGPATHS + " BLOB;");
			db.setTransactionSuccessful();
			db.endTransaction();
			Logg.D("update db success");
		}
		if (newVersion == 3 && oldVersion == 2) {
			Logg.D("update 2 to 3");
			// 1、修改原表的名称
			db.beginTransaction();
			db.execSQL("ALTER TABLE " + NoteDBField.TABLE_NAME + " RENAME TO temp");
			// 2、新建修改字段后的表
			db.execSQL("CREATE TABLE IF NOT EXISTS " + NoteDBField.TABLE_NAME + " ( " + NoteDBField.TITLE + " VARCHAR, " + NoteDBField.ID
					+ " INTEGER PRIMARY KEY, " + NoteDBField.CONTENT + " VARCHAR, " + NoteDBField.TIME + " VARCHAR," + NoteDBField.YEAR + " VARCHAR,"
					+ NoteDBField.MONTH + " VARCHAR," + NoteDBField.DAY + " VARCHAR," + NoteDBField.HOUR + " VARCHAR," + NoteDBField.MINUTE + " VARCHAR,"
					+ NoteDBField.IMGPATH + " VARCHAR," + NoteDBField.SECOND + " VARCHAR)");
			db.execSQL("ALTER TABLE " + NoteDBField.TABLE_NAME + " ADD COLUMN " + NoteDBField.IMAGESPANINFOS + " BLOB;");

			// 3、从旧表中查询出数据 并插入新表
			// INSERT INTO table SELECT ID,Username FROM tableOld;
			db.execSQL("INSERT INTO " + NoteDBField.TABLE_NAME + " SELECT " + NoteDBField.TITLE + "," + NoteDBField.ID + "," + NoteDBField.CONTENT + ","
					+ NoteDBField.TIME + "," + NoteDBField.YEAR + "," + NoteDBField.MONTH + "," + NoteDBField.DAY + "," + NoteDBField.HOUR + ","
					+ NoteDBField.MINUTE + "," + NoteDBField.IMGPATH + "," + NoteDBField.SECOND + "," + NoteDBField.IMGPATHS + " FROM temp;");
			// 4、删除旧表
			// DROP TABLE tableOld;
			db.execSQL("DROP TABLE temp");
			db.setTransactionSuccessful();
			db.endTransaction();
			Logg.D("update db success");
		}
		if (newVersion == 3 && oldVersion == 1) {
			db.beginTransaction();
			db.execSQL("ALTER TABLE " + NoteDBField.TABLE_NAME + " ADD COLUMN " + NoteDBField.IMAGESPANINFOS + " BLOB;");
			db.setTransactionSuccessful();
			db.endTransaction();
		}
		if (newVersion == 4) {
			switch (oldVersion) {
			case 1:
				db.beginTransaction();
				db.execSQL("ALTER TABLE " + NoteDBField.TABLE_NAME + " ADD COLUMN " + NoteDBField.IMAGESPANINFOS + " BLOB;");
				// 实现剪裁图表
				db.execSQL("CREATE TABLE IF NOT EXISTS " + NoteDBField.CROPPED_IMGS_TABLE_NAME + " ( " + NoteDBField.IMGPATH + " VARCHAR PRIMARY KEY, "
						+ NoteDBField.IMAGE + " BLOB)");
				if (DEBUG)
					Logg.I("cropped imgs table created when upgrade");

				// 实现缩略图表
				db.execSQL("CREATE TABLE IF NOT EXISTS " + NoteDBField.THUMBNAILS_TABLE_NAME + " ( " + NoteDBField.IMGPATH + " VARCHAR PRIMARY KEY, "
						+ NoteDBField.IMAGE + " BLOB)");
				if (DEBUG)
					Logg.I("thumbnails table created when upgrade");

				db.setTransactionSuccessful();
				db.endTransaction();
				if (DEBUG)
					Logg.I("update db from 1 to 4 success");
				break;
			case 2:
				db.beginTransaction();
				// 1、修改原表的名称
				db.execSQL("ALTER TABLE " + NoteDBField.TABLE_NAME + " RENAME TO temp");
				// 2、新建修改字段后的表
				db.execSQL("CREATE TABLE IF NOT EXISTS " + NoteDBField.TABLE_NAME + " ( " + NoteDBField.TITLE + " VARCHAR, " + NoteDBField.ID
						+ " INTEGER PRIMARY KEY, " + NoteDBField.CONTENT + " VARCHAR, " + NoteDBField.TIME + " VARCHAR," + NoteDBField.YEAR + " VARCHAR,"
						+ NoteDBField.MONTH + " VARCHAR," + NoteDBField.DAY + " VARCHAR," + NoteDBField.HOUR + " VARCHAR," + NoteDBField.MINUTE + " VARCHAR,"
						+ NoteDBField.IMGPATH + " VARCHAR," + NoteDBField.SECOND + " VARCHAR)");
				db.execSQL("ALTER TABLE " + NoteDBField.TABLE_NAME + " ADD COLUMN " + NoteDBField.IMAGESPANINFOS + " BLOB;");
				// 3、从旧表中查询出数据 并插入新表
				db.execSQL("INSERT INTO " + NoteDBField.TABLE_NAME + " SELECT " + NoteDBField.TITLE + "," + NoteDBField.ID + "," + NoteDBField.CONTENT + ","
						+ NoteDBField.TIME + "," + NoteDBField.YEAR + "," + NoteDBField.MONTH + "," + NoteDBField.DAY + "," + NoteDBField.HOUR + ","
						+ NoteDBField.MINUTE + "," + NoteDBField.IMGPATH + "," + NoteDBField.SECOND + "," + NoteDBField.IMGPATHS + " FROM temp;");
				// 4、删除旧表
				db.execSQL("DROP TABLE temp");

				// 实现剪裁图表
				db.execSQL("CREATE TABLE IF NOT EXISTS " + NoteDBField.CROPPED_IMGS_TABLE_NAME + " ( " + NoteDBField.IMGPATH + " VARCHAR PRIMARY KEY, "
						+ NoteDBField.IMAGE + " BLOB)");
				if (DEBUG)
					Logg.I("cropped imgs table created when upgrade");

				// 实现缩略图表
				db.execSQL("CREATE TABLE IF NOT EXISTS " + NoteDBField.THUMBNAILS_TABLE_NAME + " ( " + NoteDBField.IMGPATH + " VARCHAR PRIMARY KEY, "
						+ NoteDBField.IMAGE + " BLOB)");
				if (DEBUG)
					Logg.I("thumbnails table created when upgrade");

				db.setTransactionSuccessful();
				db.endTransaction();
				if (DEBUG)
					Logg.I("update db from 2 to 4 success");
				break;
			case 3:
				db.beginTransaction();
				// 实现剪裁图表
				db.execSQL("CREATE TABLE IF NOT EXISTS " + NoteDBField.CROPPED_IMGS_TABLE_NAME + " ( " + NoteDBField.IMGPATH + " VARCHAR PRIMARY KEY, "
						+ NoteDBField.IMAGE + " BLOB)");
				if (DEBUG)
					Logg.I("cropped imgs table created when upgrade");
				// 实现缩略图表
				db.execSQL("CREATE TABLE IF NOT EXISTS " + NoteDBField.THUMBNAILS_TABLE_NAME + " ( " + NoteDBField.IMGPATH + " VARCHAR PRIMARY KEY, "
						+ NoteDBField.IMAGE + " BLOB)");
				if (DEBUG)
					Logg.I("thumbnails table created when upgrade");

				db.setTransactionSuccessful();
				db.endTransaction();
				if (DEBUG)
					Logg.I("update db from 3 to 4 success");
				break;
			}
		}
		if (newVersion == 5) {
			switch (oldVersion) {
			case 1:
				db.beginTransaction();
				db.execSQL("ALTER TABLE " + NoteDBField.TABLE_NAME + " ADD COLUMN " + NoteDBField.IMAGESPANINFOS + " BLOB;");
				// 实现剪裁图表
				db.execSQL("CREATE TABLE IF NOT EXISTS " + NoteDBField.CROPPED_IMGS_TABLE_NAME + " ( " + NoteDBField.IMGPATH + " VARCHAR PRIMARY KEY, "
						+ NoteDBField.IMAGE + " BLOB)");
				if (DEBUG)
					Logg.I("cropped imgs table created when upgrade");

				// 实现缩略图表
				db.execSQL("CREATE TABLE IF NOT EXISTS " + NoteDBField.THUMBNAILS_TABLE_NAME + " ( " + NoteDBField.IMGPATH + " VARCHAR PRIMARY KEY, "
						+ NoteDBField.IMAGE + " BLOB)");
				if (DEBUG)
					Logg.I("thumbnails table created when upgrade");
				addThumbnailCount(db);
				db.setTransactionSuccessful();
				db.endTransaction();
				if (DEBUG)
					Logg.I("update db from 1 to 5 success");
				break;
			case 2:
				db.beginTransaction();
				// 1、修改原表的名称
				db.execSQL("ALTER TABLE " + NoteDBField.TABLE_NAME + " RENAME TO temp");
				// 2、新建修改字段后的表
				db.execSQL("CREATE TABLE IF NOT EXISTS " + NoteDBField.TABLE_NAME + " ( " + NoteDBField.TITLE + " VARCHAR, " + NoteDBField.ID
						+ " INTEGER PRIMARY KEY, " + NoteDBField.CONTENT + " VARCHAR, " + NoteDBField.TIME + " VARCHAR," + NoteDBField.YEAR + " VARCHAR,"
						+ NoteDBField.MONTH + " VARCHAR," + NoteDBField.DAY + " VARCHAR," + NoteDBField.HOUR + " VARCHAR," + NoteDBField.MINUTE + " VARCHAR,"
						+ NoteDBField.IMGPATH + " VARCHAR," + NoteDBField.SECOND + " VARCHAR)");
				db.execSQL("ALTER TABLE " + NoteDBField.TABLE_NAME + " ADD COLUMN " + NoteDBField.IMAGESPANINFOS + " BLOB;");
				// 3、从旧表中查询出数据 并插入新表
				db.execSQL("INSERT INTO " + NoteDBField.TABLE_NAME + " SELECT " + NoteDBField.TITLE + "," + NoteDBField.ID + "," + NoteDBField.CONTENT + ","
						+ NoteDBField.TIME + "," + NoteDBField.YEAR + "," + NoteDBField.MONTH + "," + NoteDBField.DAY + "," + NoteDBField.HOUR + ","
						+ NoteDBField.MINUTE + "," + NoteDBField.IMGPATH + "," + NoteDBField.SECOND + "," + NoteDBField.IMGPATHS + " FROM temp;");
				// 4、删除旧表
				db.execSQL("DROP TABLE temp");

				// 实现剪裁图表
				db.execSQL("CREATE TABLE IF NOT EXISTS " + NoteDBField.CROPPED_IMGS_TABLE_NAME + " ( " + NoteDBField.IMGPATH + " VARCHAR PRIMARY KEY, "
						+ NoteDBField.IMAGE + " BLOB)");
				if (DEBUG)
					Logg.I("cropped imgs table created when upgrade");
				// 实现缩略图表
				db.execSQL("CREATE TABLE IF NOT EXISTS " + NoteDBField.THUMBNAILS_TABLE_NAME + " ( " + NoteDBField.IMGPATH + " VARCHAR PRIMARY KEY, "
						+ NoteDBField.IMAGE + " BLOB)");
				if (DEBUG)
					Logg.I("thumbnails table created when upgrade");
				addThumbnailCount(db);
				db.setTransactionSuccessful();
				db.endTransaction();
				if (DEBUG)
					Logg.I("update db from 2 to 5 success");
				break;
			case 3:
				db.beginTransaction();
				// 实现剪裁图表
				db.execSQL("CREATE TABLE IF NOT EXISTS " + NoteDBField.CROPPED_IMGS_TABLE_NAME + " ( " + NoteDBField.IMGPATH + " VARCHAR PRIMARY KEY, "
						+ NoteDBField.IMAGE + " BLOB)");
				if (DEBUG)
					Logg.I("cropped imgs table created when upgrade");
				// 实现缩略图表
				db.execSQL("CREATE TABLE IF NOT EXISTS " + NoteDBField.THUMBNAILS_TABLE_NAME + " ( " + NoteDBField.IMGPATH + " VARCHAR PRIMARY KEY, "
						+ NoteDBField.IMAGE + " BLOB)");
				if (DEBUG)
					Logg.I("thumbnails table created when upgrade");
				addThumbnailCount(db);
				db.setTransactionSuccessful();
				db.endTransaction();
				if (DEBUG)
					Logg.I("update db from 3 to 5 success");
				break;
			case 4:
				addThumbnailCount(db);
				if (DEBUG)
					Logg.I("update db from 4 to 5 success");
				break;
			}
			Config.NEED_DB_SERVICE = true;
		}
	}

	private void addThumbnailCount(SQLiteDatabase db) {
		db.execSQL("ALTER TABLE " + NoteDBField.THUMBNAILS_TABLE_NAME + " ADD COLUMN " + NoteDBField.THUMBNAILS_COUNT + " VARCHAR;");
		if (DEBUG)
			Logg.I("ALTER TABLE " + NoteDBField.THUMBNAILS_TABLE_NAME + " ADD COLUMN " + NoteDBField.THUMBNAILS_COUNT + " VARCHAR;");
	}
}
