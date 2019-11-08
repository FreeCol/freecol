/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

package net.sf.freecol.util.test;

import java.util.HashMap;
import java.util.Map.Entry;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.common.option.GameOptions;
import net.sf.freecol.server.model.ServerColony;
import net.sf.freecol.server.model.ServerUnit;
import static net.sf.freecol.common.util.CollectionUtils.*;


public class FreeColTestUtils {

    private static ColonyBuilder colonyBuilder = null;

    public static ColonyBuilder getColonyBuilder(){
        Game game = FreeColTestCase.getGame();
        if(game == null){
            throw new NullPointerException("Game not set");
        }
        if(colonyBuilder == null){
            colonyBuilder = new ColonyBuilder(game);
        }
        else{
            colonyBuilder = colonyBuilder.reset().setGame(game);
        }

        return colonyBuilder;
    }

    public static class ColonyBuilder {

        // Required parameter
        static final UnitType colonistType
            = FreeColTestCase.spec().getDefaultUnitType();
        private Game game;

        private HashMap<UnitType,Integer> colonists = new HashMap<>();
        private Player player;
        private String name;
        private int initialColonists;
        private final String defaultPlayer = "model.nation.dutch";
        private String defaultName = "New Amsterdam";
        private int initialDefaultColonists = 1;
        private Tile colonyTile;


        private ColonyBuilder(Game game) {
            this.game = game;
            setStartingParams();
        }

        private void setStartingParams() {
            // Some params can only be set in build(), because the default values
            //may not be valid for the game set
            // However, the tester himself may set them to valid values later,
            //so they are set to null for now
            player = null;
            colonyTile = null;
            name = defaultName;
            initialColonists = initialDefaultColonists;
            colonists.clear();
        }

        public ColonyBuilder player(Player player) {
            if (player == null
                || none(game.getPlayers(matchKey(player)))) {
                throw new IllegalArgumentException("Player not in game");
            }
            this.player = player;
            return this;
        }

        public ColonyBuilder initialColonists(int colonists) {
            if (colonists <= 0) {
                throw new IllegalArgumentException("Number of colonists must be positive");
            }
            this.initialColonists = colonists;
            return this;
        }

        public ColonyBuilder colonyTile(Tile tile) {
            Tile tileOnMap = this.game.getMap().getTile(tile.getX(), tile.getY());
            if (tile != tileOnMap) {
                throw new IllegalArgumentException("Given tile not on map");
            }
            this.colonyTile = tile;
            return this;
        }

        public ColonyBuilder colonyName(String name) {
            if (name == null) {
                throw new IllegalArgumentException("Name cannot be null");
            }
            this.name = name;
            return this;
        }

        public ColonyBuilder addColonist(UnitType type) {
            if (!colonists.containsKey(type)) {
                colonists.put(type, 0);
            }
            Integer nCol = colonists.get(type);
            colonists.put(type, nCol + 1);
            return this;
        }

        public Colony build() {
            // player not set, get default
            if (player == null) {
                player = game.getPlayerByNationId(defaultPlayer);
                if (player == null) {
                    throw new IllegalArgumentException("Default Player "
                        + defaultPlayer + " not in game");
                }
            }

            // settlement tile no set, get default
            if (colonyTile == null) {
                colonyTile = game.getMap().getTile(5, 8);
                if (colonyTile == null) {
                    throw new IllegalArgumentException("Default tile not in game");
                }
            }

            /*
            if(this.name != null){
                for(Colony colony : player.getColonies()){
                    if(colony.getName().equals(this.name)){
                        throw new IllegalArgumentException("Another colony already has the given name");
                    }
                }
            }
            */

            Colony colony = new ServerColony(game, player, name, colonyTile);
            player.addSettlement(colony);
            colony.placeSettlement(true);//-vis(player)
            player.invalidateCanSeeTiles();//+vis(player)

            // Add colonists
            forEachMapEntry(colonists, e -> {
                    UnitType type = e.getKey();
                    Integer n = e.getValue();
                    for (int i = 0; i < n; i++) {
                        Unit colonist = new ServerUnit(game, colonyTile,
                                                       player, type);
                        colonist.setLocation(colony);
                    }
                });
            // add rest of colonists as simple free colonists
            for (int i = colony.getUnitCount(); i < initialColonists; i++) {
                Unit colonist = new ServerUnit(game, colonyTile, player,
                                               colonistType);
                colonist.setLocation(colony);
            }

            return colony;
        }

        public ColonyBuilder setGame(Game game) {
            this.game = game;
            return reset();
        }

        public ColonyBuilder reset() {
            setStartingParams();
            return this;
        }
    }


    public static boolean setStudentSelection(boolean value) {
        boolean ret = FreeColTestCase.spec()
            .getBoolean(GameOptions.ALLOW_STUDENT_SELECTION);
        FreeColTestCase.spec()
            .setBoolean(GameOptions.ALLOW_STUDENT_SELECTION, value);
        return ret;
    }
}
