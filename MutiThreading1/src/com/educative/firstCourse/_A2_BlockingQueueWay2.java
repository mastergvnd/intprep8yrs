//Consumer producer problem using mutex, mutual exclusion

package com.educative.firstCourse;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class _A2_BlockingQueueWay2 {

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
		
		producer.join();
		consumer.join();

	}

}


class BlockingQueue2<T> {
	T array[];
	int size = 0;
	int capacity = 0;
	int head = 0;
	int tail = 0;
	
	Lock lock = new ReentrantLock();
	
	public BlockingQueue2(int capacity) {
		array = (T[]) new Object[capacity];
		this.capacity = capacity;
	}
	
	public void enqueue(T item) {
		
		lock.lock();
		while(size == capacity) {
			lock.unlock();
			lock.lock();
		}
		
		if(tail == capacity)
			tail = 0;
		
		array[tail] = item;
		size++;
		tail++;
		lock.unlock();
	}
	
	public T dequeue() {
		
		T item = null;
		lock.lock();
		while(size == 0) {
			lock.unlock();
			lock.lock();
		}
		
		if(head == capacity)
			head = 0;
		
		item = array[head];
		head++;
		size--;
		lock.unlock();
		return item;
	}
	
}