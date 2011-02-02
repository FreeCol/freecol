/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class FlagTest extends JFrame {

    private static final int[][] layout = new int[51][2];

    static {
	for (int[] bars : new int[][] {
		 { 5, 4 }, { 5, 6 }, { 6, 5 }
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

	private static final double width = 100;
	private static final double length = 1.9 * width;
	private static final double unionLength = 0.4 * length;
	private static final double star = 0.0616 * width;
	private double unionWidth;

	private final Color red = new Color(.698f, .132f, .203f);
	private final Color blue = new Color(.234f, .233f, .430f);

	private BufferedImage image;

	public Flag(int stars, int stripes) {

	    image = new BufferedImage((int) length, (int) width,
				      BufferedImage.TYPE_INT_RGB);
	    Graphics2D g = image.createGraphics();
	    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
			       RenderingHints.VALUE_ANTIALIAS_ON);

	    g.setColor(red);
	    Rectangle2D.Double background =
		new Rectangle2D.Double(0, 0, length, width);
	    g.fill(background);
	    g.setColor(Color.WHITE);
	    double stripeWidth = width / stripes;
	    Rectangle2D.Double stripe =
		new Rectangle2D.Double(0, 0, length, stripeWidth);
	    AffineTransform transform = g.getTransform();
	    for (int i = 0; i < stripes / 2; i++) {
		g.translate(0, stripeWidth);
		g.fill(stripe);
		g.translate(0, stripeWidth);
	    }
	    g.setTransform(transform);
	    g.setColor(blue);
	    if (stripes == 1) {
		unionWidth = stripeWidth / 2;
	    } else {
		unionWidth = stripeWidth * Math.ceil(stripes / 2.0);
	    }
	    Rectangle2D.Double union =
		new Rectangle2D.Double(0, 0, unionLength, unionWidth);
	    g.fill(union);

	    GeneralPath unionPath;
	    if (stars == 1) {
		unionPath = getPentagram();
	    } else if (stars == 2) {
		unionPath = new GeneralPath();
		unionPath.append(getPentagram(), false);
		GeneralPath newStar = getPentagram();
		newStar.transform(AffineTransform.getTranslateInstance(unionLength/3, 0));
		unionPath.append(newStar, false);
	    } else if (stars < 14) {
		unionPath = getCircleOfStars(stars);
	    } else {
		unionPath = getGrid(stars);
	    }
	    unionPath.transform(AffineTransform
				.getTranslateInstance(-unionPath.getBounds().getX(),
						      -unionPath.getBounds().getY()));
	    double x = unionLength - unionPath.getBounds().getWidth();
	    double y = unionWidth - unionPath.getBounds().getHeight();
	    unionPath.transform(AffineTransform.getTranslateInstance(x/2, y/2));
	    g.setColor(Color.WHITE);
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
	    double hSpace = unionLength / (2 * maxCols);
	    double vSpace = unionWidth / (2 * rows);
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
	    double radius = 0.3 * unionWidth;
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

    public FlagTest() {

	super("FlagTest");
	JPanel panel = new JPanel(new GridLayout(0, 5, 5, 5));
        JScrollPane scrollPane = new JScrollPane(panel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                 JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	for (int states = 1; states < 51; states++) {
	    Flag flag = new FlagTest.Flag(states, Math.min(13, states));
	    JLabel label = new JLabel(new ImageIcon(flag.getImage()));
	    panel.add(label);
	}
	add(scrollPane);
    }


    public static void main(String[] args) {

	FlagTest frame = new FlagTest();

	// display the window
	frame.pack();
	frame.setVisible(true);


    }

}