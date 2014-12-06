/** 此类新增于20141124 为了实现listview的阻尼效果，并用反射去除边缘阴影* */

package com.mars.note.views;

import java.lang.reflect.Field;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;import android.widget.AbsListView;
import android.widget.ListView;/* * Author Mars * Date 20141124 * Description  * 			   去除阴影 * 			   实现带阻尼回弹效果的ListView，利用反射机制设置边缘阴影为透明 * 			   开启边缘回弹效果需要在XML里配置BounceListView的属性 android:overScrollMode="always" */

public class BounceListView extends ListView {
	private static final int MAX_Y_OVERSCROLL_DISTANCE = 100;	private boolean isScrollable = true;	private boolean isTouchable = true;	float preX = 0;

	private Context mContext;
	private int mMaxYOverscrollDistance;

	public BounceListView(Context context) {
		super(context);
		mContext = context;
		initBounceListView();
	}

	public BounceListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		initBounceListView();
	}

	public BounceListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
		initBounceListView();
	}

	private void initBounceListView() {
		// get the density of the screen and do some maths with it on the max
		// overscroll distance
		// variable so that you get similar behaviors no matter what the screen
		// size

		final DisplayMetrics metrics = mContext.getResources()
				.getDisplayMetrics();
		final float density = metrics.density;
		mMaxYOverscrollDistance = (int) (density * MAX_Y_OVERSCROLL_DISTANCE);

		// this.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
		//反射机制只能改变公开类的私有变量和方法
		try {
			Class<?> c = (Class<?>) Class.forName(AbsListView.class.getName());
			Field egtField = c.getDeclaredField("mEdgeGlowTop");
			Field egbBottom = c.getDeclaredField("mEdgeGlowBottom");
			egtField.setAccessible(true);
			egbBottom.setAccessible(true);
			Object egtObject = egtField.get(this); // this 指的是ListiVew实例
			Object egbObject = egbBottom.get(this);

			// egtObject.getClass() 实际上是一个 EdgeEffect 其中有两个重要属性 mGlow mEdge
			// 并且这两个属性都是Drawable类型
			Class<?> cc = (Class<?>) Class.forName(egtObject.getClass()
					.getName());
			Field mGlow = cc.getDeclaredField("mGlow");
			mGlow.setAccessible(true);
			mGlow.set(egtObject, new ColorDrawable(Color.TRANSPARENT));
			mGlow.set(egbObject, new ColorDrawable(Color.TRANSPARENT));

			Field mEdge = cc.getDeclaredField("mEdge");
			mEdge.setAccessible(true);
			mEdge.set(egtObject, new ColorDrawable(Color.TRANSPARENT));
			mEdge.set(egbObject, new ColorDrawable(Color.TRANSPARENT));
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressLint("NewApi")
	@Override
	protected boolean overScrollBy(int deltaX, int deltaY, int scrollX,
			int scrollY, int scrollRangeX, int scrollRangeY,
			int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent) {
		// This is where the magic happens, we have replaced the incoming
		// maxOverScrollY with our own custom variable mMaxYOverscrollDistance;
		return super.overScrollBy(deltaX, deltaY, scrollX, scrollY,
				scrollRangeX, scrollRangeY, maxOverScrollX,
				mMaxYOverscrollDistance, isTouchEvent);
	}		public boolean isScrollable() {		return isScrollable;	}	public void setScrollable(boolean isScrollable) {		this.isScrollable = isScrollable;	}	public void setTouchable(boolean enable) {		isTouchable = enable;	}		@Override	public void scrollTo(int x, int y) {		if (isScrollable) {			super.scrollTo(x, y);		}	}		@Override	public boolean onTouchEvent(MotionEvent ev) {		if (isTouchable) {			return super.onTouchEvent(ev);		}		return false;	}		@Override	public boolean onInterceptTouchEvent(MotionEvent ev) {		if (isTouchable) {			boolean result = super.onInterceptTouchEvent(ev);			if (ev.getAction() == MotionEvent.ACTION_DOWN) {				preX = ev.getX();				// Log.d("touch","ACTION_DOWN preX = "+preX);			} else if (ev.getAction() == MotionEvent.ACTION_MOVE) {				if (Math.abs(ev.getX() - preX) > 10) {					// Log.d("touch","ev.getX() = "+ev.getX()+",preX = "+preX);					return true;				} else {					preX = ev.getX();				}			}			return result;		}		return false;	}

}