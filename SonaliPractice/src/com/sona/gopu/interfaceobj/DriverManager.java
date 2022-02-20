package com.sona.gopu.interfaceobj;

public class DriverManager {

	public static JDBCConnection getConnection(String dbType) {
		JDBCConnection jc = null;
		if (dbType.equalsIgnoreCase("oracle")) {
			jc = new OracleConnection();
		} else if (dbType.equalsIgnoreCase("Mysql")) {
			jc = new MySQLConnection();
		} else if(dbType.equalsIgnoreCase("Sybase")) {
			jc = new SybaseConnection();
		} else {
			System.out.println("Connection not available " + dbType);
		}
		return jc;
	}
}
