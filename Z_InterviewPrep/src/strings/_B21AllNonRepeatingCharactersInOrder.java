package strings;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Assert;

public class _B21AllNonRepeatingCharactersInOrder {
	public static void main(String[] args) {
		Assert.assertEquals("Wrong answer", "for", firstUniqChar("geeksforgeksg"));

	}

	public static String firstUniqChar(String s) {
		StringBuilder sb = new StringBuilder();
		Map<Character, Integer> map = new LinkedHashMap<Character, Integer>();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if(!map.containsKey(c))
				map.put(c, i);
			else
				map.put(c, -1);
		}
		System.out.println(map);
		for(Map.Entry<Character, Integer> entry : map.entrySet()) {
			if(entry.getValue() != -1)
				sb.append(entry.getKey());
		}
		return sb.toString();
	}
}
