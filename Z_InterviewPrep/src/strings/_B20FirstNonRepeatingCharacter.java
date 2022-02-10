package strings;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Assert;

public class _B20FirstNonRepeatingCharacter {
	public static void main(String[] args) {
		Assert.assertEquals("", 5, firstUniqChar("geeksforgeeks"));

	}

	public static int firstUniqChar(String s) {
		Map<Character, Integer> map = new LinkedHashMap<Character, Integer>();
		for (char c : s.toCharArray()) {
			map.put(c, map.getOrDefault(c, 0) + 1);
		}
		System.out.println(map);
		for(Map.Entry<Character, Integer> entry : map.entrySet()) {
			if(entry.getValue() == 1)
				return s.indexOf(entry.getKey());
		}
		return -1;
	}
}
