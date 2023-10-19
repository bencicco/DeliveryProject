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

    public int MutationRate; // A number x such that the mutation rate (as a decimal) = 1/x
    public RouteGroup[] Population; // The population for the GA
    public int TotalDrivers; // A number so that the MA knows whether it has found all delivery agents, initalised through user input
    public int PopulationSize; // The population size for the GA
    public int Iterations; // The number of generations for the GA
    public float[][] Distances; // Distances[x][y] corresponds to the distance between package x and y
    public int[][] Coordinates; // Coordinates[1] refers to a coordinate array for package1: [x,y]
    public int TotalPackages; // The total number of packages
    private AID[] Agents; // Stores all the DA agents
    public int[] Capacities; // Stores the capacities for each DA
    public int[] DistanceRestraints; // Stores the distance restraint for each DA

    private RouteGroup Solution;


    private int step;
    private MasterAgent Master;

    protected void setup()
    {
        processData(); // Reads data from text file input
        step = 0;
        Master = this; // This is fucked because if you call this later on it doesn't work because it's in a private class
        System.out.println("Hallo! Master-agent " + getAID().getName() + " is ready.");
        // Stores number of drivers to know when all delivery agents have been added
        System.out.println("Enter the total number of delivery drivers available");
        Scanner scanner = new Scanner(System.in);
        TotalDrivers = scanner.nextInt();
        processData(); // Reads input from test.txt and instantiates Distances,Coordinates and TotalPackages
//        SwingUtilities.invokeLater(() ->
//        {
//            JFrame frame = new JFrame("Coordinate Visualizer");
//            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//            frame.add(new CoordinateVisualizer(Coordinates, Routes)); // Pass your Coordinates[][] array
//            frame.setSize(800, 600);
//            frame.setLocationRelativeTo(null);
//            frame.setVisible(true);
//        });

        addBehaviour(new TickerBehaviour(this, 1000)
        {
            protected void onTick()
            {
                Master.addBehaviour(new RequestPerformer());
            }
        });
    }

    private class RequestPerformer extends Behaviour  // Controls the agent protocol between master agent and delivery agents
    {
        public void action()
        {

            switch (step)
            {
                case 0: // Retrieves Delivery Agents AIDS by first grabbing all Agents in container and then asking each agent if they are a delivery agent
                    AMSAgentDescription agents[] = null;
                    try // Finds all agents in container
                    {
                        AMSAgentDescription desc = new AMSAgentDescription();
                        SearchConstraints c = new SearchConstraints();
                        c.setMaxResults(-1L);
                        agents = AMSService.search(Master,desc, c); // Stores all agents in container
                    }
                    catch (Exception e)
                    {
                        System.out.println("Problem searching AMS: " + e);
                        e.printStackTrace();
                    }
                    // ** We only want the delivery agents, #delivery agents = #agents in container - 4 (3 default agents + MA agent) ** //
                    Agents = new AID[agents.length - 4];
                    int i = 0;
                    for (AMSAgentDescription agent : agents) //
                    {
                        System.out.println("Sending a message to: " + agent.getName().getLocalName());
                        ACLMessage msg = createMessage(agent.getName(), "Are you a delivery Agent?");
                        send(msg);
                        try
                        {
                            Thread.sleep(1000); // Agent waits for 1 seconds for DA to generate reply
                        }
                        catch (InterruptedException e)
                        {
                            // Handle any exceptions if needed
                            e.printStackTrace();
                        }
                        // Agent waits 2 seconds for other agent to send reply
                        final MessageTemplate msgTemplate  = MessageTemplate.MatchPerformative(ACLMessage.CONFIRM);
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
                    if (Agents.length == TotalDrivers) // If the number of agents found = the expected number of agents
                    {
                        System.out.println("Found Agents: ");
                        for (AID agent : Agents)
                        {
                            System.out.println(agent.getLocalName());
                        }
                        step = 1; // Move onto the next step
                    }
                    else
                    {
                        // Wait until user has added all delivery agents to search again
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
                case 1: // Retrieves Capacities from Delivery Agents
                    Capacities = new int[Agents.length];
                    i = 0;
                    for (AID agent : Agents)
                    {
                        // Creating an INFORM request for Delivery Agent to send Capacity
                        ACLMessage msg = createMessage(agent, "Give me your Capacity");
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
                    if (i == Capacities.length) // If the master agent has received expected number of replies
                    {
                        step = 2; // Move onto the next step
                        i = 0;
                        while (i < Capacities.length)
                        {
                            System.out.println(Agents[i].getLocalName() + " capacity: " + Capacities[i]);
                            i++;
                        }
                    }
                    break;
                case 2: // Retrieves Distance Restraints from each DeliveryAgent
                    DistanceRestraints = new int[Agents.length];
                    i = 0;
                    for (AID agent : Agents)
                    {
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
                            if(!Objects.equals(content, "yes")) // Just to make sure that it is not receiving old replies
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
                    if (i == DistanceRestraints.length) // if the number of replies = the expected number of replies
                    {
                        step = 3; // move onto the next step
                        i = 0;
                        while (i < DistanceRestraints.length)
                        {
                            System.out.println(Agents[i].getLocalName() + " distance constraint: " + DistanceRestraints[i]);
                            i++;
                        }
                    }
                    break;

                case 3: // Uses the GA to generate optimal routes for each DA then sends routes to driver
                    System.out.println("Attempting to find solution");
                    //Set GA parameters to default values if not assigned
                    if(PopulationSize == 0) {PopulationSize = 500;}
                    if(MutationRate == 0) {MutationRate = 10;}
                    if(Iterations == 0) {Iterations = 100;}
                    GeneticAlgorithm GA = new GeneticAlgorithm(Master, PopulationSize, MutationRate, Iterations);
                    RouteGroup solution = GA.FindSolution(); // Calls FindSolution() which runs GA
                    System.out.println("Found solution");
                    solution.displayRouteGroup();
                    // ** SEND ROUTES TO DRIVER ** //
                    for (int k = 0; k < Agents.length; k++)
                    {
                        String routeMessage = "Route:";
                        for (int delivery : solution.Group[k].getOrder())
                        {
                            routeMessage += " " + delivery;
                        }

                        ACLMessage msg = createMessage(Agents[k], routeMessage);
                        send(msg);

                    }
                    step = 4; // Agent conversation finish
                    break;
            }
        }
        public boolean done()
        {
            return step == 4;
        }
    }

    public ACLMessage createMessage(AID reciever, String content)
    {
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(reciever);
        msg.setContent(content);
        return msg;
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
        Distances = new float[TotalPackages][TotalPackages];
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
    private static float distanceCalculator(int[] a, int[] b)
    {
        double xval = a[0] - b[0];
        double yval = a[1] - b[1];
        double distance = Math.sqrt(xval * xval  + yval * yval);
        return (float) distance;
    }
}