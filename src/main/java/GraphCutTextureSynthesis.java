package src.main.java;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public class GraphCutTextureSynthesis {

    class Edge {
        int src, dest, residualCapacity, originalCapacity;

        public Edge(int src, int dest, int capacity) {
            this.src = src;
            this.dest = dest;
            this.residualCapacity = this.originalCapacity = capacity;
        }
    }

    // Initialize a ForkJoinPool with a maximum of 10 threads
    private ForkJoinPool pool = new ForkJoinPool(10);
    int nodes;
    List<List<Edge>> adjList;
    int[] parent;

    public GraphCutTextureSynthesis(int nodes) {
        this.nodes = nodes;
        this.parent = new int[nodes];
        this.adjList = new ArrayList<>(nodes);
        for (int i = 0; i < nodes; i++) {
            adjList.add(new ArrayList<>());
        }
    }
    public void addEdge(int src, int dest, int capacity) {
        System.out.println("Adding edge: From Node " + src + " to Node " + dest + " with Capacity: " + capacity);
        Edge edge = new Edge(src, dest, capacity);
        Edge reverseEdge = new Edge(dest, src, 0);
        adjList.get(src).add(edge);
        adjList.get(dest).add(reverseEdge);
    }

    public int getWeight(Color sourceColor, Color targetColor) {
        System.out.println("Calculating weight between Source Color: " + sourceColor + " and Target Color: " + targetColor);
        int weight = Math.abs(sourceColor.getRed() - targetColor.getRed()) +
                Math.abs(sourceColor.getGreen() - targetColor.getGreen()) +
                Math.abs(sourceColor.getBlue() - targetColor.getBlue());
        System.out.println("Calculated weight: " + weight);
        return weight;
    }

    public boolean bfs(int s, int t) {
        System.out.println("Starting BFS: From Source Node " + s + " to Target Node " + t);
        Arrays.fill(parent, -1);
        Queue<Integer> queue = new LinkedList<>();
        queue.add(s);
        parent[s] = s;
        while (!queue.isEmpty()) {
            int current = queue.poll();
            System.out.println("Current node in BFS: " + current);
            for (Edge edge : adjList.get(current)) {
                if (parent[edge.dest] == -1 && edge.residualCapacity > 0) {
                    parent[edge.dest] = current;
                    if (edge.dest == t) {
                        System.out.println("Target node " + t + " reached in BFS");
                        return true;
                    }
                    queue.add(edge.dest);
                }
            }
        }
        System.out.println("BFS completed without reaching target node " + t);
        return false;
    }

    public boolean parallelBFS(int s, int t) {
        System.out.println("Starting parallel BFS from node: " + s + " to target node: " + t);
        Arrays.fill(parent, -1);
        BFSParallelTask task = new BFSParallelTask(s, t, 0);
        Boolean result = pool.invoke(task);
        System.out.println(result ? "Path found in parallel BFS." : "Path not found in parallel BFS.");
        return result;
    }

    private class BFSParallelTask extends RecursiveTask<Boolean> {
        int node, target, depth;
        static final int MAX_DEPTH = 3;  // This limits the depth of the BFS to control the number of tasks spawned.

        BFSParallelTask(int node, int target, int depth) {
            this.node = node;
            this.target = target;
            this.depth = depth;
        }

        @Override
        protected Boolean compute() {
            // If the current node is the target, path is found.
            if (node == target) {
                System.out.println("Target node: " + target + " reached.");
                return true;
            }

            // Limit the depth to control the number of tasks spawned.
            if (depth > MAX_DEPTH) {
                System.out.println("Maximum depth reached for node: " + node);
                return false;
            }

            List<BFSParallelTask> tasks = new ArrayList<>();

            // Ensure thread-safety when updating the shared parent array.
            synchronized(parent) {
                for (Edge edge : adjList.get(node)) {
                    if (parent[edge.dest] == -1 && edge.residualCapacity > 0) {
                        System.out.println("Spawning a task from node: " + node + " to node: " + edge.dest + " at depth: " + depth);
                        parent[edge.dest] = node;
                        BFSParallelTask task = new BFSParallelTask(edge.dest, target, depth+1);
                        tasks.add(task);
                        task.fork();
                    }
                }
            }

            // Wait for each task to complete and check if any task found the path.
            for (BFSParallelTask task : tasks) {
                if (task.join()) {
                    System.out.println("Path found through node: " + node);
                    return true;
                }
            }

            System.out.println("Exiting node: " + node + " without finding target.");
            return false;
        }
    }

    public int edmondsKarp(int s, int t) {
        int maxFlow = 0;
        while (parallelBFS(s, t)) {
            int pathFlow = Integer.MAX_VALUE;
            for (int v = t; v != s; v = parent[v]) {
                int u = parent[v];
                for (Edge edge : adjList.get(u)) {
                    if (edge.dest == v) {
                        pathFlow = Math.min(pathFlow, edge.residualCapacity);
                        break;
                    }
                }
            }

            for (int v = t; v != s; v = parent[v]) {
                int u = parent[v];
                for (Edge edge : adjList.get(u)) {
                    if (edge.dest == v) {
                        edge.residualCapacity -= pathFlow;
                        if (edge.residualCapacity == 0) {
                            // This is a saturated edge, which is part of the cut
                            System.out.println("Cut Edge: Node " + edge.src + " to Node " + edge.dest);
                        }
                        break;
                    }
                }
                for (Edge edge : adjList.get(v)) {
                    if (edge.dest == u) {
                        edge.residualCapacity += pathFlow;
                        break;
                    }
                }
            }

            maxFlow += pathFlow;
        }
        return maxFlow;
    }
    public BufferedImage createOutputImage(BufferedImage sourceImage, BufferedImage targetImage,
                                           int srcNode, int sinkNode) {
        int width = sourceImage.getWidth();
        int height = sourceImage.getHeight();

        // Create a blank output image of the same size
        BufferedImage outputImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int nodeIndex = y * width + x;

                boolean isConnectedToSource = false;
                boolean isConnectedToSink = false;

                // Check if the node is connected to the imaginary source or sink
                for (Edge edge : adjList.get(srcNode)) {
                    if (edge.dest == nodeIndex && edge.residualCapacity > 0) {
                        isConnectedToSource = true;
                        break;
                    }
                }

                for (Edge edge : adjList.get(nodeIndex)) {
                    if (edge.dest == sinkNode && edge.residualCapacity > 0) {
                        isConnectedToSink = true;
                        break;
                    }
                }

                if (isConnectedToSource) {
                    outputImage.setRGB(x, y, sourceImage.getRGB(x, y));
                } else if (isConnectedToSink) {
                    outputImage.setRGB(x, y, targetImage.getRGB(x, y));
                } else {
                    // If not connected to either, just use the target image as default
                    outputImage.setRGB(x, y, targetImage.getRGB(x, y));
                }
            }
        }

        try {
            ImageIO.write(outputImage, "png", new File("output.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outputImage;
    }

    public static void main(String[] args) throws IOException {
        long startTime = System.nanoTime();
        System.out.println("Starting Graph Cut Texture Synthesis...");

        BufferedImage sourceImage = ImageIO.read(new File("/Users/akshithreddyc/Desktop/Workplace/GraphCut/src.jpeg"));
        BufferedImage targetImage = ImageIO.read(new File("/Users/akshithreddyc/Desktop/Workplace/GraphCut/target.jpeg"));
        int width = sourceImage.getWidth();
        int height = sourceImage.getHeight();

        System.out.println("Initializing graph...");
        GraphCutTextureSynthesis g = new GraphCutTextureSynthesis(width * height + 2);
        int srcNode = width * height;
        int sinkNode = srcNode + 1;

        Point sourceTopLeft = new Point(9, 9);
        Point sourceBottomRight = new Point(29, 29);
        Point targetTopLeft = new Point(79, 79);
        Point targetBottomRight = new Point(99, 99);

        System.out.println("Building graph...");
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int nodeIndex = y * width + x;
                if (x > 0) {
                    Color sourceColor = new Color(targetImage.getRGB(x-1, y));
                    Color targetColor = new Color(sourceImage.getRGB(x, y));
                    g.addEdge(nodeIndex - 1, nodeIndex, g.getWeight(sourceColor, targetColor));
                }
                if (x < width - 1) {
                    Color sourceColor = new Color(targetImage.getRGB(x+1, y));
                    Color targetColor = new Color(sourceImage.getRGB(x, y));
                    g.addEdge(nodeIndex + 1, nodeIndex, g.getWeight(sourceColor, targetColor));
                }
                if (y > 0) {
                    Color sourceColor = new Color(targetImage.getRGB(x, y-1));
                    Color targetColor = new Color(sourceImage.getRGB(x, y));
                    g.addEdge(nodeIndex - width, nodeIndex, g.getWeight(sourceColor, targetColor));
                }
                if (y < height - 1) {
                    Color sourceColor = new Color(targetImage.getRGB(x, y+1));
                    Color targetColor = new Color(sourceImage.getRGB(x, y));
                    g.addEdge(nodeIndex + width, nodeIndex, g.getWeight(sourceColor, targetColor));
                }
                if (x >= sourceTopLeft.x && x <= sourceBottomRight.x && y >= sourceTopLeft.y && y <= sourceBottomRight.y) {
                    g.addEdge(srcNode, nodeIndex, Integer.MAX_VALUE);
                }
                if (x >= targetTopLeft.x && x <= targetBottomRight.x && y >= targetTopLeft.y && y <= targetBottomRight.y) {
                    g.addEdge(nodeIndex, sinkNode, Integer.MAX_VALUE);
                }
            }
        }

        System.out.println("Applying Edmonds-Karp algorithm...");
        int maxFlow = g.edmondsKarp(srcNode, sinkNode);
        System.out.println("Max Flow: " + maxFlow);

        System.out.println("Generating output image...");
        BufferedImage outputImage = g.createOutputImage(sourceImage, targetImage, srcNode, sinkNode);
        ImageIO.write(outputImage, "png", new File("outputPath"));

        System.out.println("Graph Cut Texture Synthesis completed!");

        long endTime = System.nanoTime();
        long durationInNano = (endTime - startTime);  // Total execution time in nano seconds
        double durationInSeconds = durationInNano / 1_000_000_000.0;  // Convert duration to seconds
        System.out.println("Time elapsed: " + durationInSeconds + " seconds");

    }
}
