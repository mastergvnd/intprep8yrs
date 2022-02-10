package SubsetSum3;

public class BruteForce {

	public static void main(String[] args) {
		int[] array = {1,2,3,7};
		int sum = 6;
		System.out.println("Does the array has elements having sum 6 : " + doesHaveSubset(array, sum, 0));
		
		array = new int[]{1, 2, 7, 1, 5};
		sum = 10;
		System.out.println("Does the array has elements having sum 10 : " + doesHaveSubset(array, sum, 0));
		
		array = new int[]{1, 3, 4, 8};
		sum = 6;
		System.out.println("Does the array has elements having sum 6 : " + doesHaveSubset(array, sum, 0));
		
	}

	private static boolean doesHaveSubset(int[] array, int sum, int currentIndex) {
		if(sum == 0)
			return true;
		
		if(array.length == 0 || currentIndex >= array.length)
			return false;
		
		if(array[currentIndex] <= sum)
			if(doesHaveSubset(array, sum - array[currentIndex], currentIndex+1))
				return true;
		
		return doesHaveSubset(array, sum, currentIndex+1);
	}

}
