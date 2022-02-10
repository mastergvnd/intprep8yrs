package defaultMethods;

public interface InterfaceA {
	default public void print() {
		System.out.println("InterfaceA print method");
	}
}
