package graph.entity.impl;

import graph.entity.IGraph;
import graph.util.BinaryHeap;
import graph.util.ListConverter;

import java.util.*;

/**
 * Abstract class implementing common methods for directed and undirected graphs
 *
 * @author Guillaume Pouilloux
 */
public abstract class AbstractGraph implements IGraph {

    protected int order;

    protected int nbEdges;

    // Store time info while walking through the graph
    protected int[] start;
    protected int[] end;
    protected int time;

    protected static final int O = Integer.MAX_VALUE;

	// Maximum cost to put on an edge
    protected static final int MAX_COST = 10;

	@Override
	public List<Integer> breadthFirstSearch(int baseVertex) {
		List<Boolean> mark = new ArrayList<>(Collections.nCopies(this.getOrder(), Boolean.FALSE));
		mark.set(baseVertex, Boolean.TRUE);

		List<Integer> visitedVertices = new ArrayList<>();
		List<Integer> minDistance = new ArrayList<>(Collections.nCopies(this.getOrder(), 0));

		Queue<Integer> toVisit = new LinkedList<>();
		toVisit.add(baseVertex);

		while(!toVisit.isEmpty()) {
			int vertex = toVisit.poll();
			visitedVertices.add(vertex);
			for(int neighbor : this.getNeighbors(vertex)) {
				if(Boolean.FALSE.equals(mark.get(neighbor))) {
					mark.set(neighbor, Boolean.TRUE);
					minDistance.set(neighbor, minDistance.get(vertex)+1);
					toVisit.add(neighbor);
				}
			}
		}

		return visitedVertices;
	}

	@Override
	public List<Integer> depthFirstSearch(int baseVertex) {
		this.initializeTime();
		List<Boolean> mark = new ArrayList<>(Collections.nCopies(this.getOrder(), Boolean.FALSE));
		List<Integer> visitedVertices = new ArrayList<>();
		mark.set(baseVertex, Boolean.TRUE);

		Stack<Integer> toVisit = new Stack<>();
		toVisit.push(baseVertex);

		while(!toVisit.isEmpty()) {
			int vertex = toVisit.pop();
			visitedVertices.add(vertex);
			start[vertex] = time++;
			for (int neighbor : this.getNeighbors(vertex)) {
				if (Boolean.FALSE.equals(mark.get(neighbor))) {
					mark.set(neighbor, Boolean.TRUE);
					toVisit.push(neighbor);
				}
			}
			end[vertex] = time++;
		}

		return visitedVertices;
	}

	@Override
	public BinaryHeap prim(int baseVertex) {
		List<Integer> neighbors = ListConverter.toList(this.getNeighbors(baseVertex));
		int predecessors[] = new int[this.getOrder()];
		int weights[] = new int[this.getOrder()];
		int cout[][] = this.getGraph();

		// initialization
		for(int i = 0; i< weights.length; i++) {
			if(neighbors.contains(i)) {
				predecessors[i] = baseVertex;
				weights[i] = cout[baseVertex][i];
			} else {
				predecessors[i] = -1; // -1 is like nil :-)
				weights[i] = O;
			}
		}
		weights[baseVertex] = 0;

		// get ready to walk through all the vertices except the base one
		List<Integer> vertices = new ArrayList<>();
		for(int i=0; i<this.getOrder(); i++) {
			if(i != baseVertex) {
				vertices.add(i);
			}
		}

		// initialize the binary heap with the base vertex
		BinaryHeap binaryHeap = new BinaryHeap(new int[]{weights[baseVertex]}, new int[]{baseVertex});

		// keep iterating while we have vertices to discover
		while(!vertices.isEmpty()) {
			int vertex = this.peekMinElement(weights, vertices); // get vertex with lowest weight
			binaryHeap.insert(weights[vertex], vertex); // insert in the binary heap
			int succs[] = this.getNeighbors(vertex);
			for(int i=0; i<succs.length; i++) {
				int succ = succs[i];
				if(vertices.contains(succ) && weights[succ] > cout[vertex][succ]) {
					weights[succ] = cout[vertex][succ];
					predecessors[succ] = vertex;
				}
			}
		}

		return binaryHeap;
	}


