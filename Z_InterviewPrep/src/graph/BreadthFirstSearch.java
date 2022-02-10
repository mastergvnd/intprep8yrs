package graph;

public class BreadthFirstSearch {

	public static void main(String[] args) {
		Graph graph = new Graph(6);
		
		graph.addEdge(0, 1); 
		graph.addEdge(0, 2); 
		graph.addEdge(0, 3); 
		graph.addEdge(1, 2); 
		graph.addEdge(2, 4); 
		
		graph.print();
		
		graph.bfs(0);
	}
}
