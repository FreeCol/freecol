
package net.sf.freecol.client.gui;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.Graphics2D;
import java.awt.Color;
import java.util.*;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.UIManager;

import net.sf.freecol.client.FreeColClient;

import net.sf.freecol.common.*;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.model.*;

/**
* This class is responsible for drawing the map/background on the <code>Canvas</code>.
* In addition, the graphical state of the map (focus, active unit..) is also a responsibillity
* of this class.
*/
public final class GUI {
    private static final Logger logger = Logger.getLogger(GUI.class.getName());
    
    private FreeColClient freeColClient;
    private final Rectangle bounds;
    private final ImageLibrary lib;
    private Game gameData;
    private boolean cursor;
    private final Vector messages;
    
    private Map.Position selectedTile;
    private Map.Position focus = null;
    private Unit activeUnit;

    // Helper variables for displaying the map.
    private final int tileHeight,
    tileWidth,
    topSpace,
    topRows,
    bottomSpace,
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
    TEXT_OFFSET_X = 2, // Relative to the state indicator.
    TEXT_OFFSET_Y = 13, // Relative to the state indicator.
    STATE_OFFSET_X = 25,
    OTHER_UNITS_OFFSET_X = -5, // Relative to the state indicator.
    OTHER_UNITS_OFFSET_Y = 1,
    OTHER_UNITS_WIDTH = 3,
    MAX_OTHER_UNITS = 10,
    MESSAGE_COUNT = 3,
    MESSAGE_AGE = 3000; // The amount of time before a message gets deleted (in milliseconds).

    // Debug variables:
    boolean displayCoordinates = false;



    /**
    * The constructor to use.
    *
    * @param freeColClient The main control class.
    * @param bounds The bounds of the GUI (= the entire screen if the app is displayed in full-screen).
    * @param gameData The data of the game that is currently being played.
    * @param lib The library of images needed to display certain things visually.
    */
    public GUI(FreeColClient freeColClient, Rectangle bounds, Game gameData, ImageLibrary lib) {
        this.freeColClient = freeColClient;
        this.bounds = bounds;
        this.gameData = gameData;
        this.lib = lib;

        cursor = true;

        tileHeight = lib.getTerrainImageHeight(0);
        tileWidth = lib.getTerrainImageWidth(0);

        // Calculate the amount of rows that will be drawn above the central Tile
        topSpace = ((int) bounds.getHeight() - tileHeight) / 2;
        bottomSpace = topSpace;

        if ((topSpace % (tileHeight / 2)) != 0) {
            topRows = topSpace / (tileHeight / 2) + 2;
        } else {
            topRows = topSpace / (tileHeight / 2) + 1;
        }
        bottomRows = topRows;

        leftSpace = ((int) bounds.getWidth() - tileWidth) / 2;
        rightSpace = leftSpace;
        logger.info("GUI created.");

        messages = new Vector(MESSAGE_COUNT);
    }


