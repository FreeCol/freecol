package net.sf.freecol.client.gui.panel;

import java.util.logging.Logger;

import java.util.EventListener;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.BevelBorder;
//import javax.swing.JPanel;


/**
* Superclass for all dialogs in FreeCol. This class also contains
* methods to create simple dialogs.
*/
public class FreeColDialog extends JPanel {
    private static final Logger logger = Logger.getLogger(FreeColDialog.class.getName());

    // Stores the response from the user:
    private Object response = null;

    // Wether or not the user have made the choice.
    private boolean responseGiven = false;




    /**
    * Default constructor.
    */
    public FreeColDialog() {
        try {
            BevelBorder border = new BevelBorder(BevelBorder.RAISED);
            setBorder(border);
        } catch(Exception e) {}
    }




    /**
    * Sets the <code>response</code> and wakes up any thread waiting for this information.
    *
    * @param response The object that should be returned by {@link #getResponse}.
    */
    public synchronized void setResponse(Object response) {
        this.response = response;
        responseGiven = true;
        logger.info("Response has been set.");
        notifyAll();
    }


    /**
    * Returns the <code>response</code> when set by <code>setResponse(Object response)</code>.
    * Waits the thread until then.
    *
    * @return The object as set by {@link #setResponse}.
    */
    public synchronized Object getResponse() {
        // Wait the thread until 'response' is availeble. Notice that we have to process
        // the events manually if the current thread is the Event Dispatch Thread (EDT).

        try {
            if (SwingUtilities.isEventDispatchThread()) {
                EventQueue theQueue = getToolkit().getSystemEventQueue();

                while (!responseGiven) {
                    // This is essentially the body of EventDispatchThread
                    AWTEvent event = theQueue.getNextEvent();
                    Object src = event.getSource();

                    // Block 'MouseEvent' beeing sent to other components:
                    if (event instanceof MouseEvent) {
                        MouseEvent me = (MouseEvent) event;
                        Component dc = SwingUtilities.getDeepestComponentAt(((ComponentEvent) event).getComponent(), me.getX(), me.getY());
                        if (!SwingUtilities.isDescendingFrom(dc, this)) {
                            continue;
                        }
                    }

                    // We cannot call theQueue.dispatchEvent, so I pasted its body here:
                    if (event instanceof ActiveEvent) {
                        ((ActiveEvent) event).dispatch();
                    } else if (src instanceof Component) {
                        ((Component) src).dispatchEvent(event);
                    } else if (src instanceof MenuComponent) {
                        ((MenuComponent) src).dispatchEvent(event);
                    } else {
                        logger.warning("unable to dispatch event: " + event);
                    }
                }
            } else {
                while (!responseGiven) {
                    wait();
                }
            }
        } catch(InterruptedException e){}

        Object tempResponse = response;
        response = null;
        responseGiven = false;

        return tempResponse;
    }


    /**
    * Convenience method for {@link #getResponse}.
    */
    public boolean getResponseBoolean() {
        return ((Boolean) getResponse()).booleanValue();
    }
    
    
    /**
    * Convenience method for {@link #getResponse}.
    */
    public int getResponseInt() {
        return ((Integer) getResponse()).intValue();
    }


    /**
    * Creates a new <code>FreeColDialog</code> with a text and a ok/cancel option.
    * The "ok"-option calls {@link #setResponse setResponse(new Boolean(true))}
    * and the "cancel"-option calls {@link #setResponse setResponse(new Boolean(false))}.
    *
    * @param text The text that explains the choice for the user.
    * @param okText The text displayed on the "ok"-button.
    * @param cancelText The text displayed on the "cancel"-button.
    * @return The <code>FreeColDialog</code>.
    */
    public static FreeColDialog createConfirmDialog(String text, String okText, String cancelText) {
        final JButton okButton = new JButton(okText);

        final FreeColDialog confirmDialog = new FreeColDialog() {
            public void requestFocus() {
                okButton.requestFocus();
            }
        };

        confirmDialog.setLayout(new BorderLayout());

        JPanel labelPanel = new JPanel(new FlowLayout());
        labelPanel.add(new JLabel(text));

        JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton cancelButton = new JButton(cancelText);

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                confirmDialog.setResponse(new Boolean(true));
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                confirmDialog.setResponse(new Boolean(false));
            }
        });

        p1.add(okButton);
        p1.add(cancelButton);

        confirmDialog.add(labelPanel, BorderLayout.CENTER);
        confirmDialog.add(p1, BorderLayout.SOUTH);

        confirmDialog.setSize(confirmDialog.getPreferredSize());

        labelPanel.setOpaque(false);
        p1.setOpaque(false);
        
        return confirmDialog;
    }


    /**
    * Creates a new <code>FreeColDialog</code> with a text field and a ok/cancel option.
    * The "ok"-option calls {@link #setResponse setResponse(textField.getText())}
    * and the "cancel"-option calls {@link #setResponse setResponse(null)}.
    *
    * @param text The text that explains the action to the user.
    * @param defaultValue The default value appearing in the text field.
    * @param okText The text displayed on the "ok"-button.
    * @param cancelText The text displayed on the "cancel"-button.
    * @return The <code>FreeColDialog</code>.
    */
    public static FreeColDialog createInputDialog(String text, String defaultValue, String okText, String cancelText) {
        final JTextField input = new JTextField(defaultValue);

        final FreeColDialog inputDialog = new FreeColDialog()  {
            public void requestFocus() {
                input.requestFocus();
            }
        };

        inputDialog.setLayout(new GridLayout(3, 1));

        JPanel buttons = new JPanel(new GridLayout(1, 2));

        JButton okButton = new JButton(okText);
        JButton cancelButton = new JButton(cancelText);

        input.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                inputDialog.setResponse(input.getText());
            }
        });

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                inputDialog.setResponse(input.getText());
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                inputDialog.setResponse(null);
            }
        });

        buttons.add(okButton);
        buttons.add(cancelButton);

        inputDialog.add(new JLabel(text));
        inputDialog.add(input);
        inputDialog.add(buttons);

        inputDialog.setSize(inputDialog.getPreferredSize());

        buttons.setOpaque(false);

        return inputDialog;
    }
    
}
