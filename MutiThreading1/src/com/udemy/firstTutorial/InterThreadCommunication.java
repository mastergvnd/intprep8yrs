package com.udemy.firstTutorial;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class InterThreadCommunication {

	public static void main(String[] args) {
		Queue<Integer> q = new LinkedList<>();
		Producer producer = new Producer(q);
		Consumer consumer = new Consumer(q);
		producer.start();
		consumer.start();
	}
}

class Producer extends Thread {
	Queue<Integer> q = null;
	Producer(Queue<Integer> q) {
		this.q = q;
	}
	public void run() {
		while(true) {
			synchronized (q) {
				while(q.size() == 3) {
					try {
						System.out.println("Queue is full... Waiting for consumer to consume");
						q.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				Random r = new Random();
				int i = r.nextInt();
				System.out.println("Produced : " + i);
				q.add(i);
				q.notify();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}

class Consumer extends Thread {
	Queue<Integer> q = null;
	Consumer(Queue<Integer> q) {
		this.q = q;
	}
	public void run() {
		while(true) {
			synchronized (q) {
				while(q.isEmpty()) {
					try {
						System.out.println("Queue is empty... Waiting for producer to produce");
						q.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				System.out.println("Consumed : " + q.remove());
				q.notify();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}