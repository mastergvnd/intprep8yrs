package com.educative.firstCourse;

public class _A3_JoinThreadsDemo {

	public static void main(String[] args) throws InterruptedException {
		Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				while(true) {
					System.out.println("Say hello again n again");
					try{
						Thread.sleep(1000);
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
				
			}
		});
		t.setDaemon(true);
		t.start();
		System.out.println("Main thread exited");
		t.join();
	}

}
