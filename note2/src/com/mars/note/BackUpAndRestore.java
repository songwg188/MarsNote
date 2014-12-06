package com.mars.note;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import com.mars.note.R;
import com.mars.note.database.NoteDBField;
import com.mars.note.database.NoteDataBaseManager;
import com.mars.note.utils.FileHelper;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class BackUpAndRestore extends Activity {
	String TAG = "BackUpAndRestore";
	ListView mListView;
	BaseAdapter mAdapter;
	List<BackupDoc> mData;
//	public static final String BACKUP_PATH = "/sdcard/mars/";
	public static final String BACKUP_PATH = Environment.getExternalStorageDirectory().getPath()+"/mars/" ;
	String[] date_title;
	String[] time_title;
	TextView listDescription;
	NoteDataBaseManager noteDBManager;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY); //Ðü¸¡Actionbar 20141202
		
		this.getActionBar().setDisplayHomeAsUpEnabled(true);
		this.getActionBar().setDisplayShowHomeEnabled(false);
		this.setContentView(R.layout.activity_backup_restore);
		date_title = this.getResources().getStringArray(R.array.date_title);
		time_title = this.getResources().getStringArray(R.array.time_title);
		mListView = (ListView) this.findViewById(R.id.backup_listview);
		noteDBManager = NoteApplication.getDbManager();
		readData();
		mAdapter = new BackupListAdapter();
		mListView.setAdapter(mAdapter);
		listDescription = new TextView(this);
		listDescription.setTextSize(18);
		listDescription.setTextColor(Color.DKGRAY);
		listDescription.setPadding(20, 20, 20, 0);
		listDescription.setGravity(Gravity.LEFT);
		listDescription.setText(R.string.backup_listheader_title);
		mListView.addHeaderView(listDescription);
	}

	@Override
	public void onResume() {
		super.onResume();
		readData();
		mAdapter.notifyDataSetChanged();
	}

	private void readData() {
		try {
			mData = FileHelper.getBackupDocs(BACKUP_PATH);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			this.onBackPressed();
			break;
		case R.id.add_backup:
			showSaveDialog();
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.backup_restore, menu);
		return true;
	}

	private class BackupListAdapter extends BaseAdapter {
		LayoutInflater inflater;

		public BackupListAdapter() {
			inflater = (LayoutInflater) BackUpAndRestore.this
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			return mData.size();
		}

		@Override
		public Object getItem(int arg0) {
			return mData.get(arg0);
		}

		@Override
		public long getItemId(int arg0) {
			return arg0;
		}

		@Override
		public View getView(int positon, View v, ViewGroup arg2) {
			if (v == null) {
				View item = inflater.inflate(R.layout.backup_list_item, null);
				v = item;
			}
			TextView fileName = (TextView) v.findViewById(R.id.file_name);
			fileName.setTextSize(18);
			fileName.setTextColor(Color.DKGRAY);
			String dateText = mData.get(positon).fileName;
			String timeText = "";
			dateText = dateText.substring(9, dateText.length() - 4);
			long dateTime = 0;
			try {
				dateTime = Long.parseLong(dateText);
				Date date = new Date();
				date.setTime(dateTime);
				Calendar cal = Calendar.getInstance();
				cal.setTime(date);
				dateText = BackUpAndRestore.this
						.getString(R.string.backup_date_title)
						+ (cal.get(Calendar.YEAR))
						+ date_title[0]
						+ (cal.get(Calendar.MONTH) + 1)
						+ date_title[1]
						+ cal.get(Calendar.DAY_OF_MONTH) + date_title[2];
				timeText = BackUpAndRestore.this
						.getString(R.string.backup_time_title)
						+ (cal.get(Calendar.HOUR_OF_DAY))
						+ time_title[0]
						+ cal.get(Calendar.MINUTE)
						+ time_title[1]
						+ cal.get(Calendar.SECOND) + time_title[2];
			} catch (Exception e) {
				dateText = mData.get(positon).fileName;
				timeText = "";
			}
			fileName.setText(dateText);
			TextView fileDescription = (TextView) v
					.findViewById(R.id.file_description);
			fileDescription.setText(timeText);
			fileDescription.setTextSize(15);
			fileDescription.setTextColor(Color.DKGRAY);
			ImageButton delete = (ImageButton) v.findViewById(R.id.delete_file);
			delete.setTag(Integer.valueOf(positon));
			delete.setOnClickListener(new android.view.View.OnClickListener() {
				@Override
				public void onClick(View v) {
					final BackupDoc f = mData.get(((Integer) v.getTag())
							.intValue());
					AlertDialog.Builder builder = new AlertDialog.Builder(
							BackUpAndRestore.this);
					builder.setMessage(BackUpAndRestore.this
							.getString(R.string.delete_backup_title)
							+ "\n"
							+ f.fileName + "?");
					builder.setPositiveButton(R.string.yes,
							new OnClickListener() {
								@Override
								public void onClick(DialogInterface arg0,
										int arg1) {
									boolean result = FileHelper
											.deleteFile(f.path);
									if (result) {
										onResume();
									} else {
									}
								}
							});
					builder.setNegativeButton(R.string.no, null);
					builder.show();
				}
			});
			ImageButton restore = (ImageButton) v
					.findViewById(R.id.restore_file);
			restore.setTag(Integer.valueOf(positon));
			restore.setOnClickListener(new android.view.View.OnClickListener() {
				@Override
				public void onClick(View v) {
					final BackupDoc f = mData.get(((Integer) v.getTag())
							.intValue());
					AlertDialog.Builder builder = new AlertDialog.Builder(
							BackUpAndRestore.this);
					builder.setMessage(BackUpAndRestore.this.getString(
							R.string.restore_backup_title, f.fileName));
					builder.setPositiveButton(R.string.yes,
							new OnClickListener() {
								@Override
								public void onClick(DialogInterface arg0,
										int arg1) {
									File db = BackUpAndRestore.this
											.getDatabasePath(NoteDBField.DBNAME);
									File backup = new File(f.path);
									FileHelper.copyFile(backup, db);
									noteDBManager.clearWidgetsRelations();
									Config.recent_needRefresh = true;
									Config.search_needRefresh = true;
									Config.calendar_needRefresh = true;
									Toast.makeText(
											BackUpAndRestore.this,
											BackUpAndRestore.this
													.getText(R.string.toast_success),
											1000).show();
									noteDBManager.refreshWidgetCollections();
								}
							});
					builder.setNegativeButton(R.string.no, null);
					builder.show();
				}
			});
			return v;
		}
	}

	private void showSaveDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		Calendar cal = Calendar.getInstance();
		Date date = cal.getTime();
		final String fileName = "mar_note_" + date.getTime() + ".bak";
		builder.setMessage(this.getString(R.string.add_new_record_title) + "\n"
				+ fileName + "\n"
				+ this.getString(R.string.add_new_record_title_2) + BACKUP_PATH
				+ "?");
		builder.setPositiveButton(R.string.yes, new OnClickListener() {
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				addNewBackUp(fileName);
			}
		});
		builder.setNegativeButton(R.string.no, null);
		builder.show();
	}

	private void addNewBackUp(String fileName) {
		File dbFile = this.getDatabasePath(NoteDBField.DBNAME);
		File exportDir = new File(BACKUP_PATH);
		if (!exportDir.exists()) {
			exportDir.mkdirs();
		}
		File backup = new File(exportDir, fileName);
		try {
			backup.createNewFile();
			FileHelper.copyFile(dbFile, backup);
			onResume();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
