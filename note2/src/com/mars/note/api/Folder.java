package com.mars.note.api;
/**
 * @author mars
 * @date 2015-1-14 下午4:22:30
 * @version 1.1
 */
public class Folder extends BaseFile{
	private String path;
	private String folderName;
	@Override
	public void setPath(String path) {
		// TODO Auto-generated method stub
		this.path = path;
	}
	@Override
	public String getPath() {
		// TODO Auto-generated method stub
		return path;
	}
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return folderName;
	}
	@Override
	public void setName(String name) {
		// TODO Auto-generated method stub
		folderName = name;
	}
}
 