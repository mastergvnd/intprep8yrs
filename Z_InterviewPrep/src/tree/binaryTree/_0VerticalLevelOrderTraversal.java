package tree.binaryTree;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.TreeMap;

public class _0VerticalLevelOrderTraversal {

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
        tree.getRoot().getRightChild().getRightChild().setRightChild(new Node('l'));
        
        BinaryTreeUtils.printTreeRepresentation(tree);
        Map<Integer, List<Character>> hdMap = printVerticalOrderTraversal(tree.getRoot());
        System.out.println("Recursive method results : ");
        printMap(hdMap);
        
        System.out.println("Iterative method results : ");
        Map<Integer, List<Character>> hdMap2 = printVerticalOrderTraversalIterative(tree.getRoot());
        printMap(hdMap2);
	}

	private static void printMap(Map<Integer, List<Character>> hdMap) {
		for(Entry<Integer, List<Character>> entry : hdMap.entrySet()) {
        	System.out.println(entry.getKey() + " " + entry.getValue());
        }
	}

	private static Map<Integer, List<Character>> printVerticalOrderTraversal(Node node) {
		Map<Integer, List<Character>> hdMap = new TreeMap<Integer, List<Character>>();
		printVerticalOrderTraversal(node, 0, hdMap);
		return hdMap;
	}
	
	private static void printVerticalOrderTraversal(Node node, int nodeHd, Map<Integer, List<Character>> hdMap) {
		if(node == null)
			return;
		List<Character> values = hdMap.get(nodeHd) == null ? new ArrayList<Character>() :  hdMap.get(nodeHd);
		values.add(node.getData());
		hdMap.put(nodeHd, values);
		
		printVerticalOrderTraversal(node.getLeftChild(), nodeHd - 1, hdMap);
		printVerticalOrderTraversal(node.getRightChild(), nodeHd + 1, hdMap);
	}
	
	private static Map<Integer, List<Character>> printVerticalOrderTraversalIterative(Node node) {
		if(node == null)
			return null;
		Map<Integer, List<Character>> hdMap = new TreeMap<Integer, List<Character>>();
		Queue<NodeWrapper> queue = new LinkedList<NodeWrapper>();
		queue.add(new NodeWrapper(0, node));
		
		while(!queue.isEmpty()) {
			NodeWrapper nodeHd = queue.poll();
			List<Character> values = hdMap.get(nodeHd.getHd()) == null ? new ArrayList<Character>() : hdMap.get(nodeHd.getHd());
			values.add(nodeHd.getNode().getData());
			hdMap.put(nodeHd.getHd(), values);
			
			if(nodeHd.getNode().getLeftChild() != null)
				queue.add(new NodeWrapper(nodeHd.getHd() - 1, nodeHd.getNode().getLeftChild()));
			
			if(nodeHd.getNode().getRightChild()!= null)
				queue.add(new NodeWrapper(nodeHd.getHd() + 1, nodeHd.getNode().getRightChild()));
		}
		return hdMap;
	}
	
	static class NodeWrapper{
		private int hd;
		private Node node;
		
		public NodeWrapper(int hd, Node node) {
			this.hd = hd;
			this.node = node;
		}
		public int getHd() {
			return hd;
		}

		public Node getNode() {
			return node;
		}

		public void setHd(int hd) {
			this.hd = hd;
		}

		public void setNode(Node node) {
			this.node = node;
		}
	}

}
