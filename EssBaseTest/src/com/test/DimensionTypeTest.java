package com.test;

import java.util.HashMap;
import java.util.LinkedHashMap;

import com.essbase.api.base.IEssBaseObject;
import com.essbase.api.datasource.IEssCube;
import com.essbase.api.metadata.IEssCubeOutline;
import com.essbase.api.metadata.IEssDimension;
import com.hyperion.planning.EssbaseConnectionManager;
import com.hyperion.planning.EssbaseCube;

public class DimensionTypeTest {
	
	private static LinkedHashMap<Integer, String> planningDimensions = new LinkedHashMap<Integer, String>();
	private static LinkedHashMap<Integer, Integer> dimesnionMapping = new LinkedHashMap<Integer, Integer>();

	public static void main(String[] args) {
		
		EssbaseConnectionManager conn = new EssbaseConnectionManager("", "", "slc14vmw.us.oracle.com");
		EssbaseCube cube = new EssbaseCube();
		cube.setApplicationName("HSP_TEMP_OTL_IMPORT_APP_BCKUP");
		cube.setCubeName("Jane_Story_SALES_New");
		HashMap<String, String> mappingResult = new HashMap<String, String>();
		try {
			conn.connectToCube(cube);
			cube.load();
			IEssCube essCube = cube.getCube();
			planningDimensions = loadPlanningDimesnions();
			dimesnionMapping = loadDimensionsMapping();
			IEssCubeOutline outline = EssbaseCube.openCubeOutline(essCube);
			IEssBaseObject dimensions[] = outline.getDimensions().getAll();
			for(IEssBaseObject baseObject : dimensions){
				IEssDimension dimension = (IEssDimension) baseObject;
				System.out.println("Dimension Name : "+dimension.getName() + " ||||||  Dimension Type : " + dimension.getCategory().stringValue());
				String otlDimName = dimension.getCategory().stringValue();
				int otlDimension = dimension.getCategory().intValue();
				if(dimesnionMapping.containsKey(otlDimension)){
					String planningDimName = planningDimensions.get(dimesnionMapping.get(otlDimension));
					mappingResult.put(otlDimName, planningDimName);
				}
			}
			System.out.println(mappingResult);
		}catch(Exception e){
			System.out.println("Exception : "+e.getMessage());
		}
	}

	private static LinkedHashMap<Integer, Integer> loadDimensionsMapping() {
		LinkedHashMap<Integer, Integer> dimensionsMapping = new LinkedHashMap<Integer, Integer>();
		dimensionsMapping.put(0, null);
		dimensionsMapping.put(1, 32);
		dimensionsMapping.put(2, 34);
		dimensionsMapping.put(3, null);
		dimensionsMapping.put(4, null);
		dimensionsMapping.put(5, 37);
		dimensionsMapping.put(6, null);
		dimensionsMapping.put(7, null);
		return dimensionsMapping;
	}

	private static LinkedHashMap<Integer, String> loadPlanningDimesnions() {
		LinkedHashMap<Integer, String> planningDimensions = new LinkedHashMap<Integer, String>();
		planningDimensions.put(31, "Scenario");
		planningDimensions.put(32, "Account");
		planningDimensions.put(33, "Entity");
		planningDimensions.put(34, "Period");
		planningDimensions.put(35, "Version");
		planningDimensions.put(37, "Currency");
		planningDimensions.put(38, "Year");
		return planningDimensions;
	}
}
