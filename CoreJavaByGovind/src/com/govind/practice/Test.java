package com.govind.practice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sun.xml.internal.ws.util.StringUtils;

public class Test {
	
	public void m1(int i) {
		System.out.println("int");
	}
	
	public void m1(float i) {
		System.out.println("float");
	}
	
	public void m1(String i) {
		System.out.println("String");
	}
	
	public void m1(StringBuffer i) {
		System.out.println("StringBuffer");
	}
	
	public void m1(Object i) {
		System.out.println("Object");
	}

	public static void main(String[] args) {
		Parent c = new Child();
		System.out.println(c.var);
		c.print();
		
		Test t = new Test();
		t.m1("Govind");
		
	}
}
