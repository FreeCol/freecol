
package net.sf.freecol.client.gui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.Timer;
import javax.swing.UIManager;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Map.Position;



/**
* This class is responsible for drawing the map/background on the <code>Canvas</code>.
* In addition, the graphical state of the map (focus, active unit..) is also a responsibillity
* of this class.
*/
public final class GUI {
    private static final Logger logger = Logger.getLogger(GUI.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";


    private final FreeColClient freeColClient;
    private final Rectangle bounds;
    private final ImageLibrary lib;
    private boolean cursor;

    /** Indicates if the game has started, has nothing to do with whether or not the
        client is logged in. */
    private boolean inGame;

    private final Vector messages;

    private Map.Position selectedTile;
    private Map.Position focus = null;
    private Unit activeUnit;

    /** A path to be displayed on the map. */
    private PathNode dragPath = null;
    private boolean dragStarted = false;

    /** This <code>Random</code>-object should only be used by {@link #drawRoad}. */
    private Random roadRandom = new Random();

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
    STATE_OFFSET_Y = 10,
    ALARM_OFFSET_X = 37,
    ALARM_OFFSET_Y = 10,
    RUMOUR_OFFSET_X = 40,
    RUMOUR_OFFSET_Y = 5,
    MISSION_OFFSET_X = 49,
    MISSION_OFFSET_Y = 10,
    OTHER_UNITS_OFFSET_X = -5, // Relative to the state indicator.
    OTHER_UNITS_OFFSET_Y = 1,
    OTHER_UNITS_WIDTH = 3,
    MAX_OTHER_UNITS = 10,
    MESSAGE_COUNT = 3,
    MESSAGE_AGE = 10000; // The amount of time before a message gets deleted (in milliseconds).

    private boolean displayTileNames = false;
    private boolean displayGrid = false;
    private GeneralPath gridPath = null;

    /** List of the enemy units which should temporarily be displayed at the top. */
    private ArrayList enemyUnitsOnTop = new ArrayList();
    
    // Debug variables:
    boolean displayCoordinates = false;
    boolean displayColonyValue = false;
    boolean debugShowMission = false;


    /**
    * The constructor to use.
    *
    * @param freeColClient The main control class.
    * @param bounds The bounds of the GUI (= the entire screen if the app is displayed in full-screen).
    * @param lib The library of images needed to display certain things visually.
    */
    public GUI(FreeColClient freeColClient, Rectangle bounds, ImageLibrary lib) {
        this.freeColClient = freeColClient;
        this.bounds = bounds;
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
        inGame = false;
        logger.info("GUI created.");

        messages = new Vector(MESSAGE_COUNT);
    }

    /**
     * Starts the unit-selection-cursor blinking animation.
     */
    public void startCursorBlinking() {
        final int delay = 500; // Milliseconds
        final FreeColClient theFreeColClient = freeColClient;
        ActionListener taskPerformer = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                setCursor(!hasCursor());
  
                if (getActiveUnit() != null && getActiveUnit().getTile() != null) {
                    //freeColClient.getCanvas().repaint(0, 0, getWidth(), getHeight());
                    freeColClient.getCanvas().refreshTile(getActiveUnit().getTile());            
                }               
            }
        };
        Timer timer = new Timer(delay, taskPerformer);  
        timer.start();
    }    
    
    /**
     * Checks if the unit selection cursor is currently being displayed.
     * @return <code>true</code> if the cursor is currently being displayed
     *      and <code>false</code> otherwise.
     */
    public boolean hasCursor() {
        return cursor;
    }
    
    
    /**
    * Sets that the unit selection cursor should be displayed.
    * @param cursor Use <code>true</code> to enable the cursor.
    */
    public void setCursor(boolean cursor) {
        this.cursor = cursor;
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

        if (selectedTile != null) {
            if (activeUnit == null ||
                (activeUnit.getTile() != null &&
                 !activeUnit.getTile().getPosition().equals(selectedTile))) {
                Tile t = gameData.getMap().getTile(selectedTile);
                if (t != null && t.getSettlement() != null && t.getSettlement() instanceof Colony
                    && t.getSettlement().getOwner().equals(freeColClient.getMyPlayer())) {

                    setFocus(selectedTile);

                    freeColClient.getCanvas().showColonyPanel((Colony) t.getSettlement());
                    return;
                }

                Unit unitInFront = getUnitInFront(gameData.getMap().getTile(selectedTile));
                if (unitInFront != null) {
                    // Because of the following comment from somewhere else I've put the end in comment -FV
                    // The user might what to check the status of a unit - SG
                    if (unitInFront != activeUnit /*&& unitInFront.getMovesLeft() > 0*/) {
                        // Clear goto order when unit is activated
                        if (clearGoToOrders && unitInFront.getDestination() != null) {
                            freeColClient.getInGameController().clearOrders(unitInFront);
                        }
                        setActiveUnit(unitInFront);
                    } else {
                        freeColClient.getInGameController().clearOrders(unitInFront);
                    }
                } else {
                    setFocus(selectedTile);
                }
            }

            // Check if the gui needs to reposition:
            if (!onScreen(selectedTile)
                    || freeColClient.getClientOptions().getBoolean(ClientOptions.ALWAYS_CENTER)) {
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
            Iterator it = enemyUnitsOnTop.iterator();
            while (it.hasNext()) {
                Unit eu = (Unit) it.next();
                if (eu.getTile() == unitTile) {
                    return eu;
                }
            }
            
            if (unitTile.getSettlement() == null) {
                Unit movableUnit = unitTile.getMovableUnit();
                if (movableUnit != null && movableUnit.getLocation() == movableUnit.getTile()) {
                    return movableUnit;
                } else {
                    Unit bestPick = null;
                    Iterator unitIterator = unitTile.getUnitIterator();
                    while (unitIterator.hasNext()) {
                        Unit u = (Unit) unitIterator.next();
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
            enemyUnitsOnTop.add(0, activeUnit);
            freeColClient.getCanvas().repaint(0, 0, getWidth(), getHeight());
            return;
        }

        if (activeUnit != null && activeUnit.getTile() == null) {
            activeUnit = null;
        }

        this.activeUnit = activeUnit;

        if (activeUnit != null && freeColClient.getGame().getCurrentPlayer() == freeColClient.getMyPlayer() && activeUnit.getState() != Unit.ACTIVE) {
            freeColClient.getInGameController().clearOrders(activeUnit);
        }

        freeColClient.getActionManager().update();
        freeColClient.getCanvas().updateJMenuBar();

        //TODO: update only within the bounds of InfoPanel
        freeColClient.getCanvas().repaint(0, 0, getWidth(), getHeight());

        //if (activeUnit != null && !activeUnit.getTile().getPosition().equals(selectedTile)) {
        if (activeUnit != null) {
            setSelectedTile(activeUnit.getTile().getPosition());
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

        enemyUnitsOnTop.clear();
        
        forceReposition();
        freeColClient.getCanvas().repaint(0, 0, getWidth(), getHeight());
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
        if ((freeColClient.getGame() != null)
                && (freeColClient.getGame().getMap() != null)
                && (focus != null)
                && inGame) {
            displayMap(g);
        } else {
            Image bgImage = (Image) UIManager.get("CanvasBackgroundImage");

            if (bgImage != null) {
                if (bgImage.getWidth(null) != bounds.width || bgImage.getHeight(null) != bounds.height) {
                    bgImage = bgImage.getScaledInstance(bounds.width, bounds.height, Image.SCALE_SMOOTH);
                    UIManager.put("CanvasBackgroundImage", bgImage);

                    /*
                      We have to use a MediaTracker to ensure that the
                      image has been scaled before we paint it.
                    */
                    MediaTracker mt = new MediaTracker(freeColClient.getCanvas());
                    mt.addImage(bgImage, 0, bounds.width, bounds.height);

                    try {
                        mt.waitForID(0);
                    } catch (InterruptedException e) {
                        g.setColor(Color.black);
                        g.fillRect(0, 0, bounds.width, bounds.height);
                        return;
                    }

                }

                g.drawImage(bgImage, 0, 0, null);
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
        Game gameData = freeColClient.getGame();

        if (focus == null) {
            return;
        }
        
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
        Game gameData = freeColClient.getGame();
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
        PART 1b
        =======
        Create a GeneralPath to draw the grid with, if needed.
        */
        if (displayGrid) {
            gridPath = new GeneralPath();
            gridPath.moveTo(0, 0);
            int nextX = tileWidth / 2;
            int nextY = - (tileHeight / 2);

            for (int i = 0; i <= ((clipRightCol - clipLeftCol) * 2 + 1); i++) {
                gridPath.lineTo(nextX, nextY);
                nextX += tileWidth / 2;
                if (nextY == - (tileHeight / 2)) {
                    nextY = 0;
                }
                else {
                    nextY = - (tileHeight / 2);
                }
            }
        }

        /*
        PART 2
        ======
        Display the Tiles and the Units.
        */

        g.setColor(Color.black);
        g.fillRect(clipBounds.x, clipBounds.y, clipBounds.width, clipBounds.height);
        int xx;
        Map map = gameData.getMap();

        /*
        PART 2a
        =======
        Display the base Tiles
        */
        
        int yy = clipTopY;

        // Row per row; start with the top modified row
        for (int tileY = clipTopRow; tileY <= clipBottomRow; tileY++) {
            xx = clipLeftX;
            if ((tileY % 2) != 0) {
                xx += tileWidth / 2;
            }

            // Column per column; start at the left side to display the tiles.
            for (int tileX = clipLeftCol; tileX <= clipRightCol; tileX++) {
                if (map.isValid(tileX, tileY)) {
                    // Display the Tile:
                    displayBaseTile(g, map, map.getTile(tileX, tileY), xx, yy, true);
                }
                xx += tileWidth;
            }

            yy += tileHeight / 2;
        }

        /*
        PART 2b
        =======
        Display the Tile overlays and Units
        */
        
        yy = clipTopY;

        // Row per row; start with the top modified row
        for (int tileY = clipTopRow; tileY <= clipBottomRow; tileY++) {
            xx = clipLeftX;
            if ((tileY % 2) != 0) {
                xx += tileWidth / 2;
            }

            if (displayGrid) {
                // Display the grid.
                g.translate(xx, yy + (tileHeight / 2));
                g.setColor(Color.BLACK);
                g.draw(gridPath);
                g.translate(- xx, - (yy + (tileHeight / 2)));
            }

            // Column per column; start at the left side to display the tiles.
            for (int tileX = clipLeftCol; tileX <= clipRightCol; tileX++) {
                if (map.isValid(tileX, tileY)) {
                    // Display the Tile overlays:
                    displayTileOverlays(g, map, map.getTile(tileX, tileY), xx, yy, true);
                }
                xx += tileWidth;
            }

            xx = clipLeftX;
            if ((tileY % 2) != 0) {
                xx += tileWidth / 2;
            }

            // Again, column per column starting at the left side. Now display the units
            for (int tileX = clipLeftCol; tileX <= clipRightCol; tileX++) {
                if (map.isValid(tileX, tileY)) {
                    // Display any units on that Tile:
                    //Tile unitTile = map.getTile(tileX, tileY);
                    Unit unitInFront = getUnitInFront(map.getTile(tileX, tileY));
                    if (unitInFront != null) {
                        displayUnit(g, unitInFront, xx, yy);
                    }
                }
                xx += tileWidth;
            }

            yy += tileHeight / 2;
        }

        /*
        PART 3
        ======
        Display the colony names.
        */
        xx = 0;
        yy = clipTopY;
        // Row per row; start with the top modified row
        for (int tileY = clipTopRow; tileY <= clipBottomRow; tileY++) {
            xx = clipLeftX;
            if ((tileY % 2) != 0) {
                xx += tileWidth / 2;
            }
            // Column per column; start at the left side
            for (int tileX = clipLeftCol; tileX <= clipRightCol; tileX++) {
                if (map.isValid(tileX, tileY)) {
                    Tile tile = map.getTile(tileX, tileY);
                    if (tile.getSettlement() instanceof Colony) {
                        Colony colony = (Colony) map.getTile(tileX, tileY).getSettlement();
                        BufferedImage stringImage = createStringImage(g, colony.getName(), colony.getOwner().getColor(), lib.getTerrainImageWidth(tile.getType()) * 4/3, 16);
                        g.drawImage(stringImage, xx + (lib.getTerrainImageWidth(tile.getType()) - stringImage.getWidth())/2 + 1, yy + (lib.getColonyImageHeight(lib.getSettlementGraphicsType(colony))) + 1, null);
                    }
                }
                xx += tileWidth;
            }
            yy += tileHeight / 2;
        }

        /*
        PART 4
        ======
        Display drag-and-drop path
        */

        if (dragPath != null) {
            PathNode temp = dragPath;
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
                    if (image != null) {
                        g.drawImage(image, p.x + (tileWidth - image.getWidth(null))/2, p.y + (tileHeight - image.getHeight(null))/2, null);
                    } else {
                        g.fillOval(p.x + tileWidth/2, p.y + tileHeight/2, 10, 10);
                        g.setColor(Color.BLACK);
                        g.drawOval(p.x + tileWidth/2, p.y + tileHeight/2, 10, 10);
                    }                
                    if (temp.getTurns() > 0) {
                        BufferedImage stringImage = createStringImage(g, Integer.toString(temp.getTurns()), textColor, tileWidth, 12);
                        g.drawImage(stringImage, p.x + (tileWidth - stringImage.getWidth(null))/2, p.y + (tileHeight - stringImage.getHeight()) / 2, null);
                    }
                }                    
                temp = temp.next;
            }
        }

        /*
        PART 5
        ======
        Display the messages.
        */

        // Don't edit the list of messages while I'm drawing them.
        synchronized (this) {
            BufferedImage si = createStringImage(g, "getSizes", Color.WHITE, bounds.width, 12);

            yy = (int) bounds.getHeight() - 300 - getMessageCount() * si.getHeight();// 200 ;
            xx = 40;

            for (int i = 0; i < getMessageCount(); i++) {
                GUIMessage message = getMessage(i);
                g.drawImage(createStringImage(g, message.getMessage(), message.getColor(), bounds.width, 12), xx, yy, null);
                yy += si.getHeight();
            }
        }
    }
    
    /**
     * Gets an image to represent the path of the given <code>Unit</code>.
     * 
     * @param u The <code>Unit</code>
     * @return The <code>Image</code>.
     */
    private Image getPathImage(Unit u) {
        if (u == null || u.isNaval()) {
            return (Image) UIManager.get("path.naval.image");
        } else if (u.isMounted()) {
            return (Image) UIManager.get("path.horse.image");
        } else if (u.getType() == Unit.WAGON_TRAIN || u.getType() == Unit.TREASURE_TRAIN || u.getType() == Unit.ARTILLERY || u.getType() == Unit.DAMAGED_ARTILLERY) {
            return (Image) UIManager.get("path.wagon.image");
        } else {
            return (Image) UIManager.get("path.foot.image");
        }
    }
    
    /**
     * Gets an image to represent the path of the given <code>Unit</code>.
     * 
     * @param u The <code>Unit</code>
     * @return The <code>Image</code>.
     */
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
    
    /**
     * Gets an image to represent the path of the given <code>Unit</code>.
     * 
     * @param u The <code>Unit</code>
     * @return The <code>Image</code>.
     */
    private Image getPathNextTurnImage(Unit u) {
        if (u == null || u.isNaval()) {
            return (Image) UIManager.get("path.naval.nextTurn.image");
        } else if (u.isMounted()) {
            return (Image) UIManager.get("path.horse.nextTurn.image");
        } else if (u.getType() == Unit.WAGON_TRAIN || u.getType() == Unit.TREASURE_TRAIN || u.getType() == Unit.ARTILLERY || u.getType() == Unit.DAMAGED_ARTILLERY) {
            return (Image) UIManager.get("path.wagon.nextTurn.image");
        } else {
            return (Image) UIManager.get("path.foot.nextTurn.image");
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
    * @param maxWidth The maximum width of the image. The size of 
    *       the <code>Font</code> will be adjusted if the image gets 
    *       larger than this value.
    * @param preferredFontSize The preferred font size.
    * @return The image that was created.
    */
    public BufferedImage createStringImage(Graphics2D g, String nameString, Color color, int maxWidth, int preferredFontSize) {
        return createStringImage(null, g, nameString, color, maxWidth, preferredFontSize);
    }
    
    /**
     * Creates an image with a string of a given color and with 
     * a black border around the glyphs.
     *
     * @param c A <code>JComponent</code>-object for getting a
     *       <code>Font</code>.
     * @param nameString The <code>String</code> to make an image of.
     * @param color The <code>Color</code> to use when displaying 
     *       the <code>nameString</code>.
     * @param maxWidth The maximum width of the image. The size of 
     *       the <code>Font</code> will be adjusted if the image gets 
     *       larger than this value.
     * @param preferredFontSize The preferred font size.
     * @return The image that was created.
     */
    public BufferedImage createStringImage(JComponent c, String nameString, Color color, int maxWidth, int preferredFontSize) {
        return createStringImage(c, null, nameString, color, maxWidth, preferredFontSize);
    }    
    
    /**
     * Creates an image with a string of a given color and with 
     * a black border around the glyphs.
     *
     * @param c A <code>JComponent</code>-object for getting a
     *       <code>Font</code>.
     * @param g A <code>Graphics</code>-object for getting a
     *       <code>Font</code>.
     * @param nameString The <code>String</code> to make an image of.
     * @param color The <code>Color</code> to use when displaying 
     *       the <code>nameString</code>.
     * @param maxWidth The maximum width of the image. The size of 
     *       the <code>Font</code> will be adjusted if the image gets 
     *       larger than this value.
     * @param preferredFontSize The preferred font size.
     * @return The image that was created.
     */
    private BufferedImage createStringImage(JComponent c, Graphics g, String nameString, Color color, int maxWidth, int preferredFontSize) {        
        Font nameFont = (c != null) ? c.getFont() : g.getFont();
        FontMetrics nameFontMetrics = (c != null) ? c.getFontMetrics(nameFont) : g.getFontMetrics(nameFont);
        BufferedImage bi = null;

        int fontSize = preferredFontSize;
        do {
            nameFont = nameFont.deriveFont(Font.BOLD, fontSize);            
            nameFontMetrics = (c != null) ? c.getFontMetrics(nameFont) : g.getFontMetrics(nameFont);
            bi = new BufferedImage(nameFontMetrics.stringWidth(nameString) + 4, nameFontMetrics.getMaxAscent() + nameFontMetrics.getMaxDescent(), BufferedImage.TYPE_INT_ARGB);
            fontSize -= 2;
        } while (bi.getWidth() > maxWidth);

        Graphics2D big = bi.createGraphics();

        big.setColor(color);
        big.setFont(nameFont);
        big.drawString(nameString, 2, nameFontMetrics.getMaxAscent());

        int playerColor = color.getRGB();
        for (int biX=0; biX<bi.getWidth(); biX++) {
            for (int biY=0; biY<bi.getHeight(); biY++) {
                int r = bi.getRGB(biX, biY);

                if (r == playerColor) {
                    continue;
                }

                for (int cX=-1; cX <=1; cX++) {
                    for (int cY=-1; cY <=1; cY++) {
                        if (biX+cX >= 0 && biY+cY >= 0 && biX+cX < bi.getWidth() && biY+cY < bi.getHeight() && bi.getRGB(biX + cX, biY + cY) == playerColor) {
                            if (playerColor != Color.BLACK.getRGB()) {
                                bi.setRGB(biX, biY, Color.BLACK.getRGB());
                            } else {
                                bi.setRGB(biX, biY, Color.WHITE.getRGB());
                            }
                            continue;
                        }
                    }
                }
            }
        }

        return bi;
    }


    /**
    * Creates an illustration for a goods production.
    *
    * @param goodsIcon   The icon representing the goods.
    * @param production  The amount of goods that is being produced.
    * @param width       The width of the image to deliver.
    * @param height      The height of the image to deliver.
    * @return The image.
    */
    public BufferedImage createProductionImage(ImageIcon goodsIcon, int production,
                                               int width, int height) {
        int limit = freeColClient.getClientOptions().getInteger(
                ClientOptions.MAX_NUMBER_OF_GOODS_IMAGES);
        return createProductionImage(goodsIcon, production, width, height, limit, false);
    }


    /**
    * Creates an illustration for a goods production.
    *
    * @param goodsIcon   The icon representing the goods.
    * @param production  The amount of goods that is being produced.
    * @param width       The width of the image to deliver.
    * @param height      The height of the image to deliver.
    * @param limit       This value is used instead of the defult value
    *                    ({@link ClientOptions#MAX_NUMBER_OF_GOODS_IMAGES}).
    * @return The image.
    */
    public BufferedImage createProductionImage(ImageIcon goodsIcon, int production, int width, int height, int limit) {
        BufferedImage bi = createProductionImage(goodsIcon, production, width, height, limit, true);
        return bi;
    }

    /**
    * Creates an illustration for a goods production, allows the plus to be requested for drawing.
    *
    * @param goodsIcon   The icon representing the goods.
    * @param production  The amount of goods that is being produced.
    * @param width       The width of the image to deliver.
    * @param height      The height of the image to deliver.
    * @param limit       This value is used instead of the defult value
    *                    ({@link ClientOptions#MAX_NUMBER_OF_GOODS_IMAGES}).
    * @param drawPlus    Flag to determine if a "+" should preceed the number drawn
    * @return The image.
    */
    public BufferedImage createProductionImage(ImageIcon goodsIcon, final int production, int width,
                                               int height, int limit, boolean drawPlus) {
        ClientOptions co = freeColClient.getClientOptions();
        int displayIconCutoffCount = limit;
        int displayIconsIfOverMax = 4;
        int displayNumbersIfOverCount = Math.min(
                co.getInteger(ClientOptions.MIN_NUMBER_FOR_DISPLAYING_GOODS_COUNT),
                displayIconCutoffCount
            );
        BufferedImage bi = createProductionImage(goodsIcon, production, width, height,
                displayIconCutoffCount, displayIconsIfOverMax, displayNumbersIfOverCount,
                drawPlus);
        return bi;
    }

    /**
    * Creates an illustration for a goods production.
    *
    * @param goodsIcon The icon representing the goods.
    * @param production The amount of goods that is beeing produced.
    * @param width The width of the image to deliver.
    * @param height The height of the image to deliver.
    * @param displayIconCutoffCount If production is over this amount,
    *        then <code>displayIconsIfOverMax</code> number of goods images
    *        is displayed.
    * @param displayIconsIfOverMax The amount of goods images to display
    *        if the production is higher than
    *        <code>displayIconCutoffCount</code>.
    * @param displayNumbersIfOverCount If production is over this number,
    *        then we display the production as a number as well.
    * @param drawPlus If <code>true</code>, then a plus is drawn
    *        preceeding non-negative numbers (<code>&gt;= 0</code>)
    * @return The image.
    */
    public BufferedImage createProductionImage(ImageIcon goodsIcon,
            final int production, int width, int height,
            int displayIconCutoffCount, int displayIconsIfOverMax,
            int displayNumbersIfOverCount, boolean drawPlus) {

        BufferedImage image = createProductionImage(goodsIcon, production, width, height,
                displayIconCutoffCount, displayIconsIfOverMax, displayNumbersIfOverCount,
                drawPlus, true, Math.min(width, (goodsIcon.getIconWidth()*3)/2));
        return image;
    }

    /**
    * Creates an illustration for a goods production.
    *
    * @param goodsIcon The icon representing the goods.
    * @param production The amount of goods that is beeing produced.
    * @param width The width of the image to deliver.
    * @param height The height of the image to deliver.
    * @param displayIconCutoffCount If production is over this amount,
    *        then <code>displayIconsIfOverMax</code> number of goods images
    *        is displayed.
    * @param displayIconsIfOverMax The amount of goods images to display
    *        if the production is higher than
    *        <code>displayIconCutoffCount</code>.
    * @param displayNumbersIfOverCount If production is over this number,
    *        then we display the production as a number as well.
    * @param drawPlus If <code>true</code>, then a plus is drawn
    *        preceeding non-negative numbers (<code>&gt;= 0</code>)
    * @param center boolean When <code>false</code> everything is
    *        left-justified. When <code>true</code>, the image is
    *        centered.
    * @param compressedWidth This width is used as a maximum widht
    *        for the goods images if the
    *        <code>production &gt; displayNumbersIfOverCount</code>.
    * @return The image.
    */
    public BufferedImage createProductionImage(ImageIcon goodsIcon,
            final int production, int width, int height,
            int displayIconCutoffCount, int displayIconsIfOverMax,
            int displayNumbersIfOverCount, boolean drawPlus,
            boolean center, int compressedWidth) {

        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();

        int drawMax = displayIconCutoffCount;
        int drawImageCount = Math.abs(production);

        if (drawImageCount > drawMax) {
            drawImageCount = displayIconsIfOverMax;
        } else {
            compressedWidth = width;
        }
        boolean drawNoImages = (drawImageCount == 0);
        if (drawNoImages) {
            drawImageCount = 1;
        }

        int iconWidth = goodsIcon.getIconWidth();
        int pixelsPerIcon = compressedWidth / drawImageCount;
        if (pixelsPerIcon-iconWidth < 0) {
            pixelsPerIcon = (compressedWidth - iconWidth) / drawImageCount;
        }
        int maxSpacing = iconWidth;

        /* TODO Tune this: all icons are the same width, but many
         * do not take up the whole width, eg. bells
         */
        boolean iconsTooFarApart = pixelsPerIcon > maxSpacing;
        if (iconsTooFarApart) {
            pixelsPerIcon = maxSpacing;
        }
        int coverage = pixelsPerIcon * (drawImageCount-1) + iconWidth;
        int leftOffset = 0;
        boolean needToCenterImages = center && coverage < width;
        if (needToCenterImages) {
            leftOffset = (width - coverage)/2;
        }

        // Draw the icons onto the image:
        for (int i=0; i<drawImageCount; i++) {
            goodsIcon.paintIcon(null, g, leftOffset + i*pixelsPerIcon, 0);
        }

        boolean displayNumbers = production >= displayNumbersIfOverCount
                || (production != drawImageCount);

        if (displayNumbers) {
            Color textColor = (production < 0) ? Color.RED : Color.WHITE;
            String number = Integer.toString(production);
            if( production >= 0 && drawPlus ) {
                number = "+"+ number;
            }
            BufferedImage stringImage = createStringImage(
                    g, number, textColor, goodsIcon.getIconWidth()*2, 12);
            int textOffset = leftOffset + (coverage-stringImage.getWidth())/2;
            textOffset = (textOffset >= 0) ? textOffset : 0;
            g.drawImage(stringImage, textOffset,
                    goodsIcon.getIconHeight()/2 - stringImage.getHeight()/2, null);
        }

        return bi;
    }


    /**
    * Draws a road, between the given points, on the provided <code>Graphics</code>.
    * When you provide the same <code>seed</code> you will get the same road.
    *
    * @param g The <code>Graphics</code> to draw the road upon.
    * @param seed The seed of the random generator that is creating the road.
    * @param x1 The x-component of the first coordinate.
    * @param y1 The y-component of the first coordinate.
    * @param x2 The x-component of the second coordinate.
    * @param y2 The y-component of the second coordinate.
    */
    public void drawRoad(Graphics2D g, long seed, int x1, int y1, int x2, int y2) {
        final int MAX_CORR = 4;
        Color oldColor = g.getColor();
        roadRandom.setSeed(seed);

        int i = Math.max(Math.abs(x2-x1), Math.abs(y2-y1));
        int baseX = x1;
        int baseY = y1;
        double addX = (x2-x1)/((double) i);
        double addY = (y2-y1)/((double) i);
        int corr = 0;
        int xCorr = 0;
        int yCorr = 0;
        int lastDiff = 1;

        g.setColor(new Color(128, 64, 0));
        g.drawLine(baseX, baseY, baseX, baseY);

        for (int j=1; j<=i; j++) {
            int oldCorr = corr;
            //if (roadRandom.nextInt(3) == 0) {
                corr = corr + roadRandom.nextInt(3)-1;
                if (oldCorr != corr) {
                    lastDiff = oldCorr - corr;
                }
            //}

            if (Math.abs(corr) > MAX_CORR || Math.abs(corr) >= i-j) {
                if (corr > 0) {
                    corr--;
                } else {
                    corr++;
                }
            }

            if (corr != oldCorr) {
                g.setColor(new Color(128, 128, 0));
                g.drawLine(baseX+(int) (j*addX)+xCorr, baseY+(int) (j*addY)+yCorr, baseX+(int) (j*addX)+xCorr, baseY+(int) (j*addY)+yCorr);
            } else {
                int oldXCorr = 0;
                int oldYCorr = 0;

                if (x2-x1 == 0) {
                    oldXCorr = corr+lastDiff;
                    oldYCorr = 0;
                } else if (y2-y1 == 0) {
                    oldXCorr = 0;
                    oldYCorr = corr+lastDiff;
                } else {
                    if (corr > 0) {
                        oldXCorr = corr+lastDiff;
                    } else {
                        oldYCorr = corr+lastDiff;
                    }
                }

                g.setColor(new Color(128, 128, 0));
                g.drawLine(baseX+(int) (j*addX)+oldXCorr, baseY+(int) (j*addY)+oldYCorr, baseX+(int) (j*addX)+oldXCorr, baseY+(int) (j*addY)+oldYCorr);
            }

            if (x2-x1 == 0) {
                xCorr = corr;
                yCorr = 0;
            } else if (y2-y1 == 0) {
                xCorr = 0;
                yCorr = corr;
            } else {
                if (corr > 0) {
                    xCorr = corr;
                } else {
                    yCorr = corr;
                }
            }

            g.setColor(new Color(128, 64, 0));
            g.drawLine(baseX+(int) (j*addX)+xCorr, baseY+(int) (j*addY)+yCorr, baseX+(int) (j*addX)+xCorr, baseY+(int) (j*addY)+yCorr);
        }
        g.setColor(oldColor);
    }


    /**
     * Displays the given <code>Tile</code> onto the given 
     * <code>Graphics2D</code> object at the location specified 
     * by the coordinates. The visualization of the <code>Tile</code>
     * also includes information from the coresponding
     * <code>ColonyTile</code> from the given <code>Colony</code>.
     * 
     * @param g The <code>Graphics2D</code> object on which to draw 
     *      the <code>Tile</code>.
     * @param map The <code>Map</code>.
     * @param tile The <code>Tile</code> to draw.
     * @param x The x-coordinate of the location where to draw the Tile
     *      (in pixels).
     * @param y The y-coordinate of the location where to draw the Tile
     *      (in pixels).
     * @param colony The <code>Colony</code> to create the visualization
     *      of the <code>Tile</code> for. This object is also used to
     *      get the <code>ColonyTile</code> for the given <code>Tile</code>.
     */
    public void displayColonyTile(Graphics2D g, Map map, Tile tile, int x, int y, Colony colony) {
        //displayTile(g, map, tile, x, y, false);
        displayBaseTile(g, map, tile, x, y, false);        
        if (tile.getOwner() != null && tile.getOwner() != colony) {
            g.drawImage(lib.getMiscImage(ImageLibrary.TILE_TAKEN), x, y, null);
        }
        displayTileOverlays(g, map, tile, x, y, false);
        int nation = tile.getNationOwner();
        if (nation != Player.NO_NATION
                && !Player.isEuropean(nation)
                && nation != colony.getOwner().getNation()
                && tile.getSettlement() == null
                && !colony.getOwner().hasFather(FoundingFather.PETER_MINUIT)) {
            Image image = lib.getMiscImage(ImageLibrary.TILE_OWNED_BY_INDIANS);
            g.drawImage(image, x+tileWidth/2-image.getWidth(null)/2, y+tileHeight/2-image.getHeight(null)/2, null);
        }
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
     * @param x The x-coordinate of the location where to draw the Tile
     * (in pixels).
     * @param y The y-coordinate of the location where to draw the Tile
     * (in pixels).
     */
    public void displayTile(Graphics2D g, Map map, Tile tile, int x, int y) {
        displayTile(g, map, tile, x, y, true);
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
     * @param drawUnexploredBorders If true; draws border between explored and
     *        unexplored terrain.
     */
    public void displayTile(Graphics2D g, Map map, Tile tile, int x, int y, boolean drawUnexploredBorders) {
        displayBaseTile(g, map, tile, x, y, drawUnexploredBorders);
        displayTileOverlays(g, map, tile, x, y, drawUnexploredBorders);
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
     * @param drawUnexploredBorders If true; draws border between explored and
     *        unexplored terrain.
     */
    private void displayBaseTile(Graphics2D g, Map map, Tile tile, int x, int y, boolean drawUnexploredBorders) {
        g.drawImage(lib.getTerrainImage(tile.getType(), tile.getX(), tile.getY()), x, y, null);

        Map.Position pos = new Map.Position(tile.getX(), tile.getY());


        for (int i = 0; i < 8; i++) {
            Map.Position p = map.getAdjacent(pos, i);
            if (map.isValid(p)) {
                Tile borderingTile = map.getTile(p);

                if (!drawUnexploredBorders && !borderingTile.isExplored() && i >= 3 && i <= 5) {
                    continue;
                }

                if (tile.getType() == borderingTile.getType() || !borderingTile.isLand() && borderingTile.getType() != Tile.OCEAN){
                    // Equal tiles and sea tiles have no effect
                    continue;
                }

                if (!tile.isLand() && borderingTile.isExplored() && borderingTile.getType() != Tile.OCEAN) {
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
     * @param drawUnexploredBorders If true; draws border between explored and
     *        unexplored terrain.
     */
    private void displayTileOverlays(Graphics2D g, Map map, Tile tile, int x, int y, boolean drawUnexploredBorders) {  
        Map.Position pos = new Map.Position(tile.getX(), tile.getY());

        if (!tile.isExplored()) {
            g.drawImage(lib.getTerrainImage(tile.getType(), tile.getX(), tile.getY()), x, y, null);
        } else {
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
            /*
            if (tile.isForested()) {
                //g.drawImage(lib.getTerrainImage(ImageLibrary.FOREST, tile.getX(), tile.getY()), x, y - 32, null);
            } else
            */
            if (tile.getAddition() == Tile.ADD_HILLS) {
                g.drawImage(lib.getTerrainImage(ImageLibrary.HILLS, tile.getX(), tile.getY()), x, y - 32, null);
            } else if (tile.getAddition() == Tile.ADD_MOUNTAINS) {
                g.drawImage(lib.getTerrainImage(ImageLibrary.MOUNTAINS, tile.getX(), tile.getY()), x, y - 32, null);
            }

            if (tile.isPlowed()) {
                g.drawImage(lib.getMiscImage(ImageLibrary.PLOWED), x, y, null);
            }

            if (tile.getRiver() != 0) {
                g.drawImage(lib.getRiverImage(tile.getRiver()), x, y, null);
            }
            if (tile.isForested()) {
                g.drawImage(lib.getForestImage(tile.getType()), x, y, null);
            }

            if (tile.hasBonus()) {
                Image bonusImage = lib.getBonusImage(tile);
                if (bonusImage != null) {
                    g.drawImage(bonusImage, x + tileWidth/2 - bonusImage.getWidth(null)/2, y + tileHeight/2 - bonusImage.getHeight(null)/2, null);
                }
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

            // Paint the roads:
            if (tile.hasRoad()) {
                long seed = Long.parseLong(Integer.toString(tile.getX()) + Integer.toString(tile.getY()));
                boolean connectedRoad = false;
                for (int i = 0; i < 8; i++) {
                    Map.Position p = map.getAdjacent(pos, i);
                    if (map.isValid(p)) {
                        Tile borderingTile = map.getTile(p);
                        if (borderingTile.hasRoad()) {
                            connectedRoad =  true;
                            int nx = x + tileWidth/2;
                            int ny = y + tileHeight/2;

                            switch (i) {
                                case 0: nx = x + tileWidth/2; ny = y; break;
                                case 1: nx = x + (tileWidth*3)/4; ny = y + tileHeight/4; break;
                                case 2: nx = x + tileWidth; ny = y + tileHeight/2; break;
                                case 3: nx = x + (tileWidth*3)/4; ny = y + (tileHeight*3)/4; break;
                                case 4: nx = x + tileWidth/2; ny = y + tileHeight; break;
                                case 5: nx = x + tileWidth/4; ny = y + (tileHeight*3)/4; break;
                                case 6: nx = x; ny = y + tileHeight/2; break;
                                case 7: nx = x + tileWidth/4; ny = y + tileHeight/4; break;
                            }

                            drawRoad(g, seed, x + tileWidth/2, y + tileHeight/2, nx, ny);
                        }
                    }
                }

                if (!connectedRoad) {
                    drawRoad(g, seed, x + tileWidth/2 - 10, y + tileHeight/2, x + tileWidth/2 + 10, y + tileHeight/2);
                    drawRoad(g, seed, x + tileWidth/2, y + tileHeight/2 - 10, x + tileWidth/2, y + tileHeight/2 + 10);
                }
            }

            Settlement settlement = tile.getSettlement();

            if (settlement != null) {
                if (settlement instanceof Colony) {
                    int type = lib.getSettlementGraphicsType(settlement);

                    // Draw image of colony in center of the tile.
                    g.drawImage(lib.getColonyImage(type), x + (lib.getTerrainImageWidth(tile.getType()) - lib.getColonyImageWidth(type)) / 2, y + (lib.getTerrainImageHeight(tile.getType()) - lib.getColonyImageHeight(type)) / 2, null);

                    String populationString = Integer.toString(((Colony)settlement).getUnitCount());
                    Color theColor = Color.WHITE;

                    int sol = ((Colony)settlement).getSoL();

                    if (sol >= 100) {
                        theColor = Color.BLUE;
                    } else if (sol >= 50) {
                        theColor = Color.GREEN;
                    } else if (((Colony)settlement).getProductionBonus() < 0) {
                        theColor = Color.RED;
                    }

                    BufferedImage stringImage = createStringImage(g, populationString, theColor, lib.getTerrainImageWidth(tile.getType()), 12);
                    g.drawImage(stringImage, x + (lib.getTerrainImageWidth(tile.getType()) - stringImage.getWidth())/2 + 1, y + ((lib.getTerrainImageHeight(tile.getType()) - stringImage.getHeight()) / 2) + 1, null);

                    g.setColor(Color.BLACK);
                } else if (settlement instanceof IndianSettlement) {
                    int type = lib.getSettlementGraphicsType(settlement);

                    // Draw image of indian settlement in center of the tile.
                    g.drawImage(lib.getIndianSettlementImage(type), x + (lib.getTerrainImageWidth(tile.getType()) - lib.getIndianSettlementImageWidth(type)) / 2, y + (lib.getTerrainImageHeight(tile.getType()) - lib.getIndianSettlementImageHeight(type)) / 2, null);

                    // Draw the color chip for the settlement.
                    g.drawImage(lib.getColorChip(((IndianSettlement)settlement).getOwner().getColor()), x + STATE_OFFSET_X, y + STATE_OFFSET_Y, null);

                    // Draw the mission chip if needed.
                    if (((IndianSettlement)settlement).getMissionary() != null) {
                        Unit missionary = ((IndianSettlement)settlement).getMissionary();
                        boolean expert = (missionary.getType() == Unit.JESUIT_MISSIONARY);
                        g.drawImage(lib.getMissionChip(missionary.getOwner().getColor(), expert), x + MISSION_OFFSET_X, y + MISSION_OFFSET_Y, null);
                    }

                    // Draw the alarm chip if needed.
                    if (freeColClient.getMyPlayer().hasContacted(((IndianSettlement)settlement).getOwner().getNation())) {
                        g.drawImage(lib.getAlarmChip(((IndianSettlement)settlement).getAlarm(freeColClient.getMyPlayer()).getLevel()), x + ALARM_OFFSET_X, y + ALARM_OFFSET_Y, null);
                    }

                    g.setColor(Color.BLACK);
                    g.drawString("-", x + TEXT_OFFSET_X + STATE_OFFSET_X, y + STATE_OFFSET_Y + TEXT_OFFSET_Y);
                } else {
                    logger.warning("Requested to draw unknown settlement type.");
                }
            } else if (tile.hasLostCityRumour()) {
                g.drawImage(lib.getMiscImage(ImageLibrary.LOST_CITY_RUMOUR),
                            x + RUMOUR_OFFSET_X, y + RUMOUR_OFFSET_Y, null);
            }

            if (FreeCol.isInDebugMode() && !freeColClient.getMyPlayer().canSee(tile)) {
                g.setColor(Color.BLACK);
                Composite oldComposite = g.getComposite();
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f));
                Polygon pol = new Polygon(new int[] {x + tileWidth/2, x + tileWidth, x + tileWidth/2, x},
                                          new int[] {y, y + tileHeight/2, y + tileHeight, y + tileHeight/2},
                                          4);
                g.fill(pol);
                g.setComposite(oldComposite);
            }

            if (drawUnexploredBorders) {
                for (int i = 3; i < 6; i++) {
                    Map.Position p = map.getAdjacent(pos, i);
                    if (map.isValid(p)) {
                        Tile borderingTile = map.getTile(p);

                        if (borderingTile.isExplored()){
                            continue;
                        }

                        g.drawImage(lib.getTerrainImage(borderingTile.getType(), i, tile.getX(), tile.getY()), x, y, null);
                    }
                }
            }
        }

        if (displayTileNames) {
            String tileName = tile.getName();
            g.setColor(Color.BLACK);
            int b = getBreakingPoint(tileName);
            if (b == -1) {
                g.drawString(tileName, x + (lib.getTerrainImageWidth(tile.getType()) - g.getFontMetrics().stringWidth(tileName))/2, y + (lib.getTerrainImageHeight(tile.getType())/2));
                /* Takes to much resources:
                BufferedImage stringImage = createStringImage(g, tileName, Color.BLACK, lib.getTerrainImageWidth(tile.getType()), 10);
                g.drawImage(stringImage, x + (lib.getTerrainImageWidth(tile.getType()) - stringImage.getWidth())/2 + 1, y + lib.getTerrainImageHeight(tile.getType())/2 - stringImage.getHeight()/2, null);
                */
            } else {
                g.drawString(tileName.substring(0, b), x + (lib.getTerrainImageWidth(tile.getType()) - g.getFontMetrics().stringWidth(tileName.substring(0, b)))/2, y + lib.getTerrainImageHeight(tile.getType())/2 - (g.getFontMetrics().getAscent()*2)/3);
                g.drawString(tileName.substring(b+1), x + (lib.getTerrainImageWidth(tile.getType()) - g.getFontMetrics().stringWidth(tileName.substring(b+1)))/2, y + lib.getTerrainImageHeight(tile.getType())/2 + (g.getFontMetrics().getAscent()*2)/3);
                /* Takes to much resources:
                BufferedImage stringImage = createStringImage(g, tileName.substring(0, b), Color.BLACK, lib.getTerrainImageWidth(tile.getType()), 10);
                g.drawImage(stringImage, x + (lib.getTerrainImageWidth(tile.getType()) - stringImage.getWidth())/2 + 1, y + lib.getTerrainImageHeight(tile.getType())/2 - (stringImage.getHeight()) - 5, null);
                stringImage = createStringImage(g, tileName.substring(b+1), Color.BLACK, lib.getTerrainImageWidth(tile.getType()), 10);
                g.drawImage(stringImage, x + (lib.getTerrainImageWidth(tile.getType()) - stringImage.getWidth())/2 + 1, y + lib.getTerrainImageHeight(tile.getType())/2 - 5, null);
                */
            }
        }

        /*
        if (tile.getPosition().equals(selectedTile)) {
            g.drawImage(lib.getMiscImage(ImageLibrary.UNIT_SELECT), x, y, null);
        }*/

        g.setColor(Color.BLACK);

        if (displayCoordinates) {
            String posString = tile.getX() + ", " + tile.getY();
            g.drawString(posString, x + (lib.getTerrainImageWidth(tile.getType()) - g.getFontMetrics().stringWidth(posString))/2, y + (lib.getTerrainImageHeight(tile.getType()) - g.getFontMetrics().getAscent())/2);
        }
        if (displayColonyValue && tile.getType() != Tile.UNEXPLORED && tile.isLand()) {
            String posString = Integer.toString(tile.getColonyValue());
            g.drawString(posString, x + (lib.getTerrainImageWidth(tile.getType()) - g.getFontMetrics().stringWidth(posString))/2, y + (lib.getTerrainImageHeight(tile.getType()) - g.getFontMetrics().getAscent())/2);
        }
    }


    /**
    * Stops any ongoing drag operation on the mapboard.
    */
    public void stopDrag() {
        freeColClient.getCanvas().setCursor(null);
        setDragPath(null);
        dragStarted = false;
    }


    /**
    * Starts a drag operation on the mapboard.
    */
    public void startDrag() {
        freeColClient.getCanvas().setCursor((Cursor) UIManager.get("cursor.go"));
        setDragPath(null);
        dragStarted = true;
    }


    /**
     * Checks if there is currently a drag operation on the mapboard.
     * @return <code>true</code> if a drag operation is in progress.
     */
    public boolean isDragStarted() {
        return dragStarted;
    }


    /**
    * Sets the path to be drawn on the map.
    * @param dragPath The path that should be drawn on the map
    *        or <code>null</code> if no path should be drawn.
    */
    public void setDragPath(PathNode dragPath) {
        //PathNode tempPath = this.dragPath;
        this.dragPath = dragPath;

        freeColClient.getCanvas().refresh();
    }


    /**
    * Gets the path to be drawn on the map.
    * @return The path that should be drawn on the map
    *        or <code>null</code> if no path should be drawn.
    */
    public PathNode getDragPath() {
        return dragPath;
    }


    /**
    * If set to <i>true</i> then tile names are drawn on the map.
    * @param displayTileNames <code>true</code> if the tile names
    *       should be displayed and <code>false</code> otherwise.
    */
    public void setDisplayTileNames(boolean displayTileNames) {
        this.displayTileNames = displayTileNames;
    }


    /**
    * If set to <i>true</i> then a grid is drawn on the map.
    * @param displayGrid <code>true</code> if the grid should be drawn
    *       on the map and <code>false</code> otherwise.
    */
    public void setDisplayGrid(boolean displayGrid) {
        this.displayGrid = displayGrid;
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
        int bestIndex = string.indexOf(' ');

        int index = 0;
        while (index != -1 && index != bestIndex) {
            if (Math.abs(center-index) < Math.abs(center-bestIndex)) {
                bestIndex = index;
            }

            index = string.indexOf(' ', bestIndex);
        }

        if (bestIndex == 0 || bestIndex == string.length()) {
            return -1;
        } else {
            return bestIndex;
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
            //if ((unit == getActiveUnit()) && cursor) {
            if ((unit == getActiveUnit()) && (cursor || (activeUnit.getMovesLeft() == 0))) {
                g.drawImage(lib.getMiscImage(ImageLibrary.UNIT_SELECT), x, y, null);
            }

            // Draw the unit.
            int type = lib.getUnitGraphicsType(unit);
            g.drawImage(lib.getUnitImage(type), (x + tileWidth / 2) - lib.getUnitImageWidth(type) / 2, (y + tileHeight / 2) - lib.getUnitImageHeight(type) / 2 - UNIT_OFFSET, null);

            // Draw an occupation and nation indicator.
            g.drawImage(lib.getColorChip(unit.getOwner().getColor()), x + STATE_OFFSET_X, y, null);
            String occupationString;
            if (unit.getOwner() != freeColClient.getMyPlayer()
                    && unit.isNaval()) {
                occupationString = Integer.toString(unit.getVisibleGoodsCount());
            } else {
                switch (unit.getState()) {
                case Unit.ACTIVE:
                    occupationString = "-";
                    break;
                case Unit.FORTIFY:
                    occupationString = "F";
                    break;
                case Unit.SENTRY:
                    //occupationString = "S";
                    occupationString = "-";
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
                    occupationString = "?";
                logger.warning("Unit has an invalid occpuation: " + unit.getState());
                }
                if (unit.getDestination() != null) {
                    occupationString = "G";
                }
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
        
        // FOR DEBUGGING:
        if (debugShowMission && freeColClient.getFreeColServer() != null) {
            net.sf.freecol.server.ai.AIUnit au = (net.sf.freecol.server.ai.AIUnit) freeColClient.getFreeColServer().getAIMain().getAIObject(unit);
            if (au != null) {
                g.setColor(Color.WHITE);
                String text = (unit.getOwner().isAI()) ? "" : "(";
                if (au.getMission() != null) {
                    String missionName = au.getMission().getClass().toString();
                    missionName = missionName.substring(missionName.lastIndexOf('.') + 1);
                    text += missionName;                    
                } else {
                    text += "No mission";
                }
                text += (unit.getOwner().isAI()) ? "" : ")";
                g.drawString(text, x , y);
            }
        }
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
            return y - 2 > topRow && y + 4 < bottomRow && x - 1 > leftColumn && x + 1 < rightColumn;
        } else {
            return y - 2 > topRow && y + 4 < bottomRow && x - 1 > leftColumn && x + 1 < rightColumn;
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
            if ((focus.getY() % 2) == 0) {
                leftOffset = (int) (bounds.getWidth() / 2);
            } else {
                leftOffset = (int) (bounds.getWidth() / 2) + tileWidth / 2;;
            }
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
     * Returns the ImageLibrary that this GUI uses to draw stuff.
     * @return The ImageLibrary that this GUI uses to draw stuff.
     */
    public ImageLibrary getImageLibrary() {
        return lib;
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
        if (t.getY() >= topRow 
                && t.getY() <= bottomRow 
                && t.getX() >= leftColumn 
                && t.getX() <= rightColumn) {
            int x = ((t.getX() - leftColumn) * tileWidth) + leftColumnX;
            int y = ((t.getY() - topRow) * tileHeight / 2) + topRowY;
            if ((t.getY() % 2) != 0) {     
                x += tileWidth / 2;
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
