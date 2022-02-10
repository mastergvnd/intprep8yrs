package com.testing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestCSV {

    protected static final String COMMA = ",";

    protected static final String DOUBLE_QUOTES = "\"";
	
	public static void main(String[] args) throws IOException {
		List<String> list = new ArrayList<>();
		list.add("Govind");
		list.add("Kumar");
		list.add("Gupta");
//		String l = StringUtils.join(list.toArray(), "\",\"");
//		System.out.println(l);
		
		String commaSeparatedAlias = "Govind,Kumar,Gupta,Oracle,";
		System.out.println(commaSeparatedAlias.replaceAll(",$", ""));
		System.out.println(new StringBuilder(DOUBLE_QUOTES).append(COMMA).append(DOUBLE_QUOTES).toString());
		
		String s = "408 Cowboy Way (US Highway 82)";
		for (int i = 0; i < s.length(); i++) {
			if(!Character.isJavaIdentifierPart(s.charAt(i))){
				System.out.println(s.charAt(i));
			}
		}
		
        String e = "408 Cowboy Way (US_Highway 82)";
        System.out.println(e);
        String e1 = eval(e);
        System.out.print("Val : "+ e1);
        
//        File file = new File("C:\\Users\\govgupta.ORADEV\\Desktop\\OP\\Member field types\\duplicate_Entries.txt"); 
//        BufferedReader br = new BufferedReader(new FileReader(file)); 
//        String st; 
//        HashSet<String> entries = new HashSet<String>();
//        while ((st = br.readLine()) != null) {
//        	if(!entries.add(st)) {
//        		System.out.println(st);
//        	}
//        } 
	}
	
	private static String eval(String e) {
        StringBuilder sb = new StringBuilder(e);
        for(int i = 0; i< sb.length(); i++) {
        	if (!Character.isJavaIdentifierStart(sb.charAt(0))) {
        		sb.deleteCharAt(0);
        	}
        }

        System.out.println("First :  "+ sb);
        for (int i = 0; i < sb.length(); i++){
            if (!Character.isJavaIdentifierPart(sb.charAt(i))) {
            	if(sb.charAt(i) == ' ')
            		sb.replace(i, i+1, "_");
            	else
            		sb.deleteCharAt(i);
                i--;
            }
        }
        return sb.toString();
    }

}
