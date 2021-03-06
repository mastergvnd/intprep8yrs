package nutanix;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tree.binaryTree.BinaryTree2;
import tree.binaryTree.BinaryTreeUtils;
import tree.binaryTree.Node2;

public class _A9KsumPathInBinaryTree3 {
//	437. Path Sum III    https://leetcode.com/problems/path-sum-iii/
//	Given the root of a binary tree and an integer targetSum, return the number of paths where the sum of the values along the path equals targetSum.

	public static void main(String[] args) {
		BinaryTree2 tree = new BinaryTree2();
		tree.setRoot(new Node2(10));
		tree.root.leftChild = new Node2(5);
		tree.root.rightChild = new Node2(-3);
		
		tree.root.leftChild.leftChild = new Node2(3);
		tree.root.leftChild.rightChild = new Node2(2);
		tree.root.rightChild.rightChild = new Node2(11);
		
		tree.root.leftChild.leftChild.leftChild = new Node2(3);
		tree.root.leftChild.leftChild.rightChild = new Node2(-2);
		tree.root.leftChild.rightChild.rightChild = new Node2(1);
		
		BinaryTreeUtils.printTreeRepresentation(tree);
		
		int count = getNumberOfSumTrees(tree.root, 8);
		System.out.println("Number of paths : " + count);
	}

	private static int getNumberOfSumTrees(Node2 root, int sum) {
		Map<Integer, Integer> map = new HashMap<Integer, Integer>();
		map.put(0, 1);
		return getNumberOfSumTrees(root, sum, 0, map);
	}

	private static int getNumberOfSumTrees(Node2 root, int target, int runningSum, Map<Integer, Integer> map) {
		if(root == null) {
			return 0;
		}
		runningSum = runningSum + root.data;
		int count = map.getOrDefault(runningSum - target, 0);
		map.put(runningSum, map.getOrDefault(runningSum, 0) + 1);
		
		count += getNumberOfSumTrees(root.leftChild, target, runningSum, map) + 
		getNumberOfSumTrees(root.rightChild,  target, runningSum, map);
		
		map.put(runningSum, map.get(runningSum) - 1);
		return count;
		
	}
	
//	private static void getNumberOfSumTrees(Node2 root, List<Integer> pathList, int sum, List<List<Integer>> paths) {
//		if(root == null) {
//			return;
//		}
//		pathList.add(root.data);
//		if(root.data == sum)
//			paths.add(new ArrayList<>(pathList));
//
//		System.out.println(pathList);
//		
//		getNumberOfSumTrees(root.leftChild, pathList, sum - root.data, paths);
//		getNumberOfSumTrees(root.rightChild, pathList, sum - root.data, paths);
//		pathList.remove(0);
//		
//	}

}
