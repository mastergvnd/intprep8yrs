package arrays;

import java.util.Arrays;

public class _A1MoveNegativeToEndWithoutAffactingOrder {

//	input : 5, -4, 3, -2, 6, -11, 12, -8, 9
//	output: 5, 3, 6, 12, 9, -4, -2, -11, -8 (negative numbers moved to end)
	public static void main(String[] args) {
		int ar[] = {5,-4,3,-2,6,-11,12,-8,9};
		moveNegativeToEndWithOrderCharge(ar);
		System.out.println(Arrays.toString(ar));
	}
	
	

	private static void moveNegativeToEndWithOrderCharge(int[] nums) {
		int start = 0, end = nums.length-1;
		while(start < end) {
			while(nums[start] > 0)
				start++;
			while(nums[end] < 0)
				end --;
			if(start < end) {
				int temp = nums[start];
				nums[start] = nums[end];
				nums[end] = temp;
			}
		}
		System.out.println(Arrays.toString(nums));
	}

}
