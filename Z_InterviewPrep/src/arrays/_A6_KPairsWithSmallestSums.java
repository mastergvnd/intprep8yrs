package arrays;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;

public class _A6_KPairsWithSmallestSums {

	public static void main(String[] args) {
		getKPairsWithSmallestSums(new int[]{1,7,11}, new int[]{2,4,6}, 3);
	}
	
	public static List<List<Integer>> getKPairsWithSmallestSums(int array1[], int array2[], int k) {
		
//		PriorityQueue<Integer> minHeap1 = new PriorityQueue<Integer>();
//		for(int num : array1)
//			minHeap1.add(num);
//		
//		PriorityQueue<Integer> minHeap2 = new PriorityQueue<Integer>();
//		for(int num : array2)
//			minHeap2.add(num);
//		
//		List<List<Integer>> result = new ArrayList<List<Integer>>();
		PriorityQueue<List<Integer>> q = new PriorityQueue<>((a, b) -> (a.get(0) + a.get(1) - (b.get(0) + b.get(1))));
//		
//		for(int i=0; i<k; i++) {
//			int element1 = minHeap1.poll();
//			int element2 = minHeap2.poll();
//			result.add(new ArrayList<Integer>(Arrays.asList(element1, element2)));
//		}

		for(int i=0; i<array1.length;i++) 
				q.add(new ArrayList<Integer>(Arrays.asList(array1[i], array2[0])));
		while(!q.isEmpty())
			System.out.print(q.poll() + "  ");
		
		System.out.println();
//		System.out.println("result : " + result);
		return null;
		
	}

}
