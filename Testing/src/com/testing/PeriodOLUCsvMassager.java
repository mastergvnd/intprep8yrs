package com.testing;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

public class PeriodOLUCsvMassager {
	public static void main(String[] args) throws IOException {
		String filePath = "C:\\Users\\govgupta.ORADEV\\Desktop\\Period.csv";
		File file = new File(filePath);
		List<String> lines = FileUtils.readLines(file);
		List<String> updatedLines = new ArrayList<String>();
		
		int startPeriodIndex = 0;
		int endPeriodIndex = 0;
		
		System.out.println(startPeriodIndex + " " + endPeriodIndex);
		for(int i = 0; i < lines.size() ; i++) {
			String line = lines.get(i);
			
			if(line.contains(" Start Period") && line.contains(" End Period") && line.contains(" DTS Generation")) {
				startPeriodIndex = findIndex(line, "Start Period");
				endPeriodIndex = findIndex(line, "End Period");
				System.out.println(startPeriodIndex + " " + endPeriodIndex);
			}
			
			if(line.startsWith("YearTotal")) {
				line = line.replaceFirst("YearTotal", "HSP_YearTotal");
				if(line.contains("year")) {
                    line = line.replaceFirst("year,", "alternate,");
                }
			}
			else if (line.startsWith("BegBalance")){
				line = line.replaceFirst("BegBalance", "HSP_BegBalance");
				if(line.contains("base")) {
                    line = line.replaceFirst("base,", "alternate,");
                }
			} else if(line.contains("YearTotal")) {
				String tokens[] = line.split(",");
				if(tokens.length > 2 && tokens[1] != null) {
					if("YearTotal".equalsIgnoreCase(tokens[1])) {
						line = line.replaceFirst("YearTotal", "HSP_YearTotal");
					}
				}
			}
			
			if (line.contains("base,")){
				line = line.replaceFirst("base,", "alternate,");
			}
			else if (line.contains("rollup,")){
					line = line.replaceFirst("rollup,", "alternate,");
					String startPeriod = line.split(",")[startPeriodIndex];
					String endPeriod = line.split(",")[endPeriodIndex];
					line = line.replaceFirst(startPeriod, "");
					line = line.replaceFirst(endPeriod, "");
			}
			
			System.out.println(line);
			updatedLines.add(line);
		}
		//System.out.println(updatedLines);
		FileUtils.writeLines(new File("C:\\Users\\govgupta.ORADEV\\Desktop\\PeriodUpt.csv"), updatedLines);
		System.out.println("Completed");
	}
	
	public static int findIndex(String line, String text) {
		int index = 0;
		for(String token : line.split(",")) {
			if(token.trim().equals(text))
				break;
			index++;
		}
		return index;
	}
}