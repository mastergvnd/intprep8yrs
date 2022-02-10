package com.testing;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ReflectionDemo {
	public static void main(String args[]) throws Exception {
		ReflectionBean obj = new ReflectionBean();

		Class cls = obj.getClass();
		System.out.println("The name of class is " + cls.getName());

		Constructor constructor = cls.getConstructor();
		System.out.println("The name of constructor is " + constructor.getName());

		System.out.println("The public methods of class are : ");

		Method[] methods = cls.getMethods();
		for (Method method : methods){
			System.out.println(method.getName());
			if(method.getParameters().length > 0){
				System.out.println(method.getParameters()[0].getType());
				System.out.println("Going to call parameterized method : ");
				method.invoke(obj, 25);
			}
		}
		
		Method methodcall1 = cls.getDeclaredMethod("method1", int.class);

		methodcall1.invoke(obj, 19);

		Field field = cls.getDeclaredField("s");

		field.setAccessible(true);

		field.set(obj, "JAVA");

		Method methodcall2 = cls.getDeclaredMethod("method1");

		methodcall2.invoke(obj);

		Method methodcall3 = cls.getDeclaredMethod("method3");

		methodcall3.setAccessible(true);

		methodcall3.invoke(obj);
	}
}