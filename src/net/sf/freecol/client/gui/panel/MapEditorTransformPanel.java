package net.sf.freecol.client.gui.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.MapEditorController;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.model.Tile;

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
    
    public static final String COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    private static final int MINOR_RIVER = -1,
            MAJOR_RIVER = -2,
            BONUS = -3,
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
        this.library = (ImageLibrary) parent.getImageProvider();

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
        buildButton(Tile.OCEAN, false, Tile.getName(Tile.OCEAN, false, Tile.ADD_NONE), new TileTypeTransform(Tile.OCEAN));
        buildButton(Tile.HIGH_SEAS, false, Tile.getName(Tile.HIGH_SEAS, false, Tile.ADD_NONE), new TileTypeTransform(Tile.HIGH_SEAS));
        for (int type = 1; type < Tile.ARCTIC; type++) {
            buildButton(type, false, Tile.getName(type, false, Tile.ADD_NONE), new TileTypeTransform(type));
        }
        buildButton(Tile.ARCTIC, false, Tile.getName(Tile.ARCTIC, false, Tile.ADD_NONE), new TileTypeTransform(Tile.ARCTIC));
        buildButton(Tile.PLAINS, true, Tile.getName(Tile.PLAINS, true, Tile.ADD_NONE), new ForestTransform());
        buildButton(ImageLibrary.HILLS, false, Tile.getName(Tile.PLAINS, false, Tile.ADD_HILLS), new AdditionTransform(Tile.ADD_HILLS));
        buildButton(ImageLibrary.MOUNTAINS, false, Tile.getName(Tile.PLAINS, false, Tile.ADD_MOUNTAINS), new AdditionTransform(Tile.ADD_MOUNTAINS));
        buildButton(MINOR_RIVER, false, "Minor River", new AdditionTransform(Tile.ADD_RIVER_MINOR));
        buildButton(MAJOR_RIVER, false, "Major River", new AdditionTransform(Tile.ADD_RIVER_MAJOR));
        buildButton(BONUS, false, "Bonus", new BonusTransform());
        buildButton(LOST_CITY_RUMOUR, false, "Lost City Rumour", new LostCityRumourTransform());
    }

    /**
     * Builds the button for the given terrain.
     * 
     * @param terrain the type of terrain
     * @param forested whether it is forested
     */
    private void buildButton(int terrain, boolean forested, String text, final MapTransform mt) {
        Image scaledImage;
        Image image;
        if (terrain == MINOR_RIVER) {
            image = library.getRiverImage(10);
            scaledImage = image.getScaledInstance((int) (image.getWidth(null) * 0.5f), (int) (image.getHeight(null) * 0.5f), Image.SCALE_SMOOTH);
        } else if (terrain == MAJOR_RIVER) {
            image = library.getRiverImage(10);
            scaledImage = image.getScaledInstance((int) (image.getWidth(null) * 0.5f), (int) (image.getHeight(null) * 0.5f), Image.SCALE_SMOOTH);
        } else if (terrain == BONUS) {
            image = library.getGoodsImage(2);
            scaledImage = image.getScaledInstance((int) (image.getWidth(null) * 0.9f), (int) (image.getHeight(null) * 0.9f), Image.SCALE_SMOOTH);
        } else if (terrain == LOST_CITY_RUMOUR) {
            image = library.getMiscImage(ImageLibrary.LOST_CITY_RUMOUR);
            scaledImage = image.getScaledInstance((int) (image.getWidth(null) * 0.5f), (int) (image.getHeight(null) * 0.5f), Image.SCALE_SMOOTH);
        } else {
            image = library.getScaledTerrainImage(terrain, forested, 1.0f);
            scaledImage = library.getScaledTerrainImage(terrain, forested, 0.5f);
        }
        
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
        private int tileType;
        
        private TileTypeTransform(int tileType) {
            this.tileType = tileType;    
        }
        
        public void transform(Tile t) {
            t.setType(tileType);     
            t.setForested(false);
            t.setAddition(Tile.ADD_NONE);
            t.setLostCityRumour(false);
            t.setBonus(false);
        }
    }
    
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
    
    private class BonusTransform extends MapTransform {
        public void transform(Tile t) {
            t.setBonus(true);           
        }
    }
    
    private class LostCityRumourTransform extends MapTransform {
        public void transform(Tile t) {
            t.setLostCityRumour(true);          
        }
    }
}
