package lambda;

import java.time.Period;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class PerdicateDemo {

	public static void main(String[] args) {
		Person person = new Person("Govind", 20);
		
		Predicate<Person> predicate = p -> p.age > 18;
		boolean isEligible = isPersonElegibleForVoting(person, predicate);
		System.out.println("Is person eligible for voting : " + isEligible);
		
		Predicate<Person> greaterThanEighteen = p -> p.age >= 18;
		Predicate<Person> lessThanSixty = p -> p.age <= 60;
		
		Predicate<Person> predicate2 = greaterThanEighteen.and(lessThanSixty);
		System.out.println("Is person eligible for club membership : " + isPersonEligibleForClubMembership(person, predicate2));
			
		BiPredicate<Person, Integer> predicate3 = (p, age) -> p.age > age;
		System.out.println("Is person eligible for voting : " + isPersonElegibleForVotingBiPredicate(person, 25, predicate3));
		
	}

	private static boolean isPersonElegibleForVotingBiPredicate(Person person, int i,BiPredicate<Person, Integer> predicate3) {
		return predicate3.test(person, i);
	}

	private static boolean isPersonEligibleForClubMembership(Person person, Predicate<Person> predicate) {
		return predicate.test(person);
	}

	private static boolean isPersonElegibleForVoting(Person person, Predicate<Person> predicate) {
		return predicate.test(person);
	}

}
