package com.testing;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class Test {
	public static void main(String[] args) throws IOException {
		
		DateFormat df = new SimpleDateFormat("yy"); // Just the year, with 2 digits
		String formattedDate = df.format(Calendar.getInstance().getTime());
		System.out.println("Date is :  "+ formattedDate);
		
		
		Path oluCsvFilePath = Paths.get(System.getProperty("java.io.tmpdir"), "CUBE_NAME", "newFile.csv");
		StringBuilder d = new StringBuilder();
		System.out.println(oluCsvFilePath.toString());
		File f = new File(oluCsvFilePath.toString());
		System.out.println(f.getParentFile()); 
		f.getParentFile().mkdirs(); 
		BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(f));
		String a[] = {"govind"};
		List<String> l = new ArrayList<String>(Arrays.asList(a));
		System.out.println(l.size());
		l.remove("govind");
		System.out.println(l.size());
		System.out.println("Starting");
		System.out.println(1&4);
		System.out.println(2&4);
		System.out.println(3&4);
		System.out.println(4&4);
		System.out.println(5&4);
		System.out.println(6&4);
		System.out.println(7&4);
		System.out.println(8&4);
		System.out.println(9&4);
		System.out.println(10&4);
		System.out.println("GOVIND" + (1&15));
		
		String array[] = { "Govind", "Gupta" };
		List<String> allAccessList = Arrays.asList(array);
		System.out.println(allAccessList);
		allAccessList.add("Kumar");
		System.out.println(allAccessList);
		String ftpUrl = "ftp://%s:%s@%s/%s;type=d";
		String host = "www.den02ahj.us.oracle.com";
		String user = "govgupta";
		String pass = "Rade@378";
		String dirPath = null;

		ftpUrl = String.format(ftpUrl, user, pass, host, dirPath);
		System.out.println("URL: " + ftpUrl);

		try {
			URL url = new URL(ftpUrl);
			URLConnection conn = url.openConnection();
			InputStream inputStream = conn.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

			String line = null;
			System.out.println("--- START ---");
			while ((line = reader.readLine()) != null) {
				System.out.println(line);
			}
		} catch (Exception e) {
			System.out.println("Exception : " + e.getMessage());
		}

	}
}
