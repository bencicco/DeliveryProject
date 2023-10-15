package DeliveryV1;
import javax.swing.*;
import java.awt.*;
import java.util.Random;
import java.util.*;

public class CoordinateVisualizer extends JPanel {
    private int[][] Coordinates;
    private RouteGroup Solution;
    private float ScaleFactorX;
    private float ScaleFactorY;


    public CoordinateVisualizer(int[][] coordinates, RouteGroup solution) {
        this.Coordinates = coordinates;
        this.Solution = solution;
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        getScalingFactor();
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;

        // Draw a black dot at (0, 0)
        g.setColor(Color.BLACK);
        g.fillRect(centerX - 5, centerY - 5, 10, 10);

        for (int i = 0; i < Coordinates.length; i++) {
            int x = (int) (Coordinates[i][0] * ScaleFactorX + centerX);
            int y = (int) (-Coordinates[i][1] * ScaleFactorY + centerY);

            // Skip drawing a circle at (0, 0) since we've already drawn a black dot
            if (Coordinates[i][0] != 0 || Coordinates[i][1] != 0)
            {
                // Generate a random color for other circles
                Random rand = new Random();
                g.setColor(Color.BLACK);
                g.fillOval(x - 5, y - 5, 10, 10);
            }
        }

        // Draw routes based on the provided array
        float thickness = 4.0f; // You can adjust the line thickness as needed
        g2d.setStroke(new BasicStroke(thickness));
        Random rand = new Random();
        Color randomColor = new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
        g.setColor(randomColor);
        for (Route route : Solution.Group)
        {
            randomColor = new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
            g.setColor(randomColor);
            if (route.getOrder().length >= 2) {
                int startX = centerX;
                int startY = centerY;

                for (int i = 0; i < route.getOrder().length; i++) {
                    int endX = (int) (Coordinates[route.getOrder()[i]][0] * ScaleFactorX + centerX);
                    int endY = (int) (-Coordinates[route.getOrder()[i]][1] * ScaleFactorY + centerY);

                    g.drawLine(startX, startY, endX, endY);

                    // Update start coordinates for the next line segment
                    startX = endX;
                    startY = endY;
                }

                // Draw a line back to the base (centre)
                g.drawLine(startX, startY, centerX, centerY);
                if (route.totalDistance != 0)
                {
                    g.drawString("Total Distance: " + route.totalDistance, startX, startY);
                }
            }
        }
    }

    public static void visualizeCoordinates(int[][] coordinates, RouteGroup solution)
    {
        JFrame frame = new JFrame("Coordinate Visualizer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new CoordinateVisualizer(coordinates, solution));
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public void getScalingFactor()
    {
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;

        for (int[] coord : Coordinates)
        {
            maxX = Math.max(maxX, Math.abs(coord[0]));
            maxY = Math.max(maxY, Math.abs(coord[1]));
        }
        ScaleFactorX = (float) ((0.9 * 800) / (2 * maxX));
        ScaleFactorY = (float) ((0.9 * 600) / (2 * maxY));
    }
}
