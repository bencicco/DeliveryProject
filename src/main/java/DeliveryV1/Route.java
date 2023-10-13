package DeliveryV1;

public class Route
{
    private int [] Depot; // Represents the coordinates of the depot. Depot[0] = x coordinate Depot[1] = y
    private int FirstPackage; // Represents the first non-negative package
    private int LastPackage; // Represents the last non-negative  package
    private int[] order; // Represents the order of packages to be delivered
    public int totalDistance; // Represents the total distance of the route

    public Route(int[] order, int totalDistance)
    {
        this.order = order;
        this.totalDistance = totalDistance;
        GetFirstPackage();
        GetLastPackage();
        Depot = new int[2];
        Depot[0] = 0;
        Depot[1] = 0;
    }

    public void GetFirstPackage()
    {
        for (int i = 0; i < order.length; i++) {
            if (getOrder()[i] >= 0)
            {
                FirstPackage = order[i]; // Update LastPackage to the index of the last non-negative integer
                return; // Exit the loop once the last non-negative integer is found
            }
        }
        // Handle the case where there are no non-negative integers in the array
        FirstPackage = -1; // -1 Represents an empty package, i.e no package to be delivered
    }

    private void GetLastPackage() {
        for (int i = getOrder().length - 1; i >= 0; i--) {
            if (getOrder()[i] >= 0)
            {
                LastPackage = order[i]; // Update LastPackage to the index of the last non-negative integer
                return; // Exit the loop once the last non-negative integer is found
            }
        }
        // Handle the case where there are no non-negative integers in the array
        LastPackage = -1; // set LastPackage to -1 if no non-negative integers are found
    }


    public int[] getOrder()
    {
        return order;
    }

    public int getTotalDistance()
    {
        return totalDistance;
    }

    public int getLength()
    {
        int length = 0;
        for (int i : order)
        {
            if (i > -1)
            {
                length += 1;
            }
        }
        return length;
    }

    public int calculateFirstDistance(int[][]coordinates)
    {
        GetFirstPackage();
        if (FirstPackage != -1)
        {
            return distanceCalculator(Depot, coordinates[FirstPackage]);
        }
        else
        {
            return 0;
        }
    }

    public int calculateLastDistance(int[][]coordinates)
    {
        GetLastPackage();
        if (FirstPackage != -1)
        {
            return distanceCalculator(Depot, coordinates[LastPackage]);
        }
        else
        {
            return 0;
        }
    }

    // Calculate the total distance of the route
    public void calculateTotalDistance(int[][] distances, int[][] coordinates)
    {
        int[] depot = new int[2];
        depot[0] = 0;
        depot[1] = 0;
        int distance = 0;
        GetLastPackage();
        GetFirstPackage();
        if (FirstPackage != -1)
        {
            distance += calculateFirstDistance(coordinates);
            distance += calculateLastDistance(coordinates);
        }
        int j = 0;
        if (FirstPackage != -1 && LastPackage != -1)
        {
            int[] StartCoordinates = coordinates[FirstPackage];
            int[] EndCoordinates = coordinates[LastPackage];
        }
        else
        {
            int[] StartCoordinates = Depot;
            int[] EndCoordinates = Depot;
        }

        for (int i = 0; i < order.length - 1;)
        {
            while(i < order.length && order[i] == -1)
            {
                 i += 1;
            }
             j = i + 1;
            while (j < order.length && order[j] == -1)
            {
                j += 1;
            }
            if (i < order.length && j < order.length && order[i] != -1 && order[j] != -1)
            {
                distance += distances[order[i]][order[j]];
            }
            i++;
        }
        totalDistance = distance;
    }

    private static int distanceCalculator(int[] a, int[] b)
    {
        double xval = a[0] - b[0];
        double yval = a[1] - b[1];
        double distance = Math.sqrt(xval * xval  + yval * yval);
        return (int) Math.round(distance); //Distance values are rounded to integers
    }
}

