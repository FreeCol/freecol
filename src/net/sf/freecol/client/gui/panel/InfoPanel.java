/**
 *  Copyright (C) 2002-2014   The FreeCol Team
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
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.action.EndTurnAction;
import net.sf.freecol.client.gui.panel.MapEditorTransformPanel.MapTransform;
import net.sf.freecol.client.gui.panel.MigPanel;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.resources.ResourceManager;


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

    /**
     * Panel for ending the turn.
     */
    public class EndTurnPanel extends MigPanel {

        public EndTurnPanel(GUI gui) {
            super(new MigLayout("wrap 1, center", "[center]", ""));

            String labelString = Messages.message("infoPanel.endTurnPanel.text");
            int width = getFontMetrics(getFont()).stringWidth(labelString);
            if (width > 150 ) {
                int index = Messages.getBreakingPoint(labelString);
                if (index > 0) {
                    add(new JLabel(labelString.substring(0, index)));
                    add(new JLabel(labelString.substring(index + 1)));
                } else {
                    add(new JLabel(labelString));
                }
            } else {
                add(new JLabel(labelString));
            }

            add(new JButton(getFreeColClient().getActionManager()
                    .getFreeColAction(EndTurnAction.id)));
            setOpaque(false);
            setSize(getPreferredSize());
        }
    }

    /**
     * Panel for displaying <code>Tile</code>-information.
     */
    public class TileInfoPanel extends MigPanel {

        private Tile tile;
        private Font font = new JLabel().getFont().deriveFont(9f);


        /**
         * Create a <code>TileInfoPanel</code>.
         */
        public TileInfoPanel() {
            super(new MigLayout("fill, wrap 5, gap 2 2"));

            setSize(226, 128);
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

            if (tile != null) {
                final Image terrain = getLibrary().getTerrainImage(tile.getType(),
                    tile.getX(), tile.getY());
                int width = terrain.getWidth(null);
                int height = terrain.getHeight(null);
                int compoundHeight = getLibrary().getCompoundTerrainImageHeight(tile.getType());
                BufferedImage image = new BufferedImage(width, compoundHeight, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = image.createGraphics();
                g.translate(0, compoundHeight - height);
                getGUI().getMapViewer().displayTerrain(g, tile);
                if (tile.isExplored()) {
                    StringTemplate items = StringTemplate.label(", ");
                    items.add(tile.getNameKey());
                    for (TileImprovement tileImprovement : tile.getCompletedTileImprovements()) {
                        items.add(tileImprovement.getType().getDescriptionKey());
                    }
                    add(GUI.localizedLabel(items), "span, align center");

                    add(new JLabel(new ImageIcon(image)), "spany");
                    if (tile.getOwner() != null) {
                        JLabel ownerLabel = GUI.localizedLabel(tile.getOwner().getNationName());
                        ownerLabel.setFont(font);
                        add(ownerLabel, "span 4");
                    }

                    int defenceBonus = (int)tile.getType().applyModifiers(100f,
                        getGame().getTurn(), Modifier.DEFENCE) - 100;
                    JLabel defenceLabel = GUI.localizedLabel(StringTemplate
                        .template("infoPanel.defenseBonus")
                        .addAmount("%bonus%", defenceBonus));
                    defenceLabel.setFont(font);
                    add(defenceLabel, "span 4");
                    JLabel moveLabel = GUI.localizedLabel(StringTemplate
                        .template("infoPanel.movementCost")
                        .addAmount("%cost%", tile.getType().getBasicMoveCost()/3));
                    moveLabel.setFont(font);
                    add(moveLabel, "span 4");

                    for (AbstractGoods goods : tile.getType().getPossibleProduction(true)) {
                        JLabel goodsLabel = new JLabel(String.valueOf(tile.getPotentialProduction(goods.getType(), null)),
                            getLibrary().getScaledGoodsImageIcon(goods.getType(), 0.50f),
                            JLabel.RIGHT);
                        goodsLabel.setToolTipText(Messages.getName(goods.getType()));
                        goodsLabel.setFont(font);
                        add(goodsLabel);
                    }
                } else {
                    add(GUI.localizedLabel("unexplored"), "span, align center");
                    add(new JLabel(new ImageIcon(image)), "spany");
                }
                revalidate();
            }
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
            super(new MigLayout("wrap 6, fill, gap 0 0", "", ""));

            setSize(226, 100);
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
                }
                if (unit != null) {
                    unit.addPropertyChangeListener(this);
                }
                this.unit = unit;
                update();
            }
        }

        /**
         * Unconditionally update this panel.
         */
        public void update() {
            removeAll();

            final ImageLibrary lib = getLibrary();
            if (unit != null) {
                add(new JLabel(lib.getUnitImageIcon(unit)),
                    "spany, gapafter 5px");
                String name = unit.getDescription(Unit.UnitLabelType.NATIONAL);

                // FIXME: this is too brittle!
                int index = name.indexOf(" (");
                if (index < 0) {
                    add(new JLabel(name), "span");
                } else {
                    add(new JLabel(name.substring(0, index)), "span");
                    add(new JLabel(name.substring(index + 1)), "span");
                }

                String text = (unit.isInEurope())
                    ? Messages.getName(unit.getOwner().getEurope())
                    : Messages.message("moves") +" "+ unit.getMovesAsString();
                add(new JLabel(text), "span");

                if (unit.isCarrier()) {
                    ImageIcon icon;
                    JLabel label;
                    for (Goods goods : unit.getGoodsList()) {
                        icon = lib.getScaledGoodsImageIcon(goods.getType(), 0.66f);
                        label = new JLabel(icon);
                        text = Messages.message(goods.getLabel(true));
                        label.setToolTipText(text);
                        add(label);
                    }
                    for (Unit carriedUnit : unit.getUnitList()) {
                        icon = lib.getUnitImageIcon(carriedUnit, 0.5);
                        label = new JLabel(icon);
                        text = carriedUnit.getDescription(Unit.UnitLabelType.NATIONAL);
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
        public void propertyChange(PropertyChangeEvent event) {
            update();
        }
    }

    private static enum InfoPanelMode {
        NONE, END, MAP, TILE, UNIT;
    }

    private static final int PANEL_WIDTH = 256;

    public static final int PANEL_HEIGHT = 128;

    private final Player player;

    private InfoPanelMode mode = InfoPanelMode.NONE;

    private final EndTurnPanel endTurnPanel;

    private final JPanel mapEditorPanel;

    private final TileInfoPanel tileInfoPanel;

    private final UnitInfoPanel unitInfoPanel;

    private Image skin;


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

        this.player = freeColClient.getMyPlayer();
        this.endTurnPanel = new EndTurnPanel(getGUI());
        this.mapEditorPanel = new JPanel(null);
        this.mapEditorPanel.setSize(130, 100);
        this.mapEditorPanel.setOpaque(false);
        this.tileInfoPanel = new TileInfoPanel();
        this.unitInfoPanel = new UnitInfoPanel();
        this.skin = (useSkin) ? ResourceManager.getImage("InfoPanel.skin")
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
        Player player;
        return (getFreeColClient().isMapEditor())
            ? InfoPanelMode.MAP
            : (getGUI().getViewMode() == GUI.VIEW_TERRAIN_MODE)
            ? InfoPanelMode.TILE
            : (unitInfoPanel.hasUnit())
            ? InfoPanelMode.UNIT
            : ((player = getFreeColClient().getMyPlayer()) != null
                && !player.hasNextActiveUnit())
            ? InfoPanelMode.END
            : InfoPanelMode.NONE;
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
        if (this.mode != newMode) {
            switch (this.mode = newMode) {
            case END:
                this.endTurnPanel.setVisible(true);
                this.mapEditorPanel.setVisible(false);
                this.tileInfoPanel.setVisible(false);
                this.unitInfoPanel.setVisible(false);
                break;
            case MAP:
                this.mapEditorPanel.setVisible(true);
                this.endTurnPanel.setVisible(false);
                this.tileInfoPanel.setVisible(false);
                this.unitInfoPanel.setVisible(false);
                break;
            case TILE:
                this.tileInfoPanel.setVisible(true);
                this.endTurnPanel.setVisible(false);
                this.mapEditorPanel.setVisible(false);
                this.unitInfoPanel.setVisible(false);
                break;
            case UNIT:
                this.unitInfoPanel.setVisible(true);
                this.endTurnPanel.setVisible(false);
                this.mapEditorPanel.setVisible(false);
                this.tileInfoPanel.setVisible(false);
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
