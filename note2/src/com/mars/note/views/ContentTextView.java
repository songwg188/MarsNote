package com.mars.note.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewTreeObserver;
import android.widget.TextView;

/**
 * @author mars
 * @date 2015-2-6 下午2:38:09
 * @version 1.1
 */
public class ContentTextView extends TextView {

	public ContentTextView(Context context) {
		super(context);
	}

	public ContentTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		// TODO Auto-generated method stub
		super.onLayout(changed, left, top, right, bottom);
	}
}
