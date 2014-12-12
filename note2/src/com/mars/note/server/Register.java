package com.mars.note.server;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.mars.note.BackUpActivity;
import com.mars.note.R;
import com.mars.note.api.BaseActivity;
import com.mars.note.utils.PictureHelper;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

public class Register extends BaseActivity implements OnClickListener {
	private static final String url = "http://192.168.1.100:8080/MarsNoteServer/Register";
	private ImageView head_photo;
	private final int SELECT_PIC_KITKAT = 0;
	private final int SELECT_PIC = 1;
	
	EditText email,pwd1,pwd2,username;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_register);
		head_photo = (ImageView) this.findViewById(R.id.head_photo);
		head_photo.setOnClickListener(this);
		
		email = (EditText) this.findViewById(R.id.email);
		pwd1 = (EditText) this.findViewById(R.id.pwd);
		pwd2 = (EditText) this.findViewById(R.id.pwd2);
		username = (EditText) this.findViewById(R.id.username);
		
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			this.onBackPressed();
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.head_photo:
			ChooseImage();
			break;
		default:
			break;
		}

	}

	public void ChooseImage() {
		Intent intent;
		// = new Intent(Intent.ACTION_GET_CONTENT);// ACTION_OPEN_DOCUMENT
		intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("image/*");
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
			startActivityForResult(intent, SELECT_PIC_KITKAT);
		} else {
			startActivityForResult(intent, SELECT_PIC);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (RESULT_OK == resultCode) {
			switch (requestCode) {
			case SELECT_PIC_KITKAT:
				Uri selectedImage = data.getData();
				String imagePath = PictureHelper.getPath(this, selectedImage);
				Uri newUri = Uri.parse("file:///" + imagePath);

				startPhotoZoom(newUri);
				break;
			case 3:
				if (data != null) {
					setPicToView(data);
				}
				break;
			default:
				break;
			}
		}
	}

	public void startPhotoZoom(Uri uri) {
		Intent intent = new Intent("com.android.camera.action.CROP");
		intent.setDataAndType(uri, "image/*");
		intent.putExtra("crop", "true");
		// aspectX aspectY 是宽高的比例
		intent.putExtra("aspectX", 1);
		intent.putExtra("aspectY", 1);
		// outputX outputY 是裁剪图片宽高
		intent.putExtra("outputX", 300);
		intent.putExtra("outputY", 300);
		intent.putExtra("return-data", true);
		intent.putExtra("noFaceDetection", true);
		startActivityForResult(intent, 3);
	}

	private void setPicToView(Intent picdata) {
		Bundle extras = picdata.getExtras();
		if (extras != null) {
			Bitmap photo = extras.getParcelable("data");
			head_photo.setImageBitmap(PictureHelper.addEdge(photo, this, 2));
			saveBitmap(photo);
		}
	}

	private void saveBitmap(Bitmap bm) {
		File exportDir = new File(BackUpActivity.BACKUP_PATH);
		if (!exportDir.exists()) {
			exportDir.mkdirs();
		}
		PictureHelper.saveBitmapToPath(bm, BackUpActivity.BACKUP_PATH+"head_photo.png");
	}

	private boolean isEmail(String email) {
		String str = "^([a-zA-Z0-9_\\-\\.]+)@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.)|(([a-zA-Z0-9\\-]+\\.)+))([a-zA-Z]{2,4}|[0-9]{1,3})(\\]?)$";
		Pattern p = Pattern.compile(str);
		Matcher m = p.matcher(email);

		return m.matches();
	}
	
	private boolean isPasswordCorrect(String pwd1,String pwd2){
		if(pwd1.length() < 6 || pwd2.length() <6 || pwd1.length() >20 || pwd2.length() >20){
			return false;
		}
		return true;
//		return pwd1.equals(pwd2);
	}
	
	private boolean isUserNameCorrect(String userName){
//		Log.d("mars","length = "+userName.length());
		if(userName == null || userName.length() == 0 || userName.length() > 20){
			return false;
		}
		return true;
	}
	
	public void registerInfo(View v){
//		Log.d("mars","registerInfo");
		if(!isEmail(email.getText().toString())){
			Toast.makeText(this, this.getString(R.string.email_format_error),1000).show();
			return;
		}
		if(!isPasswordCorrect(pwd1.getText().toString(),pwd2.getText().toString())){
			Toast.makeText(this, this.getString(R.string.pwd_not_correct_error),1000).show();
			return;
		}
		if(!pwd1.getText().toString().equals(pwd2.getText().toString())){
			Toast.makeText(this, this.getString(R.string.pwd_not_equal_error),1000).show();
			return;
		}
		if(!isUserNameCorrect(username.getText().toString())){
			Toast.makeText(this, this.getString(R.string.username_format_error),1000).show();
			return;
		}
		final JSONObject jsonObj = new JSONObject();
		try {
			jsonObj.put("validate", this.getPackageName());
			jsonObj.put("email", email.getText().toString());
			jsonObj.put("password", pwd1.getText().toString());
			jsonObj.put("username",username.getText().toString());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		new Thread(){
			@Override
			public void run() {
				uploadJSON(jsonObj);
			}
		}.start();
		
	}
	private void uploadJSON(JSONObject jsonObj){
		HttpPost request = new HttpPost(url);		
		// 绑定到请求 Entry
		StringEntity se;
		try {
			se = new StringEntity(jsonObj.toString(),"UTF-8");
			request.setEntity(se);
			HttpResponse httpResponse = new DefaultHttpClient().execute(request);
			if(httpResponse.getStatusLine().getStatusCode() == 200){
				 String result= EntityUtils.toString(httpResponse.getEntity());
//				 Log.d("mars",result);
			}
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
}
