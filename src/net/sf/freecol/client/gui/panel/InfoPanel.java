/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.action.EndTurnAction;
import net.sf.freecol.client.gui.panel.MapEditorTransformPanel.MapTransform;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.resources.ResourceManager;

import static net.sf.freecol.common.util.StringUtils.splitText;


/**
 * The InfoPanel is really a wrapper for a collection of useful panels
 * that share the lower right corner.
 *
 * - EndTurnPanel: shows the end-turn button when there are no active units
 *
 * - MapEditorPanel: shows the current transform in map editor mode
 *
 * - TileInfoPanel: shows the details of a tile
 *
 * - UnitInfoPanel: shows the current active unit
 */
public final class InfoPanel extends FreeColPanel {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(InfoPanel.class.getName());

    private static final int SLACK = 5; // Small gap

    /**
     * Panel for ending the turn.
     */
    public class EndTurnPanel extends MigPanel {

        public EndTurnPanel() {
            super(new MigLayout("wrap 1, center", "[center]", ""));

            final ImageLibrary lib = getGUI().getTileImageLibrary();
            Font font = FontLibrary.createFont(FontLibrary.FontType.NORMAL,
                FontLibrary.FontSize.TINY, lib.getScaleFactor());

            String labelString = Messages.message("infoPanel.endTurn");
            for (String s : splitText(labelString, " /",
                                      getFontMetrics(font), 150)) {
                JLabel label = new JLabel(s);
                label.setFont(font);
                add(label);
            }

            JButton button = new JButton(getFreeColClient().getActionManager()
                .getFreeColAction(EndTurnAction.id));
            button.setFont(font);
            add(button);
            setOpaque(false);
            setSize(getPreferredSize());
        }
    }

    /**
     * Panel for displaying <code>Tile</code>-information.
     */
    public class TileInfoPanel extends MigPanel {

        private static final int PRODUCTION = 4;
        
        private Tile tile;

        // TODO: Find a way of removing the need for an extremely tiny font.
        //private final Font font = new JLabel().getFont().deriveFont(8f);


        /**
         * Create a <code>TileInfoPanel</code>.
         */
        public TileInfoPanel() {
            super(new MigLayout("fill, wrap " + (PRODUCTION+1) + ", gap 1 1"));

            setSize(260, 130);
            setOpaque(false);
        }


        /**
         * Updates this <code>InfoPanel</code>.
         *
         * @param tile The displayed tile (or null if none)
         */
        public void update(Tile tile) {
            this.tile = tile;

            removeAll();

            final ImageLibrary lib = getGUI().getTileImageLibrary();
            final Font font = FontLibrary.createFont(FontLibrary.FontType.NORMAL,
                FontLibrary.FontSize.TINY, lib.getScaleFactor());
            if (tile != null) {
                final int width = getWidth() - SLACK;
                BufferedImage image = getGUI().createTileImageWithBeachBorderAndItems(tile);
                if (tile.isExplored()) {
                    String text = Messages.message(tile.getLabel());
                    for (String s : splitText(text, " /",
                                              getFontMetrics(font), width)) {
                        JLabel label = new JLabel(s);
                        //itemLabel.setFont(font);
                        add(label, "span, align center");
                    }

                    add(new JLabel(new ImageIcon(image)), "spany");

                    final Player owner = tile.getOwner();
                    if (owner == null) {
                        add(new JLabel(), "span " + PRODUCTION);
                    } else {
                        StringTemplate t = owner.getNationLabel();
                        add(Utility.localizedLabel(t), "span " + PRODUCTION);
                    }

                    JLabel defenceLabel = Utility.localizedLabel(StringTemplate
                        .template("infoPanel.defenseBonus")
                        .addAmount("%bonus%", tile.getDefenceBonusPercentage()));
                    //defenceLabel.setFont(font);
                    add(defenceLabel, "span " + PRODUCTION);

                    JLabel moveLabel = Utility.localizedLabel(StringTemplate
                        .template("infoPanel.movementCost")
                        .addAmount("%cost%", tile.getType().getBasicMoveCost()/3));
                    //moveLabel.setFont(font);
                    add(moveLabel, "span " + PRODUCTION);

                    List<AbstractGoods> produce = tile.getType()
                        .getPossibleProduction(true);
                    if (produce.isEmpty()) {
                        add(new JLabel(), "span " + PRODUCTION);
                    } else {
                        Collections.sort(produce,
                            AbstractGoods.abstractGoodsComparator);
                        for (AbstractGoods ag : produce) {
                            GoodsType type = ag.getType();
                            int n = tile.getPotentialProduction(type, null);
                            JLabel label = new JLabel(String.valueOf(n),
                                new ImageIcon(lib.getSmallIconImage(type)),
                                JLabel.RIGHT);
                            label.setToolTipText(Messages.getName(type));
                            label.setFont(font);
                            add(label);
                        }
                    }
                } else {
                    add(Utility.localizedLabel("unexplored"),
                        "span, align center");
                    add(new JLabel(new ImageIcon(image)), "spany");
                }
            }
            revalidate();
            repaint();
        }

