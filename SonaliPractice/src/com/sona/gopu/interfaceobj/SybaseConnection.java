package com.sona.gopu.interfaceobj;

public class SybaseConnection implements JDBCConnection {

	@Override
	public void connection() {
		System.out.println("Sybase connection is established");
	}
	public void prepareStatement(){
		System.out.println("Sybase prepare statement is created");
	}
}
