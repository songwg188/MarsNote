package com.mars.note.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.GridView;

public class MyGridView extends GridView {
	public boolean onMeasure = false;//�����жϵĲ���ֵ��ֻ����ɼ��㣬��onLayout���Ϊfalse,��onMeasureʱΪtrue

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
