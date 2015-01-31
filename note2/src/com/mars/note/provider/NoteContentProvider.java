package com.mars.note.provider;

import com.mars.note.app.NoteApplication;
import com.mars.note.database.NoteDataBaseManager;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

/**
 * @author mars
 * @date 2015-1-27 上午10:27:54
 * @version 1.1
 */
public class NoteContentProvider extends ContentProvider {
	private static final String TAG = "NoteContentProvider";
	NoteDataBaseManager noteDBManager;

	private static final UriMatcher sMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	static {
		sMatcher.addURI("com.mars.note.provider.NoteContentProvider", "records", 1);
	}

	@Override
	public boolean onCreate() {
		//返回true否则不加载
		return true;
	}

	@Override
	public int delete(Uri arg0, String arg1, String[] arg2) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getType(Uri arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uri insert(Uri arg0, ContentValues arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		Log.d(TAG, "uri = " + uri.toString());
		if (sMatcher.match(uri) == 1) {
			return NoteApplication.getDbManager().getCursor("records", projection, selection, selectionArgs, null, null, sortOrder);
		}
		return null;
	}

	@Override
	public int update(Uri uri, ContentValues arg1, String arg2, String[] arg3) {

		return 0;
	}

}
