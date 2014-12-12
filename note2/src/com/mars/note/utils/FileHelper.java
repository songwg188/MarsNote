package com.mars.note.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.mars.note.api.BackupDoc;

import android.util.Log;

public class FileHelper {
	public static void copyFile(File src, File dest) {
		try {
			int bytesum = 0;
			int byteread = 0;
			if (src.exists()) {
				InputStream inStream = new FileInputStream(src.getPath());
				FileOutputStream fs = new FileOutputStream(dest.getPath());
				byte[] buffer = new byte[1024];
				int length;
				while ((byteread = inStream.read(buffer)) != -1) {
					bytesum += byteread;
					System.out.println(bytesum);
					fs.write(buffer, 0, byteread);
				}
				inStream.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static boolean deleteFile(String path) {
		File f = new File(path);
		if (f.exists()) {
			return f.delete();
		}
		return false;
	}

	public static List<BackupDoc> getBackupDocs(String path) throws Exception {
		List<BackupDoc> list = new ArrayList<BackupDoc>();
		File dir = new File(path);
		if (!dir.exists()) {
			throw new Exception("no file");
		} else if (!dir.isDirectory()) {
			throw new Exception("not a direction");
		} else {
			File[] dirFiles = dir.listFiles();
			for (File f : dirFiles) {
				if (!f.isDirectory() && f.getName().endsWith(".bak")) {
					BackupDoc bd = new BackupDoc();
					bd.path = f.getPath();
					bd.fileName = f.getName();
					list.add(bd);
				}
			}
			return list;
		}
	}
}
