package com.mars.note.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import com.mars.note.R;
import com.mars.note.R.id;
import com.mars.note.R.layout;
import com.mars.note.R.menu;
import com.mars.note.api.BackupDoc;
import com.mars.note.api.BaseActivity;
import com.mars.note.api.BaseFile;
import com.mars.note.api.Config;
import com.mars.note.api.Folder;
import com.mars.note.api.AlertDialogFactory;
import com.mars.note.api.XMLDoc;
import com.mars.note.database.NoteDBField;
import com.mars.note.database.NoteDataBaseManager;
import com.mars.note.database.NoteRecord;
import com.mars.note.utils.FileHelper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class TextImpExpActivity extends BaseActivity {
	private List<NoteRecord> records;
	private NoteDataBaseManager noteDBManager;
	private List<BaseFile> mData;
	private ListView mListView;
	private BaseAdapter mAdapter;
	private LinearLayout listDescription;
	public static final String BACKUP_PATH = Environment.getExternalStorageDirectory().getPath() + "/mars/";
	private String[] date_title;
	private String[] time_title;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_text_imp_exp);
		noteDBManager = NoteApplication.getDbManager();
		readData(BACKUP_PATH);
		date_title = this.getResources().getStringArray(R.array.date_title);
		time_title = this.getResources().getStringArray(R.array.time_title);
		mAdapter = new XMLListAdapter();
		listDescription = new LinearLayout(this);
		listDescription.setPadding(20, 20, 20, 0);
		mListView = (ListView) this.findViewById(R.id.backup_listview);
		// mListView.addHeaderView(listDescription);
		mListView.setAdapter(mAdapter);
	}

	@Override
	public void onResume() {
		super.onResume();
		readData(BACKUP_PATH);
		mAdapter.notifyDataSetChanged();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			this.onBackPressed();
			break;
		case R.id.export_xml:
			exportXMLDialog();
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.export_xml, menu);
		return true;
	}

	private void readData(String path) {
		try {
			mData = FileHelper.getXmlDocs(path);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void exportXMLDialog() {
		final String fileName = System.currentTimeMillis() + FileHelper.XMLSuffix;
		// AlertDialog.Builder mDialog = new AlertDialog.Builder(this);
		// mDialog.setMessage(getString(R.string.export_xml_title, fileName) +
		// TextImpExpActivity.BACKUP_PATH + " ?");
		// mDialog.setPositiveButton(R.string.yes, new
		// DialogInterface.OnClickListener() {
		// @Override
		// public void onClick(DialogInterface arg0, int arg1) {
		// getDataList();
		// exportXML(fileName);
		// onResume();
		// }
		// });
		// mDialog.setNegativeButton(R.string.no, null);
		// mDialog.create().show();

		AlertDialogFactory.showAlertDialog(this, mListView, getString(R.string.export_xml_title, fileName) + TextImpExpActivity.BACKUP_PATH + " ?",
				new AlertDialogFactory.DialogPositiveListner() {

					@Override
					public void onClick(View arg0) {
						super.onClick(arg0);
						getDataList();
						exportXML(fileName);
						onResume();
					}
				});
	}

	private void getDataList() {
		records = noteDBManager.queryAllRecords();
	}

	private void exportXML(String fileName) {
		XmlSerializer serializer = Xml.newSerializer();
		try {
			File file = new File(TextImpExpActivity.BACKUP_PATH, fileName);
			file.createNewFile();
			FileOutputStream outputStream;
			outputStream = new FileOutputStream(file);
			serializer.setOutput(outputStream, "utf-8");
			serializer.startDocument("utf-8", null);
			// <persons>
			serializer.comment("The document is exported from com.mars.note");
			serializer.startTag("", "records");//
			Iterator<NoteRecord> it = records.iterator();
			while (it.hasNext()) {
				NoteRecord nr = it.next();
				serializer.startTag("", "record");
				serializer.attribute("", "id", nr.id);
				serializer.startTag("", NoteDBField.TITLE);
				serializer.text(nr.title);
				serializer.endTag("", NoteDBField.TITLE);
				serializer.startTag("", NoteDBField.CONTENT);
				serializer.text(nr.content);
				serializer.endTag("", NoteDBField.CONTENT);
				serializer.startTag("", "year");
				serializer.text(nr.year);
				serializer.endTag("", "year");
				serializer.startTag("", "month");
				serializer.text(nr.month);
				serializer.endTag("", "month");
				serializer.startTag("", "day");
				serializer.text(nr.day);
				serializer.endTag("", "day");
				serializer.startTag("", "hour");
				serializer.text(nr.hour);
				serializer.endTag("", "hour");
				serializer.startTag("", "minute");
				serializer.text(nr.minute);
				serializer.endTag("", "minute");
				serializer.startTag("", "second");
				serializer.text(nr.second);
				serializer.endTag("", "second");
				serializer.startTag("", "time_ms");
				serializer.text(nr.time);
				serializer.endTag("", "time_ms");
				serializer.endTag("", "record");
			}
			serializer.endTag("", "records");
			serializer.endDocument();
			outputStream.close();
			Toast.makeText(this, R.string.toast_success, 2000).show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private class XMLListAdapter extends BaseAdapter {
		LayoutInflater inflater;

		public XMLListAdapter() {
			inflater = (LayoutInflater) TextImpExpActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
			if (mData.get(positon) instanceof XMLDoc) {
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
					dateText = TextImpExpActivity.this.getString(R.string.backup_date_title) + (cal.get(Calendar.YEAR)) + date_title[0]
							+ (cal.get(Calendar.MONTH) + 1) + date_title[1] + cal.get(Calendar.DAY_OF_MONTH) + date_title[2];
					timeText = TextImpExpActivity.this.getString(R.string.backup_time_title) + (cal.get(Calendar.HOUR_OF_DAY)) + time_title[0]
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
						// AlertDialog.Builder(TextImpExpActivity.this);
						// builder.setMessage(TextImpExpActivity.this.getString(R.string.delete_xml_title)
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

						AlertDialogFactory.showAlertDialog(TextImpExpActivity.this, mListView, TextImpExpActivity.this.getString(R.string.delete_xml_title)
								+ "\n" + f.getName() + "?", new AlertDialogFactory.DialogPositiveListner() {

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
						// AlertDialog.Builder(TextImpExpActivity.this,
						// R.style.MyDialog);
						// builder.setMessage(TextImpExpActivity.this.getString(R.string.import_xml_title,
						// f.getName()));
						// builder.setPositiveButton(R.string.yes, new
						// OnClickListener() {
						// @Override
						// public void onClick(DialogInterface arg0, int arg1) {
						// try {
						// FileInputStream fileIs = new
						// FileInputStream(f.getPath());
						// importXML(fileIs);
						// } catch (FileNotFoundException e) {
						// e.printStackTrace();
						// }
						// }
						// });
						// builder.setNegativeButton(R.string.no, null);
						// builder.show();

						AlertDialogFactory.showAlertDialog(TextImpExpActivity.this, mListView,
								TextImpExpActivity.this.getString(R.string.import_xml_title, f.getName()), new AlertDialogFactory.DialogPositiveListner() {

									@Override
									public void onClick(View arg0) {
										super.onClick(arg0);
										try {
											FileInputStream fileIs = new FileInputStream(f.getPath());
											importXML(fileIs);
										} catch (FileNotFoundException e) {
											e.printStackTrace();
										}
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

	private void importXML(InputStream is) {
		List<NoteRecord> addList = new ArrayList<NoteRecord>();
		XmlPullParser pullParser = Xml.newPullParser();
		try {
			pullParser.setInput(is, "utf-8");
			int event = pullParser.getEventType();
			NoteRecord nr = null;
			while (event != XmlPullParser.END_DOCUMENT) {
				switch (event) {
				case XmlPullParser.START_TAG:
					if (pullParser.getName().equals("records")) {
						// Log.d("test", pullParser.getName() + " start");
					} else if (pullParser.getName().equals("record")) {
						// Log.d("test", pullParser.getName() + " start");
						// Log.d("test","record id = " +
						// pullParser.getAttributeValue(0));
						nr = new NoteRecord();
					} else if (pullParser.getName().equals("title")) {
						String title = pullParser.nextText();
						// Log.d("test", "title = " +title);
						nr.title = title;
					} else if (pullParser.getName().equals("content")) {
						String content = pullParser.nextText();
						// Log.d("test", "content = " +content);
						nr.content = content;
					} else if (pullParser.getName().equals("year")) {
						String year = pullParser.nextText();
						// Log.d("test", "year = " +year);
						nr.year = year;
					} else if (pullParser.getName().equals("month")) {
						String month = pullParser.nextText();
						// Log.d("test", "month = " +month);
						nr.month = month;
					} else if (pullParser.getName().equals("day")) {
						String day = pullParser.nextText();
						// Log.d("test", "day = " +day);
						nr.day = day;
					} else if (pullParser.getName().equals("hour")) {
						String hour = pullParser.nextText();
						// Log.d("test", "hour = " +hour);
						nr.hour = hour;
					} else if (pullParser.getName().equals("minute")) {
						String minute = pullParser.nextText();
						// Log.d("test", "minute = " +minute);
						nr.minute = minute;
					} else if (pullParser.getName().equals("second")) {
						String second = pullParser.nextText();
						// Log.d("test", "second = " +second);
						nr.second = second;
					} else if (pullParser.getName().equals("time_ms")) {
						String time_ms = pullParser.nextText();
						// Log.d("test", "time_ms = " +time_ms);
						nr.time = time_ms;
					}
					break;
				case XmlPullParser.END_TAG:
					if (pullParser.getName().equals("record")) {
						addList.add(nr);
					}
					break;
				}
				event = pullParser.next();
			}
			if (addList.size() != 0) {
				noteDBManager.addRecords(addList);
			}
			com.mars.note.api.Config.recent_needRefresh = true;

			Toast.makeText(this, this.getString(R.string.toast_success), 2000).show();
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
