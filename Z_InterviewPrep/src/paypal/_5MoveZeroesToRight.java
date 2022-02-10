package paypal;

import java.util.Arrays;

public class _5MoveZeroesToRight {

	public static void main(String[] args) {
		int ar[] = {0,1,0,3,12};
		System.out.println(Arrays.toString(ar));
		moveZeroesToRight(ar);
	}

	private static void moveZeroesToRight(int[] ar) {
		int count = 0;
		for(int i=0; i<ar.length; i++)
			if(ar[i] != 0) 
				ar[count++] = ar[i];
		while(count < ar.length)
			ar[count++] = 0;

		System.out.println(Arrays.toString(ar));
		
	}

}
