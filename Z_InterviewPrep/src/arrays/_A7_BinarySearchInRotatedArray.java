package arrays;

import org.junit.Assert;

public class _A7_BinarySearchInRotatedArray {

	public static void main(String[] args) {
		Assert.assertEquals("Wrong answer", 5, binarySearch(new int[]{50, 60, 70, 80, 10, 20, 30, 40}, 20));
		Assert.assertEquals("Wrong answer", 4, binarySearch(new int[]{4,5,6,7,0,1,2}, 0));
		Assert.assertEquals("Wrong answer", 1, binarySearch(new int[]{3, 1}, 1));
	}
	
	private static int binarySearch(int nums[], int target) {
		int low = 0, high = nums.length-1;
		
		while(low <= high) {
			int mid = (low + high) / 2;
			if(nums[mid] == target)
				return mid;
			if(nums[low] <= nums[mid]) {
				if(nums[low] <= target && nums[mid] >= target)
					high = mid - 1;
				else
					low = mid + 1;
			} else {
				if(nums[mid] <= target && nums[high] >= target)
					low = mid + 1;
				else
					high = mid - 1;
			}
		}
		
		return -1;
	}

}
