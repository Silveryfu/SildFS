package com.sildfs.transaction;

import java.io.Serializable;

public class SildData implements Serializable {
	private static final long serialVersionUID = -3521831796756187209L;
	
	private String data;

	public SildData(String data) {
		this.setData(data);
	} 
	
	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}
}
