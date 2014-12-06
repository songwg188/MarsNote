package com.mars.note;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.text.Editable;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.mars.note.database.*;
import com.mars.note.utils.PictureHelper;
import com.mars.note.views.DataAlterDialog;
import com.mars.note.views.NoteContentEditText;

public class Editor extends Activity implements
		android.view.View.OnClickListener {
	private Calendar calendar;
	private long recordTime;
	private EditText titleText;
	private EditText contentText; //20141205图文混排去掉底部白线 NoteContentEditText
	private View imgContainer;
	private ImageView mImageView;
	private DisplayMetrics dm = null;
	private String note_id;
	private NoteDataBaseManager noteDBManager;
	private NoteRecord mNoteRecord;
	private boolean isPastNote = false;
	private static final int SELECT_PIC_KITKAT = 0;
	private static final int SELECT_PIC = 1;
	private static final int REQUEST_VALIDATE = 2;
	private static final int REQUEST_CAMERA = 3;
	private String imagePath, cameraIMGPath;
	private ActionBar actionBar;
	private ProgressBar mProgressBar;
	public static final int MAX_CONTENT = 5000;
	public static final int MAX_TITLE = 50;
	private TextView contenTitle;
	private boolean isAddFromCalendar = false;
	private View overflow_menu, cameraOrGallery_menu;
	private PopupWindow overflow_menu_pw, cameraOrGallery_menu_pw;
	private Button changeDate, deleteSingle, shareWithOthers;
	private Button camera, gallery;
	private int imgPadding = 10;
	private int appWidgetId = -1;
	private boolean isRelatedToWidget = false; // 表示intent来自widget点击
	private boolean isAddedWidgetRelation = false;
	private Intent mIntent;
	private InputMethodManager imm; // 20141127 bug
									// 点击按钮退出activity，如果输入法显示，退出后没有隐藏
	private float screenWidth;
	private EditorHelper mEditorHelper;
	private static final int imgNums = 10;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d("mars", "onCreate");
		getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY); // 悬浮Actionbar
		imm = (InputMethodManager) getApplicationContext().getSystemService(
				INPUT_METHOD_SERVICE);
		noteDBManager = NoteApplication.getDbManager();
		setContentView(R.layout.activity_editor);
		contenTitle = new TextView(this);
		titleText = (EditText) this.findViewById(R.id.titleText);
		contentText = (EditText) this.findViewById(R.id.contentText);
		imgContainer = this.findViewById(R.id.img_container);
		mImageView = (ImageView) this.findViewById(R.id.note_image);
		mProgressBar = (ProgressBar) this.findViewById(R.id.loadimg_progress);

		initPopupWindow();

		mIntent = getIntent();
		appWidgetId = mIntent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
				-1);

		if (appWidgetId != -1) { // 此时时从widget进入的
			validate();// 验证密码
			this.getActionBar().hide(); // 先隐藏等待验证，若不隐藏则此时actionbar是悬浮的，必须在requestFeature之后
		} else { // 此时是从程序内部组件进入的
			refresh();
		}

		if (dm == null) {
			dm = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(dm);
		}
		screenWidth = dm.widthPixels;
	}

	private void initPopupWindow() {
		overflow_menu = LayoutInflater.from(this).inflate(
				R.layout.popup_window_add_note_overflow, null);
		changeDate = (Button) overflow_menu.findViewById(R.id.change_date);
		deleteSingle = (Button) overflow_menu.findViewById(R.id.delete_single);
		shareWithOthers = (Button) overflow_menu
				.findViewById(R.id.share_with_others);
		changeDate.setOnClickListener(this);
		deleteSingle.setOnClickListener(this);
		shareWithOthers.setOnClickListener(this);
		overflow_menu_pw = new PopupWindow(overflow_menu,
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, false);
		overflow_menu_pw.setBackgroundDrawable(new BitmapDrawable());
		// if not setBackgroundDrawable ,outside is not touchable
		overflow_menu_pw.setOutsideTouchable(true);
		overflow_menu_pw.setFocusable(true);
		overflow_menu_pw.update();
		cameraOrGallery_menu = LayoutInflater.from(this).inflate(
				R.layout.popup_window_camera_or_gallery, null);
		camera = (Button) cameraOrGallery_menu.findViewById(R.id.camera);
		gallery = (Button) cameraOrGallery_menu.findViewById(R.id.gallery);
		camera.setOnClickListener(this);
		gallery.setOnClickListener(this);
		cameraOrGallery_menu_pw = new PopupWindow(cameraOrGallery_menu,
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, false);
		cameraOrGallery_menu_pw.setBackgroundDrawable(new BitmapDrawable());
		cameraOrGallery_menu_pw.setOutsideTouchable(true);
		cameraOrGallery_menu_pw.setFocusable(true);
		cameraOrGallery_menu_pw.update();
	}

	private void refresh() {
		note_id = mIntent.getStringExtra("note_id");
		if (null != note_id) {
			mNoteRecord = noteDBManager.querySingleRecord(note_id);
		}
		if (mNoteRecord != null) {
			Log.d("mars", "isPastNote ");
			isPastNote = true;
			String WidgetID = noteDBManager.queryWidgetIDByNoteID(String
					.valueOf(note_id));
			if (WidgetID != null) {
				appWidgetId = Integer.parseInt(WidgetID);
			}
		} else {
			Log.d("mars", "notPastNote ");
			isPastNote = false;
			Log.d("mars", "widgetId = " + appWidgetId);
			if (appWidgetId != -1) {
				isRelatedToWidget = true;
				note_id = noteDBManager.querySingleRecordIDByWidgetID(String
						.valueOf(appWidgetId));
				if (note_id != null) {
					isAddedWidgetRelation = true;
					mNoteRecord = noteDBManager.querySingleRecord(note_id);
				}
			}
		}
		isAddFromCalendar = mIntent.getBooleanExtra("add_from_calendar", false);

		/*
		 * 如果是null,则为第一次启动editor的情况 如果不是null,是editor在后台被调用的清况，因为editor的lauchmode
		 * 是singleTash 目前默认保留缓存 因为缓存可以自动调整,而且对于重复打开同一ID的记录这样的情况，图片可以不用再加载
		 */
		if (mEditorHelper == null) {
			mEditorHelper = EditorHelper.newInstance();
		} else {
			mEditorHelper.clearPaths(); // 清空图片地址集合
		}

		initActionBar();
		initContent();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Log.d("mars", "onNewIntent");
		mIntent = intent;
		reset();

		appWidgetId = mIntent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
				-1);
		if (appWidgetId != -1) { // 此时时从widget进入的
			validate();// 验证密码
			this.getActionBar().hide(); // 先隐藏等待验证，若不隐藏则此时actionbar是悬浮的，必须在requestFeature之后
		} else { // 此时是从程序内部组件进入的
			refresh();
		}
	}

	private void reset() {
		note_id = null;
		mNoteRecord = null;
		isPastNote = false;
		appWidgetId = -1;
		isAddedWidgetRelation = false;
		isAddFromCalendar = false;
		titleText.setText("");
		contentText.setText("");
		clearIMG();

	}

	private void validate() {
		Intent validate = new Intent(this, LoginActivity.class);
		Bundle bundle = new Bundle();
		bundle.putInt("mode", 3);
		validate.putExtras(bundle);
		this.startActivityForResult(validate, REQUEST_VALIDATE);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onStart() {
		Logg.D("Editor TaskID " + getTaskId());
		super.onStart();
	}

	private void initActionBar() {
		actionBar = this.getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		calendar = Calendar.getInstance();
		recordTime = calendar.getTimeInMillis();

		if (isPastNote || isRelatedToWidget) { // widget 和
												// pastNote的时间都存在recordTime中
			recordTime = Long.parseLong(mNoteRecord.time);
			calendar.setTimeInMillis(recordTime);
		} else if (isAddFromCalendar) {
			int year = this.getIntent().getIntExtra("year", 0);
			int month = this.getIntent().getIntExtra("month", 0);
			int day = this.getIntent().getIntExtra("day", 0);
			Date date = new Date(year - 1900, month - 1, day);
			recordTime = date.getTime();
			calendar.setTime(date);
		}
		String[] date_title = this.getResources().getStringArray(
				R.array.date_title);
		String[] time_title = this.getResources().getStringArray(
				R.array.time_title);
		String nowDate = (calendar.get(Calendar.MONTH) + 1) + date_title[1]
				+ calendar.get(Calendar.DAY_OF_MONTH) + date_title[2];
		;
		String nowTime = calendar.get(Calendar.HOUR_OF_DAY) + time_title[0]
				+ calendar.get(Calendar.MINUTE) + time_title[1];
		String dayOfWeekText = getdayOfWeek(calendar.get(Calendar.DAY_OF_WEEK));
		String subTitle = dayOfWeekText + " " + nowTime;
		actionBar.setTitle(nowDate);
		actionBar.setSubtitle(subTitle);
		actionBar.setDisplayShowHomeEnabled(false);
	}

	private void initContent() {

		titleText.setTextColor(Color.DKGRAY);

		contentText.setTextColor(Color.DKGRAY);

		mImageView.setOnClickListener(new android.view.View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				Uri uri = Uri.parse("file://" + imagePath);
				intent.setDataAndType(uri, "image/*");
				Editor.this.startActivity(intent);
			}
		});

		contenTitle.setTextSize(20);
		this.registerForContextMenu(mImageView);
		EditTextWatcher mtextWatcher = new EditTextWatcher(MAX_CONTENT,
				contentText);
		contentText.addTextChangedListener(mtextWatcher);
		if (isPastNote) {
			titleText.setText(mNoteRecord.title);
			contentText.setText(mNoteRecord.content);
			imagePath = mNoteRecord.imgpath;
			if (imagePath != null && (!imagePath.equals("null"))
					&& (!"".equals(imagePath))) {
				imgContainer.setVisibility(View.VISIBLE);
				setImageView();
			} else {
				imgContainer.setVisibility(View.GONE);
			}
		}
		if (isAddedWidgetRelation) {
			titleText.setText(mNoteRecord.title);
			contentText.setText(mNoteRecord.content);
			imagePath = mNoteRecord.imgpath;
			if (imagePath != null && (!imagePath.equals("null"))
					&& (!"".equals(imagePath))) {
				imgContainer.setVisibility(View.VISIBLE);
				setImageView();
			} else {
				imgContainer.setVisibility(View.GONE);
			}
		} else {
			// contentText.setText("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
		}
		contenTitle.setText(contentText.length() + "/" + Editor.MAX_CONTENT);
		if (contentText.length() == MAX_CONTENT) {
			contenTitle.setTextColor(Color.DKGRAY);
		} else {
			contenTitle.setTextColor(Color.BLACK);
		}

		if (!this.isPastNote) {
			deleteSingle.setVisibility(View.GONE);
		}
		if (isAddedWidgetRelation) {
			deleteSingle.setVisibility(View.VISIBLE);
		}

		//
	}

	private void setImageView(Bitmap bm) {
		if (bm != null) {
			mImageView.setVisibility(View.VISIBLE);
			mImageView.setImageBitmap(bm);
		} else {
			mImageView.setVisibility(View.GONE);
			imgContainer.setVisibility(View.GONE);
		}
	}

	private void setImageView() { // 将图片路径映射到Bitmap，再显示的过程
		if (imagePath != null && (!imagePath.equals("null"))
				&& (!"".equals(imagePath))) {
			// 这里用Single模式加载图片
			Bitmap bm = mEditorHelper.getImage(imagePath);
			if (bm == null) {
				Logg.D("bm == null");
				new AsyncTask<Object, Object, Object>() {

					Bitmap pic;

					@Override
					protected void onPreExecute() {
						Log.d("task", "onPreExecute");

					}

					@Override
					protected void onProgressUpdate(Object... values) {
					}

					@Override
					protected Object doInBackground(Object... params) {
						pic = PictureHelper.getImageFromPath(imagePath, 700,
								screenWidth - 100, true, 100, Editor.this,
								imgPadding, true);
						return null;
					}

					@Override
					protected void onPostExecute(Object result) {
						mProgressBar.setVisibility(View.GONE);
						setImageView(pic);
						insertIntoEditor(pic);
						
						// 201401205 图片与地址增加的地方
						mEditorHelper.addPath(imagePath);
						mEditorHelper.addImage(imagePath, pic);

						System.gc();
					}
				}.execute();
			}else{
				Logg.D("bm != null");
				setImageView(bm);
				insertIntoEditor(bm);
			}
			
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_add_note_activity, menu);
		MenuItem content_title = menu.findItem(R.id.content_title);
		contenTitle.setPadding(0, 0, 50, 0);
		content_title.setActionView(contenTitle);
		menu.findItem(R.id.add_picture).setVisible(true);

		// 自定义Menu的View
		MenuItem others = menu.findItem(R.id.over_flow_menus);
		View view = LayoutInflater.from(this).inflate(R.layout.editor_overflow,
				null);
		ImageView img = (ImageView) view.findViewById(R.id.over_flow_menu);
		img.setOnClickListener(this);

		others.setActionView(view);

		MenuItem add_pic = menu.findItem(R.id.add_picture);
		View view2 = LayoutInflater.from(this).inflate(
				R.layout.editor_overflow2, null);
		ImageView img2 = (ImageView) view2
				.findViewById(R.id.cameraOrGallery_menu);
		img2.setOnClickListener(this);

		add_pic.setActionView(view2);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// 退出前如果输入法显示则隐藏输入法
			imm.hideSoftInputFromWindow(titleText.getWindowToken(), 0);
			onBackPressed();// 20141127
			break;
		// case R.id.add_picture:
		// Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);//
		// ACTION_OPEN_DOCUMENT
		// intent.addCategory(Intent.CATEGORY_OPENABLE);
		// intent.setType("image/*");
		// if (android.os.Build.VERSION.SDK_INT >=
		// android.os.Build.VERSION_CODES.KITKAT) {
		// startActivityForResult(intent, SELECT_PIC_KITKAT);
		// } else {
		// startActivityForResult(intent, SELECT_PIC);
		// }
		// break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	public class EditTextWatcher implements TextWatcher {
		private int maxLen;
		private EditText editText = null;

		public EditTextWatcher(int maxLen, EditText editText) {
			this.maxLen = maxLen;
			this.editText = editText;
		}

		@Override
		public void afterTextChanged(Editable arg0) {
		}

		@Override
		public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
				int arg3) {
		}

		@Override
		public void onTextChanged(CharSequence arg0, int arg1, int arg2,
				int arg3) {
			Editable editable = editText.getText();
			int len = editable.length();
			contenTitle.setText(len + "/" + this.maxLen);
			if (len == maxLen) {
				contenTitle.setTextColor(Color.DKGRAY);
			} else {
				contenTitle.setTextColor(Color.BLACK);
			}
			if (len > maxLen) {
				Toast.makeText(Editor.this,
						getString(R.string.toast_reached_max_text), 3000)
						.show();
				int selEndIndex = Selection.getSelectionEnd(editable);
				String str = editable.toString();
				String newStr = str.substring(0, maxLen);
				editText.setText(newStr);
				editable = editText.getText();
				int newLen = editable.length();
				if (selEndIndex > newLen) {
					selEndIndex = editable.length();
				}
				Selection.setSelection(editable, selEndIndex);
			}
		}
	}

	private void canBeSavedInDB() {
		if (((titleText.getText().toString() != null) && (!"".equals(titleText
				.getText().toString())))
				|| ((contentText.getText().toString() != null) && (!""
						.equals(contentText.getText().toString())))) {
			saveToDB();
		} else {

			Editor.this.finish();
			overridePendingTransition(android.R.anim.fade_in,
					android.R.anim.fade_out);

		}
	}

	private void saveToDB() {
		long result = 0;
		if (isPastNote) {
			mNoteRecord.title = titleText.getText().toString();
			mNoteRecord.content = contentText.getText().toString();
			mNoteRecord.imgpath = this.imagePath;
			mNoteRecord.time = String.valueOf(recordTime);
			mNoteRecord.year = String.valueOf(calendar.get(Calendar.YEAR));
			mNoteRecord.month = String
					.valueOf(calendar.get(Calendar.MONTH) + 1);
			mNoteRecord.day = String.valueOf(calendar
					.get(Calendar.DAY_OF_MONTH));
			mNoteRecord.hour = String.valueOf(calendar
					.get(Calendar.HOUR_OF_DAY));
			mNoteRecord.minute = String.valueOf(calendar.get(Calendar.MINUTE));
			mNoteRecord.second = String.valueOf(calendar.get(Calendar.SECOND));
			noteDBManager.updateRecord(mNoteRecord);
			if (appWidgetId != -1) {
				Intent intent = new Intent("com.mars.note.widget.refresh");
				intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
						appWidgetId);
				this.sendBroadcast(intent);
			}
			finish();
			overridePendingTransition(android.R.anim.fade_in,
					android.R.anim.fade_out);
		} else if (isAddedWidgetRelation) {
			mNoteRecord.title = titleText.getText().toString();
			mNoteRecord.content = contentText.getText().toString();
			mNoteRecord.imgpath = this.imagePath;
			mNoteRecord.time = String.valueOf(recordTime);
			mNoteRecord.year = String.valueOf(calendar.get(Calendar.YEAR));
			mNoteRecord.month = String
					.valueOf(calendar.get(Calendar.MONTH) + 1);
			mNoteRecord.day = String.valueOf(calendar
					.get(Calendar.DAY_OF_MONTH));
			mNoteRecord.hour = String.valueOf(calendar
					.get(Calendar.HOUR_OF_DAY));
			mNoteRecord.minute = String.valueOf(calendar.get(Calendar.MINUTE));
			mNoteRecord.second = String.valueOf(calendar.get(Calendar.SECOND));
			noteDBManager.updateRecord(mNoteRecord);
			finish();
			overridePendingTransition(android.R.anim.fade_in,
					android.R.anim.fade_out);
			Intent intent = new Intent("com.mars.note.widget.refresh");
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
			this.sendBroadcast(intent);
		} else {
			int totdayRecordCount = noteDBManager.getDayRecordCount(
					calendar.get(Calendar.YEAR),
					calendar.get(Calendar.MONTH) + 1,
					calendar.get(Calendar.DAY_OF_MONTH));
			if (totdayRecordCount >= Config.maxCountEachDay) {
				Toast.makeText(this,
						this.getText(R.string.toast_cant_add_note), 1000)
						.show();
			} else {
				mNoteRecord = new NoteRecord();
				mNoteRecord.title = titleText.getText().toString();
				mNoteRecord.content = contentText.getText().toString();
				mNoteRecord.time = String.valueOf(recordTime);
				mNoteRecord.year = String.valueOf(calendar.get(Calendar.YEAR));
				mNoteRecord.month = String
						.valueOf(calendar.get(Calendar.MONTH) + 1);
				mNoteRecord.day = String.valueOf(calendar
						.get(Calendar.DAY_OF_MONTH));
				mNoteRecord.hour = String.valueOf(calendar
						.get(Calendar.HOUR_OF_DAY));
				mNoteRecord.minute = String.valueOf(calendar
						.get(Calendar.MINUTE));
				mNoteRecord.second = String.valueOf(calendar
						.get(Calendar.SECOND));
				mNoteRecord.imgpath = this.imagePath;
				result = noteDBManager.addRecord(mNoteRecord);
				if (isRelatedToWidget) {
					String noteId = noteDBManager
							.querySingleRecordIDByTime(String
									.valueOf(recordTime));
					noteDBManager.addWidgetRelation(
							String.valueOf(appWidgetId), noteId);
					Intent intent = new Intent("com.mars.note.widget.refresh");
					intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
							appWidgetId);
					this.sendBroadcast(intent);
				}
				finish();
				overridePendingTransition(android.R.anim.fade_in,
						android.R.anim.fade_out);
			}
		}
	}

	private void exit() {
		canBeSavedInDB();
		Config.recent_needRefresh = true;
		Config.calendar_needRefresh = true;
	}

	@Override
	public void onBackPressed() {
		exit();
	}

	private class EditTextOnFocusChangeListener implements
			View.OnFocusChangeListener {
		@Override
		public void onFocusChange(View v, boolean hasFocus) {
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo mi) {
		getMenuInflater().inflate(R.menu.context_menu_add_note_activity, menu);
		super.onCreateContextMenu(menu, v, mi);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.delete:
			clearIMG();
			break;
		default:
			break;
		}
		return super.onContextItemSelected(item);
	}

	private void clearIMG() {
		this.imagePath = null;
		this.mImageView.setImageBitmap(null);
		imgContainer.setVisibility(View.GONE);
		mImageView.setVisibility(View.GONE);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.over_flow_menu:
			if (overflow_menu_pw.isShowing()) {
				overflow_menu_pw.dismiss();
			} else {
				overflow_menu_pw.showAsDropDown(v);
			}
			break;
		case R.id.change_date:
			final DataAlterDialog mChangeDateDialog = new DataAlterDialog(this,
					null, calendar.get(Calendar.YEAR),
					calendar.get(Calendar.MONTH),
					calendar.get(Calendar.DAY_OF_MONTH));
			final DatePicker mDatePicker = mChangeDateDialog.getDatePicker();
			mChangeDateDialog.showAll();

			mChangeDateDialog.setBtnListener(
					new android.view.View.OnClickListener() {
						@Override
						public void onClick(View v) {
							if (mDatePicker.getYear() != calendar
									.get(Calendar.YEAR)
									|| mDatePicker.getMonth() != calendar
											.get(Calendar.MONTH)
									|| mDatePicker.getDayOfMonth() != calendar
											.get(Calendar.DAY_OF_MONTH)) {
								Date date = new Date(
										mDatePicker.getYear() - 1900,
										mDatePicker.getMonth(), mDatePicker
												.getDayOfMonth());
								recordTime = date.getTime();
								calendar.setTime(date);
								String[] date_title = Editor.this
										.getResources().getStringArray(
												R.array.date_title);
								String[] time_title = Editor.this
										.getResources().getStringArray(
												R.array.time_title);
								String nowDate = (calendar.get(Calendar.MONTH) + 1)
										+ date_title[1]
										+ calendar.get(Calendar.DAY_OF_MONTH)
										+ date_title[2];
								;
								String nowTime = calendar
										.get(Calendar.HOUR_OF_DAY)
										+ time_title[0]
										+ calendar.get(Calendar.MINUTE)
										+ time_title[1];
								String dayOfWeekText = getdayOfWeek(calendar
										.get(Calendar.DAY_OF_WEEK));
								String subTitle = dayOfWeekText + " " + nowTime;
								actionBar.setTitle(nowDate);
								actionBar.setSubtitle(subTitle);
							}
							mChangeDateDialog.dismiss();
							if (overflow_menu_pw.isShowing()) {
								overflow_menu_pw.dismiss();
							}
						}
					}).show();
			break;
		case R.id.delete_single:
			AlertDialog.Builder myDialog = new AlertDialog.Builder(this);
			myDialog.setMessage(R.string.context_menu_single_delete_title);
			myDialog.setPositiveButton(Editor.this.getString(R.string.yes),
					new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							noteDBManager.deleteRecord(mNoteRecord.id);
							Config.recent_needRefresh = true;
							Editor.this.finish();
							overridePendingTransition(android.R.anim.fade_in,
									android.R.anim.fade_out);
							if (overflow_menu_pw.isShowing()) {
								overflow_menu_pw.dismiss();
							}
						}
					});
			myDialog.setNegativeButton(Editor.this.getString(R.string.no), null);
			myDialog.show();
			break;
		case R.id.share_with_others:
			Intent share = new Intent(Intent.ACTION_SEND);
			Uri uri = null;
			if (imagePath != null) {
				share.setType("image/*");
				uri = Uri.parse("file://" + imagePath);
				share.putExtra(Intent.EXTRA_STREAM, uri);
			} else {
				share.setType("text/plain");
			}
			share.putExtra(Intent.EXTRA_TEXT, titleText.getText() + "\n"
					+ contentText.getText());
			share.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			this.startActivity(Intent.createChooser(share,
					this.getString(R.string.share_title)));
			if (overflow_menu_pw.isShowing()) {
				overflow_menu_pw.dismiss();
			}
			break;
		case R.id.cameraOrGallery_menu:
			if (cameraOrGallery_menu_pw.isShowing()) {
				cameraOrGallery_menu_pw.dismiss();
			} else {
				cameraOrGallery_menu_pw.showAsDropDown(v);
			}
			break;
		case R.id.camera:
			if (cameraOrGallery_menu_pw.isShowing()) {
				cameraOrGallery_menu_pw.dismiss();
			}
			Intent camera = new Intent();
			camera.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
			cameraIMGPath = BackUpAndRestore.BACKUP_PATH + "/bitmap_"
					+ System.currentTimeMillis() + ".bmp";
			camera.putExtra(MediaStore.EXTRA_OUTPUT,
					Uri.fromFile(new File(cameraIMGPath)));
			startActivityForResult(camera, REQUEST_CAMERA);
			break;

		case R.id.gallery:
			if (cameraOrGallery_menu_pw.isShowing()) {
				cameraOrGallery_menu_pw.dismiss();
			}
			if (mEditorHelper.getPathsSize() <= imgNums) {
				Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
				intent.addCategory(Intent.CATEGORY_OPENABLE);
				intent.setType("image/*");
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
					startActivityForResult(intent, SELECT_PIC_KITKAT);
				} else {
					startActivityForResult(intent, SELECT_PIC);
				}
			} else {
				Toast.makeText(this, R.string.editor_max_imgs, 2000).show();
			}
			break;
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case SELECT_PIC_KITKAT:
			// 接受图库返回结果
			if (RESULT_OK == resultCode) {
				Uri selectedImage = data.getData();
				imgContainer.setVisibility(View.VISIBLE);
				imagePath = PictureHelper.getPath(this, selectedImage);
				setImageView();
			}
			break;
		case REQUEST_VALIDATE:
			// 接受返回的验证信息
			if (resultCode == RESULT_CANCELED) {
				this.finish(); // 20141202 验证失败则退出
			} else if (resultCode == RESULT_OK) {
				this.getActionBar().show(); // 验证成功后显示actionbar
				Log.d("mars", "refresh");
				refresh();
			}
			break;
		case REQUEST_CAMERA:
			// 20141204接受相机返回结果
			if (resultCode == RESULT_OK) {
				imagePath = cameraIMGPath;
				cameraIMGPath = null;
				imgContainer.setVisibility(View.VISIBLE);
				setImageView();
			}
			break;
		default:
			break;
		}
	}

	private String getdayOfWeek(int dayOfWeek) {
		switch (dayOfWeek) {
		case 1:
			return getString(R.string.sunday);
		case 2:
			return getString(R.string.monday);
		case 3:
			return getString(R.string.tuesday);
		case 4:
			return getString(R.string.wednesday);
		case 5:
			return getString(R.string.thursday);
		case 6:
			return getString(R.string.friday);
		case 7:
			return getString(R.string.saturday);
		default:
			return null;
		}
	}
	
	private void insertIntoEditor(Bitmap bm) {
		SpannableString ss = new SpannableString(imagePath);
		ImageSpan span = new ImageSpan(this, bm);
		ss.setSpan(span, 0, imagePath.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		
		Editable editable = contentText.getEditableText();
		int start = contentText.getSelectionStart();
		editable.insert(start, ss);// 设置ss要添加的位置
		contentText.setText(editable);// 把et添加到Edittext中
		contentText.setSelection(start + ss.length());// 设置Edittext中光标在最后面显示
	}
}
