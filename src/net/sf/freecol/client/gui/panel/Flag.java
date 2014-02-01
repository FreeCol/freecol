/**
 *  Copyright (C) 2002-2014   The FreeCol Team
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

package net.sf.freecol.client.gui.panel;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;


public class Flag {

    public enum Alignment {
        HORIZONTAL,
        VERTICAL,
        NONE
    };

    public enum Background {
        STRIPES,
        CROSS,
        // SALTIRE
    };

    public enum UnionPosition {
        LEFT(Alignment.VERTICAL, 0),
        CENTER(Alignment.VERTICAL, 1),
        RIGHT(Alignment.VERTICAL, 2),
        TOP(Alignment.HORIZONTAL, 0),
        MIDDLE(Alignment.HORIZONTAL, 1),
        BOTTOM(Alignment.HORIZONTAL, 2),
        INSET(null, 0);

        public final Alignment alignment;
        public final int index;

        UnionPosition(Alignment alignment, int index) {
            this.alignment = alignment;
            this.index = index;
        }
    }

    public static final int WIDTH = 150;
    public static final int HEIGHT = 100;
    public static final double STAR_SIZE = 0.07 * HEIGHT;

    private static final GeneralPath star
        = new GeneralPath(GeneralPath.WIND_NON_ZERO);

    static {
        double angle = 2 * Math.PI / 5;
        double y = -STAR_SIZE / 2;
        star.moveTo(0, y);
        int[] vertex = new int[] { 2, 4, 1, 3 };
        for (int i : vertex) {
            double phi = i * angle;
            double xx = -y * Math.sin(phi);
            double yy = y * Math.cos(phi);
            star.lineTo(xx, yy);
        }
        star.closePath();
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

    private List<Color> backgroundColors = new ArrayList<Color>();
    // first stripe or background
    // Color.RED,
    // second stripe
    // Color.WHITE,
    // third stripe, or second background color if cross or saltire
    // Color.BLUE

    private Color unionColor = Color.BLUE;
    private Color starColor = Color.WHITE;

    private final Rectangle2D.Double union = new Rectangle2D.Double();

    private Alignment alignment = Alignment.HORIZONTAL;
    private Background background = Background.STRIPES;
    private UnionPosition unionPosition = UnionPosition.INSET;

    /**
     * The number of stars in the union.
     */
    private int stars = 13;

    /**
     * The number of background stripes.
     */
    private int stripes = 13;


    public Flag(Alignment alignment, Background background,
                UnionPosition unionPosition) {
        this.alignment = alignment;
        this.background = background;
        this.unionPosition = unionPosition;
    }

    public List<Color> getBackgroundColors() {
        return backgroundColors;
    }

    public void setBackgroundColors(List<Color> backgroundColors) {
        this.backgroundColors = backgroundColors;
    }

    public void setBackgroundColors(Color[] colors) {
        backgroundColors.clear();
        for (Color color : colors) {
            if (color != null) {
                backgroundColors.add(color);
            }
        }
    }

    public Color getUnionColor() {
        return unionColor;
    }

    public void setUnionColor(Color unionColor) {
        this.unionColor = unionColor;
    }

    public Color getStarColor() {
        return starColor;
    }

    public void setStarColor(Color starColor) {
        this.starColor = starColor;
    }

    public int getStars() {
        return stars;
    }

    public void setStars(int stars) {
        this.stars = stars;
    }

    public int getStripes() {
        return stripes;
    }

    public void setStripes(int stripes) {
        this.stripes = stripes;
    }

    public void setStarsAndStripes(int stars, int stripes) {
        this.stars = stars;
        this.stripes = stripes;
    }

    public UnionPosition getUnionPosition() {
        return unionPosition;
    }

    public void setUnionPosition(UnionPosition position) {
        this.unionPosition = position;
    }

    public BufferedImage getImage() {
        BufferedImage image = new BufferedImage((int) WIDTH, (int) HEIGHT,
                                                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);

        // draw background
        Rectangle2D.Double rectangle = new Rectangle2D.Double();
        int colors = backgroundColors.size();
        double stripeWidth = WIDTH;
        double stripeHeight = HEIGHT;
        double quarterWidth = 0;
        double quarterHeight = 0;
        switch(background) {
        case STRIPES:
            if (stripes < 1) {
                // we always need background color
                stripes = 1;
            }
            double x = 0, y = 0;
            if (alignment == Alignment.HORIZONTAL) {
                stripeHeight = (double) HEIGHT / stripes;
                y = stripeHeight;
            } else {
                stripeWidth = (double) WIDTH / stripes;
                x = stripeWidth;
            }
            for (int index = 0; index < stripes; index++) {
                try {
                    g.setColor(backgroundColors.get(index % colors));
                    rectangle.setRect(index * x, index * y, stripeWidth, stripeHeight);
                    g.fill(rectangle);
                } catch(Exception e) {
                    // ignore for now : no background colors defined
                }
            }
            break;
        case CROSS:
            unionPosition = UnionPosition.INSET; // for the moment
            alignment = null; // probably always
            g.setColor(backgroundColors.get(0));
            rectangle.setRect(0, 0, WIDTH, HEIGHT);
            g.fill(rectangle);
            double stripeSize = HEIGHT / 7;
            quarterWidth = (WIDTH - stripeSize) / 2;
            quarterHeight = (HEIGHT - stripeSize) / 2;
            g.setColor(backgroundColors.get(1));
            rectangle.setRect(0, 0, quarterWidth, quarterHeight);
            g.fill(rectangle);
            rectangle.setRect(quarterWidth + stripeSize, quarterHeight + stripeSize,
                              quarterWidth, quarterHeight);
            g.fill(rectangle);
            if (colors > 2) {
                g.setColor(backgroundColors.get(2));
            }
            rectangle.setRect(0, quarterHeight + stripeSize, quarterWidth, quarterHeight);
            g.fill(rectangle);
            rectangle.setRect(quarterWidth + stripeSize, 0, quarterWidth, quarterHeight);
            g.fill(rectangle);
            break;
        }

        // draw union
        if (unionPosition.alignment == Alignment.VERTICAL) {
            if (alignment == Alignment.VERTICAL
                && stripes < 3) {
                union.width = stripeWidth;
                if (stripes == 2 && unionPosition == UnionPosition.RIGHT) {
                    union.x = WIDTH /2;
                }
            } else {
                union.width = WIDTH / 3;
                union.x = unionPosition.index * union.width;
            }
            union.height = HEIGHT;
        } else if (unionPosition.alignment == Alignment.HORIZONTAL) {
            if (alignment == Alignment.HORIZONTAL
                && stripes < 3) {
                union.height = stripeHeight;
                if (stripes == 2 && unionPosition == UnionPosition.BOTTOM) {
                    union.y = HEIGHT / 2;
                }
            } else {
                union.height = HEIGHT / 3;
                union.y = unionPosition.index * union.height;
            }
            union.width = WIDTH;
        } else {
            // inset
            if (alignment == Alignment.HORIZONTAL) {
                union.width = WIDTH / 2;
                if (stripes == 1) {
                    union.height = HEIGHT / 2;
                } else {
                    union.height = stripeHeight * (stripes / 2);
                }
            } else if (alignment == Alignment.VERTICAL) {
                if (stripes == 1) {
                    union.width = WIDTH / 2;
                } else {
                    union.width = stripeWidth * (stripes / 2);
                }
                union.height = HEIGHT / 2;
            } else {
                union.width = quarterWidth;
                union.height = quarterHeight;
            }
        }

        GeneralPath unionPath;
        if (stars == 0) {
            // don't draw union
            return image;
        } else if (stars == 1) {
            unionPath = getStar();
        } else if (stars == 2) {
            unionPath = new GeneralPath();
            unionPath.append(getStar(), false);
            GeneralPath newStar = getStar();
            newStar.transform(AffineTransform.getTranslateInstance(union.width/3, 0));
            unionPath.append(newStar, false);
        } else if (stars < 14) {
            unionPath = getCircleOfStars();
        } else {
            unionPath = getGridOfStars();
        }
        if (unionColor != null) {
            g.setColor(unionColor);
            g.fill(union);
        }
        g.translate(union.x, union.y);
        unionPath.transform(AffineTransform
                            .getTranslateInstance(-unionPath.getBounds().getX(),
                                                  -unionPath.getBounds().getY()));
        double x = union.width - unionPath.getBounds().getWidth();
        double y = union.height - unionPath.getBounds().getHeight();
        unionPath.transform(AffineTransform.getTranslateInstance(x/2, y/2));
        g.setColor(starColor);
        g.fill(unionPath);

        return image;
    }


    public GeneralPath getStar() {
        return (GeneralPath) star.clone();
    }

    private GeneralPath getCircleOfStars() {
        double radius = 0.3 * Math.min(union.height, union.width);
        double phi = Math.PI * 2 / stars;
        GeneralPath circle = new GeneralPath();
        for (int i = 0; i < stars; i++) {
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


    public GeneralPath getGridOfStars() {
        int[] bars = new int[2];
        for (int count = stars; count < 51; count++) {
            if (layout[count][0] > 0) {
                bars = layout[count];
                break;
            }
        }
        int maxCols = Math.max(bars[0], bars[1]);
        int rows = 2;
        int sum = bars[0] + bars[1];
        while (sum < stars) {
            sum += bars[rows%2];
            rows++;
        }
        double hSpace = union.getWidth() / (2 * maxCols);
        double vSpace = union.getHeight() / (2 * rows);
        double y = 0;
        GeneralPath grid = new GeneralPath();
        int count = 1;
        for (int row = 0; row < rows; row++) {
            int cols = bars[row%2];
            double x = (cols == maxCols) ? 0 : hSpace;
            for (int col = 0; col < cols; col++) {
                if (count > stars) {
                    break;
                }
                GeneralPath newStar = getStar();
                newStar.transform(AffineTransform.getTranslateInstance(x, y));
                grid.append(newStar, false);
                x += 2 * hSpace;
                count++;
            }
            y += 2 * vSpace;
        }
        return grid;
    }


    public static Flag tricolore(Alignment alignment, Color... colors) {
        UnionPosition position = (alignment == Alignment.HORIZONTAL)
            ? UnionPosition.MIDDLE : UnionPosition.CENTER;
        Flag result = new Flag(alignment, Background.STRIPES, position);
        result.stripes = 3;
        result.unionColor = null; // use background
        result.setBackgroundColors(colors);
        return result;
    }

    public static Flag starsAndStripes(Alignment alignment, Color unionColor, Color... colors) {
        Flag result = new Flag(alignment, Background.STRIPES, UnionPosition.INSET);
        result.unionColor = unionColor;
        result.setBackgroundColors(colors);
        return result;
    }

    public static Flag quartered(Color... colors) {
        Flag result = new Flag(null, Background.CROSS, UnionPosition.INSET);
        result.setUnionColor(null); // use background
        result.setBackgroundColors(colors);
        return result;
    }

}