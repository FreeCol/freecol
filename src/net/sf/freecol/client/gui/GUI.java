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

package net.sf.freecol.client.gui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.UIManager;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.action.DisplayTileTextAction.DisplayText;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.MapControls;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.LostCityRumour;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Region;
import net.sf.freecol.common.model.Resource;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.model.TileItem;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.resources.ImageResource;
import net.sf.freecol.common.resources.ResourceManager;


/**
* This class is responsible for drawing the map/background on the <code>Canvas</code>.
* In addition, the graphical state of the map (focus, active unit..) is also a responsibility
* of this class.
*/
public final class GUI {

    private static final Logger logger = Logger.getLogger(GUI.class.getName());
    
    /**
     * Custom component to paint turn progress.
     * <p>
     * Currently the component darken out background using alpha channel and
     * then paints the player's icon and wait message.
     */
    @SuppressWarnings("serial")
    static class GrayLayer extends Component {

        /** Color for graying out background component */
        private static final Color MASK_COLOR = new Color(0f, 0f, 0f, .6f);
        /** Default font size for message text */
        private static final int DEFAULT_FONT_SIZE = 18;
        /** Font size decrement for message text to reduce length */
        private static final int FONT_SIZE_DECREMENT = 2;
        /**
         * Maximum text width to show. This is additional constraint to the
         * component's bounds
         */
        private static final int MAX_TEXT_WIDTH = 640;

        /** Image library for icon lookup */
        private ImageLibrary imageLibrary;
        /** Player object or <code>null</code> */
        private Player player;

        public GrayLayer(ImageLibrary imageLibrary) {
            this.imageLibrary = imageLibrary;
        }

        /**
         * Executes painting. The method shadows the background image, and
         * paints the message with icon (if available) and text.
         * @param g a <code>Graphics</code> value
         */
        public void paint(Graphics g) {
            Rectangle clipArea = g.getClipBounds();
            if (clipArea == null) {
                clipArea = getBounds();
                clipArea.x = clipArea.y = 0;
            }
            if (clipArea.isEmpty()) {
                // we are done - the picture is OK
                return;
            }
            g.setColor(MASK_COLOR);
            g.fillRect(clipArea.x, clipArea.y, clipArea.width, clipArea.height);

            if (player == null) {
                // we are done, no player information
                return;
            }

            ImageIcon coatOfArmsIcon = imageLibrary
                    .getCoatOfArmsImageIcon(player.getNation());

            Rectangle iconBounds = new Rectangle();
            if (coatOfArmsIcon != null) {
                iconBounds.width = coatOfArmsIcon.getIconWidth();
                iconBounds.height = coatOfArmsIcon.getIconHeight();
            }

            Font nameFont = getFont();
            FontMetrics nameFontMetrics = getFontMetrics(nameFont);
            String message = Messages.message("waitingFor", "%nation%",
                                              Messages.message(player.getNationName()));

            Rectangle textBounds;
            int fontSize = DEFAULT_FONT_SIZE;
            int maxWidth = Math.min(MAX_TEXT_WIDTH, getSize().width);
            do {
                nameFont = nameFont.deriveFont(Font.BOLD, fontSize);
                nameFontMetrics = getFontMetrics(nameFont);
                textBounds = nameFontMetrics.getStringBounds(message, g)
                        .getBounds();
                fontSize -= FONT_SIZE_DECREMENT;
            } while (textBounds.width > maxWidth);

            Dimension size = getSize();
            textBounds.x = (size.width - textBounds.width) / 2;
            textBounds.y = (size.height - textBounds.height - iconBounds.height) / 2;

            iconBounds.x = (size.width - iconBounds.width) / 2;
            iconBounds.y = (size.height - iconBounds.height) / 2
                    + textBounds.height;

            if (textBounds.intersects(clipArea)) {
                // show message
                g.setFont(nameFont);
                g.setColor(imageLibrary.getColor(player));
                g.drawString(message, textBounds.x, textBounds.y
                        + textBounds.height);
            }
            if (coatOfArmsIcon != null && iconBounds.intersects(clipArea)) {
                // show icon
                coatOfArmsIcon.paintIcon(this, g, iconBounds.x, iconBounds.y);
            }
        }

        /**
         * Set the player for which we paint. If the player is already set, then
         * nothing happens, otherwise a repaint event is sent.
         * 
         * @param player
         *            Player for status information
         * 
         * @see #paint(Graphics)
         */
        public void setPlayer(Player player) {
            if (this.player == player) {
                return;
            }
            this.player = player;
            repaint();
        }
    }

    private final FreeColClient freeColClient;
    private Dimension size;
    /**
     * Scaled ImageLibrary only used for map painting.
     */
    private ImageLibrary lib;
    private TerrainCursor cursor;
    private ViewMode viewMode;

    /** Indicates if the game has started, has nothing to do with whether or not the
        client is logged in. */
    private boolean inGame;

    private final Vector<GUIMessage> messages;

    private Map.Position selectedTile;
    private Map.Position focus = null;
    private Unit activeUnit;

    /** A path to be displayed on the map. */
    private PathNode currentPath;
    private PathNode gotoPath = null;
    private boolean gotoStarted = false;

    // Helper variables for displaying the map.
    private int tileHeight, tileWidth, halfHeight, halfWidth,
    topSpace,
    topRows,
    //bottomSpace,
    bottomRows,
    leftSpace,
    rightSpace;

    // The y-coordinate of the Tiles that will be drawn at the bottom
    private int bottomRow = -1;
    // The y-coordinate of the Tiles that will be drawn at the top
    private int topRow;
    // The y-coordinate on the screen (in pixels) of the images of the
    // Tiles that will be drawn at the bottom
    private int bottomRowY;
    // The y-coordinate on the screen (in pixels) of the images of the
    // Tiles that will be drawn at the top
    private int topRowY;

    // The x-coordinate of the Tiles that will be drawn at the left side
    private int leftColumn;
    // The x-coordinate of the Tiles that will be drawn at the right side
    private int rightColumn;
    // The x-coordinate on the screen (in pixels) of the images of the
    // Tiles that will be drawn at the left (can be less than 0)
    private int leftColumnX;

    // The height offset to paint a Unit at (in pixels).
    private static final int UNIT_OFFSET = 20,
        STATE_OFFSET_X = 25,
        STATE_OFFSET_Y = 10,
        OTHER_UNITS_OFFSET_X = -5, // Relative to the state indicator.
        OTHER_UNITS_OFFSET_Y = 1,
        OTHER_UNITS_WIDTH = 3,
        MAX_OTHER_UNITS = 10,
        MESSAGE_COUNT = 3,
        MESSAGE_AGE = 30000; // The amount of time before a message gets deleted (in milliseconds).

    public static final int OVERLAY_INDEX = 100;
    public static final int FOREST_INDEX = 200;

    public static enum BorderType { COUNTRY, REGION };

    private int displayTileText = 0;
    private GeneralPath gridPath = null;
    private GeneralPath fog = new GeneralPath();

    // Debug variables:
    public boolean displayCoordinates = false;
    public boolean displayColonyValue = false;
    public Player displayColonyValuePlayer = null;

    public boolean debugShowMission = false;
    public boolean debugShowMissionInfo = false;
    
    private volatile boolean blinkingMarqueeEnabled;
    
    private Image cursorImage;
    
    private GrayLayer greyLayer;
    
    private java.util.Map<Unit, Integer> unitsOutForAnimation;
    private java.util.Map<Unit, JLabel> unitsOutForAnimationLabels;

    // roads
    private EnumMap<Direction, Point2D.Float> corners =
        new EnumMap<Direction, Point2D.Float>(Direction.class);
    private Stroke roadStroke = new BasicStroke(2);

    // borders
    private EnumMap<Direction, Point2D.Float> borderPoints =
        new EnumMap<Direction, Point2D.Float>(Direction.class);
    private EnumMap<Direction, Point2D.Float> controlPoints =
        new EnumMap<Direction, Point2D.Float>(Direction.class);
    private Stroke borderStroke = new BasicStroke(4);

    private Stroke gridStroke = new BasicStroke(1);

    /**
    * The constructor to use.
    *
    * @param freeColClient The main control class.
    * @param size The size of the GUI (= the entire screen if the app is displayed in full-screen).
    * @param lib The library of images needed to display certain things visually.
    */
    public GUI(FreeColClient freeColClient, Dimension size, ImageLibrary lib) {
        this.freeColClient = freeColClient;
        this.size = size;

        setImageLibrary(lib);
        
        unitsOutForAnimation = new HashMap<Unit, Integer>();
        unitsOutForAnimationLabels = new HashMap<Unit, JLabel>();

        inGame = false;
        logger.info("GUI created.");
        messages = new Vector<GUIMessage>(MESSAGE_COUNT);
        viewMode = new ViewMode(this);
        logger.info("Starting in Move Units View Mode");
        displayTileText = freeColClient.getClientOptions().getDisplayTileText();
        blinkingMarqueeEnabled = true;

        cursor = new net.sf.freecol.client.gui.TerrainCursor();

    }
    
    /**
     * Sets the ImageLibrary and calculates various items that depend
     * on tile size.
     *
     * @param lib an <code>ImageLibrary</code> value
     */
    private void setImageLibrary(ImageLibrary lib) {
        this.lib = lib;
        cursorImage = lib.getMiscImage(ImageLibrary.UNIT_SELECT);
        // ATTENTION: we assume that all base tiles have the same size
        tileHeight = lib.getTerrainImageHeight(null);
        halfHeight = tileHeight/2;
        tileWidth = lib.getTerrainImageWidth(null);
        halfWidth = tileWidth/2;

        int dx = tileWidth/16;
        int dy = tileHeight/16;
        int ddx = dx + dx/2;
        int ddy = dy + dy/2;

        // corners
        corners.put(Direction.N, new Point2D.Float(halfWidth, 0));
        corners.put(Direction.NE, new Point2D.Float(0.75f * tileWidth, 0.25f * tileHeight));
        corners.put(Direction.E, new Point2D.Float(tileWidth, halfHeight));
        corners.put(Direction.SE, new Point2D.Float(0.75f * tileWidth, 0.75f * tileHeight));
        corners.put(Direction.S, new Point2D.Float(halfWidth, tileHeight));
        corners.put(Direction.SW, new Point2D.Float(0.25f * tileWidth, 0.75f * tileHeight));
        corners.put(Direction.W, new Point2D.Float(0, halfHeight));
        corners.put(Direction.NW, new Point2D.Float(0.25f * tileWidth, 0.25f * tileHeight));

        // small corners
        controlPoints.put(Direction.N, new Point2D.Float(halfWidth, dy));
        controlPoints.put(Direction.E, new Point2D.Float(tileWidth - dx, halfHeight));
        controlPoints.put(Direction.S, new Point2D.Float(halfWidth, tileHeight - dy));
        controlPoints.put(Direction.W, new Point2D.Float(dx, halfHeight));
        // big corners
        controlPoints.put(Direction.SE, new Point2D.Float(halfWidth, tileHeight));
        controlPoints.put(Direction.NE, new Point2D.Float(tileWidth, halfHeight));
        controlPoints.put(Direction.SW, new Point2D.Float(0, halfHeight));
        controlPoints.put(Direction.NW, new Point2D.Float(halfWidth, 0));
        // small corners
        borderPoints.put(Direction.NW, new Point2D.Float(dx + ddx, halfHeight - ddy));
        borderPoints.put(Direction.N, new Point2D.Float(halfWidth - ddx, dy + ddy));
        borderPoints.put(Direction.NE, new Point2D.Float(halfWidth + ddx, dy + ddy));
        borderPoints.put(Direction.E, new Point2D.Float(tileWidth - dx - ddx, halfHeight - ddy));
        borderPoints.put(Direction.SE, new Point2D.Float(tileWidth - dx - ddx, halfHeight + ddy));
        borderPoints.put(Direction.S, new Point2D.Float(halfWidth + ddx, tileHeight - dy - ddy));
        borderPoints.put(Direction.SW, new Point2D.Float(halfWidth - ddx, tileHeight - dy - ddy));
        borderPoints.put(Direction.W, new Point2D.Float(dx + ddx, halfHeight + ddy));

        borderStroke = new BasicStroke(dy);
        roadStroke = new BasicStroke(dy/2);
        gridStroke = new BasicStroke(lib.getScalingFactor());

        fog.reset();
        fog.moveTo(halfWidth, 0);
        fog.lineTo(tileWidth, halfHeight);
        fog.lineTo(halfWidth, tileHeight);
        fog.lineTo(0, halfHeight);
        fog.closePath();

        updateMapDisplayVariables();
    }
    
    /**
     * Returns true if the given Unit is being animated.
     *
     * @param unit an <code>Unit</code> value
     * @return a <code>boolean</code> value
     */
    private boolean isOutForAnimation(final Unit unit) {
        return unitsOutForAnimation.containsKey(unit);
    }
    
    /**
     * Run some code with the given unit made invisible.
     * You can nest several of these method calls in order
     * to hide multiple units. There are no problems
     * related to nested calls with the same unit.
     * 
     * @param unit The unit to be hidden.
     * @param sourceTile a <code>Tile</code> value
     * @param r The code to be executed.
     */
    public void executeWithUnitOutForAnimation(final Unit unit,
                                               final Tile sourceTile,
                                               final OutForAnimationCallback r) {
        final JLabel unitLabel = enterUnitOutForAnimation(unit, sourceTile);
        try {
            r.executeWithUnitOutForAnimation(unitLabel);
        } finally {
            releaseUnitOutForAnimation(unit);
        }
    }
    
    /**
     * Describe <code>enterUnitOutForAnimation</code> method here.
     *
     * @param unit an <code>Unit</code> value
     * @param sourceTile a <code>Tile</code> value
     * @return a <code>JLabel</code> value
     */
    private JLabel enterUnitOutForAnimation(final Unit unit, final Tile sourceTile) {
        Integer i = unitsOutForAnimation.get(unit);
        if (i == null) {
            final JLabel unitLabel = getUnitLabel(unit);
            final Integer UNIT_LABEL_LAYER = JLayeredPane.DEFAULT_LAYER;

            i = 1;
            unitLabel.setLocation(getUnitLabelPositionInTile(unitLabel,
                    getTilePosition(sourceTile)));
            unitsOutForAnimationLabels.put(unit, unitLabel);
            freeColClient.getCanvas().add(unitLabel, UNIT_LABEL_LAYER, false);
        } else {
            i++;
        }
        unitsOutForAnimation.put(unit, i);
        return unitsOutForAnimationLabels.get(unit);
    }
    
    /**
     * Describe <code>releaseUnitOutForAnimation</code> method here.
     *
     * @param unit an <code>Unit</code> value
     */
    private void releaseUnitOutForAnimation(final Unit unit) {
        Integer i = unitsOutForAnimation.get(unit);
        if (i == null) {
            throw new IllegalStateException("Tried to release unit that was not out for animation"); 
        }
        if (i == 1) {
            unitsOutForAnimation.remove(unit);
            freeColClient.getCanvas().remove(unitsOutForAnimationLabels.remove(unit), false);
        } else {
            i--;
            unitsOutForAnimation.put(unit, i); 
        }
    }

