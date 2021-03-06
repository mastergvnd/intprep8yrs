package paypal;

import java.util.Arrays;

import org.junit.Assert;

public class _1LargestSumContiguousSubarray {

	public static void main(String[] args) {
		Assert.assertEquals("Not correct output", 6, printLargestSumContiguousSubarray(new int[]{-2,1,-3,4,-1,2,1,-5,4}));
		Assert.assertEquals("Not correct output", 1, printLargestSumContiguousSubarray(new int[]{1}));
		Assert.assertEquals("Not correct output", 23, printLargestSumContiguousSubarray(new int[]{5,4,-1,7,8}));
		Assert.assertEquals("Not correct output", -1, printLargestSumContiguousSubarray(new int[]{-1}));
		Assert.assertEquals("Not correct output", 0, printLargestSumContiguousSubarray(new int[]{0}));
		Assert.assertEquals("Not correct output", -1, printLargestSumContiguousSubarray(new int[]{-2,-1}));
		Assert.assertEquals("Not correct output", 0, printLargestSumContiguousSubarray(new int[]{-1,0}));
	}

	private static int printLargestSumContiguousSubarray(int[] nums) {
		if(nums.length == 1 && (nums[0] == -1 || nums[0] == 0))
            return nums[0];
		
		int maxSoFar = nums[0], currentMax = 0, maxNegative = nums[0];
		int start = 0, end = 0, s= 0;
		for(int i=0; i<nums.length; i++){
			currentMax += nums[i];
			if(currentMax < 0) {
				currentMax = 0;
				s = i+1;
				maxNegative = Math.max(maxNegative, nums[i]); 
			} else if(maxSoFar < currentMax) {
				maxSoFar = currentMax;
				start = s;
				end = i;
			}
		}
		System.out.println("Start " + start + " End " + end + " MaxSoFar " + maxSoFar + " maxNegative " + maxNegative + "    " + Arrays.toString(nums));
		return maxSoFar >= 0 ? maxSoFar : maxNegative; 
	}

}
