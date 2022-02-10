package tree.binaryTree;

public class Node {

	
	private char data;
	private Node leftChild;
	private Node rightChild;
	
	public Node(char value) {
		this.data = value;
		this.leftChild = null;
		this.rightChild = null;
	}
	
	public char getData() {
		return data;
	}
	public Node getLeftChild() {
		return leftChild;
	}
	public Node getRightChild() {
		return rightChild;
	}
	public void setData(char data) {
		this.data = data;
	}
	public void setLeftChild(Node leftChild) {
		this.leftChild = leftChild;
	}
	public void setRightChild(Node rightChild) {
		this.rightChild = rightChild;
	}

}
