package com.test;

import java.io.IOException;
import java.util.ArrayList;

import com.essbase.api.base.EssException;
import com.essbase.api.base.IEssIterator;
import com.essbase.api.datasource.EssSecurityFilter;
import com.essbase.api.datasource.IEssCube;
import com.essbase.api.datasource.IEssCube.IEssSecurityFilter.IEssFilterRow;
import com.essbase.api.datasource.IEssOlapUser;
import com.essbase.api.datasource.IEssPerformCustomCalc;
import com.essbase.api.session.EssGlobalStrings;
import com.hyperion.planning.EssbaseConnectionManager;
import com.hyperion.planning.EssbaseCube;
import com.hyperion.planning.ExcelUtil;

public class UnitTesting {

	public static void main(String[] args) throws IOException {
		/*
		 * EssbaseCube sourceCube = new EssbaseCube();
		 * sourceCube.setApplicationName("Sample_U");
		 * sourceCube.setCubeName("Basic");
		 */
		// sourceCube.setApplicationName("SampCopy");
		// sourceCube.setCubeName("East");
		// sourceCube.setApplicationName("ASOsamp");
		// sourceCube.setCubeName("Sample");
		EssbaseCube targetCube = new EssbaseCube();
		 targetCube.setApplicationName("HP1_ASO");
		 targetCube.setCubeName("HP1_ASO");
		// targetCube.setApplicationName("Sample_U");
		// targetCube.setCubeName("Basic");
//		targetCube.setApplicationName("Sample");
//		targetCube.setCubeName("Basic");
//		targetCube.setApplicationName("HP1_ASO");
//		targetCube.setCubeName("HP1_ASO");
		// Writer.setFilePath("D:\\diff.txt");
		// Writer.setFilePath("D:\\diff.xlsx");
		// Writer.init();
		ExcelUtil.setFilePath("D:\\sheets\\diff7.xlsx");
		ExcelUtil.init();

		try {
			/*
			 * EssbaseConnectionManager.connectToCube(sourceCube);
			 * sourceCube.load();
			 */
			EssbaseConnectionManager conn = new EssbaseConnectionManager();
			conn.connectToCube(targetCube);
			targetCube.load();
			
			IEssPerformCustomCalc customCalc = targetCube.getCube().getPerformCustomCalcInstance();

			customCalc.setPOV("{(FY17,jan,Salary)}");
			customCalc.setTarget("(FY17,Jan)");
			customCalc.setDebitMember("[Salary]");
			customCalc.setCreditMember("[Bonus]");
			//customCalc.setOffset("(Period, Years, Scenario, Version, Currency, Entity, Manager, Employee)");
			customCalc.setSourceRegion("crossjoin({[Units Sold],[Discount %]},{(FY16,dec)})");
			customCalc.setScript("([80101],Bonus) := (FY16,dec,[Units Sold]) + (FY16,dec,[Units Sold]);");
			customCalc.setGroupID(0);
			customCalc.setRuleID(0);
			ArrayList errAndWarnMsgsList = new ArrayList();
			System.out.println(customCalc.toString());
			boolean isSuccessful = customCalc.performCustomCalc(true, errAndWarnMsgsList);
			System.out.println(isSuccessful);
			
			
			
/*			System.out.println("Satring...");
			targetCube.getCube().getSecurityFilter("testFilter1").delete();
	    	String [] rowStrings = {"@IDESCENDANTS(Scenario)", "@IALLANCESTORS (Scenario)"};
	    	//IEssCube.IEssSecurityFilter filter = targetCube.getCube().createSecurityFilter("TestFilter1");

	    	System.out.println("\nList after Creating ...");
	    	getFilterList(targetCube.getCube());
	   	
	    	IEssCube.IEssSecurityFilter setFilter = targetCube.getCube().setSecurityFilter("TestFilter1", true, IEssCube.EEssCubeAccess.READ_WRITE_CUBE_DATA);
	    	getFilterList(targetCube.getCube());
	    	for (int i =0; i < rowStrings.length; i++) {
	    		setFilter.setFilterRow(rowStrings[i], (short)EssGlobalStrings.ESS_ACCESS_WRITE);
	    	}
	    	setFilter.setFilterRow("", (short)0);
			System.out.println("\nList after Setting ...");
			getFilterList(targetCube.getCube());
	    	targetCube.getCube().deleteSecurityFilter("testFilter1");*/
			verifyFilterAPI(targetCube.getCube());
//			IEssOlapUser user = targetCube.getOlapServer().getOlapUser("User1");
//			System.out.println(user.getCubeName());
            //IEssOlapUser user1 = targetCube.getOlapServer().createOlapUser("User2", "password");
            //targetCube.getCube().setUserOrGroupAccess(user1.getName(), IEssCube.EEssCubeAccess.FILTER_ACCESS);
            //IEssCube.IEssSecurityFilter filter = targetCube.getCube().createSecurityFilter("TestFilter1");
            //IEssCube.IEssSecurityFilter setFilter = targetCube.getCube().setSecurityFilter("TestFilter3", true, IEssCube.EEssCubeAccess.READ_WRITE_CUBE_DATA);
            //setFilter.setFilterRow("TestFilter3", (short)EssGlobalStrings.ESS_ACCESS_WRITE);
            //getFilterList(targetCube.getCube());
            //targetCube.getOlapServer().getOlapUser(user1.getName()).deleteUser();
            /*IEssCube cube = targetCube.getCube();
			System.out.println("Loading data...");
			String data = "Product Jan Sales Actual Market 7777\n" + "Product Feb Sales Actual Market 8888\n" + "Product Mar Sales Actual Market 9999\n";
			cube.beginUpdate(true, false);
			cube.sendString(data);
			cube.endUpdate();
			System.out.println("Data Loading completed.");*/

			// targetCube.getOlapServer().copyOlapFileObjectToServer("Sample",
			// "Basic", IEssOlapFileObject.TYPE_CALCSCRIPT, "tCalc2",
			// "C:\\Users\\govgupta.ORADEV\\Desktop\\testCalc2.csc", true);
			// targetCube.getOlapServer().deleteOlapFileObject("Sample",
			// "Basic", IEssOlapFileObject.TYPE_CALCSCRIPT, "tCalc2");

			/*
			 * System.out.println(targetCube.getOlapApp().getMinimumCubeAccess()
			 * .intValue());
			 * System.out.println(Arrays.toString(targetCube.getOlapApp().
			 * getMinimumCubeAccess().getPossibleValues()));
			 * System.out.println(targetCube.getOlapApp().getMinimumCubeAccess()
			 * .fromString("Filter access"));
			 * targetCube.getCube().setCubeAccess(EEssCubeAccess.sm_fromInt(arg0
			 * )); System.out.println(targetCube.getCube().getCubeAccess().
			 * stringValue());
			 */

			/*
			 * String str2[][] = targetCube.getCube().beginDataload("testData",
			 * IEssOlapFileObject.TYPE_RULES,
			 * "C:\\Users\\govgupta.ORADEV\\Desktop\\data3 - Copy.txt",
			 * IEssOlapFileObject.TYPE_TEXT, false, 1);
			 */
/*			File temp = File.createTempFile("testEssArchiveFile", ".tmp"); 
			System.out.println(temp.getAbsolutePath());
			String tempDir = System.getProperty("java.io.tmpdir"); 
			System.out.println(tempDir);
			IEssCubeDataloadInstance e = targetCube.getCube().createDataloadInstance();
			e.setRulesFile("C:\\Users\\govgupta.ORADEV\\Desktop\\testRule.rul");
			targetCube.getCube().beginDataload(true, true, false, e.getRulesFile(), IEssOlapFileObject.TYPE_RULES);
			File file = new File("C:\\Users\\govgupta.ORADEV\\Desktop\\data3 - Copy.txt");
			BufferedReader br = new BufferedReader(new FileReader(file));
			StringBuffer st = new StringBuffer();
			String s;
			while ((s = br.readLine()) != null)
				st.append(s).append(System.lineSeparator());
			targetCube.getCube().sendString(st.toString());
			final String[][] dataLoadErrors = targetCube.getCube().endDataload();*/
//			targetCube.getCube().beginDataload("testData", IEssOlapFileObject.TYPE_RULES, "C:\\Users\\govgupta.ORADEV\\Desktop\\data3 - Copy.txt", IEssOlapFileObject.TYPE_TEXT, false, 3);
//			System.out.println("Data load completed.");
			String data = "Name Jan FY15 Budget \"BU Version_1\" USD NY \"John S\" \"Employee 1\" 250\n" +
							"Name Feb FY15 Budget \"BU Version_1\" USD NY \"John S\" \"Employee 2\" 250";
			System.out.println(data);
/*			targetCube.getCube().beginDataload(true, false, false, "testASO52", IEssOlapFileObject.TYPE_RULES);
			targetCube.getCube().sendString(data);
			String[][] dataLoadErrors = targetCube.getCube().endDataload();*/
			//String a[][] = targetCube.getCube().loadData(IEssOlapFileObject.TYPE_RULES, null, IEssOlapFileObject.TYPE_TEXT, "C:\\Users\\govgupta.ORADEV\\Desktop\\data4Copy.txt", false);
//			String[][] a = targetCube.getCube().beginDataload(null, IEssOlapFileObject.TYPE_RULES, "C:\\Users\\govgupta.ORADEV\\Desktop\\data4Copy.txt", IEssOlapFileObject.TYPE_TEXT, false, 5);
//			IEssCubeDataloadInstance d =  targetCube.getCube().createDataloadInstance();
//			d.setDataFile("C:\\Users\\govgupta.ORADEV\\Desktop\\data3Copy.txt");
//			d.setDataFileType(IEssOlapFileObject.TYPE_TEXT);
//			d.setAbortOnError(false);
//			String[][] a = targetCube.getCube().loadDataParallel(d);
			//"C:\\Users\\govgupta.ORADEV\\Desktop\\data3 - Copy.txt"
//			targetCube.getCube().loadBufferInit(2, IEssCube.ESS_ASO_DATA_LOAD_BUFFER_DUPLICATES_ADD,IEssCube.ESS_ASO_DATA_LOAD_BUFFER_IGNORE_MISSING_VALUES, 100);
			//final String[][] dataLoadErrors = targetCube.getCube().beginDataload(null, IEssOlapFileObject.TYPE_RULES, "C:\\Users\\govgupta.ORADEV\\Desktop\\ASO_Data4.txt", IEssOlapFileObject.TYPE_TEXT, false, 2);
//			targetCube.getCube().loadBufferTerm(new long[] { 2 }, IEssCube.ESS_ASO_DATA_LOAD_BUFFER_STORE_DATA, IEssCube.ESS_ASO_DATA_LOAD_BUFFER_COMMIT, IEssCube.ESS_ASO_DATA_LOAD_INCR_TO_MAIN_SLICE);
			// targetCube.getOlapServer().copyOlapFileObjectToServer("SampCopy",
			// "Basic", IEssOlapFileObject.TYPE_TEXT, "data3",
			// "C:\\Users\\govgupta.ORADEV\\Desktop\\data3.txt", true);
			// targetCube.getOlapServer().copyOlapFileObjectFromServer("SampCopy",
			// "Basic", IEssOlapFileObject.TYPE_TEXT, "data3",
			// "C:\\Users\\govgupta.ORADEV\\Desktop\\act3.txt", false);
			
			System.out.println("File uploaded");
			// EssbaseUtil.loadCache(targetCube, new Outline());

			// EssbaseUtil.cubeComparator(sourceCube, targetCube);
			// TreeComparator.writeSummary(sourceCube, targetCube);
			// EssbaseTreeComparator.printComparison(sourceCube, targetCube);
		} catch (EssException e) {
			System.out.println("Error : " + e.getMessage());
			e.printStackTrace();
		} /*
			 * catch (IOException e) {
			 * System.out.println("Error : "+e.getMessage());
			 * e.printStackTrace(); }
			 */
		finally {
			// EssbaseConnectionManager.comeOutClean(sourceCube);
			EssbaseConnectionManager.comeOutClean(targetCube);
			// Writer.closeWriter();
			ExcelUtil.closeWorkbook();
		}
		/*
		 * Desktop dt = Desktop.getDesktop(); dt.open(new
		 * File("D:\\sheets\\diff7.xlsx"));
		 */
		System.out.println("done");
	}

