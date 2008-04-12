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

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovement;


/**
 * A panel for adjusting the river style.
 *
 * <br><br>
 * 
 * This panel is only used when running in
 * {@link FreeColClient#isMapEditor() map editor mode}.
 * 
 */
public final class RiverStylePanel extends FreeColDialog implements ActionListener {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(RiverStylePanel.class.getName());
    
    /**
     * The constructor that will add the items to this panel. 
     * @param parent The parent of this panel.
     */
    public RiverStylePanel(Canvas parent) {
        super(parent);
	setLayout(new GridLayout(9, 9));
        
        ImageLibrary library = parent.getGUI().getImageLibrary();

	JButton deleteButton = new JButton(library.getScaledImageIcon(library.getMiscImageIcon(1), 0.5f));
	deleteButton.setActionCommand("0");
	deleteButton.addActionListener(this);
	add(deleteButton);
	for (int index = 1; index < ImageLibrary.RIVER_STYLES; index++) {
	    JButton riverButton = new JButton(library.getScaledImageIcon(library.getRiverImage(index), 0.5f));
	    riverButton.setActionCommand(String.valueOf(index));
	    riverButton.addActionListener(this);
	    add(riverButton);
	}
        setSize(getPreferredSize());
    }


    /**
     * This function analyses an event and calls the right methods to take care
     * of the user's requests.
     * 
     * @param event The incoming ActionEvent.
     */
    public void actionPerformed(ActionEvent event) {
	int style = Integer.parseInt(event.getActionCommand());
	setResponse(new Integer(style));
    }

}
