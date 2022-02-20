package com.test;

import java.util.HashMap;
import java.util.List;

import com.essbase.api.base.EssException;
import com.essbase.api.datasource.IEssCube;
import com.essbase.api.metadata.IEssDimension;
import com.essbase.api.metadata.IEssMember;
import com.hyperion.planning.EssbaseConnectionManager;
import com.hyperion.planning.EssbaseCube;
import com.hyperion.planning.EssbaseUtil;
import com.hyperion.planning.NotepadWriter;

public class MembersComparison {

	public static void main(String[] args) {
		EssbaseConnectionManager conn = new EssbaseConnectionManager("", "", "slc14vmw.us.oracle.com");
		EssbaseCube cube = new EssbaseCube();
		cube.setApplicationName("Commerce");
		cube.setCubeName("Commercl");
		NotepadWriter writer = new NotepadWriter("C:\\Users\\govgupta.ORADEV\\Desktop\\BaseApp.txt");
		try {
			conn.connectToCube(cube);
			cube.load();
			IEssCube essCube = cube.getCube();
			IEssDimension dimension = cube.openCubeOutline(essCube).findDimension("Nominal");
			writer.write(dimension.getName());
			System.out.println(dimension.getName());
			List<IEssMember> memberList =  EssbaseUtil.getMembers(dimension);
			for(IEssMember member : memberList){
				writer.write(member.getName());
				System.out.println(member.getName());
				printSubmembers(writer, member);
			}
		}catch(EssException e){
			writer.write(e.getMessage());
		}finally{
			writer.closeWriter();
		}

}

	private static void printSubmembers(NotepadWriter writer, IEssMember member) throws EssException {
		if(member == null){
			return;
		}
		List<IEssMember> subMemberList =  EssbaseUtil.getSubMembers(member);
		for(IEssMember subMember : subMemberList){
			writer.write(subMember.getName());
			System.out.println(subMember.getName());
			printSubmembers(writer, subMember);
		}
	}
}
