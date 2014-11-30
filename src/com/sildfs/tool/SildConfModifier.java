package com.sildfs.tool;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

/**
 * This class defines a reader which reads primary server configure info from
 * the configuration file primary.txt
 */

public class SildConfModifier {

	public void modify(String file, String new_ip, int new_port) {
		try {
			File f = new File(file);
			FileWriter fw = new FileWriter(f, false);
			PrintWriter pw = new PrintWriter(fw);
			pw.println(new_ip);
			pw.println(Integer.toString(new_port));
			pw.flush();
			fw.flush();
			pw.close();
			fw.close();
		} catch (Exception e) {
			System.out.println("--R-- Unable to modifier the primary.txt");
		}
	}

	public static void main(String[] args) {
		SildConfReader cr = new SildConfReader();
		cr.read("./primary.txt");
	}
}