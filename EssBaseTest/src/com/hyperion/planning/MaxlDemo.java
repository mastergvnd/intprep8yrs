package com.hyperion.planning;

import com.essbase.api.base.EssException;
import com.essbase.api.metadata.IEssDimension.EEssAttributeDataType;

public class MaxlDemo {
	public static void main(String[] args) {
		System.out.println("starting");
		EssbaseCube cube = new EssbaseCube();
		cube.setApplicationName("ASOsamp2");
		cube.setCubeName("Sample");
		
		try{
			EssbaseConnectionManager conn = new EssbaseConnectionManager();
			conn.connectToCube(cube);
			cube.openMaxlSession("maxlSession");
			cube.load();
			boolean isQTEnabled = EssbaseUtil.isQueryTrackingEnabled(cube);
			System.out.println("isQTEnabled : "+isQTEnabled);
			if(!isQTEnabled){
				EssbaseUtil.enableQueryTracking(cube);
			}
			isQTEnabled = EssbaseUtil.isQueryTrackingEnabled(cube);
			System.out.println("isQTEnabled : "+isQTEnabled);
		} catch (EssException e) {
			System.out.println("Error : "+e.getMessage());
			e.printStackTrace();
		}
		finally{
			EssbaseConnectionManager.comeOutClean(cube);
		}
	}
}
