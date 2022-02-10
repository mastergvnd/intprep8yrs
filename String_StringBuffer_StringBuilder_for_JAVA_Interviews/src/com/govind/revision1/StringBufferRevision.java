package com.govind.revision1;

public class StringBufferRevision {

	public static void main(String[] args) {
		StringBuffer sb1 = new StringBuffer("Govind");
		StringBuffer sb2 = new StringBuffer("Govind");
		System.out.println(sb1 == sb2);
		System.out.println(sb1.equals(sb2));
	}

}
