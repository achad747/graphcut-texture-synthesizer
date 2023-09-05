package test.java;
import org.junit.jupiter.api.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import static org.junit.jupiter.api.Assertions.*;

public class GraphCutTextureSynthesisTest {

    @Test
    void testBFSWithExistingPath() {
        // Setup a simple graph where a path exists
        assertTrue(graphCut.bfs(0, 2), "BFS should find an existing path");
    }

    @Test
    void testBFSWithoutPath() {
        // Setup a simple graph where no path exists
        assertFalse(graphCut.bfs(0, 3), "BFS should not find a path when none exists");
    }

    public static void main(String[] args) {
        System.out.println("liajsh");
    }
}
