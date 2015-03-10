package com.mars.note.database;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.mars.note.R;
import com.mars.note.api.Config;
import com.mars.note.app.EditorActivity;
import com.mars.note.utils.Logg;

import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.Toast;

public class NoteDataBaseManager {
	private NoteDatabaseHelper mRecordsDBHelper;
	private SQLiteDatabase db;
	private Context mContext;
	private boolean DEBUG = false;

	/**
	 * 当写操作时，其他线程无法读取或写入数据，而当读操作时，其它线程无法写入数据，但却可以读取数据 。
	 */
	private ReentrantReadWriteLock mCROPIMGS_RW_LOCK; // 剪裁图的读写锁

	public NoteDataBaseManager(Context context) {
		this.mContext = context;
		mRecordsDBHelper = new NoteDatabaseHelper(mContext);
		db = mRecordsDBHelper.getWritableDatabase();
		mCROPIMGS_RW_LOCK = new ReentrantReadWriteLock(true);// 公平锁
	}

	/**
	 * 添加缩略图到数据库，无须加同步锁，同步锁由调用层完成
	 * 
	 * @param path
	 * @param bm
	 */
	public void addNewThumbnail(String path, Bitmap bm) {
		// if (isThumbnailExisted(path)) {
		// // 如果path已经存在
		// if (DEBUG)
		// Logg.S("addNewThumbnail path " + path + " exsited ,abort ...");
		// return;
		// }
		// 如果path不存在
		final ByteArrayOutputStream os = new ByteArrayOutputStream();
		bm.compress(Bitmap.CompressFormat.PNG, 100, os);
		ContentValues cv = new ContentValues();
		cv.put(NoteDBField.IMGPATH, path);
		cv.put(NoteDBField.IMAGE, os.toByteArray());
		db.insert(NoteDBField.THUMBNAILS_TABLE_NAME, null, cv);
		if (DEBUG)
			Logg.S("addNewThumbnail path " + path + " inserted ...");
	}

	/**
	 * 判断缩略图是否存在
	 * 
	 * @param path
	 * @return
	 */
	public boolean isThumbnailExisted(String path) {
		if (path == null || "".equals(path))
			return false;
		String table = NoteDBField.THUMBNAILS_TABLE_NAME;
		String[] columns = { NoteDBField.IMAGE };
		String selection = NoteDBField.IMGPATH + " = ?";
		String[] selectionArgs = { path };
		Cursor c = db.query(table, columns, selection, selectionArgs, null, null, null);
		if (c.moveToNext()) {
			c.close();
			return true;
		}
		c.close();
		return false;
	}

	/**
	 * 添加新的剪切图到数据库
	 * 
	 * @param path
	 * @param bm
	 */
	public void addNewCroppedImage(String path, Bitmap bm) {
		// 考虑多个线程同时进入，确保只有一个线程获取写锁
		mCROPIMGS_RW_LOCK.writeLock().lock();
		if (isCroppedImageExisted(path)) {
			// 如果path已经存在
			mCROPIMGS_RW_LOCK.writeLock().unlock();
			if (DEBUG)
				Logg.S("addNewCroppedImage path " + path + " exsited ,abort ...");
			return;
		}
		// 如果path不存在
		final ByteArrayOutputStream os = new ByteArrayOutputStream();
		bm.compress(Bitmap.CompressFormat.PNG, 100, os);
		ContentValues cv = new ContentValues();
		cv.put(NoteDBField.IMGPATH, path);
		cv.put(NoteDBField.IMAGE, os.toByteArray());
		db.insert(NoteDBField.CROPPED_IMGS_TABLE_NAME, null, cv);
		mCROPIMGS_RW_LOCK.writeLock().unlock();
		if (DEBUG)
			Logg.S("addNewCroppedImage path " + path + " inserted ...");
	}

