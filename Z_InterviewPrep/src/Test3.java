import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Test3 {

	//PayTm Hackerrank
	public static void main(String[] args) {
		List<Integer> s = new ArrayList<Integer>(Arrays.asList(2,5,4,3));//6,4,3,4
		List<Integer> e = new ArrayList<Integer>(Arrays.asList(8,9,7,5));//8,9,7,5
		List<Integer> a = new ArrayList<Integer>(Arrays.asList(800,1600,200,400));//900,1600,2000,400
		System.out.println(getMaxKnowledge(10, s, e, a, 2));
	}
	
	public static int getMaxKnowledge(int d, List<Integer> s, List<Integer> e, List<Integer> a, int k) {
		int maxKnow = 0;
		int firstMax = 0;
		int secondMax = 0;
		int result[] = new int[k];
		
		for(int day = 1; day <= d; day ++) {
			for(int start=0; start < s.size(); start++) {
				if(s.get(start) >= day) {
					int temp = a.get(start);
					if(temp > firstMax && temp > secondMax) {
						secondMax = Math.max(firstMax, secondMax);
						firstMax = temp;
					} else if(temp > secondMax && temp < firstMax) {
						secondMax = Math.max(temp, secondMax);
					}
				}
				maxKnow = Math.max(maxKnow, firstMax + secondMax);
			}
		}
		return maxKnow;
	}
	
//	public static int getMaxKnowledge(int d, List<Integer> s, List<Integer> e, List<Integer> a, int k) {
//		int maxKnow = 0;
//		int firstMax = 0;
//		int secondMax = 0;
//		
//		for(int day = 1; day <= d; day ++) {
//			for(int start=0; start < s.size(); start++) {
//				if(s.get(start) >= day) {
//					int temp = a.get(start);
//					if(temp > firstMax && temp > secondMax) {
//						secondMax = Math.max(firstMax, secondMax);
//						firstMax = temp;
//					} else if(temp > secondMax && temp < firstMax) {
//						secondMax = Math.max(temp, secondMax);
//					}
//				}
//				maxKnow = Math.max(maxKnow, firstMax + secondMax);
//			}
//		}
//		
//		
//		return maxKnow;
//	}

}
