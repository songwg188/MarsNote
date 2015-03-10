package com.mars.note.utils;

public class Logg {
	public static final String TAG = "mars";

	public static void D(String str) {
		android.util.Log.d(TAG+"_D", str);
	}

	public static void I(String str) {
		android.util.Log.i(TAG+"_I", str);
	}
	
	public static void S(String str) {
		android.util.Log.i(TAG+"_S", str);
	}
	
	public static void T(String str){
		android.util.Log.i(TAG+"_T", str);
	}
	
	public static void E(String str){
		android.util.Log.e(TAG+"_E", str);
	}
}
