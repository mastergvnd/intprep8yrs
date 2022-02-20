package com.hyperion.planning;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class Writer {
	public static String NEW_LINE = System.lineSeparator();
	private static BufferedWriter writer = null;
	public static String higHlightRed = "RED";
	public static String higHlightGreen = "GREEN";
	public static String higHlightBLACK = "BLACK";
	private static String filePath = null;

	public static void write(String text) throws IOException{
		writer.append(text+System.lineSeparator());
	}
	public static void write(String text, String separator) throws IOException{
		writer.append(text+separator);
	}
	public static void closeWriter() throws IOException{
		writer.close();
	}
	public static void setFilePath(String filePath) {
		Writer.filePath = filePath;
	}
	public static void init(){
		try {
			writer = new BufferedWriter(new FileWriter(filePath));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}