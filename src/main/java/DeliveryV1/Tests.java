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
        master.Capacities = new int[2];
        master.PopulationSize = 2;
        master.TotalDrivers = 2;
        master.Capacities[0] = 3;
        master.Capacities[1] = 4;
    }

    @Test
    public void DistanceCheck()
    {
        route1.calculateTotalDistance(master.Distances, master.Coordinates);
        assertEquals(12, route1.totalDistance);
    }

    @Test
    public void orderedCrossOverMutateTest()
    {
        RouteGroup[] population = master.initialisePopulation();
        for(int i = 0; i < 2; i ++)
        {
            System.out.println("parent: " + (i + 1));
            for(Route route : population[i].Group)
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
        RouteGroup child = master.crossoverAndMutate(population[1], population[0]);
        System.out.println("Child: ");
        for (Route route : child.Group)
        {
            System.out.println("Route: ");
            for (int delivery : route.getOrder())
            {
                System.out.print(delivery);
                System.out.print(", ");
            }
            System.out.println("");
        }
        System.out.println(" ");


    }

    @Test
    public void PopulationGenerationTest()
    {
        RouteGroup[] population = master.initialisePopulation();
        for(RouteGroup solution : population)
        {
            System.out.println("Solution: ");
            for(Route route : solution.Group)
            {
                System.out.println("Route: ");
                for(int delivery : route.getOrder())
                {
                    System.out.print(delivery);
                }
                System.out.println("");
            }
        }
    }
}
