package com.test;

import java.util.Arrays;

import com.essbase.api.base.EssException;
import com.essbase.api.base.IEssBaseObject;
import com.essbase.api.datasource.IEssCube;
import com.essbase.api.metadata.IEssCubeOutline;
import com.essbase.api.metadata.IEssDimension;
import com.essbase.api.metadata.IEssMember;
import com.essbase.api.session.IEssbase;
import com.hyperion.planning.EssbaseConnectionManager;
import com.hyperion.planning.EssbaseCube;

public class ReadStorageType {

	public static void main(String[] args) {
		EssbaseConnectionManager conection = new EssbaseConnectionManager("", "", "slc14vmw.us.oracle.com");
		EssbaseCube cube = new EssbaseCube();
//		cube.setApplicationName("HSP_TEMP_OTL_IMPORT_APP");
//		cube.setCubeName("RepGenteASO");
		cube.setApplicationName("HSP_TEMP_OTL_IMPORT_APP");
		cube.setCubeName("RepGenteASO");
		try {
			conection.connectToCube(cube);
			cube.load();
			IEssCube essCube = cube.getCube();
			IEssCubeOutline otl = cube.openCubeOutline(essCube);
			IEssMember m = otl.findMember("Entidade");
			System.out.println(m.getShareOption().DYNAMIC_CALC.stringValue());
			System.out.println(m.getShareOption().DYNAMIC_CALC_AND_STORE.stringValue());
			System.out.println(m.getShareOption().EXTENDED_SHARED_MEMBER.stringValue());
			System.out.println(m.getShareOption().LABEL_ONLY.stringValue());
			System.out.println(m.getShareOption().NEVER_SHARE.stringValue());
			System.out.println(m.getShareOption().SHARED_MEMBER.stringValue());
			System.out.println(m.getShareOption().STORE_DATA.stringValue());
			IEssBaseObject dims[] = otl.getDimensions().getAll();
			for(IEssBaseObject dim : dims){
				IEssDimension dimension = (IEssDimension) dim;
				if(!dimension.getName().equalsIgnoreCase("Entidade")){
					System.out.println("returning");
					continue;
				}
				System.out.println(dimension.getName() + " : " + dimension.getStorageType().stringValue());
				IEssBaseObject members[] = dimension.getDimensionRootMember().getChildMembers().getAll();
				for(IEssBaseObject baseObject : members){
					IEssMember member = (IEssMember) baseObject;
					System.out.println("\t"+member.getName() + " : " + member.getShareOption().stringValue());
					member.getShareOption().SHARED_MEMBER.stringValue();
					printSubMembers(member, "\t\t");
				}
			}
			conection.comeOutClean(cube);
		}catch(Exception e){
			System.out.println("Exception : "+e.getMessage());
		}
	}

	private static void printSubMembers(IEssMember member, String align) throws EssException {
		IEssBaseObject members[] = member.getChildMembers().getAll();
		if(members.length == 0){
			return;
		}
		for(IEssBaseObject baseObject : members){
			IEssMember sMember = (IEssMember) baseObject;
			System.out.println(align + sMember.getName() + " : " + sMember.getShareOption().stringValue());
			printSubMembers(sMember, align+"\t");
		}
	}

}
