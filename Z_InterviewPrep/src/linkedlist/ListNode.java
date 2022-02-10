package linkedlist;

public class ListNode {
	public int val;
	public ListNode next;
	
	public ListNode(int data, ListNode next) {
		this.val = data;
		this.next = next;
	}
	
	public ListNode(int data) {
		this.val = data;
		this.next = null;
	}
	
	public int getData() {
		return val;
	}
	public ListNode getNext() {
		return next;
	}
	public void setData(int data) {
		this.val = data;
	}
	public void setNext(ListNode next) {
		this.next = next;
	}
}
