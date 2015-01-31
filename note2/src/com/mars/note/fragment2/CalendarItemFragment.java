package com.mars.note.fragment2;

import com.mars.note.R;
import com.mars.note.database.NoteDataBaseManager;
import com.mars.note.views.NoteCalendar;
import android.app.Activity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * 代表每个月份的Fragment
 * @author mars
 *
 */
public class CalendarItemFragment extends BaseFragment implements
		NoteCalendar.ChangeDateListener {
	private static final String TAG = "CalendarItem";
	private Activity mActivity;
	private NoteDataBaseManager noteDBManager;
	private int pos;
	private NoteCalendar calendarView;
	private int calendar_year;
	private int calendar_month;
	private int calendar_selected_year;
	private int calendar_selected_month;
	private int calendar_selected_day;
	private boolean sync;
	private CallBack mCallBack;

	//do not put data here
	//不要用构造方法传递参数 ，用getArguments，见onCreate
	public CalendarItemFragment() {
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

	public interface CallBack extends com.mars.note.fragment2.BaseFragment.CallBack{
		abstract void onChangeSelectedDate(int year, int month, int day);
	}

	@Override
	public void setCallBack(com.mars.note.fragment2.BaseFragment.CallBack cb) {
		mCallBack = (CallBack) cb;
	}
}
