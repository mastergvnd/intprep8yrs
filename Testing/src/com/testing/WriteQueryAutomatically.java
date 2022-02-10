package com.testing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class WriteQueryAutomatically {

	public static void main(String[] args) throws IOException {
		
		String filePath = "C:\\Users\\govgupta.ORADEV\\Downloads\\QuerieData.csv";
		
		File f = new File(filePath);
		
		FileReader fr = new FileReader(f);
		
		BufferedReader br = new BufferedReader(fr);
		
		

		//BufferedReader br = new BufferedReader(new FileReader(new File(filePath)));

		String line;
		while ((line = br.readLine()) != null) {
			//String token[] = line.split(",");
			//System.out.println("Insert into Table values (\'" + token[0] + "', '" + token[1] + "', '" + token[2] + "');");
			System.out.println(line);
		}

	}

}
