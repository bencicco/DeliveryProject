package DeliveryV1;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class DeliveryAgent extends Agent {
    private int Capacity;
    private int MaxDistance;
    private int[] Details;
    private DeliveryAgentGUI GUI;
    protected void setup() {
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
