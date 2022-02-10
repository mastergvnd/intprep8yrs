package com.educative.firstCourse;

public class _B1_ImplementingSemaphore {
	
//	The complete code appears below along with a test. Note how we acquire and release the semaphore in different threads in different methods, something not possible with a mutex. 
//	Thread t1 always acquires the semaphore while thread t2 always releases it. The semaphore has a max permit of 1 so you'll see the output interleaved between the two threads. 
//	You might see the print statements from the two threads not interleave each other and may appear twice in succession. 
//	This is possible because of how threads get scheduled for execution and also because we start with an unused permit.
//	The astute reader would also observe that the given solution will always block if the semaphore is initialized with zero permits


	public static void main(String[] args) throws InterruptedException {
		final CountingSemaphore2 cs = new CountingSemaphore2(1);
		Thread t1 = new Thread(()-> {
			for(int i=0;i<5; i++) {
				try {
					cs.acquire();
					System.out.println("Acquire : " + i);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		
		Thread t2 = new Thread(() -> {
			for(int i = 0; i<5; i++) {
				try {
					cs.release();
					System.out.println("Release : " + i);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		
		t1.start();
		t2.start();
		t1.join();
		t2.join();
		System.out.println("Completed");
	}

}

class CountingSemaphore{
	int usedPermits;
	int maxCount;
	
	CountingSemaphore(int maxCount) {
		this.maxCount = maxCount;
		this.usedPermits = 0;
	}
	
	public synchronized void acquire() throws InterruptedException {
		while(usedPermits == maxCount)
			wait();
		
		usedPermits++;
		notify();
	}
	
	public synchronized void release() throws InterruptedException {
		while(usedPermits == 0)
			wait();
		
		usedPermits--;
		notify();
	}
	
}
