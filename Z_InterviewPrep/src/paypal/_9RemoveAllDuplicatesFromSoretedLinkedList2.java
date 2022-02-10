package paypal;

import org.junit.Assert;

import linkedlist.LinkedList;
import linkedlist.LinkedListUtils;
import linkedlist.ListNode;


//Input: head = [1,2,3,3,3,4,4,5]
//Output: [1,2,5]

	public class _9RemoveAllDuplicatesFromSoretedLinkedList2 {	public static void main(String[] args) {
		LinkedList list = new LinkedList(new int[]{1,1,1,2,3});
		ListNode head = removeDuplicates(list.head);
		Object actual[] = LinkedListUtils.getLinkedListElements(head);
		Assert.assertArrayEquals(new Integer[]{2, 3},actual);
		
		list = new LinkedList(new int[]{1,2,3,3,3,4,4,5});
		head = removeDuplicates(list.head);
		actual = LinkedListUtils.getLinkedListElements(head);
		Assert.assertArrayEquals(new Integer[]{1,2,5},actual);
	}

	private static ListNode removeDuplicates(ListNode head) {
		
		ListNode dummy = new ListNode(0);
		dummy.next = head;
		ListNode prev = dummy, current = head;
		
		while(current != null){
			while(current.next != null && prev.next.val == current.next.val){
				current = current.next;
			}
			if(prev.next != current) {
				prev.next = current.next;
			} else 
				prev = prev.next;
			
			current = current.next;
		}
		
		return dummy.next;
	}
}
