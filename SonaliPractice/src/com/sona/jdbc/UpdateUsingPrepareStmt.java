package com.sona.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class UpdateUsingPrepareStmt {

	public static void main(String[] args) {
		try{  
			//step1 load the driver class  
			Class.forName("oracle.jdbc.driver.OracleDriver");  
			  
			//step2 create  the connection object  
			Connection con = null;
			con = DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:xe","system","password1");  
			  
			//step3 create the statement object  
			PreparedStatement stmt = con.prepareStatement("Update Family set ISMARRIED = ? where id = ?");
			stmt.setInt(1, 0);
			stmt.setInt(2, 2);
			  
			//step4 execute query  
			int count =stmt.executeUpdate();  
			
			System.out.println("count : " + count);
			
			//step5 close the connection object  
			con.close();  
			 
			}catch(Exception e){ 
				System.out.println(e);
			}
	}

}
