package DeliveryV1;

public class RouteGroup
{
    private int totalDistance;
    private int GroupSize;
    public Route[] Group;

    public RouteGroup(int groupSize)
    {
        Group = new Route[groupSize];
        GroupSize = groupSize;
    }

    public Route GetRoute(int i)
    {
        return Group[i];
    }

    public int GetTotalDistance()
    {
        return totalDistance;
    }

    public float CalculateTotalDistance(float[][] Distances, int[][] Coordinates)
    {
        totalDistance = 0;
        for (int i = 0; i < GroupSize; i++)
        {
            if (Group[i] != null)
            {
                Group[i].calculateTotalDistance(Distances, Coordinates);
                totalDistance += Group[i].getTotalDistance();
            }
        }
        return totalDistance;
    }
    public int calculateTotalPackages()
    {
        int totalpackages = 0;
        for (Route route : Group)
        {
            totalpackages += route.getLength();
        }
        return totalpackages;
    }

    public void displayRouteGroup()
    {
        for (Route route : this.Group)
        {
            System.out.println("Route: ");
            for (int delivery : route.getOrder())
            {
                System.out.print(delivery);
                System.out.print(", ");
            }
            System.out.println("");
        }
    }

}


