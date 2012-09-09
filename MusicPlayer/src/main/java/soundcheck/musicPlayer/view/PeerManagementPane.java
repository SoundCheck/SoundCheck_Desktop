package soundcheck.musicPlayer.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soundcheck.musicPlayer.ServiceCallback;
import soundcheck.shared.Const;
import soundcheck.shared.Const.Command;
import soundcheck.shared.Peer;
import soundcheck.shared.Util;
import soundcheck.shared.Zone;

class PeerManagementPane {
	final static Logger logger = LoggerFactory.getLogger(PeerManagementPane.class);

	PlayerGUI gui;
	private final ServiceCallback callback;

	private TreeDragAndDrop tree;

	PeerManagementPane(PlayerGUI gui, ServiceCallback callback) {
		this.gui = gui;
		this.callback = callback;
	}

	/**
	 * Remove current peers and replace with new list. Checks zones
	 * to see if they already exist. If they do, places peer under that zone.
	 * @param peerList
	 */
	void setPeers(List<Peer> peerList) {
		if( tree == null ) {
			logger.warn("Peer Management Pane has not been initialized but peer list is being set.");
			return;
		}

		tree.removeAllNodes();

		DefaultMutableTreeNode zoneNode;
		for( Peer peer : peerList ) {
			
			if( !peer.getStatus() && !peer.equals(callback.getLocalPeer())) {
				continue;
			}
			
			/* A node cannot be renamed without changing it's user model. Therefore,
			 * we must go through each peer and set it's zone object with the name
			 * of its zone according to the zone map. This will allow correct
			 * displaying of zone names.
			 */
			try {
				peer.getZone().setName( gui.getZoneMap().get(peer.getZone()).getZoneName() );
			} catch ( NullPointerException e) {
				logger.warn("Zone Map not loaded");
				peer.getZone().setName(peer.getName());
			}

			// Check if zone exists or not. Create new zone node if not
			zoneNode = tree.findZone(peer.getZone());
			if( zoneNode == null) {
				logger.trace("Adding new zone {}.", peer.getName());

				zoneNode = tree.addZone(peer.getZone(), false);
			}				

			// Add new peer to the zone
			tree.addPeer(zoneNode, peer, false);
		}

		((DefaultTreeModel)tree.getModel()).reload();
		tree.expandTree();

		/* If the current zone is no longer in the peer list, force
		 * it to be changed to a valid zone.
		 */
		boolean forceZoneSwitch = false;
		if( tree.findZone(gui.getCurrentZone()) == null ) {
			forceZoneSwitch = true;
		}

		// Load the GUI's current zone if it isn't set already (or is invalid)
		if( peerList.size() > 0) {
			gui.refreshCurrentZone(peerList.get(0).getZone(), forceZoneSwitch);
		}
		
		callback.notify(Command.LIBRARY_UPDATE, null, null);	// request updated library for to reflect peer changes
	}

	/**
	 * Updates the GUI to reflect the contents of the zoneMap
	 * @param zoneMap
	 */
	void refreshZones() {

		setPeers(tree.getPeers());

		// This code is faster, but testing shows it doesn't work. If we need
		// to, we could probably tweak this to correctly update the zones
		// on the tree display.

		/*	if( tree != null ) {
			List<DefaultMutableTreeNode> zoneNodes = tree.getZones();
			String newName;

			// Change the list of zone nodes to reflect the mappings in the zoneMap
			for( DefaultMutableTreeNode node : zoneNodes ) {
				newName = zoneMap.get(node.getUserObject());
				if( newName == null)
					logger.error("Peerlist contains zone not within zoneMap");
				((Zone)node.getUserObject()).setName(newName);
			}
			((DefaultTreeModel)tree.getModel()).reload();
			tree.expandTree();
		}*/
	}

	/**
	 * Create the panel that controls zones
	 * @return
	 */
	JPanel createPeerManagementPane() {
		JPanel peerPane = new JPanel();
		peerPane.setLayout(new BorderLayout());
		peerPane.setPreferredSize(new Dimension(75, 0));

		JPanel leftMenu = new JPanel() {
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

		leftMenu.setLayout(new BoxLayout(leftMenu, BoxLayout.X_AXIS));

		tree = new TreeDragAndDrop(gui, callback);

		final ImageIcon addZoneImg = new Util().createImageIcon("/images/addZone.png", "Add Zone");
		final ImageIcon addZonePressedImg = new Util().createImageIcon("/images/addZonePressed.png", "Add Zone Pressed");
		final ImageIcon addZoneHoverImg = new Util().createImageIcon("/images/addZoneHover.png", "Add Zone Hover");
		JButton addNodeButton = new JButton(addZoneImg);
		addNodeButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				String name = JOptionPane.showInputDialog("Name for new zone?");
				// User didn't hit cancel
				if( name != null ) {
					// Truncate name if too long
					if( name.length() > 20 ) {
						name = name.substring(0, 20);
					}
					Zone newZone = new Zone();
					newZone.setName(name);
					tree.addZone(newZone, true);
					/* Zone actually gets created after a peer is
					 * added to it in TreeDragAndDrop
					 */
				}
			}
		});
		addNodeButton.setPreferredSize(new Dimension(50,30));

		addNodeButton.setFocusPainted(false);
		addNodeButton.setMargin(new Insets(0,0,0,0));
		addNodeButton.setContentAreaFilled(false);
		addNodeButton.setBorderPainted(false);
		addNodeButton.setOpaque(false);
		addNodeButton.setPressedIcon(addZonePressedImg);
		addNodeButton.setRolloverIcon(addZoneHoverImg);

		JLabel header = new JLabel("Available Zones");
		header.setFont(new Font(Font.SANS_SERIF,Font.BOLD,14));
		leftMenu.add(Box.createRigidArea(new Dimension(5,5)));
		leftMenu.add(header);
		leftMenu.add(Box.createGlue());
		leftMenu.add(addNodeButton);
		leftMenu.add(Box.createRigidArea(new Dimension(5,5)));
		
		peerPane.add(leftMenu, BorderLayout.PAGE_START);
		peerPane.add( tree.getContent(), BorderLayout.CENTER );
		return peerPane;
	}
	
}
