package paypal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;

public class _B18AnagramSubStringSearch {

	public static void main(String[] args) {
		
		Assert.assertArrayEquals(new Object[]{0,6}, searchAnagramSubstring("cbaebabacd", "abc"));
		Assert.assertArrayEquals(new Object[]{0,1,2}, searchAnagramSubstring("abab", "ab"));
		System.out.println(Arrays.toString(searchAnagramSubstring2("aaaaaaaaaaaaaaaaaaaa", "aaaaaaaaaa")));
		//Assert.assertArrayEquals(new Object[]{0,6}, searchAnagramSubstring2("aaaaaaaaaaaaaaaaaaaaaaa", "aa"));
		System.out.println("Done");
	}

	private static Object[] searchAnagramSubstring2(String s, String p) {
		if(p.length() > s.length()) 
			return new Object[0];
		ArrayList<Integer> totalCount = new ArrayList<Integer>();
		HashMap<Character, Integer> contentCount = new HashMap<Character, Integer>();
		HashMap<Character, Integer> searchCount = new HashMap<Character, Integer>();
		int startWindow = 0, endWindow = 0;
		
//        if(s.length() == 30000 && s.startsWith("a") && s.endsWith("a")) {
//            totalCount.add(0);
//            totalCount.add(10001);
//            return totalCount;
//        }
		
		while(endWindow < p.length()) {
			addCharToHashMap(contentCount, s.charAt(endWindow));
			addCharToHashMap(searchCount, p.charAt(endWindow++));
		}
		if(isSubStringAnagram(contentCount, searchCount))
			totalCount.add(startWindow);
		while(endWindow < s.length()) {
			addCharToHashMap(contentCount, s.charAt(endWindow++));
			removeCharFromHashMap(contentCount, s.charAt(startWindow++));

			if(isSubStringAnagram(contentCount, searchCount))
				totalCount.add(startWindow);
		}
		return totalCount.toArray();
	}

	private static void addCharToHashMap(HashMap<Character, Integer> contentCount, char c) {
		if(contentCount.get(c) != null)
			contentCount.put(c, contentCount.get(c) + 1);
		else
			contentCount.put(c, 1);
	}
	
	private static void removeCharFromHashMap(HashMap<Character, Integer> contentCount, char c) {
		if(contentCount.get(c) != null && contentCount.get(c) > 1)
			contentCount.put(c, contentCount.get(c) - 1);
		else
			contentCount.remove(c);
	}

	private static Object[] searchAnagramSubstring(String s, String p) {
		//This code is written using temprary array.
		if(p.length() > s.length()) 
			return new Object[0];
		
		ArrayList<Integer> totalCount = new ArrayList<Integer>();
		
		int[] contentCount = new int[26];
		int[] searchCount = new int[26];
		
		int startWindow = 0, endWindow = 0;
		while(endWindow < p.length()) {
			contentCount[s.charAt(endWindow) - 'a']++;
			searchCount[p.charAt(endWindow++) - 'a']++;
		}
		if(isSubStringAnagram(contentCount, searchCount))
			totalCount.add(startWindow);
		while(endWindow < s.length()) {
			contentCount[s.charAt(endWindow++) - 'a']++;
			contentCount[s.charAt(startWindow++) - 'a']--;
			if(isSubStringAnagram(contentCount, searchCount))
				totalCount.add(startWindow);
		}
		
		return totalCount.toArray();
	}

	private static boolean isSubStringAnagram(int[] contentCount, int[] searchCount) {
		for (int i = 0; i < searchCount.length; i++) {
			if(contentCount[i] != searchCount[i])
				return false;
		}
		return true;
	}
	
	private static boolean isSubStringAnagram(HashMap<Character, Integer> map1, HashMap<Character, Integer> map2) {
		for(Map.Entry<Character, Integer> entry : map1.entrySet())
			if(map2.get(entry.getKey()) != entry.getValue())
				return false;
		return true;
	}

}
