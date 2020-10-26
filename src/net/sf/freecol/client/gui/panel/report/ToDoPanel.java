package net.sf.freecol.client.gui.panel.report;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.client.gui.panel.Utility;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;

public class ToDoPanel extends ReportPanel {
	
	//protected final JPanel ToDoPanel;
	
	static JFrame f;
	static JTextField t;
	static JLabel l;
	
	 public ToDoPanel(FreeColClient freeColClient) {
	        super(freeColClient, "ToDoAction");
	        JLabel label = Utility.localizedLabel("ToDo");
	        add(label, BorderLayout.PAGE_START);
	        label.setFocusable(false);

	        this.t = new JTextField("", 400);
	        //this.t.setActionCommand(String.valueOf(ToDo));
	        //this.t.addActionListener(this);
	        add(this.t, BorderLayout.CENTER);
	        //this.field.setFocusable(true);

	        setSize(getPreferredSize());
	        
	       
	        
			/*
			 * f = new JFrame("ToDo"); t = new JTextField(400); JPanel p = new JPanel();
			 * p.add(t); p.add(l); f.add(p); f.setSize(300,300); f.show();
			 */
	 }
	 
	 public void actionPerformed(ActionEvent e)
	 {
		 String s = e.getActionCommand();
		 l.setText(t.getText());
		 //t.setText(" ");
	 }
}
