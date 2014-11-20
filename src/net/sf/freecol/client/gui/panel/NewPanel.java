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

    /** A box to choose a TC from. */
    private final JComboBox<FreeColTcFile> specificationBox
        = new JComboBox<FreeColTcFile>();

    private final JLabel port1Label = localizedLabel("port");
    private final JLabel port2Label = localizedLabel("startServerOnPort");
    private final JLabel ipLabel = localizedLabel("host");
    private final JLabel advantageLabel = localizedLabel("playerOptions.nationalAdvantages");
    private final JLabel rulesLabel = localizedLabel("rules");
    private final JLabel difficultyLabel = localizedLabel("difficulty");

    private final JCheckBox publicServer = new JCheckBox(Messages.message("publicServer"));
    private final JTextField name = new JTextField(getPlayerName(), 20);
    private final JTextField server = new JTextField("127.0.0.1");
    private final JTextField port1 = new JTextField(Integer.toString(FreeCol.getServerPort()));
    private final JTextField port2 = new JTextField(Integer.toString(FreeCol.getServerPort()));
    private final JButton showDifficulty = new JButton(Messages.message("showDifficulty"));
    private final Advantages[] advChoices = new Advantages[] {
        Advantages.SELECTABLE,
        Advantages.FIXED,
        Advantages.NONE
    };

    /** A box to choose the national advantages setting. */
    private final JComboBox<Advantages> nationalAdvantages
        = new JComboBox<Advantages>(advChoices);

    /** A box to choose the difficulty from. */
    private final JComboBox<OptionGroup> difficultyBox
        = new JComboBox<OptionGroup>();

    /** Handy container for the components to enable when Join is selected. */
    private final Component[] joinComponents = new Component[] {
        ipLabel, server, port1Label, port1
    };

    /** Container for components to enable if server parameters can be set. */
    private final Component[] serverComponents = new Component[] {
        publicServer, port2Label, port2
    };

    /** Container for components to enable when choosing game parameters. */
    private final Component[] gameComponents = new Component[] {
        advantageLabel, nationalAdvantages,
        rulesLabel, specificationBox,
        difficultyLabel, difficultyBox, showDifficulty
    };

    /** A button group for the main choices. */
    private final ButtonGroup group;

    /** The difficulty level to use for the new game. */
    private OptionGroup difficulty;


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
    @SuppressWarnings("unchecked") // FIXME in Java7
    public NewPanel(FreeColClient freeColClient, Specification specification) {
        super(freeColClient, new MigLayout("wrap 6", "[15]", ""));

        this.fixedSpecification = specification;
        if (this.fixedSpecification == null) { // Allow TC selection
            String selectTC = FreeCol.getTC();
            for (FreeColTcFile tc : Mods.getRuleSets()) {
                this.specificationBox.addItem(tc);
                if (selectTC.equals(tc.getId())) {
                    this.specificationBox.setSelectedItem(tc);
                }
            }
        } else { // Force the use of the TC that contains the given spec
            String selectTC = this.fixedSpecification.getId();
            for (FreeColTcFile tc : Mods.getRuleSets()) {
                if (selectTC.equals(tc.getId())) {
                    this.specificationBox.addItem(tc);
                    this.specificationBox.setSelectedItem(tc);
                }
            }
        }

        specificationBox.setRenderer(new FreeColComboBoxRenderer<FreeColTcFile>("mod."));
        nationalAdvantages.setRenderer(new FreeColComboBoxRenderer<Advantages>());
        difficultyBox.setRenderer(new FreeColComboBoxRenderer<OptionGroup>());

        JButton cancel = new JButton(Messages.message("cancel"));
        setCancelComponent(cancel);

        this.group = new ButtonGroup();
        JRadioButton
            single = new JRadioButton(Messages.message("singlePlayerGame"),
                                      true),
            join = new JRadioButton(Messages.message("joinMultiPlayerGame"),
                                    false),
            start = new JRadioButton(Messages.message("startMultiplayerGame"),
                                     false),
            meta = new JRadioButton(Messages.message("getServerList")
                + " (" + FreeCol.META_SERVER_ADDRESS + ")", false);
        this.group.add(single);
        this.group.add(join);
        this.group.add(start);
        this.group.add(meta);

        add(GUI.getDefaultHeader(Messages.message("newGamePanel")),
            "span 6, center");
        add(single, "newline, span 3");
        add(new JSeparator(JSeparator.VERTICAL), "spany 7, grow");
        JLabel nameLabel = localizedLabel("name");
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
        specificationBox.addItemListener(this);

        add(publicServer, "newline, skip, span 2");
        add(difficultyLabel);
        add(difficultyBox, "growx");
        difficultyBox.addItemListener(this);
        updateDifficulty();

        add(meta, "newline, span 3");
        add(showDifficulty, "skip 2, growx");

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
        showDifficulty.setActionCommand(String.valueOf(NewPanelAction.SHOW_DIFFICULTY));

        cancel.addActionListener(this);
        single.addActionListener(this);
        join.addActionListener(this);
        start.addActionListener(this);
        meta.addActionListener(this);
        showDifficulty.addActionListener(this);

        single.setSelected(true);
        enableComponents();

        setSize(getPreferredSize());

    }

    /**
     * Update the contents of the difficulty level box depending on
     * the specification currently selected.
     */
    @SuppressWarnings("unchecked") // FIXME in Java7
    private void updateDifficulty() {
        difficultyBox.removeAllItems();

        Specification spec = getSpecification();
        OptionGroup selected = null;
        for (OptionGroup group : spec.getDifficultyLevels()) {
            difficultyBox.addItem(group);
            // Check the equality of the group ids rather than the
            // option groups themselves
            if (difficulty != null
                && group.getId().equals(difficulty.getId())) {
                selected = group;
            }
        }
        if (selected == null) {
            selected = spec.getDifficultyOptionGroup("model.difficulty.medium");
        }
        if (selected == null) {
            int index = difficultyBox.getItemCount() / 2;
            selected = (OptionGroup)difficultyBox.getItemAt(index);
        }
        difficulty = selected;
        difficultyBox.setSelectedItem(selected);
        updateShowButton();
    }

    private void updateShowButton() {
        OptionGroup selected = (OptionGroup)difficultyBox.getSelectedItem();
        if (selected == null) {
            showDifficulty.setEnabled(false);
        } else {
            showDifficulty.setEnabled(true);
            showDifficulty.setText(Messages.message((selected.isEditable())
                    ? "editDifficulty" : "showDifficulty"));
        }
    }

    /**
     * Gets the currently selected total-conversion from the specificationBox.
     *
     * @return The selected TC.
     */
    private FreeColTcFile getTC() {
        return (FreeColTcFile)this.specificationBox.getSelectedItem();
    }

    /**
     * Gets the currently selected difficulty from the difficultyBox.
     *
     * @return The difficulty <code>OptionGroup</code>.
     */
    private OptionGroup getDifficulty() {
        return (OptionGroup)difficultyBox.getSelectedItem();
    }

    /**
     * Gets the currently selected Advantages type from the nationalAdvantages
     * box.
     *
     * @return The selected advantages type.
     */
    private Advantages getAdvantages() {
        return (Advantages)this.nationalAdvantages.getSelectedItem();
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
            this.group.getSelection().getActionCommand());
        switch (action) {
        case SINGLE:
            enableComponents(joinComponents, false);
            enableComponents(serverComponents, false);
            enableComponents(gameComponents, true);
            specificationBox.setEnabled(true);
            break;
        case JOIN:
            enableComponents(joinComponents, true);
            enableComponents(serverComponents, false);
            enableComponents(gameComponents, false);
            specificationBox.setEnabled(false);
            break;
        case START:
            enableComponents(joinComponents, false);
            enableComponents(serverComponents, true);
            enableComponents(gameComponents, true);
            specificationBox.setEnabled(true);
            break;
        case META_SERVER:
            enableComponents(joinComponents, false);
            enableComponents(serverComponents, false);
            enableComponents(gameComponents, false);
            specificationBox.setEnabled(false);
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
        if (fixedSpecification != null) return fixedSpecification;
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
        final ConnectController connectController
            = getFreeColClient().getConnectController();
        String command = event.getActionCommand();
        OptionGroup level = (OptionGroup)difficultyBox.getSelectedItem();
        int port;
        switch (Enum.valueOf(NewPanelAction.class, command)) {
        case OK:
            FreeCol.setName(name.getText());
            FreeCol.setTC(getTC().getId());
            FreeCol.setAdvantages(getAdvantages());
            Specification spec = getSpecification();
            if (getAdvantages() == Advantages.NONE) {
                spec.clearEuropeanNationalAdvantages();
            }
            NewPanelAction action = Enum.valueOf(NewPanelAction.class,
                group.getSelection().getActionCommand());
            switch (action) {
            case SINGLE:
                spec.applyDifficultyLevel(level);
                // Launch!
                if (connectController.startSinglePlayerGame(spec, false))
                    return;
                break;
            case JOIN:
                try {
                    port = Integer.parseInt(port1.getText());
                } catch (NumberFormatException e) {
                    port1Label.setForeground(Color.red);
                    break;
                }
                // Launch!
                if (connectController.joinMultiplayerGame(server.getText(),
                        port)) return;
                break;
            case START:
                try {
                    port = Integer.parseInt(port2.getText());
                } catch (NumberFormatException e) {
                    port2Label.setForeground(Color.red);
                    break;
                }
                spec.applyDifficultyLevel(level);
                // Launch!
                if (connectController.startMultiplayerGame(spec,
                        publicServer.isSelected(), port)) return;
                break;
            case META_SERVER:
                List<ServerInfo> servers = connectController.getServerList();
                if (servers != null) {
                    getGUI().showServerListPanel(servers);
                }
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
            getGUI().showDifficultyDialog(getSpecification(), level);
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
        if (e.getSource() == this.specificationBox) {
            difficulty = getDifficulty();
            updateDifficulty();
        } else if (e.getSource() == this.difficultyBox) {
            updateShowButton();
        }
    }
}
