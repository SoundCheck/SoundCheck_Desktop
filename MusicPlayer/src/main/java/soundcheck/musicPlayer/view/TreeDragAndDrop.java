package soundcheck.musicPlayer.view;

import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.DropMode;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soundcheck.musicPlayer.ServiceCallback;
import soundcheck.shared.Const;
import soundcheck.shared.Const.Command;
import soundcheck.shared.Peer;
import soundcheck.shared.Util;
import soundcheck.shared.Zone;
import soundcheck.shared.ZoneProperties;

/**
 * Extends upon JTree by allowing for nodes to be drag and drop.
 *
 */
public class TreeDragAndDrop extends JTree {
	private static final long serialVersionUID = 1L;
	final static Logger logger = LoggerFactory.getLogger(TreeDragAndDrop.class);

	private ServiceCallback callback;

	PlayerGUI gui;

	JTree tree;
	DefaultMutableTreeNode rootNode;

	final ImageIcon zoneImg = new Util().createImageIcon("/images/zoneIcon.png", "Zone Icon");
	final ImageIcon comImg = new Util().createImageIcon("/images/ComputerIcon.png", "Com Icon");
	final ImageIcon extImg = new Util().createImageIcon("/images/externalIcon.png", "External Icon");

	TreeDragAndDrop(PlayerGUI gui, ServiceCallback callback) {

		this.gui = gui;
		this.callback = callback;
	}

