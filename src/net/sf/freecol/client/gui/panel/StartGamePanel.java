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
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ScrollPaneConstants;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.PreGameController;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.panel.ColopediaPanel.PanelType;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.Specification;
import net.sf.freecol.common.model.EuropeanNationType;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationOptions;
import net.sf.freecol.common.model.NationOptions.Advantages;
import net.sf.freecol.common.model.NationOptions.NationState;
import net.sf.freecol.common.model.NationType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.generator.MapGeneratorOptions;

import net.miginfocom.swing.MigLayout;

/**
 * The panel where you choose your nation and color and connected players are
 * shown.
 */
public final class StartGamePanel extends FreeColPanel implements ActionListener {

    private static final Logger logger = Logger.getLogger(StartGamePanel.class.getName());

    private static final int START = 0, CANCEL = 1,
        READY = 3, CHAT = 4, GAME_OPTIONS = 5, MAP_GENERATOR_OPTIONS = 6;

    private static final EuropeanNationType[] europeans = 
        FreeCol.getSpecification().getEuropeanNationTypes().toArray(new EuropeanNationType[0]);

    private static final NationState[] allStates = new NationState[] {
        NationState.AVAILABLE,
        NationState.AI_ONLY,
        NationState.NOT_AVAILABLE
    };

    private static final NationState[] aiStates = new NationState[] {
        NationState.AI_ONLY,
        NationState.NOT_AVAILABLE
    };

    private Game game;

    private Player thisPlayer;

    private boolean singlePlayerGame;

    private JCheckBox readyBox;

    private JTextField chat;

    private JTextArea chatArea;

    private JButton start;

    private JButton gameOptions;

    private JButton mapGeneratorOptions;

    private JPanel table = new JPanel();

    private final ListCellRenderer stateBoxRenderer = new NationStateRenderer();

    /**
     * The constructor that will add the items to this panel.
     * 
     * @param parent The parent of this panel.
     */
    public StartGamePanel(final Canvas parent) {
        super(parent);
    }

    /**
     * Describe <code>initialize</code> method here.
     *
     * @param singlePlayerGame <code>true</code> if the user wants to start a
     *            single player game, <code>false</code> otherwise.
     */
    public void initialize(boolean singlePlayerGame) {
        this.singlePlayerGame = singlePlayerGame;
        FreeColClient freeColClient = getClient();
        game = freeColClient.getGame();
        thisPlayer = getMyPlayer();

        JButton cancel = new JButton(Messages.message("cancel"));

        JScrollPane chatScroll, tableScroll;

        setCancelComponent(cancel);

        start = new JButton(Messages.message("startGame"));
        gameOptions = new JButton(Messages.message("gameOptions"));
        mapGeneratorOptions = new JButton(Messages.message("mapGeneratorOptions"));
        readyBox = new JCheckBox(Messages.message("iAmReady"));

        chat = new JTextField();
        chatArea = new JTextArea();
        chatScroll = new JScrollPane(chatArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        table.setLayout(new MigLayout("insets 10 10 10 40", "", ""));
        refreshPlayersTable();
        tableScroll = new JScrollPane(table, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        tableScroll.getViewport().setOpaque(false);

        removeAll();
        setLayout(new MigLayout("wrap 3", "", ""));

        add(tableScroll, "span 2, grow");
        add(chatScroll, "width 250:, grow");
        add(mapGeneratorOptions, "grow");
        add(gameOptions, "grow");
        add(chat, "grow");
        add(readyBox, "span");
        add(start, "span, split 2, tag ok");
        add(cancel, "tag cancel");

        start.setActionCommand(String.valueOf(START));
        cancel.setActionCommand(String.valueOf(CANCEL));
        readyBox.setActionCommand(String.valueOf(READY));
        gameOptions.setActionCommand(String.valueOf(GAME_OPTIONS));
        mapGeneratorOptions.setActionCommand(String.valueOf(MAP_GENERATOR_OPTIONS));
        chat.setActionCommand(String.valueOf(CHAT));

        enterPressesWhenFocused(start);
        enterPressesWhenFocused(cancel);
        
        start.addActionListener(this);
        cancel.addActionListener(this);
        readyBox.addActionListener(this);
        chat.addActionListener(this);
        gameOptions.addActionListener(this);
        mapGeneratorOptions.addActionListener(this);

        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);

        setSize(getPreferredSize());

        if (singlePlayerGame) {
            // If we set the ready flag to false then the player will
            // be able to change the settings as he likes.
            thisPlayer.setReady(false);

            // Pretend as if the player is ready.
            readyBox.setSelected(true);
        } else {
            readyBox.setSelected(thisPlayer.isReady());
        }

        chatArea.setText("");

        setEnabled(true);

    }

    public void requestFocus() {
        start.requestFocus();
    }

    /**
     * Updates the map generator options displayed on this panel.
     */
    public void updateMapGeneratorOptions() {
        getClient().getPreGameController().getMapGeneratorOptions()
            .getObject(MapGeneratorOptions.MAP_SIZE);
    }

    /**
     * Updates the game options displayed on this panel.
     */
    public void updateGameOptions() {
        // Nothing yet.
    }

    /**
     * Sets whether or not this component is enabled. It also does this for its
     * children.
     * 
     * @param enabled 'true' if this component and its children should be
     *            enabled, 'false' otherwise.
     */
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        Component[] components = getComponents();
        for (int i = 0; i < components.length; i++) {
            components[i].setEnabled(enabled);
        }

        if (singlePlayerGame && enabled) {
            readyBox.setEnabled(false);
        }

        if (enabled) {
            start.setEnabled(getClient().isAdmin());
        }

        gameOptions.setEnabled(enabled);
    }

