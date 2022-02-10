package paypal;

import java.util.Stack;

import org.junit.Assert;

public class _B27BalancedParanthesis {

	public static void main(String[] args) {
		Assert.assertEquals(Boolean.TRUE, isValid("()[]{}"));
		Assert.assertEquals(Boolean.FALSE, isValid("["));
		Assert.assertEquals(Boolean.FALSE, isValid("]"));
	}

	public static boolean isValid(String s) {
		Stack<Character> stack = new Stack<Character>();
		for(Character c : s.toCharArray()) {
			if(c == '[' || c == '(' || c == '{')
				stack.push(c);
			else {
				if(stack.isEmpty())
					return false;
				Character pop = stack.pop();
				if(c == ')' && pop != '(') 
					return false;
				else if (c == ']' && pop != '[')
					return false;
				else if(c == '}' && pop != '{')
					return false;
			}
		}
		return !(stack.isEmpty()) ? Boolean.FALSE : Boolean.TRUE;
	}

}
