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
import java.awt.Shape;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Please feel free to correct any vexillological errors.
 */
public class Flag {

    public enum Alignment {
        NONE,
        HORIZONTAL,
        VERTICAL
    };

    public enum Background {
            /** A plain background. */
        PLAIN(Alignment.NONE),
            /** Quartered rectangularly. */
        QUARTERLY(Alignment.NONE),
            /** Vertical stripes. */
        PALES(Alignment.VERTICAL),
            /** Horizontal stripes. */
        FESSES(Alignment.HORIZONTAL),
            /** Diagonal top left to bottom right. */
        PER_BEND(Alignment.NONE),
            /** Diagonal bottom left to top right. */
        PER_BEND_SINISTER(Alignment.NONE),
            /** Quartered diagonally. */
        PER_SALTIRE(Alignment.NONE);

        public final Alignment alignment;

        Background(Alignment alignment) {
            this.alignment = alignment;
        }
    };

    public enum Decoration {
        NONE(),
        GREEK_CROSS(UnionPosition.CANTON),
        SYMMETRIC_CROSS(UnionPosition.CANTON),
        SCANDINAVIAN_CROSS(UnionPosition.CANTON),
        CHEVRON(UnionShape.CHEVRON, UnionPosition.LEFT),
        PALL(UnionShape.CHEVRON, UnionPosition.LEFT),
        BEND(UnionShape.BEND, UnionPosition.TOP, UnionPosition.BOTTOM),
        BEND_SINISTER(UnionShape.BEND, UnionPosition.TOP, UnionPosition.BOTTOM),
        SALTIRE(UnionShape.CHEVRON, UnionPosition.TOP, UnionPosition.BOTTOM,
                UnionPosition.LEFT, UnionPosition.RIGHT),
        SALTIRE_AND_CROSS(UnionPosition.CANTON);

        public UnionShape unionShape = UnionShape.RECTANGLE;
        public Set<UnionPosition> unionPositions = EnumSet.allOf(UnionPosition.class);

        Decoration(UnionPosition... positions) {
            this.unionPositions = EnumSet.of(UnionPosition.NONE);
            for (UnionPosition position : positions) {
                unionPositions.add(position);
            }
        }

        Decoration(UnionShape shape, UnionPosition... positions) {
            this(positions);
            this.unionShape = shape;
        }

    };

    public enum UnionShape {
        RECTANGLE,
        TRIANGLE,
        CHEVRON,
        BEND,
        RHOMBUS
    };

    public enum UnionPosition {
        LEFT(Alignment.VERTICAL, 0),
        CENTER(Alignment.VERTICAL, 1),
        RIGHT(Alignment.VERTICAL, 2),
        TOP(Alignment.HORIZONTAL, 0),
        MIDDLE(Alignment.HORIZONTAL, 1),
        BOTTOM(Alignment.HORIZONTAL, 2),
        CANTON(Alignment.NONE, 0),
        NONE(null, 0);

        public final Alignment alignment;
        public final int index;

        UnionPosition(Alignment alignment, int index) {
            this.alignment = alignment;
            this.index = index;
        }

    }

    /**
     * Width and height are used in the usual sense, not the
     * vexillological sense.
     */
    public static final int WIDTH = 150;
    public static final int HEIGHT = 100;
    public static final double SQRT_3 = Math.sqrt(3);
    public static final double DECORATION_SIZE = (double) HEIGHT / 7;
    public static final double CHEVRON_X = SQRT_3 * HEIGHT / 2;
    public static final double STAR_SIZE = 0.07 * HEIGHT;
    public static final double CROSS_OFFSET = 2 * DECORATION_SIZE;
    public static final double BEND_X = DECORATION_SIZE;
    public static final double BEND_Y = DECORATION_SIZE / SQRT_3;

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

    /**
     * If the background is striped, the background colors will be
     * used in turn, from top to bottom, or from left to right. If the
     * background is quartered, the colors will be used clockwise,
     * starting at the top left. If there are more colors than stripes
     * or quarters, the superfluous colors will be ignored.
     */
    private List<Color> backgroundColors = new ArrayList<Color>();

    private Color unionColor = Color.BLUE;
    private Color starColor = Color.WHITE;
    private Color decorationColor = Color.WHITE;

    private Background background = Background.FESSES;
    private Decoration decoration = Decoration.NONE;
    private UnionShape unionShape; // = UnionShape.RECTANGLE;
    private UnionPosition unionPosition = UnionPosition.CANTON;

