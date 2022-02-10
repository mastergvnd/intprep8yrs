package EqualSubsetSumPartition2;

public class BruteForce {

	public static void main(String[] args) {
		int array[] = new int[]{1,2,3,4};
		System.out.println("Can array be partitioned into two equal sub-arrays : " + canPartition(array));
		
		array = new int[]{1, 1, 3, 4, 7};		
		System.out.println("Can array be partitioned into two equal sub-arrays : " + canPartition(array));
		
		array = new int[]{2, 3, 4, 6};		
		System.out.println("Can array be partitioned into two equal sub-arrays : " + canPartition(array));

	}

	private static boolean canPartition(int[] array) {
		int sum = 0;
		for(int i=0;i<array.length; i++)
			sum += array[i];
		
	    if(sum % 2 != 0)
	        return false;
		
		return canPartitionRecursive(array, sum/2, 0);
	}

	private static boolean canPartitionRecursive(int[] array, int sum, int currentIndex) {
		if(sum == 0)
			return true;
		
		if(array.length == 0 || array.length <= currentIndex)
			return false;
		
		if(array[currentIndex] <= sum)
			if(canPartitionRecursive(array, sum-array[currentIndex], currentIndex + 1))
				return true;
		
		return canPartitionRecursive(array, sum, currentIndex+1);
	}

}
