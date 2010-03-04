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

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.resources.ResourceManager;

/**
 * This panel represents a single building in a Colony.
 */
public class ConstructionPanel extends JPanel implements PropertyChangeListener {

    private final Canvas parent;

    private Colony colony;

    private BuildableType buildable;

    /**
     * Creates this BuildingToolTip.
     * 
     * @param parent a <code>Canvas</code> value
     * @param colony a <code>Colony</code> value
     */
    public ConstructionPanel(final Canvas parent, Colony colony) {

        this.parent = parent;
        setLayout(new MigLayout("fill", "push[]10[]push", "0[]0"));
        setColony(colony);
    }

    public void setColony(Colony newColony) {
    	if(newColony != colony){
    		if (colony != null) {
    			colony.removePropertyChangeListener(this);
    		}
    		this.colony = newColony;

    		// we are interested in changes to the build queue, as well as
    		// changes to the warehouse and the colony's production bonus
    		colony.addPropertyChangeListener(this);
    		addMouseListener(new MouseAdapter() {
    			public void mousePressed(MouseEvent e) {
    				BuildQueuePanel queuePanel = new BuildQueuePanel(colony, parent);
    				parent.showSubPanel(queuePanel);
    			}
    		});
    	}
        initialize();
    }

    private void initialize() {
   
        removeAll();

        buildable = colony.getCurrentlyBuilding();
        if (buildable == null) {
            add(new JLabel(Messages.message("colonyPanel.clickToBuild")),
                "span, align center");
        } else {
            int turnsToComplete = colony.getTurnsToComplete(buildable);
            String turns = Messages.message("notApplicable.short");
            if (turnsToComplete >= 0) {
                turns = Integer.toString(turnsToComplete);
            }
            else if(turnsToComplete != Integer.MIN_VALUE){
                turns = ">" + Integer.toString(turnsToComplete*-1);
            }
            add(new JLabel(new ImageIcon(getScaledImage(ResourceManager.getImage(buildable.getId() + ".image"), 0.75))), "");
            add(new JLabel(Messages.message("colonyPanel.currentlyBuilding",
                                            "%buildable%", Messages.message(buildable.getNameKey()))),
                "span, align center, flowy, split " + (2 + buildable.getGoodsRequired().size()));

            add(new JLabel(Messages.message("turnsToComplete.long",
                                            "%number%", turns)),
                "span, align center");

            for (AbstractGoods requiredGoods : buildable.getGoodsRequired()) {
                int amountNeeded = requiredGoods.getAmount();
                int amountAvailable = colony.getGoodsCount(requiredGoods.getType());
                int amountProduced = colony.getProductionNetOf(requiredGoods.getType());
                add(new FreeColProgressBar(parent, requiredGoods.getType(), 0,
                                                        amountNeeded, amountAvailable, amountProduced));
            }
        }

        revalidate();
        repaint();
    }

    private Image getScaledImage(Image inImage, double factor){
        int w = (int)(factor * inImage.getWidth(null));
        int h = (int)(factor * inImage.getHeight(null));
	    BufferedImage outImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = outImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.scale(factor, factor);
		g2d.drawImage(inImage, 0, 0, null);
        g2d.dispose();
        return outImage;
    }

    public void propertyChange(PropertyChangeEvent event) {
        initialize();
    }

    public void removePropertyChangeListeners() {
        colony.removePropertyChangeListener(this);
    }
}


