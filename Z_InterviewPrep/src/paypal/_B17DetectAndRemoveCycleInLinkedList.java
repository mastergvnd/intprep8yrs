package paypal;

import org.junit.Assert;

import linkedlist.LinkedList;
import linkedlist.LinkedListUtils;
import linkedlist.ListNode;

public class _B17DetectAndRemoveCycleInLinkedList {

	public static void main(String[] args) {
		LinkedList list = new LinkedList(new int[]{1,2,3,4,5});
		LinkedListUtils.printLinkedList(list.head);
		list.head.next.next.next.next.next = list.head.next.next;
		Assert.assertEquals(3, detectAndRemoveCycle(list.head).val);

	}

	private static ListNode detectAndRemoveCycle(ListNode head) {
		ListNode slow = head, fast = head;
        boolean isCycle = false;
        while(fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
            if(slow == fast) {
             isCycle = true;
                break;
            }
        }
        if(!isCycle)
            return null;
        slow = head;
        while(slow != fast.next){
            slow = slow.next;
            fast = fast.next;
        }
        //ToDo: remove loop from it
        return slow;
	}

}
