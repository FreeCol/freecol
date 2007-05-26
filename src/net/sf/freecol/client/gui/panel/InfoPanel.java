package net.sf.freecol.client.gui.panel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.ViewMode;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.MapEditorTransformPanel.MapTransform;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import cz.autel.dmi.HIGLayout;

/**
 * This is the panel that shows more details about the currently selected unit
 * and the tile it stands on. It also shows the amount of gold the player has
 * left and stuff like that.
 */
public final class InfoPanel extends FreeColPanel {
    public static final String COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(InfoPanel.class.getName());

    private static final int PANEL_WIDTH = 256;

    private static final int PANEL_HEIGHT = 128;

    private final FreeColClient freeColClient;

    @SuppressWarnings("unused")
    private final Game game;

    private final ImageLibrary library;

    private final EndTurnPanel endTurnPanel = new EndTurnPanel();

    private final UnitInfoPanel unitInfoPanel;

    private final TileInfoPanel tileInfoPanel = new TileInfoPanel();
    
    private final JPanel mapEditorPanel;
    

    /**
     * The constructor that will add the items to this panel.
     * 
     * @param freeColClient The main controller object for the client.
     * @param game The Game object that has all kinds of useful information that
     *            we want to display here.
     * @param imageProvider The ImageProvider that can provide us with images to
     *            display on this panel.
     */
    public InfoPanel(FreeColClient freeColClient, Game game, ImageProvider imageProvider) {
        this.freeColClient = freeColClient;
        this.game = game;
        this.library = (ImageLibrary) imageProvider;

        unitInfoPanel = new UnitInfoPanel();
        setLayout(null);

        int internalPanelTop = 0;
        int internalPanelHeight = 128;
        Image skin = (Image) UIManager.get("InfoPanel.skin");
        if (skin == null) {
            setSize(PANEL_WIDTH, PANEL_HEIGHT);
        } else {
            setBorder(null);
            setSize(skin.getWidth(null), skin.getHeight(null));
            setOpaque(false);
            internalPanelTop = 75;
            internalPanelHeight = 100;
        }
        
        mapEditorPanel = new JPanel(null);
        mapEditorPanel.setSize(130, 100);
        mapEditorPanel.setOpaque(false);

        add(unitInfoPanel, internalPanelTop, internalPanelHeight);
        add(endTurnPanel, internalPanelTop, internalPanelHeight);
        add(tileInfoPanel, internalPanelTop, internalPanelHeight);
        add(mapEditorPanel, internalPanelTop, internalPanelHeight);
    }

    /**
     * Adds a panel to show information
     */
    private void add(JPanel panel, int internalPanelTop, int internalPanelHeight) {
        panel.setVisible(false);
        panel.setLocation((getWidth() - panel.getWidth()) / 2, internalPanelTop
                + (internalPanelHeight - panel.getHeight()) / 2);
        add(panel);
    }

    /**
     * Updates this <code>InfoPanel</code>.
     * 
     * @param unit The displayed unit (or null if none)
     */
    public void update(Unit unit) {
        unitInfoPanel.update(unit);
    }

    /**
     * Updates this <code>InfoPanel</code>.
     * 
     * @param mapTransform The current MapTransform.
     */
    public void update(MapTransform mapTransform) {
        if (mapTransform != null) {
            final JPanel p = mapTransform.getDescriptionPanel();
            if (p != null) {
                p.setOpaque(false);
                final Dimension d = p.getPreferredSize();
                p.setBounds(0, (mapEditorPanel.getHeight() - d.height)/2, mapEditorPanel.getWidth(), d.height);
                mapEditorPanel.removeAll();
                mapEditorPanel.add(p, BorderLayout.CENTER);
                mapEditorPanel.validate();
                mapEditorPanel.revalidate();
                mapEditorPanel.repaint();
            }
        }
    }

    
    /**
     * Updates this <code>InfoPanel</code>.
     * 
     * @param tile The displayed tile (or null if none)
     */
    public void update(Tile tile) {
        tileInfoPanel.update(tile);
    }

    /**
     * Gets the <code>Unit</code> in which this <code>InfoPanel</code> is
     * displaying information about.
     * 
     * @return The <code>Unit</code> or <i>null</i> if no <code>Unit</code>
     *         applies.
     */
    public Unit getUnit() {
        return unitInfoPanel.getUnit();
    }

