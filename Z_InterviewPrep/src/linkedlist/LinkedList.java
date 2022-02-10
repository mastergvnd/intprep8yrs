package linkedlist;

public class LinkedList {
	public LinkedList(int ar[]) {
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
