package com.mars.note;

public class Logg {
	public static final String TAG = "mars";

	public static void D(String str) {
		android.util.Log.d(TAG, str);
	}

	public static void I(String str) {
		android.util.Log.i(TAG, str);
	}
}
