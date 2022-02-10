package com.testing;

public class Photo {

	public static void main(String[] args) {
		
	
		
		int [][] dim = {{2,10}, {1,9}, {3,7}};
		System.out.println(dim.length);
		int result[] = solve(dim);
		for(int n : result){
			System.out.println(n);
		}
		StringBuilder sb = new StringBuilder("Govind,Kumar,Gupta,");
		String udas = sb.toString().replaceAll(",$", "");
		System.out.println("Govind : "+ udas);
		int a = 1;
		char c = (char)a;
		System.out.println("Gupta : " + c);
				
	}

	private static int[] solve(int[][] dim) {
		int result[] = new int[dim.length];
		int max = 0;
		int sum=0;
		for(int i = 0; i<dim.length; i++){
			max=0; sum=0;
			for(int j = 0; j<dim.length; j++){
				if(j==i)
					continue;
				if(max < dim[j][1])
					max = dim[j][1];
				sum+=dim[j][0];
			}
			result[i] = sum*max;
		}
		return result;
	}
}
