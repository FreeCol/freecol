
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
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.control.ConnectController;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.panel.AdvantageCellRenderer;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.ServerInfo;
import cz.autel.dmi.HIGLayout;

/**
* A panel filled with 'new game' items.
*/
public final class NewPanel extends FreeColPanel implements ActionListener {
    private static final Logger logger = Logger.getLogger(NewPanel.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
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
    private final JLabel        ipLabel,
        port1Label,
        port2Label,
        singlePlayerNoLabel,
        multiPlayerNoLabel;
    private final JCheckBox     publicServer,
        additionalNations,
        selectAdvantages,
        useAdvantages;

    private final JSpinner singlePlayerNo, multiPlayerNo;

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
        singlePlayerNoLabel = new JLabel(Messages.message("singlePlayerNo"));
        multiPlayerNoLabel = new JLabel(Messages.message("multiPlayerNo"));

        publicServer = new JCheckBox( Messages.message("publicServer") );
        additionalNations = new JCheckBox(Messages.message("additionalNations"));
        selectAdvantages = new JCheckBox(Messages.message("selectAdvantages"));
        useAdvantages = new JCheckBox(Messages.message("useAdvantages"));

        singlePlayerNo = new JSpinner(new SpinnerNumberModel(4, 1, 8, 1));
        singlePlayerNo.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    int players = ((Integer) singlePlayerNo.getValue()).intValue();
                    if (players > 4) {
                        additionalNations.setSelected(true);
                    }
                }
            });
        multiPlayerNo = new JSpinner(new SpinnerNumberModel(4, 2, 8, 1));

        name = new JTextField( System.getProperty("user.name", Messages.message("defaultPlayerName")) );
        name.setColumns(20);

        server = new JTextField("127.0.0.1");
        port1 = new JTextField("3541");
        port2 = new JTextField("3541");
        single = new JRadioButton(Messages.message("singlePlayerGame"), true);
        join = new JRadioButton(Messages.message("joinMultiPlayerGame"), false);
        start = new JRadioButton(Messages.message("startMultiplayerGame"), false);
        meta = new JRadioButton( Messages.message("getServerList") + " (" + FreeCol.META_SERVER_ADDRESS + ")", false);

        group.add(single);
        group.add(join);
        group.add(start);
        group.add(meta);

        int numberOfRows = 11;
        int[] widths = {21, 0, margin, 0, margin, 0, margin, 0, 6*margin, 0};
        int[] heights = new int[2 * numberOfRows - 1];
        for (int index = 1; index < heights.length; index += 2) {
            heights[index] = margin;
        }
        heights[heights.length - 2] = 3 * margin;

        setLayout(new HIGLayout(widths, heights));

        int row = 1;
        add(nameLabel, higConst.rc(row, 8, "r"));
        add(name, higConst.rc(row, 10));
        row += 2;
        add(single, higConst.rcwh(row, 1, 8, 1, "l"));
        row += 2;
        add(additionalNations, higConst.rcwh(row, 2, 3, 1, "l"));
        add(selectAdvantages, higConst.rc(row, 10, "l"));
        row += 2;
        add(singlePlayerNoLabel, higConst.rcwh(row, 2, 3, 1, "l"));
        add(singlePlayerNo, higConst.rc(row, 8));
        row += 2;
        add(meta, higConst.rcwh(row, 1, 8, 1, "l"));
        row += 2;
        add(join, higConst.rcwh(row, 1, 8, 1, "l"));
        row += 2;
        add(ipLabel, higConst.rc(row, 2));
        add(server, higConst.rc(row, 4));
        add(port1Label, higConst.rc(row, 6));
        add(port1, higConst.rc(row, 8));
        row += 2;
        add(start, higConst.rcwh(row, 1, 8, 1, "l"));
        row += 2;
        add(port2Label, higConst.rcwh(row, 2, 3, 1, "l"));
        add(port2, higConst.rc(row, 8));
        add(publicServer, higConst.rc(row, 10, "l"));
        row += 2;
        add(multiPlayerNoLabel, higConst.rcwh(row, 2, 3, 1, "l"));
        add(multiPlayerNo, higConst.rc(row, 8));
        add(useAdvantages, higConst.rc(row, 10, "l"));
        row += 2;
        add(ok, higConst.rc(row, 8, "r"));
        add(cancel, higConst.rc(row, 10, "l"));


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

        single.setSelected(true);
        setEnabledComponents();

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
        setEnabledComponents();
    }

    private void setEnabledComponents() {
        if (single.isSelected()) {
            setSinglePlayerOptions(true);
            setJoinGameOptions(false);
            setServerOptions(false);
        } else if (join.isSelected()) {
            setSinglePlayerOptions(false);
            setJoinGameOptions(true);
            setServerOptions(false);
        } else if (start.isSelected()) {
            setSinglePlayerOptions(false);
            setJoinGameOptions(false);
            setServerOptions(true);
        } else if (meta.isSelected()) {
            setSinglePlayerOptions(false);
            setJoinGameOptions(false);
            setServerOptions(false);
        }
    }

    private void setSinglePlayerOptions(boolean enabled) {
        additionalNations.setEnabled(enabled);
        selectAdvantages.setEnabled(enabled);
        singlePlayerNoLabel.setEnabled(enabled);
        singlePlayerNo.setEnabled(enabled);
    }

    private void setJoinGameOptions(boolean enabled) {
        ipLabel.setEnabled(enabled);
        server.setEnabled(enabled);
        port1Label.setEnabled(enabled);
        port1.setEnabled(enabled);
    }

    private void setServerOptions(boolean enabled) {
        port2Label.setEnabled(enabled);
        port2.setEnabled(enabled);
        publicServer.setEnabled(enabled);
        multiPlayerNoLabel.setEnabled(enabled);
        multiPlayerNo.setEnabled(enabled);
        useAdvantages.setEnabled(enabled);
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
                        int players = ((Integer) singlePlayerNo.getValue()).intValue();
                        int advantages = AdvantageCellRenderer.FIXED;
                        if (selectAdvantages.isSelected()) {
                            advantages = AdvantageCellRenderer.SELECTABLE;
                        }
                        connectController.startSingleplayerGame(name.getText(), players, 
                                                                additionalNations.isSelected(),
                                                                advantages);
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
                        int players = ((Integer) singlePlayerNo.getValue()).intValue();
                        int advantages = AdvantageCellRenderer.NONE;
                        if (useAdvantages.isSelected()) {
                            advantages = AdvantageCellRenderer.SELECTABLE;
                        }
                        connectController.startMultiplayerGame(publicServer.isSelected(), name.getText(), port,
                                                               players, advantages);
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
                case JOIN:
                case START:
                case META_SERVER:
                    setEnabledComponents();
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
