package com.mars.note.app;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ClickableSpan;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.mars.note.R;
import com.mars.note.api.BaseActivity;
import com.mars.note.api.Config;
import com.mars.note.api.ImageSpanInfo;
import com.mars.note.api.AlertDialogFactory;
import com.mars.note.api.ProgressDialogFactory;
import com.mars.note.database.*;
import com.mars.note.utils.FileHelper;
import com.mars.note.utils.Logg;
import com.mars.note.utils.PictureHelper;
import com.mars.note.utils.Util;
import com.mars.note.views.DateAlterDialog;

public class EditorActivity extends BaseActivity implements android.view.View.OnClickListener {
	private boolean DEBUG = true;
	private Calendar calendar;
	private long recordTime;
	private EditText titleText;
	private EditText contentText; // 20141205图文混排去掉底部白线 NoteContentEditText
	private DisplayMetrics dm = null;
	private String note_id;
	private NoteDataBaseManager noteDBManager;
	private NoteRecord mNoteRecord;
	private boolean isPastNote = false;
	private boolean isNewIntent = false;
	private static final int SELECT_PIC_KITKAT = 0;
	private static final int SELECT_PIC = 1;
	private static final int REQUEST_VALIDATE = 2;
	private static final int REQUEST_CAMERA = 3;
	private String imagePath, cameraIMGPath; // imagePath 第一张图片
	private byte[] imgSpanBytes;
	private ActionBar actionBar;
	public static int MAX_CONTENT;
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
	private static final int MAX_IMG_NUMS = 10;
	public static final int thread_num = 10;

	private ExecutorService executors;
	private Handler mhandler;
	private ConcurrentHashMap<String, ImageSpanLoaderStatus> mConcurentMap;
	private ReentrantLock mLock;
	private static final float BitmapSizeScale = 0.7F;
	/**
	 * 缩略图的锁,注意加锁和释放锁都要在同一线程,另外注意static的生命周期高于Activity，否则锁的作用范围不会跨出Activity，即失效
	 */
	private static ReentrantLock mTHUMBNAILS_LOCK;
	private CountDownLatch mTHUMBNAILS_LATCH, mContentLatch;
	private int saveToDBType = 0;

	View rootView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (DEBUG)
			Logg.D("EditorActivity onCreate");
		imm = (InputMethodManager) getApplicationContext().getSystemService(INPUT_METHOD_SERVICE);
		noteDBManager = NoteApplication.getDbManager();
		if (mTHUMBNAILS_LOCK == null) {
			// Logg.T("mTHUMBNAILS_LOCK new ");
			mTHUMBNAILS_LOCK = new ReentrantLock(true);// 公平锁
		}
		executors = NoteApplication.getExecutors();
		if (executors == null) {
			throw new NullPointerException("executors is null");
		}
		mConcurentMap = new ConcurrentHashMap<String, ImageSpanLoaderStatus>();// 保存线程工作状态
		mLock = new ReentrantLock(true);// 20141212 公平锁
		mhandler = new Handler();

		setContentView(R.layout.activity_editor);
		rootView = findViewById(R.id.root);

		if (savedInstanceState != null && savedInstanceState.getString("cameraIMGPath") != null) {
			cameraIMGPath = savedInstanceState.getString("cameraIMGPath");
			if (savedInstanceState.getByteArray("imageSpanInfo") != null) {
				imgSpanBytes = savedInstanceState.getByteArray("imageSpanInfo");
				// Logg.S("onCreate imageSpanInfo " + imgSpanBytes);
			}
		}

		contenTitle = new TextView(this);
		titleText = (EditText) this.findViewById(R.id.titleText);
		contentText = (EditText) this.findViewById(R.id.contentText);
		initPopupWindow();

