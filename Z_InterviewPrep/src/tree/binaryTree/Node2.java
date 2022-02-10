package tree.binaryTree;

public class Node2 {
	
	public int data;
	public Node2 leftChild;
	public Node2 rightChild;
	
	public Node2(int value) {
		this.data = value;
		this.leftChild = null;
		this.rightChild = null;
	}
	
	public int getData() {
		return data;
	}
	public Node2 getLeftChild() {
		return leftChild;
	}
	public Node2 getRightChild() {
		return rightChild;
	}
	public void setData(int data) {
		this.data = data;
	}
	public void setLeftChild(Node2 leftChild) {
		this.leftChild = leftChild;
	}
	public void setRightChild(Node2 rightChild) {
		this.rightChild = rightChild;
	}


}
