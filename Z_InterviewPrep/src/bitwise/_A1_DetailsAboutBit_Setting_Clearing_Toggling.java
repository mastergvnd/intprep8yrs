package bitwise;

public class _A1_DetailsAboutBit_Setting_Clearing_Toggling {

	public static void main(String[] args) {
		int num = 34;
		//Setting a bit means that if K-th bit is 0, then set it to 1 and if it is 1 then leave it unchanged.
		//So for setting a bit, performing a bitwise OR of the number with a set bit is the best idea.
		//N = N | 1 << K, where K is the bit that is to be set. k starts with zero, which means least significant bit's position is 0.
		System.out.println("Demo about setting a particular bit in a number");
		System.out.println("------------------------------------------------------------------");
		System.out.println("Before Setting : " + num + " --> " + Integer.toBinaryString(num));
		num = num | 1 << 4;
		System.out.println("After  Setting : " + num + " --> " + Integer.toBinaryString(num));
		System.out.println("******************************************************************");
	
		//Clearing a bit means that if K-th bit is 1, then clear it to 0 and if it is 0 then leave it unchanged.
		//Since bitwise AND of any bit with a reset bit results in a reset bit, i.e.
		//Any bit <bitwise AND> Reset bit = Reset bit
			//which means,
			//0 & 0 = 0
			//1 & 0 = 0
		//So for clearing a bit, performing a bitwise AND of the number with a reset bit is the best idea.
		//n = n & ~(1 << k), where k is the bit that is to be cleared
		
		System.out.println("Demo about clearing a particular bit in a number");
		System.out.println("------------------------------------------------------------------");
		System.out.println("Before Clearing : " + num + " --> " + Integer.toBinaryString(num));
		num = num & ~(1 << 1);
		System.out.println("After  Clearing : " + num + " --> " + Integer.toBinaryString(num));
		System.out.println("******************************************************************");

		
		//Toggling a bit means that if K-th bit is 1, then change it to 0 and if it is 0 then change it to 1.
		//Since XOR of un-set and set bit results in a set bit and XOR of a set and set bit results in an un-set bit. 
		//Hence performing bitwise XOR of any bit with a set bit results in toggle of that bit, i.e.
		//Any bit <bitwise XOR> Set bit = Toggle
		//which means,
			//0 ^ 1 = 1
			//1 ^ 1 = 0
		//So in order to toggle a bit, performing a bitwise XOR of the number with a reset bit is the best idea.
		//n = n ^ 1 << k, where k is the bit that is to be toggled

		System.out.println("Demo about toggling a particular bit in a number");
		System.out.println("------------------------------------------------------------------");
		System.out.println("Before Toggling : " + num + " --> " + Integer.toBinaryString(num));
		num = num ^ 1 << 4;
		System.out.println("After  Toggling : " + num + " --> " + Integer.toBinaryString(num));
		System.out.println("******************************************************************");


	}

}
