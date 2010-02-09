/**
 *  Copyright (C) 2002-2007  The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

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
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.control.ConnectController;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.ServerInfo;
import net.sf.freecol.common.model.NationOptions;
import net.sf.freecol.common.model.NationOptions.Advantages;

import net.miginfocom.swing.MigLayout;

/**
 * A panel filled with 'new game' items.
 */
public final class NewPanel extends FreeColPanel implements ActionListener {

    private static final Logger logger = Logger.getLogger(NewPanel.class.getName());

    private static enum NewPanelAction {
        OK,
        CANCEL,
        SINGLE,
        JOIN,
        START,
        META_SERVER
    };

    private final JTextField server, port1, port2, name;
    private final JRadioButton single, join, start, meta;
    private final JLabel ipLabel, port1Label, port2Label, advantageLabel;
    private final JCheckBox publicServer;
    private final JCheckBox selectColors;
    private final JComboBox nationalAdvantages;

    private final ConnectController connectController;


    /**
    * The constructor that will add the items to this panel.
    * 
    * @param parent The parent of this panel.
    */
    public NewPanel(Canvas parent) {
        super(parent);
        this.connectController = getClient().getConnectController();

        JButton         cancel = new JButton( Messages.message("cancel") );
        ButtonGroup     group = new ButtonGroup();
        JLabel          nameLabel = new JLabel( Messages.message("name") );

        setCancelComponent(cancel);

        ipLabel = new JLabel( Messages.message("host") );
        port1Label = new JLabel( Messages.message("port") );
        port2Label = new JLabel( Messages.message("startServerOnPort") );
        advantageLabel = new JLabel(Messages.message("playerOptions.nationalAdvantages"));

        publicServer = new JCheckBox( Messages.message("publicServer") );
        name = new JTextField( System.getProperty("user.name", Messages.message("defaultPlayerName")) );
        name.setColumns(20);

        server = new JTextField("127.0.0.1");
        port1 = new JTextField(new Integer(FreeCol.getDefaultPort()).toString());
        port2 = new JTextField(new Integer(FreeCol.getDefaultPort()).toString());
        single = new JRadioButton(Messages.message("singlePlayerGame"), true);
        join = new JRadioButton(Messages.message("joinMultiPlayerGame"), false);
        start = new JRadioButton(Messages.message("startMultiplayerGame"), false);
        meta = new JRadioButton( Messages.message("getServerList") + " (" + FreeCol.META_SERVER_ADDRESS + ")", false);

        // TODO: enable this option
        selectColors = new JCheckBox(Messages.message("playerOptions.selectColors"));
        selectColors.setSelected(true);
        selectColors.setEnabled(false);

        Advantages[] choices = new Advantages[] {
            Advantages.SELECTABLE,
            Advantages.FIXED,
            Advantages.NONE
        };
        nationalAdvantages = new JComboBox(choices);
        nationalAdvantages.setRenderer(new AdvantageRenderer());

        group.add(single);
        group.add(join);
        group.add(start);
        group.add(meta);

        setLayout(new MigLayout("", "[15]", ""));

        add(single, "span 3");
        add(new JSeparator(JSeparator.VERTICAL), "spany 7, grow");
        add(nameLabel, "span, split 2");
        add(name, "growx");

        add(meta, "newline, span 3");
        add(advantageLabel);
        add(nationalAdvantages);

        add(join, "newline, span 3");
        add(selectColors);

        add(ipLabel, "newline, skip, split 2");
        add(server, "width 80:");
        add(port1Label, "split 2");
        add(port1, "width 60:");

        add(start, "newline, span 3");

        add(port2Label, "newline, skip");
        add(port2, "width 60:");

        add(publicServer, "newline, skip");

        add(okButton, "newline, span, split 2, tag ok");
        add(cancel, "tag cancel");

        okButton.setActionCommand(String.valueOf(NewPanelAction.OK));
        cancel.setActionCommand(String.valueOf(NewPanelAction.CANCEL));
        single.setActionCommand(String.valueOf(NewPanelAction.SINGLE));
        join.setActionCommand(String.valueOf(NewPanelAction.JOIN));
        start.setActionCommand(String.valueOf(NewPanelAction.START));
        meta.setActionCommand(String.valueOf(NewPanelAction.META_SERVER));

        cancel.addActionListener(this);
        single.addActionListener(this);
        join.addActionListener(this);
        start.addActionListener(this);
        meta.addActionListener(this);

        single.setSelected(true);
        setEnabledComponents();

        setSize(getPreferredSize());
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
            setJoinGameOptions(false);
            setServerOptions(false);
            setAdvantageOptions(true);
        } else if (join.isSelected()) {
            setJoinGameOptions(true);
            setServerOptions(false);
            setAdvantageOptions(false);
        } else if (start.isSelected()) {
            setJoinGameOptions(false);
            setServerOptions(true);
            setAdvantageOptions(true);
        } else if (meta.isSelected()) {
            setJoinGameOptions(false);
            setServerOptions(false);
            setAdvantageOptions(false);
        }
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
    }

    private void setAdvantageOptions(boolean enabled) {
        advantageLabel.setEnabled(enabled);
        nationalAdvantages.setEnabled(enabled);
        //selectColors.setEnabled(enabled);
    }


    /**
    * This function analyses an event and calls the right methods to take
    * care of the user's requests.
    * @param event The incoming ActionEvent.
    */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        try {
            switch (Enum.valueOf(NewPanelAction.class, command)) {
            case OK:
                if (single.isSelected()) {
                    // TODO: select specification
                    // getCanvas().showFreeColDialog(new SpecificationDialog(getCanvas()));
                    NationOptions nationOptions = NationOptions.getDefaults();
                    nationOptions.setNationalAdvantages((Advantages) nationalAdvantages.getSelectedItem());
                    nationOptions.setSelectColors(selectColors.isSelected());
                    connectController.startSingleplayerGame(name.getText(), nationOptions);
                } else if (join.isSelected()) {
                    try {
                        int port = Integer.valueOf(port1.getText()).intValue();
                        // tell Canvas to launch client
                        connectController.joinMultiplayerGame(name.getText(), server.getText(), port);
                    } catch (NumberFormatException e) {
                        port1Label.setForeground(Color.red);
                    }
                } else if (start.isSelected()) {
                    try {
                        int port = Integer.valueOf(port2.getText()).intValue();
                        NationOptions nationOptions = NationOptions.getDefaults();
                        nationOptions.setNationalAdvantages((Advantages) nationalAdvantages.getSelectedItem());
                        nationOptions.setSelectColors(selectColors.isSelected());
                        connectController.startMultiplayerGame(publicServer.isSelected(), name.getText(),
                                                               port, nationOptions);
                    } catch (NumberFormatException e) {
                        port2Label.setForeground(Color.red);
                    }
                } else if (meta.isSelected()) {
                    ArrayList<ServerInfo> serverList = connectController.getServerList();
                    if (serverList != null) {
                        getCanvas().showServerListPanel(name.getText(), serverList);
                    }
                }
                break;
            case CANCEL:
                getCanvas().remove(this);
                getCanvas().showMainPanel();
                break;
            case SINGLE:
            case JOIN:
            case START:
            case META_SERVER:
                setEnabledComponents();
                break;
            default:
                logger.warning("Invalid Action command: " + command);
            }
        } catch (Exception e) {
            logger.warning(e.toString());
            e.printStackTrace();
        }
    }


    class AdvantageRenderer extends JLabel implements ListCellRenderer {

        public Component getListCellRendererComponent(JList list,
                                                      Object value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            setText(Messages.message("playerOptions." + value.toString()));
            return this;
        }
    }
}