        /**
         * Gets the <code>Tile</code> in which this <code>InfoPanel</code>
         * is displaying information about.
         *
         * @return The <code>Tile</code> or <i>null</i> if no
         *         <code>Tile</code> applies.
         */
        public Tile getTile() {
            return tile;
        }
    }

    /**
     * Panel for displaying <code>Unit</code>-information.
     */
    public class UnitInfoPanel extends JPanel
        implements PropertyChangeListener {

        /** The unit to display. */
        private Unit unit;


        /**
         * Create a new unit information panel.
         */
        public UnitInfoPanel() {
            super(new MigLayout("wrap 5, fill, gap 0 0", "", ""));

            setSize(260, 130);
            setOpaque(false);
        }


        /**
         * Does this panel have a unit to display?
         *
         * @return True if this panel has a non-null unit.
         */
        public boolean hasUnit() {
            return this.unit != null;
        }

        /**
         * Updates this unit information panel to use a new unit.
         *
         * @param unit The displayed <code>Unit</code> (may be null).
         */
        public void update(Unit unit) {
            if (this.unit != unit) {
                if (this.unit != null) {
                    this.unit.removePropertyChangeListener(this);
                    GoodsContainer gc = this.unit.getGoodsContainer();
                    if (gc != null) gc.removePropertyChangeListener(this);
                }
                if (unit != null) {
                    unit.addPropertyChangeListener(this);
                    GoodsContainer gc = unit.getGoodsContainer();
                    if (gc != null) gc.addPropertyChangeListener(this);
                }
                logger.info("Switching UnitInfoPanel from " +
                    (this.unit == null ? "null" :
                        (this.unit.getId() + " " + this.unit.getDescription() +
                        " " + this.unit.getMovesAsString())) +
                     " to " +
                    (unit == null ? "null" :
                        (unit.getId() + " " + unit.getDescription() +
                        " " + unit.getMovesAsString())));
                this.unit = unit;
            }
            update();
        }

        /**
         * Unconditionally update this panel.
         */
        public void update() {
            removeAll();

            final ImageLibrary lib = getGUI().getTileImageLibrary();
            Font font = FontLibrary.createFont(FontLibrary.FontType.NORMAL,
                FontLibrary.FontSize.TINY, lib.getScaleFactor());
            String text;
            JLabel textLabel;
            if (unit != null) {
                ImageIcon ii = new ImageIcon(lib.getUnitImage(unit));
                JLabel imageLabel = new JLabel(ii);
                add(imageLabel, "spany, gapafter 5px");
                int width = getWidth() - ii.getIconWidth() - SLACK;
                text = unit.getDescription(Unit.UnitLabelType.FULL);
                for (String s : splitText(text, " /",
                                          getFontMetrics(font), width)) {
                    textLabel = new JLabel(s);
                    textLabel.setFont(font);
                    add(textLabel, "span 5");
                }

                text = (unit.isInEurope())
                    ? Messages.getName(unit.getOwner().getEurope())
                    : Messages.message("infoPanel.moves")
                        + " " + unit.getMovesAsString();
                textLabel = new JLabel(text);
                textLabel.setFont(font);
                add(textLabel, "span 5");

                if (unit.isCarrier()) {
                    ImageIcon icon;
                    JLabel label;
                    for (Goods goods : unit.getGoodsList()) {
                        int amount = goods.getAmount();
                        GoodsType gt = goods.getType();
                        // FIXME: Get size of full stack from appropriate place.
                        if(amount == 100) {
                            icon = new ImageIcon(lib.getIconImage(gt));
                            label = new JLabel(icon);
                        } else {
                            icon = new ImageIcon(lib.getSmallIconImage(gt));
                            label = new JLabel(String.valueOf(amount),
                                               icon, JLabel.RIGHT);
                        }
                        text = Messages.message(goods.getLabel(true));
                        label.setFont(font);
                        label.setToolTipText(text);
                        add(label);
                    }
                    for (Unit carriedUnit : unit.getUnitList()) {
                        icon = new ImageIcon(lib.getSmallerUnitImage(carriedUnit));
                        label = new JLabel(icon);
                        text = carriedUnit.getDescription(Unit.UnitLabelType.NATIONAL);
                        label.setFont(font);
                        label.setToolTipText(text);
                        add(label);
                    }
                }
            }
            revalidate();
        }


        // Interface PropertyChangeListener

        /**
         * {@inheritDoc}
         */
        @Override
        public void propertyChange(PropertyChangeEvent event) {
            update();
        }
    }

