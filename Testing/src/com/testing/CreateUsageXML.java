package com.testing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class CreateUsageXML {

	static int clusterCount = 1;
	static int cardCount = 1;
	static int tabCount = 1;
	
	public static void main(String[] args) throws IOException {

		  File file = new File("D:\\GIT\\operationalPlanningNew\\pbcs\\planning\\HspJS\\resources\\Structure\\Oprplan\\OprplanFuseStructureUsage.xml");
		  BufferedReader br = new BufferedReader(new FileReader(file));
		  StringBuffer updatedXML = new StringBuffer("");
		  String st;
		  while ((st = br.readLine()) != null){
			  if(st.contains("<cardCluster")){
				  String s = st.substring(st.indexOf("id=\"")+4, st.indexOf("\"", st.indexOf("id=\"") + 4));
//				  st = st.replace(s, "EPM_CL_"+clusterCount++);
				  st = st.replace(s, "");
				  //System.out.println(st.substring(st.indexOf("internalID=\""), st.indexOf("\"", st.indexOf("internalID=\"") + 12)));
				  //System.out.println(s);
			  }else if(st.contains("<card")){
				  String s = st.substring(st.indexOf("id=\"")+4, st.indexOf("\"", st.indexOf("id=\"") + 4));
//				  st = st.replace(s, "EPM_CA_"+cardCount++);
				  st = st.replace(s, "");
			  }else if(st.contains("<tab")){
				  String s = st.substring(st.indexOf("id=\"")+4, st.indexOf("\"", st.indexOf("id=\"") + 4));
//				  st = st.replace(s, "EPM_TA_"+tabCount++);
				  st = st.replace(s, "");
			  }
			  updatedXML.append(st).append(System.lineSeparator());
		  }
		  System.out.println(updatedXML);
	}
}
