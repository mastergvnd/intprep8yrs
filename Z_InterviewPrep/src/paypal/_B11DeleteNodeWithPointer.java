package paypal;

import linkedlist.LinkedList;
import linkedlist.LinkedListUtils;
import linkedlist.ListNode;

public class _B11DeleteNodeWithPointer {

	public static void main(String[] args) {
		LinkedList list = new LinkedList(new int[]{1,2,3,4,5,6,7});
		LinkedListUtils.printLinkedList(list.head);
		deleteNode(list.head.next.next.next);
		LinkedListUtils.printLinkedList(list.head);
	}

	private static void deleteNode(ListNode node) {
		ListNode temp = node.next;
		node.val = temp.val;
		node.next = temp.next;
	}
}
