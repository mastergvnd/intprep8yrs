package com.hyperion.planning;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class NotepadWriter {
	
	private BufferedWriter output = null;
	public NotepadWriter(String file){
        try {
            output = new BufferedWriter(new FileWriter(file));
        } catch ( IOException e ) {
            e.printStackTrace();
        }
	}
	public void write(String text){
		try {
			output.write(text + System.lineSeparator());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void closeWriter(){
		try {
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
