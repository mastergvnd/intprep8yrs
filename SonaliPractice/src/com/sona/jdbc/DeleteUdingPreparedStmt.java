package com.sona.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;

public class DeleteUdingPreparedStmt {
	public static void main(String[] args) {
		try{
			//step1 load the driver class  
			Class.forName("oracle.jdbc.driver.OracleDriver");  
			  
			//step2 create  the connection object  
			Connection con = null;
			con = DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:xe","system","password1");  
			  
			//step3 create the statement object  
			PreparedStatement stmt = con.prepareStatement("Delete from Family where name = ?");
			stmt.setString(1, "Sonali");
			
			//step4 execute query  
			int count =stmt.executeUpdate();
			
			System.out.println("Number of records affected : " + count);
		} catch(Exception e){
			System.out.println(e);
		}
	}
}
