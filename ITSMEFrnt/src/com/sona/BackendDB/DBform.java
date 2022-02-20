package com.sona.BackendDB;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class DBform {
	
	public int generate(String uName, String pwd){
		int rowCount = 0;
		try{
			Class.forName("oracle.jdbc.driver.OracleDriver");
			Connection con = DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:xe","system","password1");
			PreparedStatement stmt = con.prepareStatement("insert into FormTable values(?,?)");
			stmt.setString(1, uName);
			stmt.setString(2, pwd);
			rowCount = stmt.executeUpdate();
			System.out.println("number of rows affected in DBForm : " + rowCount);
			con.close();
			return rowCount;
		} catch(Exception e) {
			System.out.println(e.getStackTrace());
		}
		return rowCount;
	}
}
