package linkedlist;

public class DoublyListNode {
	public int val;
	public DoublyListNode next;
	public DoublyListNode random;
	
	public DoublyListNode(int data, DoublyListNode next, DoublyListNode random) {
		this.val = data;
		this.next = next;
		this.random = random;
	}
	
	public DoublyListNode(int data) {
		this.val = data;
		this.next = null;
		this.random = null;
	}
	
	public int getData() {
		return val;
	}
	public DoublyListNode getNext() {
		return next;
	}
	public void setData(int data) {
		this.val = data;
	}
	public void setNext(DoublyListNode next) {
		this.next = next;
	}
	public DoublyListNode getRandom() {
		return random;
	}
	public void setRandom(DoublyListNode random) {
		this.random = random;
	}
	
}
