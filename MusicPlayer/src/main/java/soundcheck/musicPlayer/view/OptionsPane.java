package soundcheck.musicPlayer.view;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.HashMap;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.jdesktop.swingx.JXCollapsiblePane;

import soundcheck.musicPlayer.ServiceCallback;
import soundcheck.shared.Const.Command;

/**
 * Represents an options pane that can be collapsed into the
 * side of the GUI.
 *
 */
class OptionsPane {

	PlayerGUI gui;
	ServiceCallback callback;

	final JTextField nameField = new JTextField(15);
	final JLabel libLocation = new JLabel();

	OptionsPane(PlayerGUI gui, ServiceCallback callback) {
		this.gui = gui;
		this.callback = callback;
	}

	/**
	 * Load starting values for options into pane
	 * @param configs
	 */
	void setInitialValues(HashMap<String,String> configs) {
		nameField.setText(configs.get("LocalName"));
		String libName = configs.get("Library");

		// Try to load up the song file
		File libFile = new File(libName);
		if( libFile.isDirectory() ) {
			libLocation.setText(libName);
			
			callback.notify(Command.SET_SONGLIST, libFile);
		} else {
			libLocation.setText("Not Found");
		}
	}

	/**
	 * Create the options menu. Due to the complexity of this element
	 * this method relies on many nested layout managers.
	 * @return
	 */
	JXCollapsiblePane createOptionsPane() {
		// Create collapsible pane
		JXCollapsiblePane collapsePane = new JXCollapsiblePane();
		collapsePane.setAnimated(true);
		collapsePane.setPreferredSize(new Dimension(300,0));
		collapsePane.setDirection(JXCollapsiblePane.Direction.RIGHT);
		collapsePane.setCollapsed(true);
		collapsePane.setLayout(new FlowLayout());

		// Collapse pane can't handle layout managers, so make a pane inside it
		JPanel optionsPane = new JPanel();
		optionsPane.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		collapsePane.add(optionsPane);
		
		JLabel welcome = new JLabel("SoundCheck Options");
		welcome.setFont(new Font(Font.SANS_SERIF,Font.BOLD,18));
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 2;
		c.insets = new Insets(10,0,10,0);
		optionsPane.add(welcome, c);
		
		JButton aboutBut = new JButton("About");
		aboutBut.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				AboutFrame about = new AboutFrame();
				about.setLocationRelativeTo(gui.songPane.table);
				about.setVisible(true);
				
			}
		});
		c.gridx = 2;
		c.gridy = 0;
		c.gridwidth = 1;
		c.insets = new Insets(10,0,10,0);
		optionsPane.add(aboutBut, c);

		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 1;
		c.insets = new Insets(10,0,10,0);
		optionsPane.add(new JLabel("Computer Name: "), c);
		
		c.gridx = 1;
		c.gridy = 1;
		c.gridwidth = 2;
		c.insets = new Insets(10,0,10,0);
		optionsPane.add(nameField, c);

		final JFileChooser libFileChoose = new SoundCheckFileChooser();
		libFileChoose.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		JButton libButton = new JButton("Choose Library");
		// Displays the current library location
		libLocation.setMaximumSize(new Dimension(290, 20));
		
		libButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				int fileReturn = libFileChoose.showOpenDialog(gui.frame);

				if (fileReturn == JFileChooser.APPROVE_OPTION) {
					File file = libFileChoose.getSelectedFile();
					libLocation.setText(file.getAbsolutePath());

					callback.notify(Command.SET_SONGLIST, file);
				}				
			}
		});

		c.gridx = 0;
		c.gridy = 2;
		c.insets = new Insets(10,0,10,0);
		c.gridwidth = 1;
		optionsPane.add(new JLabel("Library Location: "), c);
		
		c.gridx = 1;
		c.gridy = 2;
		c.insets = new Insets(10,0,10,0);
		c.gridwidth = 1;
		optionsPane.add(libButton, c);

		c.gridx = 0;
		c.gridy = 3;
		c.gridwidth = 3;
		c.anchor = GridBagConstraints.LINE_START;
		c.insets = new Insets(10,0,10,0);
		optionsPane.add(libLocation, c);
		
		// Option 3 - Online/Offline setting--------------------------	
		ButtonGroup statusButtonGroup = new ButtonGroup();
		
		JRadioButton onlineButton = new JRadioButton("Online", true);
		JRadioButton offlineButton = new JRadioButton("Offline", false);
		
		statusButtonGroup.add(onlineButton);
		statusButtonGroup.add(offlineButton);
		
		c.gridx = 0;
		c.gridy = 4;
		c.gridwidth = 1;
		c.insets = new Insets(10,0,10,0);
		optionsPane.add(new JLabel("Computer Status:"), c);
		
		c.gridx = 1;
		c.gridy = 4;
		c.gridwidth = 1;
		c.insets = new Insets(10,0,10,0);
		optionsPane.add(onlineButton, c);
		
		c.gridx = 2;
		c.gridy = 4;
		c.gridwidth = 1;
		c.insets = new Insets(10,0,10,0);
		optionsPane.add(offlineButton, c);
		
		onlineButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				callback.notify(Command.PEER_STATUS_CHANGE, true);
			}
		});
		
		offlineButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				callback.notify(Command.PEER_STATUS_CHANGE, false);
			}
		});
		
		JButton saveButton = new JButton("Save Settings");
		
		c.gridx = 1;
		c.gridy = 5;
		c.insets = new Insets(10,0,10,0);
		optionsPane.add(saveButton, c);
		
		final HashMap<String,String> configs = new HashMap<String,String>();
		saveButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				String comName = nameField.getText();
				String libName = libLocation.getText();

				configs.put("LocalName", comName);
				configs.put("Library", libName);

				callback.notify(Command.CONFIG, configs);
			}
		});

		return collapsePane;
	}

}
