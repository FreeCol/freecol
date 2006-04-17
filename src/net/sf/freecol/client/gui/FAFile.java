package net.sf.freecol.client.gui;

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
 * @see net.sf.freecol.client.gui.panel.DeclarationDialog
 */
public class FAFile {

    private HashMap letters = new HashMap();
    private int maxHeight = 0;

    /**
     * Reads data from the given <code>InputStream</code> and
     * creates an object to represent this data.
     * 
     * @param is The <code>InputStream</code>
     * @throws IOException gets thrown if the data is invalid. 
     */
    public FAFile(InputStream is) throws IOException {
        load(is);
    }
    
    
    /**
     * Gets the <code>Dimension</code> of the given
     * <code>String</code> when rendered.
     * 
     * @param text The <code>String</code>.
     * @return The <code>Dimension</code>.
     */
    public Dimension getDimension(String text) {
        int width = 0;
        
        for (int i=0; i<text.length(); i++) {
            FALetter fl = getLetter(text.charAt(i));
            width += fl.advance;
        }
        
        int firstMinX = Integer.MAX_VALUE;
        FALetter letter = getLetter(text.charAt(0));
        for (int i=0; i<letter.points.length; i++) {
            Point p = (Point) letter.points[i];
            if (p.x < firstMinX) {
                firstMinX = p.x;
            }
        }  
        width += firstMinX;

        int lastMaxX = 0;
        letter = getLetter(text.charAt(text.length()-1));
        for (int i=0; i<letter.points.length; i++) {
            Point p = (Point) letter.points[i];
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
        List points = new ArrayList();

        int x = 0;
        for (int i=0; i<text.length(); i++) {
            FALetter fl = getLetter(text.charAt(i));
            for (int j=0; j<fl.points.length; j++) {
                Point p = fl.points[j];
                points.add(new Point(p.x + x, p.y));
            }
            x += fl.advance;
        }
        
        return (Point[]) points.toArray(new Point[0]);
    }

    private void load(InputStream is) throws IOException {
        letters.clear();
        
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        if (!in.readLine().startsWith("FontAnimationFile")) {
            throw new IllegalStateException("Not a FAF");
        }
        
        StringTokenizer st = new StringTokenizer(in.readLine());
        maxHeight = Integer.parseInt(st.nextToken());
        
        String line = in.readLine();
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

            line = in.readLine();
            st = new StringTokenizer(line);         
            for (int i=0; i<numberOfPoints; i++) {
                ys[i] = Integer.parseInt(st.nextToken());               
            }   
            
            FALetter newLetter = new FALetter();
            newLetter.letter = letter;
            newLetter.advance = advance;
            newLetter.points = new Point[numberOfPoints];
            
            for (int i=0; i<numberOfPoints; i++) {
                newLetter.points[i] = new Point(xs[i], ys[i]);                
            }           
            
            letters.put(new Character(letter), newLetter);
            
            line = in.readLine();
        }
    }

    private FALetter getLetter(char letter) {
        return (FALetter) letters.get(new Character(letter));
    }

    private class FALetter {
        public char letter;
        public Point[] points;
        public int advance;
    }
}
