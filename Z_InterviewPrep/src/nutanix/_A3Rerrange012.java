package nutanix;

import java.util.Arrays;

import org.junit.Assert;

public class _A3Rerrange012 {

	public static void main(String[] args) {
		int ar[] = {0,1,2,1,0,1,1,0,2,1};
		Assert.assertArrayEquals(new int[]{0,0,0,1,1,1,1,1,2,2}, getReaarangedArray(ar));

	}

	private static int[] getReaarangedArray(int[] ar) {
		int l=0, r = ar.length-1, i=0;
		
		while(i < r) {
			if(ar[i] == 0) {
				swap(i, l, ar);
				l++;
			} else if (ar[i] == 2) {
				swap(i,r,ar);
				i--;
				r--;
			}
			
			System.out.println(Arrays.toString(ar) + " " + i + " " + l + " " + r);
			i++;
		}
		System.out.println(Arrays.toString(ar));
		return ar;
	}
	
	private static void swap(int a, int b, int ar[]) {
		int temp = ar[a];
		ar[a] = ar[b];
		ar[b] = temp;
	}

}
