package linkedlist;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;

import linkedlist.LinkedList;
import linkedlist.LinkedListUtils;
import linkedlist.ListNode;


//Input: head = [1,3,1,2,9,5,8,5]
//Output: [1,3,2,9,5,8]

	public class _B10RemoveAllDuplicatesFromUnSoretedLinkedList {	public static void main(String[] args) {
		LinkedList list = new LinkedList(new int[]{1,3,1,1,1,2,9,5,8,5});
		ListNode head = removeDuplicates(list.head);
		Object actuals[] = LinkedListUtils.getLinkedListElements(head);
		System.out.println(Arrays.toString(actuals));
		Assert.assertArrayEquals("The duplicates are not removed", new Integer[]{1,3,2,9,5,8}, actuals);
	}

	private static ListNode removeDuplicates(ListNode head) {
		ListNode current = head, prev = head;
		Set<Integer> set = new HashSet<Integer>();
		while(current != null) {
			if(set.contains(current.val)) {
				prev.next = current.next;
			} else {
				set.add(current.val);
				prev = current;
			}
			current = current.next;
		}
		return head;
	}
}
