package com.mars.note.fragment;

import com.mars.note.R;
import com.mars.note.api.FragmentCallBack;

import android.app.Activity;
import android.support.v4.app.Fragment;

/**
 * @author mars
 * @date 2014-12-25 上午10:42:01
 * @version 1.1
 */
public class BaseFragment extends Fragment{
	//回调者
	public FragmentCallBack mCallBack;
	
	@Override
	public void onAttach(Activity mActivity) {
		super.onAttach(mActivity);
		setCallBack((FragmentCallBack) mActivity);
	}
	
	/**
	 * 回调创建者
	 * @param mCallBack
	 */
	public void setCallBack(FragmentCallBack mCallBack) {
		this.mCallBack = mCallBack;
	}
	
}
 