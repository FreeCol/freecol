/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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
import java.util.List;
import java.util.logging.Level;
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
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.ConnectController;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.plaf.FreeColComboBoxRenderer;
import net.sf.freecol.common.ServerInfo;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.io.FreeColModFile;
import net.sf.freecol.common.io.FreeColTcFile;
import net.sf.freecol.common.io.Mods;
import net.sf.freecol.common.model.NationOptions.Advantages;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.option.OptionGroup;


/**
 * This dialog allows the user to start a single player or multiplayer
 * game, to join a running game, and to fetch a list of games from the
 * meta-server.
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
    private final JTextField name = new JTextField(FreeCol.getName(), 20);
    private final JTextField server = new JTextField("127.0.0.1");
    private final JTextField port1 = new JTextField(Integer.toString(FreeCol.getServerPort()));
    private final JTextField port2 = new JTextField(Integer.toString(FreeCol.getServerPort()));
    private final JRadioButton single = new JRadioButton(Messages.message("singlePlayerGame"), true);
    private final JRadioButton join = new JRadioButton(Messages.message("joinMultiPlayerGame"), false);
    private final JRadioButton start = new JRadioButton(Messages.message("startMultiplayerGame"), false);
    private final JRadioButton meta = new JRadioButton( Messages.message("getServerList")
                                                        + " (" + FreeCol.META_SERVER_ADDRESS + ")", false);
    private final Advantages[] advChoices = new Advantages[] {
        Advantages.SELECTABLE,
        Advantages.FIXED,
        Advantages.NONE
    };

    @SuppressWarnings("unchecked") // FIXME in Java7
    private final JComboBox nationalAdvantages = new JComboBox(advChoices);

    @SuppressWarnings("unchecked") // FIXME in Java7
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

    /**
     * The specification to use for the new game.
     */
    private Specification specification;


    /**
     * Creates a new game panel.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param gui The <code>GUI</code> to display on.
     */
    public NewPanel(FreeColClient freeColClient, GUI gui) {
        this(freeColClient, gui, null);
    }

    /**
     * Creates a new game panel.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param gui The <code>GUI</code> to display on.
     * @param specification An optional <code>Specification</code> value for
     *     the new game.
     */
    @SuppressWarnings("unchecked") // FIXME in Java7
    public NewPanel(FreeColClient freeColClient, GUI gui,
                    Specification specification) {
        super(freeColClient, gui);
        this.specification = specification;

        String selectTC = (specification != null) ? specification.getId()
            : FreeCol.getTC();
        for (FreeColTcFile tc : Mods.getRuleSets()) {
            specificationBox.addItem(tc);
            if (selectTC.equals(tc.getId())) {
                specificationBox.setSelectedItem(tc);
            }
        }
        setRenderers();

        JButton cancel = new JButton(Messages.message("cancel"));
        JLabel nameLabel = localizedLabel("name");

        setCancelComponent(cancel);

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
        Advantages selectAdvantage = FreeCol.getAdvantages();
        for (Advantages a : advChoices) {
            if (selectAdvantage == a) {
                nationalAdvantages.setSelectedItem(a);
            }
        }

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


    /**
     * Gets the currently selected TC from the specificationBox.
     *
     * @return The TC.
     */
    private FreeColTcFile getTC() {
        Object o = specificationBox.getSelectedItem();
        return (o == null) ? null : (FreeColTcFile)o;
    }     

    /**
     * Gets the currently selected Advantages type from the nationalAdvantages
     * box.
     *
     * @return The selected advantages type.
     */
    private Advantages getAdvantages() {
        return (Advantages)nationalAdvantages.getSelectedItem();
    }

    /**
     * Get the <code>Specification</code> value.
     *
     * @return a <code>Specification</code> value
     */
    @Override
    public Specification getSpecification() {
        if (specification == null) {
            FreeColTcFile tcFile = getTC();
            if (tcFile != null) {
                try {
                    specification = tcFile.getSpecification();
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Spec read failed in "
                        + tcFile.getId(), e);
                }
            }
        }
        return specification;
    }

    /**
     * Set the <code>Specification</code> value.
     *
     * @param newSpecification The new Specification value.
     */
    public void setSpecification(final Specification newSpecification) {
        this.specification = newSpecification;
    }

    private void enableComponents(Component[] components, boolean enable) {
        for (Component c : components) {
            c.setEnabled(enable);
        }
    }

    private void enableComponents() {
        NewPanelAction action = Enum.valueOf(NewPanelAction.class,
                                             group.getSelection().getActionCommand());
        switch (action) {
        case SINGLE:
            enableComponents(joinComponents, false);
            enableComponents(serverComponents, false);
            enableComponents(gameComponents, true);
            specificationBox.setEnabled(specification == null);
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
            specificationBox.setEnabled(specification == null);
            break;
        case META_SERVER:
            enableComponents(joinComponents, false);
            enableComponents(serverComponents, false);
            enableComponents(gameComponents, false);
            break;
        }
    }


    /**
     * Moved these here out of the constructor to allow
     * warning suppression to work.
     */
    @SuppressWarnings("unchecked") // FIXME in Java7
    private void setRenderers() {
        specificationBox.setRenderer(new FreeColModFileRenderer());
        nationalAdvantages.setRenderer(new AdvantageRenderer());
    }

    private class AdvantageRenderer extends FreeColComboBoxRenderer {
        @Override
        public void setLabelValues(JLabel label, Object value) {
            label.setText(Messages.message(((Advantages)value).getKey()));
        }
    }

    private class FreeColModFileRenderer extends FreeColComboBoxRenderer {
        @Override
        public void setLabelValues(JLabel label, Object value) {
            FreeColModFile mod = (FreeColModFile) value;
            label.setText(Messages.message("mod." + mod.getId() + ".name"));
        }
    }


    /**
     * This function analyses an event and calls the right methods to take
     * care of the user's requests.
     *
     * @param event The incoming <code>ActionEvent</code>.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        final Specification spec = getSpecification();
        final ConnectController connectController
            = getFreeColClient().getConnectController();
        String command = event.getActionCommand();
        OptionGroup level;
        try {
            switch (Enum.valueOf(NewPanelAction.class, command)) {
            case OK:
                FreeCol.setName(name.getText());
                FreeCol.setTC(getTC().getId());
                FreeCol.setAdvantages(getAdvantages());
                NewPanelAction action = Enum.valueOf(NewPanelAction.class,
                    group.getSelection().getActionCommand());
                switch (action) {
                case SINGLE:
                    level = getGUI().showDifficultyDialog(spec);
                    if (level != null) {
                        spec.applyDifficultyLevel(level);
                        connectController.startSinglePlayerGame(spec);
                    }
                    break;
                case JOIN:
                    try {
                        int port = Integer.valueOf(port1.getText()).intValue();
                        // tell Canvas to launch client
                        connectController.joinMultiplayerGame(server.getText(), port);
                    } catch (NumberFormatException e) {
                        port1Label.setForeground(Color.red);
                    }
                    break;
                case START:
                    try {
                        int port = Integer.valueOf(port2.getText()).intValue();
                        level = getGUI().showDifficultyDialog(spec);
                        if (level != null) {
                            spec.applyDifficultyLevel(level);
                            connectController.startMultiplayerGame(spec,
                                publicServer.isSelected(), port, level);
                        }
                    } catch (NumberFormatException e) {
                        port2Label.setForeground(Color.red);
                    }
                    break;
                case META_SERVER:
                    List<ServerInfo> servers = connectController.getServerList();
                    if (servers != null) {
                        getGUI().showServerListPanel(servers);
                    }
                }
                break;
            case CANCEL:
                getGUI().removeFromCanvas(this);
                getGUI().showMainPanel();
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
            logger.log(Level.WARNING, "Unexpected NewPanel fail", e);
        }
    }
}
