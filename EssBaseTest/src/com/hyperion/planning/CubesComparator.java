package com.hyperion.planning;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import com.essbase.api.base.EssException;
import com.essbase.api.metadata.EssMember;
import com.essbase.api.metadata.IEssMember;
import com.essbase.api.metadata.IEssMember.EEssShareOption;
import com.essbase.services.olap.main.EssUSERINFO;

public class CubesComparator {

	public static void main(String[] args) throws IOException {
		EssbaseCube sourceCube = new EssbaseCube();
		sourceCube.setApplicationName("HP1");
		sourceCube.setCubeName("IndPln");

		EssbaseCube targetCube = new EssbaseCube();
		targetCube.setApplicationName("HSP_TEMP_OTL_IMPORT_APP");
		targetCube.setCubeName("IndPln");
								
		Writer.setFilePath("D:\\sheets\\diffNote_IndPln2.txt");
		Writer.init();
		ExcelUtil.setFilePath("D:\\sheets\\diff11_IndPln2.xlsx");
		ExcelUtil.init();
		
		try{
			EssbaseConnectionManager conn = new EssbaseConnectionManager("epm_default_cloud_admin", "welcome1", "slc08vbn.us.oracle.com:14231");
			conn.connectToCube(sourceCube);
			sourceCube.load();

			EssbaseConnectionManager conn2 = new EssbaseConnectionManager("epm_default_cloud_admin", "welcome1", "slc08vbn.us.oracle.com:14231");
			conn2.connectToCube(targetCube);
			targetCube.load();
//			EssbaseUtil.loadCache(targetCube, new Outline());
			
			EssbaseUtil.cubeComparator(sourceCube, targetCube);
			TreeComparator.writeSummary(sourceCube, targetCube);
			TreeComparator.printComparison(sourceCube, targetCube);
		} catch (EssException e) {
			System.out.println("Error : "+e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Error : "+e.getMessage());
			e.printStackTrace();
		}
		finally{
			EssbaseConnectionManager.comeOutClean(sourceCube);
			EssbaseConnectionManager.comeOutClean(targetCube);
			Writer.write(EssbaseUtil.getAllProperties().toString());
			Writer.closeWriter();
			ExcelUtil.closeWorkbook();
		}
		Desktop dt = Desktop.getDesktop();
		dt.open(new File("D:\\sheets\\diff8.xlsx"));
		System.out.println("done");
	}
}