    private static enum InfoPanelMode {
        NONE, END, MAP, TILE, UNIT;
    }

    private static final int PANEL_WIDTH = 260;

    public static final int PANEL_HEIGHT = 130;

    private InfoPanelMode mode = InfoPanelMode.NONE;

    private final EndTurnPanel endTurnPanel;

    private final JPanel mapEditorPanel;

    private final TileInfoPanel tileInfoPanel;

    private final UnitInfoPanel unitInfoPanel;

    private final Image skin;


    /**
     * The constructor that will add the items to this panel.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public InfoPanel(final FreeColClient freeColClient) {
        this(freeColClient, true);
    }

    /**
     * The constructor that will add the items to this panel.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param useSkin Use the info panel skin.
     */
    public InfoPanel(final FreeColClient freeColClient, boolean useSkin) {
        super(freeColClient);

        this.endTurnPanel = new EndTurnPanel();
        this.mapEditorPanel = new JPanel(null);
        this.mapEditorPanel.setSize(130, 100);
        this.mapEditorPanel.setOpaque(false);
        this.tileInfoPanel = new TileInfoPanel();
        this.unitInfoPanel = new UnitInfoPanel();
        this.skin = (useSkin) ? ResourceManager.getImage("image.skin.InfoPanel")
            : null;

        setLayout(null);
        int internalPanelTop = 0;
        int internalPanelHeight = 128;
        if (this.skin != null) {
            setBorder(null);
            setSize(this.skin.getWidth(null), this.skin.getHeight(null));
            setOpaque(false);
            internalPanelTop = 75;
            internalPanelHeight = 128;
        } else {
            setSize(PANEL_WIDTH, PANEL_HEIGHT);
        }

        add(this.endTurnPanel, internalPanelTop, internalPanelHeight);
        add(this.mapEditorPanel, internalPanelTop, internalPanelHeight);
        add(this.tileInfoPanel, internalPanelTop, internalPanelHeight);
        add(this.unitInfoPanel, internalPanelTop, internalPanelHeight);

        addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    Unit activeUnit = getGUI().getActiveUnit();
                    if (activeUnit != null && activeUnit.hasTile()) {
                        getGUI().setFocus(activeUnit.getTile());
                    }
                }
            });
    }

    /**
     * Adds a panel to show information
     */
    private void add(JPanel panel, int internalTop, int internalHeight) {
        panel.setVisible(false);
        panel.setLocation((getWidth() - panel.getWidth()) / 2, internalTop
                + (internalHeight - panel.getHeight()) / 2);
        add(panel);
    }

    /**
     * Get the mode for this panel.
     *
     * @return The panel mode.
     */
    private InfoPanelMode getMode() {
        return (getFreeColClient().isMapEditor())
            ? InfoPanelMode.MAP
            : (getGUI().getViewMode() == GUI.VIEW_TERRAIN_MODE)
            ? InfoPanelMode.TILE
            : (unitInfoPanel.hasUnit())
            ? InfoPanelMode.UNIT
            : (getFreeColClient().getMyPlayer() == null)
            ? InfoPanelMode.NONE
            : InfoPanelMode.END;
    }

    /**
     * Updates this <code>InfoPanel</code>.
     *
     * @param mapTransform The current MapTransform.
     */
    public void update(MapTransform mapTransform) {
        final JPanel p = (mapTransform == null) ? null
            : mapTransform.getDescriptionPanel();
        if (p != null) {
            p.setOpaque(false);
            final Dimension d = p.getPreferredSize();
            p.setBounds(0, (this.mapEditorPanel.getHeight() - d.height)/2,
                this.mapEditorPanel.getWidth(), d.height);
            this.mapEditorPanel.removeAll();
            this.mapEditorPanel.add(p, BorderLayout.CENTER);
            this.mapEditorPanel.validate();
            this.mapEditorPanel.revalidate();
        }
        update();
    }

    /**
     * Updates this <code>InfoPanel</code>.
     *
     * @param tile The displayed tile (or null if none)
     */
    public void update(Tile tile) {
        if (this.tileInfoPanel.getTile() != tile) {
            this.tileInfoPanel.update(tile);
        }
        update();
    }

    /**
     * Updates this <code>InfoPanel</code>.
     *
     * @param unit The displayed unit (or null if none)
     */
    public void update(Unit unit) {
        this.unitInfoPanel.update(unit);
        update();
    }

    /**
     * Update this <code>InfoPanel</code> by selecting the correct internal
     * panel to display.
     */
    public void update() {
        InfoPanelMode newMode = getMode();
        if(newMode == InfoPanelMode.END &&
           getFreeColClient().getMyPlayer().hasNextActiveUnit()) {
            logger.warning("Inconsistent InfoPanel status");
        }
        if (this.mode != newMode) {
            logger.info("Switching InfoPanel mode from " + mode +
                        " to " + newMode);
            switch (this.mode = newMode) {
            case END:
                this.mapEditorPanel.setVisible(false);
                this.tileInfoPanel.setVisible(false);
                this.unitInfoPanel.setVisible(false);
                this.endTurnPanel.setVisible(true);
                break;
            case MAP:
                this.endTurnPanel.setVisible(false);
                this.tileInfoPanel.setVisible(false);
                this.unitInfoPanel.setVisible(false);
                this.mapEditorPanel.setVisible(true);
                break;
            case TILE:
                this.endTurnPanel.setVisible(false);
                this.mapEditorPanel.setVisible(false);
                this.unitInfoPanel.setVisible(false);
                this.tileInfoPanel.setVisible(true);
                break;
            case UNIT:
                this.endTurnPanel.setVisible(false);
                this.mapEditorPanel.setVisible(false);
                this.tileInfoPanel.setVisible(false);
                this.unitInfoPanel.setVisible(true);
                break;
            case NONE: default:
                this.endTurnPanel.setVisible(false);
                this.mapEditorPanel.setVisible(false);
                this.tileInfoPanel.setVisible(false);
                this.unitInfoPanel.setVisible(false);
                break;
            }
        }
    }


    // Override JComponent

    /**
     * {@inheritDoc}
     */
    @Override
    public void paintComponent(Graphics graphics) {
        if (this.skin != null) graphics.drawImage(this.skin, 0, 0, null);
        super.paintComponent(graphics);
    }
}
