package com.sona.gopu.interfaceobj;

public class OracleConnection implements JDBCConnection {

	@Override
	public void connection() {
		
		System.out.println("Oracle connection is established");
		
	}
	
	public void prepareStatement(){
		System.out.println("Oracle prepare statement is created");
	}
	
	

}
