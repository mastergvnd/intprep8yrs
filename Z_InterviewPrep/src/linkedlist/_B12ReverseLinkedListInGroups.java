package linkedlist;

import org.junit.Assert;

import linkedlist.LinkedList;
import linkedlist.LinkedListUtils;
import linkedlist.ListNode;

public class _B12ReverseLinkedListInGroups {

	public static void main(String[] args) {
		LinkedList list = new LinkedList(new int[]{1,2,3,4,5,6,7,8,9,10});
		LinkedListUtils.printLinkedList(list.head);
		ListNode head = reverseListInGroups(list.head, 4);
		LinkedListUtils.printLinkedList(head);
		Object []actuals = LinkedListUtils.getLinkedListElements(head);
		Assert.assertArrayEquals(new Integer[]{4,3,2,1,8,7,6,5,9,10}, actuals);
	}

	private static ListNode reverseListInGroups(ListNode head, int k) {
		if(head == null)
			return null;
		
        ListNode temp = head;
        int counter = 0;
        while(temp!= null) {
            counter++;
            temp = temp.next;
        }
        if(counter < k)
            return head;
		
		ListNode current = head, prev = null, next = null;
		counter = 0;
		
		while(counter++ < k && current != null) {
			next = current.next;
			current.next = prev;
			prev = current;
			current = next;
			//counter++;
		}
		
		if(next != null)
			head.next = reverseListInGroups(next, k);
		return prev;
	}
}
