/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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

import java.awt.Color;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JScrollPane;
import javax.swing.SpinnerNumberModel;

import net.miginfocom.swing.MigLayout;


public class FlagTest extends JFrame implements ItemListener {

    public class ColorScheme {
        Color backgroundColor = Color.RED;
        Color unionColor = Color.BLUE;
        Color stripeColor = Color.WHITE;
        Color starColor = Color.WHITE;

        public ColorScheme(Color backgroundColor, Color unionColor,
                           Color stripeColor, Color starColor) {
            this.backgroundColor = backgroundColor;
            this.unionColor = unionColor;
            this.stripeColor = stripeColor;
            this.starColor = starColor;
        }
    }

    // based on the flag of Venezuela (Colombia and Ecuador are
    // similar)
    private ColorScheme SPAIN
        = new ColorScheme(new Color(0xcf, 0x14, 0x2b),
                          new Color(0, 0x24, 0x7d),
                          new Color(255, 204, 0),
                          Color.WHITE);

    // based on the flag of Brazil, particularly the Provisional Flag
    // of Republic of the United States of Brazil (November 15–19,
    // 1889)
    private ColorScheme PORTUGAL
        = new ColorScheme(new Color(0, 168, 89),
                          new Color(62, 64, 149),
                          new Color(255, 204, 41),
                          Color.WHITE);

    // based on the current flag of the United States and its various
    // predecessors
    private ColorScheme ENGLAND
        = new ColorScheme(new Color(.698f, .132f, .203f),
                          new Color(.234f, .233f, .430f),
                          Color.WHITE, Color.WHITE);

    // based on the flag of Louisiana in 1861 and other similar French
    // colonial flags
    private ColorScheme FRANCE
        = new ColorScheme(new Color(0xed, 0x29, 0x39),
                          Color.WHITE,
                          new Color(0, 0x23, 0x95),
                          Color.WHITE);

    // Dutch similar to French
    private ColorScheme DUTCH
        = new ColorScheme(new Color(0x21, 0x46, 0x6b),
                          Color.WHITE,
                          new Color(0xae, 0x1c, 0x28),
                          Color.WHITE);

    public enum FlagType {
        HORIZONTAL_STRIPES,
            VERTICAL_STRIPES,
            HORIZONTAL_TRICOLORE,
            VERTICAL_TRICOLORE,
            QUARTERED;
    }

    private static final int[][] layout = new int[51][2];

    static {
        for (int[] bars : new int[][] {
                { 5, 4 }, { 5, 6 }, { 6, 5 }, { 5, 5 }
            }) {
            int sum = bars[0] + bars[1];
            boolean even = true;
            while (sum < 51) {
                layout[sum] = bars;
                if (even) {
                    sum += bars[0];
                    even = false;
                } else {
                    sum += bars[1];
                    even = true;
                }
            }
        }

    }

    public class Flag {

        private static final double height = 100;
        private double width = 1.9 * height;
        private double unionWidth = 0.4 * width;
        private double star = 0.0616 * height;
        private double unionHeight;


        private BufferedImage image;

