package nutanix;

import java.util.Arrays;
import java.util.Stack;

public class _A6NextGreaterElement2 {

	// Next Greater Element
	public static void main(String[] args) {
		int ar[] = {4, 12, 5, 3, 1, 2, 5, 3, 1, 2, 4, 6};
		int result[] = getNextGreaterElement(ar);
		System.out.println(Arrays.toString(result));
	}

	private static int[] getNextGreaterElement(int[] ar) {
		int length  = ar.length;
		int[] result = new int[ar.length];
		Stack<Integer> stack = new Stack<Integer>();
		for(int i=2 * length-1; i>=0; i--) {
			while(!stack.isEmpty() && stack.peek() <= ar[i % length])
				stack.pop();
			result[i%length] = stack.isEmpty() ? -1 : stack.peek();
			stack.push(ar[i % length]);
		}
		return result;
	}

}
