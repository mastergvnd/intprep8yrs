package linkedlist;

public class LinkedList {
	int size = 0;
	public LinkedList(int ar[]) {
		this.size = ar.length;
		head = LinkedListUtils.createLinkedList(head, ar);
	}
	
	public LinkedList() {
	}
	public ListNode head;
	
	public ListNode getHead() {
		return head;
	}
	public void setHead(ListNode head) {
		this.head = head;
	}
}
