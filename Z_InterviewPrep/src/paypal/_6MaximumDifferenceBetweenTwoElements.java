package paypal;

import org.junit.Assert;

//Maximum difference between two elements such that larger element appears after the smaller number
public class _6MaximumDifferenceBetweenTwoElements {

	public static void main(String[] args) {
		Assert.assertEquals("The difference is not correct", 8, calculateMaximumDifferenceBetweenTwoWlements(new int[]{2, 3, 10, 6, 4, 8, 1}));
		Assert.assertEquals("The difference is not correct", 2, calculateMaximumDifferenceBetweenTwoWlements(new int[]{7, 9, 5, 6, 3, 2}));
		Assert.assertEquals("The difference is not correct", 1, calculateMaximumDifferenceBetweenTwoWlements(new int[]{1}));
		Assert.assertEquals("The difference is not correct", 1, calculateMaximumDifferenceBetweenTwoWlements(new int[]{1,2}));
		Assert.assertEquals("The difference is not correct", -1, calculateMaximumDifferenceBetweenTwoWlements(new int[]{6,5,4,3,2,1}));
		Assert.assertEquals("The difference is not correct", 0, calculateMaximumDifferenceBetweenTwoWlements(new int[]{2,2,2}));
	}

	private static int calculateMaximumDifferenceBetweenTwoWlements(int[] nums) {
		if(nums.length == 1)
			return nums[0];
		int maxDiff = nums[1]-nums[0];
		int minEle = nums[0];
		for(int i=1; i<nums.length; i++) {
			if(minEle > nums[i])
				minEle = nums[i];
			if(maxDiff < nums[i] - minEle)
				maxDiff = nums[i] - minEle;
		}
		System.out.println(maxDiff);
		return maxDiff == 0 ? -1 : maxDiff;
	}

}
