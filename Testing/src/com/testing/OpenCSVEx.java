package com.testing;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

//import com.opencsv.CSVWriter;

public class OpenCSVEx {

	public static void main(String[] args) throws Exception {
		
		List<String> names = new ArrayList();
		names.add("Govind");
		names.add("Kumar");
		names.add("Gupta");
		names.add("Hi");
		StringBuilder namesStr = new StringBuilder();
	    for(String name : names)
	    {
	        namesStr = namesStr.length() > 0 ? namesStr.append(",").append(name) : namesStr.append(name);
	    }
	    System.out.println(namesStr.toString());
		
//		FileWriter w = new FileWriter("D:\\yourfile.csv", true);
//		
//		CSVWriter writer = new CSVWriter(w, '\t');
//		writer.writeNext(new String[] { "Govind", "Kumar", "Gupta" });
//		writer.flush();
//		
//		
//		CSVWriter writer2 = new CSVWriter(w, '\t');
//		writer.writeNext(new String[] { "Saurabh", "Kumar", "Gupta" });
//		writer2.flush();
//		w.close();
		System.out.println("Compelted");
	}

}
