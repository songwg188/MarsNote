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
 * @date 2015-1-21 下午4:20:54
 * @version 1.1
 */
public class AlertDialogFactory {

	/**
	 * 
	 * @param v
	 *            自定义视图
	 * @param mode
	 *            类型
	 * @return
	 */
	private static PopupWindow getPopupWindow(Context context, View v) {
		PopupWindow dialog = new PopupWindow(v, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, false);
		dialog.setOutsideTouchable(true);
		dialog.setFocusable(true);
		dialog.setBackgroundDrawable(new BitmapDrawable());
		dialog.update();
		return dialog;
	}

	private static PopupWindow mDialog;

	/**
	 * 静态工厂方法，创建对话框，复用一个PopupWindow
	 * @param context
	 * @param attachView
	 * @param message
	 * @param listener
	 */
	public static void showAlertDialog(Context context, View attachView, String message, View.OnClickListener listener) {
		if (mDialog == null) {
			View dialogView = LayoutInflater.from(context).inflate(R.layout.alert_dialog, null, false);
			mDialog = AlertDialogFactory.getPopupWindow(context, dialogView);
			Button no = (Button) mDialog.getContentView().findViewById(R.id.no);
			no.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View arg0) {
					// TODO Auto-generated method stub
					if (mDialog.isShowing())
						mDialog.dismiss();
				}
			});
		}

		if (mDialog.isShowing())
			return;

		TextView title = (TextView) mDialog.getContentView().findViewById(R.id.text);
		title.setText(message);

		Button yes = (Button) mDialog.getContentView().findViewById(R.id.yes);
		yes.setOnClickListener(listener);
		mDialog.setAnimationStyle(R.style.Alterdialog_anim_style); //设置动画
		mDialog.showAtLocation(attachView, Gravity.CENTER, 0, 0);

	}

	/**
	 * 对话框的 “是” 按钮监听接口继承此类，使对话框消失
	 * @author mars
	 *
	 */
	public static class DialogPositiveListner implements View.OnClickListener {
		@Override
		public void onClick(View arg0) {
			if (mDialog.isShowing())
				mDialog.dismiss();
		}
	}
}
