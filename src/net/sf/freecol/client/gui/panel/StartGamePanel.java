
package net.sf.freecol.client.gui.panel;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.networking.Client;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Game;

/**
* The panel where you choose your nation and color and connected players are shown.
*/
public final class StartGamePanel extends JPanel implements ActionListener {
    
    public static final String  COPYRIGHT = "Copyright (C) 2003 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    private static final Logger logger = Logger.getLogger(StartGamePanel.class.getName());
    
    private static final int    START = 0,
                                CANCEL = 1,
                                MAPSIZE = 2,
                                NATION = 3,
                                COLOR = 4,
                                READY = 5,
                                CHAT = 6;
    
    private final Object[]  mapSizes = {"Small", "Medium", "Large", "Huge"};
    private final Object[]  nations = {"Dutch", "French", "English", "Spanish"};
    private final Object[]  colors = {"Black", "Blue", "Cyan", "Gray", "Green", "Magenta", "Orange", "Pink", "Red", "White", "Yellow"};

    private final Canvas        parent;
    private final FreeColClient freeColClient;
    private Game                game;
    private Player              thisPlayer;

    private final JComboBox     mapSize,
                                nation,
                                color;
    private final JLabel        name;
    private final JCheckBox     readyBox;

    

    private final JTextField    chat;
    private final JTextArea     chatArea;
    private final JPanel        optionsPanel;

    private JButton start;

    /**
    * The constructor that will add the items to this panel.
    * @param parent The parent of this panel.
    */
    public StartGamePanel(Canvas parent, FreeColClient freeColClient) {
        this.parent = parent;
        this.freeColClient = freeColClient;

        JButton     cancel = new JButton("Cancel");
        JPanel      chatPanel = new JPanel();
        JLabel      mapSizeLabel = new JLabel("Map Size"),
                    nameLabel = new JLabel("Name"),
                    nationLabel = new JLabel("Nation"),
                    colorLabel = new JLabel("Color");
        JScrollPane chatScroll;

        start = new JButton("Start Game");

        optionsPanel = new JPanel();        
        readyBox = new JCheckBox("I'm Ready");
        mapSize = new JComboBox(mapSizes);
        nation = new JComboBox(nations);
        color = new JComboBox(colors);
        name = new JLabel();

        chat = new JTextField();
        chatArea = new JTextArea();
        chatScroll = new JScrollPane(chatArea);

        mapSize.setSize(65, 20);
        mapSizeLabel.setSize(60, 20);
        optionsPanel.setSize(150, 380);
        start.setSize(110, 20);
        cancel.setSize(80, 20);
        name.setSize(80, 20);
        nation.setSize(80, 20);
        color.setSize(80, 20);
        nameLabel.setSize(80, 20);
        nationLabel.setSize(80, 20);
        colorLabel.setSize(80, 20);
        readyBox.setSize(90, 20);

        chat.setSize(220, 20);
        chatScroll.setSize(220, 75);
        chatPanel.setSize(240, 130);


        mapSize.setLocation(75, 20);
        mapSizeLabel.setLocation(10, 20);
        optionsPanel.setLocation(260, 5);
        start.setLocation(25, 370);
        cancel.setLocation(155, 370);
        name.setLocation(100, 20);
        nation.setLocation(100, 45);
        color.setLocation(100, 70);
        nameLabel.setLocation(10, 20);
        nationLabel.setLocation(10, 45);
        colorLabel.setLocation(10, 70);
        readyBox.setLocation(90, 330);

        chat.setLocation(10, 100);
        chatScroll.setLocation(10, 20);
        chatPanel.setLocation(10, 185);

        setLayout(null);
        optionsPanel.setLayout(null);
        chatPanel.setLayout(null);

        mapSize.setActionCommand(String.valueOf(MAPSIZE));
        start.setActionCommand(String.valueOf(START));
        cancel.setActionCommand(String.valueOf(CANCEL));
        nation.setActionCommand(String.valueOf(NATION));
        color.setActionCommand(String.valueOf(COLOR));
        readyBox.setActionCommand(String.valueOf(READY));
        
        chat.setActionCommand(String.valueOf(CHAT));

        mapSize.addActionListener(this);
        start.addActionListener(this);
        cancel.addActionListener(this);
        nation.addActionListener(this);
        color.addActionListener(this);
        readyBox.addActionListener(this);
        
        chat.addActionListener(this);

        // if I'm not an admin
        // start.setEnabled(false);
        
        chatArea.setEditable(false);

        optionsPanel.add(mapSize);
        optionsPanel.add(mapSizeLabel);
        add(optionsPanel);
        add(start);
        add(cancel);
        add(name);
        add(nation);
        add(color);
        add(nameLabel);
        add(nationLabel);
        add(colorLabel);
        add(readyBox);

        chatPanel.add(chat);
        chatPanel.add(chatScroll);
        add(chatPanel);

        try {
            BevelBorder border1 = new BevelBorder(BevelBorder.RAISED);
            setBorder(border1);
            TitledBorder border2 = new TitledBorder("Options");
            optionsPanel.setBorder(border2);
        } catch(Exception e) {}

        setSize(420, 400);
    }


    
    

