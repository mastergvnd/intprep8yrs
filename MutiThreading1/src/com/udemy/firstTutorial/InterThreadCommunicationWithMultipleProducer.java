package com.udemy.firstTutorial;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class InterThreadCommunicationWithMultipleProducer {

	public static void main(String[] args) {
		Queue<Integer> q = new LinkedList<>();
		ProducerThread producer = new ProducerThread(q, "Producer");
		ProducerThread newProducer = new ProducerThread(q, "NewProducer");
		ConsumerThread consumer = new ConsumerThread(q);
		producer.start();
		newProducer.start();
		consumer.start();
	}
}

class ProducerThread extends Thread {
	Queue<Integer> q = null;
	ProducerThread(Queue<Integer> q, String name) {
		super(name);
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
				System.out.println(Thread.currentThread().getName() + " thread produced : " + i);
				q.add(i);
				q.notifyAll();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}

class ConsumerThread extends Thread{
	Queue<Integer> q = null;
	ConsumerThread(Queue<Integer> q) {
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
				q.notifyAll();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}