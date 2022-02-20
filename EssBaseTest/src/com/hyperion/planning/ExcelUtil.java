package com.hyperion.planning;

import java.awt.Color;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.essbase.api.base.EssException;

public class ExcelUtil {
	public static String NEW_LINE = System.lineSeparator();
	public static String higHlightRed = "RED";
	public static String higHlightGreen = "GREEN";
	public static String higHlightBLACK = "BLACK";
	private static XSSFColor WHITE = new XSSFColor(new Color(255, 255, 255));
	private static XSSFColor BLACK = new XSSFColor(new Color(0, 0, 0));
	private static XSSFColor GREEN = new XSSFColor(new Color(0, 176, 80));
	private static XSSFColor RED = new XSSFColor(new Color(255, 0, 0));
	private static XSSFColor YELLOW = new XSSFColor(new Color(255, 230, 153));
	private static XSSFColor BLUE = new XSSFColor(new Color(155, 194, 230));
	private static String filePath = null;
	private static XSSFWorkbook workbook = null;
	private static XSSFSheet summarySheet = null;
	private static XSSFSheet comparisonSheet = null;

	public static void setFilePath(String filePath) {
		ExcelUtil.filePath = filePath;
	}
	
	public static void init() {
		workbook = new XSSFWorkbook();
		summarySheet = workbook.createSheet("Summary");
		comparisonSheet = workbook.createSheet("Comparison");
		comparisonSheet.setDisplayGridlines(false);
		XSSFFont f = workbook.createFont();
		f.setFontHeightInPoints((short) 4);
	}
	
	public static void closeWorkbook() throws IOException{
		FileOutputStream outputStream = new FileOutputStream(filePath);
        workbook.write(outputStream);
		workbook.close();
	}

	public static void write(String name, int rowNumber, int colNumber) {
		XSSFRow row = comparisonSheet.getRow(rowNumber);
		if(row == null)
			row = comparisonSheet.createRow(rowNumber);
		XSSFCell cell = row.getCell(colNumber);
		if(cell == null)
			cell =  row.createCell(colNumber);
		cell.setCellValue(name);
	}

	public static void format(String name1, String name2) {
		if(name1 != null && name2!= null)
			formatBothNames(name1, name2);
		else if(name1 != null && name2 == null)
			formatLeftName(name1, WHITE, RED);
		else if(name1 == null && name2 != null)
			formatRightName(name2, WHITE, GREEN);
	}

	private static void formatBothNames(String name1, String name2) {
		if(!name1.equals(name2)){
			formatLeftName(name1, YELLOW, RED);
			formatRightName(name2, YELLOW, GREEN);
			hightlightRow();
		}else{
			formatLeftName(name1, WHITE, BLACK);
			formatRightName(name2, WHITE, BLACK);
		}
	}

	private static void formatLeftName(String name1, XSSFColor bgColor, XSSFColor fontColor) {
		XSSFCellStyle cellStyle = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setFontName("Calibri");
        font.setFontHeightInPoints((short) 9);
        font.setColor(fontColor);
        cellStyle.setFont(font);
        cellStyle.setFillForegroundColor(bgColor);
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        comparisonSheet.getRow(TreeComparator.leftRowNumber-1).setHeightInPoints((short) 12);
        comparisonSheet.getRow(TreeComparator.leftRowNumber-1).getCell(TreeComparator.leftcolNumber).setCellStyle(cellStyle);
	}

	private static void formatRightName(String name2, XSSFColor bgColor, XSSFColor fontColor) {
		XSSFCellStyle cellStyle = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setFontName("Calibri");
        font.setFontHeightInPoints((short) 9);
        font.setColor(fontColor);
        cellStyle.setFont(font);
        cellStyle.setFillForegroundColor(bgColor);
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        comparisonSheet.getRow(TreeComparator.leftRowNumber-1).setHeightInPoints((short) 12);
        comparisonSheet.getRow(TreeComparator.rightRowNumber-1).getCell(TreeComparator.rightcolNumber).setCellStyle(cellStyle);
	}
	
	private static void hightlightRow() {
		int from = 0;
		int to = 2*(TreeComparator.rightcolNumber+TreeComparator.leftcolNumber);
		
        while(from < to){
        	XSSFRow row = comparisonSheet.getRow(TreeComparator.leftRowNumber-1);
        	row.setHeightInPoints((short) 12);
    		XSSFCell cell = row.getCell(from);
    		if(cell == null)
    			cell =  row.createCell(from);
    		
    		if(cell.getStringCellValue() == null || cell.getStringCellValue().equalsIgnoreCase("")){
    			XSSFCellStyle cellStyle = workbook.createCellStyle();
    	        cellStyle.setFillForegroundColor(YELLOW);
    	        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
	    		cell.setCellStyle(cellStyle);
    		}
    		from++;
		}
	}
	
