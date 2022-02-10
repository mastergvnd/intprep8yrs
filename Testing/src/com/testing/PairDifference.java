package com.testing;

public class PairDifference {

	static void findPair(int arr[], int n){
		
	    int size = arr.length; 
	    //int i = 0, j = 1; 
	  
	    /*while (i < size && j < size) 
	    { 
	    	if (i != j && arr[j]-arr[i] == n) 
	    	{ 
	    		System.out.print("Pair Found: "+"( "+arr[i]+", "+ arr[j]+" )"); 
	    		break;
	        } 
	        else if (arr[j] - arr[i] < n) 
	                j++; 
	        else
	                i++; 
	    }*/
	    int count = 0;
	    int min = 5000;
	    for (int i = 0; i < size; i = i+2)  
        { 
            for (int j = i + 1; j < size; j++) 
                if (arr[i] - arr[j] == n || arr[j] - arr[i] == n) 
                    count++;
            	if(count < min)
            		min = count;
        } 
	    System.out.println(count);
	}

	public static void main(String[] args) {
		int arr[] = {1,2,3,5};
		int n = 4;
		findPair(arr, n);
	}

}
