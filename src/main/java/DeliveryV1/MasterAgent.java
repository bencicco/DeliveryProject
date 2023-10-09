package DeliveryV1;
import jade.core.AID;
import jade.core.Agent;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.AMSService;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.lang.Math;

public class MasterAgent extends Agent
{

    public RouteGroup[] Population;
    public int TotalDrivers;
    public int PopulationSize; //should be initialised through some process. GUI input? ive set default in setup above generateInitialPopulation
    public int Iterations;
    public int[][] Distances; //Distances[x][y] corresponds to the distance between package x and y
    public int[][] Coordinates; //Coordinates[1] refers to a coordinate array for package1: [x,y]
    public int TotalPackages; //The total number of packages
    private AID[] Agents;
    public int[] Capacities;
    private int[] DistanceRestraints;
    private int[][] Routes;


    private int step;
    private MasterAgent ThisIsFucked;

    protected void setup()
    {

        PopulationSize = 100000;
        Population = initialisePopulation();
        processData();
        step = 0;
        ThisIsFucked = this; //This is fucked because if you call this later on it doesn't work because it's in a private class
        System.out.println("Hallo! Master-agent " + getAID().getName() + " is ready.");
        //Stores number of drivers to know when all delivery agents have been added
        System.out.println("Enter the total number of delivery drivers available");
        Scanner scanner = new Scanner(System.in);
        TotalDrivers = scanner.nextInt();
        processData(); //Reads input from test.txt and instantiates Distances,Coordinates and TotalPackages
//        SwingUtilities.invokeLater(() ->
//        {
//            JFrame frame = new JFrame("Coordinate Visualizer");
//            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//            frame.add(new CoordinateVisualizer(Coordinates, Routes)); // Pass your Coordinates[][] array
//            frame.setSize(800, 600);
//            frame.setLocationRelativeTo(null);
//            frame.setVisible(true);
//        });
        addBehaviour(new TickerBehaviour(this, 10000)
        {
            protected void onTick()
            {
                ThisIsFucked.addBehaviour(new RequestPerformer());
                if (Agents != null)
                {
                    if (step == 0 && Agents.length != TotalDrivers) {
                        System.out.println("Still requiring " + (TotalDrivers - Agents.length) + " drivers. Please add more agents in JADE");
                    }
                }
            }
        });
    }

