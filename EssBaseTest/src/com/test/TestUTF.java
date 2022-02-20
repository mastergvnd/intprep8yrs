package com.test;

import com.essbase.api.base.EssException;
import com.essbase.api.datasource.EssCube;
import com.essbase.api.datasource.EssOtlExportOptions;
import com.essbase.api.datasource.IEssCube;
import com.essbase.api.datasource.IEssOlapApplication;
import com.hyperion.planning.EssbaseConnectionManager;
import com.hyperion.planning.EssbaseCube;

public class TestUTF {

	public static void main(String[] args) {
		EssbaseConnectionManager conn = new EssbaseConnectionManager("epm_default_cloud_admin", "welcome1", "slc15rvo.us.oracle.com:14231");
		EssbaseCube cube = new EssbaseCube();
		cube.setApplicationName("My");
		cube.setCubeName("UWW309");
		try {
			conn.connectToCube(cube);
			System.out.println("Connection Sucessful");
	        try {
	            IEssOlapApplication essbaseApplication = conn.getApplication();

	            if (essbaseApplication == null) {
	            	System.out.println("No application");
	            } else {
	                IEssCube essbaseCube = essbaseApplication.getCube(cube.getCubeName());
	                EssOtlExportOptions opts = new EssOtlExportOptions();
	                opts.setOutputFlag(EssOtlExportOptions.ESS_OTLEXPORT_ALLDIMS);
	                EssCube cube2 = (EssCube)essbaseCube;
	                byte[] otlXmlBytes = cube2.exportOutline(opts, null);
	                System.out.println(otlXmlBytes.length);
	            }
	        } catch (EssException e) {
	        	e.printStackTrace();
	            throw e;
	        } finally {
	        }
		}catch(Exception e){
			e.printStackTrace();
		}
	}

}
