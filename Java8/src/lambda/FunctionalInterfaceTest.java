package lambda;

import java.util.Random;
import java.util.function.Function;

public class FunctionalInterfaceTest {

	public static void main(String[] args) {
		Function<String, Integer> function = (s1) -> s1.length();
		System.out.println("Length of string is : " + function.apply("Govind")); 
		
		String s = "20";
		System.out.println(Integer.valueOf(s));
		
		System.out.println(Math.random());
		System.out.println(new Random().nextInt());
		System.out.println(new Random().nextInt(10000));
		
		System.out.println(String.format("%04d", Integer.parseInt(Integer.toBinaryString((1 ^ 2 ^ 4) ^ 4))));
		System.out.println(String.format("%04d", Integer.parseInt(Integer.toBinaryString((1 ^ 2) ^ 1))));
		System.out.println(String.format("%04d", Integer.parseInt(Integer.toBinaryString(15 | 1))));
	}

}
