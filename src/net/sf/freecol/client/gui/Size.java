/**
 *  Copyright (C) 2002-2022   The FreeCol Team
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

package net.sf.freecol.client.gui;

/**
 * Standard sizes.
 */
public enum Size {

    TINY   (0.25f, 12f),
    SMALLER(0.5f,  14f),
    SMALL  (0.75f, 16f),
    NORMAL (1.0f,  18f),
    LARGE  (1.25f, 20f),
    BIG    (1.5f,  24f),
    HUGE   (1.75f, 32f),
    MAX    (2f,  40f);

    /** The scaling value to use for images. */
    private final float image;
    /** The value to use for font point sizes. */
    private final float font;

    Size(float image, float font) {
        this.image = image;
        this.font = font;
    }

    public float forImage() {
        return this.image;
    }

    public float forFont() {
        return this.font;
    }

    public Size scaled(float f) {
        int val = this.ordinal() + Math.round(4 * (f - 1.0f));
        return (val < 0 || val >= Size.MAX.ordinal()) ? null
            : Size.values()[val];
    }
    
    public Size up() {
        return (this == Size.MAX) ? Size.MAX
            : Size.values()[this.ordinal() + 1];
    }

    public Size down() {
        return (this == Size.TINY) ? Size.TINY
            : Size.values()[this.ordinal() - 1];
    }
};
