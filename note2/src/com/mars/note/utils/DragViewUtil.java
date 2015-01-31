package com.mars.note.utils;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;

/**
 * @author mars
 * @date 2015-1-29 上午10:59:53
 * @version 1.1
 */
public class DragViewUtil {

	private LayoutParams windowParams;
	private WindowManager windowManager;
	private int startX, startY;
	private boolean isDragging;

	public DragViewUtil(Context context) {
		windowParams = new WindowManager.LayoutParams();
		windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
	}

	/**
	 * 开始拖动
	 * 
	 * @param context
	 *            上下文
	 * @param primaryView
	 *            原始View
	 * @param dragView
	 *            拖动的View
	 */
	public void startDrag(View primaryView, View dragView) {
		if (!isDragging) {
			isDragging = true;
			startX = primaryView.getLeft();
			startY = primaryView.getTop();
			windowParams.gravity = Gravity.TOP | Gravity.LEFT;
			//窗口透明
			windowParams.format = PixelFormat.RGBA_8888; 
			windowParams.height = primaryView.getHeight();
			windowParams.width = primaryView.getWidth();
			windowParams.x = startX;
			windowParams.y = startY;
			windowParams.alpha = 0.8f;
			windowParams.flags = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | 0x00000010;
			windowManager.addView(dragView, windowParams);
//			Logg.D("startDrag windowParams.x " + windowParams.x + " windowParams.y " + windowParams.y);
		}
	}

	/**
	 * 拖动中
	 * 
	 * @param x
	 * @param y
	 * @param primaryView
	 * @param dragView
	 */
	public void drag(int x, int y, View primaryView, View dragView) {
		windowParams.x = x + startX;
		windowParams.y = y + startY;
//		Logg.D("drag windowParams.x " + windowParams.x + " windowParams.y " + windowParams.y);
		windowManager.updateViewLayout(dragView, windowParams);
	}

	/**
	 * 停止拖动
	 * 
	 * @param dragView
	 */
	public void stopDrag(View dragView) {
		if (dragView != null) {
			windowManager.removeView(dragView);
			Logg.D("stopDrag");
			dragView = null;
			isDragging = false;
		}
	}
}
