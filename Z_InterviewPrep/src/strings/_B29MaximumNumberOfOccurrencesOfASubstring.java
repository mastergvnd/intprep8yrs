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
        Deque<Character> queue = new LinkedList<Character>();
		Map<String, Integer> freq = new HashMap<String, Integer>();
		for(int i =0; i<minSize; i++)
			queue.add(s.charAt(i));
		for(int j=minSize; j<s.length(); j++) {
			if(isValidMaxLetters(queue, maxLetters)) {
				freq.put(queue.toString(), freq.getOrDefault(queue.toString(), 0) + 1);
			}
			queue.add(s.charAt(j));
			if(isValidMaxLetters(queue, maxLetters)) {
				freq.put(queue.toString(), freq.getOrDefault(queue.toString(), 0) + 1);
			}
			queue.removeFirst();
		}
		
		if(isValidMaxLetters(queue, maxLetters)) {
			freq.put(queue.toString(), freq.getOrDefault(queue.toString(), 0) + 1);
		}
		int count = 0;
		for(Map.Entry<String, Integer> entry : freq.entrySet()) 
			if(entry.getValue() > count)
				count = entry.getValue();
		return count;
    }

	private static boolean isValidMaxLetters(Deque<Character> queue, int maxLetters) {
		Set<Character> charCount = new HashSet<Character>();
		for(Character c : queue) {
			charCount.add(c);
		}
		return (charCount.size() <= maxLetters);
	}

}
