
package net.sf.freecol.common.model;


import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.ArrayList;
import java.util.logging.Logger;

import net.sf.freecol.common.FreeColException;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;



/**
* An isometric map. The map is represented as a collection of tiles.
*/
public class Map extends FreeColGameObject {
    public static final String  COPYRIGHT = "Copyright (C) 2003 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static final Logger logger = Logger.getLogger(Map.class.getName());


    // The possible sizes for a Map.
    public static final int SMALL = 0,
                            MEDIUM = 1,
                            LARGE = 2,
                            HUGE = 3,
                            CUSTOM = 4;

    // The directions a Unit can move to.
    public static final int N = 0,
                            NE = 1,
                            E = 2,
                            SE = 3,
                            S = 4,
                            SW = 5,
                            W = 6,
                            NW = 7;

    // Deltas for moving to adjacent squares. Different due to the
    // isometric map. Starting north and going clockwise.
    private static final int[] ODD_DX = {0, 1, 1, 1, 0, 0, -1, 0};
    private static final int[] ODD_DY = {-2, -1, 0, 1, 2, 1, 0, -1};
    private static final int[] EVEN_DX = {0, 0, 1, 0, 0, -1, -1, -1};
    private static final int[] EVEN_DY = {-2, -1, 0, 1, 2, 1, 0, -1};


    /**
    * This Vector contains a set of other Vectors that can be considered columns.
    * Those columns contain a set of Tiles.
    */
    private Vector columns = null;




    /**
    * Create a new <code>Map</code> of a specified size.
    *
    * @param game The <code>Game</code> this map belongs to.
    * @param size The size of the map to construct, should be one of {SMALL, MEDIUM, LARGE, HUGE}.
    */
    public Map(Game game, int size) throws FreeColException {
        super(game);

        createColumns(size);
    }


    /**
    * Create a new <code>Map</code> from a collection of tiles.
    *
    * @param game The <code>Game</code> this map belongs to.
    * @param columns This <code>Vector</code> contains the rows,
    *                that contains the tiles.
    */

    public Map(Game game, Vector columns) {
        super(game);

        this.columns = columns;
    }


    /**
    * Create a new <code>Map</code> from an <code>Element</code>
    * in a DOM-parsed XML-tree
    *
    * @param game The <code>Game</code> this map belongs to.
    * @param element The <code>Element</code> in a DOM-parsed XML-tree that describes
    *                this object.
    */
    public Map(Game game, Element element) {
        super(game, element);

        readFromXMLElement(element);
    }


    public boolean isLandWithinDistance(int x, int y, int distance) {
        Iterator i = getCircleIterator(new Position(x, y), true, distance);
        while(i.hasNext()) {
            if (getTile((Position) i.next()).isLand()) {
                return true;
            }
        }
        
        return false;
    }


    /**
    * Creates the columns contains the rows that contains the tiles.
    */
    private void createColumns(int size) throws FreeColException {
        int width, height;

        switch (size) {
            case SMALL:
                width = 30;
                height = 64;
                break;
            case MEDIUM:
                width = 60;
                height = 128;
                break;
            case LARGE:
                width = 120;
                height = 256;
                break;
            case HUGE:
                width = 240;
                height = 512;
                break;
            default:
                throw new FreeColException("Invalid map-size: " + size + ".");
        }

        createColumns(width, height);
    }


    /**
    * Creates the columns contains the rows that contains the tiles.
    */
    private void createColumns(int width, int height) {
        columns = new Vector(width);
        for (int i = 0; i < width; i++) {
            Vector v = new Vector(height);
            for (int j = 0; j < height; j++) {
                Tile t = new Tile(getGame(), i, j);
                v.add(t);
            }
            columns.add(v);
        }
    }


    /**
     * Returns the Tile at a requested position.
     *
     * @param p The position.
     * @return The Tile at the given position.
     */
    public Tile getTile(Position p) {
        return getTile(p.getX(), p.getY());
    }


    /**
     * Returns the Tile at position (x, y). 'x' specifies a column and 'y' specifies a row.
     * (0, 0) is the Tile at the top-left corner of the Map.
     *
     * @return The Tile at position (x, y).
     */
    public Tile getTile(int x, int y) {
        if ((x >= 0) && (x < getWidth()) && (y >= 0) && (y < getHeight())) {
            return (Tile) ((Vector) columns.get(x)).get(y);
        } else {
            throw new IllegalArgumentException("Illegal coordinate (" + x + ", " + y + ")");
        }
    }
    
    
    /**
    * Sets the given tile the the given coordinates.
    *
    * @param tile The <code>Tile</code>.
    */
    public void setTile(Tile tile, int x, int y) {
        ((Vector) columns.get(x)).set(y, tile);
    }


