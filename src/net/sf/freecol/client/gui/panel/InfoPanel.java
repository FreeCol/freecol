/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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
import java.util.List;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.MapTransform;
import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.client.gui.GUI;
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
import static net.sf.freecol.common.util.CollectionUtils.*;
import static net.sf.freecol.common.util.StringUtils.*;


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

    private static enum InfoPanelMode {
        NONE, END, MAP, TILE, UNIT;
    }

    private static final int PANEL_WIDTH = 260;

    public static final int PANEL_HEIGHT = 130;

    private InfoPanelMode mode = InfoPanelMode.NONE;

    private final EndTurnInfoPanel endTurnInfoPanel;

    private final JPanel mapEditorInfoPanel;

    private final TileInfoPanel tileInfoPanel;

    private final UnitInfoPanel unitInfoPanel;

    private final Image skin;


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
        super(freeColClient);

        this.endTurnInfoPanel = new EndTurnInfoPanel(freeColClient);
        this.mapEditorInfoPanel = new JPanel(null);
        this.mapEditorInfoPanel.setSize(130, 100);
        this.mapEditorInfoPanel.setOpaque(false);
        this.tileInfoPanel = new TileInfoPanel(freeColClient);
        this.unitInfoPanel = new UnitInfoPanel(freeColClient);
        this.skin = (!useSkin) ? null
            : ImageLibrary.getUnscaledImage("image.skin.InfoPanel");

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

        add(this.endTurnInfoPanel, internalPanelTop, internalPanelHeight);
        add(this.mapEditorInfoPanel, internalPanelTop, internalPanelHeight);
        add(this.tileInfoPanel, internalPanelTop, internalPanelHeight);
        add(this.unitInfoPanel, internalPanelTop, internalPanelHeight);

        addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (mode == InfoPanelMode.TILE) {
                        getGUI().setFocus(InfoPanel.this.tileInfoPanel.getTile());
                    } else if (mode == InfoPanelMode.UNIT) {
                        getGUI().setFocus(InfoPanel.this.unitInfoPanel.getTile());
                    }
                }
            });
    }

    /**
     * Adds a panel to show information
     *
     * @param panel The panel to add.
     * @param internalTop The top position.
     * @param internalHeight The enclosing height.
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
            : (getGUI().getViewMode() == GUI.ViewMode.TERRAIN)
            ? InfoPanelMode.TILE
            : (getGUI().getViewMode() == GUI.ViewMode.MOVE_UNITS)
            ? InfoPanelMode.UNIT
            : (getFreeColClient().getMyPlayer() == null)
            ? InfoPanelMode.NONE
            : InfoPanelMode.END;
    }

    /**
     * Updates this {@code InfoPanel}.
     *
     * @param mapTransform The current MapTransform.
     */
    public void update(MapTransform mapTransform) {
        final JPanel p = (mapTransform == null) ? null
            : mapTransform.getDescriptionPanel();
        if (p != null) {
            p.setOpaque(false);
            final Dimension d = p.getPreferredSize();
            p.setBounds(0, (this.mapEditorInfoPanel.getHeight() - d.height)/2,
                this.mapEditorInfoPanel.getWidth(), d.height);
            this.mapEditorInfoPanel.removeAll();
            this.mapEditorInfoPanel.add(p, BorderLayout.CENTER);
            this.mapEditorInfoPanel.revalidate();
            this.mapEditorInfoPanel.repaint();
        }
        InfoPanelMode oldMode = update();
        logger.info("InfoPanel " + oldMode + " -> " + this.mode
            + " with " + mapTransform);
    }

    /**
     * Updates this {@code InfoPanel}.
     *
     * @param tile The displayed tile (or null if none)
     */
    public void update(Tile tile) {
        this.tileInfoPanel.update(tile);
        InfoPanelMode oldMode = update();
        logger.info("InfoPanel " + oldMode + " -> " + this.mode
            + " with tile " + tile);
    }

    /**
     * Updates this {@code InfoPanel}.
     *
     * @param unit The displayed unit (or null if none)
     */
    public void update(Unit unit) {
        this.unitInfoPanel.update(unit);
        InfoPanelMode oldMode = update();
        logger.info("InfoPanel " + oldMode + " -> " + this.mode
            + " with unit " + unit);
    }

    /**
     * Update this {@code InfoPanel} by selecting the correct internal
     * panel to display.
     *
     * @return The old <code>InfoPanelMode</code>.
     */
    private InfoPanelMode update() {
        InfoPanelMode oldMode = this.mode;
        InfoPanelMode newMode = getMode();
        if (oldMode != newMode) {
            switch (this.mode = newMode) {
            case END:
                this.mapEditorInfoPanel.setVisible(false);
                this.tileInfoPanel.setVisible(false);
                this.unitInfoPanel.setVisible(false);
                this.endTurnInfoPanel.setVisible(true);
                break;
            case MAP:
                this.endTurnInfoPanel.setVisible(false);
                this.tileInfoPanel.setVisible(false);
                this.unitInfoPanel.setVisible(false);
                this.mapEditorInfoPanel.setVisible(true);
                break;
            case TILE:
                this.endTurnInfoPanel.setVisible(false);
                this.mapEditorInfoPanel.setVisible(false);
                this.unitInfoPanel.setVisible(false);
                this.tileInfoPanel.setVisible(true);
                break;
            case UNIT:
                this.endTurnInfoPanel.setVisible(false);
                this.mapEditorInfoPanel.setVisible(false);
                this.tileInfoPanel.setVisible(false);
                this.unitInfoPanel.setVisible(true);
                break;
            case NONE: default:
                this.endTurnInfoPanel.setVisible(false);
                this.mapEditorInfoPanel.setVisible(false);
                this.tileInfoPanel.setVisible(false);
                this.unitInfoPanel.setVisible(false);
                break;
            }
        }
        revalidate();
        return oldMode;
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
