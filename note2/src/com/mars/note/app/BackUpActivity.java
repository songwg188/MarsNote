package com.mars.note.app;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import com.mars.note.R;
import com.mars.note.api.BackupDoc;
import com.mars.note.api.BaseActivity;
import com.mars.note.api.BaseFile;
import com.mars.note.api.Config;
import com.mars.note.api.Folder;
import com.mars.note.api.AlertDialogFactory;
import com.mars.note.database.NoteDBField;
import com.mars.note.database.NoteDataBaseManager;
import com.mars.note.utils.FileHelper;
import com.mars.note.utils.Logg;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

public class BackUpActivity extends BaseActivity {
	private boolean DEBUG = true;
	private String TAG = "BackUpAndRestore";
	private ListView mListView;
	private BaseAdapter mAdapter;
	private List<BaseFile> mData;
	// public static final String BACKUP_PATH = "/sdcard/mars/";
	public static final String BACKUP_PATH = Environment.getExternalStorageDirectory().getPath() + "/mars/";
	private String[] date_title;
	private String[] time_title;
	private LinearLayout listDescription;
	private NoteDataBaseManager noteDBManager;
	private ProgressDialog mExecutingDialog;
	private Handler mHandler;
	BroadcastReceiver mBroadCastReceiver;
	Intent serviceIntent;
	private DisplayMetrics dm = null;
	private int screenWidth;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (dm == null) {
			dm = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(dm);
		}
		screenWidth = dm.widthPixels;
		this.setContentView(R.layout.activity_backup_restore);
		date_title = this.getResources().getStringArray(R.array.date_title);
		time_title = this.getResources().getStringArray(R.array.time_title);
		mListView = (ListView) this.findViewById(R.id.backup_listview);
		noteDBManager = NoteApplication.getDbManager();
		readData(BACKUP_PATH);
		mAdapter = new BackupListAdapter();
		listDescription = new LinearLayout(this);
		listDescription.setPadding(20, 20, 20, 0);
		// mListView.addHeaderView(listDescription);
		mListView.setAdapter(mAdapter);
		initExecutingDialog();
		mHandler = new Handler();
		serviceIntent = new Intent(BackUpActivity.this, DBService.class);
		serviceIntent.putExtra("updateThumbnailsCount", true);
		serviceIntent.putExtra("screenWidth", screenWidth);

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction("com.mars.note.updateThumbnailsCount.finished");
		mBroadCastReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (DEBUG)
					Logg.S("BackUpActivity receive Broadcast");
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						// TODO Auto-generated method stub
						Toast.makeText(BackUpActivity.this, BackUpActivity.this.getText(R.string.toast_success), 1000).show();
						noteDBManager.refreshWidgetCollections();
						BackUpActivity.this.stopService(serviceIntent);
						dissmissExecutingDialog();
						Config.NEED_DB_SERVICE = false;
					}
				});
			}
		};
		registerReceiver(mBroadCastReceiver, intentFilter);
	}

	private void initExecutingDialog() {
		mExecutingDialog = new ProgressDialog(this);
		mExecutingDialog.setMessage(getString(R.string.dialog_executing_message));
		mExecutingDialog.setCancelable(false);
	}

	private void showExectingDialog() {
		mExecutingDialog.show();
	}

	private void dissmissExecutingDialog() {
		mExecutingDialog.dismiss();
	}

	@Override
	public void onResume() {
		super.onResume();
		readData(BACKUP_PATH);
		mAdapter.notifyDataSetChanged();
	}

	private void readData(String path) {
		try {
			mData = FileHelper.getBackupDocs(path);
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
			inflater = (LayoutInflater) BackUpActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
		public View getView(final int positon, View v, ViewGroup arg2) {
			if (mData.get(positon) instanceof BackupDoc) {
				v = inflater.inflate(R.layout.backup_list_item_bak, null);
				TextView fileName = (TextView) v.findViewById(R.id.file_name);
				fileName.setTextSize(18);
				fileName.setTextColor(Color.DKGRAY);
				String dateText = mData.get(positon).getName();
				String timeText = "";
				long dateTime = 0;
				try {
					dateTime = Long.parseLong(dateText);
					Date date = new Date();
					date.setTime(dateTime);
					Calendar cal = Calendar.getInstance();
					cal.setTime(date);
					dateText = BackUpActivity.this.getString(R.string.backup_date_title) + (cal.get(Calendar.YEAR)) + date_title[0]
							+ (cal.get(Calendar.MONTH) + 1) + date_title[1] + cal.get(Calendar.DAY_OF_MONTH) + date_title[2];
					timeText = BackUpActivity.this.getString(R.string.backup_time_title) + (cal.get(Calendar.HOUR_OF_DAY)) + time_title[0]
							+ cal.get(Calendar.MINUTE) + time_title[1] + cal.get(Calendar.SECOND) + time_title[2];
				} catch (Exception e) {
					dateText = mData.get(positon).getName();
					timeText = "";
				}
				fileName.setText(dateText);
				TextView fileDescription = (TextView) v.findViewById(R.id.file_description);
				fileDescription.setText(timeText);
				fileDescription.setTextSize(15);
				fileDescription.setTextColor(Color.DKGRAY);
				ImageButton delete = (ImageButton) v.findViewById(R.id.delete_file);
				delete.setTag(Integer.valueOf(positon));
				delete.setOnClickListener(new android.view.View.OnClickListener() {
					@Override
					public void onClick(View v) {
						final BaseFile f = mData.get(((Integer) v.getTag()).intValue());
						// AlertDialog.Builder builder = new
						// AlertDialog.Builder(BackUpActivity.this);
						// builder.setMessage(BackUpActivity.this.getString(R.string.delete_backup_title)
						// + "\n" + f.getName() + "?");
						// builder.setPositiveButton(R.string.yes, new
						// OnClickListener() {
						// @Override
						// public void onClick(DialogInterface arg0, int arg1) {
						// boolean result = FileHelper.deleteFile(f.getPath());
						// if (result) {
						// onResume();
						// } else {
						// }
						// }
						// });
						// builder.setNegativeButton(R.string.no, null);
						// builder.show();

						AlertDialogFactory.showAlertDialog(BackUpActivity.this, mListView, BackUpActivity.this.getString(R.string.delete_backup_title) + "\n"
								+ f.getName() + "?", new AlertDialogFactory.DialogPositiveListner(){

							@Override
							public void onClick(View arg0) {
								super.onClick(arg0);
								boolean result = FileHelper.deleteFile(f.getPath());
								if (result) {
									onResume();
								}
							}
						});
					}
				});
				ImageButton restore = (ImageButton) v.findViewById(R.id.restore_file);
				restore.setTag(Integer.valueOf(positon));
				restore.setOnClickListener(new android.view.View.OnClickListener() {
					@Override
					public void onClick(View v) {
						final BaseFile f = mData.get(((Integer) v.getTag()).intValue());
						// AlertDialog.Builder builder = new
						// AlertDialog.Builder(BackUpActivity.this);
						// builder.setMessage(BackUpActivity.this.getString(R.string.restore_backup_title,
						// f.getName()));
						// builder.setPositiveButton(R.string.yes, new
						// OnClickListener() {
						// @Override
						// public void onClick(DialogInterface arg0, int arg1) {
						// showExectingDialog();
						// new Thread() {
						// public void run() {
						// File db =
						// BackUpActivity.this.getDatabasePath(NoteDBField.DBNAME);
						// File backup = new File(f.getPath());
						// FileHelper.copyFile(backup, db);
						// // 关闭DB
						// NoteApplication.closeDB();
						// // 重新开启DB
						// NoteApplication.openDB();
						// noteDBManager.clearWidgetsRelations();
						// Config.recent_needRefresh = true;
						// Config.search_needRefresh = true;
						// Config.calendar_needRefresh = true;
						// if (Config.NEED_DB_SERVICE) {
						// BackUpActivity.this.startService(serviceIntent);
						// } else {
						// mHandler.post(new Runnable() {
						// @Override
						// public void run() {
						// dissmissExecutingDialog();
						// Toast.makeText(BackUpActivity.this,
						// BackUpActivity.this.getText(R.string.toast_success),
						// 1000).show();
						// }
						// });
						// }
						// };
						// }.start();
						// }
						// });
						// builder.setNegativeButton(R.string.no, null);
						// builder.show();

						AlertDialogFactory.showAlertDialog(BackUpActivity.this, mListView,
								BackUpActivity.this.getString(R.string.restore_backup_title, f.getName()),
								new AlertDialogFactory.DialogPositiveListner(){
									@Override
									public void onClick(View arg0) {
										super.onClick(arg0);
										showExectingDialog();
										new Thread() {
											public void run() {
												File db = BackUpActivity.this.getDatabasePath(NoteDBField.DBNAME);
												File backup = new File(f.getPath());
												FileHelper.copyFile(backup, db);
												// 关闭DB
												NoteApplication.closeDB();
												// 重新开启DB
												NoteApplication.openDB();
												noteDBManager.clearWidgetsRelations();
												Config.recent_needRefresh = true;
												Config.search_needRefresh = true;
												Config.calendar_needRefresh = true;
												if (Config.NEED_DB_SERVICE) {
													BackUpActivity.this.startService(serviceIntent);
												} else {
													mHandler.post(new Runnable() {
														@Override
														public void run() {
															dissmissExecutingDialog();
															Toast.makeText(BackUpActivity.this, BackUpActivity.this.getText(R.string.toast_success), 1000)
																	.show();
														}
													});
												}
											};
										}.start();
									}
								});
					}
				});
			} else if (mData.get(positon) instanceof Folder) {
				v = inflater.inflate(R.layout.backup_list_item_folder, null);
				TextView fileName = (TextView) v.findViewById(R.id.file_name);
				fileName.setTextSize(18);
				fileName.setTextColor(Color.DKGRAY);
				fileName.setText(mData.get(positon).getName());
				v.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						readData(mData.get(positon).getPath());
						mAdapter.notifyDataSetChanged();
					}
				});
			}
			return v;
		}
	}

	PopupWindow mDialog;

	private void showSaveDialog() {

		Calendar cal = Calendar.getInstance();
		Date date = cal.getTime();
		final String fileName = "mar_note_" + date.getTime() + FileHelper.fileSuffix;

		AlertDialogFactory.showAlertDialog(BackUpActivity.this, mListView,
				getString(R.string.add_new_record_title) + "\n" + fileName + "\n" + this.getString(R.string.add_new_record_title_2) + BACKUP_PATH + "?",
				new AlertDialogFactory.DialogPositiveListner(){
					@Override
					public void onClick(View arg0) {
						super.onClick(arg0);
						showExectingDialog();
						new Thread() {
							public void run() {
								addNewBackUp(fileName);
								mHandler.post(new Runnable() {
									@Override
									public void run() {
										// TODO Auto-generated method stub
										onResume();
										dissmissExecutingDialog();
									}
								});
							};
						}.start();
					}
				});

		//
		// AlertDialog.Builder builder = new AlertDialog.Builder(this);
		// builder.setMessage(this.getString(R.string.add_new_record_title) +
		// "\n" + fileName + "\n" +
		// this.getString(R.string.add_new_record_title_2)
		// + BACKUP_PATH + "?");
		// builder.setPositiveButton(R.string.yes, new OnClickListener() {
		// @Override
		// public void onClick(DialogInterface arg0, int arg1) {
		// showExectingDialog();
		// new Thread() {
		// public void run() {
		// addNewBackUp(fileName);
		// mHandler.post(new Runnable() {
		// @Override
		// public void run() {
		// // TODO Auto-generated method stub
		// onResume();
		// dissmissExecutingDialog();
		// }
		// });
		// };
		// }.start();
		// }
		// });
		// builder.setNegativeButton(R.string.no, null);
		// builder.show();
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
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(mBroadCastReceiver);
		super.onDestroy();
	}
}
