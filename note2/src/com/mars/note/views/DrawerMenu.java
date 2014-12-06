package com.mars.note.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

public class DrawerMenu extends RelativeLayout{

	public DrawerMenu(Context context) {
		super(context);
	}
	
	public DrawerMenu(Context context,AttributeSet set) {
		super(context, set);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		return true;
	}

}
