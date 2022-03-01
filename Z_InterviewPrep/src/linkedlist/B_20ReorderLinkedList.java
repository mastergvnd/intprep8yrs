package linkedlist;

import java.util.Arrays;

import org.junit.Assert;

//https://leetcode.com/problems/reorder-list/
//You are given the head of a singly linked-list. The list can be represented as:
//
//L0 -> L1 -> … -> Ln - 1 -> Ln
//Reorder the list to be on the following form:
//
//L0 -> Ln -> L1 -> Ln - 1 -> L2 -> Ln - 2 -> …
public class B_20ReorderLinkedList {

	public static void main(String[] args) {
		LinkedList list = new LinkedList(new int[]{1,2,3,4,5,6,7});
		Assert.assertArrayEquals("The list is not ordered correctly", new int[]{1,7,2,6,3,5,4}, reOrderList(list.head));
		list = new LinkedList(new int[]{1,2,3,4});
		Assert.assertArrayEquals("The list is not ordered correctly", new int[]{1,4,2,3}, reOrderList(list.head));
	}
	
	public static int[] reOrderList(ListNode head) {
		
		ListNode slow = head, fast = head, start = head;
		while(fast != null && fast.next != null){
			slow = slow.next;
			fast = fast.next.next;
		}
		System.out.println("Middle ele : " + slow.val);
		ListNode middle = reverseList(slow.next);
		slow.next = null;
		while(middle != null) {
			ListNode temp1 = start.next;
			ListNode temp2 = middle.next;
			start.next = middle;
			middle.next = temp1;
			start = temp1;
			middle = temp2;
		}
		System.out.println(Arrays.toString(LinkedListUtils.getLinkedListElements(head)));
		return LinkedListUtils.getLinkedListElements(head);
	}
	
	private static ListNode reverseList(ListNode current) {
		ListNode next = null, prev = null;
		while(current != null) {
			next = current.next;
			current.next = prev;
			prev = current;
			current = next;
		}
		return prev;
	}

}
