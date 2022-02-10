package com.testing;

import java.util.ArrayList;
import java.util.List;

public class HspHealthCheckCriteriaFactory {
    private String serviceName = "";
    private String tenantName = "";
    private String appType;
	
    private static final HspHealthCheckCriteriaFactory c = new HspHealthCheckCriteriaFactory();
    private static List<HspHealthCheckCriteriaFactory> factories = new ArrayList<HspHealthCheckCriteriaFactory>();
	static{
		System.out.println("in static block");
		HspHealthCheckCriteriaXMLParser2 d = c.new HspHealthCheckCriteriaXMLParser2();
		factories = d.getfactories();
	}
	
	public static List<HspHealthCheckCriteriaFactory> getEmployeeList() {
		return factories;
	}
	public static void main(String[] args) {
		System.out.println("in main");
	}
	
	private class HspHealthCheckCriteriaXMLParser2 {
		
		public HspHealthCheckCriteriaXMLParser2() {
			System.out.println("Inside HspHealthCheckCriteriaXMLParser2");
		}

		public List<HspHealthCheckCriteriaFactory> getfactories() {
			List<HspHealthCheckCriteriaFactory> factories = new ArrayList<HspHealthCheckCriteriaFactory>();
			HspHealthCheckCriteriaFactory f1 = new HspHealthCheckCriteriaFactory();
			return factories;
		}
	}
}
