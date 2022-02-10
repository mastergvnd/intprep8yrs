package java8;

public class Employee {
	int id;
	String name;
	double salary;
	int age;
	
	public Employee(int id, String name, double salary, int age) {
		this.id = id;
		this.name = name;
		this.salary = salary;
		this.age = age;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public double getSalary() {
		return salary;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setSalary(double salary) {
		this.salary = salary;
	}
	
	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	@Override
	public String toString() {
		return "[ " + this.id + ", " + this.name + ", " + this.salary +  ", " + this.age +" ]";
	}
}
