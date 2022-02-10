package com.designPatterns.adapterPattern;

import java.util.Enumeration;
import java.util.Iterator;

public class IteratorToEnumration implements Enumeration<String>{
	Iterator<String> iterator;
	
	public IteratorToEnumration(Iterator<String> iterator) {
		this.iterator = iterator;
	}
	@Override
	public boolean hasMoreElements() {
		return iterator.hasNext();
	}

	@Override
	public String nextElement() {
		return iterator.next();
	}
	
	public void remove() {
		iterator.remove();
	}
	
}
