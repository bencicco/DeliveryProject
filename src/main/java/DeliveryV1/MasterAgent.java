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

import javax.swing.*;

public class MasterAgent extends Agent {

    private List<RouteGroup> population;
    private int TotalDrivers;
    private int PopulationSize; //should be initialised through some process. GUI input? ive set default in setup above generateInitialPopulation
    public int[][] Distances; //Distances[x][y] corresponds to the distance between package x and y
    public int[][] Coordinates; //Coordinates[1] refers to a coordinate array for package1: [x,y]
    private int TotalPackages; //The total number of packages
    private AID[] Agents;
    private int[] Capacities;
    private int[] DistanceRestraints;
    private int[][] Routes;


    private int step;
    private MasterAgent ThisIsFucked;

    protected void setup()
    {

        PopulationSize = 100;
        population = generateInitialPopulation();
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

    private RouteGroup[] initialisePopulation()
    {
        RouteGroup[] population = new RouteGroup[PopulationSize];
        // Outer loop: For each RouteGroup in the population:
        for (int i = 0; i < population.length; i++) {
            population[i] = new RouteGroup(TotalDrivers);
            // Inner loop 1: For each route in the route group:
            for (int j = 0; j < population[i].Group.length; j++) {
                population[i].Group[j] = new Route(new int[Capacities[j]], 0);
                // Inner loop 2: For each package in the route:
                for (int k = 0; k < Capacities[j]; k++)
                {
                    // Set all packages to -1;
                    population[i].Group[j].getOrder()[k] = -1;
                }
            }
        }

        for (RouteGroup solution : population) {
            List<Integer> packages = new ArrayList<>();
            for (int i = 0; i < TotalPackages; i++) {
                packages.add(i);
            }
            Random random = new Random();

            while (!packages.isEmpty()) {

                // Generate a random index within the range of available packages
                int randomIndex = random.nextInt(packages.size());
                int randomPackage = packages.get(randomIndex);
                int randomRoute = random.nextInt(TotalDrivers);
                solution.Group[randomRoute].getOrder()[randomIndex] = randomPackage;
                packages.remove(randomIndex);
            }
        }
        return population;
    }



    private List<RouteGroup> generateInitialPopulation() // prepare for nested loops :)
    {
        List<RouteGroup> Population = new ArrayList<RouteGroup>();

        int[] packageNums = new int[TotalPackages]; // creates an array of numbers, each referring to a package, the number in the array is used as a signifier to identify the packets co-ordinates
        for(int i = 0; i < TotalPackages; i++)
        {
            packageNums[i] = i;
        }

        for(int i = 0; i < PopulationSize; i++) // create a new RouteGroup for each value in the population size
        {
            RouteGroup solution = new RouteGroup(TotalDrivers); // the New initial solution That's going to be added to the overall Population

            for(int j = 0; j < TotalPackages; j++)
            {
                Random rand = new Random();

                int int_random = rand.nextInt(TotalDrivers); //Chooses random driver(route)
                int timer = 0;

                if(solution.GetRoute(int_random).getOrder().length == Capacities[int_random])
                { //check if the capacity of the driver is full
                    i-=1; //restart current iteration. Current concern is if total packages exceeds total capacity of all drivers this will be stuck in a loop
                    continue;
                }

                int position = solution.GetRoute(int_random).getOrder().length + 1;  //This gets the current position of the driver capacity (eg if this has 4 packages it will return 5)

                if (timer == 5)
                { //checks if it's time to input a negative package
                    solution.GetRoute(int_random).getOrder()[position] = -1;  //assigns the negative package
                    timer = 0;
                    continue;
                }
                else
                {
                        timer++;
                }
                solution.GetRoute(int_random).getOrder()[position] = packageNums[j]; //assigns the chosen route(driver) a new package
            }
            Population.add(solution);
        }
        return Population;
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
                    //Calculating Routes//
                    //Displaying Routes//
                    //Send route to delivery driver
                    break;
            }
        }
        public boolean done()
        {
            return step == 3;
        }
    }

    private float[] evaluateFitness(RouteGroup population, int totalPackages)
    {
        float[] populationFitness = new float[population.Group.length];
        float packageAverageDistance = population.GetTotalDistance() / totalPackages;
        for (int i = 0; i < population.Group.length; i++)
        {
            int packagesDelivered = population.GetRoute(i).getOrder().length;
            int totalDistance = population.GetRoute(i).getTotalDistance();
            populationFitness[i] = packagesDelivered - (totalDistance / (totalDistance + (packageAverageDistance * totalPackages)));
        }
        populationFitness = normalise(populationFitness);
        return populationFitness;
    }

    //This isn't functional or tested, just an idea//
    private RouteGroup tournamentSelection(List<RouteGroup> population, int tournamentSize)
    {
        List<RouteGroup> tournament = new ArrayList<>();
        for (int i = 0; i < tournamentSize; i++)
        {
            int randomIndex = (int) (Math.random() * population.size());
            tournament.add(population.get(randomIndex));
        }
        return Collections.min(tournament, Comparator.comparing(RouteGroup::GetTotalDistance));
    }

    // This is working! Creates a child from two parent route groups.
    public RouteGroup orderedCrossover(RouteGroup parent1, RouteGroup parent2)
    {
        RouteGroup Child = new RouteGroup(parent1.Group.length);
        int[] packageConsistency = new int[Coordinates.length];
        int i = 0;
        while (i < Coordinates.length)
        {
            //Used to make sure packages are not assigned twice
            packageConsistency[i] = i;
            i++;
        }
        i = 0;
        while (i < parent1.Group.length)
        {
            // Initiate length of each route
            Child.Group[i] = new Route(new int[parent1.Group[i].getOrder().length], 0);
            i++;
        }
        i = 0;
        while (i < parent1.Group.length)
        {
            // Choose a package to start the inhertiance from parent1
            int startPackage = (int) (Math.random() * parent1.Group[i].getOrder().length);
            // Choose a package to end the inheritance from parent1
            int endPackage = (int) (Math.random() * parent1.Group[i].getOrder().length);
            // Keep generating new end package until it is less than start package
            while (endPackage < startPackage)
            {
                endPackage = (int) (Math.random() * parent1.Group[i].getOrder().length);
            }
            int j = 0;
            while (j < parent1.Group[i].getOrder().length)
            {
                //Starting from the start of the child route, if the index is outside of the start and end package, inherit from package from parent 2
                if (j < startPackage || j > endPackage)
                {
                    if (packageConsistency[Child.Group[i].getOrder()[j]] >= 0 && parent2.Group[i].getOrder()[j] != -1)
                    {
                        Child.Group[i].getOrder()[j] = parent2.Group[i].getOrder()[j];
                        packageConsistency[Child.Group[i].getOrder()[j]] = -1;
                    }
                    else
                    {
                        Child.Group[i].getOrder()[j] = -1;
                    }
                }
                // If index is within start and end package inherit from parent 1.
                if (j >= startPackage && j <= endPackage)
                {
                    if (packageConsistency[Child.Group[i].getOrder()[j]] > -1 && parent1.Group[i].getOrder()[j] != -1)
                    {
                        Child.Group[i].getOrder()[j] = parent1.Group[i].getOrder()[j];
                        packageConsistency[Child.Group[i].getOrder()[j]] = -1;
                    }
                    else
                    {
                        Child.Group[i].getOrder()[j] = -1;
                    }
                }
                j++;
            }
            i++;
        }
        return Child;
    }


//
//    // Mutation: Implement a simple swap mutation
//    private void swapMutation(Route route) {
//        int[] order = route.getOrder();
//        int pos1 = (int) (Math.random() * order.length);
//        int pos2 = (int) (Math.random() * order.length);
//        int temp = order[pos1];
//        order[pos1] = order[pos2];
//        order[pos2] = temp;
//    }
//
//    private List<Route> selectRoutesForReproduction(List<Route> population, int tournamentSize, int numParents) {
//        List<Route> parents = new ArrayList<>();
//        for (int i = 0; i < numParents; i++) {
//            Route parent = tournamentSelection(population, tournamentSize);
//            parents.add(parent);
//        }
//        return parents;
//    }
//
//    private List<Route> crossoverAndMutate(List<Route> parents) {
//        List<Route> offspring = new ArrayList<>();
//        while (offspring.size() < population.size()) {
//            Route parent1 = parents.get((int) (Math.random() * parents.size()));
//            Route parent2 = parents.get((int) (Math.random() * parents.size()));
//            Route child = orderedCrossover(parent1, parent2);
//            swapMutation(child);
//            offspring.add(child);
//        }
//        return offspring;
//    }

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
}