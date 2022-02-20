package com.hyperion.planning;

import java.util.ArrayList;
import java.util.List;

import com.essbase.api.base.EssException;
import com.essbase.api.base.IEssBaseObject;
import com.essbase.api.metadata.IEssDimension;
import com.essbase.api.metadata.IEssMember;

public class TreeComparator {
	public static int leftRowNumber = 0;
	public static int rightRowNumber = 0;
	public static int leftcolNumber = 0;
	public static int rightcolNumber = 0;
	public static void printComparison(EssbaseCube sourceCube, EssbaseCube targetCube) throws EssException{
		
		int leftParentColumn = 0;
		int rightParentColumn = 6;
		
		rightcolNumber = getLargestMemberWidth(sourceCube);
		System.out.println(rightcolNumber);
	
		List<IEssDimension> sourceDimensionList = EssbaseUtil.getDimensionsAsList(sourceCube.getCubeOutline());
		List<IEssDimension> targetDimensionList = EssbaseUtil.getDimensionsAsList(targetCube.getCubeOutline());
		
		int dimensionLength = sourceDimensionList.size() < targetDimensionList.size() ? sourceDimensionList.size() : targetDimensionList.size();
		int i;
		for(i = 0; i<dimensionLength ; i++){
			System.out.println("i : "+i);
			leftcolNumber = 0;
			rightcolNumber = getLargestMemberWidth(sourceCube);
			
			IEssDimension sourceDimension = sourceDimensionList.get(i);
			IEssDimension targetDimension = targetDimensionList.get(i);
			
			ExcelUtil.write(sourceDimension.getName(), leftRowNumber++, leftcolNumber);
			ExcelUtil.write(targetDimension.getName(), rightRowNumber++, rightcolNumber);
			ExcelUtil.format(sourceDimension.getName(), targetDimension.getName());
			
			IEssBaseObject[] sMembers = sourceDimension.getDimensionRootMember().getChildMembers().getAll();
			IEssBaseObject[] tMembers = targetDimension.getDimensionRootMember().getChildMembers().getAll();
			int memLength = sMembers.length < tMembers.length ? sMembers.length : tMembers.length;
			
			leftParentColumn = ++leftcolNumber;
			rightParentColumn = ++rightcolNumber;
			for(int j = 0; j<memLength; j++){
				IEssMember sMember = (IEssMember) sMembers[j];
				IEssMember tMember = (IEssMember) tMembers[j];
				
				leftcolNumber = leftParentColumn;
				rightcolNumber = rightParentColumn;
				
				ExcelUtil.write(sMember.getName(), leftRowNumber++, leftcolNumber);
				ExcelUtil.write(tMember.getName(), rightRowNumber++, rightcolNumber);
				ExcelUtil.format(sMember.getName(), tMember.getName());
				
				printSubmembers(sMember, tMember);
			}
		}
		if(sourceDimensionList.size() > targetDimensionList.size())
			printLeftDimension(sourceDimensionList, i);
		else if (sourceDimensionList.size() < targetDimensionList.size())
			printRightDimension(targetDimensionList, i);
		
	}
	
	private static void printLeftDimension(List<IEssDimension> sourceDimensionList, int i) throws EssException {
		int leftParentColumn = 0;
		for(; i<sourceDimensionList.size() ; i++){
			System.out.println("i : "+i);
			leftcolNumber = 0;
			IEssDimension sourceDimension = sourceDimensionList.get(i);
			ExcelUtil.write(sourceDimension.getName(), leftRowNumber++, leftcolNumber);
			ExcelUtil.format(sourceDimension.getName(), null);
			IEssBaseObject[] sMembers = sourceDimension.getDimensionRootMember().getChildMembers().getAll();
			leftParentColumn = ++leftcolNumber;
			for(int j = 0; j<sMembers.length; j++){
				IEssMember sMember = (IEssMember) sMembers[j];
				leftcolNumber = leftParentColumn;
				ExcelUtil.write(sMember.getName(), leftRowNumber++, leftcolNumber);
				ExcelUtil.format(sMember.getName(), null);
				printLeftSubMembers(sMember, 0);
			}
		}
	}
	
