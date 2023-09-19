package DeliveryV1;

import javax.swing.*;
import java.awt.*;
import java.util.Random;

public class CoordinateVisualizer extends JPanel {
    private int[][] coordinates;

    public CoordinateVisualizer(int[][] coordinates) {
        this.coordinates = coordinates;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;

        // Draw a black square at (0, 0) outside the loop
        g.setColor(Color.BLACK);
        g.fillRect(centerX - 5, centerY - 5, 10, 10); // Adjust the size of the black square

        for (int i = 0; i < coordinates.length; i++) {
            int x = coordinates[i][0] + centerX;
            int y = centerY - coordinates[i][1]; // Adjust Y coordinate without inversion

            // Generate a random color for each circle
            Random rand = new Random();
            Color randomColor = new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));

            g.setColor(randomColor);
            g.fillOval(x - 5, y - 5, 10, 10); // Draw a circle at (x, y)
        }
    }

    public static void visualizeCoordinates(int[][] coordinates) {
        JFrame frame = new JFrame("Coordinate Visualizer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new CoordinateVisualizer(coordinates));
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        // Example coordinates (you can replace this with your Coordinates[][] array)
        int[][] exampleCoordinates = {
                {0, 0},
                {50, 30},
                {-20, -10},
                {10, -20},
                {-30, 40}
        };

        SwingUtilities.invokeLater(() -> visualizeCoordinates(exampleCoordinates));
    }
}




