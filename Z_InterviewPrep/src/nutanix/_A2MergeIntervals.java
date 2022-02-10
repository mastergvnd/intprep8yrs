package nutanix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class _A2MergeIntervals {

	public static void main(String[] args) {
		int intervals[][] = {{1,3}, {8,10}, {9,12}, {2,6}, {15,18}};
		
		getOverlappingIntervals(intervals);

	}

	private static int[][] getOverlappingIntervals(int[][] intervals) {
		Arrays.sort(intervals, new Comparator<int[]>() {
			@Override
			public int compare(int[] a, int[] b) {
				return a[0] - b[0];
			}
		});
		System.out.println(Arrays.deepToString(intervals));
		List<int[]> list = new ArrayList<int[]>();
		int[]current = intervals[0];
		for(int i = 1; i<intervals.length; i++) {
			int[] next = intervals[i];
			if(current[1] < next[0]) {
				list.add(current);
				current = next;
			} else {
				current[1] = Math.max(current[1], next[1]);
			}
		}
		list.add(current);
		System.out.println(Arrays.deepToString(list.toArray()));
		return list.toArray(new int[list.size()][]);
	}

}
