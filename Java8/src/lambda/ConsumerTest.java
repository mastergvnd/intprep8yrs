package lambda;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ConsumerTest {

	public static void main(String[] args) {
		Consumer<String> consumer1 = (param) -> System.out.println(param + " my name is govind");
		consumer1.accept("Hi!");
		
		Consumer<String> consumer2 = (param) -> System.out.println(param + " I am from Bangalore");
		consumer1.andThen(consumer2).accept("Hello");
		
		BiConsumer<String, String> biconsumer = (s1, s2) -> System.out.println("My name is " + s1 + " and i am from " + s2);
		biconsumer.accept("Govind", "Bangalore");
	}

}