    private class RequestPerformer extends Behaviour
    {
        public void action()
        {

            switch (step)
            {
                case 0:
                    //Retrieves Delivery Agents
                    AMSAgentDescription agents[] = null;
                    try
                    {
                        //This grabs all the agents including the default ones plus MA agent, but its disgusting. Pretty much grab all the agents then filter them
                        //would be much nicer if could grab just the delivery agents somehow
                        AMSAgentDescription desc = new AMSAgentDescription();
                        SearchConstraints c = new SearchConstraints();
                        c.setMaxResults(-1L);
                        agents = AMSService.search(ThisIsFucked,desc, c);
                    }
                    catch (Exception e)
                    {
                        System.out.println("Problem searching AMS: " + e);
                        e.printStackTrace();
                    }
                    // We only want the delivery agents, #delivery agents = #agents - 4 (3 default agents + MA agent)
                    Agents = new AID[agents.length - 4];
                    int i = 0;
                    // This stores only the agents named "delivery....." so name delivery agents e.g delivery1"
                    for (AMSAgentDescription agent : agents)
                    {
                        // Creating an INFORM request
                        System.out.println("Sending a message to: " + agent.getName().getLocalName());
                        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                        AID aid = agent.getName();
                        msg.addReceiver(aid);
                        msg.setContent("Are you a delivery Agent?");
                        send(msg);
                        try {
                            // Agent waits for 1 seconds
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            // Handle any exceptions if needed
                            e.printStackTrace();
                        }
                        //Agent waits 2 seconds for other agent to send reply
                        final MessageTemplate msgTemplate  = MessageTemplate.MatchPerformative(ACLMessage.CONFIRM);
                        //Agent waits 2 seconds for other agent to send reply
                        final ACLMessage reply = this.myAgent.receive(msgTemplate);
                        if (reply != null)
                        {
                            String content = reply.getContent();
                            if (Objects.equals(content, "yes"))
                            {
                                System.out.println(reply.getSender().getName());
                                System.out.println("Replied!");
                                Agents[i] = reply.getSender();
                                i += 1;
                            }
                        }
                    }
                    if (Agents.length == TotalDrivers)
                    {
                        System.out.println("Found Agents: ");
                        for (AID agent : Agents)
                        {
                            System.out.println(agent.getLocalName());
                        }
                        step = 1;
                    }
                    else
                    {
                        System.out.println("Could not find all Agents!");
                        boolean confirm = false;
                        String response = "";
                        while(!confirm)
                        {
                            System.out.println("Type yes to search again!");
                            Scanner scanner = new Scanner(System.in);
                            response = scanner.next();
                            if (Objects.equals(response, "yes"))
                            {
                                confirm = true;
                            }
                        }
                    }
                    break;
                case 1:
                    //Retrieves Capacities
                    Capacities = new int[Agents.length];
                    i = 0;
                    for (AID agent : Agents) {
                        // Creating an INFORM request
                        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                        msg.addReceiver(agent);
                        msg.setContent("Give me your Capacity");
                        send(msg);

                        // Template to receive the reply
                        MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
                        msg = blockingReceive(mt);
                        if (msg != null)
                        {
                            String content = msg.getContent();
                            if(!Objects.equals(content, "yes"))
                            {
                                Capacities[i] = Integer.parseInt(content);
                                i += 1;
                            }
                        }
                        else
                        {
                            System.out.println("No reply received");
                        }
                    }
                    if (i == Capacities.length)
                    {
                        step = 2;
                        i = 0;
                        while (i < Capacities.length)
                        {
                            System.out.println(Agents[i].getLocalName() + " capacity: " + Capacities[i]);
                            i++;
                        }
                    }
                    break;
                case 2:
                    DistanceRestraints = new int[Agents.length];
                    i = 0;
                    for (AID agent : Agents) {
                        // Creating an INFORM request
                        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                        msg.addReceiver(agent);
                        msg.setContent("Give me your Distance");
                        send(msg);

                        // Template to receive the reply
                        MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
                        msg = blockingReceive(mt);
                        if (msg != null)
                        {
                            String content = msg.getContent();
                            if(!Objects.equals(content, "yes"))
                            {
                                DistanceRestraints[i] = Integer.parseInt(content);
                                i += 1;
                            }
                        }
                        else
                        {
                            System.out.println("No reply received");
                        }
                    }
                    if (i == DistanceRestraints.length)
                    {
                        step = 3;
                        i = 0;
                        while (i < DistanceRestraints.length)
                        {
                            System.out.println(Agents[i].getLocalName() + " distance constraint: " + DistanceRestraints[i]);
                            i++;
                        }
                    }
                    //Recieve Distance Constraints
                    break;

                case 3:
                    // Calculating Routes
                    // Displaying Routes
                    // Send route to delivery driver
                    break;
            }
        }
        public boolean done()
        {
            return step == 3;
        }
    }