	/**
	 * Sets up the JTree.
	 * @return
	 */
	JScrollPane getContent() {
		rootNode = new DefaultMutableTreeNode("Root Node");
		treeModel = new DefaultTreeModel(rootNode);
		treeModel.addTreeModelListener(new SoundCheckTreeModelListener());

		tree = new JTree(treeModel);
		tree.setBackground(Const.bgColor);
		tree.setDragEnabled(true);
		tree.setDropMode(DropMode.ON_OR_INSERT);
		tree.setRootVisible(false);
		tree.setEditable(false);
		tree.setCellRenderer(new SoundCheckTreeCellRenderer());

		// Keep user from collapsing tree
		tree.addTreeWillExpandListener
		(new TreeWillExpandListener() {
			public void treeWillExpand(TreeExpansionEvent e) { }
			public void treeWillCollapse(TreeExpansionEvent e)
					throws ExpandVetoException {
				throw new ExpandVetoException(e, "you can't collapse this JTree");
			}
		});

		// Add drag and drop support
		tree.setTransferHandler(new TreeTransferHandler(callback, gui));

		// Any arrangement of rows can be selected at once
		tree.getSelectionModel().setSelectionMode(
				TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

		// Tree will be collapsed by default
		expandTree();

		// Add mouse listener for detecting if user selects zone
		tree.addMouseListener(new MouseAdapter() {
			private ZonePopUpMenu popup = new ZonePopUpMenu();

			public void mouseClicked(MouseEvent e) {
				int selRow = tree.getRowForLocation(e.getX(), e.getY());
				TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
				if(selRow != -1) {
					DefaultMutableTreeNode selNode = (DefaultMutableTreeNode) selPath.getLastPathComponent();
					if( selNode.getLevel() == 1) {
						Zone zone = (Zone) selNode.getUserObject();
						
						String correctPass = gui.getZoneMap().get(zone).getPassword();
								
						if( correctPass == null) {	// no password set on zone
							gui.setCurrentZone( zone );
						} else {
							String password = JOptionPane.showInputDialog("Enter Password: ");

							if( password.equals(correctPass)) {
								gui.setCurrentZone( zone );
							} else {
								JOptionPane.showMessageDialog(null, "Incorrect Password");
							}
						}
					}

				}

			}

			/**
			 * Display pop up menu for zones
			 */
			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger()) {

					int selRow = tree.getRowForLocation(e.getX(), e.getY());
					TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());

					// Only show pup up menu if mouse is over a zone
					if( selRow != -1 ) {
						DefaultMutableTreeNode selNode = (DefaultMutableTreeNode) selPath.getLastPathComponent();
						if( selNode.getChildCount() == 1 ) {
							DefaultMutableTreeNode child = (DefaultMutableTreeNode) selNode.getChildAt(0);
							Peer peer = (Peer) child.getUserObject();
							if( peer.equals(callback.getLocalPeer())) {
								popup.show(e.getComponent(), e.getX(), e.getY());
								popup.setSelectedZone( (Zone) selNode.getUserObject() );
							}
						}
					}
				}
			}
		});

		return new JScrollPane(tree);
	}

	/**
	 * Expand all the nodes to show children.
	 * @param tree
	 */
	void expandTree() {
		DefaultMutableTreeNode root =
				(DefaultMutableTreeNode)tree.getModel().getRoot();
		Enumeration<?> e = root.breadthFirstEnumeration();
		while(e.hasMoreElements()) {
			DefaultMutableTreeNode node =
					(DefaultMutableTreeNode)e.nextElement();
			if(node.isLeaf()) continue;
			int row = tree.getRowForPath(new TreePath(node.getPath()));
			tree.expandRow(row);
		}
	}

	/**
	 * Add new node as a child of the root node
	 * @param child The node to add
	 * @return
	 */
	public DefaultMutableTreeNode addZone(Zone zone, boolean shouldBeVisible) {

		DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(zone);
		((DefaultTreeModel) treeModel).insertNodeInto(childNode, rootNode, rootNode.getChildCount());

		// Make sure the user can see the new node.
		if (shouldBeVisible) {
			tree.scrollPathToVisible(new TreePath(childNode.getPath()));
		}

		return childNode;
	}

	/**
	 * Add new node to the tree
	 * 
	 * @param parent The node to add to
	 * @param child The node to add
	 * @param shouldBeVisible If view should scroll to reveal new node
	 * @return
	 */
	public DefaultMutableTreeNode addPeer(DefaultMutableTreeNode zone,
			Peer peer, boolean shouldBeVisible) {

		DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(peer);
		((DefaultTreeModel) treeModel).insertNodeInto(childNode, zone, zone.getChildCount());

		// Make sure the user can see the new node.
		if (shouldBeVisible) {
			tree.scrollPathToVisible(new TreePath(childNode.getPath()));
		}

		return childNode;
	}

	/**
	 * Removes the node from the specified path from the tree
	 * @param path The path of the node to remove
	 * @return
	 */
	public void removeNode(TreePath path) {
		DefaultMutableTreeNode deletionNode =
				(DefaultMutableTreeNode)path.getLastPathComponent();

		((DefaultTreeModel) treeModel).removeNodeFromParent(deletionNode);	  
	}

	/**
	 * Removes a node if it has no children and is a first level node (a SoundCheck zone)
	 * @param path
	 */
	public void removeZone(TreePath path) {
		try {
			DefaultMutableTreeNode deletionNode =
					(DefaultMutableTreeNode)path.getLastPathComponent();

			if( deletionNode.getChildCount() == 0 && deletionNode.getLevel() != 2) {
				removeNode(path);
			}

		} catch(NullPointerException e) {
			// Node was not selected. Ignore
		}
	}

	/**
	 * Check's if a peer exists in the tree. Otherwise returns null.
	 * @param uid A peer's UID string
	 * @return The node representing the peer
	 */
	public DefaultMutableTreeNode findPeer(String uid) {
		// The enumeration returns all the nodes
		@SuppressWarnings("unchecked")
		Enumeration<DefaultMutableTreeNode> nodes = ((DefaultMutableTreeNode) treeModel.getRoot()).breadthFirstEnumeration();

		// Iterate through tree and check if any node's UID is equal to user input
		while (nodes.hasMoreElements()) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes.nextElement();
			if ( uid.equals( ( (Peer)node.getUserObject() ).getUid() ) ) {
				return node;
			}
		}

		return null;
	}

	/**
	 * Check's if a zone exists in the tree. Otherwise returns null.
	 * @param uid A zone's UUID
	 * @return The node representing the zone
	 */
	public DefaultMutableTreeNode findZone(Zone zone) {
		// The enumeration returns all the nodes
		@SuppressWarnings("unchecked")
		Enumeration<DefaultMutableTreeNode> nodes = ((DefaultMutableTreeNode) treeModel.getRoot()).breadthFirstEnumeration();

		// Iterate through tree and check if any node's zones is equal to user input
		DefaultMutableTreeNode node;
		while (nodes.hasMoreElements()) {
			node = (DefaultMutableTreeNode) nodes.nextElement();
			if( node.getUserObject() instanceof Zone) {
				try {
					if ( zone.equals( ( (Zone)node.getUserObject() ) ) ) {
						return node;
					}
				} catch (NullPointerException e) {
					//Zone does not exist. Pass through to return null.
				}
			}
		}

		return null;
	}

	/**
	 * Returns all the nodes that represent peers in the tree
	 * @param uid A zone's UUID
	 * @return The node representing the zone
	 */
	public List<Peer> getPeers() {
		// The enumeration returns all the nodes
		@SuppressWarnings("unchecked")
		Enumeration<DefaultMutableTreeNode> nodes = ((DefaultMutableTreeNode) treeModel.getRoot()).breadthFirstEnumeration();

		List<Peer> zoneList = new ArrayList<Peer>();
		DefaultMutableTreeNode node;

		// Iterate through tree and check for nodes that are zones
		while (nodes.hasMoreElements()) {
			node = (DefaultMutableTreeNode) nodes.nextElement();
			if( node.getUserObject() instanceof Peer) {
				zoneList.add((Peer)node.getUserObject());
			}
		}

		return zoneList;
	}

	/**
	 * Returns all the nodes that represent zones in the tree
	 * @param uid A zone's UUID
	 * @return The node representing the zone
	 */
	public List<DefaultMutableTreeNode> getZones() {
		// The enumeration returns all the nodes
		@SuppressWarnings("unchecked")
		Enumeration<DefaultMutableTreeNode> nodes = ((DefaultMutableTreeNode) treeModel.getRoot()).breadthFirstEnumeration();

		List<DefaultMutableTreeNode> zoneList = new ArrayList<DefaultMutableTreeNode>();
		DefaultMutableTreeNode node;

		// Iterate through tree and check for nodes that are zones
		while (nodes.hasMoreElements()) {
			node = (DefaultMutableTreeNode) nodes.nextElement();
			if( node.getUserObject() instanceof Zone) {
				zoneList.add(node);
			}
		}

		return zoneList;
	}

	/**
	 * Check's if a zone exists in the tree. Otherwise returns null.
	 * @param uid A zone's UUID
	 * @return The node representing the zone
	 */
	public void removeAllNodes() {
		logger.trace("Removing current tree nodes.");
		Object root = treeModel.getRoot();
		while(!treeModel.isLeaf(root)) {
			((DefaultTreeModel) treeModel).removeNodeFromParent((DefaultMutableTreeNode)treeModel.getChild(root,0));
		}
	}

	class ZonePopUpMenu extends JPopupMenu {
		private static final long serialVersionUID = 1L;

		JMenuItem secure;

		private Zone selectedZone = null;

		public ZonePopUpMenu(){
			secure = new JMenuItem("Add Password");
			secure.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {

					if( selectedZone != null) {
						String password = JOptionPane.showInputDialog("Enter Password:");
						callback.notify(Command.PASSWORD_SET, password, selectedZone);
					}

				}
			});
			add(secure);
		}

		public void setSelectedZone( Zone selectedZone ) {
			this.selectedZone = selectedZone;
		}
	}

	class SoundCheckTreeCellRenderer extends DefaultTreeCellRenderer {
		private static final long serialVersionUID = 1L;

		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value,
				boolean sel, boolean expanded, boolean leaf, int row,
				boolean hasFocus) {

			super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf,
					row, hasFocus);

			setBackgroundSelectionColor(Const.highlightColor);
			setBackgroundNonSelectionColor(Const.bgColor);
			setTextSelectionColor(Const.bgColor);
			setTextNonSelectionColor(Const.txtColor);
			setBackground(null);
			setOpaque(false);

			if( ((DefaultMutableTreeNode)value).getLevel() == 1 ) {
				setIcon(zoneImg);
			} else if (((DefaultMutableTreeNode)value).getUserObject() instanceof Peer) {
				if( ( (Peer)( (DefaultMutableTreeNode)value ).getUserObject() ).isExternal() ) {
					setIcon(extImg);
				} else {
					setIcon(comImg);
				}
			}


			return this;
		}
	}
}

