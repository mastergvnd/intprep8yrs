package tree.binaryTree;

public class BinaryTreeUtils {
	public static void printTreeRepresentation(BinaryTree tree) {
		BinaryTreePrinter.printNode(tree.getRoot());
	}
	
	public static void printTreeRepresentation(BinaryTree2 tree) {
		BinaryTreePrinter2.printNode2(tree.getRoot());
	}
}
