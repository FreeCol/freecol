/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;

import net.sf.freecol.client.control.MapEditorController;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.Specification;
import net.sf.freecol.common.model.IndianNationType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.LostCityRumour;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Resource;
import net.sf.freecol.common.model.ResourceType;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Settlement.SettlementType;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileItemContainer;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.generator.River;
import net.sf.freecol.server.generator.RiverSection;


/**
 * A panel for choosing the current <code>MapTransform</code>.
 *
 * <br><br>
 *
 * This panel is only used when running in
 * {@link SpecificationClient#isMapEditor() map editor mode}.
 *
 * @see MapEditorController#getMapTransform()
 * @see MapTransform
 */
public final class MapEditorTransformPanel extends FreeColPanel {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(MapEditorTransformPanel.class.getName());

    private static final UnitType BRAVE = Specification.getSpecification().getUnitType("model.unit.brave");

    private final JPanel listPanel;
    private JToggleButton settlementButton;

    private ButtonGroup group;

    private static final TileImprovementType riverType =
        Specification.getSpecification().getTileImprovementType("model.improvement.River");

    /**
     * Describe nativePlayer here.
     */
    private static Player nativePlayer;

    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent of this panel.
     */
    public MapEditorTransformPanel(Canvas parent) {
        super(parent, new BorderLayout());

        // assume we have only native players for the moment
        if (getGame().getPlayers().isEmpty()) {
            FreeColServer server = getClient().getFreeColServer();
            if (server.getAIMain() == null) {
                server.setAIMain(new AIMain(server));
            }
            for (Nation nation : Specification.getSpecification().getIndianNations()) {
                server.addAIPlayer(nation);
            }
        }
        nativePlayer = getGame().getPlayers().get(0);
        listPanel = new JPanel(new GridLayout(2, 0));

        group = new ButtonGroup();
        buildList();

        JScrollPane sl = new JScrollPane(listPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sl.getViewport().setOpaque(false);
        add(sl);
    }



    /**
     * Builds the buttons for all the terrains.
     */
    private void buildList() {
        List<TileType> tileList = Specification.getSpecification().getTileTypeList();
        for (TileType type : tileList) {
            listPanel.add(buildButton(getLibrary().getScaledTerrainImage(type, 1f),
                                      Messages.message(type.getNameKey()),
                                      new TileTypeTransform(type)));
        }
        listPanel.add(buildButton(getLibrary().getRiverImage(10), Messages.message("minorRiver"),
                                  new RiverTransform(TileImprovement.SMALL_RIVER)));
        listPanel.add(buildButton(getLibrary().getRiverImage(20), Messages.message("majorRiver"),
                                  new RiverTransform(TileImprovement.LARGE_RIVER)));
        listPanel.add(buildButton(getLibrary().getBonusImage(Specification.getSpecification()
                                                             .getResourceTypeList().get(0)),
                                  Messages.message("editor.resource"), new ResourceTransform()));
        listPanel.add(buildButton(getLibrary().getMiscImage(getLibrary().LOST_CITY_RUMOUR),
                                  Messages.message("model.message.LOST_CITY_RUMOUR"),
                                  new LostCityRumourTransform()));
        SettlementType settlementType = ((IndianNationType) nativePlayer.getNationType()).getTypeOfSettlement();
        settlementButton = buildButton(getLibrary().getSettlementImage(settlementType),
                                       Messages.message("Settlement"), new SettlementTransform());
        listPanel.add(settlementButton);
    }

    /**
     * Builds the button for the given terrain.
     *
     * @param image an <code>Image</code> value
     * @param text a <code>String</code> value
     * @param mt a <code>MapTransform</code> value
     */
    private JToggleButton buildButton(Image image, String text, final MapTransform mt) {

        Image scaledImage = getLibrary().scaleImage(image, 0.5f);

        JPanel descriptionPanel = new JPanel(new BorderLayout());
        descriptionPanel.add(new JLabel(new ImageIcon(image)), BorderLayout.CENTER);
        descriptionPanel.add(new JLabel(text, JLabel.CENTER), BorderLayout.SOUTH);
        descriptionPanel.setBackground(Color.RED);
        mt.setDescriptionPanel(descriptionPanel);

        ImageIcon icon = new ImageIcon(scaledImage);
        final JToggleButton button = new JToggleButton(icon);
        button.setToolTipText(text);
        button.setOpaque(false);
        group.add(button);
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                getClient().getMapEditorController().setMapTransform(mt);
            }
        });
        button.setBorder(null);
        return button;
    }

    /**
     * Get the <code>NativePlayer</code> value.
     *
     * @return a <code>Player</code> value
     */
    public static Player getNativePlayer() {
        return nativePlayer;
    }

    /**
     * Set the <code>NativePlayer</code> value.
     *
     * @param newNativePlayer The new NativePlayer value.
     */
    public static void setNativePlayer(final Player newNativePlayer) {
        nativePlayer = newNativePlayer;
    }

    /**
     * Represents a transformation that can be applied to
     * a <code>Tile</code>.
     *
     * @see #transform(Tile)
     */
    public abstract class MapTransform {

        /**
         * A panel with information about this transformation.
         */
        private JPanel descriptionPanel = null;

        /**
         * Applies this transformation to the given tile.
         * @param t The <code>Tile</code> to be transformed,
         */
        public abstract void transform(Tile t);

        /**
         * A panel with information about this transformation.
         * This panel is currently displayed on the
         * {@link InfoPanel} when selected, but might be
         * used elsewhere as well.
         *
         * @return The panel or <code>null</code> if no panel
         *      has been set.
         */
        public JPanel getDescriptionPanel() {
            return descriptionPanel;
        }

        /**
         * Sets a panel that can be used for describing this
         * transformation to the user.
         *
         * @param descriptionPanel The panel.
         * @see #setDescriptionPanel(JPanel)
         */
        public void setDescriptionPanel(JPanel descriptionPanel) {
            this.descriptionPanel = descriptionPanel;
        }
    }

    public class TileTypeTransform extends MapTransform {
        private TileType tileType;

        private TileTypeTransform(TileType tileType) {
            this.tileType = tileType;
        }

        public TileType getTileType() {
            return tileType;
        }

        public void transform(Tile t) {
            t.setType(tileType);
            t.removeLostCityRumour();
        }
    }

    private class RiverTransform extends MapTransform {
        private int magnitude;

        private RiverTransform(int magnitude) {
            this.magnitude = magnitude;
        }

        public void transform(Tile tile) {
            if (tile.getType().canHaveImprovement(riverType)) {
                TileItemContainer tic = tile.getTileItemContainer();
                if (tic == null) {
                    tic = new TileItemContainer(tile.getGame(), tile);
                    tile.setTileItemContainer(tic);
                }
                int oldMagnitude = TileImprovement.NO_RIVER;
                TileImprovement river = tic.getRiver();
                if (river == null) {
                    river = new TileImprovement(tile.getGame(), tile, riverType);
                } else {
                    oldMagnitude = river.getMagnitude();
                }

                if (magnitude != oldMagnitude) {
                    tic.addRiver(magnitude, river.getStyle());
                    RiverSection mysection = new RiverSection(river.getStyle());
                    // for each neighboring tile
                    for (Direction direction : River.directions) {
                        Tile t = tile.getMap().getNeighbourOrNull(direction, tile);
                        if (t == null) {
                            continue;
                        }
                        TileImprovement otherRiver = t.getRiver();
                        if (!t.isLand() && otherRiver == null) {
                            // add a virtual river in the ocean/lake tile
                            // just for the purpose of drawing the river mouth
                            otherRiver = new TileImprovement(tile.getGame(), tile, riverType);
                            otherRiver.setMagnitude(tile.getRiver().getMagnitude());
                        }
                        if (otherRiver != null) {
                            // update the other tile river branch
                            Direction otherDirection = direction.getReverseDirection();
                            RiverSection oppositesection = new RiverSection(otherRiver.getStyle());
                            oppositesection.setBranch(otherDirection, tile.getRiver().getMagnitude());
                            otherRiver.setStyle(oppositesection.encodeStyle());
                            // update the current tile river branch
                            mysection.setBranch(direction, tile.getRiver().getMagnitude());
                        }
                        // else the neighbor tile doesn't have a river, nothing to do
                    }
                    tile.getRiver().setStyle(mysection.encodeStyle());
                }
            }
        }

    }

    /**
     * Adds, Removes or Cycles through the available resources for this Tile
     * Cycles through the ResourceTypeList and picks the next valid, or removes if end of list
     */
    private class ResourceTransform extends MapTransform {
        public void transform(Tile t) {
            // Check if there is a resource already
            Resource resource = null;
            if (t.getTileItemContainer() != null) {
                resource = t.getTileItemContainer().getResource();
            }
            if (resource != null) {
                t.getTileItemContainer().removeTileItem(resource);
            } else {
                List<ResourceType> resList = t.getType().getResourceTypeList();
                switch(resList.size()) {
                case 0:
                    return;
                case 1:
                    ResourceType resourceType = resList.get(0);
                    // TODO: create GUI for setting the quantity
                    t.setResource(new Resource(t.getGame(), t, resourceType, resourceType.getMaxValue()));
                    return;
                default:
                    List<ChoiceItem<ResourceType>> choices = new ArrayList<ChoiceItem<ResourceType>>();
                    for (ResourceType resType : resList) {
                        String name = Messages.message(resType.getNameKey());
                        choices.add(new ChoiceItem<ResourceType>(name, resType));
                    }
                    ResourceType choice = getCanvas().showChoiceDialog(null,
                                                                       Messages.message("ok"),
                                                                       Messages.message("cancel"),
                                                                       choices);
                    if (choice != null) {
                        t.setResource(new Resource(t.getGame(), t, choice, choice.getMaxValue()));
                    }
                }
            }
        }
    }

    private class LostCityRumourTransform extends MapTransform {
        public void transform(Tile t) {
            if (t.isLand()) {
                LostCityRumour rumour = t.getLostCityRumour();
                if (rumour == null) {
                    t.add(new LostCityRumour(t.getGame(), t));
                } else {
                    t.removeLostCityRumour();
                }
            }
        }
    }

    private class SettlementTransform extends MapTransform {
        public void transform(Tile t) {
            if (t.isLand()) {
                Settlement settlement = t.getSettlement();
                if (settlement == null) {
                    UnitType skill = ((IndianNationType) nativePlayer.getNationType()).getSkills().get(0)
                        .getObject();
                    String name = nativePlayer.getDefaultSettlementName(false);
                    settlement = new IndianSettlement(t.getGame(), nativePlayer, t, name, false,
                                                      skill, new HashSet<Player>(), null);
                    t.setSettlement(settlement);
                    for (int index = 0; index < 5; index++) {
                        settlement.add(new Unit(settlement.getGame(), settlement, settlement.getOwner(),
                                                BRAVE, UnitState.ACTIVE));
                    }
                }
            }
        }
    }
}
