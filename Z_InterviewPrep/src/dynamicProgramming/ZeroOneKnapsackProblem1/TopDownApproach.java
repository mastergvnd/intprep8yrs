package dynamicProgramming.ZeroOneKnapsackProblem1;

public class TopDownApproach {
	
	static int numberOfRecursiveSteps = 0;

	public static void main(String[] args) {
		int[] profits = {1,6,10,6,5};
		int[] weights = {1,2,3,5,6};
		
		int maxProfit = solveKnapsack(profits, weights, 7);
		System.out.println("Maximum Profit : " + maxProfit);
		System.out.println("Number of recursive calls : " + numberOfRecursiveSteps);
	}

	private static int solveKnapsack(int[] profits, int[] weights, int capacity) {
		Integer[][] memoize = new Integer[profits.length][capacity + 1];
		return solveKnapsackRecursive(memoize, profits, weights, capacity, 0);
	}

	private static int solveKnapsackRecursive(Integer[][] memoize, int[] profits, int[] weights, int capacity, int currentIndex) {
		numberOfRecursiveSteps++;
		if(capacity == 0 || profits.length <= currentIndex)
			return 0;
		
		if(memoize[currentIndex][capacity] != null)
			return memoize[currentIndex][capacity];
		
		int profit1 = 0;
		if(weights[currentIndex] <= capacity)
			profit1 = profits[currentIndex] + solveKnapsackRecursive(memoize, profits, weights, capacity - weights[currentIndex], currentIndex + 1);
		
		int profit2 = solveKnapsackRecursive(memoize, profits, weights, capacity, currentIndex + 1);
		memoize[currentIndex][capacity] = Math.max(profit1, profit2);; 
		
		return memoize[currentIndex][capacity];
	}

}
