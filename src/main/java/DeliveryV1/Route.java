package DeliveryV1;

public class Route
{
    private int [] Depot;
    private int FirstPackage;
    private int LastPackage;
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
        int i = 0;
        while (order[i] == -1)
        {
            i += 1;
        }
        FirstPackage = i;
    }

    public void GetLastPackage()
    {
        int i = order.length - 1;
        while (order[i] == -1)
        {
            i -= 1;
        }
        LastPackage = i;
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
        return distanceCalculator(Depot, coordinates[FirstPackage]);
    }

    public int calculateLastDistance(int[][]coordinates)
    {
        return distanceCalculator(Depot, coordinates[LastPackage]);
    }

    // Calculate the total distance of the route
    public void calculateTotalDistance(int[][] distances, int[][] coordinates)
    {
        int distance = 0;
        distance += calculateFirstDistance(coordinates);
        distance += calculateLastDistance(coordinates);
        int j = 0;
        int[] StartCoordinates = coordinates[FirstPackage];
        int[] EndCoordinates = coordinates[LastPackage];
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

