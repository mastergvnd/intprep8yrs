package tree.binaryTree;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

public class _3PerfectBinaryTreeSpecificLevelOrderTraversal2 {

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
        tree.getRoot().getLeftChild().getRightChild().setLeftChild(new Node('j'));
        tree.getRoot().getLeftChild().getRightChild().setRightChild(new Node('k'));
        tree.getRoot().getRightChild().getLeftChild().setLeftChild(new Node('l'));
        tree.getRoot().getRightChild().getLeftChild().setRightChild(new Node('m'));
        tree.getRoot().getRightChild().getRightChild().setLeftChild(new Node('n'));
        tree.getRoot().getRightChild().getRightChild().setRightChild(new Node('o'));
        
        tree.getRoot().getLeftChild().getLeftChild().getLeftChild().setLeftChild(new Node('p'));
        tree.getRoot().getLeftChild().getLeftChild().getLeftChild().setRightChild(new Node('q'));
        tree.getRoot().getLeftChild().getLeftChild().getRightChild().setLeftChild(new Node('r'));
        tree.getRoot().getLeftChild().getLeftChild().getRightChild().setRightChild(new Node('s'));
        tree.getRoot().getLeftChild().getRightChild().getLeftChild().setLeftChild(new Node('t'));
        tree.getRoot().getLeftChild().getRightChild().getLeftChild().setRightChild(new Node('u'));
        tree.getRoot().getLeftChild().getRightChild().getRightChild().setLeftChild(new Node('v'));
        tree.getRoot().getLeftChild().getRightChild().getRightChild().setRightChild(new Node('w'));
        tree.getRoot().getRightChild().getLeftChild().getLeftChild().setLeftChild(new Node('x'));
        tree.getRoot().getRightChild().getLeftChild().getLeftChild().setRightChild(new Node('y'));
        tree.getRoot().getRightChild().getLeftChild().getRightChild().setLeftChild(new Node('z'));
        tree.getRoot().getRightChild().getLeftChild().getRightChild().setRightChild(new Node('1'));
        tree.getRoot().getRightChild().getRightChild().getLeftChild().setLeftChild(new Node('2'));
        tree.getRoot().getRightChild().getRightChild().getLeftChild().setRightChild(new Node('3'));
        tree.getRoot().getRightChild().getRightChild().getRightChild().setLeftChild(new Node('4'));
        tree.getRoot().getRightChild().getRightChild().getRightChild().setRightChild(new Node('5'));

        BinaryTreeUtils.printTreeRepresentation(tree);
        
        printPerfectBinaryTreeSpecificLevelOrderTraversal2(tree.getRoot());
	}
	
	private static void printPerfectBinaryTreeSpecificLevelOrderTraversal2(Node root) {
		Stack<Character> st = new Stack<Character>();
		if(root == null)
			return;
		st.push(root.getData());
		if(root.getLeftChild() != null) {
			st.push(root.getRightChild().getData());
			st.push(root.getLeftChild().getData());
		}
		if(root.getLeftChild().getLeftChild() != null)
			printPerfectBinaryTreeSpecificLevelOrderTraversal2(root, st);
		
		while(!st.isEmpty())
			System.out.print(st.pop() + " ");
	}

	private static void printPerfectBinaryTreeSpecificLevelOrderTraversal2(Node root, Stack<Character> st) {
		
		Queue<Node> queue = new LinkedList<Node>();
		queue.add(root.getLeftChild());
		queue.add(root.getRightChild());
		
		Node first = null, second = null;
		while(!queue.isEmpty()) {
			first = queue.poll();
			second = queue.poll();
			
			st.push(second.getLeftChild().getData());
			st.push(first.getRightChild().getData());
			st.push(second.getRightChild().getData());
			st.push(first.getLeftChild().getData());
			
			if(first.getLeftChild().getLeftChild() != null) {
				queue.add(first.getRightChild());
				queue.add(second.getLeftChild());
				queue.add(first.getLeftChild());
				queue.add(second.getRightChild());
			}
		}
		
		
	}

}
