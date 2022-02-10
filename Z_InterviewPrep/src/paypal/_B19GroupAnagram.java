package paypal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;

public class _B19GroupAnagram {

	public static void main(String[] args) {
		List<ArrayList<String>> expected = new ArrayList<ArrayList<String>>();
		expected.add(new ArrayList<String>(Arrays.asList("eat", "tea", "ate")));
		expected.add(new ArrayList<String>(Arrays.asList("bat")));
		expected.add(new ArrayList<String>(Arrays.asList("tan", "nat")));
		
		Assert.assertEquals(expected, groupAnagram(new String[]{"eat","tea","tan","ate","nat","bat"}));
		
		Assert.assertEquals(expected, groupAnagram2(new String[]{"eat","tea","bat", "tan","ate","nat"}));
		System.out.println("Done");
	}

	private static List<List<String>> groupAnagram2(String[] strs) {
		if(strs == null || strs.length == 0)
			return null;
		Map<Map<Character, Integer>, List<String>> groups = new LinkedHashMap<Map<Character, Integer>, List<String>>();
		for(String str : strs) {
			Map<Character, Integer> strMap = createMapFromString(str);
			List<String> list = groups.getOrDefault(strMap, new ArrayList<String>());
			list.add(str);
			groups.put(strMap, list);
		}
		System.out.println(groups);
		return new ArrayList<List<String>>(groups.values());
	}
	
	private static Map<Character, Integer> createMapFromString(String str) {
		Map<Character, Integer> strMap = new HashMap<Character, Integer>();
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			int freq = strMap.containsKey(c) ? strMap.get(c) + 1 : 1;
			strMap.put(c, freq);
		}
		return strMap;
	}

	private static List<List<String>> groupAnagram(String[] strs) {
		if(strs == null || strs.length == 0)
			return null;
		HashMap<String, List<String>> map = new HashMap<String, List<String>>();
		for(String str : strs) {
			char[] s = str.toCharArray();
			Arrays.sort(s);
			String sorted = new String(s);
			List<String> list = map.getOrDefault(sorted, new ArrayList<String>());
			list.add(str);
			map.put(sorted, list);
		}
		return new ArrayList<List<String>>(map.values());
	}
}
