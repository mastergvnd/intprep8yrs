package com.testing;

public class OperatorTest {

	public static void main(String[] args) {
		if(getTrue() && getFalse()){
			
		}
		
		if(getFalse() && getTrue()){
			
		}

	}

	private static boolean getTrue() {
		System.out.println("True");
		return true;
	}
	
	private static boolean getFalse() {
		System.out.println("False");
		return false;
	}
	
	

}
