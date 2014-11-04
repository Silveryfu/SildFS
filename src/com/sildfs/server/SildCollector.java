package com.sildfs.server;

import java.io.File;
import java.util.TimerTask;

public class SildCollector extends TimerTask {

	private String dir;

	public SildCollector(String dir) {
		this.setDir(dir);
	}

	public void run() {
		long GAP = 15 * 60 * 1000;
		File f = new File(this.getDir() + "/.TXN");
		if (!f.exists())
			return;
		File[] listOfFiles = f.listFiles();
		long currenttime = System.currentTimeMillis();
		for (int i = 0; i < listOfFiles.length; i++) {
			long lastmodified = listOfFiles[i].lastModified();
			if (Math.abs(lastmodified - currenttime) > GAP) {
				deleteDirectory(listOfFiles[i]);
			}
		}
	}

	public void deleteDirectory(File path) {
		try {
			File[] sub = path.listFiles();
			for (File file : sub) {
				if (file.isDirectory()) {
					deleteDirectory(file);
					file.delete();
				} else {
					file.delete();
				}
			}
			path.delete();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getDir() {
		return dir;
	}

	public void setDir(String dir) {
		this.dir = dir;
	}
}