	private static void printRightDimension(List<IEssDimension> targetDimensionList, int i) throws EssException {
		int rightParentColumn = 6;
		rightcolNumber = 6;//getLargestMemberWidth(sourceCube);
		for(; i<targetDimensionList.size() ; i++){
			System.out.println("i : "+i);
			rightcolNumber = 6;//getLargestMemberWidth(sourceCube);
			IEssDimension targetDimension = targetDimensionList.get(i);
			ExcelUtil.write(targetDimension.getName(), rightRowNumber++, rightcolNumber);
			ExcelUtil.format(null, targetDimension.getName());
			IEssBaseObject[] tMembers = targetDimension.getDimensionRootMember().getChildMembers().getAll();
			rightParentColumn = ++rightcolNumber;
			for(int j = 0; j<tMembers.length; j++){
				IEssMember tMember = (IEssMember) tMembers[j];
				rightcolNumber = rightParentColumn;
				ExcelUtil.write(tMember.getName(), rightRowNumber++, rightcolNumber);
				ExcelUtil.format(null, tMember.getName());
				printRightSubMembers(tMember, 0);
			}
		}
	}

	public static void printSubmembers(IEssMember sMember, IEssMember tMember) throws EssException {
		
		int leftParentColumn = leftcolNumber++;
		int rightParentColumn = rightcolNumber++;
		
		IEssBaseObject[] sMembers = sMember.getChildMembers().getAll();
		IEssBaseObject[] tMembers = tMember.getChildMembers().getAll();
		int subMemLength = sMembers.length < tMembers.length ? sMembers.length : tMembers.length;
		int i=0;
		for(i = 0; i<subMemLength; i++){
			IEssMember sSubMember = (IEssMember) sMembers[i];
			IEssMember tSubMember = (IEssMember) tMembers[i];
			ExcelUtil.write(sSubMember.getName(), leftRowNumber++, leftcolNumber);
			ExcelUtil.write(tSubMember.getName(), rightRowNumber++, rightcolNumber);
			ExcelUtil.format(sSubMember.getName(), tSubMember.getName());
			printSubmembers(tSubMember, tSubMember);
		}
		leftcolNumber = leftParentColumn;
		rightcolNumber = rightParentColumn;
		if(sMembers.length > tMembers.length)
			printLeftSubMembers(sMember, i);
		else if(sMembers.length < tMembers.length)
			printRightSubMembers(tMember, i);
	}
	
	private static void printLeftSubMembers(IEssMember member, int i) throws EssException {
		int leftParentColumn = leftcolNumber++;
		IEssBaseObject[] members = member.getChildMembers().getAll();
		for(; i<members.length; i++){
			IEssMember sSubMember = (IEssMember) members[i];
			ExcelUtil.write(sSubMember.getName(), leftRowNumber++, leftcolNumber);
			ExcelUtil.format(sSubMember.getName(), null);
			rightRowNumber++;
			printLeftSubMembers(sSubMember, 0);
		}
		leftcolNumber = leftParentColumn;
	}

	private static void printRightSubMembers(IEssMember member, int i) throws EssException {
		int rightParentColumn = rightcolNumber++;
		IEssBaseObject[] members = member.getChildMembers().getAll();
		for(; i<members.length; i++){
			IEssMember tSubMember = (IEssMember) members[i];
			ExcelUtil.write(tSubMember.getName(), rightRowNumber++, rightcolNumber);
			ExcelUtil.format(null, tSubMember.getName());
			leftRowNumber++;
			printRightSubMembers(tSubMember, 0);
		}
		rightcolNumber = rightParentColumn;
	}

	private static int getLargestMemberWidth(EssbaseCube sourceCube) throws EssException {
		return 6;
	}

	public static void writeSummary(EssbaseCube cubeName1, EssbaseCube cubeName2) throws EssException {
		List<Object> list = new ArrayList<Object>();
		list.add("Source Cube Name");
		list.add(cubeName1.getCubeName());
		list.add("Target Cube Name");
		list.add(cubeName2.getCubeName());
		list.add("Number of dimensions in source cube");
		list.add(cubeName1.getCube().getCountDimensions());
		list.add("Number of dimensions in target cube");
		list.add(cubeName2.getCube().getCountDimensions());
		ExcelUtil.writeBasicSummary(list);
		List<String>source = new ArrayList<String>();
		source.add("Market");
		source.add("Caffeinated");
		source.add("Ounces");
		source.add("Pkg Type");
		List<String>common = new ArrayList<String>();
		common.add("Year");
		common.add("Measure");
		common.add("Product");
		List<String>target = new ArrayList<String>();
		target.add("Eastern Region");
		ExcelUtil.writeVennDiagram(source, common, target);
		ExcelUtil.writePropertyDetails();
		System.out.println("done");
	}
}