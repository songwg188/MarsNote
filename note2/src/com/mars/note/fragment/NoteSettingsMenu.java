package com.mars.note.fragment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;
import com.mars.note.BackUpAndRestore;
import com.mars.note.Config;
import com.mars.note.LoginActivity;
import com.mars.note.NoteApplication;
import com.mars.note.R;
import com.mars.note.ThemeSettings;
import com.mars.note.database.NoteDBField;
import com.mars.note.database.NoteDataBaseManager;
import com.mars.note.database.NoteRecord;
import com.mars.note.server.LoginServer;
import com.mars.note.views.BounceListView;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class NoteSettingsMenu extends Fragment {
	Activity mActivity;
	BounceListView mSettingsListView;
	BaseAdapter mAdapter;
	OnClickListener mOnClickListener;
	NoteDataBaseManager noteDBManager;
	List<NoteRecord> records;

	@Override
	public void onAttach(Activity mActivity) {
		super.onAttach(mActivity);
		this.mActivity = mActivity;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		noteDBManager = NoteApplication.getDbManager();
		final String[] settings_titles = mActivity.getResources()
				.getStringArray(R.array.settings_title);
		mOnClickListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (v.getTag() != null) {
					int position = ((Integer) v.getTag()).intValue();
					switch (position) {
					case 0:
						Intent intent0 = new Intent(mActivity,
								ThemeSettings.class);
						mActivity.startActivity(intent0);
						break;
					case 1:
						Intent intent1 = new Intent(mActivity,
								BackUpAndRestore.class);
						mActivity.startActivity(intent1);
						break;
					case 2:
						exportXMLDialog();
						break;
					case 3:
						importXMLChooser();
						break;
					case 4:
						Intent intent = new Intent(
								NoteSettingsMenu.this.getActivity(),
								LoginActivity.class);
						Bundle extras = new Bundle();
						extras.putBoolean("createOrAlterPass", true);
						intent.putExtras(extras);
						NoteSettingsMenu.this.startActivity(intent);
						break;
					case 5:
						NoteSettingsMenu.this.startActivity(new Intent(NoteSettingsMenu.this.getActivity(),LoginServer.class));						
						break;
					}
				}
			}
		};
		mAdapter = new BaseAdapter() {
			LayoutInflater mInflater = (LayoutInflater) mActivity
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			@Override
			public int getCount() {
				// TODO Auto-generated method stub
				return 7;
			}

			@Override
			public Object getItem(int arg0) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public long getItemId(int arg0) {
				// TODO Auto-generated method stub
				return arg0;
			}

			@SuppressLint("NewApi")
			@Override
			public View getView(int position, View v, ViewGroup viewGroup) {
				if (position <= 5) {
					if (v == null) {
						v = mInflater
								.inflate(R.layout.settings_list_item, null);
					}
					TextView txt = (TextView) v
							.findViewById(R.id.preference_title);
					txt.setText(settings_titles[position]);
					// txt.setTextColor(Color.DKGRAY);
					// txt.setGravity(Gravity.LEFT);
					txt.setPadding(10, 10, 10, 10);
					v.setTag(Integer.valueOf(position));
					v.setOnClickListener(mOnClickListener);
					return v;
				} else if (position == 6) {
					if (v == null) {
						v = mInflater
								.inflate(R.layout.settings_list_item, null);
						TextView txt = (TextView) v
								.findViewById(R.id.preference_title);
						txt.setVisibility(View.GONE);

						TextView txt2 = (TextView) v
								.findViewById(R.id.spinner_title);
						txt2.setVisibility(View.VISIBLE);

						Spinner spinner = (Spinner) v
								.findViewById(R.id.preference_spinner);
						spinner.setVisibility(View.VISIBLE);
						ArrayAdapter<String> adapter = new ArrayAdapter<String>(
								mActivity, R.layout.spinner_item, mActivity
										.getResources().getStringArray(
												R.array.effects));
						spinner.setAdapter(adapter);
						spinner.setPopupBackgroundResource(R.drawable.rounded_rectangle);
						spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

							@Override
							public void onItemSelected(AdapterView<?> arg0,
									View v, int position, long id) {
								SharedPreferences pref = getActivity()
										.getSharedPreferences("effect",
												Context.MODE_PRIVATE);
								Editor editor = pref.edit();
								editor.putInt("effect_id", position);
								editor.commit();
								Config.recent_needRefresh = true;
							}

							@Override
							public void onNothingSelected(AdapterView<?> arg0) {
								// TODO Auto-generated method stub

							}

						});
						SharedPreferences pref = getActivity()
								.getSharedPreferences("effect",
										Context.MODE_PRIVATE);
						int pos = pref.getInt("effect_id", 0);
						spinner.setSelection(pos, true);
					}
					return v;
				}
				return null;
			}
		};
	}

	protected void importXMLChooser() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		// intent.setType("*/xml");
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("text/plain");
		// File f = new File(BackUpAndRestore.BACKUP_PATH);
		// intent.setDataAndType(Uri.fromFile(f), "text/plain");
		try {
			this.startActivityForResult(
					Intent.createChooser(intent,
							mActivity.getString(R.string.choose_xml_title)), 0);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void exportXMLDialog() {
		final String fileName = System.currentTimeMillis() + "_mars_note_text"
				+ ".xml";
		AlertDialog.Builder mDialog = new AlertDialog.Builder(mActivity);
		mDialog.setMessage(mActivity.getString(R.string.export_xml_title,
				fileName) + BackUpAndRestore.BACKUP_PATH + " ?");
		mDialog.setPositiveButton(R.string.yes,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						getDataList();
						exportXML(fileName);
					}
				});
		mDialog.setNegativeButton(R.string.no, null);
		mDialog.show();
	}

	protected void getDataList() {
		records = noteDBManager.queryAllRecords();
	}

	protected void exportXML(String fileName) {
		XmlSerializer serializer = Xml.newSerializer();
		StringWriter writer = new StringWriter();
		try {
			File file = new File(BackUpAndRestore.BACKUP_PATH, fileName);
			file.createNewFile();
			FileOutputStream outputStream;
			outputStream = new FileOutputStream(file);
			serializer.setOutput(outputStream, "utf-8");
			serializer.startDocument("utf-8", null);
			// <persons>
			serializer.comment("this document is exported from com.mars.note");
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
			Toast.makeText(mActivity, R.string.toast_success, 2000).show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_settings, container, false);
		mSettingsListView = (BounceListView) v.findViewById(R.id.settings_list);
		View header = inflater.inflate(R.layout.note_list_item_header, null);
		mSettingsListView.addHeaderView(header);
		mSettingsListView.setAdapter(mAdapter);
		return v;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (Activity.RESULT_OK == resultCode && requestCode == 0) {
			Uri uri = data.getData();
			final String filePath = uri.getPath();
			if (filePath.endsWith("mars_note_text.xml")) {
				AlertDialog.Builder dialog = new AlertDialog.Builder(mActivity);
				dialog.setMessage(mActivity.getString(
						R.string.import_xml_title, filePath));
				dialog.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface arg0, int arg1) {
								try {
									FileInputStream fileIs = new FileInputStream(
											filePath);
									importXML(fileIs);
								} catch (FileNotFoundException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						});
				dialog.setNegativeButton(R.string.no, null);
				dialog.show();
			} else {
				Toast.makeText(mActivity, R.string.import_xml_error_title, 2000)
						.show();
			}
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
			com.mars.note.Config.recent_needRefresh = true;
			Toast.makeText(mActivity,
					mActivity.getString(R.string.toast_success), 2000).show();
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
