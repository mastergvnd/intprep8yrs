package com.src.lambda;

public class RunnableDemo {

	public static void main(String[] args) {

		//Java 7 Syntax
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				System.out.println("Inside anonymour class");
			}
		}).start();
		
		new Thread(() -> System.out.println("Inside Expression lambda")).start();
		
		new Thread(() -> {
			System.out.println("Inside block lambda");
		}).start();
		
		Runnable r = () -> System.out.println("Inside runnable interface creation using lanbda");
		new Thread(r).start();

	}

}
