package com.sona.arrayEx;

public class FirstArray {
public static void main(String args[]) {

		int a[] = new int[6];
		String []b = new String [5];
		int c[] = {41,89,34,23,2313,43};
		int d[] = new int[]{23,232,31,34};
		String family[] = {"Sonali","loves","Govind","a","lot"};
		
		
		System.out.println("Value of a array at index 1 is " +a[1]);
		System.out.println("Value of b array at index 1 is " +b[1]);
		System.out.println("Value of c array at index 1 is " +c[1]);
		System.out.println("Value of a array at index 1 is " +d[1]);
		System.out.println("Length of c array is " + c.length);
		
		int i=0;
		for (i=0;i<=c.length-1;i++)
		{
			System.out.println("The array values of c at index " +i +" is " +c[i]);
		}
		
		
		int m=0;
		for (m=0;m<=family.length-1;m++)
		{
			System.out.println("The array values of family array at index " +m +" is " +family[m]);
		}
		
		for(String f : family) {
			System.out.println(f);
		}
	}
}
