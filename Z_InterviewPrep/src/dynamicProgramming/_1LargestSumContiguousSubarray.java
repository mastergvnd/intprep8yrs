package dynamicProgramming;

//PayPal
//Kadane’s Algorithm can be viewed both as a greedy and DP. As we can see that we are keeping a running sum of integers and when it becomes less than 0, we reset it to 0 (Greedy Part). 
//This is because continuing with a negative sum is way more worse than restarting with a new range. Now it can also be viewed as a DP, at each stage we have 2 choices: 
	//Either take the current element and continue with previous sum OR restart a new range. These both choices are being taken care of in the implementation. 
public class _1LargestSumContiguousSubarray {

	public static void main(String[] args) {
		int ar[] = {-2, -3, 4, -1, -2, 1, 5, -3};
		System.out.println(printLargestSumContiguousSubarray(ar));

	}

	private static int printLargestSumContiguousSubarray(int[] ar) {
		
		int maxSoFar = 0, currentMax = 0;
		int start = 0, end = 0, s= 0;
		for(int i=0; i<ar.length; i++){
			currentMax += ar[i];
			if(currentMax < 0) {
				currentMax = 0;
				s = i+1;
			} else if(maxSoFar < currentMax) {
				maxSoFar = currentMax;
				start = s;
				end = i;
			}
			
		}
		System.out.println("Start " + start + " End " + end);
		return maxSoFar;
	}

}
