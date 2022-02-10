package nutanix;

import org.junit.Assert;

public class _A1FindMaximuminRange {

	//Min-Max Range Queries in Array
	public static void main(String[] args) {
		int ar[] = {1,5,8,9,13,6,3,7};//{1, 8, 5, 9, 6, 14, 2, 4, 3, 7};
		SegmentTree st = new SegmentTree();
		st.createSegmentTree(ar, ar.length);
		st.printSegmentTree();
		int rangeMax = st.getRangeMax(1,5,ar.length);
		System.out.println("rangeMax : " + rangeMax);
		Assert.assertEquals(13, st.getRangeMax(1,5,ar.length));
//		Assert.assertEquals(9, st.getRangeMax(0,3,ar.length));
//		Assert.assertEquals(1, st.getRangeMax(0,0,ar.length));
//		Assert.assertEquals(1, st.getRangeMax(0,0,ar.length));
	}

}
