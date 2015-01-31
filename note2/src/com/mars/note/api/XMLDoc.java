package com.mars.note.api;
/**
 * @author mars
 * @date 2015-1-20 上午10:43:32
 * @version 1.1
 */
public class XMLDoc extends BaseFile{
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
 