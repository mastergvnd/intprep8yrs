package java8;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StreamFilterExample {
	public static void main(String[] args) {
		List<Employee> list = new ArrayList<Employee>();
		list.add(new Employee(1,"Govind", 3_00_000_00, 28));
		list.add(new Employee(2,"Sonali", 5_00_000_00, 25));
		list.add(new Employee(3,"Shivam", 8_00_000_00, 23));
		
		//get the employee object of empid 2 using filter and stream.
		System.out.println(list.stream().filter(emp -> emp.getId() == 2).findFirst().orElse(null));
		System.out.println(list.stream().filter(emp -> emp.getId() == 5).findFirst().orElse(null));
		System.out.println("================================================================================================");
		
	    //Given a list of employees, you need to filter all the employee whose age is greater than 20 and print the employee names.(Java 8 APIs only)
		list.add(new Employee(4,"Imaginery", 8_00_000_00, 18));
		list.stream().filter(e -> e.getAge() > 18).map(Employee :: getName).collect(Collectors.toList()).forEach((name) -> System.out.println(name));
		System.out.println(list.stream().filter(e -> e.getAge() > 18).map(Employee :: getName).collect(Collectors.toList()));
		System.out.println("================================================================================================");
		
		//Given the list of employees, count number of employees with age 25?
		System.out.println("Number of employees with age 25 : " + list.stream().filter(e -> e.getAge() == 25).count());
		System.out.println("================================================================================================");
		
		//Given the list of employees, find the employee with name �Sonali�.
		System.out.println("Employee details with name Sonali : " + list.stream().filter(e -> e.getName().equals("Sonali")).findAny().orElse(null));
		System.out.println("================================================================================================");
		
		//Given a list of employee, find maximum age of employee?
		System.out.println("MaxAge employee : " + list.stream().mapToInt(Employee::getAge).max());
		System.out.println("================================================================================================");
		
		
	}
}
