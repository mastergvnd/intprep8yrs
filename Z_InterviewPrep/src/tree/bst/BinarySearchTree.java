package tree.bst;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Stack;

public class BinarySearchTree {
	private Node root = null;
	
	public BinarySearchTree() {
		
	}
	
	public BinarySearchTree(int[] data) {
		for(int value : data)
			addIteratively(value);
	}
	
	public Node getRoot() {
		return root;
	}

	public void setRoot(Node root) {
		this.root = root;
	}

	public boolean addIteratively(int value) {
		if(isEmpty()) {
			root = new Node(value);
			return true;
		}
		
		Node currentNode = root;
		while(currentNode != null) {
			Node leftChild = currentNode.getLeftChild();
			Node rightChild = currentNode.getRightChild();
			
			if(currentNode.getData() < value) {
				if(rightChild == null) {
					rightChild = new Node(value);
					currentNode.setRightChild(rightChild);
					return true;
				}
				currentNode = rightChild;
			}
			else {
				if(leftChild == null) {
					leftChild = new Node(value);
					currentNode.setLeftChild(leftChild);
					return true;
				}
				currentNode = leftChild;
			}
		}
		return false;
	}
	
	private boolean isEmpty() {
		return root == null;
	}

	/*
	 * This API prints a given binary search tree.
	 */
	public void printTree(Node current) {
		if(current == null)
			return;
		
		System.out.print(current.getData() + "  ");
		printTree(current.getLeftChild());
		printTree(current.getRightChild());
	}
	
	/*
	 * This API prints level order traversal of a given binary search tree.
	 */
	public void levelOrderTraversal(Node current) {
		if(current == null)
			return;
		
	}
	
	/*
	 * This API prints in-order traversal of a given binary search tree.
	 */
	public void printInOrderTraversalRec(Node root) {
		if(root == null)
			return;
		
		printInOrderTraversalRec(root.getLeftChild());
		System.out.print(root.getData() + "  ");
		printInOrderTraversalRec(root.getRightChild());
	}
	
	/*
	 * This API prints level-order traversal of a given binary search tree.
	 */
	public void printLevelOrderTravelsalItr(Node root) {
		Node node = root;
		Deque<Node> queue = new LinkedList<>();
		queue.add(node);
		while(!queue.isEmpty()) {
			node = queue.poll();
			System.out.print(node.getData() + "  ");
			if(node.getLeftChild() != null) {
				queue.add(node.getLeftChild());
			}
			if(node.getRightChild() != null) {
				queue.add(node.getRightChild());
			}
		}
	}
	
	/*
	 * This API prints level-order traversal of a given binary search tree.
	 */
	public void printLevelOrderTravelsalLineByLineItr(Node root) {
		Node node = root;
		Deque<Node> queue = new LinkedList<>();
		queue.add(node);
		queue.add(null);
		while(!queue.isEmpty()) {
			node = queue.poll();
			if(node == null) {
				if(!queue.isEmpty()) {
					queue.add(null);
					System.out.println();
				}
			} else {
				System.out.print(node.getData() + "  ");
				if(node.getLeftChild() != null) {
					queue.add(node.getLeftChild());
				}
				if(node.getRightChild() != null) {
					queue.add(node.getRightChild());
				}
			}
		}
	}
	
	/*
	 * This API prints level-order spiral traversal of a given binary search tree.
	 */
	public void printLevelOrderSpiralTravelsalItr(Node root) {
		Node node = root;
		Stack<Node> stack1 = new Stack<>();
		Stack<Node> stack2 = new Stack<>();
		stack1.add(node);
		while(!stack1.isEmpty() || !stack2.empty()) {
			while(!stack1.isEmpty()) {
				node = stack1.pop();
				System.out.print(node.getData() + "  ");
				if(node.getLeftChild() != null)
					stack2.add(node.getLeftChild());
				if(node.getRightChild() != null)
					stack2.add(node.getRightChild());
			}
			//System.out.println();
			while(!stack2.isEmpty()) {
				node = stack2.pop();
				System.out.print(node.getData() + "  ");
				
				if(node.getRightChild() != null)
					stack1.push(node.getRightChild());
				if(node.getLeftChild() != null)
					stack1.push(node.getLeftChild());
			}
			//System.out.println();
		}
	}
	
	/*
	 * This API prints reverse level-order traversal of a given binary search tree.
	 */
	public void printReverseLevelOrderTravelsalItr(Node root) {
		Node node = root;
		Deque<Node> queue = new LinkedList<>();
		Stack<Node> stack = new Stack<>();
		queue.add(node);
		while(!queue.isEmpty()) {
			node = queue.pop();
			stack.push(node);
			if(node.getRightChild() != null)
				queue.add(node.getRightChild());
			if(node.getLeftChild() != null)
				queue.add(node.getLeftChild());
		}
		while(!stack.isEmpty()) {
			node = stack.pop();
			System.out.print(node.getData() + "  ");
		}
	}
}