        public Flag(ColorScheme colorScheme, FlagType flagType, int stars, int stripes) {

            image = new BufferedImage((int) width, (int) height,
                BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

            g.setColor(colorScheme.backgroundColor);
            Rectangle2D.Double background =
                new Rectangle2D.Double(0, 0, width, height);
            g.fill(background);

            g.setColor(colorScheme.stripeColor);
            Rectangle2D.Double stripe;
            AffineTransform transform = g.getTransform();
            switch(flagType) {
            case QUARTERED:
                double stripeSize = height / 7;
                stripe = new Rectangle2D.Double(0, 0, width, stripeSize);
                unionHeight = (height - stripeSize) / 2;
                g.translate(0, unionHeight);
                g.fill(stripe);
                stripe = new Rectangle2D.Double(0, 0, stripeSize, height);
                g.setTransform(transform);
                unionWidth = (width - stripeSize) / 2;
                g.translate(unionWidth, 0);
                g.fill(stripe);
                g.setTransform(transform);
                break;
            case HORIZONTAL_STRIPES:
                double stripeHeight = height / stripes;
                stripe = new Rectangle2D.Double(0, 0, width, stripeHeight);
                for (int i = 0; i < stripes / 2; i++) {
                    g.translate(0, stripeHeight);
                    g.fill(stripe);
                    g.translate(0, stripeHeight);
                }
                if (stripes == 1) {
                    unionHeight = stripeHeight / 2;
                } else {
                    unionHeight = stripeHeight * Math.ceil(stripes / 2.0);
                }
                g.setTransform(transform);
                break;
            case VERTICAL_STRIPES:
                double stripeWidth = width / stripes;
                stripe = new Rectangle2D.Double(0, 0, stripeWidth, height);
                for (int i = 0; i < stripes / 2; i++) {
                    g.translate(stripeWidth, 0);
                    g.fill(stripe);
                    g.translate(stripeWidth, 0);
                }
                if (stripes == 1) {
                    unionWidth = stripeWidth / 2;
                } else {
                    unionWidth = stripeWidth * Math.ceil(stripes / 2.0);
                }
                unionHeight = 0.5 * height;
                g.setTransform(transform);
                break;
            case HORIZONTAL_TRICOLORE:
                unionWidth = width;
                unionHeight = height / 3;
                stripe = new Rectangle2D.Double(0, 0, unionWidth, unionHeight);
                g.fill(stripe);
                g.translate(0, unionHeight);
                break;
            case VERTICAL_TRICOLORE:
                unionWidth = width / 3;
                unionHeight = height;
                stripe = new Rectangle2D.Double(0, 0, unionWidth, unionHeight);
                g.fill(stripe);
                g.translate(unionWidth, 0);
                break;
            default:
            }

            Rectangle2D.Double union
                = new Rectangle2D.Double(0, 0, unionWidth, unionHeight);
            g.setColor(colorScheme.unionColor);
            g.fill(union);
            if (colorScheme.unionColor == colorScheme.starColor) {
                if (flagType == FlagType.VERTICAL_TRICOLORE
                    || flagType == FlagType.HORIZONTAL_TRICOLORE) {
                    // tricolore with union above or on the left
                    g.setTransform(transform);
                } else {
                    colorScheme.starColor = colorScheme.backgroundColor;
                }
            }

            GeneralPath unionPath;
            if (stars == 1) {
                unionPath = getPentagram();
            } else if (stars == 2) {
                unionPath = new GeneralPath();
                unionPath.append(getPentagram(), false);
                GeneralPath newStar = getPentagram();
                newStar.transform(AffineTransform.getTranslateInstance(unionWidth/3, 0));
                unionPath.append(newStar, false);
            } else if (stars < 14) {
                unionPath = getCircleOfStars(stars);
            } else {
                unionPath = getGrid(stars);
            }
            unionPath.transform(AffineTransform
                .getTranslateInstance(-unionPath.getBounds().getX(),
                    -unionPath.getBounds().getY()));
            double x = unionWidth - unionPath.getBounds().getWidth();
            double y = unionHeight - unionPath.getBounds().getHeight();
            unionPath.transform(AffineTransform.getTranslateInstance(x/2, y/2));
            g.setColor(colorScheme.starColor);
            g.fill(unionPath);

        }

        public Image getImage() {
            return image;
        }

        public GeneralPath getGrid(int states) {
            int[] bars = new int[2];
            for (int count = states; count < 51; count++) {
                if (layout[count][0] > 0) {
                    bars = layout[count];
                    break;
                }
            }
            int maxCols = Math.max(bars[0], bars[1]);
            int rows = 2;
            int sum = bars[0] + bars[1];
            while (sum < states) {
                sum += bars[rows%2];
                rows++;
            }
            double hSpace = unionWidth / (2 * maxCols);
            double vSpace = unionHeight / (2 * rows);
            double y = 0;
            GeneralPath star = getPentagram();
            GeneralPath grid = new GeneralPath();
            int count = 1;
            for (int row = 0; row < rows; row++) {
                int cols = bars[row%2];
                double x = (cols == maxCols) ? 0 : hSpace;
                for (int col = 0; col < cols; col++) {
                    if (count > states) {
                        break;
                    }
                    GeneralPath newStar = (GeneralPath) star.clone();
                    newStar.transform(AffineTransform.getTranslateInstance(x, y));
                    grid.append(newStar, false);
                    x += 2 * hSpace;
                    count++;
                }
                y += 2 * vSpace;
            }
            return grid;
        }

        public GeneralPath getPentagram() {
            double angle = 2 * Math.PI / 5;
            double y = -star / 2;
            GeneralPath pentagram = new GeneralPath(GeneralPath.WIND_NON_ZERO);
            pentagram.moveTo(0, y);
            int[] vertex = new int[] { 2, 4, 1, 3 };
            for (int i : vertex) {
                double phi = i * angle;
                double xx = -y * Math.sin(phi);
                double yy = y * Math.cos(phi);
                pentagram.lineTo(xx, yy);
            }
            pentagram.closePath();
            return pentagram;
        }

        public GeneralPath getCircleOfStars(int n) {
            double radius = 0.3 * unionHeight;
            double phi = Math.PI * 2 / n;
            GeneralPath star = getPentagram();
            GeneralPath circle = new GeneralPath();
            for (int i = 0; i < n; i++) {
                GeneralPath newStar = (GeneralPath) star.clone();
                double xx = -radius - radius * Math.sin(i * phi);
                double yy = -radius * Math.cos(i * phi);
                newStar.transform(AffineTransform.getTranslateInstance(xx, yy));
                circle.append(newStar, false);
            }
            double x = circle.getBounds().getX();
            double y = circle.getBounds().getY();
            circle.transform(AffineTransform.getTranslateInstance(-x, -y));
            return circle;
        }

    }

