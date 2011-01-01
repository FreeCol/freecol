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


package net.sf.freecol.client.gui.panel;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.resources.ResourceManager;

/**
 * This is the About panel 
 */
public final class AboutPanel extends FreeColPanel {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(AboutPanel.class.getName());

    /**
    * The constructor that will add the items to this panel.
    * 
    * @param parent The parent of this panel.
    */
    public AboutPanel(Canvas parent) {
        super(parent);
        
        setLayout(new BorderLayout());

        // Header with image
        JPanel header = new JPanel();
        this.add(header, BorderLayout.NORTH);
        Image tempImage = ResourceManager.getImage("TitleImage");
        if (tempImage != null) {
            JLabel logoLabel = new JLabel(new ImageIcon(tempImage));
            logoLabel.setBorder(new CompoundBorder(new EmptyBorder(2,2,2,2), new BevelBorder(BevelBorder.LOWERED)));
            header.add(logoLabel,JPanel.CENTER_ALIGNMENT);
        }
        
        // Info panel
        JPanel infoPanel = new JPanel(new GridLayout(3, 1));
        this.add(infoPanel,BorderLayout.CENTER);
        infoPanel.setBorder(new EmptyBorder(10, 10, 0, 10));
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
        textarea.setOpaque(false);
        textarea.setText(disclaimer);
        textarea.setLineWrap(true);
        textarea.setWrapStyleWord(true);
        textarea.setEditable(false);
        textarea.setFocusable(false);
        infoPanel.add(textarea);
        // copyright
        infoPanel.add(new JLabel(Messages.message("aboutPanel.copyright"),JLabel.CENTER),BorderLayout.CENTER);
        
        this.add(okButton, BorderLayout.SOUTH);

        setSize(getPreferredSize());
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
            String[] cmd = null;
            if (os==null) {
                // error, the operating system could not be determined
                return;
            } else if (os.toLowerCase().contains("mac")) {
                // Apple Macintosh, Safari is the main browser
                cmd = new String[] { "open" , "-a", "Safari", url };
            } else if (os.toLowerCase().contains("windows")) {
                // Microsoft Windows, use the default browser
                cmd = new String[] { "rundll32.exe", "url.dll,FileProtocolHandler", url};
            } else if (os.toLowerCase().contains("linux")) {
                // GNU Linux, use xdg-utils to launch the default browser (portland.freedesktop.org)
                cmd = new String[] {"xdg-open", url};
            } else {
                // Unix, ...
                // TODO: should we just call an arbitrary browser like this?
                //cmd = new String[] { "netscape",  "-remote", "openURL(" + url + ")"};
                cmd = new String[] { "firefox", url};
            }
            try {
                Runtime.getRuntime().exec(cmd);
            } catch(IOException x) {
                // couldn't start browser
            }
        }
    }
}
