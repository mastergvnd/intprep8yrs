package bitwise;

public class _A2_ToggleBitsOfANumber {

	public static void main(String[] args) {
		int num = 18;
		System.out.println("Before Toggling : " + num + " --> " + Integer.toBinaryString(63));
		System.out.println("After  Toggling : " + num + " --> " + Integer.toBinaryString(toggleAllBitsOfANumber(63)));
		System.out.println("After  Toggling : " + num + " --> " + Integer.toBinaryString(toggleAlternateBitsOfANumber(63)));
	}
	
	private static int toggleAllBitsOfANumber(int num) {
		int temp = 1;
		while(temp <= num) {
			num = num ^ temp;
			temp = temp << 1;
		}
		return num;
	}
	
	private static int toggleAlternateBitsOfANumber(int num) {
		int temp = 1;
		while(temp <= num) {
			num = num ^ temp;
			temp = temp << 2;
		}
		return num;
	}

}
