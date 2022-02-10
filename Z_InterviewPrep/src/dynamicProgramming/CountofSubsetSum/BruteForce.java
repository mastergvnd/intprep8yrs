package dynamicProgramming.CountofSubsetSum;

public class BruteForce {

	public static void main(String[] args) {
		int array[] = {1, 1, 2, 3};
		int sum = 4;
		System.out.println("Number of subsets : " + countSubSet(array, sum));
		
		array = new int[]{1, 2, 7, 1, 5};
		sum = 9;
		System.out.println("Number of subsets : " + countSubSet(array, sum));
	}

	private static int countSubSet(int[] array, int sum) {
		return countSubsetRecursive(array, sum, 0);
	}

	private static int countSubsetRecursive(int[] array, int sum, int currentIndex) {
		if(sum == 0)
			return 1;
		
		if(array.length == 0 || currentIndex >= array.length)
			return 0;
		
		int sum1 = 0;
		if(sum >= array[currentIndex])
			sum1 = countSubsetRecursive(array, sum - array[currentIndex], currentIndex + 1);
		
		int sum2 = countSubsetRecursive(array, sum, currentIndex + 1);
		
		return sum1 + sum2;
	}

}
