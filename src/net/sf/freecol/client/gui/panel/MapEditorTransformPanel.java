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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.MapEditorController;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.IndianNationType;
import net.sf.freecol.common.model.LostCityRumour;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Resource;
import net.sf.freecol.common.model.ResourceType;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.SettlementType;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.model.TileImprovementStyle;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.TileItemContainer;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.generator.RiverSection;
import net.sf.freecol.server.model.ServerIndianSettlement;
import net.sf.freecol.server.model.ServerUnit;


/**
 * A panel for choosing the current <code>MapTransform</code>.
 *
 * <br><br>
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

    private final JPanel listPanel;
    private JToggleButton settlementButton;
    private ButtonGroup group;

    /**
     * A native player to use for native settlement type and skill.
     */
    private static Player nativePlayer = null;


    /**
     * Creates a panel to choose a map transform.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public MapEditorTransformPanel(FreeColClient freeColClient) {
        super(freeColClient, new BorderLayout());

        // Make sure the native players are present.
        if (getGame().getPlayers().isEmpty()) {
            getFreeColClient().getFreeColServer().initializeAI(false);
        }
        for (Player p : getGame().getPlayers()) {
            if (p.isIndian()) {
                nativePlayer = p;
                break;
            }
        }
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
     * Builds the buttons for all the terrains.
     */
    private void buildList() {
        List<TileType> tileList = getSpecification().getTileTypeList();
        for (TileType type : tileList) {
            listPanel.add(buildButton(getLibrary().getCompoundTerrainImage(type, 0.5),
                                      Messages.message(type.getNameKey()),
                                      new TileTypeTransform(type)));
        }
        listPanel.add(buildButton(getLibrary().getRiverImage("0101", 0.5),
                                  Messages.message("minorRiver"),
                                  new RiverTransform(TileImprovement.SMALL_RIVER)));
        listPanel.add(buildButton(getLibrary().getRiverImage("0202", 0.5),
                                  Messages.message("majorRiver"),
                                  new RiverTransform(TileImprovement.LARGE_RIVER)));
        listPanel.add(buildButton(getLibrary().getBonusImage(getSpecification()
                                                             .getResourceTypeList().get(0), 0.8),
                                  Messages.message("editor.resource"), new ResourceTransform()));
        listPanel.add(buildButton(getLibrary().getMiscImage(ImageLibrary.LOST_CITY_RUMOUR, 0.66),
                                  Messages.message("model.message.LOST_CITY_RUMOUR"),
                                  new LostCityRumourTransform()));
        if (nativePlayer != null) {
            SettlementType settlementType = nativePlayer.getNationType().getCapitalType();
            settlementButton = buildButton(getLibrary().getSettlementImage(settlementType, 0.5),
                                           Messages.message("Settlement"), new SettlementTransform());
            listPanel.add(settlementButton);
        }
    }

    /**
     * Builds the button for the given terrain.
     *
     * @param image an <code>Image</code> value
     * @param text a <code>String</code> value
     * @param mt a <code>MapTransform</code> value
     */
    private JToggleButton buildButton(Image image, String text, final MapTransform mt) {

        JPanel descriptionPanel = new JPanel(new BorderLayout());
        descriptionPanel.add(new JLabel(new ImageIcon(image)), BorderLayout.CENTER);
        descriptionPanel.add(new JLabel(text, JLabel.CENTER), BorderLayout.SOUTH);
        descriptionPanel.setBackground(Color.RED);
        mt.setDescriptionPanel(descriptionPanel);

        ImageIcon icon = new ImageIcon(image);
        final JToggleButton button = new JToggleButton(icon);
        button.setToolTipText(text);
        button.setOpaque(false);
        group.add(button);
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	MapEditorController ctlr = getFreeColClient().getMapEditorController();
            	MapTransform newMapTransform = null;
            	if(ctlr.getMapTransform() != mt){
            		newMapTransform = mt;
            	}
            	ctlr.setMapTransform(newMapTransform);
            	if(newMapTransform == null && mt != null){
            		//select the invisible button, de-selecting all others
            		group.setSelected(group.getElements().nextElement().getModel(),true);
            	}
            }
        });
        button.setBorder(null);
        return button;
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
            t.changeType(tileType);
            t.removeLostCityRumour();
        }
    }

    private class RiverTransform extends MapTransform {
        private int magnitude;

        private RiverTransform(int magnitude) {
            this.magnitude = magnitude;
        }

        public void transform(Tile tile) {
            TileImprovementType riverType =
                tile.getSpecification().getTileImprovementType("model.improvement.river");

            if (tile.getType().canHaveImprovement(riverType)
                && !tile.hasRiver()) {
                String conns = "";
                for (Direction direction : Direction.longSides) {
                    Tile t = tile.getNeighbourOrNull(direction);
                    TileImprovement otherRiver = (t == null) ? null
                        : t.getRiver();
                    conns += (t == null
                        || (t.isLand() && otherRiver == null)) ? "0"
                        : Integer.toString(magnitude);
                }
                tile.addRiver(magnitude, conns);
            }
        }
    }

    /**
     * Adds, removes or cycles through the available resources for
     * this Tile.  Cycles through the ResourceTypeList and picks the
     * next valid, or removes if end of list.
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
                List<ResourceType> resList = t.getType().getResourceTypes();
                switch (resList.size()) {
                case 0:
                    return;
                case 1:
                    ResourceType resourceType = resList.get(0);
                    // TODO: create GUI for setting the quantity
                    t.addResource(new Resource(t.getGame(), t, resourceType,
                                  resourceType.getMaxValue()));
                    return;
                default:
                    List<ChoiceItem<ResourceType>> choices
                        = new ArrayList<ChoiceItem<ResourceType>>();
                    for (ResourceType rt : resList) {
                        String name = Messages.message(rt.getNameKey());
                        choices.add(new ChoiceItem<ResourceType>(name, rt));
                    }
                    ResourceType choice = getGUI().showChoiceDialog(true, null, 
                        Messages.message("editor.chooseResource"), null,
                        "cancel", choices);
                    if (choice != null) {
                        t.addResource(new Resource(t.getGame(), t, choice,
                                      choice.getMaxValue()));
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
                    t.addLostCityRumour(new LostCityRumour(t.getGame(), t));
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
                if (settlement == null && nativePlayer != null) {
                    logger.info("Adding settlement to tile " + t);
                    UnitType skill = ((IndianNationType) nativePlayer.getNationType()).getSkills().get(0)
                        .getObject();
                    String name = nativePlayer.getSettlementName(null);
                    settlement = new ServerIndianSettlement(t.getGame(),
                        nativePlayer, name, t, false, skill, null);
                    nativePlayer.addSettlement(settlement);
                    settlement.placeSettlement(true);
                    UnitType brave = getSpecification().getUnitType("model.unit.brave");
                    for (int index = 0; index < 5; index++) {
                        logger.info("Adding unit " + brave + " to settlement.");
                        settlement.add(new ServerUnit(settlement.getGame(),
                                settlement, settlement.getOwner(), brave));
                    }
                }
            }
        }
    }
}
