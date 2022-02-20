package com.sona.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

public class Metadata {

	public static void main(String[] args) {
		try{  
			//step1 load the driver class  
			Class.forName("oracle.jdbc.driver.OracleDriver");  
			  
			//step2 create  the connection object  
			Connection con = null;
			con = DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:xe","system","password1");  
			  
			//step3 create the statement object  
			PreparedStatement stmt = con.prepareStatement("select * from family");
			ResultSet rs=stmt.executeQuery(); 
			ResultSetMetaData rsmd=rs.getMetaData();
			
			int i=rsmd.getColumnCount();
			System.out.println("Column count is :" +i );
			
			String j=rsmd.getColumnTypeName(1);
			System.out.println("column type is :" +j );
			
			String m=rsmd.getColumnName(1);
			System.out.println("column name is :" +j );
			  
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