	@Override
	public int[][] floyd() {
		// initialization
		int n = this.getOrder();
		int[][] p = new int[n][n];
		int[][] v = new int[n][n];
		int[][] cost = this.getGraph();

		for(int i=0; i<n; i++) {
			for(int j=0; j<n; j++) {
				if(i == j) {
					v[i][j] = 0;
					p[i][j] = i;
				} else {
					v[i][j] = O;
					p[i][j] = 0;
				}
			}
			int[] neighbors = this.getNeighbors(i);
			for(int neighbor : neighbors) {
				v[i][neighbor] = cost[i][neighbor];
				p[i][neighbor] = i;
			}
		}

		// computing successive matrices
		for(int k=0; k<n; k++) {
			for(int i=0; i<n; i++) {
				for(int j=0; j<n; j++) {
					if(v[i][k] != O && v[k][j] != O && v[i][k] + v[k][j] < v[i][j]) {
						v[i][j] = v[i][k] + v[k][j];
						p[i][j] = p[k][j];
					}
				}
			}
		}

		return v;
	}

	@Override
	public int[] bellman(int baseVertex) {
		int[][] cost = this.getGraph();
		int[] distance = new int[this.getOrder()];
		int[] predecessor = new int[this.getOrder()];

		// Initialize distance and predecessor
		for(int i=0; i<this.getOrder(); i++) {
			distance[i] = O;
			predecessor[i] = -1; // -1 is just like nil :-)
		}
		distance[baseVertex] = 0;

		// Update the minimal distance until there is nothing else to fetch
		List<Integer> verticesToVisit = this.breadthFirstSearch(baseVertex);
		for(Integer vertex : verticesToVisit) {
			int[] neighbors = this.getNeighbors(vertex);
			for(int j=0; j<neighbors.length; j++) {
				int neighbor = neighbors[j];
				if(distance[vertex] != O && distance[vertex] + cost[vertex][neighbor] < distance[neighbor]) {
					distance[neighbor] = distance[vertex] + cost[vertex][neighbor];
					predecessor[neighbor] = vertex;
				}
			}
		}

		// Check for negative-weight cycles and throw an error if necessary
		for(int i=0; i<this.getOrder(); i++) {
			for(int j=0; j<this.getOrder(); j++) {
				if(cost[i][j] != O && distance[i] + cost[i][j] < distance[j]) {
					throw new Error("Graph contains a negative-weight cycle");
				}
			}
		}

		return distance;
	}

	@Override
    public List<List<Integer>> computeConnectedGraphs() {
        int baseVertex = (int)(Math.random() * this.getOrder()); // choose base vertex randomly
        this.depthFirstSearch(baseVertex);

        IGraph inverse = this.inverse();

        List<Map.Entry<Integer, Integer>> endMap = new ArrayList<>();
        for(int i=0; i<end.length; i++) {
            endMap.add(new AbstractMap.SimpleEntry<>(i, end[i]));
        }

        // reverse order for array end
        Collections.sort(endMap, byMapValues.reversed());

		// FIXME List<List<Integer>> is not perfect, better use IGraph instead!
		List<List<Integer>> connectedGraphs = new ArrayList<>();
        for(Map.Entry<Integer, Integer> end : endMap) {
	        if(connectedGraphs.stream().noneMatch(g -> g.contains(end.getKey())))
                connectedGraphs.add(inverse.depthFirstSearch(end.getKey()));
        }

		return connectedGraphs;
    }


    /**
     * Initialize time info stored when walking through the graph
     */
    protected void initializeTime() {
        this.time = 0;
        this.start = new int[this.getOrder()];
        this.end = new int[this.getOrder()];
    }


    /**
     * Finds the minimum element in a weightArray of Integer and removes it
     *
     * @param weightArray the weightArray of Integer
     * @param vertices the vertices
     * @return the minimum element in the weightArray
     */
    protected Integer peekMinElement(int[] weightArray, List<Integer> vertices) {
        Integer min = weightArray[0];
        Integer minElt = vertices.get(0);
        for(int i=1; i<vertices.size(); i++) {
            Integer elt = vertices.get(i);
            if(weightArray[i] < min) {
                min = weightArray[i];
                minElt = elt;
            }
        }

        vertices.remove(minElt);
        return minElt;
    }

    /**
     * Useful comparator used to compare two map entries by their value
     */
    Comparator<Map.Entry<Integer, Integer>> byMapValues = (left, right) -> left.getValue().compareTo(right.getValue());

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public int getNbEdges() {
        return nbEdges;
    }

    public void setNbEdges(int nbEdges) {
        this.nbEdges = nbEdges;
    }
}
