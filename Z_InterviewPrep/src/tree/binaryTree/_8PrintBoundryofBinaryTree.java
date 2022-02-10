package tree.binaryTree;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;

public class _8PrintBoundryofBinaryTree {

	public static void main(String[] args) {
		BinaryTree tree = new BinaryTree();
		tree.setRoot(new Node('a'));
		
        tree.getRoot().setLeftChild(new Node('b'));
        tree.getRoot().setRightChild(new Node('c'));

        tree.getRoot().getLeftChild().setLeftChild(new Node('d'));
        tree.getRoot().getLeftChild().setRightChild(new Node('e'));
        tree.getRoot().getRightChild().setLeftChild(new Node('f'));
        tree.getRoot().getRightChild().setRightChild(new Node('g'));

        tree.getRoot().getLeftChild().getLeftChild().setLeftChild(new Node('h'));
        tree.getRoot().getLeftChild().getRightChild().setLeftChild(new Node('i'));
        tree.getRoot().getLeftChild().getRightChild().setRightChild(new Node('j'));
        tree.getRoot().getRightChild().getLeftChild().setLeftChild(new Node('k'));
        tree.getRoot().getRightChild().getLeftChild().setRightChild(new Node('l'));
        
        tree.getRoot().getLeftChild().getRightChild().getRightChild().setRightChild(new Node('m'));

        BinaryTreeUtils.printTreeRepresentation(tree);
        
        printBoundryofBinaryTree(tree.getRoot());
	}

	
	private static void printBoundryofBinaryTree(Node root) {
		printLeftBoundry(root);
		printLeafNodes(root.getLeftChild());
		printLeafNodes(root.getRightChild());
		printRightBoundry(root.getRightChild());
	}


	private static void printLeftBoundry(Node root) {
		if(root == null)
			return ;
		Queue<Node> queue = new LinkedList<Node>();
		queue.add(root);
		while(!queue.isEmpty()) {
			Node temp = queue.poll();
			if(temp.getLeftChild() != null || temp.getRightChild()!= null)
				System.out.print(temp.getData() + " ");
			
			if(temp.getLeftChild() != null)
				queue.add(temp.getLeftChild());
			else if(temp.getRightChild() != null)
				queue.add(temp.getRightChild());
		}
	}
	
	private static void printRightBoundry(Node root) {
		if(root == null)
			return;
		Queue<Node> queue = new LinkedList<Node>();
		queue.add(root);
		while(!queue.isEmpty()) {
			Node temp = queue.poll();
			if(temp.getLeftChild() != null || temp.getRightChild()!= null)
				System.out.print(temp.getData() + " ");
			
			if(temp.getRightChild() != null)
				queue.add(temp.getRightChild());
			else if(temp.getLeftChild() != null)
				queue.add(temp.getLeftChild());
		}
	}
	private static void printLeafNodes(Node root) {
		if(root == null)
			return;
		Queue<Node> queue = new LinkedList<Node>();
		queue.add(root);
		while(!queue.isEmpty()) {
			Node temp = queue.poll();
			if(temp.getLeftChild() == null && temp.getRightChild() ==  null)
				System.out.print(temp.getData() + " ");
			
			if(temp.getLeftChild() != null)
				queue.add(temp.getLeftChild());
			if(temp.getRightChild() != null)
				queue.add(temp.getRightChild());
		}
	}
}
