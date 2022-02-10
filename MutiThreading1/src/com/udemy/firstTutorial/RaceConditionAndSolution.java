package com.udemy.firstTutorial;
import java.util.concurrent.atomic.AtomicInteger;

public class RaceConditionAndSolution {

	public static void main(String[] args) throws InterruptedException {
		CounterWithRaceCondition counter = new CounterWithRaceCondition();
		//CounterWithOutRaceCondition counter = new CounterWithOutRaceCondition();
		IncrementingThread i = new IncrementingThread(counter, "IncrementingThread");
		DecrementingThread d = new DecrementingThread(counter, "DecrementingThread");
		System.out.println("Startin ght threads");
		i.start();
		d.start();
		i.join();
		d.join();
		System.out.println("Final Value is : " + counter.getValue());
	}

}

	class CounterWithRaceCondition {
		int c = 0;
		
		public void increment(){
			c++;
		}
		
		public void decrement(){
			c--;
		}
		public int getValue(){
			return c;
		}
	}
	
	class CounterWithOutRaceCondition {
		AtomicInteger c = new AtomicInteger(0);
		
		public void increment(){
			c.incrementAndGet();
		}
		
		public void decrement(){
			c.decrementAndGet();
		}
		public int getValue(){
			return c.intValue();
		}
	}
	
	class IncrementingThread extends Thread {
		//CounterWithOutRaceCondition counter;
		CounterWithRaceCondition counter;
		//IncrementingThread(CounterWithOutRaceCondition counter){
		IncrementingThread(CounterWithRaceCondition counter, String name){
			super(name);
			this.counter = counter;
		}
		
		public void run() {
			for(int i=0; i<10000; i++) {
				//System.out.println("Thread Name : " + Thread.currentThread().getName());
				counter.increment();
			}
		}
	}
	
	class DecrementingThread extends Thread {
		//CounterWithOutRaceCondition counter;
		CounterWithRaceCondition counter;
		//DecrementingThread(CounterWithOutRaceCondition counter){
		DecrementingThread(CounterWithRaceCondition counter, String name){
			super(name);
			this.counter = counter;
		}
		
		public void run() {
			for(int i=0; i<10000; i++) {
				//System.out.println("Thread Name : " + Thread.currentThread().getName());
				counter.decrement();
			}
		}
	}