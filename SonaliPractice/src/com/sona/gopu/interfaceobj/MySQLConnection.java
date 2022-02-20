package com.sona.gopu.interfaceobj;

public class MySQLConnection implements JDBCConnection {

	@Override
	public void connection() {
		System.out.println("MySQL connection is established");
	}

	public void prepareStatement(){
		System.out.println("MySQL prepare statement is created");
	}
}