    /**
     * The number of stars in the union.
     */
    private int stars = 13;

    /**
     * The number of background stripes.
     */
    private int stripes = 13;


    public Flag(Background background, Decoration decoration,
                UnionPosition unionPosition) {
        this.background = background;
        this.decoration = decoration;
        this.unionPosition = unionPosition;
    }

    public List<Color> getBackgroundColors() {
        return backgroundColors;
    }

    public Flag setBackgroundColors(List<Color> backgroundColors) {
        this.backgroundColors = backgroundColors;
        return this;
    }

    public Flag setBackgroundColors(Color... colors) {
        backgroundColors.clear();
        for (Color color : colors) {
            if (color != null) {
                backgroundColors.add(color);
            }
        }
        return this;
    }

    public Color getUnionColor() {
        return unionColor;
    }

    public Flag setUnionColor(Color unionColor) {
        this.unionColor = unionColor;
        return this;
    }

    public Color getDecorationColor() {
        return decorationColor;
    }

    public Flag setDecorationColor(Color decorationColor) {
        this.decorationColor = decorationColor;
        return this;
    }

    public Color getStarColor() {
        return starColor;
    }

    public Flag setStarColor(Color starColor) {
        this.starColor = starColor;
        return this;
    }

    public int getStars() {
        return stars;
    }

    public Flag setStars(int stars) {
        this.stars = stars;
        return this;
    }

    public int getStripes() {
        return stripes;
    }

    public Flag setStripes(int stripes) {
        this.stripes = stripes;
        return this;
    }

    public Flag setStarsAndStripes(int stars, int stripes) {
        this.stars = stars;
        this.stripes = stripes;
        return this;
    }

    public UnionPosition getUnionPosition() {
        return unionPosition;
    }

    public Flag setUnionPosition(UnionPosition position) {
        this.unionPosition = position;
        return this;
    }

    public BufferedImage getImage() {
        BufferedImage image = new BufferedImage((int) WIDTH, (int) HEIGHT,
                                                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);

        if (backgroundColors.isEmpty()) {
            backgroundColors.add(Color.BLACK);
        }
        // draw background
        switch(background) {
        case FESSES:
        case PALES:
            if (stripes < 1) {
                drawBackground(g);
            } else {
                drawStripes(g, background.alignment, stripes);
            }
            break;
        case PLAIN:
            drawBackground(g);
            break;
        case QUARTERLY:
            drawQuarters(g);
            break;
        case PER_BEND:
            drawPerBend(g, false);
            break;
        case PER_BEND_SINISTER:
            drawPerBend(g, true);
            break;
        case PER_SALTIRE:
            drawPerSaltire(g);
            break;
        }

        // draw decoration
        GeneralPath decorationShape = null;
        switch(decoration) {
        case GREEK_CROSS:
        case SYMMETRIC_CROSS:
        case SCANDINAVIAN_CROSS:
            decorationShape = getCross(decoration);
            break;
        case CHEVRON:
            decorationShape = getChevron(false);
            break;
        case PALL:
            decorationShape = getPall();
            break;
        case BEND:
            decorationShape = getBend(false);
            break;
        case BEND_SINISTER:
            decorationShape = getBend(true);
            break;
        case SALTIRE:
            decorationShape = getBend(true);
            decorationShape.append(getBend(false), false);
            break;
        case SALTIRE_AND_CROSS:
            decorationShape = getBend(true);
            decorationShape.append(getBend(false), false);
            decorationShape.append(getCross(Decoration.SYMMETRIC_CROSS), false);
            break;
        }
        if (decorationShape != null) {
            g.setColor(decorationColor);
            g.fill(decorationShape);
        }

        Shape union = null;
        Shape starShape = null;
        // draw union
        if (unionShape == null && decoration != null) {
            unionShape = decoration.unionShape;
        }
        switch(unionShape) {
        case RECTANGLE:
            Rectangle2D.Double rectangle = getRectangle();
            union = rectangle;
            starShape = getUnionRectangle(rectangle);
            break;
        case CHEVRON:
            union = getChevron(decoration == Decoration.PALL);
            starShape = getUnionTriangle(true);
            break;
        case BEND:
            boolean small = (decoration == Decoration.BEND)
                || (decoration == Decoration.BEND_SINISTER);
            boolean sinister = (decoration == Decoration.BEND_SINISTER)
                || (background == Background.PER_BEND_SINISTER);
            union = getTriangle(small, sinister);
            starShape = getUnionTriangle(false);
            break;
        case RHOMBUS:
            break;
        default:
            break;
        }
        if (!(union == null || unionColor == null)) {
            g.setColor(unionColor);
            g.fill(union);
        }
        if (starShape != null) {
            g.setColor(starColor);
            g.fill(starShape);
        }
        return image;
    }

