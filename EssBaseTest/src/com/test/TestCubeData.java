package com.test;

import java.util.List;

import com.essbase.api.base.EssException;
import com.essbase.api.datasource.IEssCube.EEssRestructureOption;
import com.essbase.api.metadata.IEssDimension;
import com.essbase.api.metadata.IEssMember;
import com.hyperion.planning.EssbaseConnectionManager;
import com.hyperion.planning.EssbaseCube;
import com.hyperion.planning.EssbaseUtil;

public class TestCubeData {

	public static void main(String[] args) {

		System.out.println("starting");
		EssbaseCube cube = new EssbaseCube();
		cube.setApplicationName("AVision");
		cube.setCubeName("VisASO");
		
		try {
			EssbaseConnectionManager conn = new EssbaseConnectionManager();
			conn.connectToCube(cube);
			cube.load();
			System.out.println("Cube Type Is : "+cube.getCube().getCubeType().stringValue());
			System.out.println("Cube Type Is : "+cube.getCube().getCubeType().intValue());
			System.out.println("Server Name : "+cube.getOlapServer().getName()+"   "+conn.getServer());
			short solveOrder = -9;
			IEssMember varianceOrig = cube.getCubeOutline().findMember("Account");
			IEssMember variance = varianceOrig.createChildMember("Test1");
			variance = varianceOrig.createChildMember("Test2");
			variance.setSolveOrder(solveOrder);
			variance.setTwoPassCalculationMember(true);
			variance.updatePropertyValues();			
			cube.getCubeOutline().save(EEssRestructureOption.KEEP_ALL_DATA);
			System.out.println("Solve Order : "+variance.getSolveOrder());
/*			IEssMember v1 = cube.getCubeOutline().findMember("Test1");
			v1.delete();
			v1 = cube.getCubeOutline().findMember("Test2");
			v1.delete();
			cube.getCubeOutline().save(EEssRestructureOption.KEEP_ALL_DATA);*/
			EssbaseConnectionManager.comeOutClean(cube);
			conn.connectToCube(cube);
			cube.load();
			variance = cube.getCubeOutline().findMember("Test2");
			System.out.println("Solve Order : "+variance.getSolveOrder());
		} catch (EssException e) {
			System.out.println("Error : "+e.getMessage());
			e.printStackTrace();
		}
		finally{
			EssbaseConnectionManager.comeOutClean(cube);
		}
	

	}

}
