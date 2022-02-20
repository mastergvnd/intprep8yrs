package arrays;

import java.util.PriorityQueue;

import org.junit.Assert;


public class _A5_KthLargestElelementFromStream {

	public static void main(String[] args) {
		KthLargest kth = new KthLargest(3, new int[]{4, 5, 8, 2});
		Assert.assertEquals("Wrong Answer", 4, kth.add(3));
		Assert.assertEquals("Wrong Answer", 5, kth.add(5));
		Assert.assertEquals("Wrong Answer", 5, kth.add(10));
		Assert.assertEquals("Wrong Answer", 8, kth.add(9));
		Assert.assertEquals("Wrong Answer", 8, kth.add(4));
	}

}

class KthLargest {

	PriorityQueue<Integer> minHeap = new PriorityQueue<>();
	int k;
    public KthLargest(int k, int[] nums) {
    	this.k = k;
    	for(int num : nums) {
    		add(num);
    	}
    	System.out.println(minHeap);
    }
    
    public int add(int val) {
    	if(minHeap.size() == k && minHeap.peek() > val)
			return minHeap.peek();
    	
    	if(minHeap.size() == k)
			minHeap.poll();
    	
    	minHeap.add(val);
    	
    	return minHeap.peek();
    }
}