package com.mars.note.api;

public class BackupDoc extends BaseFile{
	private String path;
	private String fileName;
	@Override
	public void setPath(String path) {
		this.path = path;
	}
	@Override
	public void setName(String name) {
		fileName = name;
	}
	@Override
	public String getPath() {
		// TODO Auto-generated method stub
		return path;
	}
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return fileName;
	}
}
