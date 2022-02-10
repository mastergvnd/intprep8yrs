package com.udemy.firstTutorial;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class CallableAndFutureDemo {

	public static void main(String[] args) throws InterruptedException, ExecutionException {
		ExecutorService s = Executors.newSingleThreadExecutor();
		System.out.println("Submitting task");
		Future f = s.submit(new FactorialCalc(5));
		System.out.println("Submitted task");
		System.out.println("Fact is : " + f.get());
		s.shutdown();
	}

}

class FactorialCalc implements Callable {
	int num;
	public FactorialCalc(int num) {
		this.num = num;
	}
	@Override
	public Object call() throws Exception {
		long result = 1;
		while(num != 0) {
			System.out.println("Processing number : " + num);
			result *= num;
			num = num - 1;
			Thread.sleep(500);
		}
		return result;
	}
	
	
}