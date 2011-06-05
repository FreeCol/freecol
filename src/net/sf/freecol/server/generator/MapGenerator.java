package net.sf.freecol.server.generator;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.option.OptionGroup;

/**
 * Creates maps and sets the starting locations for the players.
 */
public interface MapGenerator {

    /**
     * Creates the map with the current set options
     */
    public abstract void createMap(Game game) throws FreeColException;

    /**
     * Creates a <code>Map</code> for the given <code>Game</code>.
     *
     * The <code>Map</code> is added to the <code>Game</code> after
     * it is created.
     *
     * @param game The game.
     * @param landMap Determines whether there should be land
     *                or ocean on a given tile. This array also
     *                specifies the size of the map that is going
     *                to be created.
     * @see net.sf.freecol.common.model.Map
     */
    public abstract void createEmptyMap(Game game, boolean[][] landMap);

    /**
     * Gets the options used when generating the map.
     * @return The <code>MapGeneratorOptions</code>.
     */
    public abstract OptionGroup getMapGeneratorOptions();

}