package arrays;

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;


public class _A2_LongestConsecutiveSequence {

	public static void main(String[] args) {
		int ar[] = {100,4,200,1,3,2};
		Assert.assertEquals(4, getLongestConsecutiveSequence(ar));
	}

	private static int getLongestConsecutiveSequence(int[] nums) {
		Set<Integer> set = new HashSet<Integer>(nums.length);
		for(int num : nums)
			set.add(num);
		int longestSeq = 0;
		for(int num : nums) {
			if(!set.contains(num-1)) {
				int count = 1;
				while(set.contains(num++ + 1))
					count++;
				longestSeq = Math.max(longestSeq, count);
			}
		}
		System.out.println(longestSeq);
		return longestSeq;
	}

}
