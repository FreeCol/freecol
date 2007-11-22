/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;

/**
 * This is the About panel 
 */
public final class AboutPanel extends FreeColPanel implements ActionListener {

    private static final Logger logger = Logger.getLogger(AboutPanel.class.getName());

    private static final int CLOSE = 0;
        
    private final Canvas            parent;
    private final FreeColClient     freeColClient;
 
    private JButton exitButton;
    
    /**
    * The constructor that will add the items to this panel.
    * 
    * @param parent The parent of this panel.
    * @param freeColClient The main controller object for the
    *       client.
    */
    public AboutPanel(Canvas parent, FreeColClient freeColClient) {
        this.parent = parent;
        this.freeColClient = freeColClient;
        
        setLayout(new BorderLayout());

        // Header with image
        JPanel header = new JPanel();
        this.add(header, BorderLayout.NORTH);
        Image tempImage = (Image) UIManager.get("TitleImage");
        if (tempImage != null) {
            JLabel logoLabel = new JLabel(new ImageIcon(tempImage));
            logoLabel.setBorder(new CompoundBorder(new EmptyBorder(2,2,2,2), new BevelBorder(BevelBorder.LOWERED)));
            header.add(logoLabel,JPanel.CENTER_ALIGNMENT);
        }
        
        // Info panel
        JPanel infoPanel = new JPanel(new GridLayout(3, 1));
        this.add(infoPanel,BorderLayout.CENTER);
        // version and links
        JPanel table = new JPanel(new GridLayout(3, 2));
        infoPanel.add(table);
        table.add(new JLabel(Messages.message("aboutPanel.version")));
        table.add(new JLabel(FreeCol.getRevision()));
        table.add(new JLabel(Messages.message("aboutPanel.officialSite")));
        String siteURL = "http://www.freecol.org";
        JLabel site = new JLabel("<html><font color='Blue'>"+siteURL+"</font></html>");
        site.setFocusable(true);
        site.addMouseListener(new URLMouseListener(siteURL));
        table.add(site);
        table.add(new JLabel(Messages.message("aboutPanel.sfProject")));
        String projectURL = "http://sourceforge.net/projects/freecol/";
        JLabel project = new JLabel("<html><font color='Blue'>"+projectURL+"</font></html>");
        project.setFocusable(true);
        project.addMouseListener(new URLMouseListener(projectURL));
        table.add(project);
        // license disclaimer
        String disclaimer = Messages.message("aboutPanel.legalDisclaimer");
        JTextArea textarea = new JTextArea();
        textarea.setText(disclaimer);
        textarea.setLineWrap(true);
        textarea.setWrapStyleWord(true);
        infoPanel.add(textarea);
        // copyright
        infoPanel.add(new JLabel(Messages.message("aboutPanel.copyright"),JLabel.CENTER),BorderLayout.CENTER);
        
        // Close button
        exitButton = new JButton(Messages.message("close"));
        exitButton.addActionListener(this);
        enterPressesWhenFocused(exitButton);
        setCancelComponent(exitButton);
        exitButton.setActionCommand(String.valueOf(CLOSE));        
        this.add(exitButton,BorderLayout.SOUTH);
        exitButton.setFocusable(true);

        setSize(getPreferredSize());
    }

     

    /**
    * This function analyzes an event and calls the right methods to take
    * care of the user's requests.
    * @param event The incoming ActionEvent.
    */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        try {
            switch (Integer.valueOf(command).intValue()) {
                case CLOSE:
                    parent.remove(this);
                    break;
                default:
                    logger.warning("Invalid Actioncommand: invalid number.");
            }
        } catch (NumberFormatException e) {
            logger.warning("Invalid Actioncommand: not a number.");
        }
    }
    
    /**
     * This inner class is meant to handle mouse click events from hypertext-style links
     * Swing has no explicit support for links, but they can be simulated with
     * JLabel, HTML fragments and a mouse listener such as this one.
     * This class could also be moved from AboutPanel if needed somewhere else. 
     */
    public class URLMouseListener implements MouseListener {
    	private String url;
    	public URLMouseListener(String url) {
    		this.url = url;
    	}
    	public void mouseEntered(MouseEvent e) {
    	}
    	public void mouseExited(MouseEvent e) {
    	}
    	public void mousePressed(MouseEvent e) {
    	}
    	public void mouseReleased(MouseEvent e) {
    	}
        public void mouseClicked(MouseEvent e) {
        	if (e.getButton()==MouseEvent.BUTTON1) {
        		// left click
        		openBrowserURL();
        	}
        }
        public void openBrowserURL() {
        	String os = System.getProperty("os.name");
        	String cmd;
            if (os!=null && os.startsWith("Windows")) {
            	// On windows, there is a notion of default browser
            	// this will start IE or mozilla, as per default browser settings
            	cmd = "rundll32 url.dll,FileProtocolHandler "+url;
            } else {
            	// On Unix, Linux, there's no such thing as a default browser
            	// TODO: should we just call an arbitrary browser like this?
            	//cmd = "netscape -remote openURL(" + url + ")";
            	cmd = "firefox " + url;
            }
            try {
            	Runtime.getRuntime().exec(cmd);
            } catch(IOException x) {
            	// couldn't start browser
            }
        }
        
    }
}
