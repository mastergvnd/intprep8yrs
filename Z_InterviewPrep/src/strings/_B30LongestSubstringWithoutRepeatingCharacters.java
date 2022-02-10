package strings;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;

public class _B30LongestSubstringWithoutRepeatingCharacters {

//	3. Longest Substring Without Repeating Characters

	public static void main(String[] args) {
		Assert.assertEquals(3, lengthOfLongestSubstring("abcabcbb"));
		Assert.assertEquals(2, lengthOfLongestSubstring("abba"));
	}

	public static int lengthOfLongestSubstring(String s) {
		int left = 0, right = 0, length = 0;
		Map<Character, Integer> charIndex = new HashMap<Character, Integer>(); 
		while(right < s.length()) {
			final char c = s.charAt(right);
			if(charIndex.containsKey(c)) 
				left = Math.max(charIndex.get(c) + 1, left);
			charIndex.put(c, right);
			length = Math.max(length, right-left+1);
			right++;
		}
		return length;
	}

}
