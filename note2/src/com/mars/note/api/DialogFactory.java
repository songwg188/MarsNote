package com.mars.note.api;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.PopupWindow;

/**
 * @author mars
 * @date 2015-2-6 上午10:55:00
 * @version 1.1
 */
public abstract class DialogFactory {
	public static PopupWindow getPopupWindow(Context context, View v) {
		PopupWindow dialog = new PopupWindow(v, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, false);
		dialog.setOutsideTouchable(true);
		dialog.setFocusable(true);
//		dialog.setBackgroundDrawable(new BitmapDrawable()); 外部是否可点击
		dialog.update();
		return dialog;
	}
	
//	public abstract void showDialog();
//	
//	public abstract void dismissDialog();
}
 