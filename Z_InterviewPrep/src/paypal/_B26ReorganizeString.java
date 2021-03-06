package paypal;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

import org.junit.Assert;


public class _B26ReorganizeString {

	public static void main(String[] args) {

		String str = "aabcdbaabdeaaaaaa";
		Assert.assertEquals("ababdaeadcb", reorganizeString(str));
//		q.add(new AbstractMap.SimpleEntry<Character, Integer>('B', 2));
	}

	public static String reorganizeString(String s) {
		final Map<Character, Integer> frequencies = new HashMap<Character, Integer>();;
		for(char c : s.toCharArray())
			frequencies.put(c, frequencies.getOrDefault(c, 0) + 1);
		System.out.println(frequencies);
		PriorityQueue<Character> maxHeap = new PriorityQueue<>(
				new Comparator<Character>() {
					public int compare(Character arg0, Character arg1) {
						return frequencies.get(arg1) - frequencies.get(arg0);
					}
		});
		maxHeap.addAll(frequencies.keySet());
		
		StringBuilder sb = new StringBuilder();

		while(maxHeap.size() > 1) {
			Character current = maxHeap.poll();
			Character next = maxHeap.poll();
			sb.append(current);
			sb.append(next);
			frequencies.put(current, frequencies.get(current) - 1);
			frequencies.put(next, frequencies.get(next) - 1);
			if(frequencies.get(current) > 0)
				maxHeap.add(current);
			if(frequencies.get(next) > 0)
				maxHeap.add(next);
		}
		if(!maxHeap.isEmpty()) {
			Character last = maxHeap.remove();
			if(frequencies.get(last) > 1)
				return "";
			sb.append(last);
		}
		return sb.toString();
	}

//	private static Map<Character, Integer> getCharFrequency(String s) {
//		Map<Character, Integer> frequencies = new HashMap<Character, Integer>();
//		for(char c : s.toCharArray())
//			frequencies.put(c, frequencies.getOrDefault(c, 0) + 1);
//		return frequencies;
//	}
}
