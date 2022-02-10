package multithreading;

import java.util.Collection;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

// This implementation is done using Reentrant lock and Condition class in java;
public class _A1_BlockingQueueImplementation {

	public static void main(String[] args) throws InterruptedException {
		BlockingQueue<Integer> queue = new BlockingQueue(5, true);
		Thread producer = new Thread(() -> {
			for(int i=0; i <= 50; i++) {
				try {
					queue.add(i);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				System.out.println("Produced : " + i);
			}
		}, "Producer");
		
		Thread consumer = new Thread(() -> {
			for(int i = 0; i<=50; i++) {
				try {
					System.out.println("Consumed : " + queue.remove());
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}, "Consumer");
		
		producer.start();
		consumer.start();
		
		producer.join();
		consumer.join();
		System.out.println("Done");

	}

}

class BlockingQueue<E> {
	Object array[];
	int putIndex;
	int takeIndex;
	int size;
	
	ReentrantLock lock;
	Condition notEmpty;
	Condition notFull;
	
	public BlockingQueue(int capacity) {
		this(capacity, false);
	}
	
	public BlockingQueue(int capacity, boolean fairness) {
		if(capacity <= 0)
			throw new IllegalArgumentException();
		array = new Object[capacity];
		lock = new ReentrantLock(fairness);
		notEmpty = lock.newCondition();
		notFull = lock.newCondition();
	}
	
	public BlockingQueue(int capacity, boolean fairness,  Collection<? extends E> collection) {
		this(capacity, fairness);
		
	}
	
	public void add(Object o) throws InterruptedException {
		lock.lock();
		try{
			while(this.size == array.length)
				this.notFull.await();
			array[putIndex] = o;
			putIndex++;
			if(putIndex == array.length)
				putIndex = 0;
			size++;
			this.notEmpty.signalAll();
		} finally {
			lock.unlock();
		}
	}
	
	public Object remove() throws InterruptedException {
		lock.lock();
		Object value = null;
		try{
			while(this.size == 0)
				notEmpty.await();
			value = array[takeIndex];
			takeIndex++;
			if(takeIndex == array.length)
				takeIndex = 0;
			size--;
			notFull.signalAll();
			return value;
		} finally {
			lock.unlock();
		}
	}
	
}
