/*
 *  NewPanel.java - The new panel (single player, multiplay, ...).
 *
 *  Copyright (C) 2002  The FreeCol Team
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Library General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package net.sf.freecol.client.gui.panel;

import javax.swing.*;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.*;
import javax.swing.border.BevelBorder;
import java.util.logging.Logger;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.control.ConnectController;


/**
* A panel filled with 'new game' items.
*/
public final class NewPanel extends JPanel implements ActionListener {
    private static final Logger logger = Logger.getLogger(NewPanel.class.getName());
    
    private static final int    OK = 0,
                                CANCEL = 1,
                                SINGLE = 2,
                                JOIN = 3,
                                START = 4;
    
    private final JTextField    server,
                                port1,
                                port2,
                                name;
    private final JRadioButton  single,
                                join,
                                start;
    private final JLabel        ipLabel,
                                port1Label,
                                port2Label;
    
    private final Canvas        parent;
    
    private final ConnectController connectController;
    
    /**
    * The constructor that will add the items to this panel.
    * @param parent The parent of this panel.
    */
    public NewPanel(Canvas parent, ConnectController connectController) {
        this.parent = parent;
        this.connectController = connectController;

        JButton         ok = new JButton("OK"),
                        cancel = new JButton("Cancel");
        ButtonGroup     group = new ButtonGroup();
        JLabel          nameLabel = new JLabel("Name");

        ipLabel = new JLabel("Host");
        port1Label = new JLabel("Port");
        port2Label = new JLabel("Start server on port");
        name = new JTextField(System.getProperty("user.name", "Player Name"));
        server = new JTextField("127.0.0.1");
        port1 = new JTextField("3541");
        port2 = new JTextField("3541");
        single = new JRadioButton("Single player game", true);
        join = new JRadioButton("Join multiplayer game", false);
        start = new JRadioButton("Start multiplayer game", false);
        
        group.add(single);
        group.add(join);
        group.add(start);
        
        name.setSize(175, 20);
        nameLabel.setSize(40, 20);
        ok.setSize(80, 20);
        cancel.setSize(80, 20);
        single.setSize(200, 20);
        join.setSize(200, 20);
        ipLabel.setSize(40, 20);
        server.setSize(80, 20);
        port1Label.setSize(40, 20);
        port1.setSize(40, 20);
        start.setSize(200, 20);
        port2Label.setSize(140, 20);
        port2.setSize(40, 20);

        name.setLocation(60, 10);
        nameLabel.setLocation(10, 10);
        ok.setLocation(30, 195);
        cancel.setLocation(150, 195);
        single.setLocation(10, 45);
        join.setLocation(10, 70);
        ipLabel.setLocation(30, 95);
        server.setLocation(70, 95);
        port1Label.setLocation(155, 95);
        port1.setLocation(195, 95);
        start.setLocation(10, 130);
        port2Label.setLocation(55, 155);
        port2.setLocation(195, 155);
        
        setLayout(null);
        
        ok.setActionCommand(String.valueOf(OK));
        cancel.setActionCommand(String.valueOf(CANCEL));
        single.setActionCommand(String.valueOf(SINGLE));
        join.setActionCommand(String.valueOf(JOIN));
        start.setActionCommand(String.valueOf(START));
        
        ok.addActionListener(this);
        cancel.addActionListener(this);
        single.addActionListener(this);
        join.addActionListener(this);
        start.addActionListener(this);

        ipLabel.setEnabled(false);
        server.setEnabled(false);
        port1Label.setEnabled(false);
        port1.setEnabled(false);
        port2Label.setEnabled(false);
        port2.setEnabled(false);

        add(name);
        add(nameLabel);
        add(ok);
        add(cancel);
        add(single);
        add(join);
        add(ipLabel);
        add(server);
        add(port1Label);
        add(port1);
        add(start);
        add(port2Label);
        add(port2);
        
        try {
            BevelBorder border = new BevelBorder(BevelBorder.RAISED);
            setBorder(border);
        } catch(Exception e) {}

        setSize(260, 235);
    }
    
    /**
    * Sets whether or not this component is enabled. It also does this for
    * its children.
    * @param enabled 'true' if this component and its children should be
    * enabled, 'false' otherwise.
    */
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        Component components[] = getComponents();
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
                case OK:
                    if (single.isSelected()) {
                        connectController.startSingleplayerGame(name.getText());
                    } else if (join.isSelected()) {
                        int port;

                        try {
                            port = Integer.valueOf(port1.getText()).intValue();
                        }catch (NumberFormatException e) {
                            port1Label.setForeground(Color.red);
                            break;
                        }

                        // tell Canvas to launch client
                        connectController.joinMultiplayerGame(name.getText(), server.getText(), port);
                    } else if (start.isSelected()) {
                        int port;

                        try {
                            port = Integer.valueOf(port2.getText()).intValue();
                        } catch (NumberFormatException e) {
                            port2Label.setForeground(Color.red);
                            break;
                        }
                        
                        connectController.startMultiplayerGame(name.getText(), port);
                    }
                    break;
                case CANCEL:
                    parent.remove(this);
                    break;
                case SINGLE:
                    ipLabel.setEnabled(false);
                    server.setEnabled(false);
                    port1Label.setEnabled(false);
                    port1.setEnabled(false);
                    port2Label.setEnabled(false);
                    port2.setEnabled(false);
                    break;
                case JOIN:
                    ipLabel.setEnabled(true);
                    server.setEnabled(true);
                    port1Label.setEnabled(true);
                    port1.setEnabled(true);
                    port2Label.setEnabled(false);
                    port2.setEnabled(false);
                    break;
                case START:
                    ipLabel.setEnabled(false);
                    server.setEnabled(false);
                    port1Label.setEnabled(false);
                    port1.setEnabled(false);
                    port2Label.setEnabled(true);
                    port2.setEnabled(true);
                    break;
                default:
                    logger.warning("Invalid Actioncommand: invalid number.");
            }
        }
        catch (NumberFormatException e) {
            logger.warning("Invalid Actioncommand: not a number.");
        }
    }
}
