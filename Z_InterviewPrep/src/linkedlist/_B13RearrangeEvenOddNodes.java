package linkedlist;

import org.junit.Assert;

import linkedlist.LinkedList;
import linkedlist.LinkedListUtils;
import linkedlist.ListNode;

public class _B13RearrangeEvenOddNodes {



	public static void main(String[] args) {
		LinkedList list = new LinkedList(new int[]{1,2,3,4,5,6,7,8,9,10});
		LinkedListUtils.printLinkedList(list.head);
		ListNode head = rearrangeEvenOdd(list.head);
		LinkedListUtils.printLinkedList(head);
		int []actuals = LinkedListUtils.getLinkedListElements(head);
		Assert.assertArrayEquals(new int[]{1,3,5,7,9,2,4,6,8,10}, actuals);
	}

	private static ListNode rearrangeEvenOdd(ListNode head) {
		ListNode odd = head, even = head.next;
		ListNode current = even.next;
		while(current != null) {
			odd.next = even.next;
			//even.next
		}
		
		return null;
		
	}


}