    public void initialize(Game game, Player thisPlayer) {
        this.game = game;
        this.thisPlayer = thisPlayer;
        
        clearPlayer();

        name.setText(thisPlayer.getName());
        // TODO: Initialize 'mapSize', 'nation' and 'color' from values in 'game'.
        readyBox.setSelected(thisPlayer.isReady());
        setEnabled(true);
    }


    /**
    * Sets whether or not this component is enabled. It also does this for
    * its children.
    * @param enabled 'true' if this component and its children should be
    * enabled, 'false' otherwise.
    */
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        Component[] components = getComponents();
        for (int i = 0; i < components.length; i++) {
            components[i].setEnabled(enabled);
        }

        optionsPanel.setEnabled(enabled);
        components = optionsPanel.getComponents();
        for (int i = 0; i < components.length; i++) {
            components[i].setEnabled(enabled);
        }
    }


    /**
    * This function analyses an event and calls the right methods to take
    * care of the user's requests.
    * @param event The incoming ActionEvent.
    */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();

        try {
            switch (Integer.valueOf(command).intValue()) {
                case START:
                    freeColClient.getPreGameController().requestLaunch();
                    break;
                case CANCEL:
                    parent.remove(this);
                    parent.showMainPanel();
                    break;
                case MAPSIZE:
                    break;
                case NATION:
                    freeColClient.getPreGameController().setNation(((String)nation.getSelectedItem()).toLowerCase());
                    break;
                case COLOR:
                    freeColClient.getPreGameController().setColor(((String)color.getSelectedItem()).toLowerCase());
                    break;
                case READY:
                    freeColClient.getPreGameController().setReady(readyBox.isSelected());
                    break;
                case CHAT:
                    freeColClient.getPreGameController().chat(chat.getText());
                    displayChat(name.getText(), chat.getText(), false);
                    chat.setText("");
                    break;
                default:
                    logger.warning("Invalid Actioncommand: invalid number.");
            }
        }
        catch (NumberFormatException e) {
            logger.warning("Invalid Actioncommand: not a number.");
        }
    }

    
    /**
    * Displays a chat message to the user.
    *
    * @param senderName The name of the player who sent the chat message to
    *                   the server.
    * @param message The chat message.
    * @param privateChat 'true' if the message is a private one, 'false' otherwise.
    */
    public void displayChat(String senderName, String message, boolean privateChat) {
        if (privateChat) {
            chatArea.append(senderName + " (private): " + message + '\n');
        } else {
            chatArea.append(senderName + ": " + message + '\n');
        }
    }
    

    /**
    * Clears the player from this panel. This is usually done when a new game
    * is about to be set up.
    */
    public void clearPlayer() {
        mapSize.setSelectedIndex(0);
        nation.setSelectedIndex(0);
        color.setSelectedIndex(6);
    }

    
    
    

}
