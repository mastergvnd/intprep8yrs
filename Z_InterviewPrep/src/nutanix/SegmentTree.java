package nutanix;

import java.util.Arrays;

public class SegmentTree {
	int st[];
	
	public void createSegmentTree(int ar[], int n) {
		int height = (int) Math.ceil(Math.log(n)/Math.log(2));
		int stLength = (int) (2*(Math.pow(2, height)) -1);
		System.out.println("stLength : " + stLength);
		st = new int[stLength];
		
		generateSegmentTree(ar, 0, n-1, 0);
	}
	
	public void printSegmentTree() {
		System.out.println(Arrays.toString(st));
	}
	
	public int generateSegmentTree(int[] ar, int ss, int se, int stIndex) {
		if(ss == se) {
			st[stIndex] = ar[ss];
			return ar[ss];
		}
		int mid = getMid(ss, se);
		st[stIndex] = Math.max(generateSegmentTree(ar, ss, mid, (stIndex*2) +1), generateSegmentTree(ar, mid+1, se, (stIndex*2)+2));
		
		return st[stIndex];
	}

	private static int getMid(int ss, int se) {
		return ss + ((se-ss)/2);
	}

	public int getRangeMax(int qs, int qe, int n) {
		if(qs < 0 ||  qe > n || qs > qe)
			return -1;
		System.out.println("qs " + "qe " + "ss " + "se " + "index");
		return getRangeMaxUtil(qs, qe, 0, n-1, 0);
	}

	private int getRangeMaxUtil(int qs, int qe, int ss, int se, int index) {
		System.out.print(qs + "  " + qe + "  " + ss + "  " + se + "  ");
		if(qs <= ss && qe >= se) {
			System.out.println(index);
			return st[index];
		} else
			System.out.println();
		if(qs > se || qe < ss)
			return Integer.MIN_VALUE;
		int mid = getMid(ss, se);
		return Math.max(getRangeMaxUtil(qs, qe, ss, mid, 2*index+1), getRangeMaxUtil(qs, qe, mid+1, se, 2*index+2));
	}
}