    /**
     * This function analyses an event and calls the right methods to take care
     * of the user's requests.
     * 
     * @param event The incoming ActionEvent.
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();

        try {
            switch (Integer.valueOf(command).intValue()) {
            case START:
                // The ready flag was set to false for single player
                // mode in order to allow the player to change
                // whatever he wants.
                if (singlePlayerGame) {
                    thisPlayer.setReady(true);
                }

                getClient().getPreGameController().requestLaunch();
                break;
            case CANCEL:
                getClient().getConnectController().quitGame(true);
                getCanvas().remove(this);
                getCanvas().showPanel(new NewPanel(getCanvas()));
                break;
            case READY:
                getClient().getPreGameController().setReady(readyBox.isSelected());
                refreshPlayersTable();
                break;
            case CHAT:
                if (chat.getText().trim().length() > 0) {
                    getClient().getPreGameController().chat(chat.getText());
                    displayChat(getMyPlayer().getName(), chat.getText(), false);
                    chat.setText("");
                }
                break;
            case GAME_OPTIONS:
                getCanvas().showFreeColDialog(new GameOptionsDialog(getCanvas(), getClient().isAdmin()));
                break;
            case MAP_GENERATOR_OPTIONS:
                getCanvas().showMapGeneratorOptionsDialog(getClient().isAdmin());
                break;
            default:
                logger.warning("Invalid Actioncommand: invalid number.");
            }
        } catch (NumberFormatException e) {
            logger.warning("Invalid Actioncommand: not a number.");
        }
    }

    /**
     * Displays a chat message to the user.
     * 
     * @param senderName The name of the player who sent the chat message to the
     *            server.
     * @param message The chat message.
     * @param privateChat 'true' if the message is a private one, 'false'
     *            otherwise.
     */
    public void displayChat(String senderName, String message, boolean privateChat) {
        if (privateChat) {
            chatArea.append(senderName + " (private): " + message + '\n');
        } else {
            chatArea.append(senderName + ": " + message + '\n');
        }
    }

