/**
 *  Copyright (C) 2002-2021   The FreeCol Team
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
import net.sf.freecol.client.gui.plaf.FreeColComboBoxRenderer;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColTcFile;
import net.sf.freecol.common.metaserver.MetaServerUtils;
import net.sf.freecol.common.metaserver.ServerInfo;
import net.sf.freecol.common.model.NationOptions.Advantages;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.option.OptionGroup;


/**
 * This dialog allows the user to start a single player or multiplayer
 * game, to join a running game, and to fetch a list of games from the
 * meta-server.
 */
public final class NewPanel extends FreeColPanel implements ItemListener {

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
     * null the user chooses from available specs using the
     * specification box.
     */
    private final Specification fixedSpecification;

    /**
     * The current specification, driven by the contents of the TC box.
     */
    private Specification specification = null;

    /**
     * A current difficulty level, driven by the contents of the
     * difficulty box..  Difficulty levels are relative to the rules,
     * so this can be invalidated by a change to the current
     * specification.
     */
    private OptionGroup difficulty = null;

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
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public NewPanel(FreeColClient freeColClient) {
        this(freeColClient, null);
    }

    /**
     * Creates a new game panel.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param specification An optional {@code Specification} value for
     *     the new game.
     */
    public NewPanel(FreeColClient freeColClient, Specification specification) {
        super(freeColClient, null, new MigLayout("wrap 6", "[15]", ""));

        this.fixedSpecification = specification;

        // Create the components
        this.buttonGroup = new ButtonGroup();
        JRadioButton
            single = new JRadioButton(Messages.message("newPanel.singlePlayerGame"),
                                      true),
            join = new JRadioButton(Messages.message("newPanel.joinMultiPlayerGame"),
                                    false),
            start = new JRadioButton(Messages.message("newPanel.startMultiplayerGame"),
                                     false),
            meta = new JRadioButton(Messages.message("newPanel.getServerList")
                + " (" + FreeCol.getMetaServerAddress() + ")", false);
        this.buttonGroup.add(single);
        single.setActionCommand(String.valueOf(NewPanelAction.SINGLE));
        single.addActionListener(ae -> enableComponents());
        this.buttonGroup.add(join);
        join.setActionCommand(String.valueOf(NewPanelAction.JOIN));
        join.addActionListener(ae -> enableComponents());
        this.buttonGroup.add(start);
        start.setActionCommand(String.valueOf(NewPanelAction.START));
        start.addActionListener(ae -> enableComponents());
        this.buttonGroup.add(meta);
        meta.setActionCommand(String.valueOf(NewPanelAction.META_SERVER));
        meta.addActionListener(ae -> enableComponents());
        single.setSelected(true);

        String name = getClientOptions().getText(ClientOptions.NAME);
        if (name == null || name.isEmpty()) name = FreeCol.getName();
        this.nameBox = new JTextField(name, 20);

        this.advantagesLabel
            = Utility.localizedLabel("newPanel.nationalAdvantages");
        this.advantagesBox = new JComboBox<>(new Advantages[] {
                Advantages.SELECTABLE,
                Advantages.FIXED,
                Advantages.NONE
            });
        this.advantagesBox
            .setRenderer(new FreeColComboBoxRenderer<Advantages>());

        this.serverPortLabel = Utility.localizedLabel("newPanel.startServerOnPort");
        this.serverPortField
            = new JTextField(Integer.toString(FreeCol.getServerPort()));
        this.serverPortField.addActionListener((ActionEvent ae) -> {
                getSelectedPort(NewPanel.this.serverPortField);
            });

        this.rulesLabel = Utility.localizedLabel("rules");
        this.rulesBox = new JComboBox<>();
        String selectTC;
        if (this.fixedSpecification == null) { // Allow TC selection
            selectTC = FreeCol.getTC();
            for (FreeColTcFile tc : FreeColTcFile.getRulesList()) {
                this.rulesBox.addItem(tc);
                if (selectTC.equals(tc.getId())) {
                    this.rulesBox.setSelectedItem(tc);
                }
            }
        } else { // Force the use of the TC that contains the given spec
            selectTC = this.fixedSpecification.getId();
            for (FreeColTcFile tc : FreeColTcFile.getRulesList()) {
                if (selectTC.equals(tc.getId())) {
                    this.rulesBox.addItem(tc);
                    this.rulesBox.setSelectedItem(tc);
                }
            }
        }
        if (this.rulesBox.getSelectedItem() == null) {
            this.rulesBox.setSelectedItem(this.rulesBox.getItemCount()-1);
            logger.warning("No TC found for: " + selectTC
                + ", failling back to " + this.rulesBox.getSelectedItem());
        }
        this.rulesBox
            .setRenderer(new FreeColComboBoxRenderer<FreeColTcFile>("mod."));
        this.rulesBox.addItemListener(this);

        this.publicServer
            = new JCheckBox(Messages.message("newPanel.publicServer"));

        this.difficultyLabel = Utility.localizedLabel("difficulty");
        this.difficultyBox = new JComboBox<>();
        this.difficultyBox
            .setRenderer(new FreeColComboBoxRenderer<OptionGroup>());
        this.difficultyBox.addItemListener(this);
        this.difficultyButton = Utility.localizedButton("newPanel.showDifficulty");
        this.difficultyButton.addActionListener(ae -> {
                OptionGroup newDifficulty = getGUI()
                    .showDifficultyDialog(this.specification, this.difficulty,
                                          this.difficulty.isEditable());
                if (newDifficulty != null) {
                    this.difficulty = newDifficulty;
                    update(true); // Brings in new difficulty if edited
                }
            });

        this.joinNameLabel = Utility.localizedLabel("host");
        this.joinNameField = new JTextField(FreeCol.getServerHost());
        this.joinPortLabel = Utility.localizedLabel("port");
        this.joinPortField
            = new JTextField(Integer.toString(FreeCol.getServerPort()));
        this.joinPortField.addActionListener((ActionEvent ae) -> {
                getSelectedPort(NewPanel.this.joinPortField);
            });

        okButton.setActionCommand(String.valueOf(NewPanelAction.OK));

        JButton cancel = Utility.localizedButton("cancel");
        cancel.setActionCommand(String.valueOf(NewPanelAction.CANCEL));
        cancel.addActionListener(ae -> {
                final GUI gui = getGUI();
                gui.removeComponent(this);
                if (getFreeColClient().isMapEditor()) {
                    gui.startMapEditorGUI();
                } else {
                    gui.showMainPanel(null);
                }
            });
        setCancelComponent(cancel);

        // Add all the components
        add(Utility.localizedHeader("newPanel.newGamePanel",
                                    Utility.FONTSPEC_TITLE),
            "span 6, center");
        add(single, "newline, span 3");
        add(new JSeparator(JSeparator.VERTICAL), "spany 7, grow");
        add(Utility.localizedLabel("name"), SPAN_SPLIT_2);
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

        this.specification = getSpecification();
        if (this.specification == null) {
            throw new RuntimeException("No specification found for: " + this);
        }
        this.difficulty = this.specification
            .getDifficultyOptionGroup(FreeCol.getDifficulty());
        updateDifficultyBox();
        if (this.difficulty == null) {
            int index = this.difficultyBox.getItemCount() / 2;
            this.difficultyBox.setSelectedItem(index);
            this.difficulty = getSelectedDifficulty();
        }
        if (this.difficulty == null) {
            throw new RuntimeException("No difficulty found for: " + this);
        }
        logger.info("NewPanel initialized with " + this.specification.getId()
            + "/" + this.difficulty.getId());
        enableComponents();
        setSize(getPreferredSize());
    }

    
    /**
     * Update specification and difficulty as needed.
     *
     * @param changed If true force an update.
     */
    private void update(boolean changed) {
        if (!changed) return;
        // If using a custom difficulty, update it within the
        // spec, otherwise read the difficulty from the new spec.
        if (this.difficulty.isEditable()) {
            this.specification.prepare(null, this.difficulty);
        } else {
            String oldId = this.difficulty.getId();
            this.difficulty = this.specification
                .getDifficultyOptionGroup(oldId);
        }
        updateDifficultyBox();
    }

