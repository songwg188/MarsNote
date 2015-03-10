package com.mars.note.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.mars.note.api.BackupDoc;
import com.mars.note.api.BaseFile;
import com.mars.note.api.Folder;
import com.mars.note.api.XMLDoc;

import android.util.Log;

public class FileHelper {
	public static final String fileSuffix = ".marsnote";
	public static final String XMLSuffix = "_mars_note_text.xml";
	
	public static void copyFile(File src, File dest) {
		long begin = System.currentTimeMillis();
		try {
			int bytesum = 0;
			int byteread = 0;
			if (src.exists()) {
				InputStream inStream = new FileInputStream(src.getPath());
				FileOutputStream fs = new FileOutputStream(dest.getPath());
				byte[] buffer = new byte[2048];
				while ((byteread = inStream.read(buffer)) != -1) {
					bytesum += byteread;
					System.out.println(bytesum);
					fs.write(buffer, 0, byteread);
				}
				fs.flush();
				fs.close();
				inStream.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			Logg.D("copyFile using "+(System.currentTimeMillis()-begin)/1000);
		}
	}

	public static boolean deleteFile(String path) {
		File f = new File(path);
		if (f.exists()) {
			return f.delete();
		}
		return false;
	}
	
	public static List<BaseFile> getXmlDocs(String path) throws Exception {
		List<BaseFile> list = new ArrayList<BaseFile>();
		List<BaseFile> xmlList = new ArrayList<BaseFile>();
		List<BaseFile> folderList = new ArrayList<BaseFile>();
		File dir = new File(path);
		if (!dir.exists()) {
			dir.mkdirs();
			return list;
		} else if (!dir.isDirectory()) {
			dir.mkdirs();
			return list;
		} else {
			File[] dirFiles = dir.listFiles();
			if (dirFiles != null)
				for (File f : dirFiles) {
					if (!f.isDirectory() && f.getName().endsWith(XMLSuffix)) {
						BaseFile bd = new XMLDoc();
						bd.setPath(f.getPath());
						bd.setName(f.getName().substring(0, f.getName().length() - XMLSuffix.length())) ;
						xmlList.add(bd);
					} else if (f.isDirectory()) {
						BaseFile fd = new Folder();
						fd.setPath(f.getPath());
						fd.setName(f.getName());
						folderList.add(fd);
					}
				}
		}
		// 从大到小 ,文件名是创建时间
		Collections.sort(xmlList, new Comparator<BaseFile>() {
			@Override
			public int compare(BaseFile o1, BaseFile o2) {
				return Long.valueOf(o2.getName()).compareTo(Long.valueOf(o1.getName()));
			}

		});

		Collections.sort(folderList, new Comparator<BaseFile>() {
			@Override
			public int compare(BaseFile o1, BaseFile o2) {
				// TODO Auto-generated method stub
				return o1.getName().compareToIgnoreCase(o2.getName());
			}
		});
		if (dir.getParentFile() != null) {
			BaseFile fd = new Folder();
			fd.setPath(dir.getParent());
			fd.setName("...");
			list.add(fd);
		}

		list.addAll(folderList);
		list.addAll(xmlList);
		
//		for(BaseFile bf : list){
//			Log.d("file",bf.getName());
//		}
		return list;
	}
	

	public static List<BaseFile> getBackupDocs(String path) throws Exception {
		List<BaseFile> list = new ArrayList<BaseFile>();
		List<BaseFile> docList = new ArrayList<BaseFile>();
		List<BaseFile> folderList = new ArrayList<BaseFile>();
		File dir = new File(path);
		if (!dir.exists()) {
			dir.mkdirs();
			return list;
		} else if (!dir.isDirectory()) {
			dir.mkdirs();
			return list;
		} else {
			File[] dirFiles = dir.listFiles();
			if (dirFiles != null)
				for (File f : dirFiles) {
					if (!f.isDirectory() && f.getName().endsWith(fileSuffix)) {
						BaseFile bd = new BackupDoc();
						bd.setPath(f.getPath());
						bd.setName(f.getName().substring(9, f.getName().length() - fileSuffix.length())) ;
						docList.add(bd);
					} else if (f.isDirectory()) {
						BaseFile fd = new Folder();
						fd.setPath(f.getPath());
						fd.setName(f.getName());
						folderList.add(fd);
					}
				}
		}
		// 从大到小 ,文件名是创建时间
		Collections.sort(docList, new Comparator<BaseFile>() {
			@Override
			public int compare(BaseFile o1, BaseFile o2) {
				return Long.valueOf(o2.getName()).compareTo(Long.valueOf(o1.getName()));
			}

		});

		Collections.sort(folderList, new Comparator<BaseFile>() {
			@Override
			public int compare(BaseFile o1, BaseFile o2) {
				// TODO Auto-generated method stub
				return o1.getName().compareToIgnoreCase(o2.getName());
			}
		});
		if (dir.getParentFile() != null) {
			BaseFile fd = new Folder();
			fd.setPath(dir.getParent());
			fd.setName("...");
			list.add(fd);
		}

		list.addAll(folderList);
		list.addAll(docList);
		return list;
	}
}