	public static void writeBasicSummary(List<Object> list) throws EssException {
		for(int i = 0; i<list.size(); i+=2){
			XSSFCellStyle style = getStyle(true, true, true, true);
			makeHeading(style, BLUE);
			write(list.get(i).toString(), TreeComparator.leftRowNumber, TreeComparator.leftcolNumber++, style);
			style = getStyle(true, true, true, true);
			write(list.get(i+1).toString(), TreeComparator.leftRowNumber++, TreeComparator.leftcolNumber--, style);
		}
		TreeComparator.leftRowNumber++;
	}

	private static void write(String text, int rowNumber, int colNumber, XSSFCellStyle style) {
		XSSFRow row = summarySheet.getRow(rowNumber);
		if(row == null)
			row = summarySheet.createRow(rowNumber);
		XSSFCell cell = row.getCell(colNumber);
		if(cell == null)
			cell =  row.createCell(colNumber);
		cell.setCellValue(text);
		cell.setCellStyle(style);
		summarySheet.autoSizeColumn(colNumber);
	}

	private static XSSFCellStyle getStyle(boolean top, boolean right, boolean bottom, boolean left) {
		XSSFCellStyle cellStyle = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setFontName("Calibri");
        font.setFontHeightInPoints((short) 9);
        cellStyle.setFont(font);
        if(top)
        	cellStyle.setBorderTop(BorderStyle.THIN);
        if(right)
        	cellStyle.setBorderRight(BorderStyle.THIN);
        if(bottom)
        	cellStyle.setBorderBottom(BorderStyle.THIN);
        if(left)
        	cellStyle.setBorderLeft(BorderStyle.THIN);
        
		return cellStyle;
	}

	public static void writeVennDiagram(List<String> sourceList, List<String> commonList, List<String> targetList) {
		writeVennDiagramHeader();
		int size = findMax(sourceList.size(), commonList.size(), targetList.size());
		for(int i=0; i<size; i++){
			XSSFCellStyle style = getStyle(false, true, false, false);
			String source = null;
			String common = null;
			String target = null;
			if(i<sourceList.size())
				source = sourceList.get(i);
			if(i<commonList.size())
				common = commonList.get(i);
			if(i<targetList.size())
				target = targetList.get(i);
			write(source, TreeComparator.leftRowNumber, TreeComparator.leftcolNumber++, style);
			write(common, TreeComparator.leftRowNumber, TreeComparator.leftcolNumber++, style);
			write(target, TreeComparator.leftRowNumber, TreeComparator.leftcolNumber++, style);
		
			TreeComparator.leftRowNumber++;;
			TreeComparator.leftcolNumber = 0;
		}
		formatLastRow();
	}

	private static void formatLastRow() {
		XSSFCellStyle style = getStyle(true, false, false, false);
		write(null, TreeComparator.leftRowNumber, TreeComparator.leftcolNumber++, style);
		write(null, TreeComparator.leftRowNumber, TreeComparator.leftcolNumber++, style);
		write(null, TreeComparator.leftRowNumber, TreeComparator.leftcolNumber++, style);
	}

	private static void writeVennDiagramHeader() {
		XSSFCellStyle style = getStyle(true, true, true, true);
		makeHeading(style, BLUE);
		write("Dimensions only in Source", TreeComparator.leftRowNumber, TreeComparator.leftcolNumber++, style);
		write("Common Dimensions", TreeComparator.leftRowNumber, TreeComparator.leftcolNumber++, style);
		write("Dimensions only in Target", TreeComparator.leftRowNumber, TreeComparator.leftcolNumber++, style);
		TreeComparator.leftRowNumber++;
		TreeComparator.leftcolNumber = 0;
	}
	
	private static void makeHeading(XSSFCellStyle style, XSSFColor color) {
		setBold(style);
		setBackgroundColor(style, color);
	}

	private static void setBackgroundColor(XSSFCellStyle style, XSSFColor color) {
		style.setFillForegroundColor(color);
		style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
	}

	private static void setBold(XSSFCellStyle style){
		Font font = style.getFont();
		font.setBold(true);
		style.setFont(font);
	}
	
	private static int findMax(int a, int b, int c){
		return (a > b ? (a > c ? a : c) : (b > c ? b : c));
	}

	public static void writePropertyDetails() {
		List<String> dPropertiesList = EssbaseUtil.getdPropertiesList();
		List<String> mPropertiesList = EssbaseUtil.getmPropertiesList();
	}
}
