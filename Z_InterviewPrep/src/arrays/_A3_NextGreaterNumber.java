package arrays;

import org.junit.Assert;

//Input:  n = "218765"
//Output: "251678"
//
//Input:  n = "1234"
//Output: "1243"
//
//Input: n = "4321"
//Output: "Not Possible"
//
//Input: n = "534976"
//Output: "536479"
//https://leetcode.com/problems/next-greater-element-iii/


public class _A3_NextGreaterNumber {

	public static void main(String[] args) {
		Assert.assertEquals("Wrong Number",251678, getNextGreaterNumber(218765));
		Assert.assertEquals("Wrong Number",21, getNextGreaterNumber(12));
		Assert.assertEquals("Wrong Number",-1, getNextGreaterNumber(21));
		Assert.assertEquals("Wrong Number",-1, getNextGreaterNumber(2147483486));
	}

	private static int getNextGreaterNumber(int num) {
		char[] array = Integer.toString(num).toCharArray();
		
		int firstSwapIndex = array.length - 1;
		while(firstSwapIndex > 0) {
			if(array[firstSwapIndex] > array[firstSwapIndex - 1])
				break;
			firstSwapIndex--;
		}
		
		if(firstSwapIndex == 0)
			return -1;
		
		firstSwapIndex--;
		
		
		int secondSwappingIndex = array.length - 1;
		while(secondSwappingIndex >= firstSwapIndex) {
			if(array[secondSwappingIndex] > array[firstSwapIndex])
				break;
			secondSwappingIndex--;
		}
		System.out.println(firstSwapIndex + " " + secondSwappingIndex);
		
		swap(array, firstSwapIndex, secondSwappingIndex);
		reverse(array, firstSwapIndex + 1);
		
		System.out.println(array);
		Long number = Long.parseLong(new String(array));
		return number > Integer.MAX_VALUE ? -1 : number.intValue();
	}
	
	private static void reverse(char[] array, int i) {
		int start = i;
		int end = array.length - 1;
		while(start < end) {
			swap(array, start, end);
			start++;
			end--;
		}
		
	}

	private static void swap(char[] array, int i, int j) {
		char temp = array[i];
		array[i] = array[j];
		array[j] = temp;
	}

}
