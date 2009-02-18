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
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolTip;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Colony.ColonyChangeEvent;
import net.sf.freecol.common.model.Unit;
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

        colony.addPropertyChangeListener(ColonyChangeEvent.BUILD_QUEUE_CHANGE.toString(),
                                         this);
        addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    BuildQueuePanel queuePanel = new BuildQueuePanel(parent);
                    queuePanel.initialize(colony);
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

        if (buildable != BuildableType.NOTHING) {
            JLabel turnsLabel = new JLabel(Messages.message("notApplicable.short"));
            turnsLabel.setBackground(Color.WHITE);
            turnsLabel.setOpaque(true);
            int turnsLeft = colony.getTurnsToComplete(buildable);
            if (turnsLeft > 0) {
                turnsLabel.setText(Integer.toString(turnsLeft));
            }
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
        int width = 128;
        int height = 96;

 
        Image bgImage = ResourceManager.getImage("model.building.BuildingSite.image");
        if (buildable != BuildableType.NOTHING) {
            bgImage = ResourceManager.getImage(buildable.getId() + ".image");
        }
 
        if (bgImage != null) {
            g.drawImage(bgImage, 0, 0, this);
            /*
            g.setColor(new Color(255, 255, 255, 100));
            g.fillRect(0, 0, width, height);
            */
        } else {
            Image tempImage = ResourceManager.getImage("BackgroundImage");

            if (tempImage != null) {
                for (int x = 0; x < width; x += tempImage.getWidth(null)) {
                    for (int y = 0; y < height; y += tempImage.getHeight(null)) {
                        g.drawImage(tempImage, x, y, null);
                    }
                }
            } else {
                g.setColor(getBackground());
                g.fillRect(0, 0, width, height);
            }
        }
    }

    public JToolTip createToolTip() {
        return new BuildingSiteToolTip(colony, parent);
    }

    public void propertyChange(PropertyChangeEvent event) {
        initialize();
    }

}


