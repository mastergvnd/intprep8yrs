package nutanix;

import java.util.ArrayList;
import java.util.List;

import tree.binaryTree.BinaryTree2;
import tree.binaryTree.BinaryTreeUtils;
import tree.binaryTree.Node2;

public class _A8KsumPathInBinaryTree2 {
	

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
		
		List<List<Integer>> paths = getNumberOfSumTrees(tree.root, 15);
		System.out.println(paths);
	}

	private static List<List<Integer>> getNumberOfSumTrees(Node2 root, int sum) {
		List<Integer> pathList = new ArrayList<Integer>();
		List<List<Integer>> paths = new ArrayList<List<Integer>>();
		getNumberOfSumTrees(root, pathList, sum, paths);
		return paths;
	}

	private static void getNumberOfSumTrees(Node2 root, List<Integer> pathList, int sum, List<List<Integer>> paths) {
		if(root == null)
			return;
		pathList.add(root.data);
		if(root.data == sum) {
			paths.add(new ArrayList<>(pathList));
		}
		
		getNumberOfSumTrees(root.leftChild, pathList, sum - root.data, paths);
		getNumberOfSumTrees(root.rightChild, pathList, sum - root.data, paths);
		
		pathList.remove(pathList.size() - 1);
	}

}
