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

package net.sf.freecol.client.gui.label;

import static net.sf.freecol.common.util.StringUtils.upCase;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.panel.CargoPanel;
import net.sf.freecol.client.gui.panel.ColonyPanel;
import net.sf.freecol.client.gui.panel.EuropePanel;
import net.sf.freecol.client.gui.panel.InPortPanel;
import net.sf.freecol.client.gui.panel.Utility;
import net.sf.freecol.client.gui.panel.report.ReportPanel;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitLabelType;
import net.sf.freecol.common.model.Unit.UnitState;


/**
 * This label holds Unit data in addition to the JLabel data, which makes it
 * ideal to use for drag and drop purposes.
 */
public final class UnitLabel extends FreeColLabel
        implements ActionListener, CargoLabel, Draggable {

    private static final Logger logger = Logger.getLogger(UnitLabel.class.getName());

    /** The different actions a {@code Unit} is allowed to take. */
    public enum UnitAction {
        ASSIGN,
        CLEAR_SPECIALITY,
        ACTIVATE_UNIT,
        FORTIFY,
        SENTRY,
        COLOPEDIA,
        LEAVE_TOWN,
        WORK_COLONYTILE, // Must match the WorkLocation actual type
        WORK_BUILDING,   // Must match the WorkLocation actual type
        CLEAR_ORDERS,
        ASSIGN_TRADE_ROUTE,
        LEAVE_SHIP,
        UNLOAD,
    }

    /** The enclosing client. */
    private final FreeColClient freeColClient;

    /** The unit this is a label for. */
    private final Unit unit;

    /** Is this a currently selected unit? */
    private boolean selected;

    /** Is this a small label? */
    private boolean isSmall;

    /**
     * Should the location information be ignored for this label
     * description?
     */
    private boolean ignoreLocation;

    /** Font for labels. */
    private final Font tinyFont;


    /**
     * Creates a JLabel to display a unit.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param unit The {@code Unit} to display.
     */
    public UnitLabel(FreeColClient freeColClient, Unit unit) {
        this(freeColClient, unit, false);
    }


    /**
     * Creates a JLabel to display a unit.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param unit The {@code Unit} to display.
     * @param isSmall The image will be smaller if set to {@code true}.
     */
    public UnitLabel(FreeColClient freeColClient, Unit unit, boolean isSmall) {
        this(freeColClient, unit, isSmall, false);
    }


    /**
     * Creates a JLabel to display a unit.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param unit The {@code Unit} to display.
     * @param isSmall The image will be smaller if set to {@code true}.
     * @param ignoreLocation The image will not include production or state
     *            information if set to {@code true}.
     */
    public UnitLabel(FreeColClient freeColClient, Unit unit,
                     boolean isSmall, boolean ignoreLocation) {
        this.freeColClient = freeColClient;
        this.unit = unit;
        this.selected = false;
        this.isSmall = isSmall;
        this.ignoreLocation = ignoreLocation;
        this.tinyFont = freeColClient.getGUI().getFixedImageLibrary().getScaledFont("normal-plain-tiny", null);

        setHorizontalAlignment(CENTER);
        
        updateIcon();
    }


    /**
     * Internal GUI accessor.
     *
     * @return The current {@code GUI}.
     */
    private GUI getGUI() {
        return this.freeColClient.getGUI();
    }

    /**
     * Get the correct image library for this unit label.
     *
     * @return The {@code ImageLibrary} to use.
     */
    private ImageLibrary getImageLibrary() {
        return getGUI().getFixedImageLibrary();
    }

    /**
     * Get the associated unit.
     *
     * @return The {@code Unit} this is the label for.
     */
    public Unit getUnit() {
        return this.unit;
    }

    /**
     * Sets whether or not this unit should be selected.
     *
     * @param b Whether or not this unit should be selected.
     */
    public void setSelected(boolean b) {
        this.selected = b;
    }

    /**
     * Makes a smaller version.
     *
     * @param isSmall The image will be smaller if set to {@code true}.
     */
    public void setSmall(final boolean isSmall) {
        final ImageLibrary lib = getImageLibrary();
        if (isSmall) {
            setPreferredSize(null);
            setIcon(new ImageIcon(lib.getSmallUnitImage(this.unit)));
            setBorder(Utility.blankBorder(0, 2, 0, 0));
        } else {
            Icon imageIcon = new ImageIcon(lib.getScaledUnitImage(this.unit));
            if (this.unit.getLocation() instanceof ColonyTile) {
                setPreferredSize(new Dimension(lib.scale(ImageLibrary.TILE_SIZE).width, imageIcon.getIconHeight()));
            } else {
                setPreferredSize(null);
            }
            setIcon(imageIcon);
            setBorder((this.unit.getLocation() instanceof ColonyTile)
                      ? Utility.blankBorder(0, 15, 0, 15)
                      : Utility.blankBorder(0, 5, 0, 5));
        }
        this.isSmall = isSmall;
    }

    /**
     * Sets the description label.
     *
     * The description label is a tooltip with the unit name and description of
     * the terrain its on if applicable
     *
     * @param label The string to set the label to.
     */
    public void setDescriptionLabel(String label) {
        setToolTipText(label);
    }

    /**
     * Update the icon for this unit label.
     */
    public void updateIcon() {
        setDescriptionLabel(getUnit().getDescription(UnitLabelType.FULL));
        setSmall(this.isSmall);
        // repaint(0, 0, getWidth(), getHeight());
        // uc.refresh();
    }


    // Interface CargoLabel

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addCargo(Component comp, Unit carrier, CargoPanel cargoPanel) {
        final Unit u = ((UnitLabel)comp).getUnit();
        if (carrier.canAdd(u)) {
            Container oldParent = comp.getParent();
            if (cargoPanel.igc().boardShip(u, carrier)) {
                ((UnitLabel)comp).setSmall(false);
                if (oldParent != null) oldParent.remove(comp);
                cargoPanel.update();
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeCargo(Component comp, CargoPanel cargoPanel) {
        final Unit u = ((UnitLabel)comp).getUnit();
        cargoPanel.igc().leaveShip(u);
        cargoPanel.update();
    }


    // Interface Draggable

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOnCarrier() {
        return this.unit != null && this.unit.isOnCarrier();
    }


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        final Game game = this.freeColClient.getGame();
        final Specification spec = game.getSpecification();
        final InGameController igc = this.freeColClient.getInGameController();
        String[] args = ae.getActionCommand().split("/");
        GoodsType gt;
        switch (Enum.valueOf(UnitAction.class, upCase(args[0]))) {
            case ASSIGN:
                igc.assignTeacher(this.unit,
                    game.getFreeColGameObject(args[1], Unit.class));
                break;
            case WORK_COLONYTILE:
                if (args.length < 3) break;
                ColonyTile colonyTile
                    = game.getFreeColGameObject(args[1], ColonyTile.class);
                if (args.length >= 4 && "!".equals(args[3])) {
                    // Claim tile if needed
                    if (!igc.claimTile(colonyTile.getWorkTile(),
                                       this.unit.getColony())) break;
                }
                if (colonyTile != this.unit.getLocation()) {
                    igc.work(this.unit, colonyTile);
                }
                if ((gt = spec.getGoodsType(args[2])) != null
                    && this.unit.getWorkType() != gt) {
                    igc.changeWorkType(this.unit, gt);
                }
                break;
            case WORK_BUILDING:
                if (args.length < 3) break;
                Building building
                    = game.getFreeColGameObject(args[1], Building.class);
                if (building != this.unit.getLocation()) {
                    igc.work(this.unit, building);
                }
                if ((gt = spec.getGoodsType(args[2])) != null
                    && this.unit.getWorkType() != gt) {
                    igc.changeWorkType(this.unit, gt);
                }
                break;
            case ACTIVATE_UNIT:
                igc.changeState(this.unit, UnitState.ACTIVE);
                getGUI().changeView(this.unit, false);
                break;
            case FORTIFY:
                igc.changeState(this.unit, UnitState.FORTIFYING);
                break;
            case SENTRY:
                igc.changeState(this.unit, UnitState.SENTRY);
                break;
            case COLOPEDIA:
                getGUI().showColopediaPanel(this.unit.getType().getId());
                break;
            case LEAVE_TOWN:
                igc.putOutsideColony(this.unit);
                break;
            case CLEAR_SPECIALITY:
                igc.clearSpeciality(this.unit);
                break;
            case CLEAR_ORDERS:
                igc.clearOrders(this.unit);
                break;
            case ASSIGN_TRADE_ROUTE:
                getGUI().showTradeRoutePanel(this.unit);
                break;
            case LEAVE_SHIP:
                igc.leaveShip(this.unit);
                break;
            case UNLOAD:
                igc.unload(this.unit);
                break;
        }
        updateIcon();
    }


    // Override JComponent

    /**
     * {@inheritDoc}
     */
    @Override
    public void paintComponent(Graphics g) {
        final Player player = this.freeColClient.getMyPlayer();
        final ImageLibrary lib = getImageLibrary();
        if (ignoreLocation || selected
            || (!this.unit.isCarrier()
                && this.unit.getState() != UnitState.SENTRY)) {
            setEnabled(true);
        } else if (!player.owns(this.unit) && this.unit.getColony() == null) {
            setEnabled(true);
        } else {
            setEnabled(false);
        }

        super.paintComponent(g);
        
        if (ignoreLocation) return;

        if (this.unit.getLocation() instanceof ColonyTile) {
            // Not Buildings.  Buildings have their own production display.
            GoodsType workType = this.unit.getWorkType();
            if (workType != null) {
                int production = ((ColonyTile)this.unit.getLocation())
                    .getTotalProductionOf(workType);
                if (production > 0) {
                    ProductionLabel pl = new ProductionLabel(this.freeColClient,
                        new AbstractGoods(workType, production));

                    final int visualOffsetY = -lib.scaleInt(5);
                    
                    final Dimension size = getSize();
                    final Dimension plSize = pl.getPreferredSize();                    
                    final int x = (size.width - plSize.width) / 2;
                    final int y = (size.height - plSize.height) / 2 + visualOffsetY;
                    g.translate(x, y);
                    pl.paintComponent(g);
                    g.translate(-x, -y);
                }
            }
        } else if (getParent() instanceof ColonyPanel.OutsideColonyPanel ||
                getParent() instanceof InPortPanel ||
                getParent() instanceof EuropePanel.EuropeanDocksPanel ||
                getParent().getParent() instanceof ReportPanel) {
            String text = Messages.message(this.unit.getOccupationLabel(player, false));
            g.drawImage(lib.getOccupationIndicatorChip((Graphics2D)g, this.unit, text), 0, 0, null);

            if (this.unit.isDamaged()) {
                String underRepair = Messages.message(this.unit.getRepairLabel());
                int idx = underRepair.indexOf('(');
                String underRepair1 = underRepair.substring(0, idx).trim();
                String underRepair2 = underRepair.substring(idx).trim();
                Image repairImage1 = lib.getStringImage(g, underRepair1,
                    Color.RED, this.tinyFont);
                Image repairImage2 = lib.getStringImage(g, underRepair2,
                    Color.RED, this.tinyFont);
                int textHeight = repairImage1.getHeight(null) + repairImage2.getHeight(null);
                int leftIndent = Math.min(5, Math.min(getWidth() - repairImage1.getWidth(null),
                                                      getWidth() - repairImage2.getWidth(null)));
                g.drawImage(repairImage1,
                            leftIndent, // indent from left side of label (icon is placed at the left side)
                            ((getHeight() - textHeight) / 2),
                            null);
                g.drawImage(repairImage2,
                            leftIndent,
                            ((getHeight() - textHeight) / 2) + repairImage1.getHeight(null),
                            null);
            }
        }
    }

    // Override Component

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEnabled(boolean b) {
        if (!b) {
            // Grayscale images are slow to compute, only bother in the
            // rare case we need them and let the image cache keep it.
            ImageLibrary lib = getImageLibrary();
            Icon disabledImageIcon = (this.isSmall)
                ? new ImageIcon(lib.getSmallUnitImage(this.unit, true))
                : new ImageIcon(lib.getScaledUnitImage(this.unit, true));
            setDisabledIcon(disabledImageIcon);
        }
        super.setEnabled(b);
    }
}