    /**
    * Selects the tile at the specified position. There are three
    * possible cases:
    *
    * <ol>
    *   <li>If there is a {@link Colony} on the {@link Tile} the
    *       {@link Canvas#showColonyPanel} will be invoked.
    *   <li>If the tile contains a unit that can become active, then
    *       that unit will be set as the active unit.
    *   <li>If the two conditions above do not match, then the
    *       <code>selectedTile</code> will become the map focus.
    * </ol>
    *
    * If a unit is active and is located on the selected tile,
    * then nothing (except perhaps a map reposition) will happen.
    *
    * @param selectedTile The <code>Position</code> of the tile
    *                     to be selected.
    * @see #getSelectedTile
    * @see #setActiveUnit
    * @see #setFocus
    */
    public void setSelectedTile(Position selectedTile) {
        Position oldPosition = this.selectedTile;

        this.selectedTile = selectedTile;

        if (selectedTile != null) {
            if (activeUnit == null || !activeUnit.getTile().getPosition().equals(selectedTile)) {
                Tile t = gameData.getMap().getTile(selectedTile);
                if (t != null && t.getSettlement() != null && t.getSettlement() instanceof Colony
                    && t.getSettlement().getOwner().equals(freeColClient.getMyPlayer())) {

                    setFocus(selectedTile);

                    freeColClient.getCanvas().showColonyPanel((Colony) t.getSettlement());
                    return;
                }

                Unit movableUnit = gameData.getMap().getTile(selectedTile).getMovableUnit();
                if (movableUnit != null) {
                    setActiveUnit(movableUnit);
                } else {
                    setFocus(selectedTile);
                }
            }

            // Check if the gui needs to reposition:
            if (!onScreen(selectedTile)) {
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
    }


    /**
    * Gets the selected tile.
    *
    * @return The <code>Position</code> of that tile.
    * @see #setSelectedTile
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
    * Sets the active unit. Invokes {@link #setSelectedTile} if the
    * selected tile is another tile than where the <code>activeUnit</code>
    * is located.
    *
    * @param activeUnit The new active unit.
    * @see #setSelectedTile
    */
    public void setActiveUnit(Unit activeUnit) {
        // Don't select a unit with zero moves left. -sjm
        if ((activeUnit != null) && (activeUnit.getMovesLeft() == 0)) {
            freeColClient.getInGameController().nextActiveUnit();
            return;
        }
        this.activeUnit = activeUnit;

        freeColClient.getCanvas().getMapControls().updateMoves(activeUnit);
        
        //TODO: update only within the bounds of InfoPanel
        freeColClient.getCanvas().repaint(0, 0, getWidth(), getHeight());

        if (activeUnit != null && !activeUnit.getTile().getPosition().equals(selectedTile)) {
            setSelectedTile(activeUnit.getTile().getPosition());
        }
    }

    
    /**
    * Gets the focus of the map. That is the center tile of the displayed
    * map.
    *
    * @return The <code>Position</code> of the center tile of the
    *         displayed map
    * @see #setFocus
    */
    public Position getFocus() {
        return focus;
    }


    /**
    * Sets the focus of the map.
    *
    * param focus The <code>Position</code> of the center tile of the
    *             displayed map.
    * @see #getFocus
    */
    public void setFocus(Position focus) {
        this.focus = focus;

        forceReposition();
        freeColClient.getCanvas().repaint(0, 0, getWidth(), getHeight());
    }


    /**
    * Sets the focus of the map.
    *
    * param x The x-coordinate of the center tile of the
    *         displayed map.
    * param y The x-coordinate of the center tile of the
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
        return (GUIMessage) messages.get(index);
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
        return (int) bounds.getWidth();
    }


    /**
     * Returns the height of this GUI.
     * @return The height of this GUI.
     */
    public int getHeight() {
        return (int) bounds.getHeight();
    }


    /**
     * Displays this GUI onto the given Graphics2D.
     * @param g The Graphics2D on which to display this GUI.
     */
    public void display(Graphics2D g) {
        if ((gameData != null)
            && (gameData.getMap() != null)
            && (focus != null)) {
            displayMap(g);
        } else {
            ImageIcon bgImage = (ImageIcon) UIManager.get("CanvasBackgroundImage");

            if (bgImage != null) {
                g.drawImage(bgImage.getImage(), 0, 0, null);
            } else {
                g.setColor(Color.black);
                g.fillRect(0, 0, bounds.width, bounds.height);
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
        int x = focus.getX(),
            y = focus.getY();
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
            bottomRow = ((int) bounds.getHeight() / (tileHeight / 2)) - 1;
            if ((bounds.getHeight() % (tileHeight / 2)) != 0) {
                bottomRow++;
            }
            topRow = 0;
            bottomRowY = bottomRow * (tileHeight / 2);
            topRowY = 0;
        } else if (y >= (gameData.getMap().getHeight() - bottomRows)) {
            // We are at the bottom of the map
            bottomRow = gameData.getMap().getHeight() - 1;

            topRow = (int) bounds.getHeight() / (tileHeight / 2);
            if (((int) bounds.getHeight() % (tileHeight / 2)) > 0) {
                topRow++;
            }
            topRow = gameData.getMap().getHeight() - topRow;

            bottomRowY = (int) bounds.getHeight() - tileHeight;
            topRowY = bottomRowY - (bottomRow - topRow) * (tileHeight / 2);
        } else {
            // We are not at the top of the map and not at the bottom
            bottomRow = y + bottomRows;
            topRow = y - topRows;
            bottomRowY = topSpace + (tileHeight / 2) * bottomRows;
            topRowY = topSpace - topRows * (tileHeight / 2);
        }

        /*
        PART 2
        ======
        Calculate: leftColumn, rightColumn, leftColumnX
        This will tell us which columns need to be drawn on the screen (from
        leftColumn until and including rightColumn).
        leftColumnX will tell us at which x-coordinate the left column needs
        to be drawn (this is for the Tiles where y%2==0; the others should be
        tileWidth / 2 more to the right).
        */

        if (x < leftColumns) {
            // We are at the left side of the map
            leftColumn = 0;

            rightColumn = (int) bounds.getWidth() / tileWidth - 1;
            if (((int) bounds.getWidth() % tileWidth) > 0) {
                rightColumn++;
            }

            leftColumnX = 0;
        } else if (x >= (gameData.getMap().getWidth() - rightColumns)) {
            // We are at the right side of the map
            rightColumn = gameData.getMap().getWidth() - 1;

            leftColumn = (int) bounds.getWidth() / tileWidth;
            if (((int) bounds.getWidth() % tileWidth) > 0) {
                leftColumn++;
            }

            leftColumnX = (int) bounds.getWidth() - tileWidth - tileWidth / 2 -
                leftColumn * tileWidth;
            leftColumn = rightColumn - leftColumn;
        } else {
            // We are not at the left side of the map and not at the right side
            leftColumn = x - leftColumns;
            rightColumn = x + rightColumns;
            leftColumnX = ((int) bounds.getWidth() - tileWidth) / 2 - leftColumns * tileWidth;
        }
    }


    /**
     * Displays the Map onto the given Graphics2D object. The Tile at
     * location (x, y) is displayed in the center.
     * @param g The Graphics2D object on which to draw the Map.
     */
    private void displayMap(Graphics2D g) {
        Rectangle clipBounds = g.getClipBounds();
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
        ======
        Determine which tiles need to be redrawn.
        */
        int clipTopRow = (clipBounds.y - topRowY) / (tileHeight / 2) - 1;
        int clipTopY = topRowY + clipTopRow * (tileHeight / 2);
        clipTopRow = topRow + clipTopRow;

        int clipLeftCol = (clipBounds.x - leftColumnX) / tileWidth - 1;
        int clipLeftX = leftColumnX + clipLeftCol * tileWidth;
        clipLeftCol = leftColumn + clipLeftCol;

        int clipBottomRow = (clipBounds.y + clipBounds.height - topRowY) / (tileHeight / 2);
        clipBottomRow = topRow + clipBottomRow;

        int clipRightCol = (clipBounds.x + clipBounds.width - leftColumnX) / tileWidth;
        clipRightCol = leftColumn + clipRightCol;

        /*
        PART 2
        ======
        Display the Tiles.
        */

        g.setColor(Color.black);
        g.fillRect(clipBounds.x, clipBounds.y, clipBounds.width, clipBounds.height);
        int xx;
        int yy = clipTopY;
        Map map = gameData.getMap();
        // Row per row; start with the top modified row
        for (int tileY = clipTopRow; tileY <= clipBottomRow; tileY++) {
            xx = clipLeftX;
            if ((tileY % 2) != 0) {
                xx += tileWidth / 2;
            }
            // Column per column; start at the left side
            for (int tileX = clipLeftCol; tileX <= clipRightCol; tileX++) {
                if (map.isValid(tileX, tileY)) {
                    displayTile(g, map, map.getTile(tileX, tileY), xx, yy);
                }
                xx += tileWidth;
            }
            yy += tileHeight / 2;
        }

        /*
        PART 3
        ======
        Display the Units.
        */

        yy = clipTopY;

        // Row per row; start with the top row
        for (int tileY = clipTopRow; tileY <= clipBottomRow; tileY++) {
            xx = clipLeftX;
            if ((tileY % 2) != 0) {
                xx += tileWidth / 2;
            }

            Unit activeUnit = getActiveUnit();
            // Column per column; start at the left side
            for (int tileX = clipLeftCol; tileX <= clipRightCol; tileX++) {
                Tile unitTile = gameData.getMap().getTileOrNull(tileX, tileY);
                
                if (unitTile != null && unitTile.getUnitCount() > 0 && (unitTile.getSettlement() == null || (activeUnit != null && unitTile.contains(activeUnit)))) {
                    if ((activeUnit != null) && (unitTile.contains(activeUnit))) {
                        displayUnit(g, activeUnit, xx, yy);
                    } else {
                        displayUnit(g, unitTile.getFirstUnit(), xx, yy);
                    }
                }
                xx += tileWidth;
            }
            yy += tileHeight / 2;
        }

        /*
        PART 4
        ======
        Display the messages.
        */

        // Don't edit the list of messages while I'm drawing them.
        synchronized (this) {
            yy = (int) bounds.getHeight() - 115 - MESSAGE_COUNT * 25;
            xx = 40;
            for (int i = getMessageCount() - 1; i >= 0; i--) {
                GUIMessage message = getMessage(i);
                g.setColor(message.getColor());
                g.drawString(message.getMessage(), xx, yy);
                yy += 25;
            }
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
     * @param x The x-coordinate of the location where to draw the Tile
     * (in pixels).
     * @param y The y-coordinate of the location where to draw the Tile
     * (in pixels).
     */
    public void displayTile(Graphics2D g, Map map, Tile tile, int x, int y) {
        g.drawImage(lib.getTerrainImage(tile.getType(), tile.getX(), tile.getY()), x, y, null);

        if (tile.isExplored()) {
            Map.Position pos = new Map.Position(tile.getX(), tile.getY());

            for (int i = 0; i < 8; i++) {
                Map.Position p = map.getAdjacent(pos, i);
                if (map.isValid(p)) {
                    Tile borderingTile = map.getTile(p);

                   if (tile.getType() == borderingTile.getType() ||
                        !borderingTile.isLand() ||
                        !borderingTile.isExplored()) {
                        // Equal tiles, sea tiles and unexplored tiles have no effect
                        continue;
                    }

                    if (!tile.isLand()) {
                        // Draw a beach overlayed with bordering land type
                        g.drawImage(lib.getTerrainImage(ImageLibrary.BEACH,
                                                        i,
                                                        tile.getX(), tile.getY()),
                                    x, y, null);
                        g.drawImage(lib.getTerrainImage(borderingTile.getType(),
                                                        i,
                                                        tile.getX(), tile.getY()),
                                    x, y, null);
                    } else if (borderingTile.getType() < tile.getType()) {
                        // Draw land terrain with bordering land type
                        g.drawImage(lib.getTerrainImage(borderingTile.getType(),
                                                        i,
                                                        tile.getX(), tile.getY()),
                                    x, y, null);
                    }
                }
            }

            // Until the mountain/hill bordering tiles are done... -sjm
/*            if (tile.isLand()) {
                for (int i = 0; i < 8; i++) {            
                    Map.Position p = map.getAdjacent(pos, i);
                    if (map.isValid(p)) {
                        Tile borderingTile = map.getTile(p);
                        if (borderingTile.getAddition() == Tile.ADD_HILLS) {
                            g.drawImage(lib.getTerrainImage(ImageLibrary.HILLS, i, tile.getX(), tile.getY()), x, y - 32, null);
                        } else if (borderingTile.getAddition() == Tile.ADD_MOUNTAINS) {
                            g.drawImage(lib.getTerrainImage(ImageLibrary.MOUNTAINS, i, tile.getX(), tile.getY()), x, y - 32, null);
                        }
                    }
                }
            } */
            
            // Do this after the basic terrain is done or it looks funny. -sjm
            if (tile.isForested()) {
                g.drawImage(lib.getTerrainImage(ImageLibrary.FOREST, tile.getX(), tile.getY()), x, y - 32, null);
            } else if (tile.getAddition() == Tile.ADD_HILLS) {
                g.drawImage(lib.getTerrainImage(ImageLibrary.HILLS, tile.getX(), tile.getY()), x, y - 32, null);
            } else if (tile.getAddition() == Tile.ADD_MOUNTAINS) {
                g.drawImage(lib.getTerrainImage(ImageLibrary.MOUNTAINS, tile.getX(), tile.getY()), x, y - 32, null);
            }
            
            if (tile.isLand()) {
                for (int i = 0; i < 8; i++) {
                    Map.Position p = map.getAdjacent(pos, i);
                    if (map.isValid(p)) {
                        Tile borderingTile = map.getTile(p);
                        if (tile.getType() == borderingTile.getType() ||
                            !borderingTile.isLand() ||
                            !tile.isLand() ||
                            !(borderingTile.getType() < tile.getType()) ||
                            !borderingTile.isExplored()) {
                            // Equal tiles, sea tiles and unexplored tiles have no effect
                            continue;
                        }
                        // Until the forest bordering tils are done... -sjm
                        /*if (borderingTile.isForested()) {
                            g.drawImage(lib.getTerrainImage(ImageLibrary.FOREST, i, tile.getX(), tile.getY()), x, y - 32, null);
                        } else */
                    }
                }
            }

            Settlement settlement = tile.getSettlement();

            if (settlement != null) {
                if (settlement instanceof Colony) {
                    int type = lib.getSettlementGraphicsType(settlement);

                    // Draw image of colony in center of the tile.
                    g.drawImage(lib.getColonyImage(type), x + (lib.getTerrainImageWidth(tile.getType()) - lib.getColonyImageWidth(type)) / 2, y + (lib.getTerrainImageHeight(tile.getType()) - lib.getColonyImageHeight(type)) / 2, null);
                    
                    // The 5 here is arbitrary -sjm
                    g.drawImage(lib.getColorChip(((Colony)settlement).getOwner().getColor()), x + STATE_OFFSET_X, y + 10, null);
                    String populationString = Integer.toString(((Colony)settlement).getUnitCount());
                    if (((Colony)settlement).getOwner().getColor() == Color.BLACK) {
                        g.setColor(Color.WHITE);
                    } else {
                        g.setColor(Color.BLACK);
                    }
                    g.drawString(populationString, x + TEXT_OFFSET_X + STATE_OFFSET_X, y + 10 + TEXT_OFFSET_Y);
                    
                    g.setColor(Color.BLACK);
                    
                    String nameString = ((Colony)settlement).getName();
                    g.drawString(nameString, x + (lib.getTerrainImageWidth(tile.getType()) - g.getFontMetrics().stringWidth(nameString))/2, y + (lib.getTerrainImageHeight(tile.getType()) - lib.getColonyImageHeight(type)) / 2);
                } else if (settlement instanceof IndianSettlement) {
                    int type = lib.getSettlementGraphicsType(settlement);

                    // Draw image of indian settlement in center of the tile.
                    g.drawImage(lib.getIndianSettlementImage(type), x + (lib.getTerrainImageWidth(tile.getType()) - lib.getIndianSettlementImageWidth(type)) / 2, y + (lib.getTerrainImageHeight(tile.getType()) - lib.getIndianSettlementImageHeight(type)) / 2, null);
                    
                    g.drawImage(lib.getColorChip(((IndianSettlement)settlement).getOwner().getColor()), x + STATE_OFFSET_X, y + 10, null);
                    g.drawString("-", x + TEXT_OFFSET_X + STATE_OFFSET_X, y + 10 + TEXT_OFFSET_Y);
                } else {
                    // TODO: Make this a log message:
                    System.err.println("Requested to draw unknown settlement type.");
                }
            }
        }

        if (tile.getPosition().equals(selectedTile)) {
            g.drawImage(lib.getMiscImage(ImageLibrary.UNIT_SELECT), x, y, null);
        }

        if (displayCoordinates) {
            String posString = tile.getX() + ", " + tile.getY();
            g.drawString(posString, x + (lib.getTerrainImageWidth(tile.getType()) - g.getFontMetrics().stringWidth(posString))/2, y + (lib.getTerrainImageHeight(tile.getType()) - g.getFontMetrics().getAscent())/2);
        }
    }


    /**
     * Displays the given Unit onto the given Graphics2D object at the
     * location specified by the coordinates.
     * @param g The Graphics2D object on which to draw the Unit.
     * @param unit The Unit to draw.
     * @param x The x-coordinate of the location where to draw the Unit
     * (in pixels). These are the coordinates of the Tile on which
     * the Unit is located.
     * @param y The y-coordinate of the location where to draw the Unit
     * (in pixels). These are the coordinates of the Tile on which
     * the Unit is located.
     */
    private void displayUnit(Graphics2D g, Unit unit, int x, int y) {
        try {
            // Draw the 'selected unit' image if needed.
            /*if ((unit == getActiveUnit()) && cursor) {
                g.drawImage(lib.getMiscImage(ImageLibrary.UNIT_SELECT), x, y, null);
            }*/

            // Draw the unit.
            int type = lib.getUnitGraphicsType(unit);
            g.drawImage(lib.getUnitImage(type), (x + tileWidth / 2) - lib.getUnitImageWidth(type) / 2, (y + tileHeight / 2) - lib.getUnitImageHeight(type) / 2 - UNIT_OFFSET, null);

            // Draw an occupation and nation indicator.
            g.drawImage(lib.getColorChip(unit.getOwner().getColor()), x + STATE_OFFSET_X, y, null);
            String occupationString;
            switch (unit.getState()) {
                case Unit.ACTIVE:
                    occupationString = "-";
                    break;
                case Unit.FORTIFY:
                    occupationString = "F";
                    break;
                case Unit.SENTRY:
                    occupationString = "S";
                    break;
                case Unit.IN_COLONY:
                    occupationString = "B";
                    break;
                case Unit.PLOW:
                    occupationString = "P";
                    break;
                case Unit.BUILD_ROAD:
                    occupationString = "R";
                    break;
                default:
                    throw new FreeColException("Unit has an invalid occpuation.");
            }
            if (unit.getOwner().getColor() == Color.BLACK) {
                g.setColor(Color.WHITE);
            } else {
                g.setColor(Color.BLACK);
            }
            g.drawString(occupationString, x + TEXT_OFFSET_X + STATE_OFFSET_X, y + TEXT_OFFSET_Y);

            // Draw one small line for each additional unit (like in civ3).
            int unitsOnTile = unit.getTile().getTotalUnitCount();
            if (unitsOnTile > 1) {
                g.setColor(Color.WHITE);
                int unitLinesY = y + OTHER_UNITS_OFFSET_Y;
                for (int i = 0; (i < unitsOnTile) && (i < MAX_OTHER_UNITS); i++) {
                    g.drawLine(x + STATE_OFFSET_X + OTHER_UNITS_OFFSET_X, unitLinesY, x + STATE_OFFSET_X + OTHER_UNITS_OFFSET_X + OTHER_UNITS_WIDTH, unitLinesY);
                    unitLinesY += 2;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Starts a new game. Gives the data of the new game to this GUI.
     * @param data The data of the new game.
     */
    public void setGame(Game data) {
        gameData = data;
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
            return true; // complete repaint about to happen
        }
        if (y > topRow && y < bottomRow &&
            x > leftColumn + 2 && x < rightColumn) {
            // It is within the boundaries, check if it is obscured by
            // the minimap or the info area
            if (y > bottomRow - 2) {
                if (x > leftColumn + 3 && x < rightColumn - 2) {
                    return true;
                }
                // Else case falls through to false return
            } else {
                return true;
            }
        }
        return false;
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
     * @param x The x-coordinate in pixels.
     * @param y The y-coordinate in pixels.
     * @return The map coordinates of the Tile that is located at
     * the given position on the screen.
     */
    public Map.Position convertToMapCoordinates(int x, int y) {
        if ((gameData == null) || (gameData.getMap() == null)) {
            return null;
        }

        int leftOffset;
        if (focus.getX() < getLeftColumns()) {
            // we are at the left side of the map
            if ((focus.getY() % 2) == 0) {
                leftOffset = tileWidth * focus.getX() + tileWidth / 2;
            } else {
                leftOffset = tileWidth * (focus.getX() + 1);
            }
        } else if (focus.getX() >= (gameData.getMap().getWidth() - getRightColumns())) {
            // we are at the right side of the map
            if ((focus.getY() % 2) == 0) {
                leftOffset = (int) bounds.getWidth() - (gameData.getMap().getWidth() - focus.getX()) * tileWidth;
            } else {
                leftOffset = (int) bounds.getWidth() - (gameData.getMap().getWidth() - focus.getX() - 1) * tileWidth - tileWidth / 2;
            }
        } else {
            leftOffset = (int) (bounds.getWidth() / 2);
        }

        int topOffset;
        if (focus.getY() < topRows) {
            // we are at the top of the map
            topOffset = (focus.getY() + 1) * (tileHeight / 2);
        } else if (focus.getY() >= (gameData.getMap().getHeight() - bottomRows)) {
            // we are at the bottom of the map
            topOffset = (int) bounds.getHeight() - (gameData.getMap().getHeight() - focus.getY()) * (tileHeight / 2);
        } else {
            topOffset = (int) (bounds.getHeight() / 2);
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
        if (y >= (gameData.getMap().getHeight() - bottomRows)) {
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
        if (x >= (gameData.getMap().getWidth() - getRightColumns(y))) {
            return true;
        } else {
            return false;
        }
    }


    /**
     * Returns the ImageLibrary that this GUI uses to draw stuff.
     * @return The ImageLibrary that this GUI uses to draw stuff.
     */
    public ImageLibrary getImageLibrary() {
        return lib;
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
        Rectangle result = new Rectangle(0, 0, bounds.width, bounds.height);
        if (y >= topRow && y <= bottomRow && x >= leftColumn && x <= rightColumn) {
            result.y = ((y - topRow) * tileHeight / 2) + topRowY - tileHeight;
            result.x = ((x - leftColumn) * tileWidth) + leftColumnX;
            if ((y % 2) != 0) {
                result.x += tileWidth / 2;
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
}
