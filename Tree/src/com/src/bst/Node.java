package com.src.bst;

public class Node {
	
	private int data;
	private Node leftChild;
	private Node rightChild;
	
	public Node(int value) {
		this.data = value;
		this.leftChild = null;
		this.rightChild = null;
	}
	
	public int getData() {
		return data;
	}
	public Node getLeftChild() {
		return leftChild;
	}
	public Node getRightChild() {
		return rightChild;
	}
	public void setData(int data) {
		this.data = data;
	}
	public void setLeftChild(Node leftChild) {
		this.leftChild = leftChild;
	}
	public void setRightChild(Node rightChild) {
		this.rightChild = rightChild;
	}
}
