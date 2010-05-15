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
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
import java.util.logging.Logger;

import javax.swing.JLabel;

import net.sf.freecol.client.gui.Canvas;

/**
 * This is the panel that pops up when an error needs to be reported.
 */
public final class ErrorPanel extends FreeColDialog<Boolean> implements ActionListener {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(ErrorPanel.class.getName());

    private static final int lineWidth = 320;

    private LinkedList<JLabel> errorLabels; // A LinkedList of JLabel objects.

    /**
     * The constructor that will add the items to this panel.
     * 
     * @param parent The parent of this panel.
     */
    public ErrorPanel(Canvas parent) {
        super(parent);

        setLayout(null);

        errorLabels = null;

       //   add(okButton);
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

        okButton.setLocation(130, 25 + (lines.size() - 1) * 20);
        Rectangle2D rect = getFontMetrics(getFont()).getStringBounds(okButton.getText(), getGraphics());
        okButton.setSize((int)rect.getWidth() + 40, 25);
        
        add(okButton);

        setSize(340, 50 + (lines.size() - 1) * 20);
    }

    /**
     * Release the blocked thread when an action is performed.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        super.actionPerformed(event);
        setResponse(true);
    }
}
