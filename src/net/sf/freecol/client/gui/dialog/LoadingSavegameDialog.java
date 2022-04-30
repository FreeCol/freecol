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

package net.sf.freecol.client.gui.dialog;

import java.awt.FlowLayout;
import java.net.InetAddress;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.LoadingSavegameInfo;
import net.sf.freecol.client.gui.panel.Utility;
import net.sf.freecol.common.i18n.Messages;


/**
 * Dialog for setting some options when loading a game.
 */
public final class LoadingSavegameDialog extends FreeColConfirmDialog {

    private static final Logger logger = Logger.getLogger(LoadingSavegameDialog.class.getName());

    private final JRadioButton singlePlayer;

    private final JRadioButton privateMultiplayer;

    private final JRadioButton publicMultiplayer;

    private final JTextField serverNameField;
    
    private final JComboBox<InetAddress> serverAddressBox;

    private final JSpinner portField;


    /**
     * Creates a dialog to set the options for loading a saved game.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param frame The owner frame.
     */
    public LoadingSavegameDialog(FreeColClient freeColClient, JFrame frame) {
        super(freeColClient, frame);

        JPanel panel = new JPanel();
        panel.setBorder(Utility.blankBorder(10, 10, 10, 10));
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.setOpaque(false);

        JLabel header = Utility.localizedHeaderLabel("loadingSavegameDialog.name",
                                                     JLabel.CENTER,
                                                     Utility.FONTSPEC_SUBTITLE);
        header.setBorder(Utility.blankBorder(20, 0, 0, 0));

        JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEADING));
        p1.add(Utility.localizedLabel("loadingSavegameDialog.serverName"));

        serverNameField = new JTextField();
        
        serverAddressBox = Utility.createServerInetAddressBox();

        JPanel p2 = new JPanel(new FlowLayout(FlowLayout.LEADING));
        p2.add(Utility.localizedLabel("loadingSavegameDialog.port"));

        portField = new JSpinner(new SpinnerNumberModel(FreeCol.getServerPort(),
                                                        1, 65536, 1));
        portField.setEditor(new JSpinner.NumberEditor(portField, "#"));
        
        ButtonGroup bg = new ButtonGroup();
        String str = Messages.message("loadingSavegameDialog.singlePlayer");
        singlePlayer = new JRadioButton(str);
        singlePlayer.setSelected(true);
        bg.add(singlePlayer);
        str = Messages.message("loadingSavegameDialog.privateMultiplayer");
        privateMultiplayer = new JRadioButton(str);
        bg.add(privateMultiplayer);
        str = Messages.message("loadingSavegameDialog.publicMultiplayer");
        publicMultiplayer = new JRadioButton(str);
        bg.add(publicMultiplayer);
        
        serverAddressBox.addActionListener(event -> {
            final InetAddress address = (InetAddress) serverAddressBox.getSelectedItem();
            publicMultiplayer.setEnabled(!address.isLoopbackAddress());
        });
        final InetAddress address = (InetAddress) serverAddressBox.getSelectedItem();
        publicMultiplayer.setEnabled(!address.isLoopbackAddress());

        panel.add(header);
        panel.add(p1);
        panel.add(serverNameField);
        panel.add(serverAddressBox);
        panel.add(p2);
        panel.add(portField);
        panel.add(singlePlayer);
        panel.add(privateMultiplayer);
        panel.add(publicMultiplayer);
        panel.setSize(panel.getPreferredSize());

        initializeConfirmDialog(frame, true, panel, null, "ok", "cancel");
    }


    /**
     * Is a single player game selected?
     *
     * @return True if single player is selected.
     */
    private boolean isSinglePlayer() {
        return singlePlayer.isSelected();
    }

    /**
     * Is a public server game selected?
     *
     * @return True if public server is selected.
     */
    public boolean isPublic() {
        return publicMultiplayer.isSelected() && !getAddress().isLoopbackAddress();
    }
    
    public InetAddress getAddress() {
        return singlePlayer.isSelected() ? null : (InetAddress) serverAddressBox.getSelectedItem();
    }

    /**
     * Get the selected port number.
     *
     * @return The port number.
     */
    public int getPort() {
        return singlePlayer.isSelected() ? -1 : ((Integer) portField.getValue());
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
     * Get all important information at once.
     * 
     * @return A LoadingSavegameInfo.
     */
    public LoadingSavegameInfo getInfo() {
        return new LoadingSavegameInfo(isSinglePlayer(), getAddress(), getPort(), getServerName(), isPublic());
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
}
