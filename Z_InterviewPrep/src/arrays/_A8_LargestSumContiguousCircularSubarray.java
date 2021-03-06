package arrays;

import org.junit.Assert;

public class _A8_LargestSumContiguousCircularSubarray {

	public static void main(String[] args) {
		Assert.assertEquals("Not correct output", 22, printLargestSumContiguousSubarray(new int[]{8, -8, 9, -9, 10, -11, 12}));
		Assert.assertEquals("Not correct output", 1, printLargestSumContiguousSubarray(new int[]{1}));
		Assert.assertEquals("Not correct output", 24, printLargestSumContiguousSubarray(new int[]{5,4,-1,7,8}));
		Assert.assertEquals("Not correct output", -1, printLargestSumContiguousSubarray(new int[]{-1}));
		Assert.assertEquals("Not correct output", 0, printLargestSumContiguousSubarray(new int[]{0}));
		Assert.assertEquals("Not correct output", -1, printLargestSumContiguousSubarray(new int[]{-2,-1}));
		Assert.assertEquals("Not correct output", 0, printLargestSumContiguousSubarray(new int[]{-1,0}));
		Assert.assertEquals("Not correct output", 23, printLargestSumContiguousSubarray(new int[]{10, -3, -4, 7, 6, 5, -4, -1}));
		Assert.assertEquals("Not correct output", 52, printLargestSumContiguousSubarray(new int[]{-1, 40, -14, 7, 6, 5, -4, -1}));
		Assert.assertEquals("Not correct output", 15, printLargestSumContiguousSubarray(new int[]{3,1,3,2,6}));
		String s=  "Govind";
	}
	
	private static int printLargestSumContiguousSubarray(int[] nums) {
		if(nums.length == 1 && (nums[0] == -1 || nums[0] == 0))
            return nums[0];
		
		int maxSoFar = nums[0], currentMax = nums[0], minSoFar = nums[0], currentMin = nums[0], sum = nums[0];
		for(int i=1; i<nums.length; i++) {
			
			sum += nums[i];
			
			currentMax = Math.max(currentMax + nums[i], nums[i]);
			maxSoFar = Math.max(maxSoFar, currentMax);
			
			currentMin = Math.min(currentMin + nums[i],  nums[i]);
			minSoFar = Math.min(minSoFar, currentMin);
		}
		
		if(minSoFar == sum)
			return maxSoFar;
		
		return Math.max(maxSoFar, sum - minSoFar);
	}

}
