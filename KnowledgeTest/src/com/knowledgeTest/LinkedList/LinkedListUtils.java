package com.knowledgeTest.LinkedList;

public class LinkedListUtils {
	
	public static boolean detectLoop(Node head){
		Node slowPtr = head;
		Node fastPtr = head;
		while(slowPtr != null && fastPtr != null && fastPtr.getNext() != null){
			slowPtr = slowPtr.getNext();
			fastPtr = fastPtr.getNext().getNext();
			if(slowPtr == fastPtr)
				return true;
		}
		return false;
	}

	public static int detectLoopLength(Node head) {
		Node slowPtr = head;
		Node fastPtr = head;
		while(slowPtr != null && fastPtr != null && fastPtr.getNext() != null){
			slowPtr = slowPtr.getNext();
			fastPtr = fastPtr.getNext().getNext();
			if(slowPtr == fastPtr){
				int count = 1;
				while(slowPtr.getNext() != fastPtr){
					count++;
					slowPtr = slowPtr.getNext();
				}
				return count;
			}
		}
		return 0;
	}
}