    public RouteGroup[] initialisePopulation()
    {
        RouteGroup[] population = new RouteGroup[PopulationSize];
        // Outer loop: For each RouteGroup in the population:
        for (int i = 0; i < population.length; i++)
        {
            population[i] = new RouteGroup(TotalDrivers);
            // Inner loop 1: For each route in the route group:
            for (int j = 0; j < population[i].Group.length; j++)
            {
                population[i].Group[j] = new Route(new int[Capacities[j]], 0);
                // Inner loop 2: For each package in the route:
                for (int k = 0; k < Capacities[j]; k++)
                {
                    // Set all packages to -1;
                    population[i].Group[j].getOrder()[k] = -1;
                }
            }
        }

        for (RouteGroup solution : population)
        {
            List<Integer> packages = new ArrayList<>();
            for (int i = 0; i < TotalPackages; i++) {
                packages.add(i);
            }
            Random random = new Random();

            while (!packages.isEmpty())
            {
                // Generate a random index within the range of available packages
                int randomIndex = random.nextInt(packages.size());
                int randomPackage = packages.get(randomIndex);
                int randomRoute = random.nextInt(TotalDrivers);
                int randomOrder = random.nextInt(solution.Group[randomRoute].getOrder().length);
                solution.Group[randomRoute].getOrder()[randomOrder] = randomPackage;
                packages.remove(randomIndex);
            }
        }
        Population = population;
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
            int totalDistance = population[i].CalculateTotalDistance(Distances, Coordinates);
            // Float distance = 1 - Float.parseFloat(0 + "." + totalDistance);
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
    public RouteGroup orderedCrossover(RouteGroup parent1, RouteGroup parent2) {
        RouteGroup child = new RouteGroup(parent1.Group.length);
        for (int i = 0; i < parent1.Group.length; i++)
        { // Initialise route distances
            child.Group[i] = new Route(new int[Capacities[i]], 0);
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
        // 5% change of mutating
        int mutation_chance = (int) (Math.random() * 20);
        if (mutation_chance == 5)
        {
            child = swapMutation(child);
        }
        return child;
    }
    public List<RouteGroup> tournamentSelection()
    {
        List<RouteGroup> tournament = new ArrayList<>();
        float[] fitness = evaluateFitness(Population, TotalPackages);
        System.out.println("Average fitness: " + getMedianFitness(fitness));
        while (tournament.size() < PopulationSize / 2 && getMedianFitness(fitness) != 1)
        {
            for (int i = 0; i < fitness.length; i++)
            {
                if (fitness[i] > getMedianFitness(fitness))
                {
                    tournament.add(Population[i]);
                }
            }
        }
        return tournament;
    }

    public RouteGroup FindSolution()
    {
        int iterationCount = 0;
        while (iterationCount < Iterations && getMedianFitness(evaluateFitness(Population, TotalPackages)) != 1)
        {
            createNewGeneration(tournamentSelection());
            iterationCount += 1;
        }
        float f = 0;
        int bestsolutionindex = 0;
        float bestfitness = 0;
        float [] fitness = evaluateFitness(Population, TotalPackages);
        for (int i = 0; i < fitness.length; i++)
        {
            if (fitness[i] > bestfitness)
            {
                bestfitness = fitness[i];
                bestsolutionindex = i;
            }
        }
        return Population[bestsolutionindex];

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
        Population = new RouteGroup[newGeneration.size()];
        for (int i = 0; i < newGeneration.size(); i++)
        {
            Population[i] = newGeneration.get(i);
        }
    }

    public void processData()
    {
        try
        {
            readFile(); //Reads data entry from text file (test.txt) and adds to Coordinates
            updateDistanceArray(); //Uses Coordinates to instantiate Distances

        }
        catch (FileNotFoundException e)
        {
            throw new RuntimeException(e);
        }
    }


    private void readFile() throws FileNotFoundException
    {
        File file = new File("test.txt");
        Scanner scanner = new Scanner(file);
        TotalPackages = scanner.nextInt();  //First line of text file = total number of packages
        Coordinates = new int[TotalPackages][2];
        int i = 0;
        while (scanner.hasNextLine()) //Following lines of text file have xy coordinates in the form x,y
        {
            String line = scanner.nextLine();
            String[] values = line.split(","); // Split the line into two values using a comma as the delimiter
            if (values.length == 2) // Ensure there are two values before trying to parse
            {
                Coordinates[i][0] = Integer.parseInt(values[0].trim()); // Parse and store the values
                Coordinates[i][1] = Integer.parseInt(values[1].trim());
                i += 1;
            }
        }

    }

    private void updateDistanceArray()
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
                    Distances[i][j] = distanceCalculator(Coordinates[i], Coordinates[j]);
                }
            }
        }
    }
    private static int distanceCalculator(int[] a, int[] b)
    {
        double xval = a[0] - b[0];
        double yval = a[1] - b[1];
        double distance = Math.sqrt(xval * xval  + yval * yval);
        return (int) Math.round(distance); //Distance values are rounded to integers
    }
}