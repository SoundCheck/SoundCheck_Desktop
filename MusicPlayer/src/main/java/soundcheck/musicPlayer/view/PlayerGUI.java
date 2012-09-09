package soundcheck.musicPlayer.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.jdesktop.swingx.JXCollapsiblePane;
import org.jdesktop.swingx.JXTable;
import org.pushingpixels.substance.api.skin.SubstanceRavenLookAndFeel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soundcheck.musicPlayer.ServiceCallback;
import soundcheck.shared.Const;
import soundcheck.shared.Peer;
import soundcheck.shared.Song;
import soundcheck.shared.Util;
import soundcheck.shared.Zone;
import soundcheck.shared.ZoneProperties;

/**
 * Creates the GUI by using methods to create each of the 5 quadrants in border layout.
 * Listeners are created inline for the individual elements.
 *
 */
public final class PlayerGUI {
	final static Logger logger = LoggerFactory.getLogger(PlayerGUI.class);

	List<Song> sl = new ArrayList<Song>();

	JFrame frame;
	private final OptionsPane optionsPane;
	private final PeerManagementPane peerPane;
	final SongPane songPane;
	private final PlaylistPane playPane;
	public final ControlPane controlPane;
	final InfoPane infoPane;

	JXCollapsiblePane collapsiblePane;

	// The zone that is being modified by user actions on the GUI
	private volatile Zone currentZone;

	private volatile Map<Zone, ZoneProperties> zoneMap;

	/**
	 * Create the application.
	 */
	public PlayerGUI(ServiceCallback callback) {
		// Create nice looking window
		JFrame.setDefaultLookAndFeelDecorated(true);
		try {
			UIManager.setLookAndFeel( new SubstanceRavenLookAndFeel());
		} catch (UnsupportedLookAndFeelException e) {
			logger.error("",e);
		}

		// Create GUI components
		optionsPane = new OptionsPane(this, callback);
		peerPane = new PeerManagementPane(this, callback);
		songPane = new SongPane(this, callback);
		playPane = new PlaylistPane(this, callback);
		controlPane = new ControlPane(this);
		infoPane = new InfoPane(this);

		initialize();
	}

	/**
	 * Change which zone is being modified by user actions
	 * @param zone
	 */
	void setCurrentZone(Zone zone) {
		// If zone is empty, don't allow switching to it.
		if( zoneMap.containsKey(zone) ) {

			currentZone = zone;

			/* The zone's name is set before it is added to
			 * the JTree, so we know this will be correct
			 * if the zone is added from the JTree */
			playPane.setZone(currentZone.toString());

			playPane.setPlayList(zoneMap.get(currentZone).getPlayList());
		}
	}

	/**
	 * Set the progress bar to the specified time
	 * @param timeStamp
	 */
	public void updateProgressBar(long timeStamp) {
		infoPane.setProgressPosition(timeStamp);
	}
	
	public void isStreaming(boolean isPlaying) {
		playPane.setIconPlay(isPlaying);
	}

	/**
	 * Get the zone that is being modified by user actions
	 * @return
	 */
	public Zone getCurrentZone() {
		return currentZone;
	}

	/**
	 * Get the queue currently displayed in the GUI. If no queue
	 * exists, return a list of size 0.
	 * @return
	 */
	List<Song> getCurrentQueue() {
		if( zoneMap != null && currentZone != null ) {
			return zoneMap.get(currentZone).getPlayList();
		} else {
			return new ArrayList<Song>();
		}
	}

	/**
	 * Sets the current zone to the given zone only if
	 * the current zone is not valid as is.
	 */
	void refreshCurrentZone(Zone zone, boolean force) {
		// Ensure argument zone is valid
		if( zone == null) {
			return;
		}

		// Check if current zone is valid
		if( (currentZone == null 
				|| zoneMap.get(currentZone) == null)
				|| force == true ) {
			setCurrentZone(zone);
		}
	}

	/**
	 * Set the values of the config pane to the values in the hash map
	 * @param configs
	 */
	public void setInitConfig(HashMap<String,String> configs) {
		optionsPane.setInitialValues(configs);
	}

