package MinimumSubsetSumDifference4;

public class BruteForce {

	public static void main(String[] args) {
		int array[] = {1, 2, 3, 9};
		
		System.out.println("Minimum difference is : " + minDiff(array));

	}

	private static int minDiff(int[] array) {
		return minDiffRecursive(array, 0, 0, 0);
		
	}

	private static int minDiffRecursive(int[] array, int currentIndex, int sum1, int sum2) {
		 if (currentIndex == array.length)
		      return Math.abs(sum1 - sum2);
		    int diff1 = minDiffRecursive(array, currentIndex+1, sum1+array[currentIndex], sum2);
		    int diff2 = minDiffRecursive(array, currentIndex+1, sum1, sum2+array[currentIndex]);

		    return Math.min(diff1, diff2);
	}

}
