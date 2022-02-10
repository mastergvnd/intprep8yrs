package defaultMethods;

public interface InterfaceB {
	
	default public void print() {
		System.out.println("InterfaceB print method");
	}

}
