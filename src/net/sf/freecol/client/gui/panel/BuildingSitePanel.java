/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolTip;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.resources.ResourceManager;

import net.miginfocom.swing.MigLayout;

/**
 * This panel represents a single building in a Colony.
 */
public class BuildingSitePanel extends JPanel implements PropertyChangeListener {

    private final Canvas parent;

    private final Colony colony;

    private BuildableType buildable;

    /**
     * Creates this ColonyPanel.
     * 
     * @param colony The colony to display information from.
     * @param parent a <code>Canvas</code> value
     */
    public BuildingSitePanel(final Colony colony, final Canvas parent) {

        this.colony = colony;
        this.parent = parent;

        // we are interested in changes to the build queue, as well as
        // changes to the warehouse and the colony's production bonus
        colony.addPropertyChangeListener(this);
        addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    BuildQueuePanel queuePanel = new BuildQueuePanel(colony, parent);
                    parent.showSubPanel(queuePanel);
                }
            });

        setToolTipText(" ");
        setLayout(new MigLayout("fill", "", ""));

        initialize();
    }

    public void initialize() {
   
        removeAll();

        buildable = colony.getCurrentlyBuilding();

        if (buildable != null) {
            JLabel turnsLabel = new JLabel();
            turnsLabel.setBackground(Color.WHITE);
            turnsLabel.setOpaque(true);
            String turnsStr = GUI.getTurnsText(colony, buildable);
            turnsLabel.setText(turnsStr);
          
            add(turnsLabel, "align center, wrap");
        }

        revalidate();
        repaint();
    }

    /**
     * Paints this component.
     * 
     * @param g The graphics context in which to paint.
     */
    public void paintComponent(Graphics g) {
        Image bgImage = (buildable != null)
            ? ResourceManager.getImage(buildable.getId() + ".image")
            : ResourceManager.getImage("model.building.BuildingSite.image");
        g.drawImage(bgImage, 0, 0, this);
    }

    public JToolTip createToolTip() {
        return new BuildingSiteToolTip(colony, parent);
    }

    public void propertyChange(PropertyChangeEvent event) {
        initialize();
    }

    public void removePropertyChangeListeners() {
        colony.removePropertyChangeListener(this);
    }

}
