package ZeroOneKnapsackProblem1;

public class BottomUpApproach {

	public static void main(String[] args) {
		int[] profits = {1,6,10,16};
		int[] weights = {1,2,3,5};
		
		int maxProfit = solveKnapsack(profits, weights, 7);
		System.out.println("Maximum Profit : " + maxProfit);
	}

	private static int solveKnapsack(int[] profits, int[] weights, int capacity) {
		
		if(weights.length <= 0 || capacity == 0 || weights.length != profits.length)
			return 0;
		
		int dp[][] = new int[profits.length][capacity + 1];
		
		for(int i=0; i<profits.length; i++)
			dp[i][0] = 0;
		
		for(int c=0; c<=capacity; c++)
			if(weights[0] <= c)
				dp[0][c] = profits[0];
				
		for(int i=1; i<profits.length; i++) {
			for(int c=1; c<=capacity; c++) {
				int profit1 = 0, profit2 = 0;
				if(weights[i] <= c)
					profit1 = profits[i] + dp[i-1][c-weights[i]];
				
				profit2 = dp[i-1][c];
				
				dp[i][c] = Math.max(profit1, profit2);
			}
		}
		
		
		printMetrix(weights, capacity, dp);
		
		printSelectedElements(dp, weights, profits, capacity);
		
		return dp[weights.length-1][capacity];
	}

	private static void printMetrix(int[] weights, int capacity, int[][] dp) {
		for(int i=0; i<weights.length; i++){
			for(int j=0; j<=capacity; j++){
				System.out.print(dp[i][j] + " ");
			}
			System.out.println();
		}
	}
	
	 private static void printSelectedElements(int dp[][], int[] weights, int[] profits, int capacity){
		   System.out.print("Selected weights:");
		   int totalProfit = dp[weights.length-1][capacity];
		   for(int i=weights.length-1; i > 0; i--) {
		     if(totalProfit != dp[i-1][capacity]) {
		       System.out.print(" " + weights[i]);
		       capacity -= weights[i];
		       totalProfit -= profits[i];
		     }
		   }

		   if(totalProfit != 0)
		     System.out.print(" " + weights[0]);
		   System.out.println("");
		 }

}
