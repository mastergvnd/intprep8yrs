package com.hyperion.planning;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.essbase.api.base.EssException;
import com.essbase.api.base.IEssBaseObject;
import com.essbase.api.metadata.IEssCubeOutline;
import com.essbase.api.metadata.IEssDimension;
import com.essbase.api.metadata.IEssMember;

public class EssBaseUtilBackUp {

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
		Map<String, IEssDimension> dimenionsList = new HashMap<String, IEssDimension>();
		IEssBaseObject[] dimensions = cubeOutLine.getDimensions().getAll();
		for(int i = 0; i<dimensions.length; i++){
			IEssDimension dimension = (IEssDimension) dimensions[i];
			dimenionsList.put(dimension.getName(), dimension);
		}
		return dimenionsList;
	}
	
	public static Map<String, IEssMember> readMembersAsMap(IEssDimension dimension) throws EssException{
		Map<String, IEssMember> membersList = new HashMap<String, IEssMember>();
		IEssBaseObject[] members = dimension.getDimensionRootMember().getChildMembers().getAll();
		for(int i = 0; i<members.length; i++){
			IEssMember member = (IEssMember) members[i];
			membersList.put(member.getName(), member);
		}
		return membersList;
	}
	
	public static Map<String, IEssMember> readSubMembersAsMap(IEssMember member) throws EssException{
		Map<String, IEssMember> membersList = new HashMap<String, IEssMember>();
		IEssBaseObject[] members = member.getChildMembers().getAll();
		for(int i = 0; i<members.length; i++){
			IEssMember subMember = (IEssMember) members[i];
			membersList.put(subMember.getName(), subMember);
		}
		return membersList;
	}

	public static void cubeComparator(EssbaseCube sourceCube, EssbaseCube targetCube) throws EssException, IOException {
		Map<String, IEssDimension> sourceDimensionMap = readDimensionsAsMap(sourceCube.getCubeOutline());
		Map<String, IEssDimension> targetDimensionMap = readDimensionsAsMap(targetCube.getCubeOutline());
		
		Writer.write("Source Cube Name : "+sourceCube.getCubeName());
		Writer.write("Target Cube Name : "+targetCube.getCubeName());
		Writer.write("Number of dimensions in source cube : "+sourceDimensionMap.size());
		Writer.write("Number of dimensions in target cube : "+targetDimensionMap.size(), System.lineSeparator()+System.lineSeparator());
		
		System.out.println("Source Cube Name : "+sourceCube.getCubeName());
		System.out.println("Target Cube Name : "+targetCube.getCubeName());
		System.out.println("Number of dimensions in source cube : " + sourceDimensionMap.size());
		System.out.println("Number of dimensions in target cube : " + targetDimensionMap.size());	
		System.out.println();
		
		findDimensionDifferences(sourceDimensionMap, targetDimensionMap);
		
		for(String dName : sourceDimensionMap.keySet()){
			IEssDimension sDimension = sourceDimensionMap.get(dName);
			IEssDimension tDimension = targetDimensionMap.get(dName);
			if(sDimension != null && tDimension != null)
				findMembersDifferences(sDimension, tDimension);
		}
		System.out.println(mOrder);
		System.out.println(mProperties);
		Writer.write(mOrder.toString());
		Writer.write(mProperties.toString());
	}

	private static void findMembersDifferences(IEssDimension sDimension, IEssDimension tDimension) throws EssException, IOException {
		Map<String, IEssMember> sourceMembersMap = readMembersAsMap(sDimension);
		Map<String, IEssMember> targetMembersMap = readMembersAsMap(tDimension);
		
		for(String mName : sourceMembersMap.keySet()){
			if(targetMembersMap.containsKey(mName)){
				IEssMember sMember = sourceMembersMap.get(mName);
				IEssMember tMember = targetMembersMap.get(mName);
				if(sMember.getMemberNumber() != tMember.getMemberNumber()){
					mOrder.append("Member Names are same but order is different : ("+sMember.getName()+") "+sMember.getMemberNumber()+" , "+tMember.getMemberNumber()+System.lineSeparator());
				}
				checkforMemberProperties(sMember, tMember);
				findSubMembersDifferences(sMember, tMember);
			}else{
				System.out.println("Member is missing in target Member  : "+mName);
				Writer.write("Member is missing in target Member  : "+mName);
			}
		}
		
		for(String mName : targetMembersMap.keySet()){
			if(!sourceMembersMap.containsKey(mName)){
				System.out.println("Member is missing in source Member : "+mName);
				Writer.write("Member is missing in source Member : "+mName);
			}
		}
	}

	private static void findSubMembersDifferences(IEssMember sMember, IEssMember tMember) throws EssException, IOException {
		Map<String, IEssMember> sourceMembersMap = readSubMembersAsMap(sMember);
		Map<String, IEssMember> targetMembersMap = readSubMembersAsMap(tMember);
		
		for(String mName : sourceMembersMap.keySet()){
			if(targetMembersMap.containsKey(mName)){
				IEssMember sSubMember = sourceMembersMap.get(mName);
				IEssMember tSubMember = targetMembersMap.get(mName);
				if(sSubMember.getMemberNumber() != sSubMember.getMemberNumber()){
					System.out.println("Member Names are same but order is different : ("+sSubMember.getName()+") "+sSubMember.getMemberNumber()+" , "+tSubMember.getMemberNumber()+System.lineSeparator());
					mOrder.append("Member Names are same but order is different : ("+sSubMember.getName()+") "+sSubMember.getMemberNumber()+" , "+tSubMember.getMemberNumber()+System.lineSeparator());
				}	
				checkforMemberProperties(sSubMember, tSubMember);
				findSubMembersDifferences(sSubMember, tSubMember);
			}else{
				System.out.println("Member is missing in target Member : "+mName);
				Writer.write("Member is missing in target Member : "+mName);
			}
		}
	}

	private static void checkforMemberProperties(IEssMember sMember, IEssMember tMember) throws EssException {
		Map<String, String> smProperty = readMemberProperties(sMember);
		Map<String, String> tmProperty = readMemberProperties(tMember);
		for(String pName : smProperty.keySet()){
			if(tmProperty.containsKey(pName) && !smProperty.get(pName).equals(tmProperty.get(pName)))
				mProperties.append("Member Property is not matching : ("+sMember.getName()+" , "+pName+") -> "+smProperty.get(pName)+" , "+tmProperty.get(pName)+System.lineSeparator());
			else if(!tmProperty.containsKey(pName))
				mProperties.append("Property is missing in target Member : "+pName+System.lineSeparator());
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
			if(tdProperty.containsKey(dName) && !sdProperty.get(dName).equals(tdProperty.get(dName)))
				dProperties.append("Dimension Property is not matching : ("+sDimension.getName()+" , "+dName+") -> "+sdProperty.get(dName)+" , "+tdProperty.get(dName)+System.lineSeparator());
			else if(!tdProperty.containsKey(dName))
				dProperties.append("Property is missing in target Dime : "+dName+System.lineSeparator());
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
	}	
}