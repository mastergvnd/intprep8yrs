package java8;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
		
		//Given the list of employees, find the employee with name ?Sonali?.
		System.out.println("Employee details with name Sonali : " + list.stream().filter(e -> e.getName().equals("Sonali")).findAny().orElse(null));
		System.out.println("================================================================================================");
		
		//Given a list of employee, find maximum age of employee?
		System.out.println("MaxAge employee : " + list.stream().mapToInt(Employee::getAge).max());
		System.out.println("================================================================================================");
		
		//Given a list of employees, sort all the employee on the basis of age? 
		list.stream().sorted((e1, e2) -> e2.getAge() - e1.getAge()).forEach(e -> System.out.println(e));
		System.out.println("================================================================================================");
		
		//Join the all employee names with ?,? using java 8?
		List<String> employeeNames = list.stream().map(e -> e.getName()).collect(Collectors.toList());
		System.out.println(String.join(",", employeeNames));
		System.out.println("================================================================================================");
		
		//Given the list of employee, group them by employee name?
		list.add(new Employee(4,"Shivam", 3_00_000_00, 19));
		System.out.println(list.stream().collect(Collectors.groupingBy(e -> e.name)));
		
		Random r = new Random(100);
		r.ints().limit(5).sorted().forEach(System.out::println);
		
		List<Integer> list2 = Arrays.asList(1,2,3,4,5,6,7,8,9,10,11,12,1,1);
		System.out.println(list2.stream().mapToInt(a -> a).sum());
		System.out.println(list2.stream().mapToInt(a -> a*a).filter(a -> a >= 100).average());
		
		System.out.println(list2.stream().max(Comparator.comparing(Integer :: valueOf)));
		System.out.println(list2.stream().mapToInt(a -> a).min());
		
		List<String> list3 = Arrays.asList("Govind", "Kumar", "Gupta");
		list3.stream().map(a -> a + ",").forEach(System.out::println);
		
		StringJoiner str = new StringJoiner(",", "(", ")");
		str.add("Govind");
		str.add("Gupta");
		System.out.println(str);
		
		Stream.iterate(2, count -> count + 1).limit(10).filter(count -> count % 2 == 0).forEach(System.out::println);
		
		System.out.println(list3.stream().filter(s -> s.length() > 5).count());
		
		Stream.concat(list2.stream(), list3.stream()).forEach(s -> System.out.print(s + " "));
		System.out.println();
		
		list2.stream().collect(Collectors.toSet()).forEach(a -> System.out.print(a + " "));
		System.out.println();
		
		list.remove(list.size() - 1);
		list.stream().collect(Collectors.toMap(Employee::getId, Function.identity())).forEach((k, v) -> System.out.println(k + " " + v));
	}
}
