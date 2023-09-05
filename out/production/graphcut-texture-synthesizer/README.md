# Graph Cut Texture Synthesis

This project is an implementation of the Graph Cut Texture Synthesis method inspired by the research paper "Graphcut textures: image and video synthesis using graph cuts" by Vivek Kwatra et al.

## Overview

Texture synthesis is the process of algorithmically generating a large digital image from a small digital sample. This is achieved by leveraging the structural content it contains. The graph cut approach, specifically, uses graph cuts to find the optimal seam between the source and target textures. The ultimate goal is to produce visually pleasing results by minimizing errors along the seams.

## Patch Selection

A standout feature in this project is the ability for users to specify patches from both the source and target images. A patch is a region in the image that a user is particularly interested in. By providing four coordinates for both source and target images, users can dictate the top-left and bottom-right boundaries of these regions. This ensures the regions appear in the synthesized output.

### How it works:

1. **Graph Construction**: Every pixel is a node. These nodes are interconnected, forming connections to their left, right, top, and bottom counterparts. The weights of the edges between nodes are determined based on the color difference between adjacent pixels. An imaginary source and sink node are also added. Nodes from the source patch connect to the imaginary source with a very high weight (implying a strong bond). The same applies for the target patch to the imaginary sink.

2. **Edmonds-Karp Algorithm**: The Edmonds-Karp algorithm is employed to calculate the maximum flow in the graph. This, in turn, helps in identifying the best seam or cut between the source and target textures.

3. **Parallel BFS**: The Breadth First Search (BFS) used in the Edmonds-Karp algorithm has been parallelized using Java's ForkJoinPool to expedite the process.

## Execution Steps:

1. Ensure the required images are available at the designated path.
2. Adjust the patch selection coordinates to fit your requirements.
3. Launch the main method within the `GraphCutTextureSynthesis` class.
4. The synthesized image is stored as `output.png`.

## Results:

Upon completion, the program yields a seamlessly synthesized texture that combines the source and target patches. A performance metric, specifically the time elapsed during the execution, is also presented for analysis.
