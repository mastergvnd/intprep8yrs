package com.testing;

public class MagicNumber {

	public static void main(String[] args) {
		int n = 121;
		printmagic(n);
	}

	private static void printmagic(int X) {
		for (int i =1; i <= X/2; i++){
			for (int j = X-i; j >= X/2; j--){
				if(i + j == X){
					if(i == rev(j) && (i !=j)){
						System.out.println(i + "   " + j);
					}
				}
			}
		}
	}

	private static int rev(int num) {
		int reversed = 0;
		while(num != 0) {
            int digit = num % 10;
            reversed = reversed * 10 + digit;
            num /= 10;
        }
		return reversed;
	}
}
