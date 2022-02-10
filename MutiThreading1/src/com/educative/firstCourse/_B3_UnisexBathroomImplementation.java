package com.educative.firstCourse;

import java.util.concurrent.Semaphore;

public class _B3_UnisexBathroomImplementation {

	public static void main(String[] args) throws InterruptedException {
		UnisexBathroom bathroom = new UnisexBathroom();
		Thread female1 = new Thread(() -> {
			try {
				bathroom.femaleUseBathroom("Sonali");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		});
		
		
		Thread male1 = new Thread(() -> {
			try {
				bathroom.maleUseBathroom("Govind");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		});
		
		Thread male2 = new Thread(() -> {
			try {
				bathroom.maleUseBathroom("Shivam");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		});
		
		Thread male3 = new Thread(() -> {
			try {
				bathroom.maleUseBathroom("Hemant");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		});
		
		Thread male4 = new Thread(() -> {
			try {
				bathroom.maleUseBathroom("Pradeep");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		});
		
		female1.start();
		male1.start();
		male2.start();
		male3.start();
		male4.start();
		
		female1.join();
		male1.join();
		male2.join();
		male3.join();
		male4.join();
		System.out.println("Done");
	}

}

class UnisexBathroom {
	String MEN = "Men";
	String WOMEN = "Women";
	String NONE = "None";
	
	String usedInBy = NONE;
	int personCount = 0;
	Semaphore empCount = new Semaphore(3);
	
	public void maleUseBathroom(String name) throws InterruptedException {
		synchronized (this) {
			while(usedInBy == WOMEN)
				wait();
			empCount.acquire();
			personCount++;
			usedInBy = MEN;
		}
		
		useBathroom(name);
		empCount.release();
		
		synchronized (this) {
			personCount--;
			if(personCount == 0)
				usedInBy = NONE;
			notifyAll();
		}
	}
	
	private void useBathroom(String name) throws InterruptedException {
		System.out.println(name + " is using Bathroom. Current number of persons in bathroom : " + personCount);
		Thread.sleep(1000);
		System.out.println(name + " is done using bathroom");
	}

	public void femaleUseBathroom(String name) throws InterruptedException {
		synchronized (this) {
			while(usedInBy == MEN)
				this.wait();
			empCount.acquire();
			personCount++;
			usedInBy = WOMEN;
		}
		
		useBathroom(name);
		empCount.release();
		
		synchronized (this) {
			personCount--;
			if(personCount == 0)
				usedInBy = NONE;
			notifyAll();
		}
	}
}