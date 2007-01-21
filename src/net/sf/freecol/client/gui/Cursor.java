package net.sf.freecol.client.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Timer;
import javax.swing.event.EventListenerList;

import net.sf.freecol.common.model.Tile;

public class Cursor implements ActionListener  {
    
    public static final int OFF = 0;
    public static final int ON = 1;
    
    private Tile tile;
    private int canvasX;
    private int canvasY;
    Timer blinkTimer;
    private boolean active;
    private EventListenerList listenerList;
    
    public Cursor(){
        active = true;
        
        final int blinkDelay = 500; // Milliseconds
        
        blinkTimer = new Timer(blinkDelay,this);
        
        listenerList = new EventListenerList();
    }
    
    public boolean isActive(){
        return active;
    }
    
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
        int eventId = active? Cursor.ON : Cursor.OFF;
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
