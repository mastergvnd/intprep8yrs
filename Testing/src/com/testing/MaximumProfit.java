package com.testing;

public class MaximumProfit {

	public static void main(String[] args) {
		int a[] = {1,2,3,4,9,8,24, 1};
		int maxProfit = solve(a);
		System.out.println("Max Profit : "+maxProfit);
	}

	private static int solve(int[] p) {
		int sum = p[0];
		int i = 0;
		int j = 1;
		while(j < p.length){
			if(p[j] % p[i] == 0){
				sum+=p[j];
				i=j;
			} 
			j++;
		}
		return sum;
	}

}
