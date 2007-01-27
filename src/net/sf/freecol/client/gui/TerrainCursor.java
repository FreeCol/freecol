package net.sf.freecol.client.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Timer;
import javax.swing.event.EventListenerList;

import net.sf.freecol.common.model.Tile;

public class TerrainCursor implements ActionListener  {
    
    public static final String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public static final int OFF = 0;
    public static final int ON = 1;
    
    private Tile tile;
    private int canvasX;
    private int canvasY;
    private Timer blinkTimer;
    private boolean active;
    private EventListenerList listenerList;
    
    /**
     * Creates a new <code>Cursor</code> instance.
     *
     */
    public TerrainCursor() {
        active = true;
        
        final int blinkDelay = 500; // Milliseconds
        
        blinkTimer = new Timer(blinkDelay,this);
        
        listenerList = new EventListenerList();
    }
    
    /**
     * Returns whether this TerrainCursor is active.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isActive(){
        return active;
    }
    
    /**
     * Sets the active state of the TerrainCursor.
     *
     * @param newState a <code>boolean</code> value
     */
    public void setActive(boolean newState){
        active = newState;
    }
    
    public void startBlinking(){
        if(!blinkTimer.isRunning())
            blinkTimer.start();
    }
    
    public void stopBlinking(){
        if(blinkTimer.isRunning())
            blinkTimer.stop();
    }
    
    public void setTile(Tile tile){
        this.tile = tile;
    }
    
    public Tile getTile(){
        return tile;
    }
    
    public void setCanvasPos(int x,int y){
        canvasX = x;
        canvasY = y;
    }
    
    public int getCanvasX(){
        return canvasX;
    }
    
    public int getCanvasY(){
        return canvasY;
    }
    
    public void addActionListener(ActionListener listener){
        listenerList.add(ActionListener.class, listener);
    }
    
    public void removeActionListener(ActionListener listener){
        listenerList.remove(ActionListener.class, listener);
    }
    
    public void actionPerformed(ActionEvent timerEvent){
        active = !active;
        int eventId = active? ON : OFF;
        ActionEvent blinkEvent = new ActionEvent(this,eventId,"blink");
        
        fireActionEvent(blinkEvent);
    }
    
    public void fireActionEvent(ActionEvent event){
        ActionListener[] listeners = listenerList.getListeners(ActionListener.class);
        for(int i=0; i < listenerList.getListenerCount(); i++){
            listeners[i].actionPerformed(event);
        }
    }
  
    
}
