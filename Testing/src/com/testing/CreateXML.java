package com.testing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

public class CreateXML {

    private static final String TENANT_NAME = "tenantName";
    private static final String SERVICE_NAME = "serviceName";
    
	public static void main(String[] args) throws IOException {
		  File file = new File("C:\\Users\\govgupta.ORADEV\\Desktop\\tenants_details.txt");
		  BufferedReader br = new BufferedReader(new FileReader(file));
		  StringBuffer xml = new StringBuffer("");
		  String st;
		  while ((st = br.readLine()) != null){
			  xml.append("<healthCheckStatistics applicationType=\"PBCS\" ");
			  st = st.replace("registerHealthCHeckFactory(new HspHealthCheckCriteriaFactory().setAppType(appType).", "").trim();
			  String methods[] = st.split("\\.");
			  for(String method : methods){
				  String key = "";
				  String value = "";
				  if(method.contains("TenantName")){
					  key = TENANT_NAME;
					  value = method.substring(method.indexOf("\"")+1, method.lastIndexOf("\""));
					  xml.append(key+"=\""+value+"\" ");
				  }
				  else 	if(method.contains("ServiceName")){
					  key = SERVICE_NAME;
					  value = method.substring(method.indexOf("\"")+1, method.lastIndexOf("\""));
					  xml.append(key+"=\""+value+"\">").append(System.lineSeparator());
				  }
				  else{
					  xml.append("\t").append("<criteria name=\"");
					  method = method.replace("set", "");
					  key = method.substring(0, method.indexOf("("));
					  value = method.substring(method.indexOf("(")+1, method.indexOf(")"));
					  xml.append(key).append("\" value=\"").append(value).append("\"/>").append(System.lineSeparator());
				  }
			  }
			  xml.append("</healthCheckStatistics>").append(System.lineSeparator());
		  }
		  System.out.println(xml);
	}
}
