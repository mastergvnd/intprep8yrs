package nutanix;

import java.util.ArrayList;
import java.util.List;

import tree.binaryTree.BinaryTree2;
import tree.binaryTree.BinaryTreeUtils;
import tree.binaryTree.Node2;

public class _A7KsumPathInBinaryTree1 {
	

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
		
		boolean isPath = hasPathSum(tree.root, 18);
		System.out.println(isPath);
	}

	private static boolean hasPathSum(Node2 root, int sum) {
		if(root == null) 
			return false;
		
		if(root.data == sum && root.leftChild == null && root.rightChild == null)
			return true;
		
		return (hasPathSum(root.leftChild, sum - root.data) || hasPathSum(root.rightChild, sum - root.data));
	}

}
