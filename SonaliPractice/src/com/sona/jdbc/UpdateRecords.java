package com.sona.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class UpdateRecords {

	public static void main(String[] args) {
		try{  
			//step1 load the driver class  
			Class.forName("oracle.jdbc.driver.OracleDriver");  
			  
			//step2 create  the connection object  
			Connection con = null;
			con = DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:xe","system","password1");  
			  
			//step3 create the statement object  
			Statement stmt = con.createStatement();  
			  
			//step4 execute query  
			int count =stmt.executeUpdate("update Family set ISMARRIED='1' where ID='2'" );  
			
			System.out.println("count : " + count);
			
			//step5 close the connection object  
			con.close();  
			 
			}catch(Exception e){ 
				System.out.println(e);
			}
	}

}
