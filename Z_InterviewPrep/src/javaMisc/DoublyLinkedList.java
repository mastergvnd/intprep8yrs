package javaMisc;

public class DoublyLinkedList<K, V> {
	DoublyListNode<K, V> head = new DoublyListNode<K, V>(null, null);
	DoublyListNode<K, V> tail = new DoublyListNode<K, V>(null, null);

	public DoublyLinkedList() {
		head.next = tail;
		tail.prev = head;
		head.prev = null;
		tail.next = null;
	}
	
	public void addNodeAtStart(DoublyListNode<K, V> node) {
		node.next = this.head.next;
		this.head.next.prev = node;
		this.head.next = node;
		node.prev = this.head;
	}
	
	public DoublyListNode<K, V> getLastNode() {
		return this.tail.prev;
	}
	
	public void deleteNode(DoublyListNode<K, V> node) {
		node.prev.next = node.next;
		node.next.prev = node.prev;
	}
	
	static class DoublyListNode<K, V> {
		public Pair<K, V> val;
		public DoublyListNode<K, V> next;
		public DoublyListNode<K, V> prev;
		
		public DoublyListNode(K val1, V val2) {
			this.val = new Pair<K, V>(val1, val2);
		}
	}


}