	/**
	 * 获取剪贴图
	 * 
	 * @param path
	 *            主键，路径
	 * @return
	 */
	public Bitmap getCroppedImage(String path) {
		// 其他线程可读，但是不能写
		mCROPIMGS_RW_LOCK.readLock().lock();
		String table = NoteDBField.CROPPED_IMGS_TABLE_NAME;
		String[] columns = { NoteDBField.IMAGE };
		String selection = NoteDBField.IMGPATH + " = ?";
		String[] selectionArgs = { path };
		Cursor c = db.query(table, columns, selection, selectionArgs, null, null, null);
		if (c.moveToNext()) {
			if (DEBUG)
				Logg.S("find image from sqlite ...");
			byte[] bytes = c.getBlob(c.getColumnIndex(NoteDBField.IMAGE));
			mCROPIMGS_RW_LOCK.readLock().unlock();
			Bitmap bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
			if (DEBUG)
				Logg.S("revert bytes to bitmap done ...");
			c.close();
			return bm;
		}
		mCROPIMGS_RW_LOCK.readLock().unlock();
		Logg.S("getCroppedImage error path " + path);
		c.close();
		return null;
	}

	/**
	 * 从数据库中获取缩略图（不加锁）
	 * 
	 * @param path
	 * @return
	 */
	public Bitmap getThumbnail(String path) {
		String table = NoteDBField.THUMBNAILS_TABLE_NAME;
		String[] columns = { NoteDBField.IMAGE };
		String selection = NoteDBField.IMGPATH + " = ?";
		String[] selectionArgs = { path };
		Cursor c = db.query(table, columns, selection, selectionArgs, null, null, null);
		if (c.moveToNext()) {
			if (DEBUG)
				Logg.S("getThumbnail find image from sqlite ...");
			byte[] bytes = c.getBlob(c.getColumnIndex(NoteDBField.IMAGE));
//			BitmapFactory.Options newOpts = new BitmapFactory.Options();
//			newOpts.inJustDecodeBounds = true;
//			Bitmap bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, newOpts);
//			newOpts.inJustDecodeBounds = false;
//			newOpts.inSampleSize = 3;
//			bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, newOpts);
			
			Bitmap bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
			
			if (DEBUG)
				Logg.S("getThumbnail revert bytes to bitmap done ...");
			c.close();
			return bm;
		}
		if (DEBUG)
			Logg.S("getThumbnail error path " + path);
		c.close();
		return null;
		// throw new NullPointerException("bitmap not found from sqlite ...");
	}

	/**
	 * 判断剪裁图是否存在于数据库
	 * 
	 * @param path
	 *            图片的绝对地址，用于区别图片
	 * @return true 存在，false 不存在
	 */
	public boolean isCroppedImageExisted(String path) {
		if (path == null || "".equals(path))
			return false;

		String table = NoteDBField.CROPPED_IMGS_TABLE_NAME;
		String[] columns = { NoteDBField.IMAGE };
		String selection = NoteDBField.IMGPATH + " = ?";
		String[] selectionArgs = { path };
		Cursor c = db.query(table, columns, selection, selectionArgs, null, null, null);
		if (c.moveToNext()) {
			c.close();
			return true;
		}
		c.close();
		return false;
	}

	private boolean isCroppedImageRelatedToRecord(String path) {
		if (path == null || "".equals(path))
			return false;

		String table = NoteDBField.TABLE_NAME;
		String[] columns = { NoteDBField.IMGPATH };
		String selection = NoteDBField.IMGPATH + " = ?";
		String[] selectionArgs = { path };
		Cursor c = db.query(table, columns, selection, selectionArgs, null, null, null);
		if (c.moveToNext()) {
			c.close();
			return true;
		}
		c.close();
		return false;
	}

	private Cursor getCursor(String sql) {
		return db.rawQuery(sql, null);
	}

	public Cursor getCursor(String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy) {
		return db.query(table, columns, selection, selectionArgs, groupBy, having, orderBy);
	}

	public void openDB() {
		mRecordsDBHelper = new NoteDatabaseHelper(mContext);
		db = mRecordsDBHelper.getWritableDatabase();
	}

	public void closeDB() {
		db.close();
		db = null;
		mRecordsDBHelper = null;
	}

	/**
	 * 清空widget表的所有关联，并刷新桌面
	 */
	public synchronized void clearWidgetsRelations() {
		db.execSQL("DELETE FROM " + NoteDBField.WIDGETS_TABLE_NAME);
		Intent intent = new Intent("com.mars.note.widget.clearall");
		mContext.sendBroadcast(intent);
	}

