
package com.hyperion.planning;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.essbase.api.base.EssException;
import com.essbase.api.base.IEssBaseObject;
import com.essbase.api.metadata.IEssCubeOutline;
import com.essbase.api.metadata.IEssDimension;
import com.essbase.api.metadata.IEssMember;

public class EssbaseOperations {

	private static StringBuffer dProperties = new StringBuffer();
	private static StringBuffer dOrder = new StringBuffer();
	private static StringBuffer mOrder = new StringBuffer();
	private static StringBuffer mProperties = new StringBuffer(); 
	public static List<IEssDimension> readDimensionsAsList(IEssCubeOutline cubeOutLine) throws EssException{
		List<IEssDimension> dimenionsList = new ArrayList<IEssDimension>();
		IEssBaseObject[] dimensions = cubeOutLine.getDimensions().getAll();
		for(int i = 0; i<dimensions.length; i++){
			IEssDimension dimension = (IEssDimension) dimensions[i];
			dimenionsList.add(dimension);
		}
		return dimenionsList;
	}
	
	public static Map<String, IEssDimension> readDimensionsAsMap(IEssCubeOutline cubeOutLine) throws EssException{
		Map<String, IEssDimension> dimenionsList = new LinkedHashMap<String, IEssDimension>();
		IEssBaseObject[] dimensions = cubeOutLine.getDimensions().getAll();
		for(int i = 0; i<dimensions.length; i++){
			IEssDimension dimension = (IEssDimension) dimensions[i];
			dimenionsList.put(dimension.getName(), dimension);
		}
		return dimenionsList;
	}

	public static void cubeComparator(EssbaseCube sourceCube, EssbaseCube targetCube) throws EssException, IOException {
		Map<String, IEssDimension> sourceDimensinMap = readDimensionsAsMap(sourceCube.getCubeOutline());
		Map<String, IEssDimension> targetDimensinMap = readDimensionsAsMap(targetCube.getCubeOutline());
		Writer.write("Source Cube Name : "+sourceCube.getCubeName());
		Writer.write("Target Cube Name : "+targetCube.getCubeName());
		System.out.println("Source Cube Name : "+sourceCube.getCubeName());
		System.out.println("Target Cube Name : "+targetCube.getCubeName());
		System.out.println("Number of dimensions in source cube: " + sourceDimensinMap.size());
		System.out.println("Number of dimensions in target cube: " + targetDimensinMap.size());	
		System.out.println();
		Writer.write("Number of dimensions in source cube: "+sourceDimensinMap.size());
		Writer.write("Number of dimensions in target cube: "+targetDimensinMap.size(), System.lineSeparator()+System.lineSeparator());
		findDimensionDifferences(sourceDimensinMap, targetDimensinMap);
		for(String dName : sourceDimensinMap.keySet()){
			IEssDimension sDimension = sourceDimensinMap.get(dName);
			IEssBaseObject[] members = sDimension.getDimensionRootMember().getChildMembers().getAll();
			for(int i = 0; i<members.length; i++){
				IEssMember sMember = (IEssMember) members[i];
				findMembersDifferences(sMember, targetCube.getCubeOutline());
			}
		}
		System.out.println(mOrder);
		System.out.println(mProperties);
	}

	private static void findMembersDifferences(IEssMember sMember, IEssCubeOutline targetOutline) throws EssException {
		IEssMember tMember = null;
		try{
			tMember = targetOutline.findMember(sMember.getName());
		}catch(EssException e){
			
		}
		if(tMember != null){
			if(sMember.getMemberNumber() != tMember.getMemberNumber()){
				mOrder.append("Member Names are same but order is different : ("+sMember.getName()+") "+sMember.getMemberNumber()+" , "+tMember.getMemberNumber()+System.lineSeparator());
			}
			checkforMemberProperties(sMember, tMember);
		}
		IEssBaseObject[] subMembers = sMember.getChildMembers().getAll();
		for(int i = 0; i<subMembers.length; i++){
			IEssMember subMember = (IEssMember) subMembers[i];
			findMembersDifferences(subMember, targetOutline);
		}
	}

