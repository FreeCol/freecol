/**
 *  Copyright (C) 2002-2022   The FreeCol Team
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

import static net.sf.freecol.common.util.CollectionUtils.count;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.panel.FreeColButton.ButtonStyle;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Game.LogoutReason;
import net.sf.freecol.common.model.NationOptions;
import net.sf.freecol.common.model.NationOptions.NationState;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.option.GameOptions;
import net.sf.freecol.common.option.MapGeneratorOptions;


/**
 * The panel where you choose your nation and color and connected players are
 * shown.
 */
public final class StartGamePanel extends FreeColPanel {

    private static final Logger logger = Logger.getLogger(StartGamePanel.class.getName());

    private boolean singlePlayerGame;

    private JCheckBox readyBox;

    private JTextField chat;

    private JTextArea chatArea;

    private JButton start, cancel, gameOptions, mapGeneratorOptions;

    private PlayersTable table;

    private final ActionListener startCmd = ae -> {
        int row = table.getSelectedRow();
        int col = table.getSelectedColumn();
        if (row > -1 && col > -1){
            table.getCellEditor(row, col).stopCellEditing();
        }
        if (!checkVictoryConditions()) return;
        // The ready flag was set to false for single player
        // mode in order to allow the player to change
        // whatever he wants.
        if (singlePlayerGame) getMyPlayer().setReady(true);
        getFreeColClient().getPreGameController().requestLaunch();
    };

    private final ActionListener cancelCmd = ae -> {
        final GUI gui = getGUI();
        
        if (getFreeColClient().isLoggedIn()) {
            getFreeColClient().getConnectController().requestLogout(LogoutReason.NEW_GAME);
        }
        getFreeColClient().stopServer();
        gui.removeComponent(this);
        gui.showNewPanel();
    };

    private final ActionListener readyBoxCmd = ae -> {
        getFreeColClient().getPreGameController().setReady(readyBox.isSelected());
        refreshPlayersTable();
    };

    private final ActionListener chatCmd = ae -> {
        if (!chat.getText().isEmpty()) {
            getFreeColClient().getPreGameController().sendChat(chat.getText());
            displayChat(getMyPlayer().getName(), chat.getText(), false);
            chat.setText("");
        }
    };

    private final ActionListener gameOptionsCmd = ae -> {
        final FreeColClient fcc = getFreeColClient();
        getGUI().showGameOptionsDialog(fcc.isAdmin(), (gameOptions) -> {
            if (gameOptions != null) {
                fcc.getGame().setGameOptions(gameOptions);
                fcc.getPreGameController().updateGameOptions();                
            }
        });
    };

    private final ActionListener mapGeneratorOptionsCmd = ae -> {
        final FreeColClient fcc = getFreeColClient();
        getGUI().showMapGeneratorOptionsDialog(fcc.isAdmin(), mgo -> {
            if (mgo != null) {
                fcc.getGame().setMapGeneratorOptions(mgo);
                fcc.getPreGameController().updateMapGeneratorOptions();
            }
        });
    };

