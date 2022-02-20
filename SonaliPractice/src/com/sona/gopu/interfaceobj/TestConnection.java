package com.sona.gopu.interfaceobj;

public class TestConnection {
	
	public static void main(String[] args) {
		
	JDBCConnection conn = DriverManager.getConnection("sybase");
	conn.connection();
	
	conn.prepareStatement();
		
	}

}
