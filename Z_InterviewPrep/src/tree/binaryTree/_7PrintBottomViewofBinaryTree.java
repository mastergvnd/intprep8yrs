package tree.binaryTree;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;

public class _7PrintBottomViewofBinaryTree {

	public static void main(String[] args) {
		BinaryTree tree = new BinaryTree();
		tree.setRoot(new Node('a'));
		
        tree.getRoot().setLeftChild(new Node('b'));
        tree.getRoot().setRightChild(new Node('c'));

        tree.getRoot().getLeftChild().setLeftChild(new Node('d'));
        tree.getRoot().getLeftChild().setRightChild(new Node('e'));
        tree.getRoot().getRightChild().setRightChild(new Node('f'));    
        
        tree.getRoot().getLeftChild().getRightChild().setLeftChild(new Node('g'));
        tree.getRoot().getLeftChild().getRightChild().setRightChild(new Node('h'));

        
        BinaryTreeUtils.printTreeRepresentation(tree);
        
        Map<Integer, Character> topView = printTopViewofBinaryTree(tree.getRoot());
        for(Character value : topView.values())
        	System.out.print(value + " ");
	}

	
	private static Map<Integer, Character> printTopViewofBinaryTree(Node root) {
		if(root == null)
			return null;
		Map<Integer, Character> topView = new TreeMap<Integer, Character>();
		Queue<NodeWrapper> queue = new LinkedList<NodeWrapper>();
		queue.add(new NodeWrapper(root, 0));
		while(!queue.isEmpty()) {
			NodeWrapper temp = queue.poll();
			topView.put(temp.getHd(), temp.getNode().getData());
			
			if(temp.getNode().getLeftChild() != null)
				queue.add(new NodeWrapper(temp.getNode().getLeftChild(), temp.getHd()-1));
			if(temp.getNode().getRightChild() != null)
				queue.add(new NodeWrapper(temp.getNode().getRightChild(), temp.getHd()+1));
		}
		
		return topView;
	}


	static class NodeWrapper{
		private Node node;
		private int hd;
		public NodeWrapper(Node node, int hd) {
			this.node = node;
			this.hd = hd;
		}
		public Node getNode() {
			return node;
		}
		public int getHd() {
			return hd;
		}
		public void setNode(Node node) {
			this.node = node;
		}
		public void setHd(int hd) {
			this.hd = hd;
		}
	}
}
