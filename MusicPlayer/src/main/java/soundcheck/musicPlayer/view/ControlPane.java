package soundcheck.musicPlayer.view;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.jdesktop.swingx.JXCollapsiblePane;

import soundcheck.shared.Util;

public class ControlPane {

	PlayerGUI gui;
	ImageIcon infoIcon = new Util().createImageIcon("/images/textLogoSmall.png", "Logo");
	JLabel infoLabel = new JLabel();

	ControlPane(PlayerGUI gui) {
		this.gui = gui;
		infoLabel.setIcon(infoIcon);
	}

	public void infoStart() {
		infoLabel.setIcon(null);
	}
	
	public void infoUpdate(String info) {
		infoLabel.setText(info);
	}
	
	public void infoEnd() {
		infoLabel.setText("");
		infoLabel.setIcon(infoIcon);
	}

	/**
	 * Create control panel on top of window
	 * @return
	 */
	JPanel createControlPane() {
		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS)); 

		// Create button to open options menu
		JButton optionsButton = new JButton();
		optionsButton.setMinimumSize(new Dimension(30,30));
		optionsButton.setPreferredSize(new Dimension(30,30));
		optionsButton.setMaximumSize(new Dimension(30,30));
		optionsButton.setIcon(new Util().createImageIcon("/images/gear.png", "Options"));
		optionsButton.addActionListener(gui.collapsiblePane.getActionMap().get(
				JXCollapsiblePane.TOGGLE_ACTION));

		// Create Search bar
		final JTextField filterField = new JTextField(24);
		filterField.setMaximumSize(filterField.getPreferredSize());
		filterField.setFont(new Font(Font.SERIF,Font.ITALIC,12));
		filterField.setText(" Search");
		//Whenever filterText changes, invoke newFilter.
		filterField.getDocument().addDocumentListener(
				new DocumentListener() {
					public void changedUpdate(DocumentEvent e) {
						if( !filterField.getText().equals(" Search")) {
							gui.newFilter(filterField.getText());
						}
					}
					public void insertUpdate(DocumentEvent e) {
						if( !filterField.getText().equals(" Search")) {
							gui.newFilter(filterField.getText());
						}
					}
					public void removeUpdate(DocumentEvent e) {
						if( !filterField.getText().equals(" Search")) {
							gui.newFilter(filterField.getText());
						}
					}
				});

		filterField.addFocusListener(new FocusListener() {

			@Override
			public void focusGained(FocusEvent arg0) {
				if(filterField.getText().equals(" Search")) {
					filterField.setFont(new Font(Font.SERIF,Font.PLAIN,12));
					filterField.setText("");	
				}
			}

			@Override
			public void focusLost(FocusEvent arg0) {
				if(filterField.getText().equals("")) {
					filterField.setFont(new Font(Font.SERIF,Font.ITALIC,12));
					filterField.setText(" Search");
				}
			}

		});

		// Add components to top pane
		topPanel.add(Box.createRigidArea(new Dimension(5,40)));
		topPanel.add(optionsButton);
		topPanel.add(Box.createRigidArea(new Dimension(100,40)));
		topPanel.add(Box.createHorizontalGlue());
		topPanel.add(infoLabel);
		topPanel.add(Box.createHorizontalGlue());
		topPanel.add(filterField);
		topPanel.add(Box.createRigidArea(new Dimension(5,40)));

		return topPanel; 
	}

}
