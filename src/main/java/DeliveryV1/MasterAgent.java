package DeliveryV1;

import jade.core.AID;
import jade.core.Agent;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Objects;
import java.util.Scanner;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.AMSService;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class MasterAgent extends Agent {

    private int TotalDrivers;
    private int[][] Distances; //Distances[x][y] corresponds to the distance between package x and y
    private int[][] Coordinates; //Coordinates[1] refers to a coordinate array for package1: [x,y]
    private int TotalPackages; //The total number of packages
    private AID[] Agents;
    private int[] Capacities;

    private int step;
    private MasterAgent ThisIsFucked;

    protected void setup()
    {
        step = 0;
        ThisIsFucked = this; //This is fucked because if you call this later on it doesn't work because it's in a private class
        System.out.println("Hallo! Master-agent " + getAID().getName() + " is ready.");
        //Stores number of drivers to know when all delivery agents have been added
        System.out.println("Enter the total number of delivery drivers available");
        Scanner scanner = new Scanner(System.in);
        TotalDrivers = scanner.nextInt();
        ProcessData(); //Reads input from test.txt and instantiates Distances,Coordinates and TotalPackages
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
    private class RequestPerformer extends Behaviour { //Need to turn this into a cyclic behaviour
        public void action()
        {

            switch (step) {
                case 0:
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
                        System.out.println("Sending a message to: " + agent.getName());
                        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                        AID aid = agent.getName();
                        msg.addReceiver(aid);
                        msg.setContent("Are you a delivery Agent?");
                        send(msg);
                        try {
                            // Agent waits for 2 seconds
                            Thread.sleep(1000); // 2 seconds
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
                    if (Agents[Agents.length - 1] != null)
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
                    //Calculating Routes//
                    break;

                case 3:
                    //Send route to delivery driver
            }
        }
        public boolean done()
        {
            return step == 2;
        }
    }



    private void ProcessData()
    {
        try
        {
            ReadFile(); //Reads data entry from text file (test.txt) and adds to Coordinates
            UpdateDistanceArray(); //Uses Coordinates to instantiate Distances

        }
        catch (FileNotFoundException e)
        {
            throw new RuntimeException(e);
        }
    }


    private void ReadFile() throws FileNotFoundException {
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
                Coordinates[i][1] = Integer.parseInt(values[0].trim());
                i += 1;
            }
        }

    }

    private void UpdateDistanceArray()
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
                    Distances[i][j] = DistanceCalculator(Coordinates[i], Coordinates[j]);
                }
            }
        }
    }
    private static int DistanceCalculator(int[] a, int[] b)
    {
        double xval = a[0] - b[0];
        double yval = a[1] - b[1];
        double distance = Math.sqrt(xval * xval  + yval * yval);
        return (int) Math.round(distance); //Distance values are rounded to integers
    }
}