    private Shape getUnionRectangle(Rectangle2D.Double union) {

        if (union == null) return null;

        double radius = 0.3 * Math.min(union.height, union.width);

        GeneralPath unionPath;
        if (stars == 0) {
            // nothing to do
            return null;
        } else if (stars == 1) {
            unionPath = getStar();
            unionPath.transform(AffineTransform.getScaleInstance(2, 2));
        } else if (stars == 2) {
            unionPath = new GeneralPath();
            GeneralPath newStar = getStar();
            newStar.transform(AffineTransform.getScaleInstance(1.5, 1.5));
            unionPath.append(newStar, false);
            newStar = getStar();
            newStar.transform(AffineTransform.getScaleInstance(1.5, 1.5));
            newStar.transform(AffineTransform.getTranslateInstance(radius, 0));
            unionPath.append(newStar, false);
        } else if (stars < 14) {
            unionPath = getCircleOfStars(radius);
        } else {
            unionPath = getGridOfStars(union);
        }
        Rectangle2D bounds = unionPath.getBounds2D();
        double x = (union.x - bounds.getX()) + (union.width - bounds.getWidth()) / 2;
        double y = (union.y - bounds.getY()) + (union.height - bounds.getHeight()) / 2;
        unionPath.transform(AffineTransform.getTranslateInstance(x, y));
        return unionPath;
    }

    private Shape getUnionTriangle(boolean isosceles) {
        boolean small = (decoration == Decoration.PALL);
        Shape union = getChevron(small);

        double x = 0;
        double y = 0;
        double radius = 0;
        if (isosceles) {
            radius = SQRT_3 * HEIGHT / 6;
            x = CHEVRON_X;
            y = HEIGHT;
            if (small) {
                x -= BEND_X;
                y -= 2 * BEND_Y;
            }
        } else {
            // pythagorean
            double c = Math.sqrt(WIDTH * WIDTH + HEIGHT * HEIGHT);
            double A = HEIGHT * WIDTH / 2;
            radius = 2 * A / (WIDTH + HEIGHT + c);
            if (unionPosition == UnionPosition.TOP
                || unionPosition == UnionPosition.BOTTOM) {
                x = HEIGHT;
                y = WIDTH;
            } else {
                x = WIDTH;
                y = HEIGHT;
            }
        }
        // leave a margin
        radius = radius * 0.6;

        int sum = 0;
        int rows = 1;
        while (sum < stars) {
            sum += rows;
            rows++;
        }
        int missing = sum - stars;
        double slope = y / x;
        double dx = x / rows;
        double xx = dx / 2;
        rows--;
        GeneralPath unionPath = new GeneralPath();
        for (int index = rows; index > 0; index--) {
            double height = y - xx * slope;
            double dy = height / index;
            double yy = dy / 2;
            double offset = isosceles
                ? (HEIGHT - height) / 2 : 0;
            int count = index;
            if (missing > 0) {
                count = index - missing;
                yy += missing / 2 * dy;
                missing = 0;
            }
            for (int star = 0; star < count; star++) {
                GeneralPath newStar = getStar();
                newStar.transform(AffineTransform.getTranslateInstance(xx, yy + offset));
                unionPath.append(newStar, false);
                yy += dy;
            }
            xx += dx;
        }
        /*
        unionPath.transform(AffineTransform.getTranslateInstance(SQRT_3 * HEIGHT / 6
                                                                 - unionPath.getBounds().getWidth() /2,
                                                               SQRT_3 * HEIGHT / 6));
        */
        return unionPath;
    }

    private double getStripeWidth(Alignment alignment) {
        return (alignment == Alignment.HORIZONTAL)
            ? WIDTH : (double) WIDTH / stripes;
    }

    private double getStripeHeight(Alignment alignment) {
        return (alignment == Alignment.VERTICAL)
            ? HEIGHT : (double) HEIGHT / stripes;
    }

