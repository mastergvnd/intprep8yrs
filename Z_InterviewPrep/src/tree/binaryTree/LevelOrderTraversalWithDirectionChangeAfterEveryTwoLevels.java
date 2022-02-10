package tree.binaryTree;

import static org.junit.Assert.*;

import org.junit.Test;

public class LevelOrderTraversalWithDirectionChangeAfterEveryTwoLevels {

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
        
        tree.getRoot().getLeftChild().getLeftChild().getRightChild().setLeftChild(new Node('p'));
        tree.getRoot().getLeftChild().getRightChild().getRightChild().setLeftChild(new Node('q'));
        tree.getRoot().getLeftChild().getRightChild().getRightChild().setRightChild(new Node('r'));
        tree.getRoot().getRightChild().getLeftChild().getRightChild().setRightChild(new Node('s'));
        
        BinaryTreeUtils.printTreeRepresentation(tree);
        
        char[] actual = printLevelOrderTraversalWithDirectionChangeAfterEveryTwoLevels(tree.getRoot());
        char[] expected = {};
        assertEquals("Tree traversal is not correct.", expected,  actual);
	}

	public static char[] printLevelOrderTraversalWithDirectionChangeAfterEveryTwoLevels(Node root) {
		
		return null;
	}
	
}
