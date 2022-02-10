package com.testing;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class PrintWriterTest {

	public static void main(String[] args) throws FileNotFoundException {
		File Fileright = new File("C:\\Users\\govgupta.ORADEV\\Desktop\\test.txt");

        PrintWriter pw = new PrintWriter(Fileright);

        for (int i = 0; i <= 3; i++) {
           pw.println(i);
           System.out.println(i);
        }

        pw.close();
	}

}