    /**
     * Check if the TC changed.
     *
     * @return True if the TC changed.
     */
    private boolean checkTC() {
        if (this.specification.getId()
            .equals(getSelectedTC().getId())) return false;
        this.specification = getSpecification();
        return true;
    }
        
    /**
     * Check if the difficulty box changed.
     *
     * @return True if the difficulty changed.
     */
    private boolean checkDifficulty() {
        OptionGroup difficulty = getSelectedDifficulty();
        if (this.difficulty.getId().equals(difficulty.getId())) return false;
        this.difficulty = difficulty;
        return true;
    }
    
    /**
     * Update just the difficulty box state.
     */
    private void updateDifficultyBox() {
        // Update the contents of the difficulty level box depending on
        // the specification currently selected.
        this.difficultyBox.removeItemListener(this);
        this.difficultyBox.removeAllItems();
        for (OptionGroup og : this.specification.getDifficultyLevels()) {
            this.difficultyBox.addItem(og);
        }
        if (this.difficulty != null) {
            this.difficultyBox.setSelectedItem(this.difficulty);
        }
        this.difficultyButton.setEnabled(this.difficulty != null);
        String text = (this.difficulty != null && this.difficulty.isEditable())
            ? "newPanel.editDifficulty"
            : "newPanel.showDifficulty";
        this.difficultyButton.setText(Messages.message(text));
        this.difficultyBox.addItemListener(this);
    }

