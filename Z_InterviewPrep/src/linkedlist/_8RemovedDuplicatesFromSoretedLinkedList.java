package linkedlist;

import linkedlist.ListNode;
import linkedlist.LinkedList;
import linkedlist.LinkedListUtils;

//Input: head = [1,2,3,3,4,4,5]
//Output: [1,2,3,4,5]
public class _8RemovedDuplicatesFromSoretedLinkedList {

	public static void main(String[] args) {
		LinkedList list = new LinkedList();
		list.head = new ListNode(1);
		
		list.head.next = new ListNode(1);
		list.head.next.next = new ListNode(1);
		list.head.next.next.next = new ListNode(2);
		list.head.next.next.next.next = new ListNode(3);
		list.head.next.next.next.next.next = new ListNode(3);
		
		LinkedListUtils.printLinkedList(list.head);
		ListNode head = removeDuplicates(list.head);
		System.out.println();
		LinkedListUtils.printLinkedList(head);
	}

	private static ListNode removeDuplicates(ListNode head) {
		
		ListNode temp = head;
		while(temp != null){
			while(temp.next != null && temp.val == temp.next.val){
				temp.next = temp.next.next;
			}
			temp = temp.next;
		}
		
		return head;
	}

}
