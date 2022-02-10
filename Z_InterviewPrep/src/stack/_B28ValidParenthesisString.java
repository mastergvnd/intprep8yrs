package stack;

import java.util.Stack;

import org.junit.Assert;

public class _B28ValidParenthesisString {

	public static void main(String[] args) {
		Assert.assertEquals(Boolean.TRUE, isValid("()"));
		Assert.assertEquals(Boolean.TRUE, isValid("(*)"));
		Assert.assertEquals(Boolean.TRUE, isValid("(*))"));
		Assert.assertEquals(Boolean.FALSE, isValid(")"));
		Assert.assertEquals(Boolean.FALSE, isValid("("));
		Assert.assertEquals(Boolean.TRUE, isValid("((((()(()()()*()(((((*)()*(**(())))))(())()())(((())())())))))))(((((())*)))()))(()((*()*(*)))(*)()"));
		Assert.assertEquals(Boolean.FALSE, isValid("(((((()*)(*)*))())())(()())())))((**)))))(()())()"));
		Assert.assertEquals(Boolean.FALSE, isValid("(((((*(()((((*((**(((()()*)()()()*((((**)())*)*)))))))(())(()))())((*()()(((()((()*(())*(()**)()(())"));
		Assert.assertEquals(Boolean.TRUE, isValid("()*()"));
		Assert.assertEquals(Boolean.FALSE, isValid("()()()((((()((()(()())(()))(())))((()((()())*(((())()))(()((())(((((((())()*)())((())*))))*)())()))"));
	}
	
	public static boolean isValid(String s, int a) {
		int leftBrOrStar = 0, left = 0;
		for(char c : s.toCharArray()) {
			
			//Actual Logic // use either this block or shortcut block
			if(c == ')') {
				if(leftBrOrStar == 0 )
					return false;
				leftBrOrStar--;
			} else 
				leftBrOrStar++;
			
			if(c == '(')
				left++;
			else {
				left--;
				left = Math.max(0, left);
			}
			//Actual logic end
			
			//shortcut
			leftBrOrStar += (c == ')' ? -1 : 1);
			if(leftBrOrStar < 0) return false;
			
			left += (c == '(' ? 1 : -1);
			left = Math.max(0, left);
			//shortcut end
		}
		return left == 0;
	}

	public static boolean isValid(String s) {
		Stack<Integer> starStack = new Stack<Integer>();
		Stack<Integer> openBrStack = new Stack<Integer>();
		for(int i=0; i<s.length(); i++) {
			char c = s.charAt(i);
			if( c == '(')
				openBrStack.push(i);
			else if (c ==  '*')
				starStack.push(i);
			else {
				if(openBrStack.isEmpty() && starStack.isEmpty())
					return false;
				if(!openBrStack.isEmpty())
					openBrStack.pop();
				else
					starStack.pop();
			}
		}
		while(!openBrStack.isEmpty()) {
			if(starStack.isEmpty())
				return false;
			if(openBrStack.pop() > starStack.pop())
				return false;
		}
		return true;
	}

}