    private JComboBox flagType;
    private JComboBox colorScheme;
    private JComboBox states;

    private final FlagType[] flagTypes = new FlagType[] {
        FlagType.HORIZONTAL_STRIPES,
        FlagType.VERTICAL_STRIPES,
        FlagType.HORIZONTAL_TRICOLORE,
        FlagType.VERTICAL_TRICOLORE,
        FlagType.QUARTERED
    };
    private final ColorScheme[] colorSchemes = new ColorScheme[] {
        ENGLAND, SPAIN, PORTUGAL, FRANCE, DUTCH
    };
    private final String[] colorSchemeNames = new String[] {
        "ENGLAND", "SPAIN", "PORTUGAL", "FRANCE", "DUTCH"
    };

    final JLabel label = new JLabel();

    @SuppressWarnings("unchecked")
    public FlagTest() {

        super("FlagTest");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new MigLayout("wrap 3"));
        flagType = new JComboBox(flagTypes);
        flagType.addItemListener(this);
        add(flagType);
        colorScheme = new JComboBox(colorSchemeNames);
        colorScheme.addItemListener(this);
        add(colorScheme);
        String[] allStates = new String[50];
        for (int index = 0; index < 50; index++) {
            allStates[index] = Integer.toString(index + 1);
        }
        states = new JComboBox(allStates);
        states.setSelectedIndex(12);
        states.addItemListener(this);
        add(states);
        add(label, "width 200, height 100");
        itemStateChanged(null);
    }

    public void itemStateChanged(ItemEvent e) {
        ColorScheme cs = colorSchemes[colorScheme.getSelectedIndex()];
        FlagType ft = flagTypes[flagType.getSelectedIndex()];
        int number = states.getSelectedIndex() + 1;
        Flag flag = new FlagTest.Flag(cs, ft, number, Math.min(13, number));
        label.setIcon(new ImageIcon(flag.getImage()));
    }

    public static void main(String[] args) {

        FlagTest frame = new FlagTest();

        // display the window
        frame.pack();
        frame.setVisible(true);


    }
}
