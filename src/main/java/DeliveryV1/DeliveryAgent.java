package DeliveryV1;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class DeliveryAgent extends Agent {

    DeliveryAgent thisAgent = this;
    private int Capacity;
    private int MaxDistance;
    private DeliveryAgentGUI GUI;
    private Route route;
    protected void setup()
    {
        route = new Route(new int[Capacity], 0, MaxDistance);
        // Create and show the GUI
        GUI = new DeliveryAgentGUI(this);
        GUI.showGUI();
        //Waits for window to close before continuing
        WaitForWindowClose();
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action()
            {
                ACLMessage request = receive();
                if (request != null)
                {
                    String content = request.getContent();
                    if ("Are you a delivery Agent?".equals(content))
                    {
                        ACLMessage reply = request.createReply();
                        reply.setPerformative(ACLMessage.CONFIRM);
                        reply.setContent("yes"); // Send the agent's AID
                        send(reply);
                    }
                    else if("Give me your Capacity".equals(content))
                    {
                        ACLMessage reply = request.createReply();
                        reply.setPerformative(ACLMessage.INFORM);
                        reply.setContent(String.valueOf(Capacity));
                        send(reply);
                    }

                    else if("Give me your Distance".equals(content))
                    {
                        ACLMessage reply = request.createReply();
                        reply.setPerformative(ACLMessage.INFORM);
                        reply.setContent(String.valueOf(MaxDistance));
                        send(reply);
                    }

                    else if (content.startsWith("Route:")) {
                        // Extract the route information from the message content
                        String routeInfo = content.substring("Route:".length()).trim();
                        String[] packageOrders = routeInfo.split(" ");

                        // Create an array to store the package order
                        int[] packageOrder = new int[packageOrders.length];

                        // Parse the package order
                        for (int i = 0; i < packageOrders.length; i++) {
                            packageOrder[i] = Integer.parseInt(packageOrders[i]);
                        }

                        // Create a new Route object
                        Route newRoute = new Route(packageOrder, 0, MaxDistance);

                        // Now you have the new Route object, you can use it as needed
                        route = newRoute;
                    }
                }
                else
                {
                    block();
                }

            }
        });
    }

    protected void UpdateConstraints(int cap, int distance) {
        Capacity = cap;
        MaxDistance = distance;
    }
    private void WaitForWindowClose()
    {
        try {
            GUI.await();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }
}
