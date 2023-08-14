package DeliveryV1;

import jade.core.Agent;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import jade.core.behaviours.Behaviour;
public class MasterAgent extends Agent {
    private int[][] Distances; //Distances[x][y] corresponds to the distance between package x and y
    private int[][] Coordinates; //Coordinates[1] refers to a coordinate array for package1: [x,y]
    private int TotalPackages; //The total number of packages

    protected void setup()
    {
        System.out.println("Hallo! Master-agent " + getAID().getName() + " is ready.");
        ProcessData(); //Reads input from test.txt and instantiates Distances,Coordinates and TotalPackages
    }

    private class RequestPerformer extends Behaviour {
        private int step = 0;
        public void action()
        {
            // Implement the specific actions agent should perform here
            // For example, sending messages, receiving replies, decision-making, etc.
            step = 1;
        }
        public boolean done()
        {
            return step == 1;
        }
    }



    private void ProcessData()
    {
        try
        {
            ReadFile(); //Reads data entry from text file (test.txt) and adds to Coordinates
            UpdateDistanceArray(); //Uses Coordinates to instantiate Distances

        }
        catch (FileNotFoundException e)
        {
            throw new RuntimeException(e);
        }
    }


    private void ReadFile() throws FileNotFoundException {
        File file = new File("test.txt");
        Scanner scanner = new Scanner(file);
        TotalPackages = scanner.nextInt();  //First line of text file = total number of packages
        Coordinates = new int[TotalPackages][2];
        int i = 0;
        //Following lines of text file have xy coordinates in the form x,y
        while (scanner.hasNextLine())
        {
            String line = scanner.nextLine();
            // Split the line into two values using a comma as the delimiter
            String[] values = line.split(",");
            // Ensure there are two values before trying to parse
            if (values.length == 2)
            {
                // Parse and store the values
                Coordinates[i][0] = Integer.parseInt(values[0].trim());
                Coordinates[i][1] = Integer.parseInt(values[0].trim());
                i += 1;
            }
        }

    }

    private void UpdateDistanceArray()
    {
        Distances = new int[TotalPackages][TotalPackages];
        for(int i = 0; i <  TotalPackages; i++)
        {
            for (int j = 0; j < TotalPackages; j++)
            {
                if (i == j)
                {
                    Distances[i][j] = 0;
                }
                else
                {
                    Distances[i][j] = DistanceCalculator(Coordinates[i], Coordinates[j]);
                }
            }
        }
    }
    private static int DistanceCalculator(int[] a, int[] b)
    {
        double xval = a[0] - b[0];
        double yval = a[1] - b[1];
        double distance = Math.sqrt(xval * xval  + yval * yval);
        //Distance values are rounded to integers//
        return (int) Math.round(distance);
    }
}











