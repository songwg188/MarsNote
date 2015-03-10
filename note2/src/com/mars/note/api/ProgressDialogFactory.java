package com.mars.note.api;

import com.mars.note.R;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;

/**
 * @author mars
 * @date 2015-2-6 上午10:52:05
 * @version 1.1
 */
public class ProgressDialogFactory extends DialogFactory{
	private static PopupWindow mDialog;

	/**
	 * 静态工厂方法，创建对话框，复用一个PopupWindow
	 * @param context
	 * @param attachView
	 */
	public static void showProgressDialog(Context context, View attachView) {
		if (mDialog == null) {
			View dialogView = LayoutInflater.from(context).inflate(R.layout.progress_dialog, null, false);
			mDialog = AlertDialogFactory.getPopupWindow(context, dialogView);
		}

		if (mDialog.isShowing())
			return;

		mDialog.setAnimationStyle(R.style.Alterdialog_anim_style); //设置动画
		mDialog.showAtLocation(attachView, Gravity.CENTER, 0, 0);

	}
	
	public static void dismissProgressDialog(){
		if(mDialog != null && mDialog.isShowing())
			mDialog.dismiss();
	}
}
 