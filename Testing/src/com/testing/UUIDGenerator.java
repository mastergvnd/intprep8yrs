package com.testing;

import java.util.HashMap;
import java.util.UUID;

public class UUIDGenerator {
	
	private static HashMap<String, String> HM = createHashMap();
	
	public static void main(String[] args) {
		
		System.out.println(UUID.randomUUID());  
		System.out.println(UUID.randomUUID());
		System.out.println(UUID.randomUUID());
		System.out.println(HM.get("Govind"));
//		System.out.println("Approval Unit Submission Group : " + UUID.fromString("Approva lUnit Submission Group"));
//		System.out.println("Approval Unit Hierarchy : " + UUID.fromString("Approval Unit Hierarchy"));
//		System.out.println("Cell Level Security Access : " + UUID.fromString("Cell Level Security Access"));
	}

	private static HashMap<String, String> createHashMap() {
		System.out.println("Insode init method");
		HashMap<String, String> bb = new HashMap<>();
		bb.put("Govind", "Gupta");
		return bb;
	}
}
