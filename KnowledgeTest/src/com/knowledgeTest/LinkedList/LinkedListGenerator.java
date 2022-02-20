package com.knowledgeTest.LinkedList;

import java.util.HashSet;
import java.util.Set;

public class LinkedListGenerator {
	public static Node generateRegularList(int numberOfNodes, boolean shouldPrintList){
		Node head = null;
		int count = 0;
		while(count++ < numberOfNodes){
			Node node = new Node();
			node.setInfo(count);
			node.setNext(null);
			if(head == null)
				head = node;
			else{
				Node temp = head;
				while(temp.getNext() != null)
					temp = temp.getNext();
				temp.setNext(node);
			}
		}
		if(shouldPrintList)
			printRegularList(head);
		return head;
	}
	
	public static Node generateCycledList(int numberOfNodes, int numberOfNodesInCycle){
		Node head = generateRegularList(numberOfNodes, false);
		Node lastNode = getLastNode(head);
		if(numberOfNodes > numberOfNodesInCycle){
			int count = 1;
			Node temp = head;
			while(count++ < numberOfNodesInCycle)
				temp = temp.getNext();
			lastNode.setNext(temp);
		}else{
			try {
				throw new Exception("Number of nodes is less than the number of nodes in the cycle.");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		printCycledList(head);
		return head;
	}
	
	public static Node generateCircularList(int numberOfNodes){
		return null;
	}
	
	private static void printRegularList(Node head){
		Node temp = head;
		while(temp != null){
			System.out.print(temp.getInfo() + "     ");
			temp = temp.getNext();
		}
		System.out.println(System.lineSeparator());
	}
	
	private static Node getLastNode(Node head){
		Node temp = head;
		while(temp.getNext() != null)
			temp = temp.getNext();
		return temp;
	}
	
	private static void printCycledList(Node head){
		Set<Node> visitedNodes = new HashSet<Node>();
		Node temp = head;
		while(true){
			if(visitedNodes.contains(temp)){
				System.out.println();
				System.out.print("Loop is found at element : "+temp.getInfo());
				break;
			}
			System.out.print(temp.getInfo() + "     ");
			visitedNodes.add(temp);
			temp = temp.getNext();
		}
		System.out.println();
	}
}
