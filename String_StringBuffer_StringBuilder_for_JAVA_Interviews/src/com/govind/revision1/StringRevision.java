package com.govind.revision1;

public class StringRevision {

	public static void main(String[] args) {
		String s = "govind";
		System.out.println(s.hashCode());
		s = s.concat(" Gupta");
		System.out.println(s.hashCode());
		//System.out.println(s2.hashCode());
		
		System.out.println("--------------------");
		StringBuffer sb = new StringBuffer("Govind");
		StringBuffer sb2 = sb.append("Gupta");
		System.out.println(sb.hashCode());
		System.out.println(sb2.hashCode());
		
		System.out.println("--------------------");
		String s1 = new String("Sonali");
		String s2 = new String("Sonali");
		System.out.println(s1.hashCode());
		System.out.println(s2.hashCode());
		System.out.println(s1.equals(s2));
		System.out.println(s1 == s2);
		
		System.out.println("--------------------");
		String s3 = "Kumar";
		String s4 = "Kumar";
		System.out.println(s3.hashCode());
		System.out.println(s4.hashCode());
		System.out.println(s3.equals(s4));
		System.out.println(s3 == s4);
		
		System.out.println("Govind");
		String str = "4294967295";
		System.out.println(Integer.parseInt(str));
	}
}
