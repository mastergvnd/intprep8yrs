package strings;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Assert;

public class _B21kthNonRepeatingCharacter {
	public static void main(String[] args) {
		Assert.assertEquals("Wrong answer", 'o', kthUniqChar("geeksforgeks", 2));

	}

	public static char kthUniqChar(String s, int k) {
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
			if(entry.getValue() != -1 && --k == 0)
				return entry.getKey();
				
		}
		return ' ';
	}
}