	public static void getFilterList(IEssCube cube) throws EssException {
		IEssIterator iterator = cube.getSecurityFilters();
		System.out.println(iterator.getCount());
		for (int i = 0; i < iterator.getCount(); i++) {
			IEssCube.IEssSecurityFilter filter = (IEssCube.IEssSecurityFilter) iterator.getAt(i);
			IEssCube.IEssSecurityFilter.IEssFilterRow row = filter.getFilterRow();
			while(row != null){
				System.out.println(row.getRowString());
				row = filter.getFilterRow();
			}
			System.out.println("Filter Name: " + filter.getName() + ", Access: " + filter.getAccess() + ", Row: "
					+ filter.getFilterRow());
		}
	}
	
	// Original API design    
  public static void verifyFilterAPI(IEssCube cube) throws EssException {
  	
		String[] rowStrings = { "@IDESCENDANTS(Scenario)", "@IALLANCESTORS (Scenario)" };
//		IEssCube.IEssSecurityFilter filter = cube.createSecurityFilter("TestFilter1");
		
		System.out.println("\nList after Creating ...");
		getFilterList(cube);

		IEssCube.IEssSecurityFilter filter = cube.setSecurityFilter("TestFilter10", true, IEssCube.EEssCubeAccess.READ_WRITE_CUBE_DATA);
		for (int i = 0; i < rowStrings.length; i++) {
			filter.setFilterRow(rowStrings[i], (short)IEssCube.EEssCubeAccess.READ_WRITE_CUBE_DATA_INT_VALUE);
		}
		filter.setFilterRow("", (short)IEssCube.EEssCubeAccess.READ_CUBE_DATA_INT_VALUE);

		System.out.println("\nList after Setting ...");
		getFilterList(cube);
		


/*		IEssCube.IEssSecurityFilter filter1 = cube.getSecurityFilter("TestFilter10");
		System.out.println("\nDetails of getSecurityFilter for " + filter1.getName());
		for (int i = 0; i < rowStrings.length; i++) {
			IEssFilterRow filtRow = filter1.getFilterRow();
			System.out.print("\tString:" + filtRow.getRowString());
			System.out.println("\tAccess:" + filtRow.getAccess());
		}
		filter1.getFilterRow();*/
  	
		System.out.println("\nVerifying Filters ...");
		try {
			filter.verifyFilter(rowStrings);
			IEssCube.IEssSecurityFilter filter1 = cube.getSecurityFilter("TestFilter10");
			for (int i = 0; i < rowStrings.length; i++) {
				((EssSecurityFilter) filter1).verifyFilterRow(rowStrings[i]);
			}
			((EssSecurityFilter) filter1).verifyFilterRow("");
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("Verified Filters.");

		filter.delete();
		System.out.println("\nList of filters after Deleting ...");
		getFilterList(cube);
  }

}
