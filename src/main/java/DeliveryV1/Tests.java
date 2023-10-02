package DeliveryV1;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Tests
{
    public int[] route1Order = new int[4];
    public int[] route2Order = new int[4];
    Route route1;
    Route route2;
    RouteGroup parent1;
    RouteGroup parent2;
    MasterAgent master;
    @Before
    public void Setup()
    {
        //Instantiate route1
        route1Order[0] = -1;
        route1Order[1] = 1;
        route1Order[2] = 2;
        route1Order[3] = -1;
        route1 = new Route(route1Order, 0);
        //Instantiate route2
        route2Order[0] = 3;
        route2Order[1] = 2;
        route2Order[2] = 0;
        route2Order[3] = 1;
        route2 = new Route(route2Order, 0);
        parent1 = new RouteGroup(1);
        parent1.Group[0] = route1;
        parent2 = new RouteGroup(1);
        parent2.Group[0] = route2;
        master = new MasterAgent();
        master.processData();
    }

    @Test
    public void DistanceCheck()
    {
        route1.calculateTotalDistance(master.Distances, master.Coordinates);
        assertEquals(12, route1.totalDistance);
    }
    @Test
    public void OrderedCrossOverTest()
    {
        System.out.println(" ");
        System.out.println("Parent1: ");
        for(int i : parent1.Group[0].getOrder())
        {
            System.out.print(i);
        }
        System.out.println("");
        System.out.println("Parent2: ");

        for(int i : parent2.Group[0].getOrder())
        {
            System.out.print(i);
        }
        System.out.println("");
        RouteGroup child = master.orderedCrossover(parent2, parent1);
        System.out.println("");
        System.out.println("Child: ");
        for(int i : child.Group[0].getOrder())
        {
            System.out.print(i);
        }
        System.out.println("");
    }
}