    private void drawBackground(Graphics2D g) {
        g.setColor(backgroundColors.get(0));
        g.fillRect(0, 0, WIDTH, HEIGHT);
    }

    private void drawStripes(Graphics2D g, Alignment alignment, int stripes) {
        int colors = backgroundColors.size();
        double stripeWidth = getStripeWidth(alignment);
        double stripeHeight = getStripeHeight(alignment);
        double x = (alignment == Alignment.VERTICAL)
            ? stripeWidth : 0;
        double y = (alignment == Alignment.HORIZONTAL)
            ? stripeHeight : 0;
        Rectangle2D.Double rectangle = new Rectangle2D.Double();
        for (int index = 0; index < stripes; index++) {
            g.setColor(backgroundColors.get(index % colors));
            rectangle.setRect(index * x, index * y, stripeWidth, stripeHeight);
            g.fill(rectangle);
        }
    }

    private void drawQuarters(Graphics2D g) {
        int colors = backgroundColors.size();
        int[] x = new int[] { 0, 1, 1, 0 };
        int[] y = new int[] { 0, 0, 1, 1 };
        double halfWidth = WIDTH / 2;
        double halfHeight = HEIGHT / 2;
        double offset = (decoration == Decoration.SCANDINAVIAN_CROSS)
            ? CROSS_OFFSET : 0;
        Rectangle2D.Double rectangle = new Rectangle2D.Double();
        for (int index = 0; index < 4; index++) {
            g.setColor(backgroundColors.get(index % colors));
            rectangle.setRect(x[index] * halfWidth - offset, y[index] * halfHeight,
                              halfWidth + x[index] * offset, halfHeight);
            g.fill(rectangle);
        }
    }

    private void drawPerBend(Graphics2D g, boolean sinister) {
        drawBackground(g);
        int colors = backgroundColors.size();
        GeneralPath path = new GeneralPath();
        path.moveTo(0, HEIGHT);
        path.lineTo(sinister ? WIDTH : 0, 0);
        path.lineTo(WIDTH, HEIGHT);
        g.setColor(backgroundColors.get(1 % colors));
        g.fill(path);
    }

    private void drawPerSaltire(Graphics2D g) {
        int colors = backgroundColors.size();
        GeneralPath path = new GeneralPath();
        int[] x = new int[] { 0, WIDTH, WIDTH, 0 };
        int[] y = new int[] { 0, 0, HEIGHT, HEIGHT };
        double halfWidth = WIDTH / 2;
        double halfHeight = HEIGHT / 2;
        for (int index = 0; index < 4; index++) {
            path.moveTo(x[index], y[index]);
            path.lineTo(halfWidth, halfHeight);
            int nextIndex = (index + 1) % 4;
            path.lineTo(x[nextIndex], y[nextIndex]);
            g.setColor(backgroundColors.get(index % colors));
            g.fill(path);
            path.reset();
        }
    }

    private GeneralPath getCross(Decoration decoration) {
        double quarterWidth = (WIDTH - DECORATION_SIZE) / 2;
        double quarterHeight = (HEIGHT - DECORATION_SIZE) / 2;
        double offset = 0;
        double width = WIDTH;
        double height = HEIGHT;
        switch(decoration) {
        case SCANDINAVIAN_CROSS:
            offset = CROSS_OFFSET;
            break;
        case GREEK_CROSS:
            width = height = Math.min(WIDTH, HEIGHT) - 2 * DECORATION_SIZE;
            break;
        }
        GeneralPath cross = new GeneralPath();
        cross.append(new Rectangle2D.Double((WIDTH - width) / 2, quarterHeight,
                                            width, DECORATION_SIZE), false);
        cross.append(new Rectangle2D.Double(quarterWidth - offset, (HEIGHT - height) / 2,
                                            DECORATION_SIZE, height), false);
        return cross;
    }

    private GeneralPath getBend(boolean sinister) {
        GeneralPath path = new GeneralPath();
        if (sinister) {
            path.moveTo(0, HEIGHT);
            path.lineTo(0, HEIGHT - BEND_Y);
            path.lineTo(WIDTH - BEND_X, 0);
            path.lineTo(WIDTH, 0);
            path.lineTo(WIDTH, BEND_Y);
            path.lineTo(BEND_X, HEIGHT);
        } else {
            path.moveTo(0, 0);
            path.lineTo(BEND_X, 0);
            path.lineTo(WIDTH, HEIGHT - BEND_Y);
            path.lineTo(WIDTH, HEIGHT);
            path.lineTo(WIDTH - BEND_X, HEIGHT);
            path.lineTo(0, BEND_Y);
        }
        return path;
    }

