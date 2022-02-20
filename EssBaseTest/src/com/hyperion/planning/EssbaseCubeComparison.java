package com.hyperion.planning;

import java.util.List;

import com.essbase.api.base.EssException;
import com.essbase.api.datasource.IEssCube;
import com.essbase.api.datasource.IEssOlapApplication;
import com.essbase.api.metadata.IEssCubeOutline;
import com.essbase.api.metadata.IEssDimension;
import com.essbase.api.metadata.IEssMember;

public class EssbaseCubeComparison {

	private String outputFilePath                    = null;
	
	public String getOutputFilePath() {
		return outputFilePath;
	}

	public void setOutputFilePath(String outputFilePath) {
		this.outputFilePath = outputFilePath;
	}
	
	public void initializeResources() {
		try {
			super.init();
			sourceOlapApp = getApplication(getSourceApplication());
			targetOlapApp = getApplication(getTargetApplication());
			sourceCube = getCube(sourceOlapApp, getSourceCubeName());
			targetCube = getCube(targetOlapApp, getTargetCubeName());
			sourceCubeOutline = openCubeOutline(sourceCube);	
			targetCubeOutline = openCubeOutline(targetCube);
		}catch (EssException e) {
			System.out.println("Error : "+e.getMessage());
			e.printStackTrace();
		}
	}

	public void findDifference() throws EssException {
		System.out.println(sourceCube.getCountDimensions());
		System.out.println(targetCube.getCountDimensions());
		List<IEssDimension> dimensionsList = EssBaseOperations.readDimension(sourceCubeOutline);
		EssBaseOperations.readMembers(dimensionsList);
		System.out.println(dimensionsList.size());
		System.out.println(sourceCubeOutline.getName()+sourceCubeOutline.getName());
		
		sourceCubeOutline.close();
		sourceCube.clearActive();
		sourceCube.clearAllData();
		sourceOlapApp.stop();
		System.out.println("Is cube active : "+sourceCube.isActive());
		targetCube.setActive();
		System.out.println("Is sourceCubeOutline open : "+sourceCubeOutline.isOpen());
		
		targetCubeOutline = EssBaseUtil.openCubeOutline(targetCube);
		List<IEssDimension> dimensionsList2 = EssBaseOperations.readDimension(targetCubeOutline);
		System.out.println(dimensionsList2.size());
		EssBaseOperations.readMembers(dimensionsList2);
	}

	public void cleanResources() {
		comeOutClean(sourceCubeOutline, targetCubeOutline);
	}
}