	public synchronized void addRecords(List<NoteRecord> addList) {
		Iterator<NoteRecord> it = addList.iterator();
		try {
			db.beginTransaction();
			while (it.hasNext()) {
				NoteRecord nr = (NoteRecord) it.next();
				ContentValues cv = new ContentValues();
				cv.put(NoteDBField.TITLE, nr.title);
				cv.put(NoteDBField.ID, nr.id);
				cv.put(NoteDBField.CONTENT, nr.content);
				cv.put(NoteDBField.TIME, nr.time);
				cv.put(NoteDBField.YEAR, nr.year);
				cv.put(NoteDBField.MONTH, nr.month);
				cv.put(NoteDBField.DAY, nr.day);
				cv.put(NoteDBField.HOUR, nr.hour);
				cv.put(NoteDBField.MINUTE, nr.minute);
				cv.put(NoteDBField.SECOND, nr.second);
				cv.put(NoteDBField.IMGPATH, nr.imgpath);
				cv.put(NoteDBField.IMAGESPANINFOS, nr.imageSpanInfos);
				db.insert(NoteDBField.TABLE_NAME, null, cv);
			}
			db.setTransactionSuccessful();
			refreshWidgetCollections();
		} catch (Exception ex) {
			System.err.println("err");
		} finally {
			db.endTransaction();
		}
	}

	public synchronized void addWidgetRelation(String widgetID, String recordID) {
		ContentValues cv = new ContentValues();
		cv.put(NoteDBField.WIDGETS_ID, widgetID);
		cv.put(NoteDBField.NOTES_ID, recordID);
		db.insert(NoteDBField.WIDGETS_TABLE_NAME, null, cv);
		Toast.makeText(mContext, R.string.toast_success, 1000).show();
	}

	public synchronized void deleteWidgetRelation(String widgetID) {
		db.execSQL("DELETE FROM " + NoteDBField.WIDGETS_TABLE_NAME + " WHERE " + NoteDBField.WIDGETS_ID + " = " + widgetID);
	}

	public synchronized String queryWidgetIDByNoteID(String noteID) {
		Cursor c = getCursor("SELECT " + NoteDBField.WIDGETS_ID + " FROM " + NoteDBField.WIDGETS_TABLE_NAME + " WHERE " + NoteDBField.NOTES_ID + " = " + noteID);
		String widgetID = null;
		if (c.moveToNext()) {
			widgetID = c.getString(c.getColumnIndex(NoteDBField.WIDGETS_ID));
		}
		c.close();
		return widgetID;
	}

	public synchronized String querySingleRecordIDByWidgetID(String widgetID) {
		Cursor c = getCursor("SELECT " + NoteDBField.NOTES_ID + " FROM " + NoteDBField.WIDGETS_TABLE_NAME + " WHERE " + NoteDBField.WIDGETS_ID + " = "
				+ widgetID);
		String noteID = null;
		if (c.moveToNext()) {
			noteID = c.getString(c.getColumnIndex(NoteDBField.NOTES_ID));
		}
		c.close();
		return noteID;
	}

	public synchronized String querySingleRecordIDByTime(String time) {
		Cursor c = getCursor("SELECT " + NoteDBField.ID + " FROM " + NoteDBField.TABLE_NAME + " WHERE " + NoteDBField.TIME + " = " + time);
		String ID = null;
		if (c.moveToNext()) {
			ID = c.getString(c.getColumnIndex(NoteDBField.ID));
		}
		c.close();
		return ID;
	}

