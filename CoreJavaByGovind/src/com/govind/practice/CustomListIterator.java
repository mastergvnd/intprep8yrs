package com.govind.practice;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.RandomAccess;

public class CustomListIterator {

	public static void main(String[] args) {
		CustomArrayList<Customer> customList = new CustomArrayList();
		customList.add(new Customer(1, "Govind"));
		customList.add(new Customer(2, "Sonali"));
		customList.add(new Customer(3, "Shivam"));
		System.out.println(customList);
		Iterator itr = customList.iterator();
		while(itr.hasNext()) {
			Customer c = (Customer) itr.next();
			if(c.getName().equals("Shivam")){
				itr.remove();
			}
			System.out.println(c);
		}
		System.out.println(customList.get(6));
	}
}


class Customer {
	int customerId;
	String name;
	
	public Customer() {
	}
	
	public Customer(int id, String name) {
		this.customerId = id;
		this.name = name;
	}
	
	public int getCustomerId() {
		return customerId;
	}
	public String getName() {
		return name;
	}
	public void setCustomerId(int customerId) {
		this.customerId = customerId;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		return "Id : " + this.customerId + " Name: " + this.name;
	}
}

class CustomArrayList<E> extends AbstractList<E> implements List<E>, RandomAccess, Cloneable, Serializable{
	private static int DEFAULT_LIST_SIZE = 10;
	private transient Object elementData[];
	private int size;
	
	public CustomArrayList() {
		this(DEFAULT_LIST_SIZE);
	}
	
	public CustomArrayList(int capacity) {
		elementData = new Object[capacity];
	}
	
	@Override
	public boolean add(E object) {
		this.elementData[size++] = object;
		return true;
	}
	
	@Override
	public Iterator<E> iterator() {
		return new Itr();
	}

	@Override
	public E get(int index) {
		checkRange(index);
		return elementData(index);
	}

	private E elementData(int index) {
		return (E) this.elementData[index];
	}

	private void checkRange(int index) {
		if(index >= this.size)
			throw new IndexOutOfBoundsException(outOfBoundMsg(index));
	}

	private String outOfBoundMsg(int index) {
		return "Index : " + index + ", Size : " + this.size;
	}

	@Override
	public int size() {
		return this.size;
	}
	
	@Override
	public E remove(int index) {
		checkRange(index);
		checkForComodification();
		
		return null;
	}

	private void checkForComodification() {
		if (CustomArrayList.this.modCount != this.modCount)
            throw new ConcurrentModificationException();
	}
	
	private class Itr implements Iterator<E> {
		int cursor;
		int expectedModCount;
		int lastRet;
		
		private Itr() {
			lastRet = -1;
			expectedModCount = CustomArrayList.this.modCount;
		}
		
		@Override
		public boolean hasNext() {
			return this.cursor != size;
		}

		@Override
		public E next() {
			checkforComodification();
			if(this.cursor >= size)
				throw new NoSuchElementException();
			if(CustomArrayList.this.modCount >= CustomArrayList.this.elementData.length)
				throw new ConcurrentModificationException();
			lastRet = cursor + 1;
			return (E) CustomArrayList.this.elementData[cursor++];
		}
		
		public void remove() {
			if(this.lastRet < 0)
				throw new IllegalStateException();
			
			checkforComodification();
			CustomArrayList.this.remove(this.lastRet);
			
			this.cursor = this.lastRet;
			this.lastRet = -1;
			this.expectedModCount = CustomArrayList.this.modCount;
			
		}

		private void checkforComodification() {
			if(modCount != expectedModCount) 
				throw new ConcurrentModificationException();
			
		}
	}
	
}