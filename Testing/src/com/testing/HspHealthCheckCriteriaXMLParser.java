package com.testing;

import java.util.ArrayList;
import java.util.List;

public class HspHealthCheckCriteriaXMLParser {
	
	public HspHealthCheckCriteriaXMLParser() {
		System.out.println("Inside HspHealthCheckCriteriaXMLParser");
	}

	public List<HspHealthCheckCriteriaFactory> getfactories() {
		List<HspHealthCheckCriteriaFactory> factories = new ArrayList<HspHealthCheckCriteriaFactory>();
		HspHealthCheckCriteriaFactory f1 = new HspHealthCheckCriteriaFactory();
		return factories;
	}
	
}
