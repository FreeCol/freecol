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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.MapEditorController;
import net.sf.freecol.client.control.MapTransform;
import net.sf.freecol.client.gui.ChoiceItem;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.dialog.RiverStyleDialog;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.IndianNationType;
import net.sf.freecol.common.model.LostCityRumour;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Resource;
import net.sf.freecol.common.model.ResourceType;
import net.sf.freecol.common.model.SettlementType;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.UnitType;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.server.model.ServerIndianSettlement;


/**
 * A panel for choosing the current {@code MapTransform}.
 *
 * This panel is only used when running in
 * {@link net.sf.freecol.client.FreeColClient#isMapEditor() map editor mode}.
 *
 * @see MapEditorController#getMapTransform()
 * @see MapTransform
 */
public final class MapEditorTransformPanel extends FreeColPanel {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(MapEditorTransformPanel.class.getName());

    private static final class RiverTransform extends MapTransform {
        private final int magnitude;

        private RiverTransform(int magnitude) {
            this.magnitude = magnitude;
        }

        @Override
        public void transform(Tile tile) {
            TileImprovementType riverType =
                tile.getSpecification().getTileImprovementType("model.improvement.river");

            if (riverType.isTileTypeAllowed(tile.getType())) {
                if (!tile.hasRiver()) {
                    StringBuilder sb = new StringBuilder(64);
                    for (Direction direction : Direction.longSides) {
                        Tile t = tile.getNeighbourOrNull(direction);
                        TileImprovement otherRiver = (t == null) ? null
                            : t.getRiver();
                        if (t == null || (t.isLand() && otherRiver == null)) {
                            sb.append('0');
                        } else {
                            sb.append(magnitude);
                        }
                    }
                    tile.addRiver(magnitude, sb.toString());
                } else {
                    tile.removeRiver();
                }
            }
        }
    }

    private static final class RiverStyleTransform extends MapTransform {

        public static final int CHANGE_CONNECTIONS = 0;
        public static final int SET_STYLE = 1;

        private String style;
        private int type;

        private RiverStyleTransform(int type) {
            this.style = null;
            this.type = type;
        }

        public void setStyle(String style) {
            this.style = style;
        }

        public int getType() {
            return type;
        }

        @Override
        public void transform(Tile tile) {
            TileImprovement river = tile.getRiver();

            if (river != null) {
                if (type == CHANGE_CONNECTIONS)
                    river.updateRiverConnections(style);
                else
                    river.setRiverStyle(style);
            }
        }
    }

    /**
     * Adds, removes or cycles through the available resources for
     * this Tile.  Cycles through the ResourceTypeList and picks the
     * next valid, or removes if end of list.
     */
    private class ResourceTransform extends MapTransform {

        @Override
        public void transform(Tile t) {
            // Check if there is a resource already
            Resource resource = null;
            if (t.getTileItemContainer() != null) {
                resource = t.getTileItemContainer().getResource();
            }
            if (resource != null) {
                t.getTileItemContainer().removeTileItem(resource);
            } else {
                List<ResourceType> resList = t.getType().getResourceTypeValues();
                switch (resList.size()) {
                case 0:
                    return;
                case 1:
                    ResourceType resourceType = first(resList);
                    // FIXME: create GUI for setting the quantity
                    t.addResource(new Resource(t.getGame(), t, resourceType,
                                  resourceType.getMaxValue()));
                    return;
                default:
                    ResourceType choice = getResourceChoice(resList);
                    if (choice != null) {
                        t.addResource(new Resource(t.getGame(), t, choice,
                                      choice.getMaxValue()));
                    }
                }
            }
        }
    }

    private static class LostCityRumourTransform extends MapTransform {
        @Override
        public void transform(Tile t) {
            if (t.isLand()) {
                LostCityRumour rumour = t.getLostCityRumour();
                if (rumour == null) {
                    t.addLostCityRumour(new LostCityRumour(t.getGame(), t));
                } else {
                    t.removeLostCityRumour();
                }
            }
        }
    }

    private class SettlementTransform extends MapTransform {
        @Override
        public void transform(Tile t) {
            if (!t.isLand() || t.hasSettlement()) return;
            UnitType skill = first(((IndianNationType)getNativeNation().getType())
                .getSkills()).getObject();
            Player nativePlayer = getGame().getPlayerByNation(getNativeNation());
            if (nativePlayer == null) return;
            String name = nativePlayer.getSettlementName(null);
            ServerIndianSettlement sis
                = new ServerIndianSettlement(t.getGame(),
                    nativePlayer, name, t, false, skill, null);
            nativePlayer.addSettlement(sis);
            sis.placeSettlement(true);
            sis.addUnits(null);
            logger.info("Add settlement " + sis.getName() + " to tile " + t);
        }
    }

    /** A native nation to use for native settlement type and skill. */
    private static Nation nativeNation;

    private final JPanel listPanel;
    private JToggleButton settlementButton;
    private final ButtonGroup group;


    /**
     * Creates a panel to choose a map transform.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public MapEditorTransformPanel(FreeColClient freeColClient) {
        super(freeColClient, null, new BorderLayout());

        listPanel = new JPanel(new GridLayout(2, 0));

        group = new ButtonGroup();
        //Add an invisible, move button to de-select all others
        group.add(new JToggleButton());
        buildList();

        JScrollPane sl = new JScrollPane(listPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sl.getViewport().setOpaque(false);
        add(sl);
    }


    /**
     * Get the last used native nation.
     *
     * @return A suitable {@code Nation}.
     */     
    private Nation getNativeNation() {
        if (nativeNation == null) { // Initialize if not yet set
            nativeNation = first(getSpecification().getIndianNations());
        }
        return nativeNation;
    }

