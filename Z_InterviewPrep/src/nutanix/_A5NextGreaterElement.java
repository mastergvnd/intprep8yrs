package nutanix;

import java.util.Arrays;
import java.util.Stack;

public class _A5NextGreaterElement {

	// Next Greater Element of each element.
	public static void main(String[] args) {
		int ar[] = {4, 12, 5, 3, 1, 2, 5, 3, 1, 2, 4, 6};
		int result[] = getNextGreaterElement(ar);
		System.out.println(Arrays.toString(result));
	}

	private static int[] getNextGreaterElement(int[] ar) {
		int[] result = new int[ar.length];
		Stack<Integer> stack = new Stack<Integer>();
		for(int i=ar.length -1; i>=0; i--) {
			if(stack.isEmpty()) {
				stack.push(ar[i]);
				result [i] = -1;
				continue;
			}
			while(!stack.isEmpty() && stack.peek() <= ar[i])
				stack.pop();
			result[i] = !stack.isEmpty() ? stack.peek() : -1;
			stack.push(ar[i]);
		}
		return result;
	}

}