    private GeneralPath getPall() {
        double y1 = (HEIGHT - DECORATION_SIZE) / 2;
        double y2 = (HEIGHT + DECORATION_SIZE) / 2;
        double x = BEND_X + y1 * SQRT_3;
        GeneralPath path = new GeneralPath(getChevron(true));
        path.lineTo(0, HEIGHT);
        path.lineTo(BEND_X, HEIGHT);
        path.lineTo(x, y2);
        path.lineTo(WIDTH, y2);
        path.lineTo(WIDTH, y1);
        path.lineTo(x, y1);
        path.lineTo(BEND_X, 0);
        path.lineTo(0, 0);
        return path;
    }

    private GeneralPath getChevron(boolean small) {
        GeneralPath path = new GeneralPath();
        double x = 0;
        double y = 0;
        if (small) {
            x = BEND_X;
            y = BEND_Y;
        }
        path.moveTo(0, y);
        path.lineTo(CHEVRON_X - x, HEIGHT / 2);
        path.lineTo(0, HEIGHT - y);
        return path;
    }

    private GeneralPath getTriangle(boolean small, boolean sinister) {
        GeneralPath path = new GeneralPath();
        double x = 0;
        double y = 0;
        if (small) {
            x = BEND_X;
            y = BEND_Y;
        }
        if (sinister) {
            path.moveTo(x, HEIGHT);
            path.lineTo(WIDTH, y);
            path.lineTo(WIDTH, HEIGHT);
        } else {
            path.moveTo(0, HEIGHT - y);
            path.lineTo(0, 0);
            path.lineTo(WIDTH - x, 0);
        }
        return path;
    }


    private Rectangle2D.Double getRectangle() {
        if (unionPosition == null || unionPosition == UnionPosition.NONE) {
            return null;
        }
        Rectangle2D.Double union = new Rectangle2D.Double();
        if (unionPosition.alignment == Alignment.VERTICAL) {
            if (background.alignment == Alignment.VERTICAL
                && stripes < 3) {
                union.width = WIDTH / stripes;
                if (stripes == 2 && unionPosition == UnionPosition.RIGHT) {
                    union.x = WIDTH /2;
                }
            } else {
                union.width = WIDTH / 3;
                union.x = unionPosition.index * union.width;
            }
            union.height = HEIGHT;
        } else if (unionPosition.alignment == Alignment.HORIZONTAL) {
            if (background.alignment == Alignment.HORIZONTAL
                && stripes < 3) {
                union.height = HEIGHT / stripes;
                if (stripes == 2 && unionPosition == UnionPosition.BOTTOM) {
                    union.y = HEIGHT / 2;
                }
            } else {
                union.height = HEIGHT / 3;
                union.y = unionPosition.index * union.height;
            }
            union.width = WIDTH;
        } else {
            // canton
            union.width = WIDTH / 2;
            union.height = HEIGHT / 2;
            if (background.alignment == Alignment.HORIZONTAL) {
                union.height = (stripes < 3)
                    ? HEIGHT / 2
                    : (stripes / 2) * getStripeHeight(background.alignment);
            } else if (background.alignment == Alignment.VERTICAL) {
                union.width = (stripes < 3)
                    ? WIDTH / 2
                    : (stripes / 2) * getStripeWidth(background.alignment);
            }
            if (decoration == Decoration.GREEK_CROSS
                || decoration == Decoration.SYMMETRIC_CROSS) {
                union.width = (WIDTH - DECORATION_SIZE) / 2;
                union.height = (HEIGHT - DECORATION_SIZE) / 2;
            } else if (decoration == Decoration.SCANDINAVIAN_CROSS) {
                union.width = (WIDTH - DECORATION_SIZE) / 2
                    - CROSS_OFFSET;
                union.height = (HEIGHT - DECORATION_SIZE) / 2;
            }
        }

        return union;
    }

    public GeneralPath getStar() {
        return (GeneralPath) star.clone();
    }

    private GeneralPath getCircleOfStars(double radius) {
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


    public GeneralPath getGridOfStars(Rectangle2D.Double union) {
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

}