    /**
     * Set the native nation.
     *
     * @param newNativeNation The new native {@code Nation}.
     */
    public static void setNativeNation(Nation newNativeNation) {
        nativeNation = newNativeNation;
    }
    
    /**
     * Builds the buttons for all the terrains.
     */
    private void buildList() {
        final Specification spec = getSpecification();
        final Dimension terrainSize = ImageLibrary
            .scaleDimension(ImageLibrary.TILE_OVERLAY_SIZE,
                            ImageLibrary.SMALLER_SCALE);
        final Dimension riverSize = ImageLibrary
            .scaleDimension(ImageLibrary.TILE_SIZE,
                            ImageLibrary.SMALLER_SCALE);

        for (TileType type : spec.getTileTypeList()) {
            listPanel.add(buildButton(getGUI().createTileImageWithOverlayAndForest(type, terrainSize),
                    Messages.getName(type),
                    new TileTypeTransform(type)));
        }

        listPanel.add(buildButton(ImageLibrary.getRiverImage("0101", riverSize),
                Messages.message("mapEditorTransformPanel.minorRiver"),
                new RiverTransform(TileImprovement.SMALL_RIVER)));
        listPanel.add(buildButton(ImageLibrary.getRiverImage("0202", riverSize),
                Messages.message("mapEditorTransformPanel.majorRiver"),
                new RiverTransform(TileImprovement.LARGE_RIVER)));
        listPanel.add(buildButton(ImageLibrary.getRiverImage("2022", riverSize),
                Messages.message("mapEditorTransformPanel.changeRiverConnections"),
                new RiverStyleTransform(RiverStyleTransform.CHANGE_CONNECTIONS)));
        listPanel.add(buildButton(ImageLibrary.getRiverImage("1022", riverSize),
                Messages.message("mapEditorTransformPanel.setRiverStyle"),
                new RiverStyleTransform(RiverStyleTransform.SET_STYLE)));

        final ResourceType rt = first(spec.getResourceTypeList());
        listPanel.add(buildButton(ImageLibrary.getResourceTypeImage(rt,
                    riverSize, false),
                Messages.message("mapEditorTransformPanel.resource"),
                new ResourceTransform()));

        listPanel.add(buildButton(ImageLibrary.getLCRImage(riverSize),
                Messages.getName(ModelMessage.MessageType.LOST_CITY_RUMOUR),
                new LostCityRumourTransform()));

        SettlementType settlementType = getNativeNation().getType()
            .getCapitalType();
        listPanel.add(buildButton(ImageLibrary.getSettlementTypeImage(settlementType, riverSize),
                Messages.message("settlement"),
                new SettlementTransform()));
    }

    /**
     * Builds the button for the given terrain.
     *
     * @param image an {@code Image} value
     * @param text a {@code String} value
     * @param mt a {@code MapTransform} value
     * @return A suitable button.
     */
    private JToggleButton buildButton(Image image, String text,
                                      final MapTransform mt) {
        final MapEditorController ctlr
            = getFreeColClient().getMapEditorController();
        JPanel descriptionPanel = new JPanel(new BorderLayout());
        descriptionPanel.add(new JLabel(new ImageIcon(image)),
                             BorderLayout.CENTER);
        descriptionPanel.add(new JLabel(text, JLabel.CENTER),
                BorderLayout.PAGE_END);
        descriptionPanel.setBackground(Color.RED);
        mt.setDescriptionPanel(descriptionPanel);

        ImageIcon icon = new ImageIcon(image);
        final JToggleButton button = new JToggleButton(icon);
        button.setToolTipText(text);
        button.setOpaque(false);
        group.add(button);
        button.addActionListener((ActionEvent ae) -> {
                MapTransform newMapTransform = null;
                if (ctlr.getMapTransform() != mt) {
                    if (mt instanceof RiverStyleTransform) {
                        RiverStyleTransform rst = (RiverStyleTransform)mt;
                        boolean all = rst.getType() != RiverStyleTransform.CHANGE_CONNECTIONS;
                        String style = getGUI()
                            .showRiverStyleDialog(ImageLibrary
                                .getRiverStyleKeys(all));
                        if (style != null) rst.setStyle(style);
                    }
                    newMapTransform = mt;
                }
                ctlr.setMapTransform(newMapTransform);
                if (newMapTransform == null && mt != null) {
                    // Select the invisible button, de-selecting all others
                    group.setSelected(group.getElements().nextElement()
                        .getModel(), true);
                }
            });
        button.setBorder(null);
        return button;
    }

    /**
     * Ask the user for a resource choice from a list.
     *
     * Ripped out of ResourceTransform to dodge the name clash with
     * CollectionUtils.transform which we want to use here.
     *
     * @param resources A list of {@code ResourceType}s to choose from.
     * @return The chosen {@code ResourceType}.
     */
    private ResourceType getResourceChoice(List<ResourceType> resources) {
        final Function<ResourceType, ChoiceItem<ResourceType>> mapper
            = rt -> new ChoiceItem<ResourceType>(Messages.getName(rt), rt);
        StringTemplate tmpl = StringTemplate.template("mapEditorTransformPanel.chooseResource");
        return getGUI().getChoice(tmpl, "cancel",
                                  transform(resources, alwaysTrue(), mapper));
    }

    public static final class TileTypeTransform extends MapTransform {
        private final TileType tileType;

        private TileTypeTransform(TileType tileType) {
            this.tileType = tileType;
        }

        public TileType getTileType() {
            return tileType;
        }

        @Override
        public void transform(Tile t) {
            t.changeType(tileType);
            t.removeLostCityRumour();
        }
    }
}
