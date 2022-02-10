package tree.binaryTree;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Stack;

public class _1ReverseLevelOrderTraversalInSpiralForm {

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
        tree.getRoot().getLeftChild().getLeftChild().setRightChild(new Node('i'));
        tree.getRoot().getRightChild().getLeftChild().setLeftChild(new Node('j'));
        tree.getRoot().getRightChild().getLeftChild().setRightChild(new Node('k'));
        
        tree.getRoot().getLeftChild().getLeftChild().getLeftChild().setLeftChild(new Node('l'));
        tree.getRoot().getLeftChild().getLeftChild().getLeftChild().setRightChild(new Node('m'));
        
        BinaryTreeUtils.printTreeRepresentation(tree);
        System.out.println("Solution with 3 stacks");
        printReverseLevelOrderTraversalInSpiralForm(tree.getRoot());
        System.out.println();
        System.out.println("Solution with one stack and one DEqueue");
        printReverseLevelOrderTraversalInSpiralFormUsingDEqueue(tree.getRoot());
	}

	private static void printReverseLevelOrderTraversalInSpiralForm(Node root) {
		if(root == null)
			return;
		Stack<Node> st1 = new Stack<Node>();
		st1.add(root);
		Stack<Node> st2 = new Stack<Node>();
		Stack<Character> result = new Stack<Character>();
		while(!st1.isEmpty() || !st2.isEmpty()) {
			while(!st1.isEmpty()) {
				Node temp = st1.pop();
				result.add(temp.getData());
				if(temp.getRightChild() != null)
					st2.push(temp.getRightChild());
				if(temp.getLeftChild() != null)
					st2.push(temp.getLeftChild());
			}
			while(!st2.isEmpty()) {
				Node temp = st2.pop();
				result.add(temp.getData());
				if(temp.getLeftChild() != null)
					st1.push(temp.getLeftChild());
				if(temp.getRightChild() != null)
					st1.push(temp.getRightChild());
			}
			
		}
		while(!result.isEmpty())
			System.out.print("  " + result.pop());
	}
	
	private static void printReverseLevelOrderTraversalInSpiralFormUsingDEqueue(Node root) {
		if(root == null)
			return;
		Deque<Node> queue = new LinkedList<Node>();
		queue.addFirst(root);
		Stack<Character> result = new Stack<Character>();
		while(queue.peekFirst()!=null || queue.peekLast()!=null) {
			while(queue.peekFirst()!=null) {
				Node temp = queue.removeFirst();
				result.add(temp.getData());
				if(temp.getRightChild() != null)
					queue.addLast(temp.getRightChild());
				if(temp.getLeftChild() != null)
					queue.addLast(temp.getLeftChild());
			}
			while(queue.peekLast()!=null) {
				Node temp = queue.removeLast();
				result.add(temp.getData());
				if(temp.getRightChild() != null)
					queue.addFirst(temp.getRightChild());
				if(temp.getLeftChild() != null)
					queue.addFirst(temp.getLeftChild());
			}
			
		}
		while(!result.isEmpty())
			System.out.print("  " + result.pop());
	}
}
