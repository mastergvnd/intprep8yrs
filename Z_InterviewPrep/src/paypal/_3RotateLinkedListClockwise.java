package paypal;

import linkedlist.ListNode;
import linkedlist.LinkedList;
import linkedlist.LinkedListUtils;

public class _3RotateLinkedListClockwise {

	public static void main(String[] args) {
		LinkedList list = new LinkedList();
		list.head = new ListNode(1);
		
		list.head.next = new ListNode(2);
		list.head.next.next = new ListNode(3);
		list.head.next.next.next = new ListNode(4);
		list.head.next.next.next.next = new ListNode(5);
		list.head.next.next.next.next.next = new ListNode(6);
		list.head.next.next.next.next.next.next = new ListNode(7);
		list.head.next.next.next.next.next.next.next = new ListNode(8);
		
		LinkedListUtils.printLinkedList(list.head);
		
		ListNode head = printRotateLinkedListClockwise(list.head, 9);
		System.out.println();
		LinkedListUtils.printLinkedList(head);
	}

	private static ListNode printRotateLinkedListClockwise(ListNode head, int k) {
		ListNode current = head;
		ListNode temp = head;
		
		if(head == null)
			return head;
		int length = 1;
		while(temp.next != null) {
			temp = temp.next;
			length++;
		}

		if(k > length)
			k = k % length;
		k = length - k;
		
		if(k == 0 || k == length)
			return head;
		
		int count = 1;
		while(count < k && current != null){
			current = current.next;
			count++;
		}
		
		ListNode kthNode = current;
		temp.next = head;
		head = kthNode.next;
		kthNode.next = null;
		
		return head;
	}

}
