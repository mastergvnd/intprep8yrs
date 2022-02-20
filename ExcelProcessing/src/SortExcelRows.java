import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class SortExcelRows {

	private static Sheet vcSubRuleWorksheet = null;
	private static Workbook workbook = null;
	private static LinkedHashMap<String, ArrayList<Integer>> subRulesOriginalPosition = new LinkedHashMap<String, ArrayList<Integer>>();
	private static HashMap<String, ArrayList<Integer>> subRulesIndexPosition = new HashMap<String, ArrayList<Integer>>();
	

	private static XSSFWorkbook destWorkbook = new XSSFWorkbook();

	private static XSSFSheet destSheet = destWorkbook.createSheet("Copied");

	public static void main(String[] args) throws FileNotFoundException {
		long startTime = System.currentTimeMillis();
		//String filePath = "C:\\Users\\govgupta.ORADEV\\Desktop\\Bugs and Enh\\VC CLS Excel\\SubrulesSort.xlsx";
		String filePath = "C:\\Users\\govgupta.ORADEV\\Desktop\\Bugs and Enh\\VC CLS Excel\\Sample Excels\\1000RulesUp.xlsx";
		File definitionFile = new File(filePath);
		System.out.println("Creating fis");
		FileInputStream fis = new FileInputStream(definitionFile);
		System.out.println("Created fis");
		try {
			doParse(fis);
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("Number of rows : " + vcSubRuleWorksheet.getLastRowNum());
		ArrayList<Integer> subRulesPos = null;
		for (int i = 1; i <= vcSubRuleWorksheet.getLastRowNum(); i++) {
			Row row = vcSubRuleWorksheet.getRow(i);
			Cell cell = row.getCell(0);
			if (!subRulesOriginalPosition.containsKey(cell.getStringCellValue())) {
				subRulesPos = new ArrayList<Integer>();
				subRulesPos.add(i + 1);
				subRulesOriginalPosition.put(cell.getStringCellValue(), subRulesPos);
			} else {
				subRulesPos = subRulesOriginalPosition.get(cell.getStringCellValue());
				subRulesPos.add(i + 1);
				subRulesOriginalPosition.put(cell.getStringCellValue(), subRulesPos);
			}
		}
		System.out.println("Positions Size : " + subRulesOriginalPosition.size());
		System.out.println("Positions " +"" /*subRulesActualSheetPosition*/);

		int p = 1;
		for (HashMap.Entry entry : subRulesOriginalPosition.entrySet()) {
			String ruleName = (String) entry.getKey();
			int subRulesCount = ((ArrayList<Integer>) entry.getValue()).size();
			ArrayList<Integer> indexes = new ArrayList<Integer>();
			indexes.add(p);
			indexes.add(0);
			p += subRulesCount;
			subRulesIndexPosition.put(ruleName, indexes);
		}
		System.out.println("Indexes : " + "" /*subRulesIndexPosition*/);
		
		createAndWriteRow(vcSubRuleWorksheet.getRow(0), 0);

		for (int i = 1; i <= vcSubRuleWorksheet.getLastRowNum(); i++) {
			Row row = vcSubRuleWorksheet.getRow(i);
			String ruleName = row.getCell(0).getStringCellValue();
			ArrayList<Integer> position = subRulesIndexPosition.get(ruleName);
			int rowIndex = position.get(0) + position.get(1);
			position.add(1, position.get(1) + 1);
			subRulesIndexPosition.put(ruleName, position);
			createAndWriteRow(row, rowIndex);
		}
		
		try { 
            FileOutputStream out = new FileOutputStream(new File("C:\\Users\\govgupta.ORADEV\\Desktop\\Bugs and Enh\\VC CLS Excel\\Sample Excels\\CopiedEx.xlsx")); 
            destWorkbook.write(out); 
            out.close(); 
            System.out.println("file written successfully on disk."); 
        } catch (Exception e) { 
            e.printStackTrace(); 
        }  
		
		long endTime = System.currentTimeMillis();
		System.out.println("Total time taken : "  + TimeUnit.MILLISECONDS.toSeconds(endTime - startTime));

	}

	private static void createAndWriteRow(Row row, int rowIndex) {
		Row destRow = destSheet.createRow(rowIndex);
		for (int c = 0; c < row.getLastCellNum(); c++) {
			Cell cell = destRow.createCell(c);
			cell.setCellValue(row.getCell(c).getRichStringCellValue());
		}
	}

	private static void doParse(FileInputStream fis) {
		try {
			workbook = new XSSFWorkbook(fis);
			System.out.println("Workbook created");
		} catch (IOException e) {
			e.printStackTrace();
		}
		preParseProcessing();
	}

	private static void preParseProcessing() {
		vcSubRuleWorksheet = workbook.getSheet("Sub Rules");
		System.out.println("sheet initialized");
	}
}
