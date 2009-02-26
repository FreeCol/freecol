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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JLabel;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;

/**
 * This is the panel that pops up when an error needs to be reported.
 */
public final class ErrorPanel extends FreeColDialog<Boolean> implements ActionListener {



    private static final Logger logger = Logger.getLogger(ErrorPanel.class.getName());

    private static final int OK = 0;

    private static final int lineWidth = 320;

    @SuppressWarnings("unused")
    private final Canvas parent;

    private LinkedList<JLabel> errorLabels; // A LinkedList of JLabel objects.

    private JButton errorButton;


    /**
     * The constructor that will add the items to this panel.
     * 
     * @param parent The parent of this panel.
     */
    public ErrorPanel(Canvas parent) {
        this.parent = parent;

        setLayout(null);

        errorButton = new JButton(Messages.message("ok"));
        errorButton.setSize(80, 20);
        errorButton.setActionCommand(String.valueOf(OK));
        errorButton.addActionListener(this);

        errorLabels = null;

        add(errorButton);
    }

    public void requestFocus() {
        errorButton.requestFocus();
    }

    /**
     * Adapts the appearance of this ErrorPanel to the given error message. If
     * the error message is wider than lineWidth then the height of this panel
     * will be adjusted.
     * 
     * @param message The error message to display in this error panel.
     */
    public void initialize(String message) {
        LinkedList<String> lines = new LinkedList<String>();
        while (getFontMetrics(getFont()).getStringBounds(message, getGraphics()).getWidth() + 40 > lineWidth) {
            int spaceIndex = message.indexOf(' ');
            int previousIndex = -1;
            while (getFontMetrics(getFont()).getStringBounds(message.substring(0, spaceIndex), getGraphics())
                    .getWidth() + 40 <= lineWidth) {
                previousIndex = spaceIndex;
                if ((spaceIndex + 1) >= message.length()) {
                    spaceIndex = 0;
                    break;
                }
                spaceIndex = message.indexOf(' ', spaceIndex + 1);
                if (spaceIndex == -1) {
                    spaceIndex = 0;
                    break;
                }
            }

            if ((previousIndex >= 0) && (spaceIndex >= 0)) {
                lines.add(message.substring(0, previousIndex));
                if (previousIndex + 1 < message.length()) {
                    message = message.substring(previousIndex + 1);
                } else {
                    break;
                }
            } else {
                lines.add(message);
                lines.add("Internal error in ErrorPanel");
                break;
            }
        }

        if (message.trim().length() > 0) {
            lines.add(message);
        }

        if (errorLabels != null) {
            for (int i = 0; i < errorLabels.size(); i++) {
                remove(errorLabels.get(i));
            }

            errorLabels.clear();
        } else {
            errorLabels = new LinkedList<JLabel>();
        }

        for (int i = 0; i < lines.size(); i++) {
            JLabel label = new JLabel(lines.get(i));
            label.setSize(lineWidth, 20);
            label.setLocation(10, 2 + i * 20);
            add(label);
            errorLabels.add(label);
        }

        errorButton.setLocation(130, 25 + (lines.size() - 1) * 20);
        add(errorButton);

        setSize(340, 50 + (lines.size() - 1) * 20);
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
            case OK:
                // parent.closeErrorPanel();
                setResponse(Boolean.TRUE);
                break;
            default:
                logger.warning("Invalid Actioncommand: invalid number.");
            }
        } catch (NumberFormatException e) {
            logger.warning("Invalid Actioncommand: not a number.");
        }
    }
}