/**
 * Handle the transfer of a Transferable within a JTree
 *
 */
class TreeTransferHandler extends TransferHandler {
	private static final long serialVersionUID = 1L;

	ServiceCallback callback;
	PlayerGUI gui;

	private DataFlavor nodesFlavor;
	private final DataFlavor[] flavors = new DataFlavor[1];
	private DefaultMutableTreeNode[] nodesToRemove;

	/**
	 * Constructor.
	 */
	public TreeTransferHandler(ServiceCallback callback, PlayerGUI gui) {
		this.callback = callback;
		this.gui = gui;
		try {
			String mimeType = DataFlavor.javaJVMLocalObjectMimeType +
					";class=\"" +
					javax.swing.tree.DefaultMutableTreeNode[].class.getName() +
					"\"";
			nodesFlavor = new DataFlavor(mimeType);
			flavors[0] = nodesFlavor;
		} catch(ClassNotFoundException e) {
			System.out.println("ClassNotFound: " + e.getMessage());
		}
	}

	/**
	 * Called repeatedly during drag and drop operation to determine acceptability
	 * of transfers.
	 */
	@Override
	public boolean canImport(TransferHandler.TransferSupport support) {
		if(!support.isDrop()) {
			return false;
		}
		support.setShowDropLocation(true);
		if(!support.isDataFlavorSupported(nodesFlavor)) {
			return false;
		}

		// Do not allow a drop on the drag source selections.
		JTree.DropLocation dl =
				(JTree.DropLocation)support.getDropLocation();
		JTree tree = (JTree)support.getComponent();
		int dropRow = tree.getRowForPath(dl.getPath());
		int[] selRows = tree.getSelectionRows();
		for(int i = 0; i < selRows.length; i++) {
			if(selRows[i] == dropRow) {
				return false;
			}
		}
		// Do not allow a non-leaf node to be copied to a level
		// which is less than its source level.
		TreePath dest = dl.getPath();
		DefaultMutableTreeNode target =
				(DefaultMutableTreeNode)dest.getLastPathComponent();
		TreePath path = tree.getPathForRow(selRows[0]);
		DefaultMutableTreeNode firstNode =
				(DefaultMutableTreeNode)path.getLastPathComponent();
		if(firstNode.getChildCount() > 0 &&
				target.getLevel() < firstNode.getLevel()) {
			return false;
		}
		// Do not allow leaf nodes to be copied to a level
		// different than what they are on
		if(target.getLevel() != firstNode.getLevel()-1) {
			return false;
		}
		// Do not allow MOVE-action drops if a non-leaf node is
		// selected unless all of its children are also selected.
		int action = support.getDropAction();
		if(action == MOVE) {
			return haveCompleteNode(tree);
		}
		return true;
	}

