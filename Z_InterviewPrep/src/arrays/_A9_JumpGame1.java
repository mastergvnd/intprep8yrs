package arrays;

import org.junit.Assert;

public class _A9_JumpGame1 {
	// 55. Jump Game
	// You are given an integer array nums. You are initially positioned at the
	// array's first index, and each element in the array represents your
	// maximum jump length at that position.
	//
	// Return true if you can reach the last index, or false otherwise.
	// https://leetcode.com/problems/jump-game/

	public static void main(String[] args) {
		Assert.assertTrue("Wrong answer", canJump(new int []{1,1,2,5,2,1,0,0,1,3}));
		Assert.assertTrue("Wrong answer", canJump(new int []{ 2, 3, 1, 1, 4 }));
	}

	public static boolean canJump(int[] nums) {
		int reachable = 0;
		for(int i=0;i<nums.length; i++) {
			System.out.println(i + " " + reachable);
			if(reachable < i)
				return false;
			reachable = Math.max(reachable, i + nums[i]);
		}
		
		return true;
	}

}
