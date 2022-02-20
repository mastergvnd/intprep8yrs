package com.knowledgeTest.LinkedList;

public class Test {

	public static void main(String[] args) {
		detectLoop();
		detectLoopLength();
	}
	
	private static void detectLoop() {
		Node head = LinkedListGenerator.generateCycledList(7,3);
		//Node head = LinkedListGenerator.generateRegularList(7,true);
		System.out.println("Is loop found in list : "+LinkedListUtils.detectLoop(head));
		System.out.println(System.lineSeparator());
	}
	
	private static void detectLoopLength() {
		Node head = LinkedListGenerator.generateCycledList(7,3);
		//Node head = LinkedListGenerator.generateRegularList(7,true);
		System.out.println("The loop length is : "+LinkedListUtils.detectLoopLength(head));
		System.out.println(System.lineSeparator());
	}
}