		mIntent = getIntent();
		appWidgetId = mIntent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);

		if (dm == null) {
			dm = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(dm);
		}
		screenWidth = dm.widthPixels;
		screenHeight = dm.heightPixels;
		imgPadding = Util.dpToPx(getResources(), 3);

		MAX_CONTENT = getResources().getInteger(R.integer.editor_max_content);

		if (appWidgetId != -1) { // 此时时从widget进入的
			validate();// 验证密码
			this.getActionBar().hide(); // 先隐藏等待验证，若不隐藏则此时actionbar是悬浮的，必须在requestFeature之后
		} else { // 此时是从程序内部组件进入的
			refresh();
		}
	}

	private void showExectingDialog() {
		ProgressDialogFactory.showProgressDialog(this, rootView);
	}

	private void dissmissExecutingDialog() {
		ProgressDialogFactory.dismissProgressDialog();
	}

	private void initPopupWindow() {
		overflow_menu = LayoutInflater.from(this).inflate(R.layout.popup_window_add_note_overflow, null);
		changeDate = (Button) overflow_menu.findViewById(R.id.change_date);
		deleteSingle = (Button) overflow_menu.findViewById(R.id.delete_single);
		shareWithOthers = (Button) overflow_menu.findViewById(R.id.share_with_others);
		changeDate.setOnClickListener(this);
		deleteSingle.setOnClickListener(this);
		shareWithOthers.setOnClickListener(this);
		overflow_menu_pw = new PopupWindow(overflow_menu, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, false);
		// 如果不设置,popwindow外部不能touch
		overflow_menu_pw.setBackgroundDrawable(new BitmapDrawable());

		overflow_menu_pw.setOutsideTouchable(true);
		overflow_menu_pw.setFocusable(true);
		overflow_menu_pw.update();
		cameraOrGallery_menu = LayoutInflater.from(this).inflate(R.layout.popup_window_camera_or_gallery, null);
		camera = (Button) cameraOrGallery_menu.findViewById(R.id.camera);
		gallery = (Button) cameraOrGallery_menu.findViewById(R.id.gallery);
		camera.setOnClickListener(this);
		gallery.setOnClickListener(this);
		cameraOrGallery_menu_pw = new PopupWindow(cameraOrGallery_menu, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, false);
		cameraOrGallery_menu_pw.setBackgroundDrawable(new BitmapDrawable());
		cameraOrGallery_menu_pw.setOutsideTouchable(true);
		cameraOrGallery_menu_pw.setFocusable(true);
		cameraOrGallery_menu_pw.update();
	}

	private void refresh() {
		// Logg.S("onCreate refresh");
		note_id = mIntent.getStringExtra("note_id");
		if (null != note_id) {
			mNoteRecord = noteDBManager.querySingleRecord(note_id);
		}
		if (mNoteRecord != null) {
			if (DEBUG)
				Logg.I("isPastNote ");
			isPastNote = true;
			String WidgetID = noteDBManager.queryWidgetIDByNoteID(String.valueOf(note_id));
			if (WidgetID != null) {
				appWidgetId = Integer.parseInt(WidgetID);
			}
		} else {
			if (DEBUG)
				Logg.I("notPastNote ");
			isPastNote = false;
			if (DEBUG)
				Logg.I("widgetId = " + appWidgetId);
			if (appWidgetId != -1) {
				isRelatedToWidget = true;
				note_id = noteDBManager.querySingleRecordIDByWidgetID(String.valueOf(appWidgetId));
				if (note_id != null) {
					isAddedWidgetRelation = true;
					mNoteRecord = noteDBManager.querySingleRecord(note_id);
				}
			}
		}
		if (mNoteRecord != null) {
			// if (DEBUG)
			// Logg.I("content : " + mNoteRecord.content);
		}

		isAddFromCalendar = mIntent.getBooleanExtra("add_from_calendar", false);
		/*
		 * 如果是null,则为第一次启动editor的情况 如果不是null,是editor在后台被调用的清况，因为editor的lauchmode
		 * 是singleTash 目前默认保留缓存 因为缓存可以自动调整,而且对于重复打开同一ID的记录这样的情况，图片可以不用再加载
		 */

		initActionBar();
		initContent();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if (DEBUG)
			Logg.D("onNewIntent");
		mIntent = intent;
		reset();
		isNewIntent = true;
		mConcurentMap.clear();
		appWidgetId = mIntent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
		if (appWidgetId != -1) { // 此时时从widget进入的
			validate();// 验证密码
			this.getActionBar().hide(); // 先隐藏等待验证，若不隐藏则此时actionbar是悬浮的，必须在requestFeature之后
		} else { // 此时是从程序内部组件进入的
			refresh();
		}
	}

	/**
	 * 清空数据
	 */
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

	/**
	 * 拍照时，Activity可能因为内存不足回收，造成图片地址丢失
	 */
	@Override
	public void onSaveInstanceState(Bundle outState) {
		if (DEBUG)
			Logg.D("EditorActivity onSaveInstanceState");
		outState.putString("cameraIMGPath", cameraIMGPath);
		byte[] bytes = getImageSpanInfoBytesFromObject(getSpansInfo());
		if (bytes != null) {
			outState.putByteArray("imageSpanInfo", bytes);
			// Logg.S("onSaveInstanceState imageSpanInfo " + bytes);
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onStart() {
		if (DEBUG)
			Logg.D("EditorActivity onStart");
		super.onStart();
	}

	@Override
	protected void onResume() {
		if (DEBUG)
			Logg.D("EditorActivity onResume");
		super.onResume();
	}

	private void initActionBar() {
		actionBar = this.getActionBar();
		calendar = Calendar.getInstance();
		recordTime = calendar.getTimeInMillis();
		// widget 和 pastNote的时间都存在recordTime中
		if (isPastNote || isAddedWidgetRelation) {
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
		String[] date_title = this.getResources().getStringArray(R.array.date_title);
		String[] time_title = this.getResources().getStringArray(R.array.time_title);
		String nowDate = (calendar.get(Calendar.MONTH) + 1) + date_title[1] + calendar.get(Calendar.DAY_OF_MONTH) + date_title[2];
		;
		String nowTime = calendar.get(Calendar.HOUR_OF_DAY) + time_title[0] + calendar.get(Calendar.MINUTE) + time_title[1];
		String dayOfWeekText = getdayOfWeek(calendar.get(Calendar.DAY_OF_WEEK));
		String subTitle = dayOfWeekText + " " + nowTime;
		actionBar.setTitle(nowDate);
		actionBar.setSubtitle(subTitle);
	}

	private void initContent() {
		// Logg.S("onCreate initContent");
		titleText.setTextColor(Color.DKGRAY);
		contentText.setTextColor(Color.DKGRAY);

		contenTitle.setTextSize(20);
		contentText.addTextChangedListener(new EditTextWatcher(MAX_CONTENT, contentText));

		// 20141219 支持ImageSpan的点击事件
		// contentText.setMovementMethod(movement);
		// Logg.S("initContent isPastNote " + isPastNote);
		// Logg.S("initContent isAddedWidgetRelation " + isAddedWidgetRelation);
		if (isPastNote || isAddedWidgetRelation || isNewIntent) {
			isNewIntent = false;

			titleText.setText(mNoteRecord.title);
			// 如何解析图片？
			// 20141211 从数据库获取bytes[]
			contentText.setText(mNoteRecord.content);

			byte[] data = mNoteRecord.imageSpanInfos;
			if (data != null) {
				contentText.setVisibility(View.INVISIBLE);
				if (DEBUG)
					Logg.D("contentText hide replaceAllImgs");
				// bytes[]转换对象
				ArrayList<ImageSpanInfo> imageSpanInfoList = Util.getImageSpanInfoListFromBytes(data);
				// 替换所有ImageSpan
				replaceAllImgs(imageSpanInfoList);
			} else {
				Logg.D("initContent data null");
			}
			imagePath = mNoteRecord.imgpath;
		}
		contenTitle.setText(contentText.length() + "/" + EditorActivity.MAX_CONTENT);
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
		MenuItem content_title = menu.findItem(R.id.content_title).setVisible(false);
		contenTitle.setPadding(0, 0, 50, 0);
		content_title.setActionView(contenTitle);
		menu.findItem(R.id.add_picture).setVisible(true);

		// 自定义Menu的View
		MenuItem others = menu.findItem(R.id.over_flow_menus);
		View view = LayoutInflater.from(this).inflate(R.layout.editor_overflow, null);
		ImageView img = (ImageView) view.findViewById(R.id.over_flow_menu);
		img.setOnClickListener(this);

		others.setActionView(view);

		MenuItem add_pic = menu.findItem(R.id.add_picture);
		View view2 = LayoutInflater.from(this).inflate(R.layout.editor_overflow2, null);
		ImageView img2 = (ImageView) view2.findViewById(R.id.cameraOrGallery_menu);
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
		public void beforeTextChanged(CharSequence chars, int start, int count, int after) {
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			Editable editable = editText.getText();
			int len = editable.length();
			contenTitle.setText(len + "/" + this.maxLen);
			if (len == maxLen) {
				contenTitle.setTextColor(Color.DKGRAY);
			} else {
				contenTitle.setTextColor(Color.BLACK);
			}
			if (len > maxLen) {
				Toast.makeText(EditorActivity.this.getApplicationContext(), getString(R.string.toast_reached_max_text), 3000).show();
				// if (DEBUG)
				// Logg.I("toast_reached_max_text");
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
	private void relaceImageSpans(ImageSpan span, int start, int end, String path) {
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
	private void relaceImageSpans(Drawable drawable, int start, int end, String path) {
		// bug 本地图片被删除后无法获取尺寸信息，
		float[] out = PictureHelper.getCompressedMeasure(path.substring(5, path.length() - 5), screenWidth * BitmapSizeScale, screenWidth * BitmapSizeScale);
		Editable et = contentText.getText();
		if (out == null) {
			// et.delete(start, end);
			out = new float[] { screenWidth * 0.7F, screenWidth * 0.7F };
		}

		SpannableString ss = new SpannableString(path);// path is like <img ...
														// // img>
		if (drawable == null) {
			throw new NullPointerException("drawable cant be null");
		}
		drawable.setBounds(0, 0, (int) out[1], (int) out[0]);
		ImageSpan span = new ImageSpan(drawable, ImageSpan.ALIGN_BASELINE);
		ss.setSpan(span, 0, path.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		et.delete(start, end);
		et.insert(start, ss);
		contentText.setSelection(end);
	}

	private void replaceAllImageSpans(String path, ArrayList<ImageSpanInfo> list, Bitmap bm) {
		if (bm == null) {
			throw new NullPointerException("bm cant be null");
		}

		for (ImageSpanInfo isi : list) {
			if (path.equals(isi.path)) {
				// path is like <img ... img>
				SpannableString ss = new SpannableString(path);
				ImageSpan span = new ImageSpan(this, bm, ImageSpan.ALIGN_BASELINE);
				ss.setSpan(span, 0, path.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
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
		if (((titleText.getText().toString() != null) && (!"".equals(titleText.getText().toString())))
				|| ((contentText.getText().toString() != null) && (!"".equals(contentText.getText().toString())))) {
			saveToDB();
		} else {
			EditorActivity.this.finish();
			overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
		}
	}

	/**
	 * 注意与数据库获取区分，是从当前文本获取ImageSpan的路径，首位置、末尾位置，供解析
	 * 
	 * @return 返回ImageSpanInfo的集合
	 */
	private ArrayList<ImageSpanInfo> getSpansInfo() {
		Editable edit = contentText.getText();
		ImageSpan[] imageSpans = edit.getSpans(0, contentText.getText().length(), ImageSpan.class);

		if (imageSpans.length == 0) {
			if (DEBUG)
				Logg.I("no span");
			return null;
		}
		if (DEBUG)
			Logg.I("span count " + imageSpans.length);
		// List保存Span信息
		ArrayList<ImageSpanInfo> list = new ArrayList<ImageSpanInfo>();
		for (ImageSpan ip : imageSpans) {
			int start = edit.getSpanStart(ip);
			int end = edit.getSpanEnd(ip);
			String path = edit.toString().substring(start, end);
			if (start < 0 || end < 0 || path == null) {
				throw new IllegalStateException("start < 0 || end < 0 || path == null");
			}
			ImageSpanInfo info = new ImageSpanInfo();
			info.start = start;
			info.end = end;
			info.path = path;
			list.add(info);
		}
		if (DEBUG)
			Logg.I("list size " + list.size());
		return list;
	}

	/**
	 * 将ArrayList转化为二进制数组
	 * 
	 * @param list
	 *            ArrayList对象
	 * @return 二进制数组
	 */
	private byte[] getImageSpanInfoBytesFromObject(ArrayList<ImageSpanInfo> list) {
		if (list == null)
			return null;

		if (DEBUG)
			Logg.I("imagespan cout to db " + list.size());
		ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
		try {
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(arrayOutputStream);
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
	private ArrayList<ImageSpanInfo> addImageSpanInfosToRecord(NoteRecord nr) {
		// 20141211 保存ImageSpanInfos 到数据库
		// 注意ArrayList已经实现了Serializable,可以直接序列化
		ArrayList<ImageSpanInfo> spanInfo = getSpansInfo();
		if (DEBUG)
			Logg.I("ArrayList before revert by bytes");
		if (spanInfo != null) {
			// 将ArrayList Object 转换为二进制数组
			byte[] bytes = getImageSpanInfoBytesFromObject(spanInfo);
			if (bytes == null) {
				throw new NullPointerException("bytes is null!");
			}
			nr.imageSpanInfos = bytes;
			String path0 = spanInfo.get(spanInfo.size() - 1).path;
			imagePath = path0.substring(5, path0.length() - 5);
			if (DEBUG) {
				Logg.I("addImageSpanInfosToRecord imagePath " + imagePath);
			}
		} else {
			// 20141215 同步更新预览图 ，imagespan info
			imagePath = null;
			nr.imageSpanInfos = null;
		}
		return spanInfo;
	}

	private void saveCroppedImage(final String path) {
		// 此处异步可能导致图片未保存，已经退出到主界面，另一个线程先去读取图片未读到抛出异常
		// new Thread() {
		// @Override
		// public void run() {
		Bitmap bm = getBitmapFromImageSpan(path);
		if (bm == null)
			throw new NullPointerException("bitmap cant be null");
		if (DEBUG) {
			Logg.I("path " + path);
			Logg.I("saveCroppedImage bm width " + bm.getWidth() + " height " + bm.getHeight());
		}
		bm = PictureHelper.getCropImageFromBitmap(bm, getResources().getDimension(R.dimen.listview_image_width),
				getResources().getDimension(R.dimen.listview_image_height), true, 100, EditorActivity.this, 7, true);
		noteDBManager.addNewCroppedImage(path, bm);
		// }
		// }.start();
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
			if (overflow_menu_pw.isShowing()) {
				overflow_menu_pw.dismiss();
			}

			final DateAlterDialog mChangeDateDialog = new DateAlterDialog(this, null, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
					calendar.get(Calendar.DAY_OF_MONTH));
			final DatePicker mDatePicker = mChangeDateDialog.getDatePicker();
			mChangeDateDialog.showAll();

			mChangeDateDialog.setBtnListener(new android.view.View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (mDatePicker.getYear() != calendar.get(Calendar.YEAR) || mDatePicker.getMonth() != calendar.get(Calendar.MONTH)
							|| mDatePicker.getDayOfMonth() != calendar.get(Calendar.DAY_OF_MONTH)) {
						Date date = new Date(mDatePicker.getYear() - 1900, mDatePicker.getMonth(), mDatePicker.getDayOfMonth());
						recordTime = date.getTime();
						calendar.setTime(date);
						String[] date_title = EditorActivity.this.getResources().getStringArray(R.array.date_title);
						String[] time_title = EditorActivity.this.getResources().getStringArray(R.array.time_title);
						String nowDate = (calendar.get(Calendar.MONTH) + 1) + date_title[1] + calendar.get(Calendar.DAY_OF_MONTH) + date_title[2];
						;
						String nowTime = calendar.get(Calendar.HOUR_OF_DAY) + time_title[0] + calendar.get(Calendar.MINUTE) + time_title[1];
						String dayOfWeekText = getdayOfWeek(calendar.get(Calendar.DAY_OF_WEEK));
						String subTitle = dayOfWeekText + " " + nowTime;
						actionBar.setTitle(nowDate);
						actionBar.setSubtitle(subTitle);
					}
					mChangeDateDialog.dismiss();

				}
			}).show();
			break;
		case R.id.delete_single:
			// AlertDialog.Builder myDialog = new AlertDialog.Builder(this);
			// myDialog.setMessage(R.string.context_menu_single_delete_title);
			// myDialog.setPositiveButton(EditorActivity.this.getString(R.string.yes),
			// new OnClickListener() {
			// @Override
			// public void onClick(DialogInterface dialog, int which) {
			// noteDBManager.deleteRecord(mNoteRecord);
			// Config.recent_needRefresh = true;
			// EditorActivity.this.finish();
			// overridePendingTransition(android.R.anim.fade_in,
			// android.R.anim.fade_out);
			// if (overflow_menu_pw.isShowing()) {
			// overflow_menu_pw.dismiss();
			// }
			// }
			// });
			// myDialog.setNegativeButton(EditorActivity.this.getString(R.string.no),
			// null);
			// myDialog.show();
			if (overflow_menu_pw.isShowing()) {
				overflow_menu_pw.dismiss();
			}

			AlertDialogFactory.showAlertDialog(this, rootView, this.getString(R.string.context_menu_single_delete_title),
					new AlertDialogFactory.DialogPositiveListner() {
						@Override
						public void onClick(View arg0) {
							super.onClick(arg0);
							noteDBManager.deleteRecord(mNoteRecord);
							Config.recent_needRefresh = true;
							EditorActivity.this.finish();
							overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

						}
					});
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
			share.putExtra(Intent.EXTRA_TEXT, titleText.getText() + "\n" + contentText.getText());
			share.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			this.startActivity(Intent.createChooser(share, this.getString(R.string.share_title)));
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
				File picFolder = new File(BackUpActivity.BACKUP_PATH + "pics/");
				if (!picFolder.exists()) {
					picFolder.mkdirs();
				}
				cameraIMGPath = BackUpActivity.BACKUP_PATH + "pics/bitmap_" + System.currentTimeMillis() + ".bmp";
				camera.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(cameraIMGPath)));
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
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
					Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
					intent.addCategory(Intent.CATEGORY_OPENABLE);
					intent.setType("image/*");
					startActivityForResult(intent, SELECT_PIC_KITKAT);
				} else {
					Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
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
		ImageSpan[] imageSpan = edit.getSpans(0, edit.length(), ImageSpan.class);
		// Logg.I("imagespan count " + imageSpan.length);
		// mSet取不相同的路径
		HashSet<String> mSet = new HashSet<String>();
		for (ImageSpan ip : imageSpan) {
			int start = edit.getSpanStart(ip);
			int end = edit.getSpanEnd(ip);
			String path = edit.toString().substring(start, end);
			mSet.add(path);
			// Logg.I(path);
		}
		// Logg.I("mSet size " + mSet.size());
		if (mSet.size() < MAX_IMG_NUMS) {
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
		case SELECT_PIC:
			// 接受图库返回结果 4.2
			if (RESULT_OK == resultCode) {
				Uri uri = data.getData();
				Cursor cursor = getContentResolver().query(uri, null, null, null, null);
				cursor.moveToFirst();
				// String imgNo = cursor.getString(0); // 图片编号
				imagePath = cursor.getString(1); // 图片文件路径
				// String imgSize = cursor.getString(2); // 图片大小
				// String imgName = cursor.getString(3); // 图片文件名
				cursor.close();
				// imgContainer.setVisibility(View.VISIBLE);
				setImageView();
			}
			break;
		case REQUEST_VALIDATE:
			// 接受返回的验证信息
			if (resultCode == RESULT_CANCELED) {
				this.finish(); // 20141202 验证失败则退出
			} else if (resultCode == RESULT_OK) {
				this.getActionBar().show(); // 验证成功后显示actionbar
				// Logg.I("refresh");
				refresh();
			}
			break;
		case REQUEST_CAMERA:

			// 20141204接受相机返回结果
			if (resultCode == RESULT_OK) {
				imagePath = cameraIMGPath;
				// Logg.S("onActivityResult imagePath " + imagePath);
				if (imgSpanBytes != null) {
					ArrayList<ImageSpanInfo> imageSpanInfoList = Util.getImageSpanInfoListFromBytes(imgSpanBytes);
					imgSpanBytes = null;
					// 替换所有ImageSpan
					replaceAllImgs(imageSpanInfoList);
				} else {
					setImageView();
					Logg.S("add ImageView normal");
					cameraIMGPath = null;
				}
			} else {
				cameraIMGPath = null;
				if (imgSpanBytes != null) {
					ArrayList<ImageSpanInfo> imageSpanInfoList = Util.getImageSpanInfoListFromBytes(imgSpanBytes);
					imgSpanBytes = null;
					// 替换所有ImageSpan
					replaceAllImgs(imageSpanInfoList);
				}
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
				// Logg.I("find existed ImageSpan");
				return new ImageSpan(ip.getDrawable(), ImageSpan.ALIGN_BASELINE);
			}
		}
		// mSet取不相同的路径
		return null;
	}

	private Bitmap getBitmapFromImageSpan(String source) {
		Editable edit = contentText.getText();
		if (DEBUG)
			Logg.I("path getBitmapFromImageSpan " + source);
		ImageSpan[] spans = edit.getSpans(0, edit.length(), ImageSpan.class);
		for (ImageSpan ip : spans) {
			int start = edit.getSpanStart(ip);
			int end = edit.getSpanEnd(ip);
			String path = edit.toString().substring(start, end);
			path = path.substring(5, path.length() - 5);

			if (source.equals(path)) {
				if (DEBUG)
					Logg.I("find existed ImageSpan");
				Drawable d = ip.getDrawable();
				BitmapDrawable bd = null;
				try {
					bd = (BitmapDrawable) d;
				} catch (ClassCastException e) {
					if (DEBUG)
						Logg.I("unvailable drawable");
					return null;
				}
				return bd.getBitmap();
			}
		}
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

		if (imagePath != null && (!imagePath.equals("null")) && (!"".equals(imagePath))) {
			insertIntoEditor(imagePath);
			/*
			 * 不再用缓存模式
			 */
		}
	}

	/**
	 * 向光标位置插入ImageSpan,针对EditText已经有ImageSpan的情况 Bug 光标位置-1 ?
	 * 
	 * @param ip
	 *            ImageSpan
	 * @param path
	 *            路径
	 */
	private void insertIntoEditor(ImageSpan span, String path) {
		if (("<img " + path + " img>").length() + contentText.getText().length() > MAX_CONTENT) {
			Toast.makeText(getApplicationContext(), R.string.toast_reached_max_text, 2000).show();
			return;
		}
		SpannableString ss = new SpannableString("<img " + path + " img>");
		if (span == null)
			throw new NullPointerException("span cant be null");
		ss.setSpan(span, 0, ("<img " + path + " img>").length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		Editable et = contentText.getText();// 先获取Edittext中的内容
		int start = contentText.getSelectionStart();
		et.insert(start, ss);// 设置ss要添加的位置
		contentText.setSelection(start + ss.length());// 设置Edittext中光标在最后面显示
		// Logg.I("insertIntoEditor by using existed ImageSpan");
		// attachClickableSpan();
	}

	/**
	 * 向光标位置插入ImageSpan,针对EditText没有图片的情况 Bug 光标位置-1 ?
	 * 
	 * @param path
	 *            图片路径
	 */
	private void insertIntoEditor(String path) {
		if (("<img " + path + " img>").length() + contentText.getText().length() > MAX_CONTENT) {
			Toast.makeText(getApplicationContext(), R.string.toast_reached_max_text, 2000).show();
			return;
		}
		SpannableString ss = new SpannableString("<img " + path + " img>");
		// 不再用缓存模式
		// Bitmap bm = mEditorHelper.getImage(path);
		Bitmap bm = PictureHelper.getImageFromPath(imagePath, screenWidth * 0.7F, screenWidth * 0.7F, false, 100, EditorActivity.this, imgPadding, false);
		if (bm == null) {
			throw new NullPointerException("bm cant be null");
		}
		// FileHelper.deleteFile(imagePath);

		ImageSpan span = new ImageSpan(this, bm, ImageSpan.ALIGN_BASELINE);
		ss.setSpan(span, 0, ("<img " + path + " img>").length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		Editable et = contentText.getText();// 先获取Edittext中的内容
		int start = contentText.getSelectionStart();

		et.insert(start, ss);// 插入图片到光标处
		contentText.setSelection(start + ss.length());// 设置Edittext中光标在最后面显示
		// Logg.I("insertIntoEditor by loading new");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	private class ImageSpanLoaderStatus {
		public boolean isWorking = false;
		public boolean isFinished = false;
	}

	/**
	 * 将数据保存到数据库
	 */
	private void saveToDB() {
		if (isPastNote) {
			mNoteRecord.title = titleText.getText().toString();
			mNoteRecord.content = contentText.getText().toString();
			mNoteRecord.time = String.valueOf(recordTime);
			mNoteRecord.year = String.valueOf(calendar.get(Calendar.YEAR));
			mNoteRecord.month = String.valueOf(calendar.get(Calendar.MONTH) + 1);
			mNoteRecord.day = String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
			mNoteRecord.hour = String.valueOf(calendar.get(Calendar.HOUR_OF_DAY));
			mNoteRecord.minute = String.valueOf(calendar.get(Calendar.MINUTE));
			mNoteRecord.second = String.valueOf(calendar.get(Calendar.SECOND));
			ArrayList<ImageSpanInfo> spanInfos = addImageSpanInfosToRecord(mNoteRecord);
			mNoteRecord.imgpath = imagePath;
			if (Config.DB_SAVE_MODE && imagePath != null && !"".equals(imagePath)) {
				saveCroppedImage(imagePath);
				saveToDBType = 1;
				saveThumbnails();
			} else {
				noteDBManager.updateRecord(mNoteRecord);
				if (appWidgetId != -1) {
					Intent intent = new Intent("com.mars.note.widget.refresh");
					intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
					this.sendBroadcast(intent);
				}
				finish();
				overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
			}
		} else if (isAddedWidgetRelation) {
			mNoteRecord.title = titleText.getText().toString();
			mNoteRecord.content = contentText.getText().toString();
			mNoteRecord.time = String.valueOf(recordTime);
			mNoteRecord.year = String.valueOf(calendar.get(Calendar.YEAR));
			mNoteRecord.month = String.valueOf(calendar.get(Calendar.MONTH) + 1);
			mNoteRecord.day = String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
			mNoteRecord.hour = String.valueOf(calendar.get(Calendar.HOUR_OF_DAY));
			mNoteRecord.minute = String.valueOf(calendar.get(Calendar.MINUTE));
			mNoteRecord.second = String.valueOf(calendar.get(Calendar.SECOND));
			ArrayList<ImageSpanInfo> spanInfos = addImageSpanInfosToRecord(mNoteRecord);
			mNoteRecord.imgpath = imagePath;
			if (Config.DB_SAVE_MODE && imagePath != null && !"".equals(imagePath)) {
				saveCroppedImage(imagePath);
				saveToDBType = 2;
				saveThumbnails();
			} else {
				noteDBManager.updateRecord(mNoteRecord);
				finish();
				overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
				Intent intent = new Intent("com.mars.note.widget.refresh");
				intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
				this.sendBroadcast(intent);
			}
		} else {
			int totdayRecordCount = noteDBManager.getDayRecordCount(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1,
					calendar.get(Calendar.DAY_OF_MONTH));
			if (totdayRecordCount >= Config.maxCountEachDay) {
				Toast.makeText(this, this.getText(R.string.toast_cant_add_note), 1000).show();
			} else {
				mNoteRecord = new NoteRecord();
				mNoteRecord.title = titleText.getText().toString();
				mNoteRecord.content = contentText.getText().toString();
				mNoteRecord.time = String.valueOf(recordTime);
				mNoteRecord.year = String.valueOf(calendar.get(Calendar.YEAR));
				mNoteRecord.month = String.valueOf(calendar.get(Calendar.MONTH) + 1);
				mNoteRecord.day = String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
				mNoteRecord.hour = String.valueOf(calendar.get(Calendar.HOUR_OF_DAY));
				mNoteRecord.minute = String.valueOf(calendar.get(Calendar.MINUTE));
				mNoteRecord.second = String.valueOf(calendar.get(Calendar.SECOND));
				ArrayList<ImageSpanInfo> spanInfos = addImageSpanInfosToRecord(mNoteRecord);
				if (Config.DB_SAVE_MODE && imagePath != null && !"".equals(imagePath)) {
					saveCroppedImage(imagePath);
					saveToDBType = 3;
					saveThumbnails();
				} else {
					mNoteRecord.imgpath = this.imagePath;
					noteDBManager.addRecord(mNoteRecord);
					if (isRelatedToWidget) {
						String noteId = noteDBManager.querySingleRecordIDByTime(String.valueOf(recordTime));
						noteDBManager.addWidgetRelation(String.valueOf(appWidgetId), noteId);
						Intent intent = new Intent("com.mars.note.widget.refresh");
						intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
						this.sendBroadcast(intent);
					}
					finish();
					overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
				}
			}
		}
	}

	/**
	 * 保存所有的缩略图到数据库
	 */
	private void saveThumbnails() {
		final Editable edit = contentText.getText();
		final ImageSpan[] imageSpans = edit.getSpans(0, contentText.getText().length(), ImageSpan.class);
		if (imageSpans.length == 0) {
			if (DEBUG)
				Logg.I("saveThumbnails no image");
			return;
		}

		/**
		 * 等待所有子线程（执行单个写入图片到数据库的操作）执行完毕，此时计数器为0，继续执行，释放锁mTHUMBNAILS_LOCK
		 */
		mTHUMBNAILS_LATCH = new CountDownLatch(imageSpans.length);

		showExectingDialog();
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					/**
					 * 防止图片未加载到数据库，已经请求数据库的情况，在写入数据库时加入锁
					 */
					mTHUMBNAILS_LOCK.lock();
					// 有图片需要存入数据库
					for (ImageSpan ip : imageSpans) {
						int start = edit.getSpanStart(ip);
						int end = edit.getSpanEnd(ip);
						String path = edit.toString().substring(start, end);
						path = path.substring(5, path.length() - 5);
						if (DEBUG)
							Logg.I("saveThumbnails path = " + path);
						if (start < 0 || end < 0 || path == null) {
							throw new IllegalStateException("start < 0 || end < 0 || path == null");
						}
						if (noteDBManager.isThumbnailExisted(path)) {
							if (DEBUG)
								Logg.I("saveThumbnails path = " + path + " existed");
							// 若图片存在，则不需写入，计数器-1
							mTHUMBNAILS_LATCH.countDown();
							// 跳到下一次循环
							continue;
						} else {
							if (DEBUG)
								Logg.I("saveThumbnails path = " + path + " not existed");
						}
						final String path0 = path;
						Drawable d = ip.getDrawable();
						BitmapDrawable bd = (BitmapDrawable) d;
						final Bitmap bm = bd.getBitmap();

						// 此线程会将bitmap写入数据库
						Runnable r = new Runnable() {
							@Override
							public void run() {
								noteDBManager.addNewThumbnail(path0, bm);
								if (DEBUG)
									Logg.I("saveThumbnails path " + path0 + " done");
								// 写入图片完成后，计数器-1
								mTHUMBNAILS_LATCH.countDown();
							}
						};
						executors.execute(r);
					}
					if (DEBUG)
						Logg.T("saveThumbnails mTHUMBNAILS_LOCK locked");
					// 锁住不继续执行，直到计数器为0
					mTHUMBNAILS_LATCH.await();
					mTHUMBNAILS_LATCH = null;
					if (DEBUG)
						Logg.I("saveThumbnails all task done");
					// 不同的线程不能够释放锁，即加锁和释放锁的线程必须是同一个
					// 此时表示可以读取缩略图或写入缩略图了
					// Thread.sleep(5000); //test
					mTHUMBNAILS_LOCK.unlock();
					mhandler.post(new Runnable() {
						@Override
						public void run() {
							dissmissExecutingDialog();
							switch (saveToDBType) {
							case 1:
								noteDBManager.updateRecord(mNoteRecord);
								if (appWidgetId != -1) {
									Intent intent = new Intent("com.mars.note.widget.refresh");
									intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
									sendBroadcast(intent);
								}
								finish();
								overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
								break;
							case 2:
								noteDBManager.updateRecord(mNoteRecord);
								finish();
								overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
								Intent intent2 = new Intent("com.mars.note.widget.refresh");
								intent2.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
								sendBroadcast(intent2);
								break;
							case 3:
								mNoteRecord.imgpath = imagePath;
								noteDBManager.addRecord(mNoteRecord);
								if (isRelatedToWidget) {
									String noteId = noteDBManager.querySingleRecordIDByTime(String.valueOf(recordTime));
									noteDBManager.addWidgetRelation(String.valueOf(appWidgetId), noteId);
									Intent intent3 = new Intent("com.mars.note.widget.refresh");
									intent3.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
									sendBroadcast(intent3);
								}
								finish();
								overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
								break;
							default:
								throw new IllegalArgumentException("error0");
							}
						}
					});

					if (DEBUG)
						Logg.T("saveThumbnails mTHUMBNAILS_LOCK release lock");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	/**
	 * 将字符串转换为ImageSpan，从数据库中得到ImageSpanInfo
	 */
	private void replaceAllImgs(final ArrayList<ImageSpanInfo> spanInfo) {
		// 从数据库读取List<ImageSpanInfo> spanInfo
		if (spanInfo == null) {
			if (DEBUG)
				Logg.D("replaceAllImgs no image");
			return;
		}

		// * 等待所有子线程（执行单个读取图片的操作）执行完毕，此时计数器为0，继续执行，释放锁mTHUMBNAILS_LOCK
		mTHUMBNAILS_LATCH = new CountDownLatch(spanInfo.size());
		// //等待所有子线程置换完Preload图片后才显示EditText,否则图片地址会显示出来
		// mContentLatch = new CountDownLatch(spanInfo.size());
		// new Thread(){
		// public void run() {
		// try {
		// Logg.T("waiting for preload pics been replaced");
		// mContentLatch.await();
		// mContentLatch = null;
		// mhandler.post(new Runnable() {
		// @Override
		// public void run() {
		// // TODO Auto-generated method stub
		// contentText.setVisibility(View.VISIBLE);
		// Logg.T("preload pics been replaced");
		// }
		// });
		// } catch (InterruptedException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		//
		// };
		// }.start();

		executors.execute(new Runnable() {
			@Override
			public void run() {
				try {
					/**
					 * 在读取图片时加锁，防止写入
					 */
					mTHUMBNAILS_LOCK.lock();
					if (DEBUG)
						Logg.D("replaceAllImgs mTHUMBNAILS_LOCK locked");

					for (final ImageSpanInfo isi : spanInfo) {
						// 20141211 注意！缓存中的path是绝对地址,isi的path有<img img>在外部
						final String imgPath = isi.path.substring(5, isi.path.length() - 5);
						// Logg.I("ImageSpanInfo imgPath " + imgPath);

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
						// 20141215 此处只需要runnable对象，将交给线程池内部的线程
						Runnable runnable = new Runnable() {
							@Override
							public void run() {
								mLock.lock();// 同时只有一个线程能进入临界区
								ImageSpanLoaderStatus isls = mConcurentMap.get(imgPath);
								if (isls.isFinished) {
									if (DEBUG)
										Logg.D("ImageSpanLoader has finished work");
									mTHUMBNAILS_LATCH.countDown();
									return;
								}
								if (DEBUG)
									Logg.D("ImageSpanLoader hasn't finished work");
								if (!isls.isWorking) {
									// 不可能有两个相同imgPath的线程走到这个逻辑
									isls.isWorking = true;
									mLock.unlock();
									// mhandler.post(new Runnable() {
									// @Override
									// public void run() {
									// // 使用预置图片
									// relaceImageSpans((getResources().getDrawable(R.drawable.editor_preload_img)),
									// isi.start, isi.end, isi.path);
									// mContentLatch.countDown();
									// if (DEBUG)
									// Logg.I("relace ImageSpans by temp");
									// }
									// });

									Bitmap bm = null;
									if (Config.DB_SAVE_MODE) {
										bm = noteDBManager.getThumbnail(imgPath);
										if (bm == null) {
											if (DEBUG)
												Logg.I("path " + imgPath + " not in db");
											// 针对为开启数据库存储的模式
											bm = PictureHelper.getImageFromPath(imgPath, screenWidth * BitmapSizeScale, screenWidth * BitmapSizeScale, false,
													100, EditorActivity.this, imgPadding, false);
										}
									} else {
										bm = PictureHelper.getImageFromPath(imgPath, screenWidth * BitmapSizeScale, screenWidth * BitmapSizeScale, false, 100,
												EditorActivity.this, imgPadding, false);
									}
									final Bitmap pic = bm;
									if (pic == null) {
										if (DEBUG)
											Logg.D("path " + imgPath + " not in db and local");
										mTHUMBNAILS_LATCH.countDown();
										return;
									}
									if (DEBUG)
										Logg.D("path " + imgPath + " in db or local");
									mLock.lock();
									isls.isWorking = false;// 工作完成同步记号
									// 替换所有SpannableString
									mhandler.postDelayed(new Runnable() {
										@Override
										public void run() {
											// 201401212
											// Bitmap构造新的ImageSpan,替换文字
											replaceAllImageSpans(isi.path, spanInfo, pic);
											if (DEBUG)
												Logg.D("relaceImageSpans all by loading new");
										}
									}, 0);
									isls.isFinished = true;
									mLock.unlock();
									mTHUMBNAILS_LATCH.countDown();
								} else {
									mhandler.post(new Runnable() {
										@Override
										public void run() {
											// 使用预置图片
											relaceImageSpans((getResources().getDrawable(R.drawable.editor_preload_img)), isi.start, isi.end, isi.path);
											if (DEBUG)
												Logg.D("relaceImageSpans by temp");
										}
									});
									mLock.unlock();
									mTHUMBNAILS_LATCH.countDown();
								}
							}
						};
						executors.execute(runnable);
					}

					// 锁住不继续执行，直到计数器为0
					mTHUMBNAILS_LATCH.await();
					mTHUMBNAILS_LATCH = null;
					if (DEBUG)
						Logg.D("replaceAllImgs all task done");
					// 不同的线程不能够释放锁，即加锁和释放锁的线程必须是同一个
					// 此时表示可以读取缩略图或写入缩略图了
					mTHUMBNAILS_LOCK.unlock();
					mhandler.post(new Runnable() {
						@Override
						public void run() {
							// TODO Auto-generated method stub
							contentText.setVisibility(View.VISIBLE);
							if (DEBUG)
								Logg.D("contentText show");
						}
					});
					if (DEBUG)
						Logg.T("replaceAllImgs mTHUMBNAILS_LOCK release lock");
					if (cameraIMGPath != null) {
						mhandler.post(new Runnable() {
							@Override
							public void run() {
								setImageView();
								cameraIMGPath = null;
								Logg.S("add ImageView when activity been recycled");
							}
						});
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});

	}
}
