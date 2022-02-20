package com.sona.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class InsertOrCreate {

	public static void main(String[] args) {
		try{  
			//step1 load the driver class  
			Class.forName("oracle.jdbc.driver.OracleDriver");  
			  
			//step2 create  the connection object  
			Connection con = null;
			con = DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:xe","system","password1");  
			  
			//step3 create the statement object  
			PreparedStatement stmt = con.prepareStatement("insert into Family values(?,?,?,?)");
			stmt.setInt(1, 1);
			stmt.setString(2, "Govind");
			stmt.setString(3, "Bangalore");
			stmt.setInt(4, 1);
			  
			//step4 execute query  
			int count =stmt.executeUpdate(); 
			
			System.out.println("count is  " + count);
			
			//step5 close the connection object  
			con.close();  
			 
			}catch(Exception e){ 
				System.out.println(e);
			}
	}

}
