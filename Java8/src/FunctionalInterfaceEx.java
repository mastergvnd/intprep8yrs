
@FunctionalInterface
public interface FunctionalInterfaceEx {
	abstract void print();
	
	default void run() {
		System.out.println("Run");
	}
	
	default void sit() {
		System.out.println("Sit");
	}
}
