package com.mars.note.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

public class PictureHelper {
	private static final boolean DEBUG = false;

	/**
	 * 
	 * @param srcPath
	 *            绝对地址
	 * @param destW
	 *            目标宽度
	 * @param destH
	 *            目标高度
	 * @param compress
	 *            是否压缩
	 * @param size
	 *            目标尺寸
	 * @param context
	 *            上下文
	 * @param padding
	 *            边框长度
	 * @param addEdge
	 *            是否画边框
	 * @return 剪裁后的图片
	 */
	public static Bitmap getCropImage(String srcPath, float destW, float destH, boolean compress, int size, Context context, int padding, boolean addEdge) {
		if (DEBUG) {
			Logg.I("path = " + srcPath);
			Logg.I("destH  = " + destH + " , destW = " + destW);
		}
		BitmapFactory.Options newOpts = new BitmapFactory.Options();
		newOpts.inJustDecodeBounds = true;
		Bitmap bitmap = BitmapFactory.decodeFile(srcPath, newOpts);//
		newOpts.inJustDecodeBounds = false;
		int w = newOpts.outWidth;
		int h = newOpts.outHeight;
		int be = 1;//
		boolean needMatrixScale = false;
		if (w <= h && w >= destW) {
			// 先以原图宽处以目标宽得到倍数去整（舍去小数），得到的bitmap宽度近似于目标宽，但实际大于等于目标宽
			be = (int) (w / destW);
			needMatrixScale = true;
		} else if (w <= h && w < destW) {
			if (h >= destH) {
				be = 1;
			} else {
				be = 1;
			}
		}else if(w > h && h>=destH){
			be = (int) (h / destH);
			needMatrixScale = true;
		}else if(w > h && h<destH){
			if (w >= destW) {
				be = 1;
			} else {
				be = 1;
			}
		}else{
			throw new RuntimeException("other situation,which should never happen");
		}

		if (be <= 0)
			be = 1;
		newOpts.inSampleSize = be;//
		
		//first step
		try {
			// 得到近似尺寸的bitmap
			bitmap = BitmapFactory.decodeFile(srcPath, newOpts);
		} catch (java.lang.OutOfMemoryError e) {
			e.printStackTrace();
			System.gc();
			bitmap = null;
			return null;
		}
		if (bitmap == null) {
			return null;
		}
		int srcH = bitmap.getHeight();
		int srcW = bitmap.getWidth();
		if (DEBUG) {
			Logg.I("srcH 1 = " + bitmap.getHeight() + " , srcW 1 = " + bitmap.getWidth());
		}
		
		//matrix scale step
		if (needMatrixScale) {
			Matrix matrix = new Matrix();
			float scale = 0;
			if(w <= h){
				scale = ((float) destW) / srcW;
			}else{
				scale = ((float) destH) / srcH;
			}
			matrix.postScale(scale, scale);
			bitmap = Bitmap.createBitmap(bitmap, 0, 0, srcW, srcH, matrix, true);
			if (DEBUG) {
				Logg.I("srcH 2 = " + bitmap.getHeight() + " , srcW 2 = " + bitmap.getWidth());
			}
		}

		// crop step
		if (w <= h) {
			//包含w >= destW，w < destW两种情况
			if (bitmap.getHeight() > destH) {
				// 如果图片的高度大于destH，将进一步剪裁
				bitmap = Bitmap.createBitmap(bitmap, 0, (int) ((bitmap.getHeight() - destH) / 2), (int) bitmap.getWidth(), (int) destH);
				if (DEBUG) {
					Logg.I("w <= h srcH 3 = " + bitmap.getHeight() + " , srcW 3 = " + bitmap.getWidth());
				}
			}
		}else if (w > h) {
			//包含h>=destH，h<destH两种情况
			if (bitmap.getWidth() > destW) {
				// 如果图片的宽度大于destW，将进一步剪裁
				bitmap = Bitmap.createBitmap(bitmap, (int) ((bitmap.getWidth() - destW) / 2), 0, (int) destW, (int) bitmap.getHeight());
				if (DEBUG) {
					Logg.I("w > h srcH 4 = " + bitmap.getHeight() + " , srcW 3 = " + bitmap.getWidth());
				}
			}
		}else{
			throw new RuntimeException("crop step : other situation,which should never happen");
		}
		
		// compress step
		if (compress) {
			if (addEdge) {
				Bitmap bm = addEdge(compressImage(bitmap, size), context, padding);
				return bm;
			} else {
				Bitmap bm = compressImage(bitmap, size);
				return bm;
			}
		}
		if (addEdge) {
			Bitmap bm = addEdge(bitmap, context, padding);
			return bm;
		}
		return bitmap;
	}

