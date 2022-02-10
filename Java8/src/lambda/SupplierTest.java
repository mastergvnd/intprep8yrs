package lambda;

import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class SupplierTest {

	public static void main(String[] args) {
		Supplier<Person> supplier = () -> new Person("Govind", 22);
		Predicate<Person> predicate = p -> p.age > 18;
		System.out.println("Is Person eligible for voting : " + isPersonEligibleForVoting(supplier, predicate));
		
		IntSupplier supplier2 = () -> (int)(Math.random() * 10);
		System.out.println("Random Int value : " + supplier2.getAsInt());
		
		DoubleSupplier doubleSupplier = () -> (Math.random() * 10);
		System.out.println("Random double value : " + doubleSupplier.getAsDouble());
	}

	private static boolean isPersonEligibleForVoting(Supplier<Person> supplier, Predicate<Person> predicate) {
		return predicate.test(supplier.get());
	}

}
