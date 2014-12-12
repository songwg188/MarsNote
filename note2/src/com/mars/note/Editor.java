package com.mars.note;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.util.DisplayMetrics;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.mars.note.api.BaseActivity;
import com.mars.note.api.Config;
import com.mars.note.api.EditorHelper;
import com.mars.note.api.ImageSpanInfo;
import com.mars.note.api.Logg;
import com.mars.note.database.*;
import com.mars.note.utils.PictureHelper;
import com.mars.note.utils.Util;
import com.mars.note.views.DataAlterDialog;

public class Editor extends BaseActivity implements
		android.view.View.OnClickListener {
	private Calendar calendar;
	private long recordTime;
	private EditText titleText;
	private EditText contentText; // 20141205图文混排去掉底部白线 NoteContentEditText
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

	public static final int MAX_CONTENT = 5000;
	public static final int MAX_TITLE = 50;
	private TextView contenTitle;
	private boolean isAddFromCalendar = false;
	private View overflow_menu, cameraOrGallery_menu;
	private PopupWindow overflow_menu_pw, cameraOrGallery_menu_pw;
	private Button changeDate, deleteSingle, shareWithOthers;
	private Button camera, gallery;
	private int imgPadding;
	private int appWidgetId = -1;
	private boolean isRelatedToWidget = false; // 表示intent来自widget点击
	private boolean isAddedWidgetRelation = false;
	private Intent mIntent;
	private InputMethodManager imm; // 20141127 bug
									// 点击按钮退出activity，如果输入法显示，退出后没有隐藏
	private int screenWidth, screenHeight;
	private EditorHelper mEditorHelper;
	private static final int imgNums = 10;
	public static final int thread_num = 10;

	private ExecutorService executors;
	Handler mhandler;
	private ConcurrentHashMap<String, ImageSpanLoaderStatus> mConcurentMap;
	private ReentrantLock mLock;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		imm = (InputMethodManager) getApplicationContext().getSystemService(
				INPUT_METHOD_SERVICE);
		noteDBManager = NoteApplication.getDbManager();
		executors = NoteApplication.getExecutors();
		if (executors == null) {
			executors = Executors.newFixedThreadPool(Editor.thread_num);
			NoteApplication.setExecutors(executors);
		}
		mConcurentMap = new ConcurrentHashMap<String, ImageSpanLoaderStatus>();// 保存线程工作状态
		mLock = new ReentrantLock(true);// 20141212 公平锁
		mhandler = new Handler();

		setContentView(R.layout.activity_editor);
		contenTitle = new TextView(this);
		titleText = (EditText) this.findViewById(R.id.titleText);
		contentText = (EditText) this.findViewById(R.id.contentText);

		initPopupWindow();

		mIntent = getIntent();
		appWidgetId = mIntent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
				-1);

		if (dm == null) {
			dm = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(dm);
		}
		screenWidth = dm.widthPixels;
		screenHeight = dm.heightPixels;
		imgPadding = Util.dpToPx(getResources(), 3);

		if (appWidgetId != -1) { // 此时时从widget进入的
			validate();// 验证密码
			this.getActionBar().hide(); // 先隐藏等待验证，若不隐藏则此时actionbar是悬浮的，必须在requestFeature之后
		} else { // 此时是从程序内部组件进入的
			refresh();
		}

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
		// 如果不设置,popwindow外部不能touch
		overflow_menu_pw.setBackgroundDrawable(new BitmapDrawable());

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
			Logg.D("isPastNote ");
			isPastNote = true;
			String WidgetID = noteDBManager.queryWidgetIDByNoteID(String
					.valueOf(note_id));
			if (WidgetID != null) {
				appWidgetId = Integer.parseInt(WidgetID);
			}
		} else {
			Logg.D("notPastNote ");
			isPastNote = false;
			Logg.D("widgetId = " + appWidgetId);
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
		if (mNoteRecord != null) {
			Logg.D("content : " + mNoteRecord.content);
		}

		isAddFromCalendar = mIntent.getBooleanExtra("add_from_calendar", false);

		/*
		 * 如果是null,则为第一次启动editor的情况 如果不是null,是editor在后台被调用的清况，因为editor的lauchmode
		 * 是singleTash 目前默认保留缓存 因为缓存可以自动调整,而且对于重复打开同一ID的记录这样的情况，图片可以不用再加载
		 */
		if (mEditorHelper == null) {
			mEditorHelper = EditorHelper.newInstance();
		} else {
			mEditorHelper.clearImageCache();
		}

		initActionBar();
		initContent();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Logg.D("onNewIntent");
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
		// clearIMG();

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
	}

	private void initContent() {
		titleText.setTextColor(Color.DKGRAY);
		contentText.setTextColor(Color.DKGRAY);

		contenTitle.setTextSize(20);
		contentText.addTextChangedListener(new EditTextWatcher(MAX_CONTENT,
				contentText));

		if (isPastNote || isAddedWidgetRelation) {
			titleText.setText(mNoteRecord.title);
			// 如何解析图片？
			// 20141211 从数据库获取bytes[]

			contentText.setText(mNoteRecord.content);

			byte[] data = mNoteRecord.imageSpanInfos;
			if (data != null) {
				// bytes[]转换对象
				Logg.D("bytes length = " + data.length);
				ArrayList<ImageSpanInfo> imageSpanInfoList = getImageSpanInfoListFromBytes(data);
				Logg.D("imageSpanInfoList size = " + imageSpanInfoList.size());
				// 替换所有ImageSpan
				replaceAllImgs(imageSpanInfoList);
			} else {
				// 恢复软件盘
				// contentText.setInputType(InputType.TYPE_CLASS_TEXT);
			}

			imagePath = mNoteRecord.imgpath;
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
		public void afterTextChanged(Editable editable) {
		}

		@Override
		public void beforeTextChanged(CharSequence chars, int start, int count,
				int after) {
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
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

	/**
	 * 用EditText已有的ImageSpan替换文本
	 * 
	 * @param span
	 * @param start
	 *            起始位置
	 * @param end
	 *            结束位置
	 * @param path
	 *            路径 like <img ... img>
	 */
	private void relaceImageSpans(ImageSpan span, int start, int end,
			String path) {
		SpannableString ss = new SpannableString(path);
		ss.setSpan(span, 0, path.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		Editable et = contentText.getText();
		et.delete(start, end);
		et.insert(start, ss);
		contentText.setSelection(end);
	}

	/**
	 * 用Bitmap构造新的ImageSpan
	 * 
	 * @param bm
	 *            imagespan的图片
	 * @param start
	 *            开始位置
	 * @param end
	 *            结束位置
	 * @param path
	 *            路径 like <img ... img>
	 */
	private void relaceImageSpans(Drawable drawable, int start, int end,
			String path) {
		SpannableString ss = new SpannableString(path);// path is like <img ...
														// // img>
		if (drawable == null) {
			throw new NullPointerException("drawable cant be null");
		}
		drawable.setBounds(0, 0, (int) (screenWidth * 0.7), drawable.getIntrinsicHeight());
		ImageSpan span = new ImageSpan(drawable, ImageSpan.ALIGN_BASELINE);
		ss.setSpan(span, 0, path.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		Editable et = contentText.getText();
		et.delete(start, end);
		et.insert(start, ss);
		contentText.setSelection(end);
	}

	/**
	 * 将字符串转换为ImageSpan，从数据库中得到ImageSpanInfo
	 */
	private void replaceAllImgs(final ArrayList<ImageSpanInfo> spanInfo) {
		// 从数据库读取List<ImageSpanInfo> spanInfo
		if (spanInfo == null) {
			return;
		}
		for (final ImageSpanInfo isi : spanInfo) {
			// 20141211 注意！缓存中的path是绝对地址,isi的path有<img img>在外部
			final String imgPath = isi.path.substring(5, isi.path.length() - 5);
			// Logg.D("ImageSpanInfo imgPath " + imgPath);

			// 初始化mConcurentMap中的键值对
			if (mConcurentMap.get(imgPath) == null)
				mConcurentMap.put(imgPath, new ImageSpanLoaderStatus());

			/**
			 * 20141212 Description 如果是同一imgPath,则都用同一个Bitmap资源
			 * 当进入Editor,第一个加载imgPath,负责读取图片到内存，然后将所有相同imgPath的地发
			 * 替换为同一Bitmap的ImageSpan 之后所有线程只将图片替换为预置图片，然后结束
			 * 当图片的工作完成，所有对这一资源的线程直接返回，无须工作，因为第一个线程已经完成了所有工作
			 */
			// 此处多线程需要同步
			Thread thread = new Thread() {
				@Override
				public void run() {
					mLock.lock();// 同时只有一个线程能进入临界区
					ImageSpanLoaderStatus isls = mConcurentMap.get(imgPath);
					if (isls.isFinished) {
						Logg.D("ImageSpanLoader has finished work");
						return;
					}
					Logg.D("ImageSpanLoader hasn't finished work");
					if (!isls.isWorking) {
						// 不可能有两个相同imgPath的线程走到这个逻辑
						isls.isWorking = true;
						mLock.unlock();

						mhandler.post(new Runnable() {
							@Override
							public void run() {
								// 使用预置图片
								relaceImageSpans(
										(getResources()
												.getDrawable(
														R.drawable.editor_preload_img)),
										isi.start, isi.end, isi.path);
								Logg.D("relaceImageSpans by temp");
							}
						});

						final Bitmap pic = PictureHelper.getImageFromPath(
								imgPath, screenWidth * 0.7F,
								screenWidth * 0.7F, false, 100, Editor.this,
								imgPadding, false);

						mLock.lock();
						isls.isWorking = false;// 工作完成同步记号
						// 替换所有SpannableString
						mhandler.post(new Runnable() {
							@Override
							public void run() {
								// 201401212 Bitmap构造新的ImageSpan,替换文字
								replaceAllImageSpans(isi.path, spanInfo, pic);
								Logg.D("relaceImageSpans all by loading new");
							}

						});
						isls.isFinished = true;
						mLock.unlock();
					} else {

						mhandler.post(new Runnable() {
							@Override
							public void run() {
								// 使用预置图片
								relaceImageSpans(
										( getResources()
												.getDrawable(
														R.drawable.editor_preload_img)), isi.start,
										isi.end, isi.path);
								Logg.D("relaceImageSpans by temp");
							}
						});
						mLock.unlock();

					}
				}
			};
			executors.execute(thread);
		}
	}

	private void replaceAllImageSpans(String path,
			ArrayList<ImageSpanInfo> list, Bitmap bm) {
		if (bm == null) {
			throw new NullPointerException("bm cant be null");
		}

		for (ImageSpanInfo isi : list) {
			if (path.equals(isi.path)) {
				// path is like <img ... img>
				SpannableString ss = new SpannableString(path);
				ImageSpan span = new ImageSpan(this, bm,
						ImageSpan.ALIGN_BASELINE);
				ss.setSpan(span, 0, path.length(),
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				Editable et = contentText.getText();
				et.delete(isi.start, isi.end);
				et.insert(isi.start, ss);
				contentText.setSelection(isi.end);
			}
		}
	}

	/**
	 * 判断是否能保存到数据库 如果文本不为空则保存否则直接退出
	 */
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

	/**
	 * 注意与数据库获取区分，是从当前文本获取ImageSpan的路径，首位置、末尾位置，供解析
	 * 
	 * @return 返回ImageSpanInfo的集合
	 */
	private ArrayList<ImageSpanInfo> getSpansInfo() {
		Editable edit = contentText.getText();
		ImageSpan[] imageSpans = edit.getSpans(0, contentText.getText()
				.length(), ImageSpan.class);

		if (imageSpans.length == 0) {
			Logg.D("no span");
			return null;
		}
		Logg.D("span count " + imageSpans.length);
		// List保存Span信息
		ArrayList<ImageSpanInfo> list = new ArrayList<ImageSpanInfo>();
		for (ImageSpan ip : imageSpans) {
			int start = edit.getSpanStart(ip);
			int end = edit.getSpanEnd(ip);
			String path = edit.toString().substring(start, end);
			if (start < 0 || end < 0 || path == null) {
				throw new IllegalStateException(
						"start < 0 || end < 0 || path == null");
			}

			ImageSpanInfo info = new ImageSpanInfo();
			info.start = start;
			info.end = end;
			info.path = path;
			list.add(info);
		}
		Logg.D("list size " + list.size());
		return list;
	}

	/**
	 * 从二进制数组转换Arrayist对象
	 * 
	 * @param bytes
	 *            二进制数组
	 * @return ArrayList返回对象
	 */
	private ArrayList<ImageSpanInfo> getImageSpanInfoListFromBytes(byte[] bytes) {
		ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(bytes);
		try {
			ObjectInputStream inputStream = new ObjectInputStream(
					arrayInputStream);
			ArrayList<ImageSpanInfo> list = (ArrayList<ImageSpanInfo>) inputStream
					.readObject();
			Logg.D("imagespan cout from db " + list.size());
			inputStream.close();
			arrayInputStream.close();
			return list;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 将ArrayList转化为二进制数组
	 * 
	 * @param list
	 *            ArrayList对象
	 * @return 二进制数组
	 */
	private byte[] getImageSpanInfoBytesFromObject(ArrayList<ImageSpanInfo> list) {
		Logg.D("imagespan cout to db " + list.size());
		ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
		try {
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(
					arrayOutputStream);
			objectOutputStream.writeObject(list);
			objectOutputStream.flush();
			byte[] data = arrayOutputStream.toByteArray();
			objectOutputStream.close();
			arrayOutputStream.close();
			return data;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 向NoteRecord 增加ImageSpan的信息 ImageSpanInfo 包含图片地址 开始位置 结束位置
	 * 
	 * @param nr
	 *            记录
	 */
	private void addImageSpanInfosToRecord(NoteRecord nr) {
		// 20141211 保存ImageSpanInfos 到数据库
		// 注意ArrayList已经实现了Serializable,可以直接序列化
		ArrayList<ImageSpanInfo> spanInfo = getSpansInfo();
		Logg.D("ArrayList before revert by bytes");
		if (spanInfo != null) {
			// for (ImageSpanInfo isi : spanInfo) {
			// Logg.D("path " + isi.path + " ; start " + isi.start + " ; end "
			// + isi.end);
			// }
			// 将ArrayList Object 转换为二进制数组
			byte[] bytes = getImageSpanInfoBytesFromObject(spanInfo);
			if (bytes == null) {
				throw new NullPointerException("bytes is null!");
			}
			// Logg.D("bytes length = "+bytes.length);
			nr.imageSpanInfos = bytes;
		}
	}

	/**
	 * 将数据保存到数据库
	 */
	private void saveToDB() {

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
			addImageSpanInfosToRecord(mNoteRecord);
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
			addImageSpanInfosToRecord(mNoteRecord);
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
				addImageSpanInfosToRecord(mNoteRecord);
				noteDBManager.addRecord(mNoteRecord);
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

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo mi) {
		getMenuInflater().inflate(R.menu.context_menu_add_note_activity, menu);
		super.onCreateContextMenu(menu, v, mi);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.delete:
			// clearIMG();
			break;
		default:
			break;
		}
		return super.onContextItemSelected(item);
	}

	@Deprecated
	private void clearIMG() {
		this.imagePath = null;
		// this.mImageView.setImageBitmap(null);
		// imgContainer.setVisibility(View.GONE);
		// mImageView.setVisibility(View.GONE);
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
			if (!hasReachedMax()) { // 控制图片数量
				Intent camera = new Intent();
				camera.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
				cameraIMGPath = BackUpActivity.BACKUP_PATH + "/bitmap_"
						+ System.currentTimeMillis() + ".bmp";
				camera.putExtra(MediaStore.EXTRA_OUTPUT,
						Uri.fromFile(new File(cameraIMGPath)));
				startActivityForResult(camera, REQUEST_CAMERA);
			} else {
				Toast.makeText(this, R.string.editor_max_imgs, 2000).show();
			}
			break;

		case R.id.gallery:
			if (cameraOrGallery_menu_pw.isShowing()) {
				cameraOrGallery_menu_pw.dismiss();
			}
			if (!hasReachedMax()) { // 控制图片数量
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

	/**
	 * 
	 * @return true表示达到最大，false表示尚未达到
	 */
	private boolean hasReachedMax() {
		Editable edit = contentText.getText();
		ImageSpan[] imageSpan = edit
				.getSpans(0, edit.length(), ImageSpan.class);
		// Logg.D("imagespan count " + imageSpan.length);
		// mSet取不相同的路径
		HashSet<String> mSet = new HashSet<String>();
		for (ImageSpan ip : imageSpan) {
			int start = edit.getSpanStart(ip);
			int end = edit.getSpanEnd(ip);
			String path = edit.toString().substring(start, end);
			mSet.add(path);
			Logg.D(path);
		}
		Logg.D("mSet size " + mSet.size());
		if (mSet.size() < 10) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case SELECT_PIC_KITKAT:
			// 接受图库返回结果
			if (RESULT_OK == resultCode) {
				Uri selectedImage = data.getData();
				// imgContainer.setVisibility(View.VISIBLE);
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
				Logg.D("refresh");
				refresh();
			}
			break;
		case REQUEST_CAMERA:
			// 20141204接受相机返回结果
			if (resultCode == RESULT_OK) {
				imagePath = cameraIMGPath;
				cameraIMGPath = null;
				// imgContainer.setVisibility(View.VISIBLE);
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

	/**
	 * 从当前的EditText获取ImageSpan,如果存在则返回否则返回Null
	 * 
	 * @return
	 */
	private ImageSpan getImageSpanFromExistence(String source) {
		Editable edit = contentText.getText();
		ImageSpan[] spans = edit.getSpans(0, edit.length(), ImageSpan.class);
		for (ImageSpan ip : spans) {
			int start = edit.getSpanStart(ip);
			int end = edit.getSpanEnd(ip);
			String path = edit.toString().substring(start, end);
			path = path.substring(5, path.length() - 5);

			if (source.equals(path)) {
				Logg.D("find existed ImageSpan");
				return new ImageSpan(ip.getDrawable(), ImageSpan.ALIGN_BASELINE);
			}
		}
		// mSet取不相同的路径
		return null;
	}

	/**
	 * 将图片路径映射到Bitmap，再通过SpannableString 和 ImageSpan显示到EditText
	 */
	private void setImageView() {
		// 如果EditText中已经有相同资源的ImageSpan,则不再读取图片
		ImageSpan imageSpan = getImageSpanFromExistence(imagePath);
		if (imageSpan != null) {
			insertIntoEditor(imageSpan, imagePath);
			return;
		}

		if (imagePath != null && (!imagePath.equals("null"))
				&& (!"".equals(imagePath))) {
			insertIntoEditor(imagePath);

			/*
			 * 不再用缓存模式 
			 */
		}
	}

	/**
	 * 向光标位置插入ImageSpan,针对EditText已经有ImageSpan的情况
	 * 
	 * @param ip
	 *            ImageSpan
	 * @param path
	 *            路径
	 */
	private void insertIntoEditor(ImageSpan span, String path) {
		SpannableString ss = new SpannableString("<img " + path + " img>");
		if (span == null)
			throw new NullPointerException("span cant be null");
		ss.setSpan(span, 0, ("<img " + path + " img>").length(),
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		Editable et = contentText.getText();// 先获取Edittext中的内容
		int start = contentText.getSelectionStart();
		et.insert(start, ss);// 设置ss要添加的位置
		contentText.setSelection(start + ss.length());// 设置Edittext中光标在最后面显示
		Logg.D("insertIntoEditor by using existed ImageSpan");
	}

	/**
	 * 向光标位置插入ImageSpan,针对EditText没有图片的情况
	 * 
	 * @param path
	 *            图片路径
	 */
	private void insertIntoEditor(String path) {
		SpannableString ss = new SpannableString("<img " + path + " img>");
		// 不再用缓存模式
		// Bitmap bm = mEditorHelper.getImage(path);
		Bitmap bm = PictureHelper.getImageFromPath(imagePath,
				screenWidth * 0.7F, screenWidth * 0.7F, false, 100, Editor.this,
				imgPadding, false);
		if (bm == null) {
			throw new NullPointerException("bm cant be null");
		}

		ImageSpan span = new ImageSpan(this, bm, ImageSpan.ALIGN_BASELINE);
		ss.setSpan(span, 0, ("<img " + path + " img>").length(),
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		Editable et = contentText.getText();// 先获取Edittext中的内容
		int start = contentText.getSelectionStart();
		et.insert(start, ss);// 插入图片到光标处
		contentText.setSelection(start + ss.length());// 设置Edittext中光标在最后面显示
		Logg.D("insertIntoEditor by loading new");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mEditorHelper.clearImageCache();
		mEditorHelper = null;
		Logg.D("Editor destroyed");
	}

	private class ImageSpanLoaderStatus {
		public boolean isWorking = false;
		public boolean isFinished = false;
	}
}
