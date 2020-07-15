/**
 *  Copyright (C) 2002-2020   The FreeCol Team
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
import java.awt.LayoutManager;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.MapTransform;
import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.panel.MigPanel;
import net.sf.freecol.client.gui.action.EndTurnAction;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import static net.sf.freecol.common.util.CollectionUtils.*;
import static net.sf.freecol.common.util.StringUtils.*;


/**
 * The InfoPanel is a wrapper for several informative displays in the
 * lower right corner.
 */
public final class InfoPanel extends FreeColPanel
    implements PropertyChangeListener {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(InfoPanel.class.getName());

    private static final int SLACK = 5; // Small gap
    private static final int PRODUCTION = 4;

    private static enum InfoPanelMode {
        NONE, END, MAP, TILE, UNIT;
    }

    /** Preferred size for non-skinned panel. */
    public static final Dimension PREFERRED_SIZE = new Dimension(260, 130);
    
    /** The image library to use for the font. */
    private final ImageLibrary lib;

    /** The font for the end turn message. */
    private final Font font;

    /** An optional background image (the standard one has shape). */
    private final Image skin;

    /** Placement of the internal panel. */
    private int internalTop = 0, internalHeight = 128;
    
    /** The panel mode. */
    private InfoPanelMode mode = InfoPanelMode.NONE;

    /** The associated map transform when in MAP mode. */
    private MapTransform mapTransform = null;
    
    /** The associated tile when in TILE mode. */
    private Tile tile = null;
    
    /** The associated unit when in UNIT mode. */
    private Unit unit = null;


    /**
     * The constructor that will add the items to this panel.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public InfoPanel(final FreeColClient freeColClient) {
        this(freeColClient, true);
    }

    /**
     * The constructor that will add the items to this panel.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param useSkin Use the info panel skin.
     */
    public InfoPanel(final FreeColClient freeColClient, boolean useSkin) {
        super(freeColClient, null, null);

        this.lib = freeColClient.getGUI().getTileImageLibrary();
        this.font = FontLibrary.createFont(FontLibrary.FontType.NORMAL,
            FontLibrary.FontSize.TINY, lib.getScaleFactor());
        this.skin = (!useSkin) ? null
            : ImageLibrary.getUnscaledImage("image.skin.InfoPanel");

        if (this.skin != null) {
            setBorder(null);
            setSize(this.skin.getWidth(null), this.skin.getHeight(null));
            // skin is output in overridden paintComponent which calls
            // its parent which will display panels added here
            setOpaque(false);
            this.internalTop = 75;
            this.internalHeight = 128;
        } else {
            setSize(PREFERRED_SIZE);
        }
        // No layout manager!  Panels will be sized and placed explicitly

        addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    Tile tile = InfoPanel.this.getTile();
                    if (tile != null) getGUI().setFocus(tile);
                }
            });
    }

    /**
     * Size, place and request redraw of the given panel.
     *
     * @param panel The new panel to display.
     */
    private void setPanel(MigPanel panel) {
        panel.setSize(new Dimension((int)(this.getWidth() * 0.75),
                (int)(this.getHeight() * 0.6)));
        panel.setLocation((this.getWidth() - panel.getWidth()) / 2,
            internalTop + (internalHeight - panel.getHeight()) / 2
            //(this.getHeight() - panel.getHeight()) / 2
            );
        if (this.skin != null) panel.setOpaque(false);
        this.removeAll();
        this.add(panel);
        this.revalidate();
        this.repaint();
    }
    
    /**
     * Get the mode-dependent associated tile.
     *
     * @return The {@code Tile} associated with this panel.
     */
    private Tile getTile() {
        switch (this.mode) {
        case TILE:
            return this.tile;
        case UNIT:
            return (this.unit == null) ? null : this.unit.getTile();
        default:
            break;
        }
        return null;
    }

    /**
     * Change the panel mode.
     *
     * The important job here is to clear out all the old settings.
     *
     * @param newMode The new {@code InfoPanelMode}.
     * @return The old {@code InfoPanelMode}.
     */
    private InfoPanelMode changeMode(InfoPanelMode newMode) {
        InfoPanelMode oldMode = this.mode;
        if (oldMode != newMode) {
            switch (oldMode) {
            case MAP:
                this.mapTransform = null;
                break;
            case TILE:
                this.tile = null;
                break;
            case UNIT:
                this.unit.removePropertyChangeListener(this);
                GoodsContainer gc = this.unit.getGoodsContainer();
                if (gc != null) gc.removePropertyChangeListener(this);
                this.unit = null;
                break;
            default:
                break;
            }
            this.mode = newMode;
        }
        return oldMode;
    }

    /**
     * Fill in an end turn message into a new panel and add it.
     */
    private void fillEndPanel() {
        MigPanel panel = new MigPanel(new MigLayout("wrap 1, center", "[center]", ""));
        
        String labelString = Messages.message("infoPanel.endTurn");
        for (String s : splitText(labelString, " /",
                                  getFontMetrics(font), 150)) {
            JLabel label = new JLabel(s);
            label.setFont(font);
            panel.add(label);
        }
        JButton button = new JButton(getFreeColClient().getActionManager()
            .getFreeColAction(EndTurnAction.id));
        button.setFont(font);
        panel.add(button);

        setPanel(panel);
    }
    
    /**
     * Fill map transform information into a new panel and add it.
     *
     * @param mapTransform The {@code MapTransform} to display.
     * @return The {@code MapTransform}.
     */
    private MapTransform fillMapPanel(MapTransform mapTransform) {
        MigPanel panel = new MigPanel(new BorderLayout());
        
        final JPanel p = (mapTransform == null) ? null
            : mapTransform.getDescriptionPanel();
        if (p != null) {
            p.setOpaque(false);
            final Dimension d = p.getPreferredSize();
            p.setBounds(0, (this.getHeight() - d.height)/2,
                        this.getWidth(), d.height);
            panel.add(p, BorderLayout.CENTER);
        }

        setPanel(panel);
        return mapTransform;
    }

    /**
     * Fill tile information into a new panel and add it.
     *
     * @param tile The {@code Tile} to display.
     * @return The {@code Tile}.
     */
    private Tile fillTilePanel(Tile tile) {
        MigPanel panel = new MigPanel(new MigLayout("fill, wrap " + (PRODUCTION+1) + ", gap 1 1",
                "", ""));

        if (tile != null) {
            BufferedImage image = getGUI()
                .createTileImageWithBeachBorderAndItems(tile);
            if (tile.isExplored()) {
                final int width = panel.getWidth() - SLACK;
                String text = Messages.message(tile.getLabel());
                for (String s : splitText(text, " /",
                                          getFontMetrics(font), width)) {
                    JLabel label = new JLabel(s);
                    //itemLabel.setFont(font);
                    panel.add(label, "span, align center");
                }
                panel.add(new JLabel(new ImageIcon(image)), "spany");
                final Player owner = tile.getOwner();
                if (owner == null) {
                    panel.add(new JLabel(), "span " + PRODUCTION);
                } else {
                    StringTemplate t = owner.getNationLabel();
                    panel.add(Utility.localizedLabel(t), "span " + PRODUCTION);
                }

                JLabel defenceLabel = Utility.localizedLabel(StringTemplate
                    .template("infoPanel.defenseBonus")
                    .addAmount("%bonus%", tile.getDefenceBonusPercentage()));
                //defenceLabel.setFont(font);
                panel.add(defenceLabel, "span " + PRODUCTION);

                JLabel moveLabel = Utility.localizedLabel(StringTemplate
                    .template("infoPanel.movementCost")
                    .addAmount("%cost%", tile.getType().getBasicMoveCost()/3));
                //moveLabel.setFont(font);
                add(moveLabel, "span " + PRODUCTION);

                List<AbstractGoods> produce
                    = sort(tile.getType().getPossibleProduction(true),
                           AbstractGoods.descendingAmountComparator);
                if (produce.isEmpty()) {
                    panel.add(new JLabel(), "span " + PRODUCTION);
                } else {
                    for (AbstractGoods ag : produce) {
                        GoodsType type = ag.getType();
                        int n = tile.getPotentialProduction(type, null);
                        JLabel label = new JLabel(String.valueOf(n),
                            new ImageIcon(lib.getSmallGoodsTypeImage(type)),
                            JLabel.RIGHT);
                        label.setToolTipText(Messages.getName(type));
                        label.setFont(font);
                        panel.add(label);
                    }
                }
            } else {
                panel.add(Utility.localizedLabel("unexplored"),
                    "span, align center");
                panel.add(new JLabel(new ImageIcon(image)), "spany");
            }
        }

        setPanel(panel);
        return tile;
    }

    /**
     * Fill unit information into a new panel and add it.
     *
     * @param unit The {@code Unit} to display.
     * @return The {@code Unit}.
     */
    private Unit fillUnitPanel(Unit unit) {
        MigPanel panel = new MigPanel(new MigLayout("wrap 5, fill, gap 0 0", "", ""));

        if (unit != null) {
            String text;
            JLabel textLabel;
            ImageIcon ii = new ImageIcon(lib.getScaledUnitImage(unit));
            JLabel imageLabel = new JLabel(ii);
            panel.add(imageLabel, "spany, gapafter 5px");
            int width = panel.getWidth() - ii.getIconWidth() - SLACK;
            text = unit.getDescription(Unit.UnitLabelType.FULL);
            for (String s : splitText(text, " /",
                                      getFontMetrics(font), width)) {
                textLabel = new JLabel(s);
                textLabel.setFont(font);
                panel.add(textLabel, "span 5");
            }

            text = (unit.isInEurope())
                ? Messages.getName(unit.getOwner().getEurope())
                : Messages.message("infoPanel.moves")
                    + " " + unit.getMovesAsString();
            textLabel = new JLabel(text);
            textLabel.setFont(font);
            panel.add(textLabel, "span 5");

            if (unit.isCarrier()) {
                ImageIcon icon;
                JLabel label;
                for (Goods goods : unit.getGoodsList()) {
                    int amount = goods.getAmount();
                    GoodsType gt = goods.getType();
                    // FIXME: Get size of full stack from appropriate place.
                    if (amount == GoodsContainer.CARGO_SIZE) {
                        icon = new ImageIcon(lib.getScaledGoodsTypeImage(gt));
                        label = new JLabel(icon);
                    } else {
                        icon = new ImageIcon(lib.getSmallGoodsTypeImage(gt));
                        label = new JLabel(String.valueOf(amount),
                                           icon, JLabel.RIGHT);
                    }
                    text = Messages.message(goods.getLabel(true));
                    label.setFont(font);
                    label.setToolTipText(text);
                    panel.add(label);
                }
                for (Unit carriedUnit : unit.getUnitList()) {
                    icon = new ImageIcon(lib.getSmallerUnitImage(carriedUnit));
                    label = new JLabel(icon);
                    text = carriedUnit.getDescription(Unit.UnitLabelType.NATIONAL);
                    label.setFont(font);
                    label.setToolTipText(text);
                    panel.add(label);
                }
            }
        }

        setPanel(panel);
        return unit;
    }

    /**
     * Update this {@code InfoPanel} to end turn mode.
     */
    public void update() {
        boolean updated = false;
        InfoPanelMode oldMode = changeMode(InfoPanelMode.END);
        if (oldMode != InfoPanelMode.END) {
            fillEndPanel();
            updated = true;
        }
        logger.info("InfoPanel " + ((updated) ? "updated " : "maintained ")
            + oldMode + " -> " + this.mode);
    }
        
    /**
     * Update this {@code InfoPanel} to map mode with a given transform.
     *
     * @param mapTransform The {@code MapTransform} to display.
     */
    public void update(MapTransform mapTransform) {
        boolean updated = false;
        InfoPanelMode oldMode = changeMode(InfoPanelMode.MAP);
        if (oldMode != InfoPanelMode.MAP || mapTransform != this.mapTransform) {
            this.mapTransform = fillMapPanel(mapTransform);
            updated = true;
        }
        logger.info("InfoPanel " + ((updated) ? "updated " : "maintained ")
            + oldMode + " -> " + this.mode + " with " + mapTransform);
    }

    /**
     * Update this {@code InfoPanel} to tile mode with a given tile.
     *
     * @param tile The displayed {@code Tile}.
     */
    public void update(Tile tile) {
        boolean updated = false;
        InfoPanelMode oldMode = changeMode(InfoPanelMode.TILE);
        if (oldMode != InfoPanelMode.TILE || tile != this.tile) {
            this.tile = fillTilePanel(tile);
            updated = true;
        }
        logger.info("InfoPanel " + ((updated) ? "updated " : "maintained ")
            + oldMode + " -> " + this.mode + " with tile " + tile);
    }

    /**
     * Update this {@code InfoPanel} to unit mode with a given unit.
     *
     * @param unit The displayed {@code Unit}.
     */
    public void update(Unit unit) {
        // Switch to end turn display if no active unit
        if (unit == null) {
            update();
            return;
        }
        
        boolean updated = false;
        InfoPanelMode oldMode = changeMode(InfoPanelMode.UNIT);
        if (oldMode != InfoPanelMode.UNIT || unit != this.unit) {
            unit.addPropertyChangeListener(this);
            GoodsContainer gc = unit.getGoodsContainer();
            if (gc != null) gc.addPropertyChangeListener(this);
            this.unit = fillUnitPanel(unit);
            updated = true;
        }
        logger.info("InfoPanel " + ((updated) ? "updated " : "maintained ")
            + oldMode + " -> " + this.mode + " with unit " + unit);
    }

    /**
     * Refresh this panel.
     *
     * Apparently this is necessary when adding the info panel back into the
     * canvas with the skinned corner, otherwise the unit does not get
     * displayed.
     * TODO: Explain why, or fix so we do not need this.
     */
    public void refresh() {
        switch (this.mode) {
        case END:
            fillEndPanel();
            break;
        case MAP:
            fillMapPanel(this.mapTransform);
            break;
        case TILE:
            fillTilePanel(this.tile);
            break;
        case UNIT:
            fillUnitPanel(this.unit);
            break;
        default:
            break;
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


    // Interface PropertyChangeListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void propertyChange(PropertyChangeEvent event) {
        refresh();
    }
}
