package DeliveryV1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class GeneticAlgorithm
{
    private int PopulationSize; // Size of the population
    private int MutationRate; // A number x such that chance of mutation = 1/x
    private int Iterations; // Number of generations till complete
    private MasterAgent Master;

    public GeneticAlgorithm(MasterAgent Master, int PopulationSize, int MutationRate, int Iterations)
    {
        this.PopulationSize = PopulationSize;
        this.MutationRate = MutationRate;
        this.Master = Master;
        this.Iterations = Iterations;
    }
    public void initialisePopulation() // This function intialises the population in two phases
    {
        // Phase one: intialise route lengths and set packages to -1
        RouteGroup[] population = new RouteGroup[PopulationSize];
        for (int i = 0; i < population.length; i++)  // Outer loop: For each RouteGroup in the population:
        {
            population[i] = new RouteGroup(Master.TotalDrivers);
            for (int j = 0; j < population[i].Group.length; j++) // Inner loop 1: For each route in the route group:
            {
                population[i].Group[j] = new Route(new int[Master.Capacities[j]], 0, Master.DistanceRestraints[j]);
                for (int k = 0; k < Master.Capacities[j]; k++)  // Inner loop 2: For each package in the route:
                {
                    // Set all packages to -1;
                    population[i].Group[j].getOrder()[k] = -1;
                    // -1 Represents an empty package, i.e no package. A route consisting entirely of -1 is a null route
                }
            }
        }
        // Phase two: Assign random packages to each 'null' route
        for (RouteGroup solution : population)
        {
            List<Integer> packages = new ArrayList<>();
            for (int i = 0; i < Master.TotalPackages; i++)
            {
                packages.add(i);
            }
            Random random = new Random();

            while (!packages.isEmpty())
            {
                // Generate a random index within the range of available packages
                int randomIndex = random.nextInt(packages.size()); // Choose a random index
                int randomPackage = packages.get(randomIndex); // Package corresponding to random index
                int randomRoute = random.nextInt(Master.TotalDrivers); // Choose random route
                int randomOrder = random.nextInt(solution.Group[randomRoute].getOrder().length);
                int originalPackage = solution.Group[randomRoute].getOrder()[randomOrder];
                solution.Group[randomRoute].calculateTotalDistance(Master.Distances, Master.Coordinates); // Choose a random index of route
                packages.remove(randomIndex);
                if (solution.Group[randomRoute].totalDistance < solution.Group[randomRoute].maxDistance)  // If the cyrrent route has not exceded its max Distance
                {
                    solution.Group[randomRoute].getOrder()[randomOrder] = randomPackage; // Assign random package
                    solution.Group[randomRoute].calculateTotalDistance(Master.Distances, Master.Coordinates);
                    if (solution.Group[randomRoute].totalDistance > solution.Group[randomRoute].maxDistance) // If the change has caused the route to exceed the distance
                    {
                        solution.Group[randomRoute].getOrder()[randomOrder] = originalPackage; //Undo the change
                    }

                }
            }
        }
        Master.Population = population;
    }

    public float[] evaluateFitness(RouteGroup[] population, int totalPackages)
    {
        float[] populationFitness = new float[population.length];
        float routegroupAverageDistance = 0;
        for (RouteGroup routegroup : population)
        {
            routegroupAverageDistance += routegroup.GetTotalDistance() / totalPackages;
        }
        for (int i = 0; i < population.length; i++)
        {
            int packagesDelivered = population[i].calculateTotalPackages();
            int totalDistance = population[i].CalculateTotalDistance(Master.Distances, Master.Coordinates);
            populationFitness[i] = (float) packagesDelivered - (totalDistance / (totalDistance + (routegroupAverageDistance * totalPackages)));
        }
        populationFitness = normalise(populationFitness);
        return populationFitness;
    }

    private static float getMedianFitness(float[] a) {
        // Make a copy of the input array to avoid modifying the original array
        float[] sorted = a.clone();
        Arrays.sort(sorted);

        int length = sorted.length;

        if (length % 2 == 0) {
            // If the length of the array is even, return the average of the two middle values
            int middleIndex1 = length / 2 - 1;
            int middleIndex2 = length / 2;
            return (sorted[middleIndex1] + sorted[middleIndex2]) / 2.0f;
        }
        else
        {
            // If the length of the array is odd, return the middle value
            int middleIndex = length / 2;
            return sorted[middleIndex];
        }
    }


    private static float[] normalise(float[] a)
    {
        float[] result = new float[a.length];
        float[] sorted = a.clone();
        Arrays.sort(sorted);

        float minVal = sorted[0];
        float maxVal = sorted[sorted.length - 1];
        for (int i = 0; i < a.length; i++)
        {
            result[i] = (a[i] - minVal) / (maxVal - minVal);
        }
        return result;
    }

    public RouteGroup orderedCrossover(RouteGroup parent1, RouteGroup parent2)
    {
        RouteGroup child = new RouteGroup(parent1.Group.length); // Initialise number of routes of child
        // ** Initialize the length of each route within child RouteGroup ** //
        for (int i = 0; i < parent1.Group.length; i++)
        {
            child.Group[i] = new Route(new int[Master.Capacities[i]], 0, Master.DistanceRestraints[i]);
        }
        List<Integer> packageConsistency = new ArrayList<>(); // Used to ensure a package isn't delivered twice
        for (int i = 0; i < child.Group.length; i++)
        {
            // ** Choose random section for child to inherit from parent 1 ** //
            int StartPackage = (int) (Math.random() * child.Group[i].getOrder().length);
            int randomEnd = (int) (Math.random() * ((child.Group[i].getOrder().length) - StartPackage));
            int EndPackage = child.Group[i].getOrder().length - randomEnd;
            // ** Assign child packages based on sections inherited from parent 1, other spaces filled in by parent 2 ** //

            for (int j = 0; j < child.Group[i].getOrder().length; j++)
            {
                if (j >= StartPackage && j <= EndPackage) // If the package is in the section to be inherited from parent 1
                {
                    if (parent1.Group[i].getOrder()[j] != -1) // If the package is not an empty package
                    {
                        if (!packageConsistency.contains(parent1.Group[i].getOrder()[j])) // If the package has not already been assigned
                        {
                            packageConsistency.add(parent1.Group[i].getOrder()[j]); // Update the package consistency
                            child.Group[i].getOrder()[j] = parent1.Group[i].getOrder()[j];// Assign the package to the child
                            child.Group[i].calculateTotalDistance(Master.Distances, Master.Coordinates);
                            if (child.Group[i].totalDistance > child.Group[i].maxDistance)
                            {
                                child.Group[i].getOrder()[j] = -1;
                            }
                        }
                        else
                        {
                            child.Group[i].getOrder()[j] = -1; // If the package has already been assigned, assign empty package to child
                        }
                    }
                    else
                    {
                        child.Group[i].getOrder()[j] = -1; // If the parent package is -1 assign -1 to child
                    }
                }
                else if (j < StartPackage || j > EndPackage) // If the package to be assigned is from child two
                {
                    if (parent2.Group[i].getOrder()[j] != -1) {
                        if (!packageConsistency.contains(parent2.Group[i].getOrder()[j])) // Ensure the package to be inherited has not been assigned
                        {
                            packageConsistency.add(parent2.Group[i].getOrder()[j]);
                            child.Group[i].getOrder()[j] = parent2.Group[i].getOrder()[j];
                        }
                        else
                        {
                            child.Group[i].getOrder()[j] = -1; // Inherit nothing if parent package has already been assigned
                        }
                    }
                    else
                    {
                        child.Group[i].getOrder()[j] = -1; // Inherit null package from parent 2
                    }
                }

            }
        }
        return child;
    }

    // ** Mutation: Swaps two random packages ** //
    private RouteGroup swapMutation(RouteGroup solution)
    {  int[] subroute1 = null;
        int[] subroute2 = null;
        int[] modifiedsubroute1 = null;
        int[] modifiedsubroute2 = null;
        // Pick two random routes from the RouteGroup
        int randomRoute1 = (int) (Math.random() * solution.Group.length);
        int randomRoute2 = (int) (Math.random() * solution.Group.length);
        // Pick a random package from each random route
        int randomPos1 = (int) (Math.random() * solution.Group[randomRoute1].getOrder().length);
        int randomPos2 = (int) (Math.random() * solution.Group[randomRoute2].getOrder().length);
        if (randomPos1 != 0 && randomPos1 != solution.Group[randomRoute1].getOrder().length - 1)
        {
            subroute1 = new int[3];
            subroute1[0] = solution.Group[randomRoute1].getOrder()[randomPos1 - 1];
            subroute1[1] = solution.Group[randomRoute1].getOrder()[randomPos1];
            subroute1[2] = solution.Group[randomRoute1].getOrder()[randomPos1 + 1];
            modifiedsubroute1 = new int[3];
            modifiedsubroute1[0] = solution.Group[randomRoute1].getOrder()[randomPos1 - 1];
            modifiedsubroute1[1] = solution.Group[randomRoute2].getOrder()[randomPos2];
            modifiedsubroute1[2] = solution.Group[randomRoute1].getOrder()[randomPos1 + 1];
        }
        else if (randomPos1 == 0)
        {
            subroute1 = new int[2];
            subroute1[0] = solution.Group[randomRoute1].getOrder()[0];
            subroute1[1] = solution.Group[randomRoute1].getOrder()[1];
            modifiedsubroute1 = new int[2];
            modifiedsubroute1[0] = solution.Group[randomRoute2].getOrder()[randomPos2];
            modifiedsubroute1[1] = solution.Group[randomRoute1].getOrder()[1];
        }
        else if (randomPos1 == solution.Group[randomRoute1].getOrder().length - 1)
        {
            subroute1 = new int[2];
            subroute1[0] = solution.Group[randomRoute1].getOrder()[solution.Group[randomRoute1].getOrder().length - 1];
            subroute1[1] = solution.Group[randomRoute1].getOrder()[solution.Group[randomRoute1].getOrder().length - 2];
            modifiedsubroute1 = new int[2];
            modifiedsubroute1[0] = solution.Group[randomRoute2].getOrder()[randomPos2];
            modifiedsubroute1[1] = solution.Group[randomRoute1].getOrder()[solution.Group[randomRoute1].getOrder().length - 2];
        }
        if (randomPos2 != 0 && randomPos2 != solution.Group[randomRoute2].getOrder().length - 1)
        {
            subroute2 = new int[3];
            subroute2[0] = solution.Group[randomRoute2].getOrder()[randomPos2 - 1];
            subroute2[1] = solution.Group[randomRoute2].getOrder()[randomPos2];
            subroute2[2] = solution.Group[randomRoute2].getOrder()[randomPos2 + 1];
            modifiedsubroute2 = new int[3];
            modifiedsubroute2[0] = solution.Group[randomRoute2].getOrder()[randomPos2 - 1];
            modifiedsubroute2[1] = solution.Group[randomRoute1].getOrder()[randomPos1];
            modifiedsubroute2[2] = solution.Group[randomRoute2].getOrder()[randomPos2 + 1];
        }
        else if (randomPos2 == 0)
        {
            subroute2 = new int[2];
            subroute2[0] = solution.Group[randomRoute1].getOrder()[0];
            subroute2[1] = solution.Group[randomRoute2].getOrder()[1];
            modifiedsubroute2 = new int[2];
            modifiedsubroute2[0] = solution.Group[randomRoute1].getOrder()[randomPos1];
            modifiedsubroute2[1] = solution.Group[randomRoute2].getOrder()[1];
        }
        else if (randomPos2 == solution.Group[randomRoute2].getOrder().length - 1)
        {
            subroute2 = new int[2];
            subroute2[0] = solution.Group[randomRoute2].getOrder()[solution.Group[randomRoute2].getOrder().length - 1];
            subroute2[1] = solution.Group[randomRoute2].getOrder()[solution.Group[randomRoute2].getOrder().length - 2];
            modifiedsubroute2 = new int[2];
            modifiedsubroute2[0] = solution.Group[randomRoute1].getOrder()[randomPos1];
            modifiedsubroute2[1] = solution.Group[randomRoute2].getOrder()[solution.Group[randomRoute2].getOrder().length - 2];
        }
        solution.Group[randomRoute1].calculateTotalDistance(Master.Distances, Master.Coordinates);
        solution.Group[randomRoute2].calculateTotalDistance(Master.Distances, Master.Coordinates);
        Route route1 = new Route(subroute1, 0, Master.DistanceRestraints[randomRoute1]);
        Route route2 = new Route(subroute2, 0, Master.DistanceRestraints[randomRoute2]);
        Route modifiedroute1 = new Route(modifiedsubroute1, 0, Master.DistanceRestraints[randomRoute1]);
        Route modifiedroute2 = new Route(modifiedsubroute2, 0, Master.DistanceRestraints[randomRoute2]);
        route1.calculateTotalDistance(Master.Distances, Master.Coordinates);
        route2.calculateTotalDistance(Master.Distances, Master.Coordinates);
        modifiedroute1.calculateTotalDistance(Master.Distances, Master.Coordinates);
        modifiedroute2.calculateTotalDistance(Master.Distances, Master.Distances);
        if (solution.Group[randomRoute1].totalDistance - route1.totalDistance + modifiedroute1.totalDistance <= solution.Group[randomRoute1].maxDistance && solution.Group[randomRoute2].totalDistance - route2.totalDistance + modifiedroute2.totalDistance <= solution.Group[randomRoute2].maxDistance)
        {
            int tempstorage = solution.Group[randomRoute1].getOrder()[randomPos1]; // Store the original value
            // Reassign values
            solution.Group[randomRoute1].getOrder()[randomPos1] = solution.Group[randomRoute2].getOrder()[randomPos2];
            solution.Group[randomRoute2].getOrder()[randomPos2] = tempstorage;
        }
            return solution;
    }

    // CrossOver and Mutate: Combines swapMutation and Ordered Cross over using MutationRate
    public RouteGroup crossoverAndMutate(RouteGroup parent1, RouteGroup parent2)
    {
        RouteGroup child = orderedCrossover(parent1, parent2);
        // ** Chance of mutation rate = 1/MutationRate ** //
        int mutation_chance = (int) (Math.random() * MutationRate);
        if (mutation_chance == 1)
        {
            child = swapMutation(child);
        }
        return child;
    }
    public List<RouteGroup> tournamentSelection()
    {
        List<RouteGroup> tournament = new ArrayList<>();
        float[] fitness = evaluateFitness(Master.Population, Master.TotalPackages);
        float average_fitness = getMedianFitness(fitness);
        while (tournament.size() < PopulationSize / 2 && average_fitness != 1)
        {
            for (int i = 0; i < fitness.length; i++)
            {
                if (fitness[i] > getMedianFitness(fitness)) //Ensures 50% of populations survives
                {
                    tournament.add(Master.Population[i]);
                }
            }
        }
        return tournament;
    }

    public RouteGroup FindSolution()
    {
        initialisePopulation();
        System.out.println("Population Initialised!");
        int iterationCount = 0;
        // ** While max Iterations has not been reached and population has not converged ** //
        while (iterationCount < Iterations && getMedianFitness(evaluateFitness(Master.Population, Master.TotalPackages)) != 1)
        {
            createNewGeneration(tournamentSelection()); // Produce next Generation
            iterationCount += 1;
            System.out.println("Population: " + iterationCount);
        }
        int bestsolutionindex = 0;
        float bestfitness = 0;
        float [] fitness = evaluateFitness(Master.Population, Master.TotalPackages);
        for (int i = 0; i < fitness.length; i++)
        {
            if (fitness[i] > bestfitness)
            {
                bestfitness = fitness[i];
                bestsolutionindex = i;
            }
        }
        return Master.Population[bestsolutionindex];
    }

    public void createNewGeneration (List<RouteGroup> tournament)
    {
        List<RouteGroup> newGeneration = new ArrayList<>();
        while (tournament.size() > 1)
        {
            // ** Select random two parents from tournament ** //
            int parent1 = (int) (Math.random() * tournament.size());
            int parent2 = (int) (Math.random() * tournament.size());
            while (parent1 == parent2) // if parent1 and parent2 are the same, reassign parent 2
            {
                parent2 = (int) (Math.random() * tournament.size());
            }
            newGeneration.add(crossoverAndMutate(tournament.get(parent1), tournament.get(parent2)));
            newGeneration.add(crossoverAndMutate(tournament.get(parent1), tournament.get(parent2)));
            newGeneration.add(crossoverAndMutate(tournament.get(parent1), tournament.get(parent2)));
            newGeneration.add(crossoverAndMutate(tournament.get(parent1), tournament.get(parent2)));

            tournament.remove(parent1);
            if (parent2 > 0)
            {
                tournament.remove(parent2 - 1);
            } else
            {
                tournament.remove(parent2);
            }
        }
        // If there's one parent left, undergo Asexual reproduction
        if (tournament.size() == 1)
        {
            newGeneration.add(crossoverAndMutate(tournament.get(0), tournament.get(0)));
            newGeneration.add(crossoverAndMutate(tournament.get(0), tournament.get(0)));
            newGeneration.add(crossoverAndMutate(tournament.get(0), tournament.get(0)));
            newGeneration.add(crossoverAndMutate(tournament.get(0), tournament.get(0)));
        }
        Master.Population = new RouteGroup[newGeneration.size()]; //Reassign population
        for (int i = 0; i < newGeneration.size(); i++)
        {
            Master.Population[i] = newGeneration.get(i);
        }
    }
}
