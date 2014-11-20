/**
 *  Copyright (C) 2002-2014   The FreeCol Team
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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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
import net.sf.freecol.client.ClientOptions;
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
import net.sf.freecol.common.option.Option;
import net.sf.freecol.common.option.OptionGroup;


/**
 * This dialog allows the user to start a single player or multiplayer
 * game, to join a running game, and to fetch a list of games from the
 * meta-server.
 */
public final class NewPanel extends FreeColPanel
    implements ActionListener, ItemListener {

    private static final Logger logger = Logger.getLogger(NewPanel.class.getName());

    /** The actions for this panel. */
    private static enum NewPanelAction {
        OK,
        CANCEL,
        SINGLE,
        JOIN,
        START,
        META_SERVER,
        SHOW_DIFFICULTY
    };

    /**
     * A particular specification to use for the new game.  If not
     * null, the specification box just contains this spec, but if
     * nullthe user chooses from available specs using the
     * specification box.
     */
    private final Specification fixedSpecification;

    /** Field to input the player name. */
    private final JTextField nameBox;

    /** A button group for the main choices. */
    private final ButtonGroup buttonGroup;

    /** The label for the national advantages. */
    private final JLabel advantagesLabel;

    /** A box to choose the national advantages setting. */
    private final JComboBox<Advantages> advantagesBox;

    /** Start server name label. */
    private final JLabel serverPortLabel;

    /** Start server port number label and field to input through. */
    private final JTextField serverPortField;

    /** The label for the rules selection. */
    private final JLabel rulesLabel;

    /** A box to choose the rules from. */
    private final JComboBox<FreeColTcFile> rulesBox;

    /** The check box to select a public server with. */
    private final JCheckBox publicServer;

    /** A label for the difficulty level selection. */
    private final JLabel difficultyLabel;

    /** A box to choose the difficulty from. */
    private final JComboBox<OptionGroup> difficultyBox;

    /** A button to show/edit difficulty level. */
    private final JButton difficultyButton;

    /** Join multiplayer server name label. */
    private final JLabel joinNameLabel;

    /** Join multiplayer server name selection. */
    private final JTextField joinNameField;

    /** Join multiplayer server port label. */
    private final JLabel joinPortLabel;

    /** Join multiplayer server port selection. */
    private final JTextField joinPortField;

    /** Container for the components to enable when Join is selected. */
    private final Component[] joinComponents;

    /** Container for components to enable if server parameters can be set. */
    private final Component[] serverComponents;

    /** Container for components to enable when choosing game parameters. */
    private final Component[] gameComponents;


    /**
     * Creates a new game panel.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public NewPanel(FreeColClient freeColClient) {
        this(freeColClient, null);
    }

    /**
     * Creates a new game panel.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param specification An optional <code>Specification</code> value for
     *     the new game.
     */
    public NewPanel(FreeColClient freeColClient, Specification specification) {
        super(freeColClient, new MigLayout("wrap 6", "[15]", ""));

        this.fixedSpecification = specification;

        // Create the components
        this.buttonGroup = new ButtonGroup();
        JRadioButton
            single = new JRadioButton(Messages.message("singlePlayerGame"),
                                      true),
            join = new JRadioButton(Messages.message("joinMultiPlayerGame"),
                                    false),
            start = new JRadioButton(Messages.message("startMultiplayerGame"),
                                     false),
            meta = new JRadioButton(Messages.message("getServerList")
                + " (" + FreeCol.META_SERVER_ADDRESS + ")", false);
        this.buttonGroup.add(single);
        single.setActionCommand(String.valueOf(NewPanelAction.SINGLE));
        single.addActionListener(this);
        this.buttonGroup.add(join);
        join.setActionCommand(String.valueOf(NewPanelAction.JOIN));
        join.addActionListener(this);
        this.buttonGroup.add(start);
        start.setActionCommand(String.valueOf(NewPanelAction.START));
        start.addActionListener(this);
        this.buttonGroup.add(meta);
        meta.setActionCommand(String.valueOf(NewPanelAction.META_SERVER));
        meta.addActionListener(this);
        single.setSelected(true);

        this.nameBox = new JTextField(getPlayerName(), 20);

        this.advantagesLabel
            = localizedLabel("playerOptions.nationalAdvantages");
        this.advantagesBox = new JComboBox<Advantages>(new Advantages[] {
                Advantages.SELECTABLE,
                Advantages.FIXED,
                Advantages.NONE
            });
        this.advantagesBox
            .setRenderer(new FreeColComboBoxRenderer<Advantages>());

        this.serverPortLabel = localizedLabel("startServerOnPort");
        this.serverPortField
            = new JTextField(Integer.toString(FreeCol.getServerPort()));

        this.rulesLabel = localizedLabel("rules");
        this.rulesBox = new JComboBox<FreeColTcFile>();
        if (this.fixedSpecification == null) { // Allow TC selection
            String selectTC = FreeCol.getTC();
            for (FreeColTcFile tc : Mods.getRuleSets()) {
                this.rulesBox.addItem(tc);
                if (selectTC.equals(tc.getId())) {
                    this.rulesBox.setSelectedItem(tc);
                }
            }
        } else { // Force the use of the TC that contains the given spec
            String selectTC = this.fixedSpecification.getId();
            for (FreeColTcFile tc : Mods.getRuleSets()) {
                if (selectTC.equals(tc.getId())) {
                    this.rulesBox.addItem(tc);
                    this.rulesBox.setSelectedItem(tc);
                }
            }
        }
        this.rulesBox
            .setRenderer(new FreeColComboBoxRenderer<FreeColTcFile>("mod."));
        this.rulesBox.addItemListener(this);

        this.publicServer = new JCheckBox(Messages.message("publicServer"));

        this.difficultyLabel = localizedLabel("difficulty");
        this.difficultyBox = new JComboBox<OptionGroup>();
        this.difficultyBox
            .setRenderer(new FreeColComboBoxRenderer<OptionGroup>());
        this.difficultyBox.addItemListener(this);
        this.difficultyButton = new JButton(Messages.message("showDifficulty"));
        this.difficultyButton
            .setActionCommand(String.valueOf(NewPanelAction.SHOW_DIFFICULTY));
        this.difficultyButton.addActionListener(this);

        this.joinNameLabel = localizedLabel("host");
        this.joinNameField = new JTextField("127.0.0.1");
        this.joinPortLabel = localizedLabel("port");
        this.joinPortField
            = new JTextField(Integer.toString(FreeCol.getServerPort()));

        okButton.setActionCommand(String.valueOf(NewPanelAction.OK));

        JButton cancel = new JButton(Messages.message("cancel"));
        cancel.setActionCommand(String.valueOf(NewPanelAction.CANCEL));
        cancel.addActionListener(this);
        setCancelComponent(cancel);

        // Add all the components
        add(GUI.getDefaultHeader(Messages.message("newGamePanel")),
            "span 6, center");
        add(single, "newline, span 3");
        add(new JSeparator(JSeparator.VERTICAL), "spany 7, grow");
        add(localizedLabel("name"), "span, split 2");
        add(this.nameBox, "growx");
        add(start, "newline, span 3");
        add(this.advantagesLabel);
        add(this.advantagesBox, "growx");
        add(this.serverPortLabel, "newline, skip");
        add(this.serverPortField, "width 60:");
        add(this.rulesLabel);
        add(this.rulesBox, "growx");
        add(this.publicServer, "newline, skip, span 2");
        add(this.difficultyLabel);
        add(this.difficultyBox, "growx");
        add(meta, "newline, span 3");
        add(this.difficultyButton, "skip 2, growx");
        add(join, "newline, span 3");
        add(this.joinNameLabel, "newline, skip, split 2");
        add(this.joinNameField, "width 80:");
        add(this.joinPortLabel, "split 2");
        add(this.joinPortField, "width 60:");
        add(okButton, "newline, span, split 2, tag ok");
        add(cancel, "tag cancel");
        joinComponents = new Component[] {
            this.joinNameLabel, this.joinNameField,
            this.joinPortLabel, this.joinPortField
        };
        serverComponents = new Component[] {
            this.serverPortLabel, this.serverPortField, this.publicServer
        };
        gameComponents = new Component[] {
            this.advantagesLabel, this.advantagesBox,
            this.rulesLabel, this.rulesBox,
            this.difficultyLabel, this.difficultyBox, this.difficultyButton
        };

        updateDifficulty();
        enableComponents();
        setSize(getPreferredSize());
    }


    /**
     * Update the contents of the difficulty level box depending on
     * the specification currently selected.
     */
    private void updateDifficulty() {
        final Specification spec = getSpecification();

        OptionGroup selected = getDifficulty();
        this.difficultyBox.removeAllItems();
        for (OptionGroup og : spec.getDifficultyLevels()) {
            this.difficultyBox.addItem(og);
        }
        if (selected == null) {
            selected = spec.getDifficultyOptionGroup("model.difficulty.medium");
            if (selected == null) {
                int index = this.difficultyBox.getItemCount() / 2;
                selected = this.difficultyBox.getItemAt(index);
            }
        }
        this.difficultyBox.setSelectedItem(selected);
        updateShowButton();
    }

    /**
     * Update the show button.
     */
    private void updateShowButton() {
        OptionGroup selected = getDifficulty();
        if (selected == null) {
            difficultyButton.setEnabled(false);
        } else {
            difficultyButton.setEnabled(true);
            difficultyButton.setText(Messages.message((selected.isEditable())
                    ? "editDifficulty" : "showDifficulty"));
        }
    }

    /**
     * Gets the currently selected Advantages type from the nationalAdvantages
     * box.
     *
     * @return The selected advantages type.
     */
    private Advantages getAdvantages() {
        return (Advantages)this.advantagesBox.getSelectedItem();
    }

    /**
     * Gets the currently selected total-conversion from the rulesBox.
     *
     * @return The selected TC.
     */
    private FreeColTcFile getTC() {
        return (FreeColTcFile)this.rulesBox.getSelectedItem();
    }

    /**
     * Gets the currently selected difficulty from the difficultyBox.
     *
     * @return The difficulty <code>OptionGroup</code>.
     */
    private OptionGroup getDifficulty() {
        return (OptionGroup)this.difficultyBox.getSelectedItem();
    }

    /**
     * Get the preferred player name.
     *
     * This is either the value of the client option
     * "model.option.playerName", or the value of the system property
     * "user.name", or the localized value of "defaultPlayerName".
     *
     * @return A name for the player.
     */
    private String getPlayerName() {
        String name = getClientOptions().getText(ClientOptions.NAME);
        if (name == null || name.isEmpty()) name = FreeCol.getName();
        return name;
    }

    /**
     * Enable components according to the selected button.
     */
    private void enableComponents() {
        NewPanelAction action = Enum.valueOf(NewPanelAction.class,
            this.buttonGroup.getSelection().getActionCommand());
        switch (action) {
        case SINGLE:
            enableComponents(this.joinComponents, false);
            enableComponents(this.serverComponents, false);
            enableComponents(this.gameComponents, true);
            this.rulesBox.setEnabled(true);
            break;
        case JOIN:
            enableComponents(this.joinComponents, true);
            enableComponents(this.serverComponents, false);
            enableComponents(this.gameComponents, false);
            this.rulesBox.setEnabled(false);
            break;
        case START:
            enableComponents(this.joinComponents, false);
            enableComponents(this.serverComponents, true);
            enableComponents(this.gameComponents, true);
            this.rulesBox.setEnabled(true);
            break;
        case META_SERVER:
            enableComponents(this.joinComponents, false);
            enableComponents(this.serverComponents, false);
            enableComponents(this.gameComponents, false);
            this.rulesBox.setEnabled(false);
            break;
        default:
            break;
        }
    }

    /**
     * Dis/Enable a group of components.
     *
     * @param components The <code>Component</code>s to set.
     * @param enable Enable if true.
     */
    private void enableComponents(Component[] components, boolean enable) {
        for (Component c : components) {
            c.setEnabled(enable);
        }
    }


    // Override FreeColPanel

    /**
     * Get the specification.  Either the one set for this panel, or the
     * one implied by the currently selected TC.
     *
     * @return The current <code>Specification</code>.
     */
    @Override
    public Specification getSpecification() {
        if (this.fixedSpecification != null) return this.fixedSpecification;
        FreeColTcFile tcFile = getTC();
        if (tcFile != null) {
            try {
                return tcFile.getSpecification();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Spec read failed in "
                    + tcFile.getId(), e);
            }
        }
        return null;
    }


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent event) {
        final Specification spec = getSpecification();
        final ConnectController cc = getFreeColClient().getConnectController();
        final String command = event.getActionCommand();

        int port;
        switch (Enum.valueOf(NewPanelAction.class, command)) {
        case OK:
            FreeCol.setName(this.nameBox.getText());
            FreeCol.setTC(getTC().getId());
            FreeCol.setAdvantages(getAdvantages());
            if (getAdvantages() == Advantages.NONE) {
                spec.clearEuropeanNationalAdvantages();
            }
            NewPanelAction action = Enum.valueOf(NewPanelAction.class,
                buttonGroup.getSelection().getActionCommand());
            switch (action) {
            case SINGLE:
                spec.applyDifficultyLevel(getDifficulty());
                if (cc.startSinglePlayerGame(spec, false)) return;
                break;
            case JOIN:
                try {
                    port = Integer.parseInt(this.joinPortField.getText());
                } catch (NumberFormatException e) {
                    this.joinPortLabel.setForeground(Color.red);
                    break;
                }
                if (cc.joinMultiplayerGame(this.joinNameField.getText(),
                                           port)) return;
                break;
            case START:
                try {
                    port = Integer.parseInt(this.serverPortField.getText());
                } catch (NumberFormatException e) {
                    this.serverPortLabel.setForeground(Color.red);
                    break;
                }
                spec.applyDifficultyLevel(getDifficulty());
                if (cc.startMultiplayerGame(spec,
                        this.publicServer.isSelected(), port)) return;
                break;
            case META_SERVER:
                List<ServerInfo> servers = cc.getServerList();
                if (servers != null) getGUI().showServerListPanel(servers);
                break;
            default:
                break;
            }
            break;
        case CANCEL:
            getGUI().removeFromCanvas(this);
            getGUI().showMainPanel(null);
            break;
        case SHOW_DIFFICULTY:
            getGUI().showDifficultyDialog(spec, getDifficulty());
            break;
        case SINGLE: case JOIN: case START: case META_SERVER:
            enableComponents();
            break;
        default:
            super.actionPerformed(event);
            break;
        }
    }


    // Interface ItemListener

    /**
     * {@inheritDoc}
     */
    public void itemStateChanged(ItemEvent e) {
        if (e.getSource() == this.rulesBox) {
            updateDifficulty();
        } else if (e.getSource() == this.difficultyBox) {
            updateShowButton();
        }
    }
}
