/**
 *  Copyright (C) 2002-2017   The FreeCol Team
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
import java.util.Locale;
import java.util.logging.Logger;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.SwingGUI;
import net.sf.freecol.client.gui.panel.CargoPanel;
import net.sf.freecol.client.gui.panel.ColonyPanel;
import net.sf.freecol.client.gui.panel.EuropePanel;
import net.sf.freecol.client.gui.panel.InPortPanel;
import net.sf.freecol.client.gui.panel.report.ReportPanel;
import net.sf.freecol.client.gui.panel.Utility;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.WorkLocation;

import static net.sf.freecol.common.model.Unit.*;
import static net.sf.freecol.common.util.StringUtils.lastPart;


/**
 * This label holds Unit data in addition to the JLabel data, which makes it
 * ideal to use for drag and drop purposes.
 */
public final class UnitLabel extends FreeColLabel
        implements ActionListener, CargoLabel, Draggable {

    private static final Logger logger = Logger.getLogger(UnitLabel.class.getName());

    /**
     * The different actions a {@code Unit} is allowed to take.
     */
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

    private final FreeColClient freeColClient;

    private final SwingGUI gui;

    private final Unit unit;

    private boolean selected;

    private boolean isSmall;

    private boolean ignoreLocation;

    private final boolean useTileImageLibrary;


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
        this(freeColClient, unit, isSmall, ignoreLocation, false);
    }


    /**
     * Creates a JLabel to display a unit.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param unit The {@code Unit} to display.
     * @param isSmall The image will be smaller if set to {@code true}.
     * @param ignoreLocation The image will not include production or state
     *            information if set to {@code true}.
     * @param useTileImageLibrary If false use ImageLibrary in GUI.
     *              If true use tileImageLibrary in SwingGUI.
     */
    public UnitLabel(FreeColClient freeColClient, Unit unit,
                     boolean isSmall, boolean ignoreLocation,
                     boolean useTileImageLibrary) {
        this.freeColClient = freeColClient;
        this.gui = (SwingGUI)freeColClient.getGUI();
        this.unit = unit;

        selected = false;
        this.isSmall = isSmall;
        this.ignoreLocation = ignoreLocation;
        this.useTileImageLibrary = useTileImageLibrary;

        updateIcon();
    }


    /**
     * Returns this UnitLabel's unit data.
     *
     * @return This UnitLabel's unit data.
     */
    public Unit getUnit() {
        return unit;
    }


    /**
     * Sets whether or not this unit should be selected.
     *
     * @param b Whether or not this unit should be selected.
     */
    public void setSelected(boolean b) {
        selected = b;
    }


    /**
     * Sets whether or not this unit label should include production and state
     * information.
     *
     * @param b Whether or not this unit label should include production and
     *            state information.
     */
    public void setIgnoreLocation(boolean b) {
        ignoreLocation = b;
    }


    /**
     * Makes a smaller version.
     *
     * @param isSmall The image will be smaller if set to {@code true}.
     */
    public void setSmall(final boolean isSmall) {
        final ImageLibrary lib = useTileImageLibrary
                                 ? gui.getTileImageLibrary()
                                 : gui.getImageLibrary();
        if (isSmall) {
            Icon imageIcon = new ImageIcon(lib.getSmallUnitImage(unit)); // Color image
            Icon disabledImageIcon = new ImageIcon(lib.getSmallUnitImage(unit, true)); // Grayscale Image
            setPreferredSize(null);

            setIcon(imageIcon);
            setDisabledIcon(disabledImageIcon);
            setBorder(Utility.blankBorder(0, 2, 0, 0));
            this.isSmall = true;
        } else {
            Icon imageIcon = new ImageIcon(lib.getUnitImage(unit)); // Color image
            Icon disabledImageIcon = new ImageIcon(lib.getUnitImage(unit, true)); // Grayscale Image
            if (unit.getLocation() instanceof ColonyTile) {
                Dimension tileSize = lib.scaleDimension(ImageLibrary.TILE_SIZE);
                tileSize.width /= 2;
                tileSize.height = imageIcon.getIconHeight();
                setSize(tileSize);
            } else {
                setPreferredSize(null);
            }

            setIcon(imageIcon);
            setDisabledIcon(disabledImageIcon);
            setBorder((unit.getLocation() instanceof ColonyTile)
                      ? Utility.blankBorder(0, 15, 0, 15)
                      : Utility.blankBorder(0, 5, 0, 5));
            this.isSmall = false;
        }
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
     * Paints this UnitLabel.
     *
     * @param g The graphics context in which to do the painting.
     */
    @Override
    public void paintComponent(Graphics g) {
        final Player player = freeColClient.getMyPlayer();
        final ImageLibrary lib = useTileImageLibrary
                                 ? gui.getTileImageLibrary()
                                 : gui.getImageLibrary();
        if (ignoreLocation || selected
                || (!unit.isCarrier() && unit.getState() != UnitState.SENTRY)) {
            setEnabled(true);
        } else if (unit.getOwner() != player
                && unit.getColony() == null) {
            setEnabled(true);
        } else {
            setEnabled(false);
        }

        super.paintComponent(g);
        if (ignoreLocation)
            return;

        if (unit.getLocation() instanceof ColonyTile) {
            // Not Buildings.  Buildings have their own production display.
            GoodsType workType = unit.getWorkType();
            if (workType != null) {
                int production = ((ColonyTile)unit.getLocation())
                        .getTotalProductionOf(workType);
                ProductionLabel pl = new ProductionLabel(freeColClient, lib,
                                                         new AbstractGoods(workType, production));
                g.translate(0, 10);
                pl.paintComponent(g);
                g.translate(0, -10);
            }
        } else if (getParent() instanceof ColonyPanel.OutsideColonyPanel ||
                getParent() instanceof InPortPanel ||
                getParent() instanceof EuropePanel.DocksPanel ||
                getParent().getParent() instanceof ReportPanel) {
            String text = Messages.message(unit.getOccupationLabel(player, false));
            g.drawImage(lib.getOccupationIndicatorChip((Graphics2D)g, unit, text), 0, 0, null);

            if (unit.isDamaged()) {
                String underRepair = Messages.message(unit.getRepairLabel());
                String underRepair1 = underRepair.substring(0, underRepair.indexOf('(')).trim();
                String underRepair2 = underRepair.substring(underRepair.indexOf('(')).trim();
                Font font = FontLibrary.createFont(FontLibrary.FontType.NORMAL,
                                                   FontLibrary.FontSize.TINY, lib.getScaleFactor());
                Image repairImage1 = lib.getStringImage(g, underRepair1, Color.RED, font);
                Image repairImage2 = lib.getStringImage(g, underRepair2, Color.RED, font);
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


    /**
     * Update the icon for this unit label.
     */
    public void updateIcon() {
        setDescriptionLabel(unit.getDescription(UnitLabelType.FULL));
        setSmall(isSmall);
        // repaint(0, 0, getWidth(), getHeight());
        // uc.refresh();
    }


    /**
     * Can a unit be equipped with a particular label.
     *
     * @param data The label to add.
     * @return True if the label refers to suitable equipment.
     */
    public boolean canUnitBeEquippedWith(JLabel data) {
        return getUnit().hasAbility(Ability.CAN_BE_EQUIPPED)
                && data instanceof AbstractGoodsLabel;
    }


    //Interface CargoLabel

    /**
     * {@inheritDoc}
     */
    @Override
    public Component addCargo(Component comp, Unit carrier, CargoPanel cargoPanel) {
        Unit unit = ((UnitLabel)comp).getUnit();
        if (carrier.canAdd(unit)) {
            Container oldParent = comp.getParent();
            if (cargoPanel.igc().boardShip(unit, carrier)) {
                ((UnitLabel) comp).setSmall(false);
                if (oldParent != null) oldParent.remove(comp);
                cargoPanel.update();
                return comp;
            }
        }
        return null;
    }

    @Override
    public void removeCargo(Component comp, CargoPanel cargoPanel) {
        Unit unit = ((UnitLabel)comp).getUnit();
        cargoPanel.igc().leaveShip(unit);
        cargoPanel.update();
    }

    // Interface Draggable

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOnCarrier() {
        return unit != null && unit.isOnCarrier();
    }


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        final Game game = freeColClient.getGame();
        final Specification spec = game.getSpecification();
        final InGameController igc = freeColClient.getInGameController();
        String[] args = ae.getActionCommand().split("/");
        GoodsType gt;
        switch (Enum.valueOf(UnitAction.class,
                             args[0].toUpperCase(Locale.US))) {
            case ASSIGN:
                igc.assignTeacher(unit,
                                  game.getFreeColGameObject(args[1], Unit.class));
                break;
            case WORK_COLONYTILE:
                if (args.length < 3) break;
                ColonyTile colonyTile
                        = game.getFreeColGameObject(args[1], ColonyTile.class);
                if (args.length >= 4 && "!".equals(args[3])) {
                    // Claim tile if needed
                    if (!igc.claimTile(colonyTile.getWorkTile(),
                                       unit.getColony())) break;
                }
                if (colonyTile != unit.getLocation()) igc.work(unit, colonyTile);
                if ((gt = spec.getGoodsType(args[2])) != null
                        && unit.getWorkType() != gt) {
                    igc.changeWorkType(unit, gt);
                }
                break;
            case WORK_BUILDING:
                if (args.length < 3) break;
                Building building
                        = game.getFreeColGameObject(args[1], Building.class);
                if (building != unit.getLocation()) igc.work(unit, building);
                if ((gt = spec.getGoodsType(args[2])) != null
                        && unit.getWorkType() != gt) {
                    igc.changeWorkType(unit, gt);
                }
                break;
            case ACTIVATE_UNIT:
                igc.changeState(unit, UnitState.ACTIVE);
                gui.setActiveUnit(unit);
                break;
            case FORTIFY:
                igc.changeState(unit, UnitState.FORTIFYING);
                break;
            case SENTRY:
                igc.changeState(unit, UnitState.SENTRY);
                break;
            case COLOPEDIA:
                gui.showColopediaPanel(unit.getType().getId());
                break;
            case LEAVE_TOWN:
                igc.putOutsideColony(unit);
                break;
            case CLEAR_SPECIALITY:
                igc.clearSpeciality(unit);
                break;
            case CLEAR_ORDERS:
                igc.clearOrders(unit);
                break;
            case ASSIGN_TRADE_ROUTE:
                gui.showTradeRoutePanel(unit);
                break;
            case LEAVE_SHIP:
                igc.leaveShip(unit);
                break;
            case UNLOAD:
                igc.unload(unit);
                break;
        }
        updateIcon();
    }
}
