package com.designPatterns.adapterPattern;

import java.util.Enumeration;
import java.util.Iterator;

public class EnumerationToIterator implements Iterator<String>{
	private Enumeration<String> enumeration;
	
	public EnumerationToIterator(Enumeration<String> enumeration) {
		this.enumeration = enumeration;
	}

	@Override
	public boolean hasNext() {
		return enumeration.hasMoreElements();
	}

	@Override
	public String next() {
		return enumeration.nextElement();
	}
	
	public void remove() {
		System.out.println("Inside");
		throw new UnsupportedOperationException();
	}
}
