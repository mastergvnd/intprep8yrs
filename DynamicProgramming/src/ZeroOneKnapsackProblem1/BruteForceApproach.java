package ZeroOneKnapsackProblem1;

public class BruteForceApproach {
	
	static int numberOfRecursiveSteps = 0;

	public static void main(String[] args) {
		int[] profits = {1,6,10,6,5};
		int[] weights = {1,2,3,5,6};
		
		int maxProfit = solveKnapsack(profits, weights, 7);
		
		System.out.println("Maximum Profit : " + maxProfit);
		System.out.println("Number of recursive calls : " + numberOfRecursiveSteps);
	}

	private static int solveKnapsack(int[] profits, int[] weights, int capacity) {
		return solveKnapsackRecursive(profits, weights, capacity, 0);
	}

	private static int solveKnapsackRecursive(int[] profits, int[] weights, int capacity, int currentIndex) {
		numberOfRecursiveSteps++;
		if(capacity == 0 || currentIndex >= profits.length)
			return 0;

		
		int profit1 = 0;
		if(weights[currentIndex] <= capacity)
			profit1 = profits[currentIndex] + solveKnapsackRecursive(profits, weights, capacity - weights[currentIndex], currentIndex + 1);
		
		int profit2 = solveKnapsackRecursive(profits, weights, capacity, currentIndex + 1);
		
		return Math.max(profit1, profit2);
	}

}
