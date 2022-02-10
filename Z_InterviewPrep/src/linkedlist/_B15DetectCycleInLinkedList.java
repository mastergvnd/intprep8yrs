package linkedlist;

import org.junit.Assert;

import linkedlist.LinkedList;
import linkedlist.LinkedListUtils;
import linkedlist.ListNode;

public class _B15DetectCycleInLinkedList {

	public static void main(String[] args) {
		LinkedList list = new LinkedList(new int[]{1,2,3,4,5});
		LinkedListUtils.printLinkedList(list.head);
		list.head.next.next.next.next.next = list.head.next.next;
		Assert.assertTrue(hasCycle(list.head));
		
		list = new LinkedList(new int[]{1,2,3,4,5});
		LinkedListUtils.printLinkedList(list.head);
		Assert.assertFalse(hasCycle(list.head));
	}

	public static boolean hasCycle(ListNode head) {
        ListNode slow = head, fast = head;
        while(fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
            if(slow == fast)
                return true;
        }
        return false;
    }
	
}
