package paypal;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Test {

	public static void main(String[] args) {
		Integer []ar = new Integer[26];
		System.out.println(Arrays.toString(ar));
		Arrays.fill(ar, 0);
		System.out.println(Arrays.toString(ar));
		System.out.println("1 << 10 : " + (1 << 10));
		List<String> list = new ArrayList<>();
		list.add("Govind");
		System.out.println("Result : " + list.stream().filter(s -> s.equals("Govind")));
		BigInteger b = BigInteger.valueOf(3);
		System.out.println("B : " + b);
		String a = "Govind";
	}

}
