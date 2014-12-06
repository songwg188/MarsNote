package com.mars.note.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.widget.EditText;

public class NoteContentEditText extends EditText {
	public NoteContentEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		Paint paint = new Paint();
		paint.setStyle(Style.STROKE);
		paint.setColor(Color.WHITE);
		paint.setStrokeWidth(1);
		paint.setAntiAlias(true);
//		android.util.Log.d("edittext", "" + this.getLineCount());
//		android.util.Log.d("edittext", "" + this.getLineHeight());
		for (int i = 0; i < this.getLineCount(); i++) {
			canvas.drawLine(0, (i + 1) * getLineHeight() + 5, this.getWidth(),
					(i + 1) * getLineHeight() + 5, paint);
		}
	}
}
