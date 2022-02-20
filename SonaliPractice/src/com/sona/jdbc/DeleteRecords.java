package com.sona.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DeleteRecords {
	
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
			int count =stmt.executeUpdate("delete from Family where name='Govind'");
			
			System.out.println("Number of records affected : " + count);
		} catch(Exception e){
			System.out.println(e);
		}
	}
	
}