    /**
     * Returns the width of this Map.
     * @return The width of this Map.
     */
    public int getWidth() {
        return columns.size();
    }


    /**
     * Returns the height of this Map.
     * @return The height of this Map.
     */
    public int getHeight() {
        return ((Vector) columns.get(0)).size();
    }


    /**
     * Returns the neighbouring Tile of the given Tile in the given direction.
     *
     * @param direction The direction in which the neighbour is located given t.
     * @param t The Tile to get a neighbour of.
     * @return The neighbouring Tile of the given Tile in the given direction.
     */
    public Tile getNeighbourOrNull(int direction, Tile t) {
        return getNeighbourOrNull(direction, t.getX(), t.getY());
    }


    /**
     * Returns the neighbouring Tile of the given Tile in the given direction.
     *
     * @param direction The direction in which the neighbour is located given
     * the base tile.
     * @param x The base tile X coordinate.
     * @param y The base tile Y coordinate.
     * @return The neighbouring Tile of the given coordinate in the given
     *         direction or null if invalid.
     */
    public Tile getNeighbourOrNull(int direction, int x, int y) {
        if (isValid(x, y)) {
            Position pos = getAdjacent(new Position(x, y), direction);
            return getTileOrNull(pos.getX(), pos.getY());
        } else {
            return null;
        }
    }


    /**
     * Get a tile or null if it does not exist.
     *
     * @param x The x position.
     * @param y The y position.
     * @return tile if position is valid, null otherwise.
     */
    public Tile getTileOrNull(int x, int y) {
        return isValid(x, y) ? getTile(x, y) : null;
    }


    /**
     * Get a tile or null if it does not exist.
     *
     * @param position The position of the tile.
     * @return tile if position is valid, null otherwise.
     */
    public Tile getTileOrNull(Position position) {
        return isValid(position) ? getTile(position) : null;
    }


    /**
     * Returns all the tiles surrounding the given tile within
     * the given range.
     *
     * @param t The tile that lies on the center of the tiles to return.
     * @param range How far away do we need to go starting from the
     * center tile.
     * @return The tiles surrounding the given tile.
     */
    public Vector getSurroundingTiles(Tile t, int range) {
        Vector result = new Vector();
        Position tilePosition = new Position(t.getX(), t.getY());
        Iterator i = (range == 1) ?
            getAdjacentIterator(tilePosition) :
            getCircleIterator(tilePosition, true, range);

        while (i.hasNext()) {
            Position p = (Position) i.next();
            if (!p.equals(tilePosition)) {
                result.add(getTile(p));
            }
        }

        return result;
    }


    /**
     * Gets an <code>Iterator</code> of every <code>Tile</code> on the map.
     * @return the code>Iterator</code>
     */
    public WholeMapIterator getWholeMapIterator() {
        return new WholeMapIterator();
    }


    /**
     * Get the position adjacent to a given position, in a given
     * direction.
     *
     * @param position The position
     * @param direction The direction (N, NE, E, etc.)
     * @return Adjacent position
     */
    public Position getAdjacent(Position position, int direction) {
        int x = position.getX() + ((position.getY() & 1) != 0 ?
            ODD_DX[direction] : EVEN_DX[direction]);
        int y = position.getY() + ((position.getY() & 1) != 0 ?
            ODD_DY[direction] : EVEN_DY[direction]);
        return new Position(x, y);
    }


    /**
     * Get an adjacent iterator.
     *
     * @param centerPosition The center position to iterate around
     * @return Iterator
     */
    public Iterator getAdjacentIterator(Position centerPosition) {
        return new AdjacentIterator(centerPosition);
    }


    /**
     * Get a border adjacent iterator.
     *
     * @param centerPosition The center position to iterate around
     * @return Iterator
     */
    public Iterator getBorderAdjacentIterator
        (Position centerPosition) {
        return new BorderAdjacentIterator(centerPosition);
    }