	public synchronized long addRecord(NoteRecord nr) {
		ContentValues cv = new ContentValues();
		cv.put(NoteDBField.TITLE, nr.title);
		cv.put(NoteDBField.ID, nr.id);
		cv.put(NoteDBField.CONTENT, nr.content);
		cv.put(NoteDBField.TIME, nr.time);
		cv.put(NoteDBField.YEAR, nr.year);
		cv.put(NoteDBField.MONTH, nr.month);
		cv.put(NoteDBField.DAY, nr.day);
		cv.put(NoteDBField.HOUR, nr.hour);
		cv.put(NoteDBField.MINUTE, nr.minute);
		cv.put(NoteDBField.SECOND, nr.second);
		cv.put(NoteDBField.IMGPATH, nr.imgpath);
		cv.put(NoteDBField.IMAGESPANINFOS, nr.imageSpanInfos);

		long returnID = -1;

		try {
			db.beginTransaction();
			// for (int i = 0; i < 20; i++) {
			// cv.put(NoteDBField.CONTENT, i);
			returnID = db.insert(NoteDBField.TABLE_NAME, null, cv);
			// }
			db.setTransactionSuccessful();
			refreshWidgetCollections();
		} catch (Exception ex) {
			System.err.println("err");
			returnID = -1;
		} finally {
			db.endTransaction();
		}
		return returnID;
	}

	public synchronized int[] getCurrentMonthRecordCount(int year, int month, int totalDaysOfMonth) {
		int[] currentMonthRecord = new int[totalDaysOfMonth];
		for (int i = 1; i <= totalDaysOfMonth; i++) {
			int count = this.getDayRecordCount(year, month, i);
			currentMonthRecord[(i - 1)] = count;
		}
		return currentMonthRecord;
	}

	public synchronized List<NoteRecord> querySelectedRecords(int year, int month, int dayOfMonth) {
		ArrayList<NoteRecord> recordInfos = new ArrayList<NoteRecord>();
		Cursor c = getCursor("SELECT * FROM " + NoteDBField.TABLE_NAME + " WHERE " + NoteDBField.YEAR + " = " + year + " AND " + NoteDBField.MONTH + " = "
				+ month + " AND " + NoteDBField.DAY + " = " + dayOfMonth + " ORDER BY " + NoteDBField.TIME + " DESC");
		while (c.moveToNext()) {
			NoteRecord nr = new NoteRecord();
			nr.title = c.getString(c.getColumnIndex(NoteDBField.TITLE));
			nr.id = c.getString(c.getColumnIndex(NoteDBField.ID));
			nr.content = c.getString(c.getColumnIndex(NoteDBField.CONTENT));
			nr.time = c.getString(c.getColumnIndex(NoteDBField.TIME));
			nr.year = c.getString(c.getColumnIndex(NoteDBField.YEAR));
			nr.month = c.getString(c.getColumnIndex(NoteDBField.MONTH));
			nr.day = c.getString(c.getColumnIndex(NoteDBField.DAY));
			nr.hour = c.getString(c.getColumnIndex(NoteDBField.HOUR));
			nr.minute = c.getString(c.getColumnIndex(NoteDBField.MINUTE));
			nr.second = c.getString(c.getColumnIndex(NoteDBField.SECOND));
			nr.imgpath = c.getString(c.getColumnIndex(NoteDBField.IMGPATH));
			nr.imageSpanInfos = c.getBlob(c.getColumnIndex(NoteDBField.IMAGESPANINFOS));
			recordInfos.add(nr);
		}
		c.close();
		Long after = System.currentTimeMillis();
		return recordInfos;
	}

	public synchronized List<NoteRecord> queryDividedPagerRecords(int currentPageNum, int from, int num) {
		ArrayList<NoteRecord> recordInfos = new ArrayList<NoteRecord>();
		Cursor c = getCursor("SELECT * FROM " + NoteDBField.TABLE_NAME + " ORDER BY " + NoteDBField.TIME + " DESC LIMIT " + from * num + "," + currentPageNum
				* num);
		while (c.moveToNext()) {
			NoteRecord nr = new NoteRecord();
			nr.title = c.getString(c.getColumnIndex(NoteDBField.TITLE));
			nr.id = c.getString(c.getColumnIndex(NoteDBField.ID));
			nr.content = c.getString(c.getColumnIndex(NoteDBField.CONTENT));
			nr.time = c.getString(c.getColumnIndex(NoteDBField.TIME));
			nr.year = c.getString(c.getColumnIndex(NoteDBField.YEAR));
			nr.month = c.getString(c.getColumnIndex(NoteDBField.MONTH));
			nr.day = c.getString(c.getColumnIndex(NoteDBField.DAY));
			nr.hour = c.getString(c.getColumnIndex(NoteDBField.HOUR));
			nr.minute = c.getString(c.getColumnIndex(NoteDBField.MINUTE));
			nr.second = c.getString(c.getColumnIndex(NoteDBField.SECOND));
			nr.imgpath = c.getString(c.getColumnIndex(NoteDBField.IMGPATH));
			nr.imageSpanInfos = c.getBlob(c.getColumnIndex(NoteDBField.IMAGESPANINFOS));
			recordInfos.add(nr);
		}
		c.close();
		return recordInfos;
	}

