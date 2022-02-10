package graph;
import java.util.LinkedList;
import java.util.Queue;

public class Graph {
	
	LinkedList<Integer> adjList[];
	int vertices;
	public Graph(int vertices) {
		this.vertices = vertices;
		adjList = new LinkedList[this.vertices];
		for(int i = 0; i <this.vertices; i++)
			adjList[i] = new LinkedList<Integer>();
	}
	
	public boolean addEdge(int source, int destination) {
		adjList[source].add(destination);
		adjList[destination].add(source);
		return true;
	}
	
	public void bfs(int root) {
		Queue<Integer> queue = new LinkedList<>();
		boolean isVisited[] = new boolean[vertices];

		queue.add(root);
		isVisited[root] = true;
		while(!queue.isEmpty()) {
			Integer current = queue.poll();
			for(Integer child : adjList[current]) {
				if(!isVisited[child]) {
					queue.add(child);
					isVisited[child] = true;
				}
			}
			System.out.print(current + " ");
			isVisited[current] = true;
		}
	}
	
	public void print() {
		int sourceVertex = 0;
		int numberOfVertices = adjList.length;
		while(sourceVertex < numberOfVertices) {
			System.out.print("Vertex : " + sourceVertex + " ==> ");
			for(Integer vertex : adjList[sourceVertex]) {
				System.out.print(vertex + " ");
			}
			sourceVertex++;
			System.out.println();
		}
	}
}
