/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

package net.sf.freecol.tools;

import java.io.RandomAccessFile;
import java.util.Arrays;


public class ColonizationSaveGameReader {

    private final static int PLAYER_DATA = 0x9e;
    private final static int COLONY_DATA = 0x186;
    private final static String[] NATIONS = {
        "English", "French", "Spanish", "Dutch"
    };

    private class GameData {

        private final int mapWidth;
        private final int mapHeight;
        private final int numberOfColonies;
        private final int difficulty;

        public GameData(byte[] data) {
            mapWidth = data[0xc];
            mapHeight = data[0xe];
            numberOfColonies = data[0x2e];
            difficulty = data[0x36];
        }

        public void print() {
            System.out.println("Map size is " + mapWidth + " x " + mapHeight);
            System.out.println("Difficulty is " + difficulty);
            System.out.println(numberOfColonies + " colonies found");
        }

        public int getNumberOfColonies() {
            return numberOfColonies;
        }
    }

    private class PlayerData {

        public static final int LENGTH = 52;

        private final String newLandName;
        private final String playerName;
        private final boolean humanPlayer;

        public PlayerData(byte[] data, int offset) {
            playerName = getString(data, offset, 23);
            newLandName = getString(data, offset + 24, 23);
            humanPlayer = (data[offset + 49] == 0);
        }

        public void print() {
            System.out.println("Player name is " + playerName
                               + (humanPlayer ? " [human]" : " [AI]"));
            System.out.println("New land name is " + newLandName);
        }

    }

    private class ColonyData {

        public static final int LENGTH = 202;
        public static final int COLONIST_OCCUPATION = 0x20;
        public static final int COLONIST_SPECIALITY = 0x40;
        public static final int TILES = 0x70;

        private final int x;
        private final int y;
        private final int numberOfColonists;
        private final String name;
        private final Colonist[] colonists;

        public ColonyData(byte[] data, int offset) {
            x = data[offset];
            y = data[offset + 1];
            name = getString(data, offset + 2, offset + 25);
            numberOfColonists = data[offset + 0x1f];
            colonists = new Colonist[numberOfColonists];
            for (int index = 0; index < numberOfColonists; index++) {
                int tile = -1;
                for (int tileIndex = 0; tileIndex < 8; tileIndex++) {
                    if (data[offset + TILES + tileIndex] == index) {
                        tile = tileIndex;
                        break;
                    }
                }
                colonists[index] = new Colonist(data[offset + COLONIST_OCCUPATION + index],
                                                data[offset + COLONIST_SPECIALITY + index],
                                                tile);
            }


        }

        public void print() {
            System.out.println(name + " [" + x + ", " + y + "], "
                               + numberOfColonists + " colonists");
            for (Colonist colonist : colonists) {
                colonist.print();
            }
        }
    }

    public class Colonist {

        public final String[] OCCUPATION = {
            "Farmer",
            "Sugar planter",
            "Tobacco planter",
            "Cotton planter",
            "Fur trapper",
            "Lumberjack",
            "Ore miner",
            "Silver miner",
            "Fisherman",
            "Distiller",
            "Tobacconist",
            "Weaver",
            "Fur Trader",
            "Carpenter",
            "Blacksmith",
            "Gunsmith",
            "Preacher",
            "Statesman",
            "Teacher",
            "",
            "Pioneer",
            "Veteran Soldier",
            "Scout",
            "Veteran Dragoon",
            "Missionary",
            "Indentured Servant",
            "Petty Criminal",
            "Indian convert",
            "Free colonist",
            "Armed brave",
            "Mounted brave"
        };

        public final String[] TILES = {
            "N", "E", "S", "W", "NW", "NE", "SE", "SW"
        };

        final int occupation;
        final int speciality;
        final int tile;

        public Colonist(int occupation, int speciality, int tile) {
            this.occupation = occupation;
            this.speciality = speciality;
            this.tile = tile;
        }

        public void print() {
            String tileString = (tile >= 0)
                ? " [tile " + TILES[tile] + "]" : "";
            System.out.println(OCCUPATION[speciality] + " working as "
                               + OCCUPATION[occupation] + tileString);
        }

    }


    private final byte[] data;

    public ColonizationSaveGameReader(byte[] data) {
        this.data = data;
    }

    public static void main(String[] args) throws Exception {

        byte[] data;
        try (RandomAccessFile reader = new RandomAccessFile(args[0], "r")) {
            data = new byte[(int) reader.length()];
            reader.read(data);
        }
        new ColonizationSaveGameReader(data).run();
    }


    private void run() {

        GameData gameData = new GameData(data);
        gameData.print();
        for (int index = 0; index < 4; index++) {
            System.out.println("Nation is " + NATIONS[index]);
            PlayerData playerData = new PlayerData(data, PLAYER_DATA +
                                                   index * PlayerData.LENGTH);
            playerData.print();
        }
        int count = gameData.getNumberOfColonies();
        for (int index = 0; index < count; index++) {
            ColonyData colonyData = new ColonyData(data, COLONY_DATA +
                                                   index * ColonyData.LENGTH);
            colonyData.print();
        }


    }

    public static String getString(byte[] data, int start, int length) {
        byte[] bytes = Arrays.copyOfRange(data, start, start + length);
        String value = new String(bytes);
        int index = value.indexOf(0);
        if (index < 0) {
            return value;
        } else {
            return value.substring(0, index);
        }
    }
}