	/**
	 * Set the visibility of the GUI
	 * @param b True if the GUI should be visible
	 */
	public void setVisible(boolean b) {
		frame.setVisible(b);
	}

	/**
	 * Apply update to peer management pane.
	 * @param peerList
	 */
	public void updatePeerList(List<Peer> peerList) {
		logger.trace("Updating peerlist");
		peerPane.setPeers(peerList);
	}

	/**
	 * Apply peer song update to song pane.
	 * @param peerList
	 */
	public void updateSongList(List<Song> songList) {
		logger.trace("Updating songlist");
		songPane.loadSongList( songList );
	}

	/**
	 * Apply new zone filter to peer management pane
	 * @param zoneMap
	 */
	public void updateZoneMap(Map<Zone,ZoneProperties> zoneMap) {
		this.zoneMap = zoneMap;
		peerPane.refreshZones();
		playPane.setPlayList(zoneMap.get(currentZone).getPlayList());
	}

	public Map<Zone, ZoneProperties> getZoneMap() {
		return zoneMap;
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		// Create the overall frame
		frame = new JFrame();

		// Revert to system look and feel for specific components, since I don't have enough time to
		// learn how to modify my own look and feels and the L&F I'm using overrides custom settings.
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException e) {
			logger.error("",e);
		} catch (InstantiationException e) {
			logger.error("",e);
		} catch (IllegalAccessException e) {
			logger.error("",e);
		} catch (UnsupportedLookAndFeelException e) {
			logger.error("",e);
		}

		frame.setBackground(Const.bgColor);

		frame.setTitle(Const.PROJECT_NAME);

		frame.setIconImage( new Util().createImageIcon("/images/soundCheckLogoSmall.png", "SoundCheck Icon").getImage());
		frame.setBounds(100, 100, 1280, 720);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout(5, 0));
		frame.setResizable(true);

		// Create the left pane. This pane contains zone information
		JPanel leftPanel = peerPane.createPeerManagementPane();

		// Create the middle pane. This is where song information is stored.
		JXTable songTable = songPane.createSongPane();

		// Create right pane. This controls the playlist
		JPanel rightPanel = playPane.createPlaylistPane();
		rightPanel.setPreferredSize(new Dimension(150, 0));

		// Revert back to nice looking L&F for the remainder of the options
		try {
			UIManager.setLookAndFeel(new SubstanceRavenLookAndFeel());
		} catch (UnsupportedLookAndFeelException e) {
			logger.error("",e);
		}

		// Create the collapsible left pane for the options menu.
		collapsiblePane = optionsPane.createOptionsPane();

		// Add scroll panes
		JScrollPane songScrollPane = new JScrollPane(songTable);

		JScrollPane rightScrollPane = new JScrollPane(rightPanel);
		rightScrollPane.setMinimumSize(new Dimension(210,130));
		rightScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		// Create top pane. This pane contains the search bar
		JPanel topPanel = controlPane.createControlPane();

		// Create the bottom pane. This contains information on the currently playing song
		JPanel bottomPanel = infoPane.createInfoPane();

		// Build the splitpanes to allow resizing of panes
		JSplitPane leftSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, songScrollPane );
		JSplitPane rightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplit, rightScrollPane);

		leftSplit.setResizeWeight(.2);
		rightSplit.setResizeWeight(.9);

		leftSplit.setContinuousLayout(true);
		rightSplit.setContinuousLayout(true);

		leftSplit.setDividerSize(5);
		rightSplit.setDividerSize(5);

		// Add all elements to the frame
		frame.getContentPane().add(topPanel, BorderLayout.PAGE_START);
		frame.getContentPane().add(collapsiblePane, BorderLayout.LINE_START);
		frame.getContentPane().add(rightSplit, BorderLayout.CENTER);
		frame.getContentPane().add(bottomPanel, BorderLayout.PAGE_END);
	}

	/** 
	 * The search box has changed. Update the filter
	 * for the song list.
	 */
	void newFilter(String filterText) {
		songPane.newFilter(filterText);
	}
}
