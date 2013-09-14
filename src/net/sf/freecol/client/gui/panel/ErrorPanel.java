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

import java.awt.event.ActionEvent;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.io.FreeColDirectories;


/**
 * This is the panel that pops up when an error needs to be reported.
 */
public final class ErrorPanel extends FreeColPanel {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(ErrorPanel.class.getName());

    private static final String SHOW = "show";

    private JButton showButton = null;

    private JScrollPane scrollPane = null;


    /**
     * Creates a panel to display the given error message.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param message The error message to display in this error panel.
     */
    public ErrorPanel(FreeColClient freeColClient, String message) {
        super(freeColClient, new MigLayout());

        showButton = new JButton(Messages.message("errorMessage.showLogFile"));
        showButton.setActionCommand(SHOW);
        showButton.addActionListener(this);

        add(getDefaultTextArea(message, 40), "wrap 20");
        add(okButton, "split 2, tag ok");
        add(showButton);
    }

    /**
     * Creates an error panel containing the log file.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public ErrorPanel(FreeColClient freeColClient) {
        super(freeColClient, new MigLayout());

        File logFile = new File(FreeColDirectories.getLogFilePath());
        byte[] buffer = new byte[(int) logFile.length()];
        BufferedInputStream logFileStream = null;
        String message = null;
        try {
            logFileStream = new BufferedInputStream(new FileInputStream(logFile));
            logFileStream.read(buffer);
            message = new String(buffer);
        } catch (Exception e) {
            // ignore
        } finally {
            if (logFileStream != null) {
                try {
                    logFileStream.close();
                } catch (IOException e) {
                    // failed
                }
            }
        }

        JTextArea textArea = getDefaultTextArea(message, 40);
        textArea.setFocusable(true);
        textArea.setEditable(false);
        
        scrollPane = new JScrollPane(textArea,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getViewport().setOpaque(false);

        add(scrollPane, "height 200:200:, wrap 20");
        add(okButton, "tag ok");
    }


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent event) {
        final String command = event.getActionCommand();
        if (SHOW.equals(command)) {
            getGUI().showLogFilePanel();
        } else {
            super.actionPerformed(event);
        }
    }


    // Override Component

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeNotify() {
        super.removeNotify();

        removeAll();
        scrollPane = null;
        showButton = null;
    }
}