	public synchronized int getAllRecordsQuantity() {
		Cursor c = getCursor("SELECT COUNT(*) AS TOTALCOUNT FROM " + NoteDBField.TABLE_NAME);
		if (c.moveToNext()) {
			int out = Integer.parseInt(c.getString(c.getColumnIndex("TOTALCOUNT")));
			c.close();
			return out;
		} else {
			c.close();
			return -1;
		}
	}

	public synchronized int getMaxRecordsCount() {
		Cursor c = getCursor("SELECT * FROM " + NoteDBField.TABLE_NAME + " ORDER BY " + NoteDBField.TIME + " DESC LIMIT " + 0 + "," + Config.maxShownCount);
		int result = c.getCount();
		c.close();
		return result;
	}

	public synchronized int getDayRecordCount(int year, int month, int dayOfMonth) {
		Cursor c = getCursor("SELECT COUNT(*) AS TOTALCOUNT FROM " + NoteDBField.TABLE_NAME + " WHERE " + NoteDBField.YEAR + " = " + year + " AND "
				+ NoteDBField.MONTH + " = " + month + " AND " + NoteDBField.DAY + " = " + dayOfMonth);
		int count = -1;
		if (c.moveToNext()) {
			count = Integer.parseInt(c.getString(c.getColumnIndex("TOTALCOUNT")));
		}
		c.close();
		return count;
	}

	public synchronized ArrayList<NoteRecord> queryAllRecords() {
		ArrayList<NoteRecord> recordInfos = new ArrayList<NoteRecord>();
		Cursor c = getCursor("SELECT * FROM " + NoteDBField.TABLE_NAME + " ORDER BY " + NoteDBField.TIME + " DESC");
		while (c.moveToNext()) {
			NoteRecord nr = new NoteRecord();
			nr.title = c.getString(c.getColumnIndex(NoteDBField.TITLE));
			nr.id = c.getString(c.getColumnIndex(NoteDBField.ID));
			nr.content = c.getString(c.getColumnIndex(NoteDBField.CONTENT));
			nr.time = c.getString(c.getColumnIndex(NoteDBField.TIME));
			nr.year = c.getString(c.getColumnIndex(NoteDBField.YEAR));
			nr.month = c.getString(c.getColumnIndex(NoteDBField.MONTH));
			nr.day = c.getString(c.getColumnIndex(NoteDBField.DAY));
			nr.hour = c.getString(c.getColumnIndex(NoteDBField.HOUR));
			nr.minute = c.getString(c.getColumnIndex(NoteDBField.MINUTE));
			nr.second = c.getString(c.getColumnIndex(NoteDBField.SECOND));
			nr.imgpath = c.getString(c.getColumnIndex(NoteDBField.IMGPATH));
			nr.imageSpanInfos = c.getBlob(c.getColumnIndex(NoteDBField.IMAGESPANINFOS));
			recordInfos.add(nr);
		}
		c.close();
		return recordInfos;
	}

