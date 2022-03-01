package linkedlist;

import java.util.ArrayList;
import java.util.List;

public class LinkedListUtils {
	
	public static void printLinkedList(ListNode node) {
		ListNode temp = node;
		while(temp != null) {
			System.out.print(" " + temp.getData() + " ");
			temp = temp.getNext();
		}
		System.out.println();
	}
	
	public static void printDoublyLinkedList(DoublyListNode head) {
		DoublyListNode ptr = head;
        while (ptr != null) {
            System.out.println("Data = " + ptr.val + ", Random = " + (ptr.random == null ? "NULL" : ptr.random.val) + ", Next Value = " + (ptr.next == null ? "NULL" : ptr.next.val));
            ptr = ptr.next;
        }
	}
	
	public static ListNode createLinkedList(ListNode head, int ar[]) {
		for(int a : ar) {
			ListNode node = new ListNode(a);
			if(head == null)
				head = node;
			else {
				ListNode temp = head;
				while(temp.next != null)
					temp = temp.next;
				temp.next = node;
			}
		}
		return head;
	}
	
	public static int[] getLinkedListElements(ListNode head) {
		List<Integer> list = new ArrayList<Integer>();
		
		ListNode temp = head;
		while(temp != null) {
			list.add(temp.val);
			temp = temp.next;
		}
		return list.stream().mapToInt(Integer::intValue).toArray();
			
	}
}
