
public class WellWisher {

	public static void wish(Greeting greeting) {
		greeting.greet();
	}
	public static void main(String[] args) {
		Greeting hindi = new HindiGreeting();
		wish(hindi);
		
		Greeting eng = new EnglishGreeting();
		wish(eng);
		
		wish(
			() -> System.out.println("Ola")
		);

	}

}