    /**
     * Get the selected player name from the nameBox.
     *
     * @return The selected player name.
     */
    private String getSelectedName() {
        return this.nameBox.getText();
    }

    /**
     * Gets the currently selected Advantages type from the nationalAdvantages
     * box.
     *
     * @return The selected advantages type.
     */
    private Advantages getSelectedAdvantages() {
        return (Advantages)this.advantagesBox.getSelectedItem();
    }

    /**
     * Gets the currently selected total-conversion from the rulesBox.
     *
     * @return The selected TC.
     */
    private FreeColTcFile getSelectedTC() {
        return (FreeColTcFile)this.rulesBox.getSelectedItem();
    }

    /**
     * Gets the currently selected difficulty from the difficultyBox.
     *
     * @return The difficulty {@code OptionGroup}.
     */
    private OptionGroup getSelectedDifficulty() {
        return (OptionGroup)this.difficultyBox.getSelectedItem();
    }

    /**
     * Get the value of a port field.
     *
     * @param field The field to read.
     * @return The port number in the field, or negative on error.
     */
    private int getSelectedPort(JTextField field) {
        int port;
        try {
            port = Integer.parseInt(field.getText());
        } catch (NumberFormatException e) {
            port = -1;
        }
        if (0 < port && port < 0x10000) return port;
        field.setForeground(Color.red);
        return -1;
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
     * @param components The {@code Component}s to set.
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
     * @return The current {@code Specification}.
     */
    @Override
    public Specification getSpecification() {
        if (this.fixedSpecification != null) return this.fixedSpecification;
        return FreeCol.loadSpecification(getSelectedTC(), null, null);
    }


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        final ConnectController cc = getFreeColClient().getConnectController();
        final GUI gui = getGUI();
        final String command = ae.getActionCommand();

        switch (Enum.valueOf(NewPanelAction.class, command)) {
        case OK:
            FreeCol.setName(getSelectedName());
            FreeCol.setAdvantages(getSelectedAdvantages());
            FreeCol.setTC(getSelectedTC().getId());

            NewPanelAction action = Enum.valueOf(NewPanelAction.class,
                buttonGroup.getSelection().getActionCommand());
            switch (action) {
            case SINGLE:
                this.specification.prepare(getSelectedAdvantages(),
                                           this.difficulty);
                if (cc.startSinglePlayerGame(this.specification)) return;
                break;
            case JOIN:
                int joinPort = getSelectedPort(this.joinPortField);
                if (joinPort < 0) break;
                if (cc.joinMultiplayerGame(this.joinNameField.getText(),
                                           joinPort)) return;
                break;
            case START:
                int serverPort = getSelectedPort(this.serverPortField);
                if (serverPort < 0) break;
                this.specification.prepare(getSelectedAdvantages(),
                                           this.difficulty);
                if (cc.startMultiplayerGame(this.specification,
                        this.publicServer.isSelected(), serverPort)) return;
                break;
            case META_SERVER:
                List<ServerInfo> servers = MetaServerUtils.getServerList();
                if (servers != null) gui.showServerListPanel(servers);
                break;
            default:
                break;
            }
            break;
        default:
            super.actionPerformed(ae);
            break;
        }
    }


    // Interface ItemListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void itemStateChanged(ItemEvent e) {
        update(checkTC() || checkDifficulty());
    }
}
