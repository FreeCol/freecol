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
import java.util.List;
import java.util.logging.Logger;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.MapEditorController;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.model.ResourceType;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;

/**
 * A panel for choosing the current <code>MapTransform</code>.
 *
 * <br><br>
 * 
 * This panel is only used when running in
 * {@link FreeColClient#isMapEditor() map editor mode}.
 * 
 * @see MapEditorController#getMapTransform()
 * @see MapTransform
 */
public final class MapEditorTransformPanel extends FreeColPanel {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(MapEditorTransformPanel.class.getName());
    



    private static final int MINOR_RIVER = -1,
            MAJOR_RIVER = -2,
            RESOURCE = -3,
            LOST_CITY_RUMOUR = -4;
    
    private final FreeColClient freeColClient;

    private final ImageLibrary library;
    
    private final JPanel listPanel;
    
    private ButtonGroup group;
    

    /**
     * The constructor that will add the items to this panel. 
     * @param parent The parent of this panel.
     */
    public MapEditorTransformPanel(Canvas parent) {
        super(new BorderLayout());
        
        this.freeColClient = parent.getClient();
        this.library = parent.getGUI().getImageLibrary();

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
        List<TileType> tileList = FreeCol.getSpecification().getTileTypeList();
        for (TileType type : tileList) {
            buildButton(type.getIndex(), type.getName(), new TileTypeTransform(type));
        }
        buildButton(MINOR_RIVER, "Minor River", new RiverTransform(MINOR_RIVER));
        buildButton(MAJOR_RIVER, "Major River", new RiverTransform(MAJOR_RIVER));
        buildButton(RESOURCE, "Change/Remove Resource", new ResourceTransform());
        buildButton(LOST_CITY_RUMOUR, "Lost City Rumour", new LostCityRumourTransform());
    }

    /**
     * Builds the button for the given terrain.
     * 
     * @param index the index of terrain or one of {MINOR_RIVER, MAJOR_RIVER, BONUS, LOST_CITY_RUMOUR}
     */
    private void buildButton(int index, String text, final MapTransform mt) {
        Image scaledImage;
        Image image;
        if (index == MINOR_RIVER) {
            image = library.getRiverImage(10);
        } else if (index == MAJOR_RIVER) {
            image = library.getRiverImage(10);
        } else if (index == RESOURCE) {
            image = library.getBonusImage(FreeCol.getSpecification().getResourceType(0));
        } else if (index == LOST_CITY_RUMOUR) {
            image = library.getMiscImage(ImageLibrary.LOST_CITY_RUMOUR);
        } else {
            image = library.getScaledTerrainImage(FreeCol.getSpecification().getTileType(index), 1.0f);
        }
        scaledImage = image.getScaledInstance((int) (image.getWidth(null) * 0.5f), (int) (image.getHeight(null) * 0.5f), Image.SCALE_SMOOTH);
        
        JPanel descriptionPanel = new JPanel(new BorderLayout());
        descriptionPanel.add(new JLabel(new ImageIcon(image)), BorderLayout.CENTER);
        descriptionPanel.add(new JLabel(text, JLabel.CENTER), BorderLayout.SOUTH);
        descriptionPanel.setBackground(Color.RED);
        mt.setDescriptionPanel(descriptionPanel);
        
        ImageIcon icon = new ImageIcon(scaledImage);
        final JButton button = new JButton(icon);
        //button.setText(text);
        //button.setVerticalTextPosition(SwingConstants.BOTTOM);
        //button.setHorizontalTextPosition(SwingConstants.CENTER);
        button.setToolTipText(text);
        button.setOpaque(false);
        group.add(button);
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                freeColClient.getMapEditorController().setMapTransform(mt);
            }
        });
        button.setBorder(null);
        listPanel.add(button);
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
    
    private class TileTypeTransform extends MapTransform {
        private TileType tileType;
        
        private TileTypeTransform(TileType tileType) {
            this.tileType = tileType;    
        }
        
        public void transform(Tile t) {
            t.setType(tileType);     
            t.setLostCityRumour(false);
        }
    }
/*  Depreciated    
    private class ForestTransform extends MapTransform {
        public void transform(Tile t) {
            t.setForested(true);            
        }
    }

    private class AdditionTransform extends MapTransform {
        private int addition;
        
        private AdditionTransform(int addition) {
            this.addition = addition;
        }
        
        public void transform(Tile t) {
            if (addition == Tile.ADD_RIVER_MAJOR ||
                addition == Tile.ADD_RIVER_MINOR) {
                t.addRiver(addition);
            } else {
                t.setAddition(addition);            
            }
        }
    }
*/
    
    private class RiverTransform extends MapTransform {
        private int magnitude;
        
        private RiverTransform(int magnitude) {
            this.magnitude = magnitude;
        }
        
        public void transform(Tile t) {
            t.addRiver(magnitude);
        }
    }

    /**
     * Adds, Removes or Cycles through the available resources for this Tile
     * Cycles through the ResourceTypeList and picks the next valid, or removes if end of list
     */
    private class ResourceTransform extends MapTransform {
        public void transform(Tile t) {
            TileType tileType = t.getType();
            List<ResourceType> resList = tileType.getResourceTypeList();
            // Check if there is a resource already
            if (t.hasResource()) {
                // Get the index for this Resource in the resList
                int index = resList.indexOf(t.getTileItemContainer().getResource().getType());
                ResourceType resType = null;
                if (++index < resList.size()) {
                    // Valid resource after this one, otherwise remain null
                    resType = resList.get(index);
                }
                t.setResource(resType);
            } else {
                if (resList.size() > 0) {
                    // Take first valid in ResourceList
                    t.setResource(resList.get(0));
                }
            }
        }
    }
    
    private class LostCityRumourTransform extends MapTransform {
        public void transform(Tile t) {
            t.setLostCityRumour(true);          
        }
    }
}
