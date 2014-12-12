package com.mars.note.fragment;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import com.mars.note.Editor;
import com.mars.note.Main;
import com.mars.note.R;
import com.mars.note.api.FragmentCallBack;
import com.mars.note.database.NoteDataBaseManager;
import com.mars.note.database.NoteRecord;
import com.mars.note.views.NoteCalendar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

/**
 * 代表每个月份的Fragment
 * @author mars
 *
 */
public class CalendarItem extends Fragment implements
		NoteCalendar.ChangeDateListener {
	private static final String TAG = "CalendarItem";
	private Activity mActivity;
	NoteDataBaseManager noteDBManager;
	int pos;
	NoteCalendar calendarView;
	int calendar_year;
	int calendar_month;
	int calendar_selected_year;
	int calendar_selected_month;
	int calendar_selected_day;
	boolean sync;
	CallBack mCallBack;

	public void setCalendarCallBack(CallBack cb) {
		mCallBack = cb;
	}

	//do not put data here
	//不要用构造方法传递参数 ，用getArguments，见onCreate
	public CalendarItem() {
	}

	@Override
	public void onAttach(Activity mActivity) {
		super.onAttach(mActivity);
		this.mActivity = mActivity;
	}
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle bundle = this.getArguments();
		this.pos = bundle.getInt("pos");
		calendar_year = 1900 + (pos / 12);
		calendar_month = pos % 12 + 1;
		calendar_selected_year = bundle.getInt("calendar_selected_year");
		calendar_selected_month = bundle.getInt("calendar_selected_month");
		calendar_selected_day = bundle.getInt("calendar_selected_day");
		sync = bundle.getBoolean("sync");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_calendar_item, container,
				false);
		calendarView = (NoteCalendar) v.findViewById(R.id.calendar);
		calendarView.setSelectedDay(calendar_selected_year,
				calendar_selected_month, calendar_selected_day);
		calendarView.setYearAndMonth(calendar_year, calendar_month);
		calendarView.setOnChangeDateListener(this); //View的生命周期是随Fragment的
		calendarView.setTouchable(sync);
		return v;
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onChangeSelectedDate(int year, int month, int day) {
		mCallBack.onChangeSelectedDate(year, month, day);
	}

	public interface CallBack {
		abstract void onChangeSelectedDate(int year, int month, int day);
	}
}
