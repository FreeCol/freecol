/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.FreeCol;
import net.sf.freecol.client.control.ConnectController;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.plaf.FreeColComboBoxRenderer;
import net.sf.freecol.common.ServerInfo;
import net.sf.freecol.common.io.FreeColModFile.ModInfo;
import net.sf.freecol.common.io.FreeColTcFile;
import net.sf.freecol.common.io.Mods;
import net.sf.freecol.common.model.NationOptions.Advantages;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.option.OptionGroup;

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

    private final JLabel ipLabel = localizedLabel("host");
    private final JLabel port1Label = localizedLabel("port");
    private final JLabel port2Label = localizedLabel("startServerOnPort");
    private final JLabel advantageLabel = localizedLabel("playerOptions.nationalAdvantages");
    private final JLabel rulesLabel = localizedLabel("rules");

    private final JCheckBox publicServer = new JCheckBox(Messages.message("publicServer"));
    private final JTextField name = new JTextField(System.getProperty("user.name",
                                                                      Messages.message("defaultPlayerName")), 20);

    private final JTextField server = new JTextField("127.0.0.1");
    private final JTextField port1 = new JTextField(Integer.toString(FreeCol.getDefaultPort()));
    private final JTextField port2 = new JTextField(Integer.toString(FreeCol.getDefaultPort()));
    private final JRadioButton single = new JRadioButton(Messages.message("singlePlayerGame"), true);
    private final JRadioButton join = new JRadioButton(Messages.message("joinMultiPlayerGame"), false);
    private final JRadioButton start = new JRadioButton(Messages.message("startMultiplayerGame"), false);
    private final JRadioButton meta = new JRadioButton( Messages.message("getServerList")
                                                        + " (" + FreeCol.META_SERVER_ADDRESS + ")", false);
    private final Advantages[] choices = new Advantages[] {
        Advantages.SELECTABLE,
        Advantages.FIXED,
        Advantages.NONE
    };
    private final JComboBox nationalAdvantages = new JComboBox(choices);

    private final JComboBox specificationBox = new JComboBox();

    private final Component[] joinComponents = new Component[] {
        ipLabel, server, port1Label, port1
    };

    private final Component[] serverComponents = new Component[] {
        publicServer, port2Label, port2
    };

    private final Component[] gameComponents = new Component[] {
        advantageLabel, nationalAdvantages,
        rulesLabel, specificationBox
    };

    private final ButtonGroup group = new ButtonGroup();

    private final ConnectController connectController;


    /**
    * The constructor that will add the items to this panel.
    * 
    * @param parent The parent of this panel.
    */
    public NewPanel(Canvas parent) {
        super(parent);
        this.connectController = getClient().getConnectController();

        for (FreeColTcFile tc : Mods.getRuleSets()) {
            specificationBox.addItem(tc.getModInfo());
            if (FreeCol.DEFAULT_TC.equals(tc.getId())) {
                specificationBox.setSelectedItem(tc.getModInfo());
            }
        }

        JButton cancel = new JButton( Messages.message("cancel") );
        JLabel nameLabel = localizedLabel("name");

        setCancelComponent(cancel);

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

        add(start, "newline, span 3");
        add(advantageLabel);
        add(nationalAdvantages, "growx");

        add(port2Label, "newline, skip");
        add(port2, "width 60:");
        add(rulesLabel);
        add(specificationBox, "growx");

        add(publicServer, "newline, skip, span 2");

        add(meta, "newline, span 3");

        add(join, "newline, span 3");

        add(ipLabel, "newline, skip, split 2");
        add(server, "width 80:");
        add(port1Label, "split 2");
        add(port1, "width 60:");

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
        enableComponents();

        setSize(getPreferredSize());
    }

    private void enableComponents(Component[] components, boolean enable) {
        for (Component c : components) {
            c.setEnabled(enable);
        }
    }

    private void enableComponents() {
        NewPanelAction action = Enum.valueOf(NewPanelAction.class,
                                             group.getSelection().getActionCommand());
        switch(action) {
        case SINGLE:
            enableComponents(joinComponents, false);
            enableComponents(serverComponents, false);
            enableComponents(gameComponents, true);
            break;
        case JOIN:
            enableComponents(joinComponents, true);
            enableComponents(serverComponents, false);
            enableComponents(gameComponents, false);
            break;
        case START:
            enableComponents(joinComponents, false);
            enableComponents(serverComponents, true);
            enableComponents(gameComponents, true);
            break;
        case META_SERVER:
            enableComponents(joinComponents, false);
            enableComponents(serverComponents, false);
            enableComponents(gameComponents, false);
            break;
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
            switch (Enum.valueOf(NewPanelAction.class, command)) {
            case OK:
                NewPanelAction action = Enum.valueOf(NewPanelAction.class,
                                                     group.getSelection().getActionCommand());
                switch(action) {
                case SINGLE:
                    String tc = ((ModInfo) specificationBox.getSelectedItem()).getId();
                    FreeColTcFile tcData = new FreeColTcFile(tc);
                    Specification specification = tcData.getSpecification();
                    OptionGroup level = getCanvas()
                        .showFreeColDialog(new DifficultyDialog(getCanvas(), specification));
                    Advantages advantages;
                    if (level != null) {
                        specification.applyDifficultyLevel(level);
                        advantages = (Advantages) nationalAdvantages.getSelectedItem();
                        connectController.startSingleplayerGame(specification, name.getText(), advantages);
                    }
                    break;
                case JOIN:
                    try {
                        int port = Integer.valueOf(port1.getText()).intValue();
                        // tell Canvas to launch client
                        connectController.joinMultiplayerGame(name.getText(), server.getText(), port);
                    } catch (NumberFormatException e) {
                        port1Label.setForeground(Color.red);
                    }
                    break;
                case START:
                    try {
                        int port = Integer.valueOf(port2.getText()).intValue();
                        tc = ((ModInfo) specificationBox.getSelectedItem()).getId();
                        tcData = new FreeColTcFile(tc);
                        specification = tcData.getSpecification();
                        level = getCanvas()
                            .showFreeColDialog(new DifficultyDialog(getCanvas(), specification));
                        specification.applyDifficultyLevel(level);
                        advantages = (Advantages) nationalAdvantages.getSelectedItem();
                        connectController.startMultiplayerGame(specification, publicServer.isSelected(), name.getText(),
                                                               port, advantages, level);
                    } catch (NumberFormatException e) {
                        port2Label.setForeground(Color.red);
                    }
                    break;
                case META_SERVER:
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
                enableComponents();
                break;
            default:
                logger.warning("Invalid Action command: " + command);
            }
        } catch (Exception e) {
            logger.warning(e.toString());
            e.printStackTrace();
        }
    }


    private class AdvantageRenderer extends FreeColComboBoxRenderer {
        @Override
        public void setLabelValues(JLabel label, Object value) {
            label.setText(Messages.message("playerOptions." + value.toString()));
        }
    }
}
