package com.mars.note.views;

import com.mars.note.R;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.EditText;

public class EditTextWithDel extends EditText {
	Drawable enable,unable;
	
	
	public EditTextWithDel(Context context) {
		super(context);
		init();
	}

	public EditTextWithDel(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init(){
		enable = this.getResources().getDrawable(R.drawable.edittext_del_enable);
		unable = this.getResources().getDrawable(R.drawable.edittext_del_unable);
		addTextChangedListener(new TextWatcher() {  
            @Override  
            public void onTextChanged(CharSequence s, int start, int before, int count) {}  
            @Override  
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}  
            @Override  
            public void afterTextChanged(Editable s) {  
                setDrawable();  
            }  
        });  
        setDrawable();  
	}
	//…Ë÷√…æ≥˝Õº∆¨  
    private void setDrawable() {  
        if(length() < 1)          	{
            setCompoundDrawablesWithIntrinsicBounds(null, null, unable, null);
//        	unable.setBounds(0, 0, 10, 0);
        }
        else{  
            setCompoundDrawablesWithIntrinsicBounds(null, null, enable, null); 
//            enable.setBounds(0, 0, 10, 0);
        }
    }
    
    @Override  
    public boolean onTouchEvent(MotionEvent event) {
        if (enable != null && event.getAction() == MotionEvent.ACTION_UP) {  
            int eventX = (int) event.getRawX();  
            int eventY = (int) event.getRawY();  
//            Log.e(TAG, "eventX = " + eventX + "; eventY = " + eventY);  
            Rect rect = new Rect();  
            getGlobalVisibleRect(rect);  
            
            rect.left = rect.right - enable.getIntrinsicWidth()-30;  
            if(rect.contains(eventX, eventY))   
                setText("");  
        }  
        return super.onTouchEvent(event);  
    }  
	
}
