package com.designPatterns.adapterPattern;

import java.util.Enumeration;

//Consumer1 or Client1
public class AmazonUS {
	public void displayCategories(Enumeration<String> prodCat) {
		while(prodCat.hasMoreElements()) {
			System.out.println(prodCat.nextElement());
		}
	}
}
