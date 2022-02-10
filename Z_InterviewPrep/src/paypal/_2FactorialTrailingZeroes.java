package paypal;

import org.junit.Assert;

public class _2FactorialTrailingZeroes {

	public static void main(String[] args) {
		System.out.println("Start");
		Assert.assertEquals("Not correct", -1, printFactorialTrailingZeroes(0));
		Assert.assertEquals("Not correct", 0, printFactorialTrailingZeroes(3));
		Assert.assertEquals("Not correct", 1, printFactorialTrailingZeroes(5));
		Assert.assertEquals("Not correct", 1, printFactorialTrailingZeroes(6));
		Assert.assertEquals("Not correct", 1, printFactorialTrailingZeroes(7));
		Assert.assertEquals("Not correct", 1, printFactorialTrailingZeroes(8));
		Assert.assertEquals("Not correct", 1, printFactorialTrailingZeroes(9));
		Assert.assertEquals("Not correct", 2, printFactorialTrailingZeroes(10));
		Assert.assertEquals("Not correct", 3, printFactorialTrailingZeroes(15));
		Assert.assertEquals("Not correct", 4, printFactorialTrailingZeroes(20));
		Assert.assertEquals("Not correct", 4, printFactorialTrailingZeroes(23));
		Assert.assertEquals("Not correct", 7, printFactorialTrailingZeroes(30));
		Assert.assertEquals("Not correct", 9, printFactorialTrailingZeroes(40));
		Assert.assertEquals("Not correct", 12, printFactorialTrailingZeroes(50));
		Assert.assertEquals("Not correct", 14, printFactorialTrailingZeroes(60));
		Assert.assertEquals("Not correct", 16, printFactorialTrailingZeroes(70));
		Assert.assertEquals("Not correct", 19, printFactorialTrailingZeroes(80));
		Assert.assertEquals("Not correct", 21, printFactorialTrailingZeroes(90));
		Assert.assertEquals("Not correct", 24, printFactorialTrailingZeroes(100));
		Assert.assertEquals("Not correct", 28, printFactorialTrailingZeroes(123));
		Assert.assertEquals("Not correct", 277, printFactorialTrailingZeroes2(1123));
		
		System.out.println("Finish");
//		https://www.handakafunda.com/number-of-trailing-zeros/
	}

	private static int printFactorialTrailingZeroes(int num) {
		if(num == 0)
			return -1;
		int count = 0;
		for(int i=5; num/i >= 1; i*=5)
			count+=	num/i;
		
		return count;
	}
	
	private static int printFactorialTrailingZeroes2(int num) {
		if(num == 0)
			return -1;
		int count = 0;
		while(num>=1) {
			count+=	num/5;
			num = num/5;
		}
		
		return count;
	}

}
