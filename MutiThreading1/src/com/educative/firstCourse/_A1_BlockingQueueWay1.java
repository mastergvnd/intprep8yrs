//Consumer producer problem using the synchronized keyword, which is equivalent of a monitor in Java.

package com.educative.firstCourse;


public class _A1_BlockingQueueWay1 {

	public static void main(String[] args) throws InterruptedException {
		
		final BlockingQueue<Integer> queue = new BlockingQueue<Integer>(5);
		
		Thread producer = new Thread(new Runnable() {
			@Override
			public void run() {
				for(int i = 1; i <= 50; i++) {
					queue.enqueue(i);
					System.out.println("Produced : " + i);
				}
			}
		}, "Producer");

		Thread consumer = new Thread(new Runnable() {
			@Override
			public void run() {
				for(int i = 1; i <= 50; i++) {
					System.out.println("Consumed : " + queue.dequeue());
				}
			}
		}, "Consumer");
		
		producer.start();
		consumer.start();
		
		//Wait for both threads to terminate.
		producer.join();
		consumer.join();
		System.out.println("Completed");
		
	}

}


class BlockingQueue<T> {
	T[] array;
	int head = 0;
	int tail = 0;
	int capacity = 0;
	int size = 0;
	
	Object lock = new Object();
	
	@SuppressWarnings("unchecked")
	BlockingQueue(int capacity) {
		array = (T[]) new Object[capacity];
		this.capacity = capacity;
	}
	
	public void enqueue(T element) {
		
		synchronized (lock) {
			while(size == capacity){
				try {
					lock.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			if(tail == capacity)
				tail = 0;
			
			array[tail] = element;
			tail++;
			size++;
			lock.notifyAll();
		}		
	}
	
	public synchronized T dequeue() {
		T item = null;
		synchronized (lock) {
			while(size == 0) {
				try {
					lock.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			if(head == capacity)
				head = 0;
			
			item = array[head];
			head++;
			size --;
			lock.notifyAll();
		}
		return item;
	}
}