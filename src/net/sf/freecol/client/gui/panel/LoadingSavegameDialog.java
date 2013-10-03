/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;


/**
 * Dialog for setting some options when loading a game.
 */
public final class LoadingSavegameDialog extends FreeColDialog<Boolean> {

    private static final Logger logger = Logger.getLogger(LoadingSavegameDialog.class.getName());


    private JLabel header;

    private JRadioButton singlePlayer;

    private JRadioButton privateMultiplayer;

    private JRadioButton publicMultiplayer;

    private JTextField serverNameField;

    private JSpinner portField;


    /**
     * Creates a dialog to set the options for loading a saved game.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public LoadingSavegameDialog(FreeColClient freeColClient) {
        super(freeColClient);

        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        header = new JLabel(Messages.message("LoadingSavegame.title"),
                            JLabel.CENTER);
        header.setFont(GUI.MEDIUM_HEADER_FONT);
        header.setBorder(new EmptyBorder(20, 0, 0, 0));

        JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p1.add(new JLabel(Messages.message("LoadingSavegame.serverName"),
                          JLabel.LEFT));

        serverNameField = new JTextField();

        JPanel p2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p2.add(new JLabel(Messages.message("LoadingSavegame.port"),
                          JLabel.LEFT));

        portField = new JSpinner(new SpinnerNumberModel(FreeCol.getServerPort(),
                                                        1, 65536, 1));
        ButtonGroup bg = new ButtonGroup();
        String str = Messages.message("LoadingSavegame.singlePlayer");
        singlePlayer = new JRadioButton(str);
        bg.add(singlePlayer);
        str = Messages.message("LoadingSavegame.privateMultiplayer");
        privateMultiplayer = new JRadioButton(str);
        bg.add(privateMultiplayer);
        str = Messages.message("LoadingSavegame.publicMultiplayer");
        publicMultiplayer = new JRadioButton(str);
        bg.add(publicMultiplayer);

        panel.add(header);
        panel.add(p1);
        panel.add(serverNameField);
        panel.add(p2);
        panel.add(portField);
        panel.add(singlePlayer);
        panel.add(privateMultiplayer);
        panel.add(publicMultiplayer);
        panel.setSize(panel.getPreferredSize());

        initialize(DialogType.PLAIN, true, panel, null, new String[] {
                Messages.message("ok"),
                Messages.message("cancel")
            });
    }


    /**
     * Is a single player game selected?
     *
     * @return True if single player is selected.
     */
    public boolean isSinglePlayer() {
        return singlePlayer.isSelected();
    }

    /**
     * Is a public server game selected?
     *
     * @return True if public server is selected.
     */
    public boolean isPublic() {
        return publicMultiplayer.isSelected();
    }

    /**
     * Get the selected port number.
     *
     * @return The port number.
     */
    public int getPort() {
        return ((Integer) portField.getValue()).intValue();
    }

    /**
     * Get the specified server name.
     *
     * @return The server name.
     */
    public String getServerName() {
        return serverNameField.getName();
    }

    /**
     * Reset the dialog to a given state.
     *
     * @param publicServer The public server state.
     * @param singlePlayer The single player state.
     */
    public void reset(boolean publicServer, boolean singlePlayer) {
        this.singlePlayer.setSelected(false);
        this.privateMultiplayer.setSelected(false);
        this.publicMultiplayer.setSelected(false);

        if (singlePlayer) {
            this.singlePlayer.setSelected(true);
        } else if (publicServer) {
            this.publicMultiplayer.setSelected(true);
        } else {
            this.privateMultiplayer.setSelected(true);
        }
        this.serverNameField.setText("");
    }


    /**
     * {@inheritDoc}
     */
    public Boolean getResponse() {
        Object value = getValue();
        return (options[0].equals(value)) ? Boolean.TRUE : Boolean.FALSE;
    }
}
