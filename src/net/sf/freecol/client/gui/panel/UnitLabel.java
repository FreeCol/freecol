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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.common.resources.ResourceManager;
import net.sf.freecol.common.util.Utils;


/**
 * This label holds Unit data in addition to the JLabel data, which makes it
 * ideal to use for drag and drop purposes.
 */
public final class UnitLabel extends JLabel
    implements ActionListener, Draggable {

    @SuppressWarnings("unused")
        private static Logger logger = Logger.getLogger(UnitLabel.class.getName());

    public static enum UnitAction {
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

    private final GUI gui;

    private final Unit unit;

    private final InGameController inGameController;

    private boolean selected;

    private boolean isSmall = false;

    private boolean ignoreLocation;


    /**
     * Creates a JLabel to display a unit.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param unit The <code>Unit</code> to display.
     */
    public UnitLabel(FreeColClient freeColClient, Unit unit) {
        this.freeColClient = freeColClient;
        this.gui = freeColClient.getGUI();
        this.unit = unit;
        this.inGameController = freeColClient.getInGameController();

        selected = false;
        setSmall(false);
        setIgnoreLocation(false);

        updateIcon();
    }

    /**
     * Creates a JLabel to display a unit.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param unit The <code>Unit</code> to display.
     * @param isSmall The image will be smaller if set to <code>true</code>.
     */
    public UnitLabel(FreeColClient freeColClient, Unit unit, boolean isSmall) {
        this(freeColClient, unit);

        setSmall(isSmall);
        setIgnoreLocation(false);
    }

    /**
     * Creates a JLabel to display a unit.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param unit The <code>Unit</code> to display.
     * @param isSmall The image will be smaller if set to <code>true</code>.
     * @param ignoreLocation The image will not include production or state
     *            information if set to <code>true</code>.
     */
    public UnitLabel(FreeColClient freeColClient, Unit unit,
                     boolean isSmall, boolean ignoreLocation) {
        this(freeColClient, unit);

        setSmall(isSmall);
        setIgnoreLocation(ignoreLocation);
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
     * @param isSmall The image will be smaller if set to <code>true</code>.
     */
    public void setSmall(boolean isSmall) {
        ImageIcon imageIcon = gui.getImageLibrary().getUnitImageIcon(unit);
        ImageIcon disabledImageIcon = gui.getImageLibrary().getUnitImageIcon(unit, true);
        if (isSmall) {
            setPreferredSize(null);
            // setIcon(new
            // ImageIcon(imageIcon.getImage().getScaledInstance(imageIcon.getIconWidth()
            // / 2, imageIcon.getIconHeight() / 2, Image.SCALE_DEFAULT)));
            setIcon(new ImageIcon(imageIcon.getImage().getScaledInstance((imageIcon.getIconWidth() / 3) * 2,
                                                                         (imageIcon.getIconHeight() / 3) * 2, Image.SCALE_SMOOTH)));

            setDisabledIcon(new ImageIcon(disabledImageIcon.getImage().getScaledInstance(
                                                                                         (imageIcon.getIconWidth() / 3) * 2, (imageIcon.getIconHeight() / 3) * 2, Image.SCALE_SMOOTH)));
            setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
            this.isSmall = true;
        } else {
            if (unit.getLocation() instanceof ColonyTile) {
                final Tile tile = ((ColonyTile) unit.getLocation()).getTile();
                final TileType tileType = tile.getType();
                final Image image = gui.getImageLibrary().getTerrainImage(tileType, tile.getX(), tile.getY());
                setSize(new Dimension(image.getWidth(null) / 2,
                                      imageIcon.getIconHeight()));
            } else {
                setPreferredSize(null);
            }

            setIcon(imageIcon);
            setDisabledIcon(disabledImageIcon);
            if (unit.getLocation() instanceof ColonyTile) {
                setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
            } else {
                setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
            }
            this.isSmall = false;
        }
    }

    /**
     * Gets the description label.
     *
     * The description label is a tooltip with the unit name and description of
     * the terrain its on if applicable *
     *
     * @return This UnitLabel's description label.
     */
    public String getDescriptionLabel() {
        return getToolTipText();
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
    public void paintComponent(Graphics g) {
        final Player player = freeColClient.getMyPlayer();
        if (ignoreLocation || selected
            || (!unit.isCarrier() && unit.getState() != Unit.UnitState.SENTRY)) {
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
            GoodsType workType = unit.getWorkType();
            if (workType != null) {
                int production = ((ColonyTile)unit.getLocation())
                    .getTotalProductionOf(workType);
                ProductionLabel pl = new ProductionLabel(freeColClient,
                                                         workType, production);
                g.translate(0, 10);
                pl.paintComponent(g);
                g.translate(0, -10);
            }
        } else if (getParent() instanceof ColonyPanel.OutsideColonyPanel ||
                   getParent() instanceof InPortPanel ||
                   getParent() instanceof EuropePanel.DocksPanel ||
                   getParent().getParent() instanceof ReportPanel) {
            String text = Messages.message(unit.getOccupationKey(player.owns(unit)));
            ImageLibrary lib = gui.getImageLibrary();
            g.drawImage(lib.getOccupationIndicatorChip(unit, text), 0, 0, null);

            if (unit.isDamaged()) {
                String underRepair = Messages.message(StringTemplate.template("underRepair")
                                                      .addAmount("%turns%", unit.getTurnsForRepair()));
                String underRepair1 = underRepair.substring(0, underRepair.indexOf('(')).trim();
                String underRepair2 = underRepair.substring(underRepair.indexOf('(')).trim();
                Font font = ResourceManager.getFont("NormalFont", 14f);
                Image repairImage1 = lib.getStringImage((Graphics2D)g, underRepair1, Color.RED, font);
                Image repairImage2 = lib.getStringImage((Graphics2D)g, underRepair2, Color.RED, font);
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
     * Analyzes an event and calls the right external methods to take care of
     * the user's request.
     *
     * @param event The incoming action event
     */
    public void actionPerformed(ActionEvent event) {
        final Game game = freeColClient.getGame();
        final Specification spec = game.getSpecification();
        String[] args = event.getActionCommand().split("/");
        switch (Enum.valueOf(UnitAction.class,
                             args[0].toUpperCase(Locale.US))) {
        case ASSIGN:
            inGameController.assignTeacher(unit,
                game.getFreeColGameObject(args[1], Unit.class));
            /*
            Component uc = getParent();
            while (uc != null) {
                if (uc instanceof ColonyPanel) {
                    ((ColonyPanel) uc).reinitialize();
                    break;
                }
                uc = uc.getParent();
            }
            */
            break;
        case WORK_COLONYTILE:
            if (args.length < 3) break;
            ColonyTile colonyTile
                = game.getFreeColGameObject(args[1], ColonyTile.class);
            if (args.length >= 4 && "!".equals(args[3])) {
                // Claim tile if needed
                if (!inGameController.claimLand(colonyTile.getWorkTile(), 
                                                unit.getColony(), 0)) break;
            }
            if (colonyTile != unit.getLocation()) {
                inGameController.work(unit, colonyTile);
            }
            inGameController.changeWorkType(unit, spec.getGoodsType(args[2]));
            break;
        case WORK_BUILDING:
            if (args.length < 3) break;
            Building building
                = game.getFreeColGameObject(args[1], Building.class);
            if (building == unit.getLocation()) break;
            inGameController.changeWorkType(unit, spec.getGoodsType(args[2]));
            inGameController.work(unit, building);
            break;
        case ACTIVATE_UNIT:
            inGameController.changeState(unit, Unit.UnitState.ACTIVE);
            gui.setActiveUnit(unit);
            break;
        case FORTIFY:
            inGameController.changeState(unit, Unit.UnitState.FORTIFYING);
            break;
        case SENTRY:
            inGameController.changeState(unit, Unit.UnitState.SENTRY);
            break;
        case COLOPEDIA:
            gui.showColopediaPanel(unit.getType().getId());
            break;
        case LEAVE_TOWN:
            inGameController.putOutsideColony(unit);
            break;
        case CLEAR_SPECIALITY:
            inGameController.clearSpeciality(unit);
            break;
        case CLEAR_ORDERS:
            inGameController.clearOrders(unit);
            break;
        case ASSIGN_TRADE_ROUTE:
            inGameController.assignTradeRoute(unit);
            break;
        case LEAVE_SHIP:
            inGameController.leaveShip(unit);
            break;
        case UNLOAD:
            inGameController.unload(unit);
            break;
        }
        updateIcon();
    }


    public void updateIcon() {
        ImageLibrary lib = gui.getImageLibrary();
        setIcon(lib.getUnitImageIcon(unit));
        setDisabledIcon(lib.getUnitImageIcon(unit, true));
        setDescriptionLabel(Messages.message(unit.getFullLabel()));
        StringTemplate label = unit.getEquipmentLabel();
        if (label != null) {
            setDescriptionLabel(getDescriptionLabel() + " ("
                                + Messages.message(label) + ")");
        }
        setSmall(isSmall);

        Component uc = getParent();
        while (uc != null) {
            if (uc instanceof ColonyPanel) {
                if (unit.getColony() == null) {
                    gui.removeFromCanvas(uc);
                    freeColClient.updateActions();
                } else {
                    // ((ColonyPanel) uc).reinitialize();
                }

                break;
            } else if (uc instanceof EuropePanel) {
                break;
            }

            uc = uc.getParent();
        }

        // repaint(0, 0, getWidth(), getHeight());
        // uc.refresh();
    }

    public boolean canUnitBeEquipedWith(JLabel data){
        if(!getUnit().hasAbility(Ability.CAN_BE_EQUIPPED)){
            return false;
        }

        if(data instanceof GoodsLabel && ((GoodsLabel)data).isToEquip()){
            return true;
        }

        if(data instanceof MarketLabel && ((MarketLabel)data).isToEquip()){
            return true;
        }

        return false;
    }

    public boolean isOnCarrier() {
        return unit != null && unit.isOnCarrier();
    }

    /**
     * Gets a string corresponding to the UnitAction to work at a work
     * location.
     *
     * @param wl The <code>WorkLocation</code> to use.
     * @return The unit action as a string.
     */
    public static String getWorkLabel(WorkLocation wl) {
        return "WORK_" + Utils.lastPart(wl.getClass().toString(), ".")
            .toUpperCase(Locale.US);
    }
}
