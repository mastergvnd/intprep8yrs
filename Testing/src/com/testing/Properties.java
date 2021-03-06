package com.testing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;

public class Properties {
	private static StringBuffer DIMENSION_STORAGE_CATEGORY_MISMATCH = new StringBuffer();
	private static StringBuffer MEMBER_ORDER_MISMATCH = new StringBuffer();
	private static StringBuffer UDA_MISMATCH = new StringBuffer();
	private static StringBuffer DEFAULT_ALIAS_MISMATCH = new StringBuffer();
	private static StringBuffer FORMULA_MISMATCH = new StringBuffer();
	private static StringBuffer MEMBER_IN_CUBE_1_NOT_IN_CUBE_2 = new StringBuffer();
	private static StringBuffer TIME_BALANCE_MISMATCH = new StringBuffer();
	private static StringBuffer MEMBER_IN_CUBE_2_NOT_IN_CUBE_1 = new StringBuffer();
	private static StringBuffer MEMBER_STORAGE_MISMATCH = new StringBuffer();
	private static StringBuffer SKIP_OPTION_MISMATCH = new StringBuffer();
	
	public static void main(String[] args) throws Exception {
		
		String fileName = "C:\\Users\\govgupta.ORADEV\\Desktop\\OP\\Properties Analysis\\UDA and ALIAS fixes\\Analysis\\otlCompare.log";
		File file = new File(fileName);
		FileReader fr = new FileReader(file);
		BufferedReader br = new BufferedReader(fr);
		String line;
		while((line = br.readLine()) != null){
		    if(line.startsWith("DIMENSION STORAGE CATEGORY MISMATCH")){
		    	DIMENSION_STORAGE_CATEGORY_MISMATCH.append(line).append(System.lineSeparator());
		    } else if(line.startsWith("MEMBER ORDER MISMATCH")){
		    	MEMBER_ORDER_MISMATCH.append(line).append(System.lineSeparator());
		    } else if(line.startsWith("UDA MISMATCH")){
		    	UDA_MISMATCH.append(line).append(System.lineSeparator());
		    } else if(line.startsWith("DEFAULT ALIAS MISMATCH")){
		    	DEFAULT_ALIAS_MISMATCH.append(line).append(System.lineSeparator());
		    } else if(line.startsWith("FORMULA MISMATCH")){
		    	FORMULA_MISMATCH.append(line).append(System.lineSeparator());
		    } else if(line.startsWith("MEMBER IN CUBE 1, NOT IN CUBE 2")){
		    	MEMBER_IN_CUBE_1_NOT_IN_CUBE_2.append(line).append(System.lineSeparator());
		    } else if(line.startsWith("TIME BALANCE MISMATCH")){
		    	TIME_BALANCE_MISMATCH.append(line).append(System.lineSeparator());
		    } else if(line.startsWith("MEMBER IN CUBE 2, NOT IN CUBE 1")){
		    	MEMBER_IN_CUBE_2_NOT_IN_CUBE_1.append(line).append(System.lineSeparator());
		    } else if (line.startsWith("MEMBER STORAGE MISMATCH")){
		    	MEMBER_STORAGE_MISMATCH.append(line).append(System.lineSeparator());
		    } else if(line.startsWith("SKIP OPTION MISMATCH")){
		    	SKIP_OPTION_MISMATCH.append(line).append(System.lineSeparator());
		    }
		    else{
		    	throw new Exception(line);
		    }
		}
		PrintWriter out = new PrintWriter("C:\\Users\\govgupta.ORADEV\\Desktop\\OP\\Properties Analysis\\UDA and ALIAS fixes\\Analysis\\otlCompare2.log");
		out.println(DIMENSION_STORAGE_CATEGORY_MISMATCH);
		out.println(MEMBER_ORDER_MISMATCH);
		out.println(MEMBER_IN_CUBE_1_NOT_IN_CUBE_2);
		out.println(MEMBER_IN_CUBE_2_NOT_IN_CUBE_1);
		out.println(MEMBER_STORAGE_MISMATCH);
		out.println(TIME_BALANCE_MISMATCH);
		out.println(FORMULA_MISMATCH);
		out.println(SKIP_OPTION_MISMATCH);
		out.println(UDA_MISMATCH);
		out.println(DEFAULT_ALIAS_MISMATCH);
		out.close();
		System.out.println("Completed");
	}
}
