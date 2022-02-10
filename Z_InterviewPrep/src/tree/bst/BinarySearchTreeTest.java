package tree.bst;

public class BinarySearchTreeTest {

	public static void main(String[] args) {
		BinarySearchTree bst = new BinarySearchTree();
		bst.addIteratively(6);
		bst.addIteratively(4);
		bst.addIteratively(9);
		bst.addIteratively(2);
		bst.addIteratively(5);
		bst.addIteratively(8);
		bst.addIteratively(12);
		bst.addIteratively(10);
		bst.addIteratively(14);
		
		System.out.print("Normal Travelsal : ");
		bst.printTree(bst.getRoot());
		System.out.println(System.lineSeparator());
		
		System.out.print("Inorder Traversal : ");
		bst.printInOrderTraversalRec(bst.getRoot());
		System.out.println(System.lineSeparator());
		
		System.out.print("Level Order Traversal : ");
		bst.printLevelOrderTravelsalItr(bst.getRoot());
		System.out.println(System.lineSeparator());
		
		System.out.print("Level Order Spiral Traversal : ");
		bst.printLevelOrderSpiralTravelsalItr(bst.getRoot());
		System.out.println(System.lineSeparator());
		
		System.out.print("Level Order line by line Traversal : ");
		bst.printLevelOrderTravelsalLineByLineItr(bst.getRoot());
		System.out.println(System.lineSeparator());
		
		System.out.print("Reverse Level Order Traversal : ");
		bst.printReverseLevelOrderTravelsalItr(bst.getRoot());
		System.out.println(System.lineSeparator());
	}
}
