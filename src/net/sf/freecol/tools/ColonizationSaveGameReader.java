/**
 *  Copyright (C) 2002-2021   The FreeCol Team
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

import java.io.EOFException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;


public class ColonizationSaveGameReader {

    private final static int PLAYER_DATA = 0x9e;
    private final static int COLONY_DATA = 0x186;
    private final static String[] NATIONS = {
        "English", "French", "Spanish", "Dutch"
    };

    private static class GameData {

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

    private static class PlayerData {

        public static final int LENGTH = 52;

        public static void print(byte[] data, int offset) {
            String playerName = getString(data, offset, 23);
            String newLandName = getString(data, offset + 24, 23);
            boolean isHuman = data[offset + 49] == 0;

            System.out.println("Player name is " + playerName
                               + (isHuman ? " [human]" : " [AI]"));
            System.out.println("New land name is " + newLandName);
        }

    }

    private static class ColonyData {

        public static final int LENGTH = 202;
        public static final int COLONIST_OCCUPATION = 0x20;
        public static final int COLONIST_SPECIALITY = 0x40;
        public static final int TILES = 0x70;

        public static void print(byte[] data, int offset) {
            int x = data[offset];
            int y = data[offset + 1];
            String name = getString(data, offset + 2, offset + 25);
            int numberOfColonists = data[offset + 0x1f];
            System.out.println(name + " [" + x + ", " + y + "], "
                    + numberOfColonists + " colonists");

            for (int index = 0; index < numberOfColonists; index++) {
                int tile = -1;
                for (int tileIndex = 0; tileIndex < 8; tileIndex++) {
                    if (data[offset + TILES + tileIndex] == index) {
                        tile = tileIndex;
                        break;
                    }
                }
                int occupation = data[offset + COLONIST_OCCUPATION];
                int speciality = data[offset + COLONIST_SPECIALITY];
                Colonist.print(occupation, speciality, tile);
            }
        }
    }

    public static class Colonist {

        public static final String[] OCCUPATION = {
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

        public static final String[] TILES = {
            "N", "E", "S", "W", "NW", "NE", "SE", "SW"
        };

        public static void print(int occupation, int speciality, int tile) {
            String tileString = (tile >= 0)
                ? " [tile " + TILES[tile] + "]" : "";
            System.out.println(OCCUPATION[speciality] + " working as "
                               + OCCUPATION[occupation] + tileString);
        }

    }

    public static void main(String[] args) throws Exception {

        byte[] data;
        try (RandomAccessFile reader = new RandomAccessFile(args[0], "r")) {
            data = new byte[(int) reader.length()];
            reader.readFully(data);
            run(data);
        } catch (EOFException ee) {
            System.err.println("Could not read from " + args[0] + ": " + ee);
            System.exit(1);
        }
    }


    private static void run(byte[] data) {

        GameData gameData = new GameData(data);
        gameData.print();
        for (int index = 0; index < 4; index++) {
            System.out.println("Nation is " + NATIONS[index]);
            PlayerData.print(data, PLAYER_DATA + index * PlayerData.LENGTH);
        }
        int count = gameData.getNumberOfColonies();
        for (int index = 0; index < count; index++) {
            ColonyData.print(data, COLONY_DATA + index * ColonyData.LENGTH);
        }


    }

    public static String getString(byte[] data, int start, int length) {
        byte[] bytes = Arrays.copyOfRange(data, start, start + length);
        String value = new String(bytes, StandardCharsets.UTF_8);
        int index = value.indexOf(0);
        return (index < 0) ? value : value.substring(0, index);
    }
}
