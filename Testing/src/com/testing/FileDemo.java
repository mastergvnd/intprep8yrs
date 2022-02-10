package com.testing;

import java.io.File;
import java.io.FileOutputStream;

public class FileDemo {

	public static void main(String[] args) {
		File f = null;
		String[] strs = { "D:\\test1.txt", "D:\\test2.txt" };
		try {
			// for each string in string array
			for (String s : strs) {
				f = new File(s);
				FileOutputStream fos = new FileOutputStream(f);
				fos.close();
			}
		} catch (Exception e) {
			// if any I/O error occurs
			e.printStackTrace();
		}
	}

}
