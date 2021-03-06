package arrays;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import org.junit.Assert;

//https://leetcode.com/problems/maximum-profit-in-job-scheduling/

public class _A4_MaximumProfitInJobScheduling {

	public static void main(String[] args) {
		Solution solution = new Solution();
		int maxProfit = solution.jobScheduling(new int[]{1,2,3,3}, new int[]{3,4,5,6}, new int[]{50,10,40,70});
		Assert.assertEquals("Wrong answer", 120, maxProfit);
	}

}

class Solution {
	
	private class Job{
		int startTime;
		int endTime;
		int profit;
		
		Job(int startTime, int endTime, int profit) {
			this.startTime = startTime;
			this.endTime = endTime;
			this.profit = profit;
		}
		
		@Override
		public String toString() {
			return startTime + " " + endTime + " " + profit;
		}
	}
    public int jobScheduling(int[] startTime, int[] endTime, int[] profit) {
       List<Job> jobs = new ArrayList<Job>();
    	for(int i=0; i<startTime.length; i++) {
        	jobs.add(new Job(startTime[i], endTime[i], profit[i]));
        }
    	
    	Collections.sort(jobs, (a,b) -> a.endTime - b.endTime);
    	TreeMap<Integer, Integer> dp = new TreeMap<>();
    	dp.put(0, 0);
    	for(Job job : jobs) {
    		int val = job.profit + dp.floorEntry(job.startTime).getValue();
    		if(val > dp.lastEntry().getValue())
    			dp.put(job.endTime, val);
    	}
    	System.out.println("Max Profit : " + dp.lastEntry().getValue());
    	return dp.lastEntry().getValue();
    }
}