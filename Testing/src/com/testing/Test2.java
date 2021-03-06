package com.testing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

public class Test2 {

	public static final String YEAR_TOTAL = "YearTotal";
    public static final String HSP = "HSP";
    public static final String UNDERSCORE = "_";
    public String PLAN_TYPE_COLUMN = "Plan Type (%s)";

	public static void main(String[] args) {
		
		System.out.println("----------------------------------------------------------");
		System.out.println(1 & 32767);
		System.out.println("----------------------------------------------------------");
		
		HashSet<String> missingSubVars = new HashSet<>();
		String mydata = "Cube refresh failed for Essbase Cube: missSubV, Message: \n" +
		"Error: Substitution variable [SubVar1] doesn't exist.\n" +
		"Error: Substitution variable [SubVar3] doesn't exist.\n" +
		"Error: Substitution variable [SubVar3] doesn't exist.\n" +
		"Error: Substitution variable [SubVar5] doesn't exist.\n" +
		"Error compiling formula for [G2N_Entity]: Unknown member [??] on line [10]\n" +
		"Error: Substitution variable [PPDYear] doesn't exist.";

		Pattern pattern = Pattern.compile("Substitution variable \\[(.*?)\\]");
		Matcher matcher = pattern.matcher(mydata);
		while (matcher.find()) {
			String value = matcher.group(1);
			missingSubVars.add(value);
			mydata = mydata.replace("Error: Substitution variable ["+value+"] doesn't exist.\n", "");
		}
		System.out.println(missingSubVars);
		System.out.println(mydata);
		System.out.println("----------------------------------------");
		String g = "GovindKumar";
		String p = g.replaceFirst("(?i)govind", "GGGovind");
		System.out.println(g);
		System.out.println(p);
		
		for(int i = 0; i<5; i++) {
			String t = HSP + UNDERSCORE + YEAR_TOTAL;
	        System.out.println(compare(t, HSP + UNDERSCORE + YEAR_TOTAL));
		}
        System.out.println("----------------------------------------------------------");
        
        final String HSP = "HSP";
        String t1 = HSP + UNDERSCORE + YEAR_TOTAL;
        System.out.println(compare(t1, HSP + UNDERSCORE + YEAR_TOTAL));
        System.out.println("----------------------------------------------------------");
        
        String HSP2 = "HSP";
        String t2 = HSP2 + UNDERSCORE + YEAR_TOTAL;
        System.out.println(compare(t2, HSP2 + UNDERSCORE + YEAR_TOTAL));
        System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        List<String> ar = new ArrayList<String>();
        ar.add("Cube1");
//        ar.add("Plan2");
//        ar.add("MulCube");
        System.out.println(getPlanTypeHeaders(ar));
        getplanTypesRowValues(ar);
        System.out.println("111111111111111111111111111111111111111111111111111111111111111111111111111111111111111");
        System.out.println(0 & 32767);
        
        Test2 t = new Test2();
        String planTypeColumnFinalValue = String.format(t.PLAN_TYPE_COLUMN, "Cube1");
        System.out.println("planTypeColumnFinalValue : " + planTypeColumnFinalValue);
        
        System.out.println(3 ^ 4);
        List<String> al1 = new ArrayList();
        al1.add("Default");
        al1.add("English");
        al1.add("French");
        
        List<String> al2 = new ArrayList();
        al2.add("Default");
        al2.add("Long Names");
        al2.add("French");
        al2.removeAll(al1);
        System.out.println(al1 + "" + al2);
	}
	
	static boolean compare(String t1, String t2) {
        boolean b = t1 == t2;
        return b;
    }
	
    public static String getPlanTypeHeaders(List<String> cubesName) {
        StringBuilder planTypeheaders = new StringBuilder();
        for(String cubeName : cubesName) {
            planTypeheaders.append("Plan Type (").append(cubeName).append(")").append(",");
        }
        return planTypeheaders.toString().replaceAll(",$", ""); //removing last separator ","
    }
    
    public static String getplanTypesRowValues(List<String> planTypeColumns) {
        List<String> planTypesRow = new ArrayList<String>(Collections.nCopies(planTypeColumns.size(), "FALSE"));
        for(String planTypeColumn : planTypeColumns) {
            if(planTypeColumn.equals("Cube1")) {
                planTypesRow.set(planTypeColumns.indexOf(planTypeColumn), "TRUE");
            }
        }
        String planTypesRowValues = StringUtils.join(planTypesRow.iterator(), ",");
        System.out.println(planTypesRowValues);
        return planTypesRowValues;
    }

}
