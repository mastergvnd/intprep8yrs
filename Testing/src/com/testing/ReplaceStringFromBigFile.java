package com.testing;

import java.io.IOException;
import java.io.InputStream;

public class ReplaceStringFromBigFile {

	public static void main(String[] args) throws IOException {
//		String[] cmdArray = {"call", "sed 's/Kumar/replaced/' D:\\Big_File.txt > D:\\Big_File2.txt"};
//		Process runCmd = Runtime.getRuntime().exec(cmdArray);
		
		String[] cmdArray = {"cmd", "/c", "sed 's/Kumar/GKG/g' D:\\Big_File.txt > D:\\Big_File2.txt"};
		Process runCmd = Runtime.getRuntime().exec(cmdArray);
		System.out.println("Completed");
	}

}