	/**
	 * If a parent node is selected, check that all of it's
	 * children are selected with it.
	 * @param tree
	 * @return true if the entire node is selected
	 */
	private boolean haveCompleteNode(JTree tree) {
		int[] selRows = tree.getSelectionRows();
		TreePath path = tree.getPathForRow(selRows[0]);
		DefaultMutableTreeNode first =
				(DefaultMutableTreeNode)path.getLastPathComponent();
		int childCount = first.getChildCount();
		// first has children and no children are selected.
		if(childCount > 0 && selRows.length == 1)
			return false;
		// first may have children.
		for(int i = 1; i < selRows.length; i++) {
			path = tree.getPathForRow(selRows[i]);
			DefaultMutableTreeNode next =
					(DefaultMutableTreeNode)path.getLastPathComponent();
			if(first.isNodeChild(next)) {
				// Found a child of first.
				if(childCount > selRows.length-1) {
					// Not all children of first are selected.
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Creates a Transferable to use as the source for a data transfer.
	 * Returns the representation of the data to be transferred, or
	 * null if the component's property is null 
	 */
	@Override
	protected Transferable createTransferable(JComponent c) {
		JTree tree = (JTree)c;
		TreePath[] paths = tree.getSelectionPaths();
		if(paths != null) {
			// Make up a node array of copies for transfer and
			// another for/of the nodes that will be removed in
			// exportDone after a successful drop.
			List<DefaultMutableTreeNode> copies =
					new ArrayList<DefaultMutableTreeNode>();
			List<DefaultMutableTreeNode> toRemove =
					new ArrayList<DefaultMutableTreeNode>();
			DefaultMutableTreeNode node =
					(DefaultMutableTreeNode)paths[0].getLastPathComponent();
			DefaultMutableTreeNode copy = copy(node);
			copies.add(copy);
			toRemove.add(node);
			for(int i = 1; i < paths.length; i++) {
				DefaultMutableTreeNode next =
						(DefaultMutableTreeNode)paths[i].getLastPathComponent();
				// Do not allow higher level nodes to be added to list.
				if(next.getLevel() < node.getLevel()) {
					break;
				} else if(next.getLevel() > node.getLevel()) {  // child node
					copy.add(copy(next));
					// node already contains child
				} else {                                        // sibling
					copies.add(copy(next));
					toRemove.add(next);
				}
			}
			DefaultMutableTreeNode[] nodes =
					copies.toArray(new DefaultMutableTreeNode[copies.size()]);
			nodesToRemove =
					toRemove.toArray(new DefaultMutableTreeNode[toRemove.size()]);
			return new NodesTransferable(nodes);
		}
		return null;
	}

	/** Defensive copy used in createTransferable. */
	private DefaultMutableTreeNode copy(TreeNode node) {
		return new DefaultMutableTreeNode(node);
	}

	/**
	 * Invoked after data has been exported. This method should remove the data
	 * that was transferred if the action was MOVE. 
	 */
	@Override
	protected void exportDone(JComponent source, Transferable data, int action) {
		if((action & MOVE) == MOVE) {
			JTree tree = (JTree)source;
			DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
			// Remove nodes saved in nodesToRemove in createTransferable.
			for(int i = 0; i < nodesToRemove.length; i++) {
				model.removeNodeFromParent(nodesToRemove[i]);
			}
		}
	}

	/**
	 * Returns the type of transfer actions
	 * supported by the source; any bitwise-OR
	 * combination of COPY, MOVE and LINK. 
	 */
	@Override
	public int getSourceActions(JComponent c) {
		return MOVE;
	}

	/**
	 * Causes transfer to occur from drag and drop operation
	 */
	@Override
	public boolean importData(TransferHandler.TransferSupport support) {
		if(!canImport(support)) {
			return false;
		}
		// Extract transfer data.
		DefaultMutableTreeNode[] nodes = null;
		try {
			Transferable t = support.getTransferable();
			nodes = (DefaultMutableTreeNode[])t.getTransferData(nodesFlavor);
		} catch(UnsupportedFlavorException ufe) {
			System.out.println("UnsupportedFlavor: " + ufe.getMessage());
		} catch(java.io.IOException ioe) {
			System.out.println("I/O error: " + ioe.getMessage());
		}
		// Get drop location info.
		JTree.DropLocation dl =
				(JTree.DropLocation)support.getDropLocation();
		int childIndex = dl.getChildIndex();
		TreePath dest = dl.getPath();
		DefaultMutableTreeNode parent =
				(DefaultMutableTreeNode)dest.getLastPathComponent();
		JTree tree = (JTree)support.getComponent();
		DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
		// Configure for drop mode.
		int index = childIndex;    // DropMode.INSERT
		if(childIndex == -1) {     // DropMode.ON
			index = parent.getChildCount();
		}
		// Add data to model.
		for(int i = 0; i < nodes.length; i++) {
			model.insertNodeInto(nodes[i], parent, index++);
		}

		// Notify ViewController that nodes have changed parent
		new Thread(new MoveHandler(nodes, parent)).start();

		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getClass().getName();
	}


	public class NodesTransferable implements Transferable {
		DefaultMutableTreeNode[] nodes;

		public NodesTransferable(DefaultMutableTreeNode[] nodes) {
			this.nodes = nodes;
		}

		public Object getTransferData(DataFlavor flavor)
				throws UnsupportedFlavorException {
			if(!isDataFlavorSupported(flavor))
				throw new UnsupportedFlavorException(flavor);
			return nodes;
		}

		public DataFlavor[] getTransferDataFlavors() {
			return flavors;
		}

		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return nodesFlavor.equals(flavor);
		}
	}

	private class MoveHandler implements Runnable {

		DefaultMutableTreeNode[] nodes;
		DefaultMutableTreeNode parent;

		MoveHandler(DefaultMutableTreeNode[] nodes, DefaultMutableTreeNode parent) {
			this.nodes = nodes;
			this.parent = parent;
		}

		@Override
		public void run() {
			updatePeers();
		}

		/**
		 * Gets the list of peers that have changed zones, updates their zone
		 * field, and outputs the list to the controller class.
		 * @param nodes
		 * @param parent
		 */
		private void updatePeers() {
			List<Peer> peerList = new ArrayList<Peer>();
			for(DefaultMutableTreeNode node : nodes) {
				// The transfer handler does some strange voodoo to these objects.
				peerList.add((Peer) ((DefaultMutableTreeNode)node.getUserObject()).getUserObject());
			}
			Zone zone = (Zone)parent.getUserObject();

			// Add zone if it doesn't exist
			if( !gui.getZoneMap().containsKey(zone)) {
				callback.notify(Command.NEW_ZONE, new ZoneProperties(zone.getName()), zone);
			}

			for(Peer peer : peerList) {
				peer.setZone(zone);
			}
			callback.notify(Command.PEER_ZONE_CHANGE, peerList);
		}

	}
}