	public static float[] getCompressedMeasure(String path, float destH, float destW) {
		BitmapFactory.Options newOpts = new BitmapFactory.Options();
		newOpts.inJustDecodeBounds = true; // 只解析尺寸信息，并不生成bitmap实例
		BitmapFactory.decodeFile(path, newOpts);
		int w = newOpts.outWidth;
		int h = newOpts.outHeight;
		if (w == 0 || h == 0)
			return null;
		float HW_scale = h / w; // 原图高度与宽度的比例
		float DS_scale = 1.0f; // 目标与原图的比例
		if (destW <= w) {
			DS_scale = destW / w;
		}
		if (h > 5000 && h > w) {
			DS_scale = destH / h;
		}
		float[] out = { DS_scale * h, DS_scale * w };
		Logg.D("temp out_w = " + out[1] + " , temp out_h = " + out[0]);
		return out;
	}

	/**
	 * 
	 * @param srcPath
	 * @param destH
	 * @param destW
	 * @param compress
	 *            可能导致内存溢出
	 * @param size
	 * @param context
	 * @param padding
	 * @param addEdge
	 *            可能导致内存溢出
	 * @return
	 */
	public static Bitmap getImageFromPath(String srcPath, float destH, float destW, boolean compress, int size, Context context, int padding, boolean addEdge) {
		BitmapFactory.Options newOpts = new BitmapFactory.Options();
		newOpts.inJustDecodeBounds = true; // 只解析尺寸信息，并不生成bitmap实例
		Bitmap bitmap = BitmapFactory.decodeFile(srcPath, newOpts);
		newOpts.inJustDecodeBounds = false;
		int w = newOpts.outWidth;
		int h = newOpts.outHeight;
		Logg.D("h = " + h);
		Logg.D("w = " + w);
		int be = 1;
		if (destW < w) {
			be = (int) (newOpts.outWidth / destW);
		}
		if (h > 5000) // 3000高度以上的图片按高度比例压缩
			be = (int) (newOpts.outHeight / destH);

		if (be <= 0)
			be = 1;
		newOpts.inSampleSize = be;//
		newOpts.inPreferredConfig = Bitmap.Config.ARGB_8888;// 一个像素占据4字节(4b)
		bitmap = BitmapFactory.decodeFile(srcPath, newOpts);
		if (bitmap == null) {
			return null;
		}

		// 将图片缩放至指定尺寸
		int srcH = bitmap.getHeight();
		int srcW = bitmap.getWidth();
		Logg.D("srcH = " + srcH);
		Logg.D("srcW = " + srcW);

		Logg.D("destH = " + destH);
		Logg.D("destW = " + destW);

		// Logg.D("be = " + be);

		if (be >= 1) {
			Matrix matrix = new Matrix();
			// if (w > h) {
			if (h <= 5000 && srcW > destW) {
				float scale = ((float) destW) / srcW;
				matrix.postScale(scale, scale);
				bitmap = Bitmap.createBitmap(bitmap, 0, 0, srcW, srcH, matrix, true);

				Logg.D("matrixH = " + bitmap.getHeight());
				Logg.D("matrixW = " + bitmap.getWidth());
			}

		}

		if (compress) {
			if (addEdge) {
				return addEdge(compressImage(bitmap, size), context, padding);
			} else {
				return compressImage(bitmap, size);
			}
		}
		if (addEdge) {
			return addEdge(bitmap, context, padding);
		}
		System.gc();// ?
		return bitmap;
	}

