package com.educative.firstCourse;

public class _B2_ProducerConsumerWithSemaphores {
	public static void main(String[] args) throws InterruptedException {
		BoundedBuffer<Integer> queue = new BoundedBuffer<Integer>(10);
		Thread t1 = new Thread(() -> {
			for(int i=1; i<=5; i++){
				try {
					queue.enqueue(i);
					//I saw a scenario where consumer consumed an item before producer producing it. 
					// The reason could be that the context switching happened at below line.
					// The producer has produced an item and that's why the consumer consuled it and printed it before printing procuer statement.
					System.out.println("Producer " + Thread.currentThread().getName() + " has produced an item : " + i);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}, "Producer1");
		
		Thread t2 = new Thread(() -> {
			for(int i=1;i<=5; i++) {
				try {
					Integer item = queue.dequeue();
					System.out.println("Consumer " + Thread.currentThread().getName() + " has consumed an item : " + item);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
			}
		}, "Consumer1");
		
		t1.start();
		t2.start();
		
		t1.join();
		t2.join();
		System.out.println("Job Completed!!");
	}
}

class BoundedBuffer<E> {
	E array[];
	int tail = 0;
	int head = 0;
	int size = 0;
	int capacity;
	CountingSemaphore2 lock = new CountingSemaphore2(1, 1);
	CountingSemaphore2 prodSemaphore;
	CountingSemaphore2 consSemaphore;
	
	@SuppressWarnings("unchecked")
	BoundedBuffer(int capacity) {
		this.capacity = capacity;
		this.array = (E[]) new Object[capacity];
		prodSemaphore = new CountingSemaphore2(capacity, capacity);
		consSemaphore = new CountingSemaphore2(capacity, 0);
	}
	
	public void enqueue(E item) throws InterruptedException {
		prodSemaphore.acquire();
		lock.acquire();
		
		if(tail == capacity)
			tail = 0;
		
		array[tail] = item;
		tail++;
		size++;
		
		lock.release();
		consSemaphore.release();
	}
	
	public E dequeue() throws InterruptedException {
		E item = null;
		consSemaphore.acquire();
		lock.acquire();
		
		//here we don't need while(this.size == capacity) wait(), because this condition is there in semaphore.acquire() method.
		// Once the thread is in wait state, consSemaphore will notify prodSemaphore to produce an item.
		
		if(head == capacity) 
			head = 0;
		
		item = array[head];
		array[head] = null;
		size--;
		head++;
		
		lock.release();
		prodSemaphore.release();
		
		return item;
	}
}

class CountingSemaphore2{
	int usedPermits = 0;
	int maxCapacity;
	
	CountingSemaphore2(int maxCapacity) {
		this.maxCapacity = maxCapacity;
	}
	
	CountingSemaphore2(int maxCapacity, int initialPermits) {
		this.maxCapacity = maxCapacity;
		this.usedPermits = maxCapacity - initialPermits;
	}
	
	public synchronized void acquire() throws InterruptedException {
		while(this.usedPermits == this.maxCapacity)
			wait();
		this.usedPermits++;
		notify();
	}
	
	public synchronized void release() throws InterruptedException {
		while(this.usedPermits == 0)
			wait();
		this.usedPermits--;
		notify();
	}
}