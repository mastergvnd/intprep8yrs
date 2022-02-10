package tree.binaryTree;

import java.util.LinkedList;
import java.util.Queue;

public class _4PrintLeftViewofBinaryTree {

	public static void main(String[] args) {
		BinaryTree tree = new BinaryTree();
		
		tree.setRoot(new Node('a'));
		
		tree.getRoot().setLeftChild(new Node('b'));
		tree.getRoot().setRightChild(new Node('c'));
		
		tree.getRoot().getRightChild().setLeftChild(new Node('d'));
		tree.getRoot().getRightChild().setRightChild(new Node('e'));
		
		tree.getRoot().getRightChild().getLeftChild().setLeftChild(new Node('f'));
		tree.getRoot().getRightChild().getLeftChild().setRightChild(new Node('g'));
		
		BinaryTreeUtils.printTreeRepresentation(tree);
		
		printLeftViewofBinaryTree(tree.getRoot());
	}

	private static void printLeftViewofBinaryTree(Node root) {
		if(root == null)
			return;
		
		Queue<Node> queue = new LinkedList<Node>();
		queue.add(root);
		
		while(!queue.isEmpty()) {
			int size = queue.size();
			for(int i = 1; i <= size; i++) {
				Node temp = queue.poll();
				if(i == 1)
					System.out.print(temp.getData() + " ");
				if(temp.getLeftChild() != null)
					queue.add(temp.getLeftChild());
				if(temp.getRightChild() != null)
					queue.add(temp.getRightChild());
			}
		}
		
	}

}