	public synchronized NoteRecord querySingleRecord(String id) {
		Cursor c = getCursor("SELECT * FROM " + NoteDBField.TABLE_NAME + " WHERE " + NoteDBField.ID + " = " + id);
		NoteRecord nr = null;
		if (c.moveToNext()) {
			nr = new NoteRecord();
			nr.title = c.getString(c.getColumnIndex(NoteDBField.TITLE));
			nr.id = c.getString(c.getColumnIndex(NoteDBField.ID));
			nr.content = c.getString(c.getColumnIndex(NoteDBField.CONTENT));
			nr.time = c.getString(c.getColumnIndex(NoteDBField.TIME));
			nr.year = c.getString(c.getColumnIndex(NoteDBField.YEAR));
			nr.month = c.getString(c.getColumnIndex(NoteDBField.MONTH));
			nr.day = c.getString(c.getColumnIndex(NoteDBField.DAY));
			nr.hour = c.getString(c.getColumnIndex(NoteDBField.HOUR));
			nr.minute = c.getString(c.getColumnIndex(NoteDBField.MINUTE));
			nr.second = c.getString(c.getColumnIndex(NoteDBField.SECOND));
			nr.imgpath = c.getString(c.getColumnIndex(NoteDBField.IMGPATH));
			nr.imageSpanInfos = c.getBlob(c.getColumnIndex(NoteDBField.IMAGESPANINFOS));
		}
		c.close();
		return nr;
	}

	public synchronized void deleteRecord(NoteRecord nr) {
		db.beginTransaction();
		db.execSQL("DELETE FROM " + NoteDBField.TABLE_NAME + " WHERE " + NoteDBField.ID + " = " + nr.id);
		if (isCroppedImageRelatedToRecord(nr.imgpath)) {
			if (DEBUG)
				Logg.S("path " + nr.imgpath + " related to others");
		} else {
			if (DEBUG)
				Logg.S("path " + nr.imgpath + " not related to others");
			db.delete(NoteDBField.CROPPED_IMGS_TABLE_NAME, NoteDBField.IMGPATH + " = ? ", new String[] { nr.imgpath });
		}
		db.setTransactionSuccessful();
		db.endTransaction();

		String widgetId = queryWidgetIDByNoteID(nr.id);
		if (widgetId != null) {
			deleteWidgetRelation(widgetId);
			Intent intent = new Intent("com.mars.note.widget.delete");
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
			mContext.sendBroadcast(intent);
		}
		refreshWidgetCollections();
	}

	/**
	 * 刷新widget集合
	 */
	public void refreshWidgetCollections() {
		Intent intent = new Intent("com.mars.note.widgetcollections.refresh");
		mContext.sendBroadcast(intent);
	}

	public synchronized void batchDeleteRecord(List<NoteRecord> nrList) {
		try {
			db.beginTransaction();
			for (NoteRecord nr : nrList) {
				// deleteRecord(nr.id);
				db.execSQL("DELETE FROM " + NoteDBField.TABLE_NAME + " WHERE " + NoteDBField.ID + " = " + nr.id);
				if (isCroppedImageRelatedToRecord(nr.imgpath)) {
					if (DEBUG)
						Logg.S("path " + nr.imgpath + " related to others");

				} else {
					if (DEBUG)
						Logg.S("path " + nr.imgpath + " not related to others");
					db.delete(NoteDBField.CROPPED_IMGS_TABLE_NAME, NoteDBField.IMGPATH + " = ? ", new String[] { nr.imgpath });
				}

				String widgetId = queryWidgetIDByNoteID(nr.id);
				if (widgetId != null) {
					deleteWidgetRelation(widgetId);
					Intent intent = new Intent("com.mars.note.widget.delete");
					intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
					mContext.sendBroadcast(intent);
				}
			}
			db.setTransactionSuccessful();
			refreshWidgetCollections();
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			db.endTransaction();
		}
	}

