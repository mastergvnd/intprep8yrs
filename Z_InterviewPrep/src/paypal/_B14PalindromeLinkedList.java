package paypal;

import org.junit.Assert;

import linkedlist.LinkedList;
import linkedlist.LinkedListUtils;
import linkedlist.ListNode;

public class _B14PalindromeLinkedList {

	public static void main(String[] args) {
		LinkedList list = new LinkedList(new int[]{1,2,3,0,3,2,1});
		LinkedListUtils.printLinkedList(list.head);
		boolean isPalindrome = checkForPalindrome(list.head);
		Assert.assertEquals(true, isPalindrome);
		
		list = new LinkedList(new int[]{1,2,3,3,2,1});
		LinkedListUtils.printLinkedList(list.head);
		isPalindrome = checkForPalindrome(list.head);
		Assert.assertEquals(true, isPalindrome);
		
		list = new LinkedList(new int[]{1,2});
		LinkedListUtils.printLinkedList(list.head);
		isPalindrome = checkForPalindrome(list.head);
		Assert.assertEquals(false, isPalindrome);

		list = new LinkedList(new int[]{1,1});
		LinkedListUtils.printLinkedList(list.head);
		isPalindrome = checkForPalindrome(list.head);
		Assert.assertEquals(true, isPalindrome);
	}

	private static boolean checkForPalindrome(ListNode head) {
		ListNode slow = head, fast=head, slowPrev=null;
		
		while(fast != null && fast.next != null) {
			slowPrev = slow;
			slow = slow.next;
			fast = fast.next.next;
		}
		boolean isEvenNodes = fast == null ? true : false;
		ListNode current = isEvenNodes ? slow : slow.next;
		ListNode next=null, prev=null;
		while(current != null) {
			next = current.next;
			current.next = prev;
			prev = current;
			current = next;
		}
		
//		LinkedListUtils.printLinkedList(head);
//		LinkedListUtils.printLinkedList(prev);
		
		current = head;
		ListNode revNode = prev;
		while(revNode != null) {
			if(current.val != revNode.val)
				return false;
			current = current.next;
			revNode = revNode.next;
		}
		return true;
	}

}
