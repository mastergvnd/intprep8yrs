package com.testing;

//import org.apache.commons.beanutils.MethodUtils;

public class OuterClass {
	private int i = 7;
	private String name= "Govind";
	public int getI() {
		return i;
	}
	public String getName() {
		return name;
	}
	public void setI(int i) {
		this.i = i;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	private class InnerClass{
		private void accessMethod(){
			//MethodUtils.getAccessibleMethod(method);
			
		}
	}
}