    /**
     * Gets the <code>Tile</code> in which this <code>InfoPanel</code> is
     * displaying information about.
     * 
     * @return The <code>Tile</code> or <i>null</i> if no <code>Tile</code>
     *         applies.
     */
    public Tile getTile() {
        return tileInfoPanel.getTile();
    }

    /**
     * Paints this component.
     * 
     * @param graphics The Graphics context in which to draw this component.
     */
    public void paintComponent(Graphics graphics) {
        int viewMode = freeColClient.getGUI().getViewMode().getView();
        if (!freeColClient.isMapEditor()) {
            if (mapEditorPanel.isVisible()) {
                mapEditorPanel.setVisible(false);
            }
            switch (viewMode) {
            case ViewMode.MOVE_UNITS_MODE:
                if (unitInfoPanel.getUnit() != null) {
                    if (!unitInfoPanel.isVisible()) {
                        unitInfoPanel.setVisible(true);
                        endTurnPanel.setVisible(false);
                    }
                } else if (freeColClient.getMyPlayer() != null
                        && !freeColClient.getMyPlayer().hasNextActiveUnit()) {
                    if (!endTurnPanel.isVisible()) {
                        endTurnPanel.setVisible(true);
                        unitInfoPanel.setVisible(false);
                    }
                }
                tileInfoPanel.setVisible(false);
                break;
            case ViewMode.VIEW_TERRAIN_MODE:
                unitInfoPanel.setVisible(false);
                endTurnPanel.setVisible(false);
                tileInfoPanel.setVisible(true);
                break;
            }
        } else {
            if (!mapEditorPanel.isVisible()) {
                mapEditorPanel.setVisible(true);
                unitInfoPanel.setVisible(false);
                endTurnPanel.setVisible(false);
                tileInfoPanel.setVisible(false);
            }
        }

        Image skin = (Image) UIManager.get("InfoPanel.skin");
        if (skin != null) {
            graphics.drawImage(skin, 0, 0, null);
        }
        
        super.paintComponent(graphics);
    }


    /**
     * Panel for displaying <code>Tile</code>-information.
     */
    public class TileInfoPanel extends JPanel {

        private final JLabel tileLabel, tileNameLabel, bonusLabel, riverLabel, roadLabel, plowLabel, ownerLabel;

        private final JPanel picturePanel, labelPanel;

        private Tile tile;


        public TileInfoPanel() {
            super(null);

            picturePanel = new JPanel(new BorderLayout());
            picturePanel.setOpaque(false);
            picturePanel.setSize(130, 100);
            picturePanel.setLocation(0, 0);
            tileLabel = new JLabel();
            tileLabel.setHorizontalAlignment(JLabel.CENTER);
            tileLabel.setVerticalAlignment(JLabel.CENTER);
            picturePanel.add(tileLabel, BorderLayout.CENTER);
            // tileNameLabel = new JLabel();
            // picturePanel.add(tileNameLabel, BorderLayout.SOUTH);
            add(picturePanel);

            int[] widths = { 0 };
            int[] heights = { 0, 0, 0, 0 };
            labelPanel = new JPanel(new HIGLayout(widths, heights));
            labelPanel.setOpaque(false);

            tileNameLabel = new JLabel();
            bonusLabel = new JLabel();
            riverLabel = new JLabel(Messages.message("river"));
            roadLabel = new JLabel(Messages.message("road"));
            plowLabel = new JLabel(Messages.message("plowed"));
            ownerLabel = new JLabel();

            tileLabel.setFocusable(false);
            tileNameLabel.setFocusable(false);
            bonusLabel.setFocusable(false);
            roadLabel.setFocusable(false);
            riverLabel.setFocusable(false);
            plowLabel.setFocusable(false);
            ownerLabel.setFocusable(false);
            labelPanel.setSize(100, 100);
            labelPanel.setLocation(130, 10);// (100-labelPanel.getHeight())/2);
            add(labelPanel);

            setSize(226, 100);
            setOpaque(false);
        }

