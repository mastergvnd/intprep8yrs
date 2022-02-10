package com.testing;

public class Test1 {
	
	public static synchronized void t1() throws InterruptedException {
		for (int i = 0; i < 5; i++) {
			System.out.println("t1");
			Thread.sleep(1000);
		}
		
	}
	
	public synchronized void t2() throws InterruptedException {
		for (int i = 0; i < 5; i++) {
			System.out.println("t2");
			Thread.sleep(1000);
		}
		
	}
	
	public static void main(String[] args) {
		final Test1 test = new Test1();
//		final Test1 test2 = new Test1();
		Thread t1 = new Thread(){
			@Override
			public void run() {
				try {
					test.t1();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
			}
		};
		
		Thread t2 = new Thread(){
			@Override
			public void run() {
				try {
					test.t2();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
		t1.start();
		t2.start();
	}


}
