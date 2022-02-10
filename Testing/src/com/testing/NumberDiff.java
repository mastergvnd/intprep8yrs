package com.testing;

import java.util.ArrayList;
import java.util.List;

public class NumberDiff {

	public static void main(String[] args) {
		List<Integer> l1 = new ArrayList<>();
		l1.add(1234);
		l1.add(4321);
		
		List<Integer> l2 = new ArrayList<>();
		l2.add(2345);
		l2.add(3214);
		
		int moves = findMoves(l1, l2);
		System.out.println("Number of moves : "+ moves);
	}

	private static int findMoves(List<Integer> l1, List<Integer> l2) {
		int moves = 0;
		System.out.println(l1.size() + " " + l2.size());
		for(int i = 0; i < l1.size(); i++){
			System.out.println("govind");
			int num1 = (int)l1.get(i);
			int num2 = (int)l2.get(i);
			while(num1 > 0){
				int mod1 = num1%10;
				int mod2 = num2%10;
				int diff = mod1-mod2;
				num1 = num1/10;
				num2 = num2/10;
				if(diff < 0)
					diff = diff*-1;
				moves+=diff;
				System.out.println("Moves : "+diff);
			}
		}
		return moves;
	}

}
