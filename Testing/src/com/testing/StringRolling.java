package com.testing;

public class StringRolling {
	public static void main(String[] args) {
		String s = rollingString("abc", new String[]{"0 0 L", "2 2 L", "0 2 R"});
		System.out.println(s);
	}
	
	static String rollingString(String s, String[] operations){
		StringBuilder str = new StringBuilder(s);
		for(String operation : operations){
			String opArray[] = operation.split(" ");
			int start = Integer.parseInt(opArray[0]);
			int end = Integer.parseInt(opArray[1]);
			String op = opArray[2];
			if(op.equals("L")){
				rollBackWard(start, end, str);
			}else if (op.equals("R")){
				rollForward(start, end, str);
			}
		}
		return str.toString();
	}

	private static void rollBackWard(int start, int end, StringBuilder str) {
		while(start <= end){
			char c = str.charAt(start);
			/*if(c == 'a'){
				c = 'z';
			}else{
				c = (char)(((c-'a'-1)%26)+'a');
			}*/
			c = (char)(((((c-'a'-1)%26)+'a')%97)+97);
			str.setCharAt(start, c);
			start++;
		}
		System.out.println("rollBackWard : "+str);
	}

	private static void rollForward(int start, int end, StringBuilder str) {
		while(start <= end){
			char c = str.charAt(start);
			if(c == 'z')
				c = 'a';
			else
				c = (char)(((c-'a'+1)%26)+'a');
			str.setCharAt(start, c);
			start++;
		}
		System.out.println("rollForward : "+str);
	}
}
