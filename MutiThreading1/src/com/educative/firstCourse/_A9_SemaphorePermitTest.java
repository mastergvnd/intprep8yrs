package com.educative.firstCourse;

import java.util.concurrent.Semaphore;

public class _A9_SemaphorePermitTest {

	public static void main(String[] args) {
		Semaphore s = new Semaphore(1);
		System.out.println(s.availablePermits());
		s.acquireUninterruptibly();
		System.out.println(s.availablePermits());
		s.acquireUninterruptibly(); // try commenting this line and seee the difference
		System.out.println(s.availablePermits());

		s.release();
		System.out.println(s.availablePermits());
		s.release();
		System.out.println(s.availablePermits());
		s.release();
		System.out.println(s.availablePermits());
		System.out.println("Completed");
	}

}
