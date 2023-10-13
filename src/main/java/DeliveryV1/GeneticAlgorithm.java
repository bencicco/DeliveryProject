package DeliveryV1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class GeneticAlgorithm
{
    private int PopulationSize;
    private int MutationRate;

    private int Iterations;

    private MasterAgent Master;

    public GeneticAlgorithm(MasterAgent Master, int PopulationSize, int MutationRate, int Iterations)
    {
        this.PopulationSize = PopulationSize;
        this.MutationRate = MutationRate;
        this.Master = Master;
        this.Iterations = Iterations;
    }
    public RouteGroup[] initialisePopulation() // This function intialises the population in two phases
    {
        // Phase one: intialise route lengths and set packages to -1
        RouteGroup[] population = new RouteGroup[PopulationSize];
        for (int i = 0; i < population.length; i++)  // Outer loop: For each RouteGroup in the population:
        {
            population[i] = new RouteGroup(Master.TotalDrivers);
            for (int j = 0; j < population[i].Group.length; j++) // Inner loop 1: For each route in the route group:
            {
                population[i].Group[j] = new Route(new int[Master.Capacities[j]], 0);
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
                int randomIndex = random.nextInt(packages.size());
                int randomPackage = packages.get(randomIndex);
                int randomRoute = random.nextInt(Master.TotalDrivers);
                int randomOrder = random.nextInt(solution.Group[randomRoute].getOrder().length);
                solution.Group[randomRoute].getOrder()[randomOrder] = randomPackage;
                packages.remove(randomIndex);
            }
        }
        Master.Population = population;
        return population;
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
            populationFitness[i] = (float) (packagesDelivered) - (float) (0.001 * totalDistance);
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
        } else {
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


    // This is working! Creates a child from two parent route groups.
    public RouteGroup orderedCrossover(RouteGroup parent1, RouteGroup parent2)
    {
        RouteGroup child = new RouteGroup(parent1.Group.length);
        for (int i = 0; i < parent1.Group.length; i++)
        { // Initialise route distances
            child.Group[i] = new Route(new int[Master.Capacities[i]], 0);
        }
        List<Integer> packageConsistency = new ArrayList<>();
        for (int i = 0; i < child.Group.length; i++)
        {
            int StartPackage = (int) (Math.random() * child.Group[i].getOrder().length);
            int randomEnd = (int) (Math.random() * (child.Group[i].getOrder().length) - StartPackage);
            int EndPackage = child.Group[i].getOrder().length - randomEnd;
            for (int j = 0; j < child.Group[i].getOrder().length; j++)
            {
                if (j >= StartPackage && j <= EndPackage)
                {
                    if(parent1.Group[i].getOrder()[j] != -1)
                    {
                        if (!packageConsistency.contains(parent1.Group[i].getOrder()[j])) {
                            packageConsistency.add(parent1.Group[i].getOrder()[j]);
                            child.Group[i].getOrder()[j] = parent1.Group[i].getOrder()[j];
                        }
                        else
                        {
                            child.Group[i].getOrder()[j] = -1;
                        }
                    }
                    else
                    {
                        child.Group[i].getOrder()[j] = -1;
                    }
                }
                else if (j < StartPackage || j > EndPackage)
                {
                    if(parent2.Group[i].getOrder()[j] != -1)
                    {
                        if (!packageConsistency.contains(parent2.Group[i].getOrder()[j]))
                        {
                            packageConsistency.add(parent2.Group[i].getOrder()[j]);
                            child.Group[i].getOrder()[j] = parent2.Group[i].getOrder()[j];
                        }
                        else
                        {
                            child.Group[i].getOrder()[j] = -1;
                        }
                    }
                    else
                    {
                        child.Group[i].getOrder()[j] = -1;
                    }
                }
            }
        }
        return child;
    }

    // Mutation: Implement a simple swap mutation
    private RouteGroup swapMutation(RouteGroup solution)
    {
        int randomRoute1 = (int) (Math.random() * solution.Group.length);
        int randomRoute2 = (int) (Math.random() * solution.Group.length);
        int randomPos1 = (int) (Math.random() * solution.Group[randomRoute1].getOrder().length);
        int randomPos2 = (int) (Math.random() * solution.Group[randomRoute2].getOrder().length);
        int tempstorage = solution.Group[randomRoute1].getOrder()[randomPos1];
        solution.Group[randomRoute1].getOrder()[randomPos1] = solution.Group[randomRoute2].getOrder()[randomPos2];
        solution.Group[randomRoute2].getOrder()[randomPos2] = tempstorage;
        return solution;
    }

    public RouteGroup crossoverAndMutate(RouteGroup parent1, RouteGroup parent2)
    {
        RouteGroup child = orderedCrossover(parent1, parent2);
        // 25% change of mutating
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
        while (tournament.size() < PopulationSize / 2 && getMedianFitness(fitness) != 1)
        {
            for (int i = 0; i < fitness.length; i++)
            {
                if (fitness[i] > getMedianFitness(fitness))
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
        int iterationCount = 0;
        while (iterationCount < Iterations && getMedianFitness(evaluateFitness(Master.Population, Master.TotalPackages)) != 1)
        {
            createNewGeneration(tournamentSelection());
            iterationCount += 1;
        }
        float f = 0;
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
            int parent1 = (int) (Math.random() * tournament.size());
            int parent2 = (int) (Math.random() * tournament.size());
            while (parent1 == parent2)
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
        if (tournament.size() == 1)
        {
            newGeneration.add(crossoverAndMutate(tournament.get(0), tournament.get(0)));
            newGeneration.add(crossoverAndMutate(tournament.get(0), tournament.get(0)));
            newGeneration.add(crossoverAndMutate(tournament.get(0), tournament.get(0)));
            newGeneration.add(crossoverAndMutate(tournament.get(0), tournament.get(0)));
        }
        Master.Population = new RouteGroup[newGeneration.size()];
        for (int i = 0; i < newGeneration.size(); i++)
        {
            Master.Population[i] = newGeneration.get(i);
        }
    }
}