	public static Bitmap compressImage(Bitmap image, int size) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		image.compress(Bitmap.CompressFormat.JPEG, 100, baos);
		int options = 100;
		if (DEBUG)
			Logg.I("src img size = " + (baos.toByteArray().length / 1024) + "kb");
		while (baos.toByteArray().length / 1024 > size) {
			options -= 10;//
			baos.reset();//
			image.compress(Bitmap.CompressFormat.JPEG, options, baos);
		}
		if (DEBUG)
			Logg.I("dest img size = " + (baos.toByteArray().length / 1024) + "kb");
		ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());//
		Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);//
		return bitmap;
	}

	// get the absolute path from the uri
	@SuppressLint("NewApi")
	public static String getPath(final Context context, final Uri uri) {
		final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
		// DocumentProvider
		if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
			// ExternalStorageProvider
			if (isExternalStorageDocument(uri)) {
				final String docId = DocumentsContract.getDocumentId(uri);
				final String[] split = docId.split(":");
				final String type = split[0];
				if ("primary".equalsIgnoreCase(type)) {
					return Environment.getExternalStorageDirectory() + "/" + split[1];
				}
				// TODO handle non-primary volumes
			}
			// DownloadsProvider
			else if (isDownloadsDocument(uri)) {
				final String id = DocumentsContract.getDocumentId(uri);
				final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
				return getDataColumn(context, contentUri, null, null);
			}
			// MediaProvider
			else if (isMediaDocument(uri)) {
				final String docId = DocumentsContract.getDocumentId(uri);
				final String[] split = docId.split(":");
				final String type = split[0];
				Uri contentUri = null;
				if ("image".equals(type)) {
					contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
				} else if ("video".equals(type)) {
					contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
				} else if ("audio".equals(type)) {
					contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
				}
				final String selection = "_id=?";
				final String[] selectionArgs = new String[] { split[1] };
				return getDataColumn(context, contentUri, selection, selectionArgs);
			}
		}
		// MediaStore (and general)
		else if ("content".equalsIgnoreCase(uri.getScheme())) {
			// Return the remote address
			if (isGooglePhotosUri(uri))
				return uri.getLastPathSegment();
			return getDataColumn(context, uri, null, null);
		}
		// File
		else if ("file".equalsIgnoreCase(uri.getScheme())) {
			return uri.getPath();
		}
		return null;
	}

	public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
		Cursor cursor = null;
		final String column = "_data";
		final String[] projection = { column };
		try {
			cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
			if (cursor != null && cursor.moveToFirst()) {
				final int index = cursor.getColumnIndexOrThrow(column);
				return cursor.getString(index);
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return null;
	}

	public static boolean isExternalStorageDocument(Uri uri) {
		return "com.android.externalstorage.documents".equals(uri.getAuthority());
	}

	public static boolean isDownloadsDocument(Uri uri) {
		return "com.android.providers.downloads.documents".equals(uri.getAuthority());
	}

	public static boolean isMediaDocument(Uri uri) {
		return "com.android.providers.media.documents".equals(uri.getAuthority());
	}

	private static boolean isGooglePhotosUri(Uri uri) {
		return "com.google.android.apps.photos.content".equals(uri.getAuthority());
	}

	// The method is to draw a customed padding from the bitmap
	public static Bitmap addEdge(Bitmap src, Context context, int padding) {
		int w = src.getWidth();
		int h = src.getHeight();
		Bitmap des = Bitmap.createBitmap(w + padding * 2, h + padding * 2, Config.ARGB_8888);
		Canvas canvas = new Canvas(des);
		Paint paint = new Paint();
		paint.setStyle(Style.FILL);
		paint.setColor(Color.WHITE);
		canvas.drawRect(0, 0, w + padding * 2, h + padding * 2, paint);
		paint.setStyle(Style.STROKE);
		paint.setColor(Color.DKGRAY);
		paint.setStrokeWidth(2);
		canvas.drawRect(0, 0, w + padding * 2, h + padding * 2, paint);
		canvas.drawBitmap(src, padding, padding, null);
		canvas.save(Canvas.ALL_SAVE_FLAG);
		canvas.restore();
		return des;
	}

	public static void saveBitmapToPath(Bitmap bm, String path) {
		File f = new File(path);
		try {
			f.createNewFile();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		FileOutputStream fOut = null;
		try {
			fOut = new FileOutputStream(f);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		bm.compress(Bitmap.CompressFormat.PNG, 100, fOut);
		try {
			fOut.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			fOut.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static Bitmap drawableToBitmap(Drawable drawable) {
		BitmapDrawable bd = (BitmapDrawable) drawable;
		Bitmap bm = bd.getBitmap();
		return bm;
	}
	
	public static Bitmap getCropImageFromBitmap(Bitmap bm, float destW, float destH, boolean compress, int size, Context context, int padding, boolean addEdge) {
		if (DEBUG)
			Logg.I("destH  = " + destH + " , destW = " + destW);
		Bitmap bitmap = bm;
		if(bitmap == null)
			throw new NullPointerException("bitmap cant be null");
		int w = bm.getWidth();
		int h = bm.getHeight();
		boolean needMatrixScale = false;
		if (w <= h && w >= destW) {
			// 先以原图宽处以目标宽得到倍数去整（舍去小数），得到的bitmap宽度近似于目标宽，但实际大于等于目标宽
			needMatrixScale = true;
		} else if(w > h && h>=destH){
			needMatrixScale = true;
		}

		int srcH = bitmap.getHeight();
		int srcW = bitmap.getWidth();
		if (DEBUG) {
			Logg.I("srcH 1 = " + bitmap.getHeight() + " , srcW 1 = " + bitmap.getWidth());
		}
		
		//matrix scale step
		if (needMatrixScale) {
			Matrix matrix = new Matrix();
			float scale = 0;
			if(w <= h){
				scale = ((float) destW) / srcW;
			}else{
				scale = ((float) destH) / srcH;
			}
			matrix.postScale(scale, scale);
			bitmap = Bitmap.createBitmap(bitmap, 0, 0, srcW, srcH, matrix, true);
			if (DEBUG) {
				Logg.I("srcH 2 = " + bitmap.getHeight() + " , srcW 2 = " + bitmap.getWidth());
			}
		}

		// crop step
		if (w <= h) {
			//包含w >= destW，w < destW两种情况
			if (bitmap.getHeight() > destH) {
				// 如果图片的高度大于destH，将进一步剪裁
				bitmap = Bitmap.createBitmap(bitmap, 0, (int) ((bitmap.getHeight() - destH) / 2), (int) bitmap.getWidth(), (int) destH);
				if (DEBUG) {
					Logg.I("w <= h srcH 3 = " + bitmap.getHeight() + " , srcW 3 = " + bitmap.getWidth());
				}
			}
		}else if (w > h) {
			//包含h>=destH，h<destH两种情况
			if (bitmap.getWidth() > destW) {
				// 如果图片的宽度大于destW，将进一步剪裁
				bitmap = Bitmap.createBitmap(bitmap, (int) ((bitmap.getWidth() - destW) / 2), 0, (int) destW, (int) bitmap.getHeight());
				if (DEBUG) {
					Logg.I("w > h srcH 4 = " + bitmap.getHeight() + " , srcW 3 = " + bitmap.getWidth());
				}
			}
		}else{
			throw new RuntimeException("crop step : other situation,which should never happen");
		}
		
		// compress step
		if (compress) {
			if (addEdge) {
				return addEdge(compressImage(bitmap, size), context, padding);
			} else {
				return compressImage(bitmap, size);
			}
		}
		if (addEdge) {
			return addEdge(bitmap, context, padding);
		}
		return bitmap;
	}

}
