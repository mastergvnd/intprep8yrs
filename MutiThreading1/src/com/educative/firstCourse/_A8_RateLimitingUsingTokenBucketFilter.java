package com.educative.firstCourse;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class _A8_RateLimitingUsingTokenBucketFilter {

	public static void main(String[] args) throws InterruptedException {
		final TokenGenerator tokenGenerator = new TokenGenerator(10);
		Thread t1 = new Thread(new Runnable() {
			@Override
			public void run() {
				for(int i=0; i<10; i++) {
					int token = tokenGenerator.getToken();
					System.out.println("Token generated for first thread : " + token);
				}
			}
		});
		
		Thread t2 = new Thread(new Runnable() {
			@Override
			public void run() {
				for(int i=0; i<10; i++) {
					int token = tokenGenerator.getToken();
					System.out.println("Token generated for Second thread : " + token);
				}
			}
		});
		
		t1.start();
		//t2.start();
		
		t1.join();
		//t2.join();
	}
}


class TokenGenerator{
	
	Queue<Integer> queue = new LinkedList<>();
	int capacity = 0;
	int size = 0;
	Object lock = new Object();
	Random random = new Random(1);
	public TokenGenerator(int cap) {
		this.capacity = cap;
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				
				synchronized (lock) {
					while(size == capacity) {
						try {
							lock.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					
//					if(size == capacity) {
//						size = 0;
//					}
					
					while(true) {
						if(size == capacity) {
							try {
								lock.wait();
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
						int token = random.nextInt(20);
						queue.add(token);
						size++;
						System.out.println("Token Generated : " + token + " All tokens : " + queue);
						try {
							System.out.println("Going to slepp for 1 second...");
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						lock.notifyAll();
					}
				}
			}
		});
		
		//t.setDaemon(true);
		t.start();
	}
	
	public int getToken() {
		
		synchronized (lock) {
			System.out.println("Value of size : " + size);
			while(size == 0) {
				//System.out.println("Waiting for token to be generated...");
				try {
					lock.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			int number = queue.remove();
			size--;
			lock.notifyAll();
			System.out.println("Elements : " + queue);
			return number;	
		}
	}
	
}