    /**
     * Get a flood fill iterator.
     *
     * @param centerPosition The center position to iterate around
     * @return Iterator
     */
    public Iterator getFloodFillIterator(Position centerPosition) {
        return new FloodFillIterator(centerPosition);
    }


    /**
     * Get a circle iterator.
     *
     * @param center The center position to iterate around
     * @param isFilled True to get all of the positions in the circle
     * @param radius Radius of circle
     * @return Iterator
     */
    public Iterator getCircleIterator(Position center, boolean isFilled,
                                      int radius) {
        return new CircleIterator(center, isFilled, radius);
    }


    /**
     * Checks whether a position is valid (within the map limits).
     * @param position The position
     * @return True if it is valid
     */
    public boolean isValid(Position position) {
        return isValid(position.getX(), position.getY());
    }


    /**
     * Checks whether a position is valid (within the map limits).
     * @param x X coordinate
     * @param y Y coordinate
     * @return True if it is valid
     */
    public boolean isValid(int x, int y) {
        return ((x >= 0) && (x < getWidth()) && (y >= 0) && (y < getHeight()));
    }

    
    /**
     * Gets the distance in tiles between two map positions.
     * With an isometric map this is a non-trivial task.  The formula
     * below has been developed largely through trial and error.  It
     * should cover all cases, but I wouldn't bet my life on it.
     *
     * @param position1 The first position.
     * @param position2 The second position.
     * @return Distance
     */
    public int getDistance(Position position1, Position position2) {
        return getDistance(position1.getX(), position1.getY(), position2.getX(), position2.getY());
    }


    /**
     * Gets the distance in tiles between two map positions.
     *
     * @param ax Position A x-coordinate
     * @param ay Position A y-coordinate
     * @param bx Position B x-coordinate
     * @param by Position B y-coordinate
     * @return Distance
     */
    public int getDistance(int ax, int ay, int bx, int by) {
        int r = bx - ax - (ay - by)/2;

        if (by > ay && ay%2 == 0 && by%2 != 0) {
            r++;
        } else if (by < ay && ay%2 != 0 && by%2 == 0) {
            r--;
        }

        return Math.max(Math.abs(ay-by+r), Math.abs(r));
    }





    /**
     * Represents a position on the Map.
     */
    public static final class Position {
        private final int x, y;

        

        /**
         * Creates a new object with the given position.
         * @param posX The x-coordinate for this position.
         * @param posY The y-coordinate for this position.
         */
        public Position(int posX, int posY) {
            x = posX;
            y = posY;
        }

        

        /**
         * Returns the x-coordinate of this Position.
         * @return The x-coordinate of this Position.
         */
        public int getX() {
            return x;
        }


        /**
         * Returns the y-coordinate of this Position.
         * @return The y-coordinate of this Position.
         */
        public int getY() {
            return y;
        }


        /**
         * Indicates whether some other object is "equal to" this one.
         *
         * @param   obj the reference object with which to compare.
         * @return  true iff the coordinates match.
         */
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            return x == ((Position) obj).x && y == ((Position) obj).y;
        }


        /**
         * Returns a hash code value. The current implementation (which
         * may change at any time) works well as long as the maximum
         * coordinates fit in 16 bits.
         *
         * @return  a hash code value for this object.
         */
        public int hashCode() {
            return x | (y << 16);
        }