        /**
         * Paints this component.
         * 
         * @param graphics The Graphics context in which to draw this component.
         */
        public void paintComponent(Graphics graphics) {
            if (tile != null) {
                tileNameLabel.setText(tile.getName());
                labelPanel.removeAll();
                labelPanel.add(tileNameLabel, higConst.rc(1, 1));
                int row = 2;
                if (tile.getAddition() == Tile.ADD_RIVER_MINOR) {
                    riverLabel.setText(Messages.message("minorRiver"));
                    labelPanel.add(riverLabel, higConst.rc(row, 1));
                    row++;
                } else if (tile.getAddition() == Tile.ADD_RIVER_MAJOR) {
                    riverLabel.setText(Messages.message("majorRiver"));
                    labelPanel.add(riverLabel, higConst.rc(row, 1));
                    row++;
                }
                if (tile.hasRoad()) {
                    labelPanel.add(roadLabel, higConst.rc(row, 1));
                    row++;
                }
                if (tile.isPlowed()) {
                    labelPanel.add(plowLabel, higConst.rc(row, 1));
                    row++;
                }
                if (tile.getNationOwner() != Player.NO_NATION) {
                    ownerLabel.setText(Player.getNationAsString(tile.getNationOwner()));
                    labelPanel.add(ownerLabel, higConst.rc(row, 1));
                }

                int width = library.getTerrainImageWidth(tile.getType());
                int height = library.getTerrainImageHeight(tile.getType());
                BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                freeColClient.getGUI().displayTerrain(image.createGraphics(), freeColClient.getGame().getMap(), tile,
                        0, 0);
                tileLabel.setIcon(new ImageIcon(image));
            } else {
                tileLabel.setIcon(null);
            }
            labelPanel.validate();
            super.paintComponent(graphics);
        }

