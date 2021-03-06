package paypal;

import java.util.HashMap;
import java.util.Map;

import linkedlist.DoublyLinkedList;
import linkedlist.DoublyListNode;
import linkedlist.LinkedListUtils;

public class _7CloneSinglyLinkedListWithRandomPointers {

	public static void main(String[] args) {
		DoublyLinkedList list = new DoublyLinkedList();
		list.head = new DoublyListNode(1);
		
		list.head.next = new DoublyListNode(2);
		list.head.next.next = new DoublyListNode(3);
		list.head.next.next.next = new DoublyListNode(4);
		list.head.next.next.next.next = new DoublyListNode(5);
		
		list.head.random = list.head.next.next;
        list.head.next.random = list.head;
        list.head.next.next.random = list.head.next.next.next.next;
        list.head.next.next.next.random = list.head.next.next.next.next;
        list.head.next.next.next.next.random = list.head.next;

        DoublyListNode clonedList = deepCloneWithoutAdditionalSpace(list.head);
        System.out.println("Original list : ");
        LinkedListUtils.printDoublyLinkedList(list.head);
        System.out.println("Cloned list : ");
        LinkedListUtils.printDoublyLinkedList(clonedList);
        DoublyListNode clonedList2 = deepCloneWithAdditionalSpace(list.head);
        System.out.println("Cloned list2 : ");
        LinkedListUtils.printDoublyLinkedList(clonedList2);
	}
	
    private static DoublyListNode deepCloneWithAdditionalSpace(DoublyListNode head) {
		Map<DoublyListNode, DoublyListNode> map = new HashMap<DoublyListNode, DoublyListNode>();
		DoublyListNode current = head, copy = null;
		while(current != null) {
			copy = new DoublyListNode(current.val);
			map.put(current, copy);
			current = current.next;
		}
		current = head;
		while(current != null) {
			DoublyListNode original = map.get(current);
			original.next = map.get(current.next);
			original.random = map.get(current.random);
			current = current.next;
			
		}
		return map.get(head);
	}

	public static DoublyListNode deepCloneWithoutAdditionalSpace(DoublyListNode head) {
    	DoublyListNode ptr = head;
    	//create a cloned node in between.
    	while(ptr != null) {
    		DoublyListNode temp = new DoublyListNode(ptr.val);
    		temp.next = ptr.next;
    		ptr.next = temp;
    		ptr = ptr.next.next;
    	}
    	ptr = head;
    	//copy the random pointer
    	while(ptr != null) {
    		DoublyListNode current = ptr.next;
    		current.random = ptr.random.next;
    		ptr = ptr.next.next;
    	}
    	DoublyListNode head2 = head.next;
    	ptr = head;
    	DoublyListNode current= head2;
    	//seggregate both the lists
    	while(ptr != null) {
    		ptr.next = ptr.next.next;
    		current.next = (current.next != null ? current.next.next : current.next);
    		ptr = ptr.next;
    		current = current.next;
    		
    	}
    	return head2;
    }

}
