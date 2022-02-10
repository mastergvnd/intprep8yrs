package nutanix;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;

public class _A4RemoveDuplicateLetters {

	public static void main(String[] args) {
		Assert.assertEquals("acdb", getRemovedDuplicateLetters("cbacdcbc"));
		Assert.assertEquals("abc", getRemovedDuplicateLetters("bcabc"));
	}

	private static String getRemovedDuplicateLetters(String s) {
		int freq[] = new int[26];
		boolean isAdded[] = new boolean[26];
		
		for(char c : s.toCharArray()) {
			freq[c - 'a']++;
		}
		List<Character> result = new ArrayList<Character>();
		for(char current : s.toCharArray()) {
			
			freq[current - 'a'] -= 1;
			
			if(isAdded[current - 'a'])
				continue;
			
			while(!result.isEmpty() && result.get(result.size() - 1) > current && freq[result.get(result.size() - 1) - 'a'] > 0) {
				char lastChar = result.remove(result.size() - 1);
				isAdded[lastChar - 'a'] = false;
			}
			
			result.add(current);
			isAdded[current - 'a'] = true;
		}
		return result.toString().replaceAll("[,\\s\\[\\]]", "");
	}

}
