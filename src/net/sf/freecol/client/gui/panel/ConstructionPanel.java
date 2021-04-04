/**
 *  Copyright (C) 2002-2021   The FreeCol Team
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

import java.awt.Font;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.Size;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Turn;

import static net.sf.freecol.common.util.StringUtils.getBreakingPoint;


/**
 * This panel shows the progress of constructing a building or
 * unit in a colony.
 */
public class ConstructionPanel extends MigPanel
    implements PropertyChangeListener {

    public static final String EVENT
        = Colony.ColonyChangeEvent.BUILD_QUEUE_CHANGE.toString();

    /** The enclosing client. */
    private final FreeColClient freeColClient;

    /** Should a mouse click open the build queue? */
    private final boolean openBuildQueue;

    /** The colony performing the construction. */
    private Colony colony;

    /** The text to display if buildable == null. */
    private StringTemplate defaultLabel
        = StringTemplate.key("constructionPanel.clickToBuild");


    /**
     * Creates a ConstructionPanel.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param colony The {@code Colony} whose construction is to be
     *     modified.
     * @param openBuildQueue True if the build queue should be immediately
     *     shown.
     */
    public ConstructionPanel(FreeColClient freeColClient,
                             Colony colony, boolean openBuildQueue) {
        super("ConstructionPanelUI",
            new MigLayout("fill, gapy 2:5, wrap 2", "push[]10[center]push"));

        this.freeColClient = freeColClient;
        this.colony = colony;
        this.openBuildQueue = openBuildQueue;

        setOpaque(openBuildQueue);
    }


    /**
     * Add a listener for button presses on this panel to show the BuildQueuePanel
     */
    public void initialize() {
        if (colony != null) {
            // we are interested in changes to the build queue, as well as
            // changes to the warehouse and the colony's production bonus
            colony.addPropertyChangeListener(EVENT, this);
                
            if (openBuildQueue) {
                addMouseListener(new MouseAdapter() {
                        @Override
                        public void mousePressed(MouseEvent e) {
                            freeColClient.getGUI().showBuildQueuePanel(colony);
                        }
                    });
            }
        }
        update();
    }

    /**
     * Removes PropertyChangeListeners and MouseListeners
     */
    public void cleanup() {
        if (colony != null) {
            colony.removePropertyChangeListener(EVENT, this);
        }
        for (MouseListener listener : getMouseListeners()) {
            removeMouseListener(listener);
        }
    }

    /**
     * This method updates the Construction Panel.
     *
     * With zero arguments, the update() method can only be
     * run on what a given colony is currently building.
     */
    public void update() {
        update((colony == null) ? null : colony.getCurrentlyBuilding());
    }

    /**
     * This method updates the Construction Panel.
     *
     * With one argument, the update() method can be called
     * to update the panel based on a called {@code BuildableType}
     * This method is used when a change to to the
     * {@code BuildQueuePanel} are made.
     *
     * @param buildable The BuildableType object to update.
     *
     * @see BuildQueuePanel for the only use of the one-argument method.
     */
    public void update(BuildableType buildable) {
        removeAll();
        final ImageLibrary lib = this.freeColClient.getGUI()
            .getFixedImageLibrary();
        final Font font = FontLibrary.getUnscaledFont("normal-plain-smaller");

        if (buildable == null) {
            String clickToBuild = Messages.message(getDefaultLabel());
            int breakingPoint = getBreakingPoint(clickToBuild);
            if (breakingPoint > 0) {
                JLabel label0 = new JLabel(
                    clickToBuild.substring(0, breakingPoint));
                label0.setFont(font);
                add(label0, "span, align center");
                JLabel label1 = new JLabel(
                    clickToBuild.substring(breakingPoint + 1));
                label1.setFont(font);
                add(label1, "span, align center");
            } else {
                JLabel label = new JLabel(clickToBuild);
                label.setFont(font);
                add(label, "span, align center");
            }
        } else {
            int turns = colony.getTurnsToComplete(buildable);
            Image image = lib.getSmallBuildableTypeImage(buildable, colony.getOwner());
            add(new JLabel(new ImageIcon(image)), "spany");
            JLabel label0 = Utility.localizedLabel(buildable.getCurrentlyBuildingLabel());
            label0.setFont(font);
            add(label0);
            JLabel label1 = Utility.localizedLabel(StringTemplate
                .template("constructionPanel.turnsToComplete")
                .addName("%number%", Turn.getTurnsText(turns)));
            label1.setFont(font);
            add(label1);

            for (AbstractGoods ag : buildable.getRequiredGoodsList()) {
                int amountNeeded = ag.getAmount();
                int amountAvailable = colony.getGoodsCount(ag.getType());
                int amountProduced = colony.getAdjustedNetProductionOf(ag.getType());
                add(new FreeColProgressBar(this.freeColClient, ag.getType(), 0,
                                           amountNeeded, amountAvailable, amountProduced),
                    "height 20:");
            }
        }

        revalidate();
        repaint();
    }


    /**
     * @return A {@code StringTemplate} of the ConstructionPanel's Label
     */
    private final StringTemplate getDefaultLabel() {
        return defaultLabel;
    }

    /**
     * Set the ConstructionPanel's Label as a {@code StringTemplate}
     *
     * @param newDefaultLabel The default StringTemplate label of the panel.
     */
    public final void setDefaultLabel(final StringTemplate newDefaultLabel) {
        this.defaultLabel = newDefaultLabel;
    }


    // Interface PropertyChangeListener

    /**
     * {@inheritDoc}
     *
     * Upon a change to a bound parameter, call the {@link #update()} method.
     */
    @Override
    public void propertyChange(PropertyChangeEvent event) {
        update();
    }
}
