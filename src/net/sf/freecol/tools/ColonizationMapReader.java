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


/**
 * Just pass the name of a Colonization map file (with extension ".MP").
 *
 * The map file starts with a six-byte header. Byte zero encodes the
 * map width, byte two encodes the map height. The function of the
 * other bytes is unknown, their values, however, are fixed. The
 * header is followed by three "layers", each the size of the map. The
 * first "layer" encodes the terrain type. The function of the other
 * layers is unknown. They are filled with zero bytes.
 *
 * It seems that the least significant three bits encode the basic
 * terrain type, the next two bits encode the forest overlay and
 * the special tile types ice, ocean and sea lanes. The three most
 * significant bits encode combinations of the hill, mountain and
 * river overlays.
 *
 * bits 0-2: tile type
 * bit 3 (8): forest
 * bit 4 (16): forest
 * bits 3+4 (24): special, values larger than 26 are not defined
 *
 * bits 5-7: overlays
 * 0: nothing
 * 1: hill
 * 2: minor river
 * 3: hill + minor river (extremely rare)
 * 4: nothing
 * 5: mountain
 * 6: major river
 * 7: mountain + major river (never seen)
 *
 *
 */
public class ColonizationMapReader {

    public static final int WIDTH = 0;
    public static final int HEIGHT = 2;

    private static final char[] tiletypes = {
        't', // 0x00 tundra
        'd', // 0x01 desert
        'p', // 0x02 plains
        'r', // 0x03 prairie
        'g', // 0x04 grassland
        'v', // 0x05 savannah
        'm', // 0x06 marsh
        's', // 0x07 swamp

        'B', // 0x08 boreal (tundra with forest)
        'S', // 0x09 scrub (desert with forest)
        'M', // 0x0a mixed (plains with forest)
        'L', // 0x0b broadleaf (prairie with forest)
        'C', // 0x0c conifer (grassland with forest)
        'T', // 0x0d tropical (savannah with forest)
        'W', // 0x0e wetland (marsh with forest)
        'R', // 0x0f rain (swamp with forest)

        'B', // 0x10 boreal (tundra with forest)
        'S', // 0x11 scrub (desert with forest)
        'M', // 0x12 mixed (plains with forest)
        'L', // 0x13 broadleaf (prairie with forest)
        'C', // 0x14 conifer (grassland with forest)
        'T', // 0x15 tropical (savannah with forest)
        'W', // 0x16 wetland (marsh with forest)
        'R', // 0x17 rain (swamp with forest)

        '_', // 0x18 ice
        '.', // 0x19 ocean
        ':', // 0x1a sea lane
        '?', // undefined
        '?', // undefined
        '?', // undefined
        '?', // undefined
        '?', // undefined
    };


    private static final byte[] header = {
        58, 0, 72, 0, 4, 0
    };
    private static byte[] layer1;

    public static void main(String[] args) throws Exception {

        if ("--palette".equals(args[0])) {
            RandomAccessFile writer = new RandomAccessFile(args[1], "rw");
            byte width = 58;
            byte height = 72;
            int size = width * height * 3 + header.length;
            layer1 = new byte[size];
            for (int i = 0; i < header.length; i++) {
                layer1[i] = header[i];
            }
            Arrays.fill(layer1, header.length, header.length + width * height, (byte) 25); // fill with ocean
            int ROWS = 32;
            int COLUMNS = 8;
            int offset = header.length + width + 1;
            for (int y = 0; y < ROWS; y++) {
                for (int x = 0; x < COLUMNS; x++) {
                    byte value = (byte) (COLUMNS * y + x);
                    if ((value & 24) == 24 && x > 2) {
                        // undefined
                        value = 26;
                    }
                    layer1[offset + x] = value;
                }
                offset += width;
            }
            writer.write(layer1);
        } else {
            RandomAccessFile reader = new RandomAccessFile(args[0], "r");
            reader.read(header);

            System.out.println(String.format("Map width:  %02d", (int) header[WIDTH]));
            System.out.println(String.format("Map height: %02d", (int) header[HEIGHT]));

            int size = header[WIDTH] * header[HEIGHT];
            layer1 = new byte[size];
            reader.read(layer1);

            int index = 0;
            for (int y = 0; y < header[HEIGHT]; y++) {
                for (int x = 0; x < header[WIDTH]; x++) {
                    int decimal = layer1[index] & 0xff;
                    char terrain = tiletypes[decimal & 31];
                    int overlay = decimal >> 5;
                    switch(overlay) {
                    case 1: terrain = '^'; // hill
                        break;
                    case 2: terrain = '~'; // minor river
                        break;
                    case 3: terrain = 'x'; // hill + minor river
                        break;
                    case 5: terrain = '*'; // mountain
                        break;
                    case 6: terrain = '='; // major river
                        break;
                    case 7: terrain = 'X'; // mountain + major river
                        break;
                    default:
                        break;
                    };
                    System.out.print(terrain);
                    index++;
                }
                System.out.println("\n");
            }
            System.out.println("\n");
        }
    }

}
