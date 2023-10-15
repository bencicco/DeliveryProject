package DeliveryV1;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.CountDownLatch;

public class DeliveryAgentGUI extends JFrame
{
    private DeliveryAgent myAgent;
    public JPanel p;
    private final CountDownLatch doneLatch = new CountDownLatch(1);

    private JTextField maximumParcels, maximumDistance;

    DeliveryAgentGUI(DeliveryAgent a)
    {
        super(a.getLocalName());

        myAgent = a;
        p = new JPanel();
        p.setLayout(new GridLayout(2, 2));
        p.add(new JLabel("Package Capacity:"));
        maximumParcels = new JTextField(15);
        p.add(maximumParcels);
        p.add(new JLabel("Maximum Distance"));
        maximumDistance = new JTextField(15);
        p.add(maximumDistance);
        getContentPane().add(p, BorderLayout.CENTER);
        JButton addButton = new JButton("Add");
        addButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent ev)
            {
                try
                {
                    String maxParcels = maximumParcels.getText().trim();
                    String maxDistance = maximumDistance.getText().trim();
                    myAgent.UpdateConstraints(Integer.parseInt(maxParcels), Integer.parseInt(maxDistance));
                    maximumParcels.setText("");
                    maximumDistance.setText("");
                    doneLatch.countDown();  // Signal that we're done
                }
                catch (Exception e)
                {
                    JOptionPane.showMessageDialog(DeliveryAgentGUI.this, "Invalid values. " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        p = new JPanel();
        p.add(addButton);
        getContentPane().add(p, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                doneLatch.countDown();  // Signal that we're done
            }
        });

        setResizable(false);
    }

    public void showGUI()
    {
        pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int centerX = (int) screenSize.getWidth() / 2;
        int centerY = (int) screenSize.getHeight() / 2;
        setLocation(centerX - getWidth() / 2, centerY - getHeight() / 2);
        super.setVisible(true);
    }

    public void await() throws InterruptedException
    {
        doneLatch.await();
    }
}
