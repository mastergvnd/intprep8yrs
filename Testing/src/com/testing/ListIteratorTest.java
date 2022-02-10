package com.testing;

import java.util.ArrayList;
import java.util.ListIterator;

public class ListIteratorTest {

	public static void main(String[] args) {
		ArrayList<String> list1 = new ArrayList<String>();
		list1.add("A");
		list1.add("A");
		list1.add("B");
		list1.add("C");
		list1.add("D");
		list1.add("D");
		list1.add("D");
		list1.add("D");
		list1.add("E");
		list1.add("E");
		
		//ArrayList<String> list2 = list1;
		ListIterator<String> li1 = list1.listIterator();
		while(li1.hasNext()){
			String first = li1.next();
			li1.remove();
			ListIterator<String> li2 = list1.listIterator();
			while(li2.hasNext()){
				String sec = li2.next();
				if(first.equals(sec)){
					System.out.println(first);
					li2.remove();
				}
			}
			li1 = list1.listIterator();
		}
	}

}
