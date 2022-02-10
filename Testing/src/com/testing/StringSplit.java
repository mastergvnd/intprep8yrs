package com.testing;

import java.util.Arrays;
import java.util.Vector;

public class StringSplit {

	//static String namesAsString = "ACCOUNTING TRANSFERS,CIVIL CONSTR.,COMPLEX PROCUREMENT,COMPUTERS, DESKTOP,COMPUTERS, HARDWARE MAINTENANC,COMPUTERS, SOFTWARE LICENSES,COMPUTERS, SOFTWARE MAINTENANC,CONSTRUCTION ENGINEERING,EXEMPT - ACCOUNTING TRANSFERS,EXEMPT - CIVIL CONSTR.,EXEMPT - COMPLEX PROCUREMENT,EXEMPT - MATERIAL PURCH.,EXEMPT - PROFESSIONAL SERVICES,EXEMPT - SUB-CONTRACT SERV.,FEES,MATERIAL PURCHASES,PROCARD PURCHASES,PROFESSIONAL SERVICES,PURCHASED SERVICES,RESERVE M&S,SPARE PARTS/OTHER ISSUES,SPEC. PROCESS SPARES ISS,STORES ISSUES,SUB-CONTRACT SERVICES,SUB-CONTRACT SVCS PASS THRU,T&M CONSTRUCTION SVCS.,T&M ELECTRICAL SERVICES,T&M PIPE FITTERS,T&M RIGGING SERVICES,TEMPORARY HELP,TRAVEL, DOMESTIC-LAB EMPLOYEE,TRAVEL, FOREIGN - NON EMPLOYEE,TRAVEL, FOREIGN-LAB EMPLOYEE"; 
//	static String namesAsString = "govind,\"kumar\", gupta";
//	static String namesAsString = "[DimensionName].[ComputerHardware], [DimensionName].[ComputerHardware]";
//	static String namesAsString = "\"[DimensionName1].[Computer1,Hardware1,RAM1]\",[DimensionName2].[Computer2,Hardware2,RAM2],\"[DimensionName3].[Computer3,Hardware3]\"";
	static String namesAsString = "[DimensionName1].[Computer1,Hardware1],[DimensionName2].[Computer2],[DimensionName3].[Computer3,Hardware3]"; // fails
	public static void main(String[] args) {
		System.out.println(namesAsString);
		System.out.println("----------------------------------------------------------------------------------------------------------------------------");
		Vector<String> names = parseNamesFromString(namesAsString, false);
		for (String o : names) {
		    System.out.println(o + " ");
		}
	}
    public static Vector<String> parseNamesFromString(String NamesAsString, boolean stripQuotes) {
        if (NamesAsString == null) {
            return (null);
        }
        Vector<String> names = new Vector<String>();
        int start = 0;
        int nextComma = NamesAsString.indexOf(",", start);
        int nextQuote = -1;
        String piece = null;
        while ((nextComma > 0) && (nextComma < NamesAsString.length())) {
            piece = NamesAsString.substring(start, nextComma);
            if (piece.startsWith("\"")) {
                nextQuote = NamesAsString.indexOf("\"", start + 1);
                if ((nextQuote > 0) && (nextQuote < NamesAsString.length())) {
                    if (stripQuotes) {
                        piece = NamesAsString.substring(start + 1, nextQuote);
                    } else {
                        piece = NamesAsString.substring(start, nextQuote + 1);
                    }
                    addTrimmedStringToVector(piece, names);
                } else {
                    throw new java.lang.RuntimeException("Mismatched quotes");
                }
                nextComma = NamesAsString.indexOf(",", nextQuote + 1);
                if (nextComma > 0) {
                    start = nextComma + 1;
                } else {
                    start = -1;
                }
            } else if (piece.startsWith("[")){
            	if(!piece.endsWith("]")){
            		nextQuote = NamesAsString.indexOf("]", nextComma + 1);
	            	if((nextQuote > 0) && (nextQuote < NamesAsString.length())){
	                    if (stripQuotes) {
	                        piece = NamesAsString.substring(start + 1, nextQuote);
	                    } else {
	                        piece = NamesAsString.substring(start, nextQuote + 1);
	                    }
	            	}
            	}
            	addTrimmedStringToVector(piece, names);
            	nextComma = NamesAsString.indexOf(",", nextQuote + 1);
            	if (nextComma > 0) {
            		start = nextComma + 1;
            	} else {
            		start = -1;
            	}
            } else {
                nextQuote = NamesAsString.indexOf("\"", start + 1);
                if ((nextQuote > 0) && (nextQuote < nextComma)) {
                    nextQuote = NamesAsString.indexOf("\"", nextQuote + 1);
                    if ((nextQuote > 0) && (nextQuote < NamesAsString.length())) {
                        nextComma = NamesAsString.indexOf(",", nextQuote + 1);
                        if ((nextComma > 0) && (nextComma < NamesAsString.length())) {
                            piece = NamesAsString.substring(start, nextComma);
                            start = nextComma + 1;
                            addTrimmedStringToVector(piece, names, stripQuotes);
                        } else {
                            piece = NamesAsString.substring(start);
                            addTrimmedStringToVector(piece, names, stripQuotes);
                            start = -1;
                            break;
                        }
                    } else {
                        throw new java.lang.RuntimeException("Mismatched quotes");
                    }
                } else {
                    addTrimmedStringToVector(piece, names, stripQuotes);
                    start = nextComma + 1;
                }
            }
            if (start >= 0) {
                nextComma = NamesAsString.indexOf(",", start);
            } else {
                break;
            }
        }
        if ((start >= 0) && (start < NamesAsString.length())) {
            String whatsLeft = NamesAsString.substring(start).trim();
            if (whatsLeft.length() > 0) {
                if (stripQuotes) {
                    if (whatsLeft.startsWith("\"")) {
                        nextQuote = whatsLeft.indexOf("\"", 1);
                        if ((nextQuote > 0) && (nextQuote < whatsLeft.length())) {
                            whatsLeft = whatsLeft.substring(1, nextQuote);
                        } else {
                            throw new java.lang.RuntimeException("Mismatched quotes");
                        }
                    }
                }
                //names.addElement(whatsLeft);
                addTrimmedStringToVector(whatsLeft, names);
            }
        }
        return (names);
    }
    public static void addTrimmedStringToVector(String name, Vector nameVector) {
        nameVector.addElement(name.trim());
    }
    public static void addTrimmedStringToVector(String name, Vector nameVector, boolean stripQuotes) {
        if (stripQuotes) {
            nameVector.addElement(removeQuotes(name.trim()));
        } else {
            nameVector.addElement(name.trim());
        }
    }
    public static String removeQuotes(String s) {
        String doubleQuote = "\"";
        //@todo - this needs much more specific logic - for now, just remove double quotes on either side
        if ((s != null) && s.startsWith(doubleQuote) && s.endsWith(doubleQuote))
            return s.substring(doubleQuote.length(), s.length() - doubleQuote.length());
        else
            return s;
    }
}