	private static void checkforMemberProperties(IEssMember sMember, IEssMember tMember) throws EssException {
		Map<String, String> smProperty = readMemberProperties(sMember);
		Map<String, String> tmProperty = readMemberProperties(tMember);
		for(String pName : smProperty.keySet()){
			if(tmProperty.containsKey(pName) && !smProperty.get(pName).equals(tmProperty.get(pName))){
				mProperties.append("Member Property is not matching : ("+sMember.getName()+" , "+pName+") -> "+smProperty.get(pName)+" , "+tmProperty.get(pName)+System.lineSeparator());
			}
		}
	}
	
	public static Map<String, String> readMemberProperties(List<IEssMember> membersList) throws EssException {
		Map<String, String> propertyMap = new HashMap<String, String>();
		for(IEssMember member : membersList){
			String[] propertyNames = member.getPropertyNames();
			for(String property : propertyNames){
				try{
					String propertyValue = member.getPropertyValueAny(property).toString();
					propertyMap.put(property, propertyValue);
				}catch(EssException e){
					/*Skip this because the property is not found*/
				}
			}
			//System.out.println("Properties for Member : "+member.getName());
			//System.out.println(propertyMap);
		}
		return propertyMap;
	}
	
	public static Map<String, String> readMemberProperties(IEssMember member) throws EssException {
		List<IEssMember> list = new ArrayList<IEssMember>();
		list.add(member);
		return readMemberProperties(list);
	}

	public static Map<String, String> readDimensionProperties(IEssDimension dimension) throws EssException {
		Map<String, String> propertyMap = new HashMap<String, String>();
		String[] propertyNames = dimension.getPropertyNames();
		for (String property : propertyNames)
			propertyMap.put(property, dimension.getPropertyValueAny(property).toString());
		return propertyMap;
	}
	
	private static void checkforDimensionProperties(IEssDimension sDimension, IEssDimension tDimension) throws EssException, IOException {
		Map<String, String> sdProperty = readDimensionProperties(sDimension);
		Map<String, String> tdProperty = readDimensionProperties(tDimension);
		for(String dName : sdProperty.keySet()){
			if(tdProperty.containsKey(dName) && !sdProperty.get(dName).equals(tdProperty.get(dName))){
				dProperties.append("Dimension Property is not matching : ("+sDimension.getName()+" , "+dName+") -> "+sdProperty.get(dName)+" , "+tdProperty.get(dName)+System.lineSeparator());
			}
		}
	}

	private static void findDimensionDifferences(Map<String, IEssDimension> sDimensionMap,
			Map<String, IEssDimension> tDimensionMap) throws EssException, IOException {

		for(String dName : sDimensionMap.keySet()){
			if(tDimensionMap.containsKey(dName)){
				IEssDimension sDimension = sDimensionMap.get(dName);
				IEssDimension tDimension = tDimensionMap.get(dName);
				if(sDimension.getDimensionNumber() != tDimension.getDimensionNumber()){
					System.out.println("Dimension Names are same but order is different : ("+sDimension.getName()+") "+sDimension.getDimensionNumber()+tDimension.getDimensionNumber());
					dOrder.append("Dimension Names are same but order is different : ("+sDimension.getName()+") "+sDimension.getDimensionNumber()+tDimension.getDimensionNumber());
				}	
				checkforDimensionProperties(sDimension, tDimension);
			}else{
				System.out.println("Dimension is missing in target cube : "+dName);
				Writer.write("Dimension is missing in target cube : "+dName);
			}
		}
		
		for(String dName : tDimensionMap.keySet()){
			if(!sDimensionMap.containsKey(dName)){
				System.out.println("Dimension is missing in source cube : "+dName);
				Writer.write("Dimension is missing in source cube : "+dName);
			}
		}
		
		System.out.println(dOrder);
		System.out.println(dProperties);
		Writer.write(dOrder.toString());
		Writer.write(dProperties.toString());
//		print(sDimensionMap, tDimensionMap);
	}
}