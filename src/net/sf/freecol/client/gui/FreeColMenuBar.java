package net.sf.freecol.client.gui;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.FreeCol;

import javax.swing.*;
import java.awt.event.*;
import java.util.Iterator;
import java.util.ArrayList;


/**
* The menu bar that is displayed on the top left corner of the <code>Canvas</code>.
* @see Canvas#setJMenuBar
*/
public class FreeColMenuBar extends JMenuBar {

    private final FreeColClient freeColClient;
    private final Canvas canvas;
    private final GUI gui;
    private ArrayList inGameOptions = new ArrayList();
    


    /**
    * Creates a new <code>FreeColMenuBar</code>. This menu bar will include
    * all of the submenus and items.
    *
    * @param f The main controller.
    * @param c The {@link Canvas} that contains methods to show panels e.t.c.
    * @param g The {@link GUI} that has methods to draw the game map.
    */
    public FreeColMenuBar(FreeColClient f, Canvas c, GUI g) {
        super();
        
        this.freeColClient = f;
        this.canvas = c;
        this.gui = g;

        // --> Game
        JMenu gameMenu = new JMenu("Game");
        gameMenu.setOpaque(false);
        gameMenu.setMnemonic(KeyEvent.VK_G);
        add(gameMenu);

        JMenuItem newMenuItem = new JMenuItem("New");
        newMenuItem.setOpaque(false);
        newMenuItem.setMnemonic(KeyEvent.VK_N);
        gameMenu.add(newMenuItem);
        newMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                canvas.newGame();
            }
        });

        JMenuItem openMenuItem = new JMenuItem("Open");
        openMenuItem.setOpaque(false);
        openMenuItem.setMnemonic(KeyEvent.VK_O);
        gameMenu.add(openMenuItem);
        openMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // TODO: show a confirmation dialog and quit the game
                canvas.showOpenGamePanel();
            }
        });

        gameMenu.addSeparator();

        JMenuItem quitMenuItem = new JMenuItem("Quit");
        quitMenuItem.setOpaque(false);
        quitMenuItem.setMnemonic(KeyEvent.VK_Q);
        gameMenu.add(quitMenuItem);
        quitMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                canvas.quit();
            }
        });

        // --> View

        JMenu viewMenu = new JMenu("View");
        viewMenu.setOpaque(false);
        viewMenu.setMnemonic(KeyEvent.VK_V);
        add(viewMenu);
        inGameOptions.add(viewMenu);

        JMenuItem europeMenuItem = new JMenuItem("Europe");
        europeMenuItem.setOpaque(false);
        europeMenuItem.setMnemonic(KeyEvent.VK_E);
        viewMenu.add(europeMenuItem);
        europeMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                canvas.showEuropePanel();
            }
        });
        inGameOptions.add(europeMenuItem);

        // --> Debug
        if (FreeCol.isInDebugMode()) {
            JMenu debugMenu = new JMenu("Debug");
            debugMenu.setOpaque(false);
            debugMenu.setMnemonic(KeyEvent.VK_D);
            add(debugMenu);
            inGameOptions.add(debugMenu);

            JCheckBoxMenuItem sc = new JCheckBoxMenuItem("Show coordinates");
            sc.setOpaque(false);
            sc.setMnemonic(KeyEvent.VK_S);
            debugMenu.add(sc);
            inGameOptions.add(sc);

            sc.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    gui.displayCoordinates = ((JCheckBoxMenuItem) e.getSource()).isSelected();
                    canvas.refresh();
                }
            });

            final JMenuItem reveal = new JCheckBoxMenuItem("Reveal entire map");
            reveal.setOpaque(false);
            reveal.setMnemonic(KeyEvent.VK_R);
            debugMenu.add(reveal);
            inGameOptions.add(reveal);

            reveal.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (freeColClient.getFreeColServer() != null) {
                        freeColClient.getFreeColServer().revealMapForAllPlayers();
                    }

                    reveal.setEnabled(false);
                }
            });
        }
    }
    
    
    public void setEnabled(boolean enabled) {
        Iterator componentIterator = inGameOptions.iterator();
        while (componentIterator.hasNext()) {
            ((JComponent) componentIterator.next()).setEnabled(enabled);
        }
    }
}