	public synchronized void updateRecord(NoteRecord nr) {
		// db.execSQL("UPDATE " + NoteDBField.TABLE_NAME + " SET "
		// + NoteDBField.TITLE + " = '" + nr.title + "', "
		// + NoteDBField.IMGPATH + " = '" + nr.imgpath + "', "
		// + NoteDBField.TIME + " = '" + nr.time + "', "
		// + NoteDBField.YEAR + " = '" + nr.year + "', "
		// + NoteDBField.MONTH + " = '" + nr.month + "', "
		// + NoteDBField.DAY + " = '" + nr.day + "', " + NoteDBField.HOUR
		// + " = '" + nr.hour + "', " + NoteDBField.MINUTE + " = '"
		// + nr.minute + ""
		// + "', " + NoteDBField.SECOND + " = '" + nr.second
		// + "', " + NoteDBField.CONTENT + " = '" + nr.content + "'"
		// + "', " + NoteDBField.IMAGESPANINFOS + " = '" +
		// nr.imageSpanInfos.toString() + "'"
		// + " WHERE " + NoteDBField.ID + " = '" + nr.id + "'");
		// bug 不能更新blog 20141211
		ContentValues cv = new ContentValues();
		cv.put(NoteDBField.TITLE, nr.title);
		cv.put(NoteDBField.ID, nr.id);
		cv.put(NoteDBField.CONTENT, nr.content);
		cv.put(NoteDBField.TIME, nr.time);
		cv.put(NoteDBField.YEAR, nr.year);
		cv.put(NoteDBField.MONTH, nr.month);
		cv.put(NoteDBField.DAY, nr.day);
		cv.put(NoteDBField.HOUR, nr.hour);
		cv.put(NoteDBField.MINUTE, nr.minute);
		cv.put(NoteDBField.SECOND, nr.second);
		cv.put(NoteDBField.IMGPATH, nr.imgpath);
		cv.put(NoteDBField.IMAGESPANINFOS, nr.imageSpanInfos);
		String[] args = { nr.id };
		db.update(NoteDBField.TABLE_NAME, cv, NoteDBField.ID + "=?", args);
		refreshWidgetCollections();
	}

	public synchronized List<NoteRecord> querySelectedRecords(String key) {
		ArrayList<NoteRecord> recordInfos = new ArrayList<NoteRecord>();
		Cursor c = getCursor("SELECT * FROM " + NoteDBField.TABLE_NAME + " WHERE " + NoteDBField.TITLE + " LIKE '%" + key + "%' OR " + NoteDBField.CONTENT
				+ " LIKE '%" + key + "%'" + " ORDER BY " + NoteDBField.TIME + " DESC");
		while (c.moveToNext()) {
			NoteRecord nr = new NoteRecord();
			nr.title = c.getString(c.getColumnIndex(NoteDBField.TITLE));
			nr.id = c.getString(c.getColumnIndex(NoteDBField.ID));
			nr.content = c.getString(c.getColumnIndex(NoteDBField.CONTENT));
			nr.time = c.getString(c.getColumnIndex(NoteDBField.TIME));
			nr.year = c.getString(c.getColumnIndex(NoteDBField.YEAR));
			nr.month = c.getString(c.getColumnIndex(NoteDBField.MONTH));
			nr.day = c.getString(c.getColumnIndex(NoteDBField.DAY));
			nr.hour = c.getString(c.getColumnIndex(NoteDBField.HOUR));
			nr.minute = c.getString(c.getColumnIndex(NoteDBField.MINUTE));
			nr.second = c.getString(c.getColumnIndex(NoteDBField.SECOND));
			nr.imgpath = c.getString(c.getColumnIndex(NoteDBField.IMGPATH));
			nr.imageSpanInfos = c.getBlob(c.getColumnIndex(NoteDBField.IMAGESPANINFOS));
			recordInfos.add(nr);
		}
		c.close();
		return recordInfos;
	}

	public void beginTransaction() {
		db.beginTransaction();
	}

	public void endTransaction() {
		db.setTransactionSuccessful();
		db.endTransaction();
	}

	public synchronized void deleteImagePath(NoteRecord nr) {
		ContentValues cv = new ContentValues();
		cv.put(NoteDBField.TITLE, nr.title);
		cv.put(NoteDBField.ID, nr.id);
		cv.put(NoteDBField.CONTENT, nr.content);
		cv.put(NoteDBField.TIME, nr.time);
		cv.put(NoteDBField.YEAR, nr.year);
		cv.put(NoteDBField.MONTH, nr.month);
		cv.put(NoteDBField.DAY, nr.day);
		cv.put(NoteDBField.HOUR, nr.hour);
		cv.put(NoteDBField.MINUTE, nr.minute);
		cv.put(NoteDBField.SECOND, nr.second);
		cv.put(NoteDBField.IMGPATH, "");
		cv.put(NoteDBField.IMAGESPANINFOS, nr.imageSpanInfos);
		String[] args = { nr.id };
		db.update(NoteDBField.TABLE_NAME, cv, NoteDBField.ID + "=?", args);
		refreshWidgetCollections();
	}
}