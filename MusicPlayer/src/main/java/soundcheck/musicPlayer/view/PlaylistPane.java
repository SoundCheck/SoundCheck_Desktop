package soundcheck.musicPlayer.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.filechooser.FileFilter;

import soundcheck.musicPlayer.ServiceCallback;
import soundcheck.musicPlayer.songManagement.PlayListFileTransaction;
import soundcheck.shared.Const;
import soundcheck.shared.Const.Command;
import soundcheck.shared.Song;
import soundcheck.shared.Util;

class PlaylistPane {

	private PlayerGUI gui;
	private ServiceCallback callback;
	
	JToggleButton playPauseControl;

	private ListDragAndDrop playList;
	JLabel zoneLabel;

	PlaylistPane(PlayerGUI gui, ServiceCallback callback) {
		this.gui = gui;
		this.callback = callback;
	}

	final void setZone(String zone) {
		zoneLabel.setText(" " + zone);
	}

	/**
	 * Set the songs in the playlist
	 * @param songList
	 */
	public void setPlayList(List<Song> songList) {
		playList.setPlayList(songList);
	}

	/**
	 * Get the playlist currently displayed in this object,
	 * or null if the playlist doesn't exist.
	 * @return
	 */
	public Object[] getPlayList() {
		return playList.getPlayList();
	}
	
	/**
	 * Sets the play/pause button's icon to the pause symbol if true,
	 * and play if false.
	 * @param isPaused
	 */
	public void setIconPlay(boolean isPlaying) {
		playPauseControl.setSelected(isPlaying);
	}

	/**
	 * Create the right panel. The panel that controls playlists.
	 * @return
	 */
	JPanel createPlaylistPane() {
		final int SCROLL_SPEED = 10; // Pixels

		JPanel rightPanel = new JPanel();
		JPanel rightMenu = new JPanel() {
			private static final long serialVersionUID = 1L;
			private GradientPaint gradientPaint = new GradientPaint(0, 0, Const.mnuColor1, 0,
					60, Const.mnuColor2, true);
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				Graphics2D g2 = (Graphics2D) g;
				g2.setPaint(gradientPaint);
				g2.fillRect(0, 0, getWidth(), getHeight());
			}
		};

		final Image ctrlPic = new Util().createImage("/images/controlPic.png", "Control Picture");
		final Image ctrlBg = new Util().createImage("/images/controlBG.png", "Control Background");

