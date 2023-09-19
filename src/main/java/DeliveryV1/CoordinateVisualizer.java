package DeliveryV1;
import javax.swing.*;
import java.awt.*;
import java.util.Random;

public class CoordinateVisualizer extends JPanel {
    private int[][] coordinates;
    private int[][] routes;

    public CoordinateVisualizer(int[][] coordinates, int[][] routes) {
        this.coordinates = coordinates;
        this.routes = routes;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;

        // Draw a black dot at (0, 0)
        g.setColor(Color.BLACK);
        g.fillRect(centerX - 5, centerY - 5, 10, 10);

        for (int i = 0; i < coordinates.length; i++) {
            int x = coordinates[i][0] + centerX;
            int y = -coordinates[i][1] + centerY;

            // Skip drawing a circle at (0, 0) since we've already drawn a black dot
            if (coordinates[i][0] != 0 || coordinates[i][1] != 0) {
                // Generate a random color for other circles
                Random rand = new Random();
                Color randomColor = new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
                g.setColor(randomColor);
                g.fillOval(x - 5, y - 5, 10, 10);
            }
        }

        // Draw routes based on the provided array
        Random rand = new Random();
        Color randomColor = new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
        g.setColor(randomColor);
        for (int[] route : routes) {
            randomColor = new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
            g.setColor(randomColor);
            if (route.length >= 2) {
                int startX = centerX;
                int startY = centerY;

                for (int i = 0; i < route.length; i++) {
                    int endX = coordinates[route[i]][0] + centerX;
                    int endY = -coordinates[route[i]][1] + centerY;

                    g.drawLine(startX, startY, endX, endY);

                    // Update start coordinates for the next line segment
                    startX = endX;
                    startY = endY;
                }

                // Draw a line back to the base (centre)
                g.drawLine(startX, startY, centerX, centerY);
            }
        }
    }

    public static void visualizeCoordinates(int[][] coordinates, int[][] routes) {
        JFrame frame = new JFrame("Coordinate Visualizer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new CoordinateVisualizer(coordinates, routes));
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        // Example coordinates and routes (you can replace this with your data)
        int[][] exampleCoordinates = {
                {0, 0},
                {50, 30},
                {-20, -10},
                {10, -20},
                {-30, 40}
        };

        int[][] exampleRoutes = {
                {3, 1},
                {1, 2, 4},
        };

        SwingUtilities.invokeLater(() -> visualizeCoordinates(exampleCoordinates, exampleRoutes));
    }
}
