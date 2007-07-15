
package net.sf.freecol.client.gui.panel;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.control.ConnectController;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.ServerInfo;
import cz.autel.dmi.HIGLayout;

/**
* A panel filled with 'new game' items.
*/
public final class NewPanel extends FreeColPanel implements ActionListener {
    private static final Logger logger = Logger.getLogger(NewPanel.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static final int    OK = 0,
                                CANCEL = 1,
                                SINGLE = 2,
                                JOIN = 3,
                                START = 4,
                                META_SERVER = 5;

    private final JTextField    server,
                                port1,
                                port2,
                                name;
    private final JRadioButton  single,
                                join,
                                start,
                                meta;
    private final JPanel        namePanel,
                                joinServerPanel,
                                startServerPanel,
                                buttonPanel;
    private final JLabel        ipLabel,
                                port1Label,
                                port2Label;
    private final JCheckBox     publicServer;

    private final Canvas        parent;

    private final ConnectController connectController;
    private JButton ok = new JButton( Messages.message("ok") );


    /**
    * The constructor that will add the items to this panel.
    * 
    * @param parent The parent of this panel.
    * @param connectController The controller responsible for
    *       creating new connections.
    */
    public NewPanel(Canvas parent, ConnectController connectController) {
        this.parent = parent;
        this.connectController = connectController;

        JButton         cancel = new JButton( Messages.message("cancel") );
        ButtonGroup     group = new ButtonGroup();
        JLabel          nameLabel = new JLabel( Messages.message("name") );

        setCancelComponent(cancel);

        ipLabel = new JLabel( Messages.message("host") );
        port1Label = new JLabel( Messages.message("port") );
        port2Label = new JLabel( Messages.message("startServerOnPort") );
        publicServer = new JCheckBox( Messages.message("publicServer") );
        name = new JTextField( System.getProperty("user.name", Messages.message("defaultPlayerName")) );
        name.setColumns(30);
        server = new JTextField("127.0.0.1");
        port1 = new JTextField("3541");
        port2 = new JTextField("3541");
        single = new JRadioButton(Messages.message("singlePlayerGame"), true);
        join = new JRadioButton(Messages.message("joinMultiPlayerGame"), false);
        start = new JRadioButton(Messages.message("startMultiplayerGame"), false);
        meta = new JRadioButton( Messages.message("getServerList") + " (" + FreeCol.META_SERVER_ADDRESS + ")", false);

        namePanel = new JPanel();
        namePanel.setOpaque(false);
        namePanel.add(nameLabel);
        namePanel.add(name);

        joinServerPanel = new JPanel();
        joinServerPanel.setOpaque(false);
        joinServerPanel.add(ipLabel);
        joinServerPanel.add(server);
        joinServerPanel.add(port1Label);
        joinServerPanel.add(port1);

        startServerPanel = new JPanel();
        startServerPanel.setOpaque(false);
        startServerPanel.add(port2Label);
        startServerPanel.add(port2);

        buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.add(ok);
        buttonPanel.add(cancel);
        enterPressesWhenFocused(ok);
        enterPressesWhenFocused(cancel);

        group.add(single);
        group.add(join);
        group.add(start);
        group.add(meta);

        int numberOfRows = 9;
        int[] widths = {0};
        int[] heights = new int[2 * numberOfRows - 1];
        for (int index = 1; index < heights.length; index += 2) {
            heights[index] = margin;
        }
        int column = 1;
        setLayout(new HIGLayout(widths, heights));

        int row = 1;
        add(namePanel, higConst.rc(row, column));
        row += 2;
        add(single, higConst.rc(row, column));
        row += 2;
        add(meta, higConst.rc(row, column));
        row += 2;
        add(join, higConst.rc(row, column));
        row += 2;
        add(joinServerPanel, higConst.rc(row, column, "l"));
        row += 2;
        add(start, higConst.rc(row, column));
        row += 2;
        add(startServerPanel, higConst.rc(row, column, "l"));
        row += 2;
        add(publicServer, higConst.rc(row, column));
        row += 2;
        add(buttonPanel, higConst.rc(row, column));


        ok.setActionCommand(String.valueOf(OK));
        cancel.setActionCommand(String.valueOf(CANCEL));
        single.setActionCommand(String.valueOf(SINGLE));
        join.setActionCommand(String.valueOf(JOIN));
        start.setActionCommand(String.valueOf(START));
        meta.setActionCommand(String.valueOf(META_SERVER));

        ok.addActionListener(this);
        cancel.addActionListener(this);
        single.addActionListener(this);
        join.addActionListener(this);
        start.addActionListener(this);
        meta.addActionListener(this);

        ipLabel.setEnabled(false);
        server.setEnabled(false);
        port1Label.setEnabled(false);
        port1.setEnabled(false);
        port2Label.setEnabled(false);
        publicServer.setEnabled(false);
        port2.setEnabled(false);

        setSize(getPreferredSize());
    }


    public void requestFocus() {
        ok.requestFocus();
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

        if (single.isSelected() || meta.isSelected()) {
            ipLabel.setEnabled(false);
            server.setEnabled(false);
            port1Label.setEnabled(false);
            port1.setEnabled(false);
            port2Label.setEnabled(false);
            port2.setEnabled(false);
            publicServer.setEnabled(false);
        } else if (join.isSelected()) {
            ipLabel.setEnabled(true);
            server.setEnabled(true);
            port1Label.setEnabled(true);
            port1.setEnabled(true);
            port2Label.setEnabled(false);
            port2.setEnabled(false);
            publicServer.setEnabled(false);
        } else if (start.isSelected()) {
            ipLabel.setEnabled(false);
            server.setEnabled(false);
            port1Label.setEnabled(false);
            port1.setEnabled(false);
            port2Label.setEnabled(true);
            port2.setEnabled(true);
            publicServer.setEnabled(true);
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
                        } catch (NumberFormatException e) {
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

                        connectController.startMultiplayerGame(publicServer.isSelected(), name.getText(), port);
                    } else if (meta.isSelected()) {
                        ArrayList<ServerInfo> serverList = connectController.getServerList();
                        if (serverList != null) {
                            parent.showServerListPanel(name.getText(), serverList);
                        }
                    }
                    break;
                case CANCEL:
                    parent.remove(this);
                    parent.showMainPanel();
                    break;
                case SINGLE:
                    ipLabel.setEnabled(false);
                    server.setEnabled(false);
                    port1Label.setEnabled(false);
                    port1.setEnabled(false);
                    port2Label.setEnabled(false);
                    port2.setEnabled(false);
                    publicServer.setEnabled(false);
                    break;
                case JOIN:
                    ipLabel.setEnabled(true);
                    server.setEnabled(true);
                    port1Label.setEnabled(true);
                    port1.setEnabled(true);
                    port2Label.setEnabled(false);
                    port2.setEnabled(false);
                    publicServer.setEnabled(false);
                    break;
                case START:
                    ipLabel.setEnabled(false);
                    server.setEnabled(false);
                    port1Label.setEnabled(false);
                    port1.setEnabled(false);
                    port2Label.setEnabled(true);
                    port2.setEnabled(true);
                    publicServer.setEnabled(true);
                    break;
                case META_SERVER:
                    ipLabel.setEnabled(false);
                    server.setEnabled(false);
                    port1Label.setEnabled(false);
                    port1.setEnabled(false);
                    port2Label.setEnabled(false);
                    port2.setEnabled(false);
                    publicServer.setEnabled(false);
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
