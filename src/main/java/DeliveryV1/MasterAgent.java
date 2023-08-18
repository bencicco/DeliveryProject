package DeliveryV1;

import jade.core.AID;
import jade.core.Agent;
import java.io.File;
import java.io.FileNotFoundException;
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
    private int[][] Distances; //Distances[x][y] corresponds to the distance between package x and y
    private int[][] Coordinates; //Coordinates[1] refers to a coordinate array for package1: [x,y]
    private int TotalPackages; //The total number of packages
    private AID[] Agents;
    private int[] Capacities;
    private MasterAgent ThisIsFucked;

    protected void setup()
    {
        ThisIsFucked = this; //This is fucked because if you call this later on it doesn't work because it's in a private class
        System.out.println("Hallo! Master-agent " + getAID().getName() + " is ready.");
        ProcessData(); //Reads input from test.txt and instantiates Distances,Coordinates and TotalPackages
        RequestPerformer performer = new RequestPerformer();
        performer.action();
    }
    private class RequestPerformer extends Behaviour { //Need to turn this into a cyclic behaviour
        private int step = 0;
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
                    int j = 0;
                    // This stores only the agents named "delivery....." so name delivery agents e.g delivery1"
                    for (AMSAgentDescription amsAgentDescription : agents) {
                        AID agentID = amsAgentDescription.getName();
                        if (agentID.getName().contains("delivery")) {
                            Agents[j] = amsAgentDescription.getName();
                            j += 1;
                        }
                    }
                    //E
                    Capacities = new int[Agents.length];
                    int i = 0;
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
                            Capacities[i] = Integer.parseInt(content);
                            System.out.println("Received Capacity: " + content);
                        }
                        else
                        {
                            System.out.println("No reply received");
                        }
                    }
                    step = 1;
                    break;
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











