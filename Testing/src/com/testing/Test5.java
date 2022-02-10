package com.testing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

public class Test5 {

	public static void main(String[] args) {
		List<String> list1 = new ArrayList<String>();
		list1.add("Plan1");
		list1.add("Basic");
		
		List<String> list2 = new ArrayList<String>();
		list2.add("Basic");
		list2.add("Test1");
		
		System.out.println(list1);
		System.out.println(list2);
		
		System.out.println(list1.retainAll(list2));
		
		System.out.println(list1);
		System.out.println(list2);
		List<days> dayss = new LinkedList<days>(Arrays.asList(days.values()));
		dayss.remove(days.sun);
		Collections.swap(dayss, dayss.indexOf(days.mon), dayss.indexOf(days.tues));
		System.out.println(dayss);
		System.out.println(days.values()[0]);
	}
	
	public enum days{
		sun,
		mon,
		tues,
		wed
	}

}