        /**
         * Returns a string representation of the object.
         * @return  a string representation of the object.
         */
        public String toString() {
            return "(" + x + ", " + y + ")";
        }
    }





    /**
     * Base class for internal iterators.
     */
    private abstract class MapIterator implements Iterator {


        /**
         * Get the next position as a position rather as an object.
         * @return position.
         * @throws NoSuchElementException if iterator is exhausted.
         */
        public abstract Position nextPosition() throws NoSuchElementException;


        /**
         * Returns the next element in the iteration.
         *
         * @return the next element in the iteration.
         * @exception NoSuchElementException iteration has no more elements.
         */
        public Object next() {
            return nextPosition();
        }


        /**
         * Removes from the underlying collection the last element returned by the
         * iterator (optional operation).
         *
         * @exception UnsupportedOperationException no matter what.
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }





    private final class WholeMapIterator extends MapIterator {
        private int x;
        private int y;


        /**
         * Default constructor.
         */
        public WholeMapIterator() {
            x = 0;
            y = 0;
        }



        /**
         * Determine if the iterator has another position in it.
         * @return True of there is another position
         */
        public boolean hasNext() {
            return y < getHeight();
        }


        /**
         * Obtain the next position to iterate over.
         * @return Next position
         * @throws java.util.NoSuchElementException if last position already returned
         */
        public Position nextPosition() throws NoSuchElementException {
            if (y < getHeight()) {
                Position newPosition = new Position(x, y);
                x++;
                if (x == getWidth()) {
                    x = 0;
                    y++;
                }
                return newPosition;
            }
            throw new NoSuchElementException("Iterator exhausted");
        }
    }


    
    

    private final class AdjacentIterator extends MapIterator {
        // The starting tile position
        private Position basePosition;
        // Index into the list of adjacent tiles
        private int x = 0;

        /**
         * The constructor to use.
         * @param basePosition The position around which to iterate
         */
        public AdjacentIterator(Position basePosition) {
            this.basePosition = basePosition;
        }

        /**
         * Determine if the iterator has another position in it.
         * @return True of there is another position
         */
        public boolean hasNext() {
            for (int i = x; i < 8; i++) {
                Position newPosition = getAdjacent(basePosition, i);
                if (isValid(newPosition))
                    return true;
            }
            return false;
        }

        /**
         * Obtain the next position to iterate over.
         * @return Next position
         * @throws NoSuchElementException if last position already returned
         */
        public Position nextPosition() throws NoSuchElementException {
            for (int i = x; i < 8; i++) {
                Position newPosition = getAdjacent(basePosition, i);
                if (isValid(newPosition)) {
                    x = i + 1;
                    return newPosition;
                }
            }
            throw new NoSuchElementException("Iterator exhausted");
        }
    }


    
    

    private final class FloodFillIterator extends MapIterator {
        // The center of the circles to use
        private Position center;
        // An inner iterator for an individual circle around the center
        private Iterator iterator;
        // The radius of the current circle
        private int currentRadius;
        // True if the last circle has been exhausted
        private boolean exhausted;

        /**
         * The constructor to use.
         * @param center The position at the center of the area
         */
        public FloodFillIterator(Position center) {
            this.center = center;
            exhausted = false;
            currentRadius = 1;
            iterator = getCircleIterator(center, false, currentRadius);
        }

        /**
         * Determine if the iterator has another position in it.
         * @return True of there is another position
         */
        public boolean hasNext() {
            if (!exhausted) {
                if (!iterator.hasNext()) {
                    iterator = getCircleIterator(center, false,
                                                 ++currentRadius);
                    exhausted = !iterator.hasNext();
                }
            }
            return !exhausted;
        }

        /**
         * Obtain the next position to iterate over.
         * @return Next position
         * @throws NoSuchElementException if last position already returned
         */
        public Position nextPosition() {
            return (Position) iterator.next();
        }
    }

    
    


    private final class CircleIterator extends MapIterator {
        // The center of the circle
        //int centerX;
        //int centerY;
        // The minimum and maximum limits of the area to iterate over
        //int minX;
        //int maxX;
        //int maxY;
        // The last position returned by the iterator
        //int currentX;
        //int currentY;
        // The desired radius of the circle
        //int radius;
        // If true, all of the positions inside the circle are to be iterated
        // over; if false, only the edge of the circle
        //boolean isFilled;
        private Iterator positionIterator;
        private ArrayList positions = new ArrayList();
        private int radius;
        private boolean isFilled;

        /**
         * The constructor to use.
         * @param center The center of the circle
         * @param isFilled True to get all of the positions within the circle
         * @param radius The radius of the circle
         */
        public CircleIterator(Position center, boolean isFilled, int radius) {
            this.radius = radius;
            this.isFilled = isFilled;

            normal(center, N, 0);
            edge(center, NE, 0);
            normal(center, E, 0);
            edge(center, SE, 0);
            normal(center, S, 0);
            edge(center, SW, 0);
            normal(center, W, 0);
            edge(center, NW, 0);

            positionIterator = positions.iterator();
        }

        private void edge(Map.Position p, int type, int radiusCompleted) {
            for (int r=radiusCompleted; r<radius; r++) {
                normal(p, type - 1, r);
                normal(p, type+1==8?0:type+1, r);
                p = getAdjacent(p, type);
                
                if (isFilled && isValid(p)) {
                    positions.add(p);
                }
            }

            if (!isFilled && isValid(p)) {
                positions.add(p);
            }
        }

        private void normal(Map.Position p, int type, int radiusCompleted) {
            for (int r=radiusCompleted; r<radius; r++) {
                p = getAdjacent(p, type);

                if (isFilled && isValid(p)) {
                    positions.add(p);
                }
            }

            if (!isFilled && isValid(p)) {
                positions.add(p);
            }
        }


        /**
         * Determine if the iterator has another position in it.
         * @return True of there is another position
         */
        public boolean hasNext() {
            return positionIterator.hasNext();
        }

        
        /**
         * Obtain the next position to iterate over.
         * @return Next position
         */
        public Position nextPosition() {
            return (Position) positionIterator.next();
        }
    }

    
    


    private final class BorderAdjacentIterator extends MapIterator {
        // The starting tile position
        private Position basePosition;
        // Index into the list of adjacent tiles
        private int index;

        /**
         * The constructor to use.
         * @param basePosition The position around which to iterate
         */
        public BorderAdjacentIterator(Position basePosition) {
            this.basePosition = basePosition;
            index = 1;
        }

        /**
         * Determine if the iterator has another position in it.
         * @return True of there is another position
         */
        public boolean hasNext() {
            for (int i = index; i < 8; i += 2) {
                Position newPosition = getAdjacent(basePosition, i);
                if (isValid(newPosition))
                    return true;
            }
            return false;
        }

        /**
         * Obtain the next position to iterate over.
         * @return Next position
         * @throws NoSuchElementException if last position already returned
         */
        public Position nextPosition() throws NoSuchElementException {
            for (int i = index; i < 8; i += 2) {
                Position newPosition = getAdjacent(basePosition, i);
                if (isValid(newPosition)) {
                    index = i + 2;
                    return newPosition;
                }
            }
            throw new NoSuchElementException("Iterator exhausted");
        }
    }

    

    public void newTurn() {

    }
        
    
    /**
    * Make a XML-representation of this object.
    *
    * @param document The document to use when creating new componenets.
    * @return The DOM-element ("Document Object Model") made to represent this "Map".
    */
    public Element toXMLElement(Player player, Document document) {
        Element mapElement = document.createElement(getXMLElementTagName());

        mapElement.setAttribute("ID", getID());
        mapElement.setAttribute("width", Integer.toString(getWidth()));
        mapElement.setAttribute("height", Integer.toString(getHeight()));

        Iterator tileIterator = getWholeMapIterator();
        while (tileIterator.hasNext()) {
            Tile tile = getTile((Position) tileIterator.next());
            
            if (player.hasExplored(tile)) {
                mapElement.appendChild(tile.toXMLElement(player, document));
            } else {
                Tile hiddenTile = new Tile(getGame(), tile.getX(), tile.getY());
                hiddenTile.setID(tile.getID());
                mapElement.appendChild(hiddenTile.toXMLElement(player, document));
            }
        }

        return mapElement;
    }


    /**
    * Initialize this object from an XML-representation of this object.
    *
    * @param mapElement The DOM-element ("Document Object Model") made to represent this "Map".
    */
    public void readFromXMLElement(Element mapElement) {
        setID(mapElement.getAttribute("ID"));

        if (columns == null) {
            int width = Integer.parseInt(mapElement.getAttribute("width"));
            int height = Integer.parseInt(mapElement.getAttribute("height"));

            //createColumns(width, height);
            columns = new Vector(width);
            for (int i = 0; i < width; i++) {
                Vector v = new Vector(height);
                for (int j = 0; j < height; j++) {
                    v.add(null);
                }
                columns.add(v);
            }
        }

        NodeList tileList = mapElement.getElementsByTagName(Tile.getXMLElementTagName());

        for (int i=0; i<tileList.getLength(); i++) {
            Element tileElement = (Element) tileList.item(i);
            int x = Integer.parseInt(tileElement.getAttribute("x"));
            int y = Integer.parseInt(tileElement.getAttribute("y"));

            if (getTile(x, y) != null) {
                getTile(x, y).readFromXMLElement(tileElement);
            } else {
                setTile(new Tile(getGame(), tileElement), x, y);
            }
        }
    }


    /**
    * Returns the tag name of the root element representing this object.
    *
    * @return the tag name.
    */
    public static String getXMLElementTagName() {
        return "map";
    }
}
