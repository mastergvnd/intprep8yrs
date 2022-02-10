package strings;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;

public class _B29MaximumNumberOfOccurrencesOfASubstring {

	// 1297. Maximum Number of Occurrences of a Substring

	public static void main(String[] args) {
		Assert.assertEquals(2, maxFreq("aababcaab", 2, 3, 4));
		Assert.assertEquals(2, maxFreq("aaaa", 1, 3, 3));
		Assert.assertEquals(0, maxFreq("abcde", 2, 3, 3));
	}

	public static int maxFreq(String s, int maxLetters, int minSize, int maxSize) {
		Map<String, Integer> freq = new HashMap<String, Integer>();
		for(int i=0, j=minSize; j<s.length(); i++, j++) {
			String subStr = s.substring(i, j)
			if(isValidMaxLetters(subStr, maxLetters)) {
				freq.put(subStr, freq.getOrDefault(subStr, 0) + 1);
			}
			subStr = s.substring(i, 0);
			if(isValidMaxLetters(subStr, maxLetters)) {
				freq.put(subStr, freq.getOrDefault(subStr, 0) + 1);
			}
			subStr = new StringBuilder(subStr);
			subStr.deleteCharAt(0);
		}
		subStr = new StringBuilder(subStr);
		System.out.println(subStr);
		if(isValidMaxLetters(subStr, maxLetters)) {
			freq.put(subStr, freq.getOrDefault(subStr, 0) + 1);
		}
		System.out.println(freq);
		int count = 0;
		for(Map.Entry<StringBuilder, Integer> entry : freq.entrySet()) 
			if(entry.getValue() > count)
				count = entry.getValue();
		return count;
	}

	private static boolean isValidMaxLetters(String subStr, int maxLetters) {
		Set<Character> set = new HashSet<Character>();
		for(int i=0; i<subStr.length(); i++) {
			set.add(subStr.charAt(i));
		}
		return (set.size() <= maxLetters);
	}

}
