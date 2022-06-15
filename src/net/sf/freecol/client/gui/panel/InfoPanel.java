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

package net.sf.freecol.client.gui.panel;

import static net.sf.freecol.common.util.CollectionUtils.sort;
import static net.sf.freecol.common.util.StringUtils.splitText;

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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.MapTransform;
import net.sf.freecol.client.gui.ImageLibrary;
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
import net.sf.freecol.common.resources.PropertyList;
import net.sf.freecol.common.resources.ResourceManager;


/**
 * The InfoPanel is a wrapper for several informative displays in the
 * lower right corner.
 */
public final class InfoPanel extends FreeColPanel
    implements PropertyChangeListener {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(InfoPanel.class.getName());


    private static enum InfoPanelMode {
        NONE, END, MAP, TILE, UNIT;
    }

    /** Pixel width of text area beside icon. */
    private static final int TEXT_WIDTH = 150;

    /** A small pixel gap. */
    private static final int SLACK = 5;

    /** Number of goods/production items to show. */
    private static final int PRODUCTION = 4;

    /** Preferred size for non-skinned panel. */
    public static final Dimension PREFERRED_SIZE = new Dimension(260, 130);
    
    /** The image library to use for the font. */
    private final ImageLibrary lib;

    /** The font for the end turn message. */
    private Font font;

    /** An optional background image (the standard one has shape). */
    private Image skin;

    /** The mouse listener for the various subpanels. */
    private final MouseAdapter mouseAdapter;
    
    /** The panel mode. */
    private InfoPanelMode mode = InfoPanelMode.NONE;

    /** The associated map transform when in MAP mode. */
    private MapTransform mapTransform = null;
    
    /** The associated tile when in TILE mode. */
    private Tile tile = null;
    
    /** The associated unit when in UNIT mode. */
    private Unit unit = null;
    
    /** Use the info panel skin. */
    private boolean useSkin;


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

        this.lib = freeColClient.getGUI().getFixedImageLibrary();
        this.useSkin = useSkin;
        
        // No layout manager!  Panels will be sized and placed explicitly

        this.mouseAdapter = new MouseAdapter() {
                /**
                 * {@inheritDoc}
                 */
                @Override
                public void mousePressed(MouseEvent e) {
                    Tile tile = InfoPanel.this.getTile();
                    if (tile != null) getGUI().setFocus(tile);
                }
            };
    }
    
    public void updateLayoutIfNeeded() {
        final Font newFont = this.lib.getScaledFont("normal-plain-tiny", null);
        final Image newSkin = (useSkin) ? this.lib.getScaledImage("image.skin.InfoPanel")
            : null;
        
        if (newFont == font && newSkin == skin) {
            // No change.
            return;
        }

        this.font = newFont;
        this.skin = newSkin;
        

        if (this.skin != null) {
            setBorder(null);
            setSize(this.skin.getWidth(null), this.skin.getHeight(null));
            // skin is output in overridden paintComponent(), which calls
            // its parent, which will display panels added here
            setOpaque(false);
        } else {
            setSize(this.lib.scale(PREFERRED_SIZE));
            setBorder(FreeColImageBorder.panelWithoutShadowBorder);
            setOpaque(true);
        }
    }

    /**
     * Get a new MigPanel with specified layout and size it to fit neatly.
     *
     * @param layout The {@code LayoutManager} for the panel.
     * @return The new {@code MigPanel}.
     */
    private MigPanel newPanel(LayoutManager layout) {
        MigPanel panel = new MigPanel(layout);
        panel.setSize(new Dimension((int)(this.getWidth() * 0.8),
                (int)(this.getHeight() * 0.6)));
        return panel;
    }
    
    /**
     * Size, place and request redraw of the given panel.
     *
     * @param panel The new panel to display.
     */
    private void setPanel(MigPanel panel) {
        panel.addMouseListener(this.mouseAdapter);        
        if (this.skin != null) {
            panel.setOpaque(false);
            final PropertyList pl = ResourceManager.getPropertyList("image.skin.InfoPanel.properties");
            panel.setLocation(lib.scaleInt(pl.getInt("panel.x")), lib.scaleInt(pl.getInt("panel.y")));
            panel.setSize(lib.scaleInt(pl.getInt("panel.width")), lib.scaleInt(pl.getInt("panel.height")));
        } else {
            final int y = (this.getHeight() - panel.getHeight()/2) / 2;
            final int x = (this.getWidth() - panel.getWidth()) / 2;
            panel.setLocation(x, y);
        }
 
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
        MigPanel panel = newPanel(new MigLayout("wrap 1, center",
                                                "[center]", ""));
        
        String labelString = Messages.message("infoPanel.endTurn");
        final int width = (int)(0.3 * this.getWidth());
        panel.add(new JLabel("")); // hack, one blank entry at top
        for (String s : splitText(labelString, " /",
                                  getFontMetrics(this.font), width)) {
            JLabel label = new JLabel(s);
            label.setFont(this.font);
            panel.add(label);
        }
        JButton button = new JButton(getFreeColClient().getActionManager()
            .getFreeColAction(EndTurnAction.id));
        button.setFont(this.font);
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
        MigPanel panel = newPanel(new BorderLayout());
        
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
        MigPanel panel = newPanel(new MigLayout("fill, wrap " + (PRODUCTION+1) + ", gap 1 1",
                "", ""));

        if (tile != null) {
            BufferedImage image = getGUI()
                .createTileImageWithBeachBorderAndItems(tile);
            if (tile.isExplored()) {
                final int width = panel.getWidth() - SLACK;
                String text = Messages.message(tile.getLabel());
                for (String s : splitText(text, " /",
                                          getFontMetrics(this.font), width)) {
                    JLabel label = new JLabel(s);
                    label.setFont(this.font);
                    panel.add(label, "span, align center");
                }
                panel.add(new JLabel(new ImageIcon(image)), "spany");
                final Player owner = tile.getOwner();
                if (owner == null) {
                    panel.add(new JLabel(), "span " + PRODUCTION);
                } else {
                    StringTemplate t = owner.getNationLabel();
                    JLabel label = Utility.localizedLabel(t);
                    label.setFont(this.font);
                    panel.add(label, "span " + PRODUCTION);
                }

                JLabel defenceLabel = Utility.localizedLabel(StringTemplate
                    .template("infoPanel.defenseBonus")
                    .addAmount("%bonus%", tile.getDefenceBonusPercentage()));
                defenceLabel.setFont(this.font);
                panel.add(defenceLabel, "span " + PRODUCTION);

                JLabel moveLabel = Utility.localizedLabel(StringTemplate
                    .template("infoPanel.movementCost")
                    .addAmount("%cost%", tile.getType().getBasicMoveCost()/3));
                moveLabel.setFont(this.font);
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
                        label.setFont(this.font);
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
     * Add labels to a panel with MigLayout, on one line.
     *
     * @param panel The {@code JPanel} to add to.
     * @param labels A list of {@code JLabel}s to add.
     * @param max The maximum number of labels to put on a line
     */
    private static void addLabels(JPanel panel, List<JLabel> labels, int max) {
        for (;;) {
            int n = Math.min(max, labels.size());
            if (n <= 0) {
                break;
            } else if (n == 1) {
                panel.add(labels.get(0));
                break;
            } else {
                panel.add(labels.remove(0), "split " + n);
                for (int i = 1; i < n; i++) panel.add(labels.remove(0));
            }            
        }
    }
    
    /**
     * Fill unit information into a new panel and add it.
     *
     * @param unit The {@code Unit} to display.
     * @return The {@code Unit}.
     */
    private Unit fillUnitPanel(Unit unit) {
        ImageIcon ii = new ImageIcon(lib.getScaledUnitImage(unit));
        final int width = ii.getIconWidth();
        // Two columns filling whole space with no gaps
        //   1. Icon of fixed width spanning full height
        //   2. Text/icon fields filling the remaining horizontal space
        MigPanel panel = newPanel(new MigLayout("wrap 2, fill, gap 0 0",
                                               "[" + width + "][fill]", ""));
        panel.add(new JLabel(ii), "spany, center");
        String text = unit.getDescription(Unit.UnitLabelType.FULL);
        JLabel textLabel;
        for (String s : splitText(text, " /", getFontMetrics(this.font),
                                  panel.getWidth() - width)) {
            textLabel = new JLabel(s);
            textLabel.setFont(this.font);
            panel.add(textLabel);
        }
        
        text = (unit.isInEurope())
            ? Messages.getName(unit.getOwner().getEurope())
            : Messages.message("infoPanel.moves")
            + " " + unit.getMovesAsString();
        textLabel = new JLabel(text);
        textLabel.setFont(this.font);
        panel.add(textLabel);
        
        if (unit.isCarrier()) {
            List<JLabel> labels = new ArrayList<>();
            ImageIcon icon;
            JLabel label;
            for (Unit carriedUnit : unit.getUnitList()) {
                icon = new ImageIcon(lib.getSmallerUnitImage(carriedUnit));
                label = new JLabel(icon);
                text = carriedUnit.getDescription(Unit.UnitLabelType.NATIONAL);
                label.setFont(this.font);
                label.setToolTipText(text);
                labels.add(label);
            }
            addLabels(panel, labels, 6); // 6 units fit well enough

            labels.clear();
            for (Goods goods : unit.getGoodsList()) {
                int amount = goods.getAmount();
                GoodsType gt = goods.getType();
                icon = new ImageIcon(lib.getSmallerGoodsTypeImage(gt));
                label = new JLabel(String.valueOf(amount), icon, JLabel.CENTER);
                text = Messages.message(goods.getLabel(true));
                label.setFont(this.font);
                label.setToolTipText(text);
                labels.add(label);
            }
            addLabels(panel, labels, 3); // goods icon+number is fits less well
        }
        panel.add(new JLabel(""), "growy"); // fill up remaining vertical space
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
        if (unit != this.unit) {
            // Only update the PCLs when the unit changes
            if (this.unit != null) {
                this.unit.removePropertyChangeListener(this);
                GoodsContainer gc = this.unit.getGoodsContainer();
                if (gc != null) gc.removePropertyChangeListener(this);
            }
            unit.addPropertyChangeListener(this);
            GoodsContainer gc = unit.getGoodsContainer();
            if (gc != null) gc.addPropertyChangeListener(this);
        }
        // Always call fillUnitPanel because while the unit may not
        // change, its annotations (such as moves left) might
        this.unit = fillUnitPanel(unit);
        updated = true;
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
