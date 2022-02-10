package defaultMethods;

public class Test implements InterfaceA, InterfaceB{

	public static void main(String[] args) {
		Test t = new Test();
		t.print();
	}

	@Override
	public void print() {
		System.out.println("test class Print");
		InterfaceA.super.print();
	}
}
