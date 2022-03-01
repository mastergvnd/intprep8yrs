package linkedlist;

import org.junit.Assert;

public class B_19FindMiddleOfLinkedList {

	public static void main(String[] args) {
		LinkedList list = new LinkedList(new int[] {1,2,3,4,5});
		LinkedListUtils.printLinkedList(list.head);
		Assert.assertEquals("Middle Element is not correct.", 3, getMiddleElement(list.head));
		list = new LinkedList(new int[] {1,2,3,4,5,6,7,8});
		Assert.assertEquals("Middle Element is not correct.", 5, getMiddleElement(list.head));
	}

	private static int getMiddleElement(ListNode head) {
		if(head.next == null)
			return head.getData();
		
		ListNode slow = head, fast = head;
		
		while(fast != null && fast.next != null) {
			slow = slow.next;
			fast = fast.next.next;
		}
		System.out.println("Middle is : " + slow.val);
		return slow.val;
	}
}
