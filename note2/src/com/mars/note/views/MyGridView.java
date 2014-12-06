package com.mars.note.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.GridView;

public class MyGridView extends GridView {
	public boolean onMeasure = false;//用于判断的布尔值，只有完成计算，即onLayout后才为false,在onMeasure时为true

	public MyGridView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		onMeasure = true;
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		onMeasure = false;
		super.onLayout(changed, l, t, r, b);
	}
}