    /**
     * Draw the unit's image and occupation indicator in one JLabel object.
     * @param unit The unit to be drawn
     * @return A JLabel object with the unit's image.
     */
    private JLabel getUnitLabel(Unit unit) {
        final Image unitImg = lib.getUnitImageIcon(unit).getImage();
        //final Image chipImg = getOccupationIndicatorImage(unit);

        final int width = halfWidth + unitImg.getWidth(null)/2;
        final int height = unitImg.getHeight(null);

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics g = img.getGraphics();

        final int unitX = (width - unitImg.getWidth(null)) / 2;
        g.drawImage(unitImg, unitX, 0, null);

        //final int chipX = ((int) (STATE_OFFSET_X * lib.getScalingFactor()));
        //final int chipY = (int) (((height / 2 + UNIT_OFFSET*lib.getScalingFactor())) - halfHeight);
        //g.drawImage(chipImg, chipX, chipY, null);

        final JLabel label = new JLabel(new ImageIcon(img));
        label.setSize(width, height);
        return label;
    }

    /**
     * Describe <code>updateMapDisplayVariables</code> method here.
     *
     */
    private void updateMapDisplayVariables() {
        // Calculate the amount of rows that will be drawn above the central Tile
        topSpace = (size.height - tileHeight) / 2;
        if ((topSpace % (halfHeight)) != 0) {
            topRows = topSpace / (halfHeight) + 2;
        } else {
            topRows = topSpace / (halfHeight) + 1;
        }
        bottomRows = topRows;
        leftSpace = (size.width - tileWidth) / 2;
        rightSpace = leftSpace;
    }
    
    /**
     *  Get the <code>View Mode</code> object
     * @return the current view mode.
     */
    public ViewMode getViewMode(){
        return viewMode;
    }
    
