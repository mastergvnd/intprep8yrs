package linkedlist;

public class _B18ConvertLinkedListToDecimalNumber {

	public static void main(String[] args) {
		LinkedList list = new LinkedList(new int[]{1,0,1,1});
		LinkedListUtils.printLinkedList(list.head);
		convertLinkedListToDecimalNumber(list.head);
		
	}
	
	public static int convertLinkedListToDecimalNumber(ListNode head) {
		int res = 0;
		ListNode temp = head;
		while(temp != null) {
			res = (res << 1) + temp.val;
			temp = temp.next;
		}
		System.out.println(res);
		return res;
	}

}
