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

package net.sf.freecol.client.gui.mapviewer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;
import javax.swing.event.EventListenerList;


/**
 * The blinking cursor on the map.
 */
class TerrainCursor implements ActionListener  {

    public static final int OFF = 0;
    public static final int ON = 1;
    private static final int blinkDelay = 500; // Milliseconds

    /** Is the cursor active? */
    private boolean active;

    /** A timer for the blinking action. */
    private final Timer blinkTimer;

    /** The listeners watching for a blink. */
    private final EventListenerList listenerList = new EventListenerList();


    /**
     * Creates a new {@code TerrainCursor} instance.
     */
    TerrainCursor() {
        this.active = true;
        this.blinkTimer = new Timer(blinkDelay, this);
    }

    /**
     * Is this TerrainCursor active?
     *
     * @return True if the cursor is active.
     */
    boolean isActive() {
        return this.active;
    }

    void startBlinking() {
        if (!this.blinkTimer.isRunning()) this.blinkTimer.start();
    }

    void stopBlinking() {
        if (this.blinkTimer.isRunning()) {
            this.blinkTimer.stop();
            this.active = true;
        }
    }

    void addActionListener(ActionListener listener) {
        listenerList.add(ActionListener.class, listener);
    }

    void removeActionListener(ActionListener listener) {
        listenerList.remove(ActionListener.class, listener);
    }


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        if (!this.blinkTimer.isRunning()) {
            /*
             * Avoids changing active to false after kalling
             * this.blinkTimer.stop() 
             */
            return;
        }
        this.active = !this.active;
        int eventId = (this.active) ? ON : OFF;
        ActionEvent blinkEvent = new ActionEvent(this, eventId, "blink");
        for (ActionListener al : listenerList
                 .getListeners(ActionListener.class)) {
            al.actionPerformed(blinkEvent);
        }
    }
}
