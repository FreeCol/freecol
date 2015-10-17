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

package net.sf.freecol.common.resources;

import java.awt.Dimension;
import java.awt.Point;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;


/**
 * Represents the data within a Font Animation File.
 * 
 * @see net.sf.freecol.client.gui.panel.DeclarationPanel
 */
public class FAFile {

    // FIXME: Use two hashes, to be safer?
    private final HashMap<Object, Object> letters = new HashMap<>();
    private int maxHeight = 0;


    /**
     * Reads data from the given <code>InputStream</code> and
     * creates an object to represent this data.
     * 
     * @param is The <code>InputStream</code>
     * @throws IOException gets thrown if the data is invalid. 
     */
    public FAFile(InputStream is) throws IOException {
        load(new CREatingInputStream(is));
    }

    
    /**
     * Gets the <code>Dimension</code> of the given
     * <code>String</code> when rendered.
     * 
     * @param text The <code>String</code>.
     * @return The <code>Dimension</code>.
     */
    public Dimension getDimension(String text) {
        FAName fn = getFAName(text);
        if (fn != null) {
            return new Dimension(fn.width, fn.height);
        }

        int width = 0;
        for (int i=0; i<text.length(); i++) {
            FALetter fl = getLetter(text.charAt(i));
            width += fl.advance;
        }

        int firstMinX = Integer.MAX_VALUE;
        FALetter letter = getLetter(text.charAt(0));
        for (int i=0; i<letter.points.length; i++) {
            Point p = letter.points[i];
            if (p.x < firstMinX) {
                firstMinX = p.x;
            }
        }  

        width += firstMinX;
        int lastMaxX = 0;
        letter = getLetter(text.charAt(text.length()-1));

        for (int i=0; i<letter.points.length; i++) {
            Point p = letter.points[i];
            if (p.x > lastMaxX) {
                lastMaxX = p.x;
            }
        }        

        width += lastMaxX;        

        return new Dimension(width, maxHeight);
    }

    /**
     * Gets the points to display the given text as an
     * animation.
     * 
     * @param text The text to get the points for.
     * @return The points in the order in which they 
     *      should be drawn.
     */
    public Point[] getPoints(String text) {
        FAName fn = getFAName(text);
        if (fn != null) {
            return fn.points;
        }
        List<Point> points = new ArrayList<>();
        int x = 0;
        for (int i=0; i<text.length(); i++) {
            FALetter fl = getLetter(text.charAt(i));
            for (int j=0; j<fl.points.length; j++) {
                Point p = fl.points[j];
                points.add(new Point(p.x + x, p.y));
            }

            x += fl.advance;
        }
        return points.toArray(new Point[0]);
    }

    private void load(InputStream is) throws IOException {
        letters.clear();

        BufferedReader in = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        String line = in.readLine();
        if (line == null || !line.startsWith("FontAnimationFile")) {
            throw new IllegalStateException("Not a FAF");
        }

        line = in.readLine();
        if (line == null) {
            throw new IllegalStateException("Max height expected");
        }
        StringTokenizer st = new StringTokenizer(line);
        maxHeight = Integer.parseInt(st.nextToken());

        line = in.readLine();
        while (line != null && !line.startsWith("[Chars]")) {
            String name = line;
            if ((line = in.readLine()) == null) break;
            st = new StringTokenizer(line);
            int width = Integer.parseInt(st.nextToken());
            int height = Integer.parseInt(st.nextToken());
            int numberOfPoints = Integer.parseInt(st.nextToken());
            int[] xs = new int[numberOfPoints];
            int[] ys = new int[numberOfPoints];

            if ((line = in.readLine()) == null) break;
            st = new StringTokenizer(line);
            for (int i=0; i<numberOfPoints; i++) {
                xs[i] = Integer.parseInt(st.nextToken());               
            }

            if ((line = in.readLine()) == null) break;
            st = new StringTokenizer(line);         
            for (int i=0; i<numberOfPoints; i++) {
                ys[i] = Integer.parseInt(st.nextToken());               
            }   

            FAName newLetter = new FAName();
            newLetter.width = width;
            newLetter.height = height;
            newLetter.points = new Point[numberOfPoints];
            for (int i=0; i<numberOfPoints; i++) {
                newLetter.points[i] = new Point(xs[i], ys[i]);                
            }                       
            letters.put(name, newLetter);
            line = in.readLine();
        }

        line = in.readLine();
        while (line != null) {
            st = new StringTokenizer(line.substring(1));
            char letter = line.charAt(0);
            int advance = Integer.parseInt(st.nextToken());
            int numberOfPoints = Integer.parseInt(st.nextToken());
            int[] xs = new int[numberOfPoints];
            int[] ys = new int[numberOfPoints];
            line = in.readLine();
            st = new StringTokenizer(line);
            for (int i=0; i<numberOfPoints; i++) {
                xs[i] = Integer.parseInt(st.nextToken());               
            }

            if ((line = in.readLine()) == null) break;
            st = new StringTokenizer(line);         
            for (int i=0; i<numberOfPoints; i++) {
                ys[i] = Integer.parseInt(st.nextToken());               
            }   

            FALetter newLetter = new FALetter();
            newLetter.advance = advance;
            newLetter.points = new Point[numberOfPoints];
            for (int i=0; i<numberOfPoints; i++) {
                newLetter.points[i] = new Point(xs[i], ys[i]);                
            }           
            letters.put(letter, newLetter);
            line = in.readLine();
        }
    }


    private FALetter getLetter(char letter) {
        return (FALetter) letters.get(letter);
    }

    private FAName getFAName(String name) {
        return (FAName) letters.get(name);
    }

    private static class FALetter {
        public Point[] points;
        public int advance;
    }

    private static class FAName {
        public Point[] points;
        public int width;
        public int height;
    }

    /**
     * This utility class removes all CR:s from an {@link InputStream}.
     * It is not particularly efficient and is intended as a temporary
     * workaround.
     */
    private static class CREatingInputStream extends InputStream {
        /**
         * Constructor.
         * 
         * @param in The input stream to wrap.
         */
        CREatingInputStream(InputStream in) {
            this.in = in;
        }
        
        /**
         * Read a character, override to eat all CR:s.
         * 
         * @return next character or -1 on end of file.
         * @throws IOException if wrapped stream throws it.
         */
        @Override
        public int read() throws IOException {
            int c;
            do {
                c = this.in.read();
            } while(c == '\r');
            return c;
        }
        
        private final InputStream in;
    }
}