    /**
     * Starts the unit-selection-cursor blinking animation.
     */
    public void startCursorBlinking() {
        
        final FreeColClient theFreeColClient = freeColClient; 
        ActionListener taskPerformer = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (!blinkingMarqueeEnabled) return;
                if (getActiveUnit() != null && getActiveUnit().getTile() != null) {
                    //freeColClient.getCanvas().repaint(0, 0, getWidth(), getHeight());
                    theFreeColClient.getCanvas().refreshTile(getActiveUnit().getTile());            
                }               
            }
        };
        
        cursor.addActionListener(taskPerformer);
        
        cursor.startBlinking();
    }
    
    /**
     * Describe <code>getCursor</code> method here.
     *
     * @return a <code>TerrainCursor</code> value
     */
    public TerrainCursor getCursor(){
        return cursor;
    }
    
    /**
     * Describe <code>setSize</code> method here.
     *
     * @param size a <code>Dimension</code> value
     */
    public void setSize(Dimension size) {
        this.size = size;
        updateMapDisplayVariables();
    }
    
    /**
     * Describe <code>moveTileCursor</code> method here.
     *
     * @param direction a <code>Direction</code> value
     */
    public void moveTileCursor(Direction direction){
        Tile selectedTile = freeColClient.getGame().getMap().getTile(getSelectedTile());
        if(selectedTile != null){   
            Tile newTile = selectedTile.getNeighbourOrNull(direction);
            if(newTile != null)
                setSelectedTile(newTile.getPosition());
        }
        else{
            logger.warning("selectedTile is null");
        }
    }


    /**
     * Selects a tile, without clearing the orders of the first unit
     * contained.
     *
     * @param tile The <code>Tile</code> to select.
     * @see #setSelectedTile(Map.Position, boolean)
     */
    public void setSelectedTile(Tile tile) {
        if (tile != null) {
            setSelectedTile(tile.getPosition());
        }
    }

    /**
     * Selects the tile at the specified position, without clearing
     * the orders of the first unit contained.
     *
     * @param selectedTile The <code>Position</code> of the tile
     *                     to be selected.
     * @see #setSelectedTile(Map.Position, boolean)
     */
    public void setSelectedTile(Position selectedTile) {
        setSelectedTile(selectedTile, false);
    }

    /**
    * Selects the tile at the specified position. There are three
    * possible cases:
    *
    * <ol>
    *   <li>If there is a {@link Colony} on the {@link Tile} the
    *       {@link Canvas#showColonyPanel} will be invoked.
    *   <li>If the tile contains a unit that can become active, then
    *       that unit will be set as the active unit, and clear their
    *       goto orders if clearGoToOrders is <code>true</code>
    *   <li>If the two conditions above do not match, then the
    *       <code>selectedTile</code> will become the map focus.
    * </ol>
    *
    * If a unit is active and is located on the selected tile,
    * then nothing (except perhaps a map reposition) will happen.
    *
    * @param selectedTile The <code>Position</code> of the tile
    *                     to be selected.
    * @param clearGoToOrders Use <code>true</code> to clear goto orders
    *                        of the unit which is activated.
    * @see #getSelectedTile
    * @see #setActiveUnit
    * @see #setFocus(Map.Position)
    */
    public void setSelectedTile(Position selectedTile, boolean clearGoToOrders) {
        Game gameData = freeColClient.getGame();

        if (selectedTile != null && !gameData.getMap().isValid(selectedTile)) {
            return;
        }

        Position oldPosition = this.selectedTile;

        this.selectedTile = selectedTile;

        if (viewMode.getView() == ViewMode.MOVE_UNITS_MODE) {
            if (activeUnit == null ||
                (activeUnit.getTile() != null &&
                 !activeUnit.getTile().getPosition().equals(selectedTile))) {
                Tile t = gameData.getMap().getTile(selectedTile);
                if (t != null && t.getSettlement() != null) {
                    Canvas canvas = freeColClient.getCanvas();
                    Settlement s = t.getSettlement();
                    if (s instanceof Colony) {
                        if (s.getOwner().equals(freeColClient.getMyPlayer())) {
                            canvas.showColonyPanel((Colony) s);
                        } else if (FreeCol.isInDebugMode()) {
                            freeColClient.getInGameController().debugForeignColony(t);
                        }
                    } else if (s instanceof IndianSettlement) {
                        canvas.showIndianSettlementPanel((IndianSettlement) s);
                    } else {
                        throw new IllegalStateException("Bogus settlement");
                    }
                    return;
                }

                // else, just select a unit on the selected tile
                Unit unitInFront = getUnitInFront(gameData.getMap().getTile(selectedTile));
                if (unitInFront != null) {
                    setActiveUnit(unitInFront);
                    updateGotoPathForActiveUnit();
                } else {
                    setFocus(selectedTile);
                }
            } else if (activeUnit.getTile() != null &&
                    activeUnit.getTile().getPosition().equals(selectedTile)) {
                // Clear goto order when unit is already active
                if (clearGoToOrders && activeUnit.getDestination() != null) {
                    freeColClient.getInGameController().clearGotoOrders(activeUnit);
                    updateGotoPathForActiveUnit();
                }
            }
        }
        
        freeColClient.getActionManager().update();
        freeColClient.updateMenuBar();

        int x = 0, y = 0;
        MapControls mapControls = freeColClient.getCanvas().getMapControls();
        if (mapControls != null) {
            x = getWidth() - mapControls.getInfoPanelWidth();
            y = getHeight() - mapControls.getInfoPanelHeight();
        }
        freeColClient.getCanvas().repaint(x, y, getWidth(), getHeight());

        // Check if the gui needs to reposition:
        ClientOptions options = freeColClient.getClientOptions();
        if ((!onScreen(selectedTile)
             && options.getBoolean(ClientOptions.JUMP_TO_ACTIVE_UNIT))
            || options.getBoolean(ClientOptions.ALWAYS_CENTER)) {
            setFocus(selectedTile);
        } else {
            if (oldPosition != null) {
                freeColClient.getCanvas().refreshTile(oldPosition);
            }

            if (selectedTile != null) {
                freeColClient.getCanvas().refreshTile(selectedTile);
            }
        }
    }

    /**
     * Describe <code>showColonyPanel</code> method here.
     *
     * @param selectedTile a <code>Position</code> value
     */
    public void showColonyPanel(Position selectedTile) {
        Game gameData = freeColClient.getGame();

        if (selectedTile != null && !gameData.getMap().isValid(selectedTile)) {
            return;
        }

        if (viewMode.getView() == ViewMode.MOVE_UNITS_MODE) {
            Tile t = gameData.getMap().getTile(selectedTile);
            if (t != null && t.getSettlement() != null && t.getSettlement() instanceof Colony) {
                if (t.getSettlement().getOwner().equals(freeColClient.getMyPlayer())) {
                    setFocus(selectedTile);
                    stopBlinking();
                    freeColClient.getCanvas().showColonyPanel((Colony) t.getSettlement());
                }
            }
        }
    }
    
    /**
     * Describe <code>restartBlinking</code> method here.
     *
     */
    public void restartBlinking() {
        blinkingMarqueeEnabled = true;
    }
    
    /**
     * Describe <code>stopBlinking</code> method here.
     *
     */
    public void stopBlinking() {
        blinkingMarqueeEnabled = false;
    }

    /**
    * Gets the unit that should be displayed on the given tile.
    *
    * @param unitTile The <code>Tile</code>.
    * @return The <code>Unit</code> or <i>null</i> if no unit applies.
    */
    public Unit getUnitInFront(Tile unitTile) {
        if (unitTile == null || unitTile.getUnitCount() <= 0) {
            return null;
        }

        if (activeUnit != null && activeUnit.getTile() == unitTile) {
            return activeUnit;
        } else {
            if (unitTile.getSettlement() == null) {
                Unit bestDefendingUnit = null;
                if (activeUnit != null) {
                    bestDefendingUnit = unitTile.getDefendingUnit(activeUnit);
                    if (bestDefendingUnit != null) {
                        return bestDefendingUnit;
                    }
                }
                
                Unit movableUnit = unitTile.getMovableUnit();
                if (movableUnit != null && movableUnit.getLocation() == movableUnit.getTile()) {
                    return movableUnit;
                } else {
                    Unit bestPick = null;
                    Iterator<Unit> unitIterator = unitTile.getUnitIterator();
                    while (unitIterator.hasNext()) {
                        Unit u = unitIterator.next();
                        if (bestPick == null || bestPick.getMovesLeft() < u.getMovesLeft()) {
                            bestPick = u;
                        }
                    }
                    
                    return bestPick;
                }
            } else {
                return null;
            }
        }
    }

    
    /**
    * Gets the selected tile.
    *
    * @return The <code>Position</code> of that tile.
    * @see #setSelectedTile(Map.Position)
    */
    public Position getSelectedTile() {
        return selectedTile;
    }


    /**
    * Gets the active unit.
    *
    * @return The <code>Unit</code>.
    * @see #setActiveUnit
    */
    public Unit getActiveUnit() {
        return activeUnit;
    }


    /**
    * Sets the active unit. Invokes {@link #setSelectedTile(Map.Position)} if the
    * selected tile is another tile than where the <code>activeUnit</code>
    * is located.
    *
    * @param activeUnit The new active unit.
    * @see #setSelectedTile(Map.Position)
    */
    public void setActiveUnit(Unit activeUnit) {
        // Don't select a unit with zero moves left. -sjm
        // The user might what to check the status of a unit - SG
        /*if ((activeUnit != null) && (activeUnit.getMovesLeft() == 0)) {
            freeColClient.getInGameController().nextActiveUnit();
            return;
        }*/
        
        if (activeUnit != null && activeUnit.getOwner() != freeColClient.getMyPlayer()) {
            freeColClient.getCanvas().repaint(0, 0, getWidth(), getHeight());
            return;
        }

        if (activeUnit != null && activeUnit.getTile() == null) {
            activeUnit = null;
        }

        this.activeUnit = activeUnit;

        if (activeUnit != null) {
            if (freeColClient.getGame().getCurrentPlayer() == freeColClient.getMyPlayer()) {
                if (activeUnit.getState() != UnitState.ACTIVE) {
                    freeColClient.getInGameController().clearOrders(activeUnit);
                }
            } else {
                freeColClient.getInGameController().clearGotoOrders(activeUnit);
            }
        }
        updateGotoPathForActiveUnit();

        // The user activated a unit
        if(viewMode.getView() == ViewMode.VIEW_TERRAIN_MODE && activeUnit != null)
            viewMode.changeViewMode(ViewMode.MOVE_UNITS_MODE);

        //if (activeUnit != null && !activeUnit.getTile().getPosition().equals(selectedTile)) {
        if (activeUnit != null) {
            setSelectedTile(activeUnit.getTile().getPosition());
        } else {
            freeColClient.getActionManager().update();
            freeColClient.updateMenuBar();

            int x = 0, y = 0;
            MapControls mapControls = freeColClient.getCanvas().getMapControls();
            if (mapControls != null) {
                x = getWidth() - mapControls.getInfoPanelWidth();
                y = getHeight() - mapControls.getInfoPanelHeight();
            }
            freeColClient.getCanvas().repaint(x, y, getWidth(), getHeight());
        }
    }

    /**
     * Centers the map on the selected unit.
     */
    public void centerActiveUnit() {
        if (activeUnit != null && activeUnit.getTile() != null) {
            setFocus(activeUnit.getTile().getPosition());
        }
    }


    /**
    * Gets the focus of the map. That is the center tile of the displayed
    * map.
    *
    * @return The <code>Position</code> of the center tile of the
    *         displayed map
    * @see #setFocus(Map.Position)
    */
    public Position getFocus() {
        return focus;
    }


    /**
    * Sets the focus of the map.
    *
    * @param focus The <code>Position</code> of the center tile of the
    *             displayed map.
    * @see #getFocus
    */
    public void setFocus(Position focus) {
        this.focus = focus;

        forceReposition();
        freeColClient.getCanvas().repaint(0, 0, getWidth(), getHeight());
    }

    /**
     * Sets the focus of the map but offset to the left or right so that
     * the focus position can still be visible when a popup is raised.
     * If successful, the supplied position will either be at the center of
     * the left or right half of the map.
     *
     * @param tilePos The <code>Position</code> of a tile of the
     *                displayed map.
     * @return Positive if the focus is on the right hand side, negative
     *         if on the left, zero on failure.
     * @see #getFocus
     */
    public int setOffsetFocus(Position tilePos) {
        int where = 0;
        if (tilePos != null) {
            positionMap(tilePos);
            Map map = freeColClient.getGame().getMap();
            if (leftColumn == 0) {
                where = -1; // At left edge already
            } else if (rightColumn == map.getWidth() - 1) {
                where = 1; // At right edge already
            } else { // Move focus left 1/4 screen
                int x = tilePos.getX() - (tilePos.getX() - leftColumn) / 2;
                tilePos = new Position(x, tilePos.getY());
                where = 1;
            }
            setFocus(tilePos);
        }
        return where;
    }

    /**
    * Sets the focus of the map and repaints the screen immediately.
    *
    * @param focus The <code>Position</code> of the center tile of the
    *             displayed map.
    * @see #getFocus
    */
    public void setFocusImmediately(Position focus) {
        this.focus = focus;

        forceReposition();
        freeColClient.getCanvas().paintImmediately(0, 0, getWidth(), getHeight());
    }

    /**
    * Sets the focus of the map.
    *
    * @param x The x-coordinate of the center tile of the
    *         displayed map.
    * @param y The x-coordinate of the center tile of the
    *         displayed map.
    * @see #getFocus
    */
    public void setFocus(int x, int y) {
        setFocus(new Map.Position(x,y));
    }


    /**
     * Gets the amount of message that are currently being displayed on this GUI.
     * @return The amount of message that are currently being displayed on this GUI.
     */
    public int getMessageCount() {
        return messages.size();
    }


    /**
     * Gets the message at position 'index'. The message at position 0 is the oldest
     * message and is most likely to be removed during the next call of removeOldMessages().
     * The higher the index of a message, the more recently it was added.
     *
     * @param index The index of the message to return.
     * @return The message at position 'index'.
     */
    public GUIMessage getMessage(int index) {
        return messages.get(index);
    }


    /**
    * Adds a message to the list of messages that need to be displayed on the GUI.
    * @param message The message to add.
    */
    public synchronized void addMessage(GUIMessage message) {
        if (getMessageCount() == MESSAGE_COUNT) {
            messages.remove(0);
        }
        messages.add(message);

        freeColClient.getCanvas().repaint(0, 0, getWidth(), getHeight());
    }


    /**
     * Removes all the message that are older than MESSAGE_AGE.
     * @return 'true' if at least one message has been removed, 'false' otherwise.
     * This can be useful to see if it is necessary to refresh the screen.
     */
    public synchronized boolean removeOldMessages() {
        long currentTime = new Date().getTime();
        boolean result = false;

        int i = 0;
        while (i < getMessageCount()) {
            long messageCreationTime = getMessage(i).getCreationTime().getTime();
            if ((currentTime - messageCreationTime) >= MESSAGE_AGE) {
                result = true;
                messages.remove(i);
            } else {
                i++;
            }
        }

        return result;
    }


    /**
     * Returns the width of this GUI.
     * @return The width of this GUI.
     */
    public int getWidth() {
        return size.width;
    }


    /**
     * Returns the height of this GUI.
     * @return The height of this GUI.
     */
    public int getHeight() {
        return size.height;
    }


    /**
     * Displays this GUI onto the given Graphics2D.
     * @param g The Graphics2D on which to display this GUI.
     */
    public void display(Graphics2D g) {
        if ((freeColClient.getGame() != null)
                && (freeColClient.getGame().getMap() != null)
                && (focus != null)
                && inGame) {
            removeOldMessages();
            displayMap(g);
        } else {
            if (freeColClient.isMapEditor()) {
                g.setColor(Color.black);
                g.fillRect(0, 0, size.width, size.height);                
            } else {
                Image bgImage = ResourceManager.getImage("CanvasBackgroundImage", size);
                if (bgImage != null) {
                    g.drawImage(bgImage, 0, 0, freeColClient.getCanvas());
                    
                    // Show version on initial screen
                    String versionStr = "v. " + FreeCol.getVersion();
                    Font oldFont = g.getFont();
                    Color oldColor = g.getColor();
                    Font newFont = oldFont.deriveFont(Font.BOLD);
                    TextLayout layout = new TextLayout(versionStr, newFont, g.getFontRenderContext());
                   
                    Rectangle2D bounds = layout.getBounds();
                    float x = getWidth() - (float) bounds.getWidth() - 5;
                    float y = getHeight() - (float) bounds.getHeight();
                    g.setColor(Color.white);
                    layout.draw(g, x, y);
                    
                    // restore old values
                    g.setFont(oldFont);
                    g.setColor(oldColor);
                    
                } else {
                    g.setColor(Color.black);
                    g.fillRect(0, 0, size.width, size.height);
                }
            }
        }
    }


    /**
     * Returns the amount of columns that are to the left of the Tile
     * that is displayed in the center of the Map.
     * @return The amount of columns that are to the left of the Tile
     * that is displayed in the center of the Map.
     */
    private int getLeftColumns() {
        return getLeftColumns(focus.getY());
    }


    /**
     * Returns the amount of columns that are to the left of the Tile
     * with the given y-coordinate.
     * @param y The y-coordinate of the Tile in question.
     * @return The amount of columns that are to the left of the Tile
     * with the given y-coordinate.
     */
    private int getLeftColumns(int y) {
        int leftColumns;

        if ((y % 2) == 0) {
            leftColumns = leftSpace / tileWidth + 1;
            if ((leftSpace % tileWidth) > 32) {
                leftColumns++;
            }
        } else {
            leftColumns = leftSpace / tileWidth + 1;
            if ((leftSpace % tileWidth) == 0) {
                leftColumns--;
            }
        }

        return leftColumns;
    }


    /**
     * Returns the amount of columns that are to the right of the Tile
     * that is displayed in the center of the Map.
     * @return The amount of columns that are to the right of the Tile
     * that is displayed in the center of the Map.
     */
    private int getRightColumns() {
        return getRightColumns(focus.getY());
    }


    /**
     * Returns the amount of columns that are to the right of the Tile
     * with the given y-coordinate.
     * @param y The y-coordinate of the Tile in question.
     * @return The amount of columns that are to the right of the Tile
     * with the given y-coordinate.
     */
    private int getRightColumns(int y) {
        int rightColumns;

        if ((y % 2) == 0) {
            rightColumns = rightSpace / tileWidth + 1;
            if ((rightSpace % tileWidth) == 0) {
                rightColumns--;
            }
        } else {
            rightColumns = rightSpace / tileWidth + 1;
            if ((rightSpace % tileWidth) > 32) {
                rightColumns++;
            }
        }

        return rightColumns;
    }


    /**
     * Position the map so that the Tile at the focus location is
     * displayed at the center.
     */
    private void positionMap() {
        if (focus != null) positionMap(focus);
    }

    /**
     * Position the map so that the supplied location is
     * displayed at the center.
     *
     * @param pos The position to center at.
     */
    private void positionMap(Position pos) {
        Game gameData = freeColClient.getGame();
        
        int x = pos.getX(),
            y = pos.getY();
        int leftColumns = getLeftColumns(),
            rightColumns = getRightColumns();

        /*
        PART 1
        ======
        Calculate: bottomRow, topRow, bottomRowY, topRowY
        This will tell us which rows need to be drawn on the screen (from
        bottomRow until and including topRow).
        bottomRowY will tell us at which height the bottom row needs to be
        drawn.
        */

        if (y < topRows) {
            // We are at the top of the map
            bottomRow = (size.height / (halfHeight)) - 1;
            if ((size.height % (halfHeight)) != 0) {
                bottomRow++;
            }
            topRow = 0;
            bottomRowY = bottomRow * (halfHeight);
            topRowY = 0;
        } else if (y >= (gameData.getMap().getHeight() - bottomRows)) {
            // We are at the bottom of the map
            bottomRow = gameData.getMap().getHeight() - 1;

            topRow = size.height / (halfHeight);
            if ((size.height % (halfHeight)) > 0) {
                topRow++;
            }
            topRow = gameData.getMap().getHeight() - topRow;

            bottomRowY = size.height - tileHeight;
            topRowY = bottomRowY - (bottomRow - topRow) * (halfHeight);
        } else {
            // We are not at the top of the map and not at the bottom
            bottomRow = y + bottomRows;
            topRow = y - topRows;
            bottomRowY = topSpace + (halfHeight) * bottomRows;
            topRowY = topSpace - topRows * (halfHeight);
        }

        /*
        PART 2
        ======
        Calculate: leftColumn, rightColumn, leftColumnX
        This will tell us which columns need to be drawn on the screen (from
        leftColumn until and including rightColumn).
        leftColumnX will tell us at which x-coordinate the left column needs
        to be drawn (this is for the Tiles where y%2==0; the others should be
        halfWidth more to the right).
        */

        if (x < leftColumns) {
            // We are at the left side of the map
            leftColumn = 0;

            rightColumn = size.width / tileWidth - 1;
            if ((size.width % tileWidth) > 0) {
                rightColumn++;
            }

            leftColumnX = 0;
        } else if (x >= (gameData.getMap().getWidth() - rightColumns)) {
            // We are at the right side of the map
            rightColumn = gameData.getMap().getWidth() - 1;

            leftColumn = size.width / tileWidth;
            if ((size.width % tileWidth) > 0) {
                leftColumn++;
            }

            leftColumnX = size.width - tileWidth - halfWidth -
                leftColumn * tileWidth;
            leftColumn = rightColumn - leftColumn;
        } else {
            // We are not at the left side of the map and not at the right side
            leftColumn = x - leftColumns;
            rightColumn = x + rightColumns;
            leftColumnX = (size.width - tileWidth) / 2 - leftColumns * tileWidth;
        }
    }

    
    /**
     * Describe <code>displayGotoPath</code> method here.
     *
     * @param g a <code>Graphics2D</code> value
     * @param gotoPath a <code>PathNode</code> value
     */
    private void displayGotoPath(Graphics2D g, PathNode gotoPath) {
        if (gotoPath != null) {
            PathNode temp = gotoPath;
            Font font = ResourceManager.getFont("NormalFont", 12f);
            while (temp != null) {
                Point p = getTilePosition(temp.getTile());
                if (p != null) {
                    Tile tile = temp.getTile();
                    Image image;
                    final Color textColor; 
                    if (temp.getTurns() == 0) {
                        g.setColor(Color.GREEN);                        
                        image = getPathImage(activeUnit);
                        if (activeUnit != null 
                                && tile.isExplored()
                                && activeUnit.isNaval()
                                && tile.isLand() 
                                && (tile.getColony() == null || tile.getColony().getOwner() != activeUnit.getOwner())) {
                            image = getPathImage(activeUnit.getFirstUnit());
                        }
                        textColor = Color.BLACK;
                    } else {
                        g.setColor(Color.RED);
                        image = getPathNextTurnImage(activeUnit);
                        if (activeUnit != null
                                && tile.isExplored()
                                && activeUnit.isNaval()
                                && tile.isLand() 
                                && (tile.getColony() == null || tile.getColony().getOwner() != activeUnit.getOwner())) {
                            image = getPathNextTurnImage(activeUnit.getFirstUnit());
                        }
                        textColor = Color.WHITE;
                    }                
                    g.translate(p.x, p.y);
                    if (image != null) {
                        centerImage(g, image);
                    } else {
                        g.fillOval(halfWidth, halfHeight, 10, 10);
                        g.setColor(Color.BLACK);
                        g.drawOval(halfWidth, halfHeight, 10, 10);
                    }                
                    if (temp.getTurns() > 0) {
                        Image stringImage = createStringImage(g, Integer.toString(temp.getTurns()),
                                                              textColor, font);
                        centerImage(g, stringImage);
                    }
                    g.translate(-p.x, -p.y);
                }                    
                temp = temp.next;
            }
        }
    }

    /**
     * Centers the given Image on the tile.
     *
     * @param g a <code>Graphics2D</code> value
     * @param image an <code>Image</code> value
     */
    private void centerImage(Graphics2D g, Image image) {
        g.drawImage(image,
                    (tileWidth - image.getWidth(null))/2,
                    (tileHeight - image.getHeight(null))/2,
                    null);
    }

    /**
     * Displays the Map onto the given Graphics2D object. The Tile at
     * location (x, y) is displayed in the center.
     * @param g The Graphics2D object on which to draw the Map.
     */
    private void displayMap(Graphics2D g) {
        AffineTransform originTransform = g.getTransform();
        Rectangle clipBounds = g.getClipBounds();
        Map map = freeColClient.getGame().getMap();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);

        /*
        PART 1
        ======
        Position the map if it is not positioned yet.
        */

        if (bottomRow < 0) {
            positionMap();
        }

        /*
        PART 1a
        =======
        Determine which tiles need to be redrawn.
        */
        int firstRow = (clipBounds.y - topRowY) / (halfHeight) - 1;
        int clipTopY = topRowY + firstRow * (halfHeight);
        firstRow = topRow + firstRow;

        int firstColumn = (clipBounds.x - leftColumnX) / tileWidth - 1;
        int clipLeftX = leftColumnX + firstColumn * tileWidth;
        firstColumn = leftColumn + firstColumn;

        int lastRow = (clipBounds.y + clipBounds.height - topRowY) / (halfHeight);
        lastRow = topRow + lastRow;

        int lastColumn = (clipBounds.x + clipBounds.width - leftColumnX) / tileWidth;
        lastColumn = leftColumn + lastColumn;

        /*
        PART 1b
        =======
        Create a GeneralPath to draw the grid with, if needed.
        */
        if (freeColClient.getClientOptions().getBoolean(ClientOptions.DISPLAY_GRID)) {
            gridPath = new GeneralPath();
            gridPath.moveTo(0, 0);
            int nextX = halfWidth;
            int nextY = -halfHeight;

            for (int i = 0; i <= ((lastColumn - firstColumn) * 2 + 1); i++) {
                gridPath.lineTo(nextX, nextY);
                nextX += halfWidth;
                nextY = (nextY == 0 ? -halfHeight : 0);
            }
        }

        /*
        PART 2
        ======
        Display the Tiles and the Units.
        */

        g.setColor(Color.black);
        g.fillRect(clipBounds.x, clipBounds.y, clipBounds.width, clipBounds.height);

        /*
        PART 2a
        =======
        Display the base Tiles
        */
        g.translate(clipLeftX, clipTopY);
        AffineTransform baseTransform = g.getTransform();
        AffineTransform rowTransform = null;

        // Row per row; start with the top modified row
        for (int row = firstRow; row <= lastRow; row++) {
            rowTransform = g.getTransform();
            if (row % 2 == 1) {
                g.translate(halfWidth, 0);
            }

            // Column per column; start at the left side to display the tiles.
            for (int column = firstColumn; column <= lastColumn; column++) {
                Tile tile = map.getTile(column, row);
                displayBaseTile(g, map, tile, true);
                g.translate(tileWidth, 0);
            }
            g.setTransform(rowTransform);
            g.translate(0, halfHeight);
        }
        g.setTransform(baseTransform);

        /*
        PART 2b
        =======
        Display the Tile overlays and Units
        */

        List<Unit> units = new ArrayList<Unit>();
        List<AffineTransform> unitTransforms = new ArrayList<AffineTransform>();
        List<Settlement> settlements = new ArrayList<Settlement>();
        List<AffineTransform> settlementTransforms = new ArrayList<AffineTransform>();
        
        int colonyLabels = freeColClient.getClientOptions().getInteger(ClientOptions.COLONY_LABELS);
        boolean withNumbers = (colonyLabels == ClientOptions.COLONY_LABELS_CLASSIC);
        // Row per row; start with the top modified row
        for (int row = firstRow; row <= lastRow; row++) {
            rowTransform = g.getTransform();
            if (row % 2 == 1) {
                g.translate(halfWidth, 0);
            }

            if (freeColClient.getClientOptions().getBoolean(ClientOptions.DISPLAY_GRID)) {
                // Display the grid.
                g.translate(0, halfHeight);
                g.setStroke(gridStroke);
                g.setColor(Color.BLACK);
                g.draw(gridPath);
                g.translate(0, -halfHeight);
            }

            // Column per column; start at the left side to display the tiles.
            for (int column = firstColumn; column <= lastColumn; column++) {
                Tile tile = map.getTile(column, row);
                    
                // paint full borders
                paintBorders(g, tile, BorderType.COUNTRY, true);
                // Display the Tile overlays:
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                   RenderingHints.VALUE_ANTIALIAS_OFF);
                displayTileOverlays(g, map, tile, true, withNumbers);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                   RenderingHints.VALUE_ANTIALIAS_ON);
                // paint transparent borders
                paintBorders(g, tile, BorderType.COUNTRY, false);

                if (viewMode.displayTileCursor(tile)) {
                    drawCursor(g);
                }
                // check for units
                if (tile != null) {
                    Unit unitInFront = getUnitInFront(tile);
                    if (unitInFront != null && !isOutForAnimation(unitInFront)) {
                        units.add(unitInFront);
                        unitTransforms.add(g.getTransform());
                    }
                    // check for settlements
                    Settlement settlement = tile.getSettlement();
                    if (settlement != null) {
                        settlements.add(settlement);
                        settlementTransforms.add(g.getTransform());
                    }
                }
                g.translate(tileWidth, 0);
            }

            g.setTransform(rowTransform);
            g.translate(0, halfHeight);
        }
        g.setTransform(baseTransform);

        /*
        PART 2c
        =======
        Display units
        */
        if (units.size() > 0) {
            g.setColor(Color.BLACK);
            final Image im = lib.getMiscImage(ImageLibrary.DARKNESS);
            for (int index = 0; index < units.size(); index++) {
                final Unit unit = units.get(index);
                g.setTransform(unitTransforms.get(index));
                if (unit.isUndead()) {
                    // display darkness
                    centerImage(g, im);
                }
                displayUnit(g, unit);
            }
            g.setTransform(baseTransform);
        }

        /*
        PART 3
        ======
        Display the colony names.
        */
        if (settlements.size() > 0 && colonyLabels != ClientOptions.COLONY_LABELS_NONE) {
            for (int index = 0; index < settlements.size(); index++) {
                final Settlement settlement = settlements.get(index);
                String name = Messages.message(settlement.getNameFor(freeColClient.getMyPlayer()));
                if (name != null) {
                    Color backgroundColor = lib.getColor(settlement.getOwner());
                    Font font = ResourceManager.getFont("NormalFont", 18f);
                    int yOffset = lib.getSettlementImage(settlement).getHeight(null) + 1;
                    g.setTransform(settlementTransforms.get(index));
                    switch(colonyLabels) {
                    case ClientOptions.COLONY_LABELS_CLASSIC:
                        Image stringImage = createStringImage(g, name, backgroundColor, font);
                        g.drawImage(stringImage,
                                    (tileWidth - stringImage.getWidth(null))/2 + 1,
                                    yOffset, null);
                        break;
                    case ClientOptions.COLONY_LABELS_MODERN:
                        backgroundColor = new Color(backgroundColor.getRed(), backgroundColor.getGreen(),
                                                    backgroundColor.getBlue(), 128);

                        Image nameImage = createLabel(g, name, font, backgroundColor);
                        if (nameImage != null) {
                            int spacing = 2;
                            if (settlement instanceof Colony) {
                                String size = Integer.toString(((Colony) settlement).getUnitCount());
                                int bonusProduction = ((Colony) settlement).getProductionBonus();
                                String bonus = bonusProduction > 0 ? "+" + bonusProduction
                                    : Integer.toString(bonusProduction);
                                Image sizeImage = createLabel(g, size, font, backgroundColor);
                                Image bonusImage = createLabel(g, bonus, font, backgroundColor);
                                int width = nameImage.getWidth(null) + sizeImage.getWidth(null)
                                    + bonusImage.getWidth(null) + 2 * spacing;
                                int labelOffset = (tileWidth - width)/2;
                                g.drawImage(sizeImage, labelOffset, yOffset, null);
                                labelOffset += sizeImage.getWidth(null) + spacing;
                                g.drawImage(nameImage, labelOffset, yOffset, null);
                                labelOffset += nameImage.getWidth(null) + spacing;
                                g.drawImage(bonusImage, labelOffset, yOffset, null);
                            } else {
                                int labelOffset = (tileWidth - nameImage.getWidth(null))/2;
                                g.drawImage(nameImage, labelOffset, yOffset, null);
                            }
                            break;
                        }
                    }
                }
            }
        }

        g.setTransform(originTransform);

        /*
        PART 4
        ======
        Display goto path
        */

        displayGotoPath(g, currentPath);
        displayGotoPath(g, gotoPath);
        
        /*
        PART 5
        ======
        Grey out the map if it is not my turn (and a multiplayer game).
         */
        Canvas canvas = freeColClient.getCanvas();
        
        if (!freeColClient.isMapEditor()
            && freeColClient.getGame() != null
            && freeColClient.getMyPlayer() != freeColClient.getGame().getCurrentPlayer()) {
            
            if (greyLayer == null) {
                greyLayer = new GrayLayer(lib);
            }
            if (greyLayer.getParent() == null) { // Not added to the canvas yet.
                canvas.add(greyLayer, JLayeredPane.DEFAULT_LAYER, false);
                canvas.moveToFront(greyLayer);
            }
                        
            greyLayer.setBounds(0,0,canvas.getSize().width, canvas.getSize().height);
            greyLayer.setPlayer(freeColClient.getGame().getCurrentPlayer());
            
        }
        else {
            if (greyLayer != null && greyLayer.getParent() != null) {
                canvas.remove(greyLayer, false);
            }
        }

        /*
        PART 6
        ======
        Display the messages, if there are any.
        */

        if (getMessageCount() > 0) {
            // Don't edit the list of messages while I'm drawing them.
            synchronized (this) {
                Font font = ResourceManager.getFont("NormalFont", 12f);
                GUIMessage message = getMessage(0);
                Image si = createStringImage(g, message.getMessage(), message.getColor(), font);

                int yy = size.height - 300 - getMessageCount() * si.getHeight(null);
                int xx = 40;

                for (int i = 1; i < getMessageCount(); i++) {
                    message = getMessage(i);
                    g.drawImage(createStringImage(g, message.getMessage(), message.getColor(), font),
                                xx, yy, null);
                    yy += si.getHeight(null);
                }
            }
        }

        Image decoration = ResourceManager.getImage("menuborder.shadow.s.image");
        int width = decoration.getWidth(null);
        for (int index = 0; index < size.width; index += width) {
            g.drawImage(decoration, index, 0, null);
        }
        decoration = ResourceManager.getImage("menuborder.shadow.sw.image");
        g.drawImage(decoration, 0, 0, null);
        decoration = ResourceManager.getImage("menuborder.shadow.se.image");
        g.drawImage(decoration, size.width - decoration.getWidth(null), 0, null);
        
    }
    
    /**
     * Gets an image to represent the path of the given <code>Unit</code>.
     * 
     * @param u The <code>Unit</code>
     * @return The <code>Image</code>.
     */
    private Image getPathImage(Unit u) {
        if (u == null) {
            return null;
        } else {
            return ResourceManager.getImage("path." + u.getPathTypeImage() + ".image");
        }
    }
    
    /**
     * Gets an image to represent the path of the given <code>Unit</code>.
     * 
     * @param u The <code>Unit</code>
     * @return The <code>Image</code>.
     *
    private Image getPathIllegalImage(Unit u) {
        if (u == null || u.isNaval()) {
            return (Image) UIManager.get("path.naval.illegal.image");
        } else if (u.isMounted()) {
            return (Image) UIManager.get("path.horse.illegal.image");
        } else if (u.getType() == Unit.WAGON_TRAIN || u.getType() == Unit.TREASURE_TRAIN || u.getType() == Unit.ARTILLERY || u.getType() == Unit.DAMAGED_ARTILLERY) {
            return (Image) UIManager.get("path.wagon.illegal.image");
        } else {
            return (Image) UIManager.get("path.foot.illegal.image");
        }
    }
    */
    
    /**
     * Gets an image to represent the path of the given <code>Unit</code>.
     * 
     * @param u The <code>Unit</code>
     * @return The <code>Image</code>.
     */
    private Image getPathNextTurnImage(Unit u) {
        if (u == null) {
            return null;
        } else {
            return ResourceManager.getImage("path." + u.getPathTypeImage() + ".nextTurn.image");
        }
    }

    /**
     * Creates an image with a string of a given color and with 
     * a black border around the glyphs.
     *
     * @param g A <code>Graphics</code>-object for getting a
     *       <code>Font</code>.
     * @param nameString The <code>String</code> to make an image of.
     * @param color The <code>Color</code> to use when displaying 
     *       the <code>nameString</code>.
     * @param font a <code>Font</code> value
     * @return The image that was created.
     */
    public Image createStringImage(Graphics g, String nameString, Color color, Font font) {
        if (color == null) {
            logger.warning("createStringImage called with color null");
            color = Color.WHITE;
        }

        // Lookup in the cache if the image has been generated already
        String key = "dynamic.stringImage." + nameString
            + "." + font.getFontName().replace(' ', '-')
            + "." + Integer.toString(font.getSize())
            + "." + Integer.toHexString(color.getRGB());
        Image image = (Image) ResourceManager.getImage(key);//, lib.getScalingFactor());
        if (image != null) {
            return image;
        }

        // create an image of the appropriate size
        FontMetrics fontMetrics = g.getFontMetrics(font);
        BufferedImage bi = new BufferedImage(fontMetrics.stringWidth(nameString) + 4, 
                                             fontMetrics.getMaxAscent() + fontMetrics.getMaxDescent(),
                                             BufferedImage.TYPE_INT_ARGB);
        // draw the string with selected color
        Graphics2D big = bi.createGraphics();
        big.setColor(color);
        big.setFont(font);
        big.drawString(nameString, 2, fontMetrics.getMaxAscent());

        // draw the border around letters
        int textColor = color.getRGB();
        int borderColor = getStringBorderColor(color).getRGB();
        for (int biX = 0; biX < bi.getWidth(); biX++) {
            for (int biY = 0; biY < bi.getHeight(); biY++) {
                int r = bi.getRGB(biX, biY);

                if (r == textColor) {
                    continue;
                }

                for (int cX = -1; cX <= 1; cX++) {
                    for (int cY = -1; cY <= 1; cY++) {
                        if (biX+cX >= 0 && biY+cY >= 0
                            && biX+cX < bi.getWidth() && biY+cY < bi.getHeight()
                            && bi.getRGB(biX + cX, biY + cY) == textColor) {
                            bi.setRGB(biX, biY, borderColor);
                            continue;
                        }
                    }
                }
            }
        }
        ResourceManager.addGameMapping(key, new ImageResource(bi));
        return (Image) ResourceManager.getImage(key);//, lib.getScalingFactor());
    }

    /**
     * Creates an image with a string of a given color and with 
     * a black border around the glyphs.
     *
     * @param c A <code>JComponent</code>-object for getting a
     *       <code>Font</code>.
     * @param g A <code>Graphics</code>-object for getting a
     *       <code>Font</code>.
     * @param text The <code>String</code> to make an image of.
     * @param font The font with which to render.
     * @param color The <code>Color</code> to use when displaying 
     *       the <code>text</code>.
     * @return The image that was created.
     */
    /*
    private BufferedImage createStringImage(JComponent c, Graphics g, String text, Font font, Color color) {
        if (color == null) {
            logger.warning("createStringImage called with color null");
            color = Color.WHITE;
        }

        // Lookup in the cache if the image has been generated already
        String key = text + font.getFontName() + color.getRGB();
        BufferedImage bi = (BufferedImage) ResourceManager.getImage(key, lib.getScalingFactor());
        if (bi != null) {
            return bi;
        }

        // create an image of the appropriate size
        FontRenderContext context = new FontRenderContext(null, true, true);
        GlyphVector glyphs = font.createGlyphVector(context, text);
        FontMetrics metrics = (c != null) ? c.getFontMetrics(font) : g.getFontMetrics(font);
        Rectangle bounds = glyphs.getPixelBounds(context, 3.0f, (float)metrics.getMaxAscent());
        bi = new BufferedImage(bounds.width + 6, metrics.getMaxAscent() + 6, BufferedImage.TYPE_INT_ARGB);

        // set up the graphics
        Color outlineColor = getStringBorderColor(color);
        Graphics2D g2d = bi.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        // draw the string
        Shape textShape = glyphs.getOutline(3, metrics.getMaxAscent());
//        g2d.setColor(Color.PINK);
//        g2d.fillRect(-1000,-1000,2000,2000);
//        g2d.setColor(Color.CYAN);
//        g2d.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.setColor(outlineColor);
        g2d.draw(textShape);
        g2d.setColor(color);
        g2d.fill(textShape);

        ResourceManager.getGameMapping().add(key, new ImageResource(bi));
        return bi;
    }
    */
    
    /**
     * Creates an Image that shows the given text centred on a
     * translucent rounded rectangle with the given color.
     *
     * @param g a <code>Graphics2D</code> value
     * @param text a <code>String</code> value
     * @param font a <code>Font</code> value
     * @param backgroundColor a <code>Color</code> value
     * @return an <code>Image</code> value
     */
    private Image createLabel(Graphics2D g, String text, Font font, Color backgroundColor) {
        String key = "dynamic.label." + text
            + "." + font.getName().replace(' ', '-')
            + "." + Integer.toHexString(backgroundColor.getRGB());
        Image image = (Image) ResourceManager.getImage(key, lib.getScalingFactor());
        if (image != null) {
            return image;
        }
        TextLayout label = new TextLayout(text, font, g.getFontRenderContext());
        int padding = 10;
        int width = (int) label.getBounds().getWidth() + padding;
        int height = (int) (label.getAscent() + label.getDescent()) + padding;

        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        g2.setColor(backgroundColor);
        g2.fill(new RoundRectangle2D.Float(0, 0, width, height, padding, padding));
        g2.setColor(getForegroundColor(backgroundColor));
        label.draw(g2, padding/2, label.getAscent() + padding/2);
        ResourceManager.addGameMapping(key, new ImageResource(bi));
        return (Image) ResourceManager.getImage(key, lib.getScalingFactor());
    }


    /**
     * Draws all roads on the given Tile.
     *
     * @param g The <code>Graphics</code> to draw the road upon.
     * @param tile a <code>Tile</code> value
     */
    public void drawRoad(Graphics2D g, Tile tile) {

        Color oldColor = g.getColor();
        g.setColor(ResourceManager.getColor("road.color"));
        g.setStroke(roadStroke);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        GeneralPath path = new GeneralPath();
        List<Point2D.Float> points = new ArrayList<Point2D.Float>(8);
        for (Direction direction : Direction.values()) {
            Tile borderingTile = tile.getAdjacentTile(direction);
            if (borderingTile != null && borderingTile.hasRoad()) {
                points.add(corners.get(direction));
            }
        }

        switch(points.size()) {
        case 0:
            path.moveTo(0.35f * tileWidth, 0.35f * tileHeight);
            path.lineTo(0.65f * tileWidth, 0.65f * tileHeight);
            path.moveTo(0.35f * tileWidth, 0.65f * tileHeight);
            path.lineTo(0.65f * tileWidth, 0.35f * tileHeight);
            break;
        case 1:
            path.moveTo(halfWidth, halfHeight);
            path.lineTo(points.get(0).getX(), points.get(0).getY());
            break;
        case 2:
            path.moveTo(points.get(0).getX(), points.get(0).getY());
            path.quadTo(halfWidth, halfHeight, points.get(1).getX(), points.get(1).getY());
            break;
        case 3:
        case 4:
            Point2D p0 = points.get(points.size() - 1);
            path.moveTo(p0.getX(), p0.getY());
            for (Point2D p : points) {
                path.quadTo(halfWidth, halfHeight, p.getX(), p.getY());
            }
            break;
        default:
            for (Point2D p : points) {
                path.moveTo(halfWidth, halfHeight);
                path.lineTo(p.getX(), p.getY());
            }
        }
        g.draw(path);
        g.setColor(oldColor);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    }


    /**
     * Displays the given <code>Tile</code> onto the given 
     * <code>Graphics2D</code> object at the location specified 
     * by the coordinates. The visualization of the <code>Tile</code>
     * also includes information from the corresponding
     * <code>ColonyTile</code> from the given <code>Colony</code>.
     * 
     * @param g The <code>Graphics2D</code> object on which to draw 
     *      the <code>Tile</code>.
     * @param map The <code>Map</code>.
     * @param tile The <code>Tile</code> to draw.
     * @param colony The <code>Colony</code> to create the visualization
     *      of the <code>Tile</code> for. This object is also used to
     *      get the <code>ColonyTile</code> for the given <code>Tile</code>.
     */
    public void displayColonyTile(Graphics2D g, Map map, Tile tile, Colony colony) {
        displayBaseTile(g, map, tile, false);

        Unit occupyingUnit = null;
        int price = 0;
        if (colony != null) {
            ColonyTile colonyTile = colony.getColonyTile(tile);
            occupyingUnit = colonyTile.getOccupyingUnit();
            price = colony.getOwner().getLandPrice(tile);
            if (!colonyTile.canBeWorked()) {
                g.drawImage(lib.getMiscImage(ImageLibrary.TILE_TAKEN), 0, 0, null);
            }
        }
        displayTileOverlays(g, map, tile, false, false);
        
        if (price > 0 && tile.getSettlement() == null) {
            // tile is owned by an IndianSettlement
            Image image = lib.getMiscImage(ImageLibrary.TILE_OWNED_BY_INDIANS);
            centerImage(g, image);
        }
        
        if (occupyingUnit != null) {
            ImageIcon image = lib.getUnitImageIcon(occupyingUnit, 0.5);
            g.drawImage(image.getImage(), tileWidth/4 - image.getIconWidth() / 2,
                        halfHeight - image.getIconHeight() / 2, null);
            // Draw an occupation and nation indicator.
            g.drawImage(getOccupationIndicatorImage(g, occupyingUnit),
                        (int) (STATE_OFFSET_X * lib.getScalingFactor()), 0, null);
        }
    }


    /**
     * Displays the given Tile onto the given Graphics2D object at the
     * location specified by the coordinates. Draws the terrain and
     * improvements. Doesn't draw settlements, lost city rumours, fog
     * of war, optional values neither units.
     *
     * <br><br>The same as calling <code>displayTile(g, map, tile, x, y, true);</code>.
     * @param g The Graphics2D object on which to draw the Tile.
     * @param map The map.
     * @param tile The Tile to draw.
     */
    public void displayTerrain(Graphics2D g, Map map, Tile tile) {
        displayBaseTile(g, map, tile, true);
        displayTileItems(g, map, tile);
    }

    /**
     * Displays the given Tile onto the given Graphics2D object at the
     * location specified by the coordinates. Everything located on the
     * Tile will also be drawn except for units because their image can
     * be larger than a Tile.
     *
     * <br><br>The same as calling <code>displayTile(g, map, tile, x, y, true);</code>.
     * @param g The Graphics2D object on which to draw the Tile.
     * @param map The map.
     * @param tile The Tile to draw.
     */
    public void displayTile(Graphics2D g, Map map, Tile tile) {
        displayTile(g, map, tile, true);
    }

    /**
     * Displays the given Tile onto the given Graphics2D object at the
     * location specified by the coordinates. Everything located on the
     * Tile will also be drawn except for units because their image can
     * be larger than a Tile.
     * @param g The Graphics2D object on which to draw the Tile.
     * @param map The map.
     * @param tile The Tile to draw.
     * @param drawUnexploredBorders If true; draws border between explored and
     *        unexplored terrain.
     */
    public void displayTile(Graphics2D g, Map map, Tile tile, boolean drawUnexploredBorders) {
        displayBaseTile(g, map, tile, drawUnexploredBorders);
        displayTileOverlays(g, map, tile, drawUnexploredBorders, true);
    }

    /**
     * Displays the given Tile onto the given Graphics2D object at the
     * location specified by the coordinates. Only base terrain will be drawn.
     * @param g The Graphics2D object on which to draw the Tile.
     * @param map The map.
     * @param tile The Tile to draw.
     * @param drawUnexploredBorders If true; draws border between explored and
     *        unexplored terrain.
     */
    private void displayBaseTile(Graphics2D g, Map map, Tile tile, boolean drawUnexploredBorders) {
        if (tile == null) {
            return;
        }
        // ATTENTION: we assume that all base tiles have the same size
        g.drawImage(lib.getTerrainImage(tile.getType(), tile.getX(), tile.getY()), 0, 0, null);

        if (!tile.isLand() && tile.getStyle() > 0) {
            int edgeStyle = tile.getStyle() >> 4;
            if (edgeStyle > 0) {
                g.drawImage(lib.getBeachEdgeImage(edgeStyle), 0, 0, null);
            }
            int cornerStyle = tile.getStyle() & 15;
            if (cornerStyle > 0) {
                g.drawImage(lib.getBeachCornerImage(cornerStyle), 0, 0, null);
            }
        }

        for (Direction direction : Direction.values()) {
            Tile borderingTile = tile.getAdjacentTile(direction);
            if (borderingTile!=null) {

                if (!drawUnexploredBorders && !borderingTile.isExplored() &&
                    (direction == Direction.SE || direction == Direction.S ||
                     direction == Direction.SW)) {
                    continue;
                }

                if (tile.getType() == borderingTile.getType()) {
                    // Equal tiles, no need to draw border
                    continue;
                }
                else if (tile.isLand() && !borderingTile.isLand()) {
                    // The beach borders are drawn on the side of water tiles only
                    continue;
                }
                else if (!tile.isLand() && borderingTile.isLand() && borderingTile.isExplored()) {
                    // If there is a Coast image (eg. beach) defined, use it, otherwise skip
                    /*
                    if (borderingTile.getType().getArtCoast() != null) {
                        g.drawImage(lib.getCoastImage(borderingTile.getType(), direction,
                                                        tile.getX(), tile.getY()),
                                                        x, y, null);
                    }
                    */
                    // Draw the grass from the neighboring tile, spilling over on the side of this tile
                    g.drawImage(lib.getBorderImage(borderingTile.getType(), direction,
                                                   tile.getX(), tile.getY()),
                                0, 0, null);
                    TileImprovement river = borderingTile.getRiver();
                    if (river != null &&
                        (direction == Direction.SE || direction == Direction.SW ||
                         direction == Direction.NE || direction == Direction.NW)) {
                        int[] branches = river.getStyleBreakdown(Direction.longSides, 3);
                        if (branches[direction.getReverseDirection().ordinal()] > 0) {
                            g.drawImage(lib.getRiverMouthImage(direction, borderingTile.getRiver().getMagnitude(),
                                                               tile.getX(), tile.getY()),
                                        0, 0, null);
                        }
                    }
               } else if (tile.isExplored() && borderingTile.isExplored()) {
                    if (lib.getTerrainImage(tile.getType(), 0, 0)
                        .equals(lib.getTerrainImage(borderingTile.getType(), 0, 0))) {
                        // Do not draw limit between tile that share same graphics (ocean & great river)
                        continue;
                    } else if (borderingTile.getType().getIndex() < tile.getType().getIndex()) {
                        // Draw land terrain with bordering land type, or ocean/high seas limit
                        g.drawImage(lib.getBorderImage(borderingTile.getType(), direction,
                                                       tile.getX(), tile.getY()), 0, 0, null);
                    }
                }
            }
        }
    }    


    /**
     * Draws the borders of a territory on the given Tile. The
     * territory is either a country or a region.
     *
     * @param g a <code>Graphics2D</code> value
     * @param tile a <code>Tile</code> value
     * @param type a <code>BorderType</code> value
     * @param opaque a <code>boolean</code> value
     */
    private void paintBorders(Graphics2D g, Tile tile, BorderType type, boolean opaque) {
        if (tile == null ||
            (type == BorderType.COUNTRY
             && !freeColClient.getClientOptions().getBoolean(ClientOptions.DISPLAY_BORDERS))) {
            return;
        }
        Player owner = tile.getOwner();
        Region region = tile.getRegion();
        if ((type == BorderType.COUNTRY && owner != null)
            || (type == BorderType.REGION && region != null)) {
            Stroke oldStroke = g.getStroke();
            g.setStroke(borderStroke);
            Color oldColor = g.getColor();
            Color newColor = Color.WHITE;
            if (type == BorderType.COUNTRY) {
                newColor = new Color(lib.getColor(owner).getRed(),
                                     lib.getColor(owner).getGreen(),
                                     lib.getColor(owner).getBlue(),
                                     opaque ? 255 : 100);
            }
            g.setColor(newColor);
            GeneralPath path = new GeneralPath(GeneralPath.WIND_EVEN_ODD);
            path.moveTo(borderPoints.get(Direction.longSides[0]).x,
                        borderPoints.get(Direction.longSides[0]).y);
            for (Direction d : Direction.longSides) {
                Tile otherTile = tile.getNeighbourOrNull(d);
                Direction next = d.getNextDirection();
                Direction next2 = next.getNextDirection();
                if (otherTile == null
                    || (type == BorderType.COUNTRY && otherTile.getOwner() != owner)
                    || (type == BorderType.REGION && otherTile.getRegion() != region)) {
                    Tile tile1 = tile.getNeighbourOrNull(next);
                    Tile tile2 = tile.getNeighbourOrNull(next2);
                    if (tile2 == null
                        || (type == BorderType.COUNTRY && tile2.getOwner() != owner)
                        || (type == BorderType.REGION && tile2.getRegion() != region)) {
                        // small corner
                        path.lineTo(borderPoints.get(next).x,
                                    borderPoints.get(next).y);
                        path.quadTo(controlPoints.get(next).x,
                                    controlPoints.get(next).y,
                                    borderPoints.get(next2).x,
                                    borderPoints.get(next2).y);
                    } else {
                        int dx = 0, dy = 0;
                        switch(d) {
                        case NW: dx = halfWidth; dy = -halfHeight; break;
                        case NE: dx = halfWidth; dy = halfHeight; break;
                        case SE: dx = -halfWidth; dy = halfHeight; break;
                        case SW: dx = -halfWidth; dy = -halfHeight; break;
                        }
                        if (tile1 != null
                            && ((type == BorderType.COUNTRY && tile1.getOwner() == owner)
                                || (type == BorderType.REGION && tile1.getRegion() == region))) {
                            // short straight line
                            path.lineTo(borderPoints.get(next).x,
                                        borderPoints.get(next).y);
                            // big corner
                            Direction previous = d.getPreviousDirection();
                            Direction previous2 = previous.getPreviousDirection();
                            int ddx = 0, ddy = 0;
                            switch(d) {
                            case NW: ddy = -tileHeight; break;
                            case NE: ddx = tileWidth; break;
                            case SE: ddy = tileHeight; break;
                            case SW: ddx = -tileWidth; break;
                            }
                            path.quadTo(controlPoints.get(previous).x + dx,
                                        controlPoints.get(previous).y + dy,
                                        borderPoints.get(previous2).x + ddx,
                                        borderPoints.get(previous2).y + ddy);
                        } else {
                            // straight line
                            path.lineTo(borderPoints.get(d).x + dx,
                                        borderPoints.get(d).y + dy);
                        }
                    }
                } else {
                    path.moveTo(borderPoints.get(next2).x,
                                borderPoints.get(next2).y);
                }
            }
            g.draw(path);
            g.setColor(oldColor);
            g.setStroke(oldStroke);
        }
    }



    /**
     * Displays the given Tile onto the given Graphics2D object at the
     * location specified by the coordinates. Everything located on the
     * Tile will also be drawn except for units because their image can
     * be larger than a Tile.
     * @param g The Graphics2D object on which to draw the Tile.
     * @param map The map.
     * @param tile The Tile to draw.
     * @param drawUnexploredBorders If true; draws border between explored and
     *        unexplored terrain.
     * @param withNumber indicates if the number of inhabitants should be drawn too.
     */
    private void displayTileOverlays(Graphics2D g, Map map, Tile tile, 
                                     boolean drawUnexploredBorders, boolean withNumber) {
        if (tile != null) {
            if (drawUnexploredBorders) {
                displayUnexploredBorders(g, map, tile);
            }
            displayTileItems(g, map, tile);
            displaySettlement(g, map, tile, withNumber);
            displayFogOfWar(g, map, tile);
            displayOptionalValues(g, map, tile);
        }
    }

    /**
     * Displays the given Tile onto the given Graphics2D object at the
     * location specified by the coordinates. Addtions and improvements to
     * Tile will be drawn.
     * @param g The Graphics2D object on which to draw the Tile.
     * @param map The map.
     * @param tile The Tile to draw.
     */
    private void displayTileItems(Graphics2D g, Map map, Tile tile) {
        // ATTENTION: we assume that only overlays and forests
        // might be taller than a tile.
        if (!tile.isExplored()) {
            g.drawImage(lib.getTerrainImage(null, tile.getX(), tile.getY()), 0, 0, null);
        } else {
            // layer additions and improvements according to zIndex
            List<TileItem> tileItems = new ArrayList<TileItem>();
            if (tile.getTileItemContainer() != null) {
                tileItems = tile.getTileItemContainer().getTileItems();
            }
            int startIndex = 0;
            for (int index = startIndex; index < tileItems.size(); index++) {
                if (tileItems.get(index).getZIndex() < OVERLAY_INDEX) {
                    drawItem(g, tile, tileItems.get(index));
                    startIndex = index + 1;
                } else {
                    startIndex = index;
                    break;
                }
            }
            // Tile Overlays (eg. hills and mountains)
            Image overlayImage = lib.getOverlayImage(tile.getType(), tile.getX(), tile.getY());
            if (overlayImage != null) {
                g.drawImage(overlayImage, 0, (tileHeight - overlayImage.getHeight(null)), null);
            }
            for (int index = startIndex; index < tileItems.size(); index++) {
                if (tileItems.get(index).getZIndex() < FOREST_INDEX) {
                    drawItem(g, tile, tileItems.get(index));
                    startIndex = index + 1;
                } else {
                    startIndex = index;
                    break;
                }
            }
            // Forest
            if (tile.isForested()) {
                Image forestImage = lib.getForestImage(tile.getType());
                g.drawImage(forestImage, 0, (tileHeight - forestImage.getHeight(null)), null);
            }

            // draw all remaining items
            for (int index = startIndex; index < tileItems.size(); index++) {
                drawItem(g, tile, tileItems.get(index));
            }
        }
    }


    /**
     * Draws the given TileItem on the given Tile.
     *
     * @param g a <code>Graphics2D</code> value
     * @param tile a <code>Tile</code> value
     * @param item a <code>TileItem</code> value
     */
    private void drawItem(Graphics2D g, Tile tile, TileItem item) {

        if (item instanceof Resource) {
            Image bonusImage = lib.getBonusImage(((Resource) item).getType());
            if (bonusImage != null) {
                centerImage(g, bonusImage);
            }
        } else if (item instanceof LostCityRumour) {
            centerImage(g, lib.getMiscImage(ImageLibrary.LOST_CITY_RUMOUR));
        } else {

            TileImprovement improvement = (TileImprovement) item;
            if (improvement.isComplete()) {
                String key = improvement.getType().getId() + ".image";
                if (ResourceManager.hasResource(key)) {
                    // Has its own Overlay Image in Misc, use it
                    Image overlay = ResourceManager.getImage(key, lib.getScalingFactor());
                    g.drawImage(overlay, 0, 0, null);
                } else if (improvement.isRiver() && improvement.getMagnitude() < TileImprovement.FJORD_RIVER) {
                    g.drawImage(lib.getRiverImage(improvement.getStyle()), 0, 0, null);
                } else if (improvement.isRoad()) {
                    drawRoad(g, tile);
                }
            }
        }
    }

    /**
     * Displays the given Tile onto the given Graphics2D object at the
     * location specified by the coordinates. Settlements and Lost City
     * Rumours will be shown.
     * @param g The Graphics2D object on which to draw the Tile.
     * @param map The map.
     * @param tile The Tile to draw.
     * @param withNumber a <code>boolean</code> value
     */
    private void displaySettlement(Graphics2D g, Map map, Tile tile, boolean withNumber) {  
        if (tile.isExplored()) {
            Settlement settlement = tile.getSettlement();

            if (settlement != null) {
                if (settlement instanceof Colony) {
                    Image colonyImage = lib.getSettlementImage(settlement);
                    // Draw image of colony in center of the tile.
                    centerImage(g, colonyImage);
                    if (withNumber) {
                        String populationString = Integer.toString(((Colony)settlement).getUnitCount());
                        Color theColor = ResourceManager
                            .getColor("productionBonus." + ((Colony) settlement).getProductionBonus()
                                      + ".color");
                        Font font = ResourceManager.getFont("SimpleFont", Font.BOLD, 12f);
                        Image stringImage = createStringImage(g, populationString, theColor, font);
                        centerImage(g, stringImage);
                    }
                    //g.setColor(Color.BLACK);
                } else if (settlement instanceof IndianSettlement) {
                    IndianSettlement indianSettlement = (IndianSettlement) settlement;
                    Image settlementImage = lib.getSettlementImage(settlement);

                    // Draw image of indian settlement in center of the tile.
                    centerImage(g, settlementImage);

                    // Draw the color chip for the settlement.
                    String text = indianSettlement.isCapital() ? "*" : "-";
                    Color background = lib.getColor(indianSettlement.getOwner());
                    Color foreground = getForegroundColor(background);
                    Image chip = createChip(text, Color.BLACK, background, foreground);
                    float xOffset = STATE_OFFSET_X * lib.getScalingFactor();
                    float yOffset = STATE_OFFSET_Y * lib.getScalingFactor();
                    g.drawImage(chip, (int) xOffset, (int) yOffset, null);
                    xOffset += chip.getWidth(null) + 2;

                    // Draw the mission chip if needed.
                    Unit missionary = indianSettlement.getMissionary();
                    if (missionary != null) {
                        boolean expert = missionary.hasAbility("model.ability.expertMissionary");
                        Color mission = (expert ? Color.BLACK : Color.GRAY);
                        Color cross = lib.getColor(missionary.getOwner());
                        chip = createChip("\u271D", Color.BLACK, mission, cross);
                        g.drawImage(chip, (int) xOffset, (int) yOffset, null);
                        xOffset += chip.getWidth(null) + 2;
                    }

                    // Draw the alarm chip if needed.
                    Player player = freeColClient.getMyPlayer();
                    if (player != null
                        && indianSettlement.hasContactedSettlement(player)) {
                        final boolean visited = indianSettlement.hasBeenVisited(freeColClient.getMyPlayer());
                        chip = createChip((visited ? "!" : "?"), Color.BLACK, background, foreground);
                        g.drawImage(chip, (int) xOffset, (int) yOffset, null);
                    }
                } else {
                    logger.warning("Requested to draw unknown settlement type.");
                }
            }
        }
    }

    /**
     * Displays the given Tile onto the given Graphics2D object at the
     * location specified by the coordinates. Fog of war will be drawn.
     * @param g The Graphics2D object on which to draw the Tile.
     * @param map The map.
     * @param tile The Tile to draw.
     */
    private void displayFogOfWar(Graphics2D g, Map map, Tile tile) {  
        if (tile.isExplored()
            && freeColClient.getGame().getGameOptions().getBoolean(GameOptions.FOG_OF_WAR)
            && freeColClient.getClientOptions().getBoolean(ClientOptions.DISPLAY_FOG_OF_WAR)
            && freeColClient.getMyPlayer() != null
            && !freeColClient.getMyPlayer().canSee(tile)) {
            g.setColor(Color.BLACK);
            Composite oldComposite = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f));
            g.fill(fog);
            g.setComposite(oldComposite);
        }
    }

    /**
     * Displays the given Tile onto the given Graphics2D object at the
     * location specified by the coordinates. Borders next to unexplored
     * tiles will be drawn differently.
     * @param g The Graphics2D object on which to draw the Tile.
     * @param map The map.
     * @param tile The Tile to draw.
     */
    private void displayUnexploredBorders(Graphics2D g, Map map, Tile tile) {  
        if (tile.isExplored()) {

            for (Direction direction : Direction.values()) {
                Tile borderingTile = tile.getAdjacentTile(direction);
                if (borderingTile!=null) {

                    if (borderingTile.isExplored()){
                        continue;
                    }

                    g.drawImage(lib.getBorderImage(null, direction, tile.getX(), tile.getY()), 0, 0, null);
                }
            }
        }
    }


    /**
     * Displays the given Tile onto the given Graphics2D object at the
     * location specified by the coordinates. Show tile names, coordinates
     * and colony values.
     * @param g The Graphics2D object on which to draw the Tile.
     * @param map The map.
     * @param tile The Tile to draw.
     */
    private void displayOptionalValues(Graphics2D g, Map map, Tile tile) {
        String text = null;
        switch (displayTileText) {
        case ClientOptions.DISPLAY_TILE_TEXT_NAMES:
            if (tile.getNameKey() != null) {
                text = Messages.message(tile.getNameKey());
            }
            break;
        case ClientOptions.DISPLAY_TILE_TEXT_OWNERS:
            if (tile.getOwner() != null) {
                text = Messages.message(tile.getOwner().getNationName());
            }
            break;
        case ClientOptions.DISPLAY_TILE_TEXT_REGIONS:
            if (tile.getRegion() != null) {
                text = Messages.message(tile.getRegion().getLabel());
            }
            paintBorders(g, tile, BorderType.REGION, true);
            break;
        case ClientOptions.DISPLAY_TILE_TEXT_EMPTY:
            break;
        default:
            logger.warning("displayTileText out of range");
            break;
        }

        if (text != null) {
            int b = getBreakingPoint(text);
            if (b == -1) {
                centerString(g, text);
            } else {
                g.setColor(Color.BLACK);
                g.setFont(ResourceManager.getFont("NormalFont", 12f));
                g.drawString(text.substring(0, b),
                             (tileWidth -
                              g.getFontMetrics().stringWidth(text.substring(0, b)))/2,
                             halfHeight - (g.getFontMetrics().getAscent()*2)/3);
                g.drawString(text.substring(b+1),
                             (tileWidth -
                              g.getFontMetrics().stringWidth(text.substring(b+1)))/2,
                             halfHeight + (g.getFontMetrics().getAscent()*2)/3);
            }
        }

        if (displayCoordinates) {
            String posString = tile.getX() + ", " + tile.getY();
            if (tile.isConnected()) {
                posString += "C";
            }
            centerString(g, posString);
        }
        if (displayColonyValue && tile.isExplored() && tile.isLand()) {
            String valueString;
            if (displayColonyValuePlayer == null) {
                valueString = Integer.toString(freeColClient.getGame().getCurrentPlayer().getOutpostValue(tile));
            } else {
                valueString = Integer.toString(displayColonyValuePlayer.getColonyValue(tile));
            }
            centerString(g, valueString);
        }
    }

    /**
     * Center the given String on the current Tile.
     *
     * @param g a <code>Graphics2D</code> value
     * @param text a <code>String</code> value
     */
    private void centerString(Graphics2D g, String text) {
        g.setColor(Color.BLACK);
        g.setFont(ResourceManager.getFont("NormalFont", 12f));
        g.drawString(text,
                     (tileWidth - g.getFontMetrics().stringWidth(text))/2,
                     (tileHeight - g.getFontMetrics().getAscent())/2);
    }

    /**
     * Stops any ongoing goto operation on the mapboard.
     */
    public void stopGoto() {
        freeColClient.getCanvas().setCursor(null);
        setGotoPath(null);
        updateGotoPathForActiveUnit();
        gotoStarted = false;
    }


    /**
    * Starts a goto operation on the mapboard.
    */
    public void startGoto() {
        gotoStarted = true;
        freeColClient.getCanvas().setCursor((java.awt.Cursor) UIManager.get("cursor.go"));
        setGotoPath(null);
    }


    /**
     * Checks if there is currently a goto operation on the mapboard.
     * @return <code>true</code> if a goto operation is in progress.
     */
    public boolean isGotoStarted() {
        return gotoStarted;
    }


    /**
    * Sets the path of the active unit to display it.
    */
    public void updateGotoPathForActiveUnit() {
        if (activeUnit == null || activeUnit.getDestination() == null) {
            currentPath = null;
        } else {
            if (activeUnit.getDestination() instanceof Europe) {
                currentPath = freeColClient.getGame().getMap().findPathToEurope(activeUnit, activeUnit.getTile());
            } else if (activeUnit.getDestination().getTile() == activeUnit.getTile()) {
                // No need to do anything as the unit has arrived, there is no path to be shown.
                currentPath = null;
            } else {
                currentPath = activeUnit.findPath(activeUnit.getDestination().getTile());
            }
        }
    }

    /**
    * Sets the path to be drawn on the map.
    * @param gotoPath The path that should be drawn on the map
    *        or <code>null</code> if no path should be drawn.
    */
    public void setGotoPath(PathNode gotoPath) {
        this.gotoPath = gotoPath;

        freeColClient.getCanvas().refresh();
    }


    /**
    * Gets the path to be drawn on the map.
    * @return The path that should be drawn on the map
    *        or <code>null</code> if no path should be drawn.
    */
    public PathNode getGotoPath() {
        return gotoPath;
    }

    /**
     * Set the type of text drawn in the tiles.
     *
     * @param tileTextType a <code>DisplayText</code> value
     */
    public void setDisplayTileText(DisplayText tileTextType) {
        this.displayTileText = tileTextType.ordinal();
    }


    /**
    * Breaks a line between two words. The breaking point
    * is as close to the center as possible.
    *
    * @param string The line for which we should determine a
    *               breaking point.
    * @return The best breaking point or <code>-1</code> if there
    *         are none.
    */
    public int getBreakingPoint(String string) {
        int center = string.length() / 2;
        for (int offset = 0; offset < center; offset++) {
            if (string.charAt(center + offset) == ' ') {
                return center + offset;
            } else if (string.charAt(center - offset) == ' ') {
                return center - offset;
            }
        }
        return -1;
    }
    
    /**
     * Returns an occupation indicator, i.e. a small image with a
     * single letter or symbol that indicates the Unit's state.
     *
     * @param g a <code>Graphics</code> value
     * @param unit an <code>Unit</code> value
     * @return an <code>Image</code> value
     */
    public Image getOccupationIndicatorImage(Graphics g, Unit unit) {
        Color backgroundColor = lib.getColor(unit.getOwner());
        Color foregroundColor = getForegroundColor(backgroundColor);
        String occupationString;
        if (unit.getOwner() != freeColClient.getMyPlayer()
                && unit.isNaval()) {
            occupationString = Integer.toString(unit.getVisibleGoodsCount());
        } else {
            if (unit.getDestination() != null) {
                if(unit.getTradeRoute() != null)
                    occupationString = "model.unit.occupation.inTradeRoute";
                else
                    occupationString = "model.unit.occupation.goingSomewhere";
            } else if (unit.getState() == UnitState.IMPROVING
                       && unit.getWorkImprovement() != null) {
                occupationString = unit.getWorkImprovement().getType().getId() + ".occupationString";
            } else if (unit.getState() == UnitState.ACTIVE && unit.getMovesLeft() == 0) {
                if(unit.isUnderRepair())
                    occupationString = "model.unit.occupation.underRepair";
                else
                    occupationString = "model.unit.occupation.activeNoMovesLeft";
            } else {
                occupationString = "model.unit.occupation." + unit.getState().toString().toLowerCase();
            }
            occupationString = Messages.message(occupationString);
            if (unit.getState() == UnitState.FORTIFIED)
                foregroundColor = Color.GRAY;
        }
        // Lookup in the cache if the image has been generated already
        String key = "dynamic.occupationIndicator." + occupationString
            + "." + Integer.toHexString(backgroundColor.getRGB());
        Image img = (Image) ResourceManager.getImage(key, lib.getScalingFactor());
        if (img == null) {
            img = createChip(occupationString, Color.BLACK, backgroundColor, foregroundColor);
            ResourceManager.addGameMapping(key, new ImageResource(img));
        }
        return img;
    }

    /**
     * Create a "chip" with the given text and colors.
     *
     * @param text a <code>String</code> value
     * @param border a <code>Color</code> value
     * @param background a <code>Color</code> value
     * @param foreground a <code>Color</code> value
     * @return an <code>Image</code> value
     */
    public Image createChip(String text, Color border, Color background, Color foreground) {
        // Draw it and put it in the cache
        Font font = ResourceManager.getFont("SimpleFont", Font.BOLD,
                (float) Math.rint(12 * lib.getScalingFactor()));
        // hopefully, this is big enough
        BufferedImage bi = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        TextLayout label = new TextLayout(text, font, g2.getFontRenderContext());
        float padding = 6 * lib.getScalingFactor();
        int width = (int) (label.getBounds().getWidth() + padding);
        int height = (int) (label.getAscent() + label.getDescent() + padding);
//        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
//        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2.setColor(border);
        g2.fillRect(0, 0, width, height);
        g2.setColor(background);
        g2.fillRect(1, 1, width - 2, height - 2);
        g2.setColor(foreground);
        label.draw(g2, (float) (padding/2 - label.getBounds().getX()), label.getAscent() + padding/2);
        g2.dispose();
        return bi.getSubimage(0, 0, width, height);
    }



    /**
     * Describe <code>getForegroundColor</code> method here.
     *
     * @param background a <code>Color</code> value
     * @return a <code>Color</code> value
     */
    private Color getForegroundColor(Color background) {
        /*
         * Our eyes have different sensitivity towards
         * red, green and blue. We want a foreground
         * color with the inverse brightness.
         */
        if (background.getRed() * 0.3
                + background.getGreen() * 0.59
                + background.getBlue() * 0.11 < 126) {
            return Color.WHITE;
        } else {
            return Color.BLACK;
        }
    }
    
    /**
     * Describe <code>getStringBorderColor</code> method here.
     *
     * @param color a <code>Color</code> value
     * @return a <code>Color</code> value
     */
    private Color getStringBorderColor(Color color) {
        /*
         * I think string border colors should be black
         * unless the color of the string is
         * really dark.
         */
        if (color.getRed() * 0.3
                + color.getGreen() * 0.59
                + color.getBlue() * 0.11 < 10) {
            return Color.WHITE;
        } else {
            return Color.BLACK;
        }
    }
    
    /**
     * Displays the given Unit onto the given Graphics2D object at the
     * location specified by the coordinates.
     * @param g The Graphics2D object on which to draw the Unit.
     * @param unit The Unit to draw.
     */
    private void displayUnit(Graphics2D g, Unit unit) {
        try {
            // Draw the 'selected unit' image if needed.
            //if ((unit == getActiveUnit()) && cursor) {
            if (viewMode.displayUnitCursor(unit)) {
                drawCursor(g);
            }

            // Draw the unit.
            // If unit is sentry, draw in grayscale
            Image image = lib.getUnitImageIcon(unit, unit.getState() == UnitState.SENTRY).getImage();
            Point p = getUnitImagePositionInTile(image);
            g.drawImage(image, p.x, p.y, null);

            // Draw an occupation and nation indicator.
            g.drawImage(getOccupationIndicatorImage(g, unit),
                        (int) (STATE_OFFSET_X * lib.getScalingFactor()), 0, null);

            // Draw one small line for each additional unit (like in civ3).
            int unitsOnTile = 0;
            if (unit.getTile() != null) {
                // When a unit is moving from tile to tile, it is removed from the source tile
                // So the unit stack indicator cannot be drawn during the movement
                // see UnitMoveAnimation.animate() for details
                unitsOnTile = unit.getTile().getTotalUnitCount();
            }
            if (unitsOnTile > 1) {
                g.setColor(Color.WHITE);
                int unitLinesY = OTHER_UNITS_OFFSET_Y;
                int x1 = (int) ((STATE_OFFSET_X + OTHER_UNITS_OFFSET_X) * lib.getScalingFactor());
                int x2 = (int) ((STATE_OFFSET_X + OTHER_UNITS_OFFSET_X + OTHER_UNITS_WIDTH) * lib.getScalingFactor());
                for (int i = 0; (i < unitsOnTile) && (i < MAX_OTHER_UNITS); i++) {
                    g.drawLine(x1, unitLinesY, x2, unitLinesY);
                    unitLinesY += 2;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // FOR DEBUGGING:
        if (debugShowMission 
            && freeColClient.getFreeColServer() != null
            && (unit.getOwner().isAI()
                || unit.hasAbility("model.ability.piracy"))) {
            net.sf.freecol.server.ai.AIUnit au = (net.sf.freecol.server.ai.AIUnit)
                freeColClient.getFreeColServer().getAIMain().getAIObject(unit);
            if (au != null) {
                g.setColor(Color.WHITE);
                String text = (unit.getOwner().isAI()) ? "" : "(";
                String debuggingInfo = "";
                if (au.getMission() != null) {
                    String missionName = au.getMission().getClass().toString();
                    missionName = missionName.substring(missionName.lastIndexOf('.') + 1);
                    
                    if (FreeCol.usesExperimentalAI() && au.getGoal()!=null) {
                        missionName = "";
                        String goalName = au.getGoal().getDebugDescription();
                        text += goalName;
                    }
                    
                    text += missionName;
                    if (debugShowMissionInfo) {
                        debuggingInfo = au.getMission().getDebuggingInfo();
                    }
                } else {
                    text += "No mission";
                }                
                text += (unit.getOwner().isAI()) ? "" : ")";
                g.drawString(text, 0 , 0);
                g.drawString(debuggingInfo, 0 , 25);
            }
        }
    }
    
    /**
     * Gets the coordinates to draw a unit in a given tile.
     * @param unitImage The unit's image
     * @return The coordinates where the unit should be drawn onscreen
     */
    private Point getUnitImagePositionInTile(Image unitImage) {
        return getUnitImagePositionInTile(unitImage.getWidth(null), unitImage.getHeight(null));
    }
    
    /**
     * Gets the coordinates to draw a unit in a given tile.
     * @param unitImageWidth The unit image's width
     * @param unitImageHeight The unit image's height
     * @return The coordinates where the unit should be drawn onscreen
     */
    private Point getUnitImagePositionInTile(int unitImageWidth, int unitImageHeight) {
        int unitX = (tileWidth - unitImageWidth) / 2;
        int unitY = (tileHeight - unitImageHeight) / 2 -
                    (int) (UNIT_OFFSET * lib.getScalingFactor());
        
        return new Point(unitX, unitY);
    }
    
    /**
     * Gets the position where a unitLabel located at tile should be drawn. 
     * @param unitLabel The unit label with the unit's image and occupation indicator drawn.
     * @param tile The tile where the unitLabel will be drawn over
     * @return The position where to put the label, null if the Tile is offscreen.
     */
    public Point getUnitLabelPositionInTile(JLabel unitLabel, Tile tile) {
        return getUnitLabelPositionInTile(unitLabel, getTilePosition(tile));
    }
    
    /**
     * Gets the position where a unitLabel located at tile should be drawn. 
     * @param unitLabel The unit label with the unit's image and occupation indicator drawn.
     * @param tileP The position of the Tile on the screen.
     * @return The position where to put the label, null if tileP is null.
     */
    public Point getUnitLabelPositionInTile(JLabel unitLabel, Point tileP) {
        if (tileP != null) {
            int labelX = tileP.x + getTileWidth() / 2 - unitLabel.getWidth() / 2;
            int labelY = tileP.y + getTileHeight() / 2 - unitLabel.getHeight() / 2 -
                        (int) (UNIT_OFFSET * lib.getScalingFactor());
            
            return new Point(labelX, labelY);
        } else {
            return null;
        }
    }
    
    /**
     * Describe <code>drawCursor</code> method here.
     *
     * @param g a <code>Graphics2D</code> value
     */
    private void drawCursor(Graphics2D g) {
        g.drawImage(cursorImage, 0, 0, null);
    }


    /**
    * Notifies this GUI that the game has started or ended.
    * @param inGame Indicates whether or not the game has started.
    */
    public void setInGame(boolean inGame) {
        this.inGame = inGame;
    }


    /**
    * Checks if the game has started.
    * @return <i>true</i> if the game has started.
    * @see #setInGame
    */
    public boolean isInGame() {
        return inGame;
    }


    /**
     * Checks if the Tile/Units at the given coordinates are displayed
     * on the screen (or, if the map is already displayed and the focus
     * has been changed, whether they will be displayed on the screen
     * the next time it'll be redrawn).
     * @param x The x-coordinate of the Tile in question.
     * @param y The y-coordinate of the Tile in question.
     * @return 'true' if the Tile will be drawn on the screen, 'false'
     * otherwise.
     */
    public boolean onScreen(int x, int y) {
        if (bottomRow < 0) {
            positionMap();
            return y - 2 > topRow && y + 4 < bottomRow && x - 1 > leftColumn && x + 2 < rightColumn;
        } else {
            return y - 2 > topRow && y + 4 < bottomRow && x - 1 > leftColumn && x + 2 < rightColumn;
        }
    }


    /**
     * Checks if the Tile/Units at the given coordinates are displayed
     * on the screen (or, if the map is already displayed and the focus
     * has been changed, whether they will be displayed on the screen
     * the next time it'll be redrawn).
     *
     * @param position The position of the Tile in question.
     * @return <i>true</i> if the Tile will be drawn on the screen, <i>false</i>
     * otherwise.
     */
    public boolean onScreen(Position position) {
        return onScreen(position.getX(), position.getY());
    }


    /**
     * Converts the given screen coordinates to Map coordinates.
     * It checks to see to which Tile the given pixel 'belongs'.
     *
     * @param x The x-coordinate in pixels.
     * @param y The y-coordinate in pixels.
     * @return The map coordinates of the Tile that is located at
     * the given position on the screen.
     */
    public Map.Position convertToMapCoordinates(int x, int y) {
        Game gameData = freeColClient.getGame();
        if ((gameData == null) || (gameData.getMap() == null)) {
            return null;
        }

        int leftOffset;
        if (focus.getX() < getLeftColumns()) {
            // we are at the left side of the map
            if ((focus.getY() % 2) == 0) {
                leftOffset = tileWidth * focus.getX() + halfWidth;
            } else {
                leftOffset = tileWidth * (focus.getX() + 1);
            }
        } else if (focus.getX() >= (gameData.getMap().getWidth() - getRightColumns())) {
            // we are at the right side of the map
            if ((focus.getY() % 2) == 0) {
                leftOffset = size.width - (gameData.getMap().getWidth() - focus.getX()) * tileWidth;
            } else {
                leftOffset = size.width - (gameData.getMap().getWidth() - focus.getX() - 1) * tileWidth - halfWidth;
            }
        } else {
            if ((focus.getY() % 2) == 0) {
                leftOffset = (size.width / 2);
            } else {
                leftOffset = (size.width / 2) + halfWidth;
            }
        }

        int topOffset;
        if (focus.getY() < topRows) {
            // we are at the top of the map
            topOffset = (focus.getY() + 1) * (halfHeight);
        } else if (focus.getY() >= (gameData.getMap().getHeight() - bottomRows)) {
            // we are at the bottom of the map
            topOffset = size.height - (gameData.getMap().getHeight() - focus.getY()) * (halfHeight);
        } else {
            topOffset = (size.height / 2);
        }

        // At this point (leftOffset, topOffset) is the center pixel of the Tile
        // that was on focus (= the Tile that should have been drawn at the center
        // of the screen if possible).

        // The difference in rows/columns between the selected
        // tile (x, y) and the current center Tile.
        // These values are positive if (x, y) is located NW
        // of the current center Tile.
        int diffUp = (topOffset - y) / (tileHeight / 4),
            diffLeft = (leftOffset - x) / (tileWidth / 4);

        // The following values are used when the user clicked somewhere
        // near the crosspoint of 4 Tiles.
        int orDiffUp = diffUp,
            orDiffLeft = diffLeft,
            remainderUp = (topOffset - y) % (tileHeight / 4),
            remainderLeft = (leftOffset - x) % (tileWidth / 4);

        if ((diffUp % 2) == 0) {
            diffUp = diffUp / 2;
        } else {
            if (diffUp < 0) {
                diffUp = (diffUp / 2) - 1;
            } else {
                diffUp = (diffUp / 2) + 1;
            }
        }

        if ((diffLeft % 2) == 0) {
            diffLeft = diffLeft / 2;
        } else {
            if (diffLeft < 0) {
                diffLeft = (diffLeft / 2) - 1;
            } else {
                diffLeft = (diffLeft / 2) + 1;
            }
        }

        boolean done = false;
        while (!done) {
            if ((diffUp % 2) == 0) {
                if ((diffLeft % 2) == 0) {
                    diffLeft = diffLeft / 2;
                    done = true;
                } else {
                    // Crosspoint
                    if (((orDiffLeft % 2) == 0) && ((orDiffUp % 2) == 0)) {
                        if ((orDiffLeft > 0) && (orDiffUp > 0)) {
                            // Upper-Left
                            if ((remainderUp * 2) > remainderLeft) {
                                diffUp++;
                            } else {
                                diffLeft++;
                            }
                        } else if (orDiffUp > 0) {
                            // Upper-Right
                            if ((remainderUp * 2) > -remainderLeft) {
                                diffUp++;
                            } else {
                                diffLeft--;
                            }
                        } else if ((orDiffLeft > 0) && (orDiffUp == 0)) {
                            if (remainderUp > 0) {
                                // Upper-Left
                                if ((remainderUp * 2) > remainderLeft) {
                                    diffUp++;
                                } else {
                                    diffLeft++;
                                }
                            } else {
                                // Lower-Left
                                if ((-remainderUp * 2) > remainderLeft) {
                                    diffUp--;
                                } else {
                                    diffLeft++;
                                }
                            }
                        } else if (orDiffUp == 0) {
                            if (remainderUp > 0) {
                                // Upper-Right
                                if ((remainderUp * 2) > -remainderLeft) {
                                    diffUp++;
                                } else {
                                    diffLeft--;
                                }
                            } else {
                                // Lower-Right
                                if ((-remainderUp * 2) > -remainderLeft) {
                                    diffUp--;
                                } else {
                                    diffLeft--;
                                }
                            }
                        } else if (orDiffLeft > 0) {
                            // Lower-Left
                            if ((-remainderUp * 2) > remainderLeft) {
                                diffUp--;
                            } else {
                                diffLeft++;
                            }
                        } else {
                            // Lower-Right
                            if ((-remainderUp * 2) > -remainderLeft) {
                                diffUp--;
                            } else {
                                diffLeft--;
                            }
                        }
                    } else if ((orDiffLeft % 2) == 0) {
                        if ((orDiffLeft > 0) && (orDiffUp > 0)) {
                            // Lower-Left
                            if ((remainderUp * 2 + remainderLeft) > (tileWidth / 4)) {
                                diffLeft++;
                            } else {
                                diffUp--;
                            }
                        } else if (orDiffUp > 0) {
                            // Lower-Right
                            if ((remainderUp * 2 - remainderLeft) > (tileWidth / 4)) {
                                diffLeft--;
                            } else {
                                diffUp--;
                            }
                        } else if (orDiffLeft > 0) {
                            // Upper-Left
                            if ((-remainderUp * 2 + remainderLeft) > (tileWidth / 4)) {
                                diffLeft++;
                            } else {
                                diffUp++;
                            }
                        } else {
                            // Upper-Right
                            if ((-remainderUp * 2 - remainderLeft) > (tileWidth / 4)) {
                                diffLeft--;
                            } else {
                                diffUp++;
                            }
                        }
                    } else if ((orDiffUp % 2) == 0) {
                        if ((orDiffLeft > 0) && (orDiffUp > 0)) {
                            // Upper-Right
                            if ((remainderUp * 2 + remainderLeft) > (tileWidth / 4)) {
                                diffUp++;
                            } else {
                                diffLeft--;
                            }
                        } else if (orDiffUp > 0) {
                            // Upper-Left
                            if ((remainderUp * 2 - remainderLeft) > (tileWidth / 4)) {
                                diffUp++;
                            } else {
                                diffLeft++;
                            }
                        } else if ((orDiffLeft > 0) && (orDiffUp == 0)) {
                            if (remainderUp > 0) {
                                // Upper-Right
                                if ((remainderUp * 2 + remainderLeft) > (tileWidth / 4)) {
                                    diffUp++;
                                } else {
                                    diffLeft--;
                                }
                            } else {
                                // Lower-Right
                                if ((-remainderUp * 2 + remainderLeft) > (tileWidth / 4)) {
                                    diffUp--;
                                } else {
                                    diffLeft--;
                                }
                            }
                        } else if (orDiffUp == 0) {
                            if (remainderUp > 0) {
                                // Upper-Left
                                if ((remainderUp * 2 - remainderLeft) > (tileWidth / 4)) {
                                    diffUp++;
                                } else {
                                    diffLeft++;
                                }
                            } else {
                                // Lower-Left
                                if ((-remainderUp * 2 - remainderLeft) > (tileWidth / 4)) {
                                    diffUp--;
                                } else {
                                    diffLeft++;
                                }
                            }
                        } else if (orDiffLeft > 0) {
                            // Lower-Right
                            if ((-remainderUp * 2 + remainderLeft) > (tileWidth / 4)) {
                                diffUp--;
                            } else {
                                diffLeft--;
                            }
                        } else {
                            // Lower-Left
                            if ((-remainderUp * 2 - remainderLeft) > (tileWidth / 4)) {
                                diffUp--;
                            } else {
                                diffLeft++;
                            }
                        }
                    } else {
                        if ((orDiffLeft > 0) && (orDiffUp > 0)) {
                            // Lower-Right
                            if ((remainderUp * 2) > remainderLeft) {
                                diffLeft--;
                            } else {
                                diffUp--;
                            }
                        } else if (orDiffUp > 0) {
                            // Lower-Left
                            if ((remainderUp * 2) > -remainderLeft) {
                                diffLeft++;
                            } else {
                                diffUp--;
                            }
                        } else if (orDiffLeft > 0) {
                            // Upper-Right
                            if ((-remainderUp * 2) > remainderLeft) {
                                diffLeft--;
                            } else {
                                diffUp++;
                            }
                        } else {
                            // Upper-Left
                            if ((-remainderUp * 2) > -remainderLeft) {
                                diffLeft++;
                            } else {
                                diffUp++;
                            }
                        }
                    }
                }
            } else {
                if ((diffLeft % 2) == 0) {
                    // Crosspoint
                    if (((orDiffLeft % 2) == 0) && ((orDiffUp % 2) == 0)) {
                        if ((orDiffLeft > 0) && (orDiffUp > 0)) {
                            // Upper-Left
                            if ((remainderUp * 2) > remainderLeft) {
                                diffUp++;
                            } else {
                                diffLeft++;
                            }
                        } else if (orDiffLeft > 0) {
                            // Lower-Left
                            if ((-remainderUp * 2) > remainderLeft) {
                                diffUp--;
                            } else {
                                diffLeft++;
                            }
                        } else if ((orDiffUp > 0) && (orDiffLeft == 0)) {
                            if (remainderLeft > 0) {
                                // Upper-Left
                                if ((remainderUp * 2) > remainderLeft) {
                                    diffUp++;
                                } else {
                                    diffLeft++;
                                }
                            } else {
                                // Upper-Right
                                if ((remainderUp * 2) > -remainderLeft) {
                                    diffUp++;
                                } else {
                                    diffLeft--;
                                }
                            }
                        } else if (orDiffLeft == 0) {
                            if (remainderLeft > 0) {
                                // Lower-Left
                                if ((-remainderUp * 2) > remainderLeft) {
                                    diffUp--;
                                } else {
                                    diffLeft++;
                                }
                            } else {
                                // Lower-Right
                                if ((-remainderUp * 2) > -remainderLeft) {
                                    diffUp--;
                                } else {
                                    diffLeft--;
                                }
                            }
                        } else if (orDiffUp > 0) {
                            // Upper-Right
                            if ((remainderUp * 2) > -remainderLeft) {
                                diffUp++;
                            } else {
                                diffLeft--;
                            }
                        } else {
                            // Lower-Right
                            if ((-remainderUp * 2) > -remainderLeft) {
                                diffUp--;
                            } else {
                                diffLeft--;
                            }
                        }
                    } else if ((orDiffLeft % 2) == 0) {
                        if ((orDiffLeft > 0) && (orDiffUp > 0)) {
                            // Lower-Left
                            if ((remainderUp * 2 + remainderLeft) > (tileWidth / 4)) {
                                diffLeft++;
                            } else {
                                diffUp--;
                            }
                        } else if (orDiffLeft > 0) {
                            // Upper-Left
                            if ((-remainderUp * 2 + remainderLeft) > (tileWidth / 4)) {
                                diffLeft++;
                            } else {
                                diffUp++;
                            }
                        } else if ((orDiffUp > 0) && (orDiffLeft == 0)) {
                            if (remainderLeft > 0) {
                                // Lower-Left
                                if ((remainderUp * 2 + remainderLeft) > (tileWidth / 4)) {
                                    diffLeft++;
                                } else {
                                    diffUp--;
                                }
                            } else {
                                // Lower-Right
                                if ((remainderUp * 2 - remainderLeft) > (tileWidth / 4)) {
                                    diffLeft--;
                                } else {
                                    diffUp--;
                                }
                            }
                        } else if (orDiffLeft == 0) {
                            if (remainderLeft > 0) {
                                // Upper-Left
                                if ((-remainderUp * 2 + remainderLeft) > (tileWidth / 4)) {
                                    diffLeft++;
                                } else {
                                    diffUp++;
                                }
                            } else {
                                // Upper-Right
                                if ((-remainderUp * 2 - remainderLeft) > (tileWidth / 4)) {
                                    diffLeft--;
                                } else {
                                    diffUp++;
                                }
                            }
                        } else if (orDiffUp > 0) {
                            // Lower-Right
                            if ((remainderUp * 2 - remainderLeft) > (tileWidth / 4)) {
                                diffLeft--;
                            } else {
                                diffUp--;
                            }
                        } else {
                            // Upper-Right
                            if ((-remainderUp * 2 - remainderLeft) > (tileWidth / 4)) {
                                diffLeft--;
                            } else {
                                diffUp++;
                            }
                        }
                    } else if ((orDiffUp % 2) == 0) {
                        if ((orDiffLeft > 0) && (orDiffUp > 0)) {
                            // Upper-Right
                            if ((remainderUp * 2 + remainderLeft) > (tileWidth / 4)) {
                                diffUp++;
                            } else {
                                diffLeft--;
                            }
                        } else if (orDiffUp > 0) {
                            // Upper-Left
                            if ((remainderUp * 2 - remainderLeft) > (tileWidth / 4)) {
                                diffUp++;
                            } else {
                                diffLeft++;
                            }
                        } else if (orDiffLeft > 0) {
                            // Lower-Right
                            if ((-remainderUp * 2 + remainderLeft) > (tileWidth / 4)) {
                                diffUp--;
                            } else {
                                diffLeft--;
                            }
                        } else {
                            // Lower-Left
                            if ((-remainderUp * 2 - remainderLeft) > (tileWidth / 4)) {
                                diffUp--;
                            } else {
                                diffLeft++;
                            }
                        }
                    } else {
                        if ((orDiffLeft > 0) && (orDiffUp > 0)) {
                            // Lower-Right
                            if ((remainderUp * 2) > remainderLeft) {
                                diffLeft--;
                            } else {
                                diffUp--;
                            }
                        } else if (orDiffUp > 0) {
                            // Lower-Left
                            if ((remainderUp * 2) > -remainderLeft) {
                                diffLeft++;
                            } else {
                                diffUp--;
                            }
                        } else if (orDiffLeft > 0) {
                            // Upper-Right
                            if ((-remainderUp * 2) > remainderLeft) {
                                diffLeft--;
                            } else {
                                diffUp++;
                            }
                        } else {
                            // Upper-Left
                            if ((-remainderUp * 2) > -remainderLeft) {
                                diffLeft++;
                            } else {
                                diffUp++;
                            }
                        }
                    }
                } else {
                    if ((focus.getY() % 2) == 0) {
                        if (diffLeft < 0) {
                            diffLeft = diffLeft / 2;
                        } else {
                            diffLeft = (diffLeft / 2) + 1;
                        }
                    } else {
                        if (diffLeft < 0) {
                            diffLeft = (diffLeft / 2) - 1;
                        } else {
                            diffLeft = diffLeft / 2;
                        }
                    }
                    done = true;
                }
            }
        }
        return new Map.Position(focus.getX() - diffLeft, focus.getY() - diffUp);
    }


    /**
     * Returns 'true' if the Tile is near the top.
     * @param y The y-coordinate of a Tile.
     * @return 'true' if the Tile is near the top.
     */
    public boolean isMapNearTop(int y) {
        if (y < topRows) {
            return true;
        } else {
            return false;
        }
    }


    /**
     * Returns 'true' if the Tile is near the bottom.
     * @param y The y-coordinate of a Tile.
     * @return 'true' if the Tile is near the bottom.
     */
    public boolean isMapNearBottom(int y) {
        if (y >= (freeColClient.getGame().getMap().getHeight() - bottomRows)) {
            return true;
        } else {
            return false;
        }
    }


    /**
     * Returns 'true' if the Tile is near the left.
     * @param x The x-coordinate of a Tile.
     * @param y The y-coordinate of a Tile.
     * @return 'true' if the Tile is near the left.
     */
    public boolean isMapNearLeft(int x, int y) {
        if (x < getLeftColumns(y)) {
            return true;
        } else {
            return false;
        }
    }


    /**
     * Returns 'true' if the Tile is near the right.
     * @param x The x-coordinate of a Tile.
     * @param y The y-coordinate of a Tile.
     * @return 'true' if the Tile is near the right.
     */
    public boolean isMapNearRight(int x, int y) {
        if (x >= (freeColClient.getGame().getMap().getWidth() - getRightColumns(y))) {
            return true;
        } else {
            return false;
        }
    }


    /**
     * Get the current scale of the map.
     *
     * @return a <code>float</code> value
     */
    public float getMapScale() {
        return lib.getScalingFactor();
    }

    /**
     * Change the scale of the map by delta.
     *
     * @param delta a <code>float</code> value
     */
    public void scaleMap(float delta) {
        float newScale = lib.getScalingFactor() + delta;
        try {
            if (newScale >= 1f) {
                setImageLibrary(freeColClient.getImageLibrary());
            } else {
                setImageLibrary(freeColClient.getImageLibrary().getScaledImageLibrary(newScale));
            }
        } catch (Exception ex) {
            logger.warning("Failed to retrieve scaled image library.");
        }
        forceReposition();
        freeColClient.getCanvas().refresh();
    }

    /**
     * Gets the position of the given <code>Tile</code>
     * on the drawn map.
     * 
     * @param t The <code>Tile</code>.
     * @return The position of the given <code>Tile</code>,
     *      or <code>null</code> if the <code>Tile</code> is
     *      not drawn on the mapboard.
     */
    public Point getTilePosition(Tile t) {
        if (bottomRow < 0) {
            positionMap();
        }
        if (t.getY() >= topRow 
                && t.getY() <= bottomRow 
                && t.getX() >= leftColumn 
                && t.getX() <= rightColumn) {
            int x = ((t.getX() - leftColumn) * tileWidth) + leftColumnX;
            int y = ((t.getY() - topRow) * halfHeight) + topRowY;
            if ((t.getY() % 2) != 0) {     
                x += halfWidth;
            }
            return new Point(x, y);
        } else {
            return null;
        }
    }
    
    /**
     * Calculate the bounds of the rectangle containing a Tile on the screen,
     * and return it. If the Tile is not on-screen a maximal rectangle is returned.
     * The bounds includes a one-tile padding area above the Tile, to include the space
     * needed by any units in the Tile.
     * @param tile The tile on the screen.
     * @return The bounds rectangle
     */
    public Rectangle getTileBounds(Tile tile) {
        return getTileBounds(tile.getX(), tile.getY());
    }
    
    /**
     * Calculate the bounds of the rectangle containing a Tile on the screen,
     * and return it. If the Tile is not on-screen a maximal rectangle is returned.
     * The bounds includes a one-tile padding area above the Tile, to include the space
     * needed by any units in the Tile.
     * @param x The x-coordinate of the Tile
     * @param y The y-coordinate of the Tile
     * @return The bounds rectangle
     */
    public Rectangle getTileBounds(int x, int y) {
        Rectangle result = new Rectangle(0, 0, size.width, size.height);
        if (y >= topRow && y <= bottomRow && x >= leftColumn && x <= rightColumn) {
            result.y = ((y - topRow) * halfHeight) + topRowY - tileHeight;
            result.x = ((x - leftColumn) * tileWidth) + leftColumnX;
            if ((y % 2) != 0) {
                result.x += halfWidth;
            }
            result.width = tileWidth;
            result.height = tileHeight * 2;
        }
        return result;
    }


    /**
     * Force the next screen repaint to reposition the tiles on the window.
     */
    public void forceReposition() {
        bottomRow = -1;
    }

    /**
     * Describe <code>getTileHeight</code> method here.
     *
     * @return an <code>int</code> value
     */
    public int getTileHeight() {
        return tileHeight;
    }

    /**
     * Describe <code>getTileWidth</code> method here.
     *
     * @return an <code>int</code> value
     */
    public int getTileWidth() {
        return tileWidth;
    }
}