		JPanel rightControlPanel = new JPanel(){
			private static final long serialVersionUID = 1L;

			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponents(g);
				g.drawImage(ctrlBg, 0, 0, getWidth(), getHeight(), this);
				g.drawImage(ctrlPic, 0, 0, ctrlPic.getWidth(this), getHeight(), this);

			}
		};
		rightControlPanel.setLayout(new BoxLayout(rightControlPanel,BoxLayout.X_AXIS));

		playList = new ListDragAndDrop(gui, callback);

		final ImageIcon playImg = new Util().createImageIcon("/images/play.png", "Play");
		final ImageIcon pauseImg = new Util().createImageIcon("/images/pause.png", "Pause");
		final ImageIcon playPressedImg = new Util().createImageIcon("/images/playPressed.png", "PlayPressed");
		final ImageIcon pausePressedImg = new Util().createImageIcon("/images/pausePressed.png", "PausePressed");
		final ImageIcon playHoverImg = new Util().createImageIcon("/images/playHover.png", "PlayHover");
		final ImageIcon pauseHoverImg = new Util().createImageIcon("/images/pauseHover.png", "PauseHover");
		
		playPauseControl = new JToggleButton(playImg);
		
		playPauseControl.setFocusPainted(false);
		playPauseControl.setMargin(new Insets(0,0,0,0));
		playPauseControl.setContentAreaFilled(false);
		playPauseControl.setBorderPainted(false);
		playPauseControl.setOpaque(false);
		playPauseControl.setRolloverIcon(playHoverImg);
		playPauseControl.setSelectedIcon(pauseImg);
		playPauseControl.setRolloverSelectedIcon(pauseHoverImg);
		
		playPauseControl.setPreferredSize(new Dimension( playImg.getIconWidth(), playImg.getIconHeight() ));
		
		playPauseControl.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				/* JToggleButtons only have one pressed state. So switch
				 * which icon is displayed depending on state
				 * before clicking. */
				if(playPauseControl.isSelected()) {
					playPauseControl.setPressedIcon(pausePressedImg);
				} else {
					playPauseControl.setPressedIcon(playPressedImg);
				}
			}
			
			@Override
			public void mouseClicked(MouseEvent arg0) {
				if( playPauseControl.isSelected() ) {

					// Ensure a song is in playlist. If playlist is empty, check if user
					// has a song selected in the song list. If yes, play that song.
					// TODO Add event to notify when addSelectedSongToQueue has successfully carried out to avoid the possibility that the play command will be received before the song is added to the queue.
					if(gui.getCurrentQueue().size() > 0 || gui.songPane.addSelectedSongToQueue()) {
						callback.notify(Command.PLAY, null, gui.getCurrentZone());
					}
				} else {
					callback.notify(Command.PAUSE, null, gui.getCurrentZone());
				}
			}
		});

		final ImageIcon stopImg = new Util().createImageIcon("/images/stop.png", "Stop");
		final ImageIcon stopPressedImg = new Util().createImageIcon("/images/stopPressed.png", "StopPressed");
		final ImageIcon stopHoverImg = new Util().createImageIcon("/images/stopHover.png", "StopHover");
		final JButton stopControl = new JButton(stopImg);
		stopControl.setPreferredSize(new Dimension( stopImg.getIconWidth(), stopImg.getIconHeight() ));
		
		stopControl.setFocusPainted(false);
		stopControl.setMargin(new Insets(0,0,0,0));
		stopControl.setContentAreaFilled(false);
		stopControl.setBorderPainted(false);
		stopControl.setOpaque(false);
		stopControl.setRolloverIcon(stopHoverImg);
		stopControl.setPressedIcon(stopPressedImg);

		stopControl.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				callback.notify(Command.TEARDOWN, null, gui.getCurrentZone());
			}
		});

		final ImageIcon nextImg = new Util().createImageIcon("/images/next.png", "Next");
		final ImageIcon nextPressedImg = new Util().createImageIcon("/images/nextPressed.png", "NextPressed");
		final ImageIcon nextHoverImg = new Util().createImageIcon("/images/nextHover.png", "nextHover");
		final JButton nextControl = new JButton(nextImg);
		nextControl.setPreferredSize(new Dimension( nextImg.getIconWidth(), nextImg.getIconHeight() ));
		
		nextControl.setFocusPainted(false);
		nextControl.setMargin(new Insets(0,0,0,0));
		nextControl.setContentAreaFilled(false);
		nextControl.setBorderPainted(false);
		nextControl.setOpaque(false);
		nextControl.setRolloverIcon(nextHoverImg);
		nextControl.setPressedIcon(nextPressedImg);
		
		nextControl.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				callback.notify(Command.NEXT, null, gui.getCurrentZone());
			}
		});

		rightControlPanel.setLayout(new FlowLayout());
		rightControlPanel.add(playPauseControl);
		rightControlPanel.add(Box.createRigidArea(new Dimension(5,67)));
		rightControlPanel.add(stopControl);
		rightControlPanel.add(Box.createRigidArea(new Dimension(5,67)));
		rightControlPanel.add(nextControl);

		// Create menu bar------------------------------------------------------------------------

		// Create zone tag
		JLabel zoneTag = new JLabel(" Current Zone");
		zoneTag.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));

		// Create current zone label
		zoneLabel = new JLabel();
		zoneLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD + Font.ITALIC, 14));
		zoneLabel.setText(" None Selected");

		final JFileChooser playlistChooser = new SoundCheckFileChooser();
		playlistChooser.setFileFilter(new PlaylistFileFilter());

		// Save playlist dialog-------------
		final ImageIcon saveImg = new Util().createImageIcon("/images/save.png", "Save");
		final ImageIcon savePressedImg = new Util().createImageIcon("/images/savePressed.png", "SavePressed");
		final ImageIcon saveHoverImg = new Util().createImageIcon("/images/saveHover.png", "SaveHover");
		final JButton savePlaylist = new JButton(saveImg);
		savePlaylist.setPressedIcon(savePressedImg);
		savePlaylist.setRolloverIcon(saveHoverImg);
		savePlaylist.setFocusPainted(false);
		savePlaylist.setMargin(new Insets(0,0,0,0));
		savePlaylist.setContentAreaFilled(false);
		savePlaylist.setBorderPainted(false);
		savePlaylist.setOpaque(false);
		savePlaylist.setPreferredSize(new Dimension( saveImg.getIconWidth(), saveImg.getIconHeight() ));
		savePlaylist.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent arg0) {

				List<Song> songList = new ArrayList<Song>();
				for( Object o : getPlayList() ) {
					songList.add( (Song)o );
				}

				if( !songList.isEmpty() ) {
					boolean validFileChosen = true;
					File saveFile = null;

					do {
						int fileReturn = playlistChooser.showSaveDialog(gui.frame);

						if (fileReturn == JFileChooser.APPROVE_OPTION) {
							saveFile = playlistChooser.getSelectedFile();

							// Ensure save file is *.cejj
							String filePath = saveFile.getPath();
							if(!filePath.toLowerCase().endsWith(".cejj"))
							{
								saveFile = new File(filePath + ".cejj");
							}

							// Check if file already exists
							if( saveFile.exists() ) {
								int confirm = JOptionPane.showConfirmDialog(
										playlistChooser,
										"Overwrite file? " + saveFile.getName(),
										"Overwrite?",
										JOptionPane.YES_NO_OPTION);
								if (confirm == JOptionPane.YES_OPTION) {
									validFileChosen = true;
								}
								else if (confirm == JOptionPane.NO_OPTION) {
									validFileChosen = false;
									saveFile = null;
								}
							} else {
								validFileChosen = true;
							}
						}
					} while( validFileChosen == false );
					if( saveFile != null ) {
						PlayListFileTransaction.savePlayList(songList, saveFile);
					}
				}
			}
		});

		// Load playlist dialog-------------
		final ImageIcon loadImg = new Util().createImageIcon("/images/open.png", "Open");
		final ImageIcon loadPressedImg = new Util().createImageIcon("/images/openPressed.png", "OpenPressed");
		final ImageIcon loadHoverImg = new Util().createImageIcon("/images/openHover.png", "OpenHover");
		final JButton loadPlaylist = new JButton(loadImg);
		loadPlaylist.setPressedIcon(loadPressedImg);
		loadPlaylist.setRolloverIcon(loadHoverImg);
		loadPlaylist.setFocusPainted(false);
		loadPlaylist.setMargin(new Insets(0,0,0,0));
		loadPlaylist.setContentAreaFilled(false);
		loadPlaylist.setBorderPainted(false);
		loadPlaylist.setOpaque(false);
		loadPlaylist.setPreferredSize(new Dimension( loadImg.getIconWidth(), loadImg.getIconHeight() ));
		loadPlaylist.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				int fileReturn = playlistChooser.showOpenDialog(gui.frame);

				if (fileReturn == JFileChooser.APPROVE_OPTION) {
					File openFile = playlistChooser.getSelectedFile();
					List<Song> playList = PlayListFileTransaction.loadPlayList(openFile);
					callback.notify(Command.NEW_QUEUE, playList, gui.getCurrentZone());
				}
			}
		});

		rightMenu.setLayout(new GridBagLayout());
		GridBagConstraints gridCon = new GridBagConstraints();

		gridCon.gridx = 0;
		gridCon.gridy = 0;
		gridCon.anchor = GridBagConstraints.FIRST_LINE_START;
		rightMenu.add(zoneTag, gridCon);

		gridCon.fill = GridBagConstraints.BOTH;
		gridCon.weighty = 0.7;
		gridCon.weightx = 0.5;
		gridCon.gridx = 0;
		gridCon.gridy = 1;
		gridCon.anchor = GridBagConstraints.LINE_START;
		rightMenu.add(zoneLabel, gridCon);

		gridCon.weightx = 0.1;
		gridCon.gridheight = 2;
		gridCon.gridx = 1;
		gridCon.gridy = 0;
		rightMenu.add(savePlaylist, gridCon);

		gridCon.weightx = 0.1;
		gridCon.gridheight = 2;
		gridCon.gridx = 2;
		gridCon.gridy = 0;
		gridCon.insets = new Insets(0,1,0,0);
		rightMenu.add(loadPlaylist, gridCon);

		rightPanel.setLayout(new BorderLayout());

		// Have playlist hug the bottom of the pane above the controls
		JPanel playListPane = new JPanel();
		playListPane.setLayout(new BorderLayout());
		playListPane.setBackground(Const.bgColor);

		playListPane.add( playList.getContent(), BorderLayout.PAGE_END);

		// Create Scroll pane for playlist
		JScrollPane playListScroll = new JScrollPane(playListPane);
		playListScroll.setHorizontalScrollBarPolicy(
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
		playListScroll.setBorder(null);
		playListScroll.getVerticalScrollBar().setUnitIncrement(SCROLL_SPEED);


		rightPanel.add(rightMenu, BorderLayout.PAGE_START);
		rightPanel.add(playListScroll, BorderLayout.CENTER);
		rightPanel.add(rightControlPanel, BorderLayout.PAGE_END);

		return rightPanel;
	}

	class PlaylistFileFilter extends FileFilter {

		@Override
		public boolean accept(File f) {
			if (f.isDirectory()) {
				return true;
			}

			String extension = getExtension(f);
			if (extension != null) {
				if ( extension.equals("cejj") ||
						extension.equals("m3u") ) {
					return true;
				} else {
					return false;
				}
			}

			return false;
		}

		@Override
		public String getDescription() {
			return "Valid Playlist Formats (*.cejj, *.m3u)";
		}

		/*
		 * Get the extension of a file.
		 */  
		public String getExtension(File f) {
			String ext = null;
			String s = f.getName();
			int i = s.lastIndexOf('.');

			if (i > 0 &&  i < s.length() - 1) {
				ext = s.substring(i+1).toLowerCase();
			}
			return ext;
		}
	}
}