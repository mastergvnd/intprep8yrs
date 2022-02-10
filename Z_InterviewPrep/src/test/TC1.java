package test;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class TC1 {

	public static void main(String[] args) {
		List<Integer> list = new ArrayList<Integer>();
		list.add(1);
		list.add(2);
		list.add(3);
		list.add(4);
		System.out.println("Before update : " + list + list.size());
		list.add(1, 8);
		System.out.println("After add : " + list);
		System.out.println(list.set(1, 10));
		System.out.println("After setting : " + list);
		java.util.LinkedList<Integer> ll = new java.util.LinkedList<>();
		
		HashMap<String, String> hm = new HashMap<String, String>();
		hm.put("Govind", "Sonali");
		hm.put("Shivam", "Shivani");
		hm.put("Hemant", "Neha");
		
		System.out.println("Map : " + hm + hm.size());
		
		Iterator<Entry<String, String>> itr = hm.entrySet().iterator();
		while(itr.hasNext()) {
			Map.Entry<String, String> e = (Map.Entry<String, String>) itr.next();
			if(e.getKey().equals("Hemant"))
				itr.remove();
		}
		System.out.println("New Map : " + hm);
		
		Iterator<String> itr1 = hm.keySet().iterator();
		Iterator<String> itr2 = hm.keySet().iterator();
		itr1.remove();
		itr2.remove();
		
		String st = new Timestamp(System.currentTimeMillis()).toString();
		System.out.println(st);
		
	}

}
