package javaMisc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;

import javaMisc.DoublyLinkedList.DoublyListNode;

public class LRUCache {

	public static void main(String[] args) {
		LRUCacheIml<Integer, String> cache = new LRUCacheIml<Integer, String>(3);
		cache.put(1, "Govind");
		cache.put(2, "Sonali");
		cache.put(3, "Shivam");
		System.out.println(cache.getListValues());
		Assert.assertEquals(3, cache.getSize());
		Assert.assertEquals("Sonali", cache.get(2));
		System.out.println(cache.getListValues());
		cache.put(4, "Dollyy");
		System.out.println(cache.getListValues());
		Assert.assertEquals(3, cache.getSize());
		Assert.assertEquals("Sonali", cache.get(2));
		Assert.assertEquals(3, cache.getSize());
		System.out.println(cache.getListValues());
	}
	
	static class LRUCacheIml<K, V>{
		int capacity;
		
		Map<K, DoublyListNode<K, V>> cache;
		DoublyLinkedList<K,V> list;
		public LRUCacheIml(int capacity) {
			this.capacity = capacity;
			cache = new HashMap<K, DoublyListNode<K, V>>(capacity);
			list = new DoublyLinkedList<K,V>();
		}
		
		public V get(K key) {
			if(cache.containsKey(key)) {
				DoublyListNode<K, V> oldNode = cache.get(key);
				DoublyListNode<K, V> newNode = new DoublyListNode<K, V>(oldNode.val.getVal1(), oldNode.val.getVal2());
				list.addNodeAtStart(newNode);
				list.deleteNode(oldNode);
				cache.put(key, newNode);
				return cache.get(key).val.getVal2();
			}
			return null;
		}
		
		public void put(K key, V value) {
			if(cache.containsKey(key)) {
				DoublyListNode<K, V> node = cache.get(key);
				node.val.setVal2(value);
				cache.put(key, node);
			} else {
				if(cache.keySet().size() == capacity) {
					//remove LRU from list followed by add this new entry in list and then update the cache.
					DoublyListNode<K, V> lastNode = list.getLastNode();
					cache.remove(lastNode.val.getVal1());
					list.deleteNode(lastNode);
				}
				//put in the list and update the cache
				DoublyListNode<K, V> node = new DoublyListNode<K, V>(key, value);
				list.addNodeAtStart(node);
				cache.put(key, node);
			}
		}
		
		public int getSize() {
			return cache.size();
		}
		
		public String getCacheKeys() {
			return Arrays.toString(cache.keySet().toArray());
		}
		
		public String getListValues() {
			List<Pair<K,V>> listOfPair= new ArrayList<Pair<K, V>>();
			DoublyListNode<K, V> head = list.head.next;
			while(head.next != null) {
				listOfPair.add(head.val);
				head = head.next;
			}
			return Arrays.toString(listOfPair.toArray());
		}

	}

}