        /**
         * Updates this <code>InfoPanel</code>.
         * 
         * @param tile The displayed tile (or null if none)
         */
        public void update(Tile tile) {
            this.tile = tile;
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
    public class UnitInfoPanel extends JPanel {

        private final JLabel unitLabel, unitNameLabel, unitTypeLabel, unitMovesLabel, unitToolsLabel, goldLabel;

        private final JPanel unitCargoPanel, labelPanel;

        private Unit unit;

        private final int[] widths = { 0, 0, 0, 0 };

        private final int[] heights = { 0, 0 };

        public UnitInfoPanel() {
            super(null);

            Image tools = library.getGoodsImageIcon(Goods.TOOLS).getImage();
            ImageIcon toolsIcon = new ImageIcon(tools.getScaledInstance(tools.getWidth(null) * 2 / 3, tools
                    .getHeight(null) * 2 / 3, Image.SCALE_SMOOTH));
            unitToolsLabel = new JLabel(toolsIcon);

            unitCargoPanel = new JPanel(new HIGLayout(widths, heights));
            unitCargoPanel.setOpaque(false);

            JPanel picturePanel = new JPanel(new BorderLayout());
            picturePanel.setOpaque(false);
            picturePanel.setSize(110, 100);
            picturePanel.setLocation(0, 0);
            unitLabel = new JLabel();
            unitLabel.setHorizontalAlignment(JLabel.CENTER);
            unitLabel.setVerticalAlignment(JLabel.CENTER);
            picturePanel.add(unitLabel, BorderLayout.CENTER);
            add(picturePanel);

            int[] widths = { 0 };
            int[] heights = { 0, 0, 0, 0 };
            labelPanel = new JPanel(new HIGLayout(widths, heights));
            labelPanel.setOpaque(false);

            unitNameLabel = new JLabel();
            unitMovesLabel = new JLabel();
            unitTypeLabel = new JLabel();
            goldLabel = new JLabel();

            labelPanel.add(unitNameLabel, higConst.rc(1, 1));
            labelPanel.add(unitTypeLabel, higConst.rc(2, 1));
            labelPanel.add(unitMovesLabel, higConst.rc(3, 1));
            labelPanel.add(unitCargoPanel, higConst.rc(4, 1));

            unitLabel.setFocusable(false);
            unitNameLabel.setFocusable(false);
            unitTypeLabel.setFocusable(false);
            unitMovesLabel.setFocusable(false);
            unitToolsLabel.setFocusable(false);
            labelPanel.setSize(130, 100);
            labelPanel.setLocation(100, 10);// (100-labelPanel.getHeight())/2);
            add(labelPanel);

            setSize(226, 100);
            setOpaque(false);
        }

        /**
         * Updates this <code>InfoPanel</code>.
         * 
         * @param unit The displayed unit (or null if none)
         */
        public void update(Unit unit2) {
            this.unit = unit2;            
            
            unitCargoPanel.removeAll();
            if (unit != null) {
                String name = unit.getName();
                int index = name.indexOf(" (");
                if (index < 0) {
                    unitNameLabel.setText(name);
                    unitTypeLabel.setText(null);
                } else {
                    unitNameLabel.setText(name.substring(0, index));
                    unitTypeLabel.setText(name.substring(index + 1));
                }
                unitLabel.setIcon(library.getUnitImageIcon(library.getUnitGraphicsType(unit)));
                unitMovesLabel.setText(Messages.message("moves") + " " + unit.getMovesAsString());
                if (unit.isPioneer()) {
                    unitToolsLabel.setText(String.valueOf(unit.getNumberOfTools()));
                    unitCargoPanel.add(unitToolsLabel, higConst.rc(1, 1));
                } else if (unit.getType() == Unit.TREASURE_TRAIN) {
                    goldLabel.setText(unit.getTreasureAmount() + " " + Messages.message("gold"));
                    unitCargoPanel.add(goldLabel, higConst.rc(1, 1));
                } else if (unit.isCarrier()) {
                    int counter = 1;
                    int row = 1;
                    Iterator<Goods> goodsIterator = unit.getGoodsIterator();
                    while (goodsIterator.hasNext()) {
                        Goods goods = goodsIterator.next();
                        JLabel goodsLabel = new JLabel(library.getScaledGoodsImageIcon(goods.getType(), 0.66f));
                        goodsLabel.setToolTipText(String.valueOf(goods.getAmount()) + " " + goods.getName());
                        unitCargoPanel.add(goodsLabel, higConst.rc(row, counter));
                        if (counter == 4) {
                            row++;
                            counter = 1;
                        } else {
                            counter++;
                        }
                    }
                    Iterator<Unit> unitIterator = unit.getUnitIterator();
                    while (unitIterator.hasNext()) {
                        Unit unit = unitIterator.next();
                        int graphicsType = library.getUnitGraphicsType(unit);
                        JLabel unitLabel = new JLabel(library.getScaledUnitImageIcon(graphicsType, 0.5f));
                        unitLabel.setToolTipText(unit.getName());
                        unitCargoPanel.add(unitLabel, higConst.rc(row, counter));
                        if (counter == 4) {
                            row++;
                            counter = 1;
                        } else {
                            counter++;
                        }
                    }
                    unitCargoPanel.validate();
                }
            } else {
                unitLabel.setIcon(null);
            }
            labelPanel.validate();
        }

        /**
         * Gets the <code>Unit</code> in which this <code>InfoPanel</code>
         * is displaying information about.
         * 
         * @return The <code>Unit</code> or <i>null</i> if no
         *         <code>Unit</code> applies.
         */
        public Unit getUnit() {
            return unit;
        }
    }

    /**
     * Panel for ending the turn.
     */
    public class EndTurnPanel extends JPanel {

        private JLabel endTurnLabel = new JLabel(Messages.message("infoPanel.endTurnPanel.text"), JLabel.CENTER);

        private JButton endTurnButton = new JButton(Messages.message("infoPanel.endTurnPanel.endTurnButton"));


        public EndTurnPanel() {
            super(new FlowLayout(FlowLayout.CENTER, 10, 10));

            add(endTurnLabel);
            add(endTurnButton);
            setOpaque(false);
            setSize(230, endTurnLabel.getPreferredSize().height + endTurnButton.getPreferredSize().height + 30);

            /*
             * TODO: The action listener does not work, because this button
             * looses it's focus. The reason why the focus gets lost should be
             * found, in order to use the actionlistener.
             */
            /*
             * endTurnButton.addActionListener(new ActionListener() { public
             * void actionPerformed(ActionEvent e) {
             * freeColClient.getInGameController().endTurn(); } });
             */

            endTurnButton.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    freeColClient.getInGameController().endTurn();
                }
            });
        }
    }
}
