package com.mars.note.api;

import android.app.Fragment;
import android.widget.GridView;

import com.mars.note.fragment.BaseFragment;
import com.mars.note.fragment.CalendarFragment;
import com.mars.note.fragment.NoteSettingsFragment;
import com.mars.note.fragment.RecentFragment;
import com.mars.note.fragment.SearchFragment;
import com.mars.note.fragment2.CalendarItemFragment;
import com.mars.note.fragment2.GridViewItemFragment;

/**
 * @author mars
 * @date 2014-12-25 上午10:37:54
 * @version 1.1
 */
public class FragmentFactory {
	public static final int RECENTFRAGMENT = 1;
	public static final int SEARCHFRAGMENT = 0;
	public static final int CALENDARFRAGMENT = 2;

	public static BaseFragment newBaseFragmentInstance(int id) {
		switch (id) {
		case RECENTFRAGMENT:
			return RecentFragment.getInstance();
		case SEARCHFRAGMENT:
			return SearchFragment.getInstance();
		case CALENDARFRAGMENT:
			return CalendarFragment.getInstance();
		default:
			return null;
		}
	}
	
	public static NoteSettingsFragment newSettingsFragment(){
		return new NoteSettingsFragment();
	}
	
	public static com.mars.note.fragment2.BaseFragment newCalendarItem(){
		return new CalendarItemFragment();
	}
	
	public static com.mars.note.fragment2.BaseFragment newGridViewItem(){
		return new GridViewItemFragment();
	}
	
}