    /**
     * Create the panel from which to start a game.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public StartGamePanel(FreeColClient freeColClient) {
        super(freeColClient, null, new MigLayout("fill", "", "[grow][][]"));
    }


    public void initialize(boolean singlePlayer) {
        removeAll();
        this.singlePlayerGame = singlePlayer;

        if (singlePlayer || getMyPlayer().isAdmin()) {
            getSpecification().updateGameAndMapOptions();
        }

        NationOptions nationOptions = getGame().getNationOptions();
        cancel = Utility.localizedButton("cancel");
        JScrollPane chatScroll = null, tableScroll;
        table = new PlayersTable(getFreeColClient(), nationOptions,
                                 getMyPlayer());

        start = new FreeColButton(Messages.message("startGame")).withButtonStyle(ButtonStyle.IMPORTANT);

        gameOptions = Utility.localizedButton(Messages
            .nameKey(GameOptions.TAG));

        mapGeneratorOptions = Utility.localizedButton(Messages
            .nameKey(MapGeneratorOptions.TAG));

        readyBox = new JCheckBox(Messages.message("startGamePanel.iAmReady"));

        if (singlePlayerGame) {
            // If we set the ready flag to false then the player will
            // be able to change the settings as he likes.
            getMyPlayer().setReady(false);
            // Pretend as if the player is ready.
            readyBox.setSelected(true);
        } else {
            readyBox.setSelected(getMyPlayer().isReady());
            chat = new JTextField();
            chatArea = new JTextArea();
            chatScroll = new JScrollPane(chatArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                                         ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        }

        refreshPlayersTable();
        tableScroll = new JScrollPane(table, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER) {
            @Override
            public Dimension getPreferredSize() {
                final int tableWidth = getImageLibrary().scaleInt(600);
                final int tableHeight =  getImageLibrary().scaleInt(300);
                return new Dimension(tableWidth, tableHeight);
            }
        };
        tableScroll.getViewport().setOpaque(false);

        final int tableWidth = getImageLibrary().scaleInt(600);
        final int tableHeight =  getImageLibrary().scaleInt(300);
        add(tableScroll, "grow");
        tableScroll.setSize(getPreferredSize());
        if (!singlePlayerGame) {
            final int chatHeight = (int) (FontLibrary.getFontScaling() * 100);
            add(chatScroll, "newline, height " + chatHeight + "px:,growx");
            add(chat, "newline, growx");
        }
        add(mapGeneratorOptions, "newline, split 2, growx, top, sg");
        add(gameOptions, "growx, top, sg");
        add(readyBox, "newline, span, split 3, tag left, gaptop unrelated");
        add(start, "tag ok");
        add(cancel, "tag cancel");

        if (!singlePlayerGame) {
            chat.addActionListener(chatCmd);
            chatArea.setEditable(false);
            chatArea.setLineWrap(true);
            chatArea.setWrapStyleWord(true);
            chatArea.setText("");
        }

        start.addActionListener(startCmd);
        cancel.addActionListener(cancelCmd);
        readyBox.addActionListener(readyBoxCmd);
        gameOptions.addActionListener(gameOptionsCmd);
        mapGeneratorOptions.addActionListener(mapGeneratorOptionsCmd);

        setEscapeAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                cancel.doClick();
            }
        });
        
        setEnabled(true);
    }

    /**
     * Check that the user has not specified degenerate victory conditions
     * that are automatically true.
     *
     * @return True if the victory conditions are sensible.
     */
    private boolean checkVictoryConditions() {
        Specification spec = getSpecification();
        if (singlePlayerGame
            && spec.getBoolean(GameOptions.VICTORY_DEFEAT_EUROPEANS)
            && !spec.getBoolean(GameOptions.VICTORY_DEFEAT_REF)) {
            int n = count(getGame().getNationOptions().getNations().entrySet(),
                          e -> (e.getKey().getType().isEuropean()
                              && !e.getKey().isUnknownEnemy()
                              && e.getValue() != NationState.NOT_AVAILABLE));
            if (n == 0) {
                getGUI().showInformationPanel("info.noEuropeans");
                return false;
            }
        }
        return true;
    }

    /**
     * Displays a chat message to the user.
     *
     * @param senderName The name of the player who sent the chat
     *     message to the server.
     * @param message The chat message.
     * @param privateChat 'true' if the message is a private one, 'false'
     *     otherwise.
     */
    public void displayChat(String senderName, String message,
                            boolean privateChat) {
        if (privateChat) {
            chatArea.append(senderName + " (" + Messages.message("private")
                + "): " + message + '\n');
        } else {
            chatArea.append(senderName + ": " + message + '\n');
        }
    }

    /**
     * Refreshes the table that displays the players and the choices that
     * they've made.
     */
    public void refreshPlayersTable() {
        if (table != null) table.update();
    }


    // Override Component

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeNotify() {
        // Do not propagate to superclass.  This panel is reused so
        // avoid the destructive cleanups in FreeColPanel.removeNotify.

        start.removeActionListener(startCmd);
        cancel.removeActionListener(cancelCmd);
        readyBox.removeActionListener(readyBoxCmd);
        gameOptions.removeActionListener(gameOptionsCmd);
        mapGeneratorOptions.removeActionListener(mapGeneratorOptionsCmd);
        if (chat != null) chat.removeActionListener(chatCmd);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void requestFocus() {
        start.requestFocus();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        Component[] components = getComponents();
        for (Component component : components) {
            component.setEnabled(enabled);
        }

        if (singlePlayerGame && enabled) {
            readyBox.setEnabled(false);
        }

        if (enabled) {
            start.setEnabled(getFreeColClient().isAdmin());
        }

        gameOptions.setEnabled(enabled);
    }
}