    /**
     * Refreshes the table that displays the players and the choices that
     * they've made.
     */
    public void refreshPlayersTable() {
        final PreGameController controller = getClient().getPreGameController();

        table.removeAll();
        JLabel playerLabel = new JLabel(Messages.message("player"));
        JButton nationButton = new JButton(Messages.message("nation"));
        JButton advantageButton = new JButton(Messages.message("advantage"));
        JLabel colorLabel = new JLabel(Messages.message("color"));

        nationButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    getCanvas().showColopediaPanel(PanelType.NATIONS);
                }
            });

        advantageButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    getCanvas().showColopediaPanel(PanelType.NATION_TYPES);
                }
            });
        
        table.add(nationButton);
        table.add(new JLabel(Messages.message("availability")));
        table.add(advantageButton);
        table.add(colorLabel);
        table.add(playerLabel);
        table.add(new JSeparator(JSeparator.HORIZONTAL), "newline, span, growx");

        NationOptions nationOptions = getGame().getNationOptions();
        Nation playerNation = getMyPlayer().getNation();
        for (final Nation nation : Specification.getSpecification().getNations()) {
            NationState state = nationOptions.getNations().get(nation);
            if (state == null) {
                continue;
            }
            table.add(new JLabel(nation.getName()), "newline");
            if (nation != playerNation 
                && (singlePlayerGame || getClient().isAdmin())) {
                final JComboBox stateBox = new JComboBox(nation.isSelectable() ? allStates : aiStates);
                stateBox.setSelectedItem(state);
                stateBox.setRenderer(stateBoxRenderer);
                stateBox.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent event) {
                            controller.setAvailable(nation, (NationState) stateBox.getSelectedItem());
                        }
                    });
                table.add(stateBox);
            } else {
                table.add(new JLabel(state.getName()));
            }
            if (nation == playerNation
                && nationOptions.getNationalAdvantages() == NationOptions.Advantages.SELECTABLE) {
                final JComboBox nationBox = new JComboBox(europeans);
                nationBox.setSelectedItem(getMyPlayer().getNationType());
                nationBox.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent event) {
                            controller.setNationType((NationType) nationBox.getSelectedItem());
                            refreshPlayersTable();
                        }
                    });
                table.add(nationBox);
            } else {
                table.add(new JLabel(nation.getType().getName()));
            }
            ColorButton colorButton;
            if (nation == playerNation) {
                colorButton = new ColorButton(getMyPlayer().getColor());
                colorButton.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent event) {
                            Color newColor = getCanvas().showFreeColDialog(new ColorChooserDialog(getCanvas()));
                            if (newColor != null) {
                                controller.setColor(newColor);
                                refreshPlayersTable();
                            }
                        }
                    });
            } else {
                colorButton = new ColorButton(nation.getColor());
                colorButton.setEnabled(false);
            }
            table.add(colorButton);
            Player player = getGame().getPlayer(nation.getId());
            if (player == null) {
                if (state == NationState.AVAILABLE) {
                    JButton selectButton = new JButton(Messages.message("select"));
                    selectButton.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent event) {
                                controller.setNation(nation);
                                controller.setColor(nation.getColor());
                                refreshPlayersTable();
                            }
                        });
                    table.add(selectButton);
                }
            } else {
                table.add(new JLabel(player.getName()));
            }
        }
        table.revalidate();
        table.repaint();
    }


    public class ColorButton extends JButton {

        public ColorButton(Color color) {
            super(Messages.message("color"));
            setBackground(color);
            setOpaque(true);
            setToolTipText("RGB value: " + color.getRed() + ", " + color.getGreen() + ", "
                           + color.getBlue());
        }

        protected void paintComponent(Graphics g) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    /**
     * This class represents a panel that holds a JColorChooser and OK
     * and cancel buttons.  Once constructed this panel is comparable
     * to the dialog that is returned from
     * JColorChooser::createDialog.
    */
    private class ColorChooserDialog extends FreeColDialog<Color> {

        private final JColorChooser colorChooser = new JColorChooser();

        /**
         * The constructor to use.
         * @param l The ActionListener for the OK and cancel buttons.
         */
        public ColorChooserDialog(Canvas canvas) {
            super(canvas);

            JButton cancelButton = new JButton( Messages.message("cancel") );

            setLayout(new MigLayout("", "", ""));

            add(colorChooser);
            add(okButton, "newline 20, split 2, tag ok");
            add(cancelButton, "tag cancel");

            cancelButton.addActionListener(this);
            setOpaque(true);
            setSize(getPreferredSize());
        }

        public void actionPerformed(ActionEvent event) {
            String command = event.getActionCommand();
            if (OK.equals(command)) {
                setResponse(colorChooser.getColor());
            } else {
                setResponse(null);
            }
        }

    }

    class NationStateRenderer extends JLabel implements ListCellRenderer {
        public Component getListCellRendererComponent(JList list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            setText(((NationState) value).getName());
            return this;
        }
    }


}