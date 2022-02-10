package com.testing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FinalDiscountPrice {

	public static void main(String[] args) {
		//int ar[] = {5,1,3,4,6,2};
		int ar[] = {2,3,1,2,4,2};
		List<Integer> list = new ArrayList<>();
/*		list.add(5);
		list.add(1);
		list.add(3);
		list.add(4);
		list.add(6);
		list.add(2);*/
		
/*		list.add(2);
		list.add(3);
		list.add(1);
		list.add(2);
		list.add(4);
		list.add(2);*/
		
		list.add(1);
		list.add(3);
		list.add(3);
		list.add(2);
		list.add(5);
		printResult(list);
	}

	private static void printResult(List<Integer> list) {
		int sum = 0;
		List<Integer> noDiscountList = new ArrayList<>();
		int smallest = 0;
		for(int i=0; i<list.size(); i++){
			if(i == list.size()-1){
				sum = sum + list.get(i);
				noDiscountList.add(i);
				break;
			}
			smallest = getsmallest(list, i+1);
			if(list.get(i) >= smallest){
				sum += list.get(i)-smallest;
			} else{
				sum += list.get(i);
				noDiscountList.add(i);
			}
		}
		System.out.println("Sum = " + sum);
		System.out.println("No discount items are : " + noDiscountList);
	}

	private static int getsmallest(List<Integer> list, int i) {
		if(i == list.size()){
			return list.get(i-1);
		}
		int min = list.get(i);
		while(i < list.size()){
			if(min > list.get(i))
				min = list.get(i);
			i++;
		}
		return min;
	}

}
