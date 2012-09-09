package soundcheck.musicPlayer.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.TransferHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soundcheck.musicPlayer.ServiceCallback;
import soundcheck.shared.Const;
import soundcheck.shared.Const.Command;
import soundcheck.shared.Song;

/**
 * Extends upon JTree by allowing for nodes to be drag and drop.
 *
 */
public class ListDragAndDrop extends JList {
	private static final long serialVersionUID = 1L;
	final static Logger logger = LoggerFactory.getLogger(ListDragAndDrop.class);

	private PlayerGUI gui;
	ServiceCallback callback;

	private JList list;
	private DefaultListModel model;

	ListDragAndDrop(PlayerGUI gui, ServiceCallback callback) {
		this.gui = gui;
		this.callback = callback;
	}

	/**
	 * Sets up the JTree.
	 * @return
	 */
	JList getContent() {

		model = new DefaultListModel();
		list = new JList(model);

		// Only select one song in playlist at a time
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION );

		list.setDragEnabled(true);
		list.setDropMode(DropMode.INSERT);

		// Add drag and drop support
		list.setTransferHandler(new ListTransferHandler(list, callback, gui));

		list.setCellRenderer(new PlayListCellRenderer());
		list.setBorder(null);

		list.addMouseListener(new MouseAdapter() {
			private PlayListPopUpMenu popup = new PlayListPopUpMenu();

			/**
			 * Display pop up menu for songs
			 */
			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger()) {
					JList source = (JList)e.getSource();
					source.setSelectedIndex(source.locationToIndex(e.getPoint()));
					popup.show(source, e.getX(), e.getY());
				}
			}

			/**
			 * Play songs that are double clicked
			 */
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					int index = list.locationToIndex(e.getPoint());
					Song selectedSong = (Song) list.getModel().getElementAt(index);
					
					model.remove(index);
					
					model.add(model.size(), selectedSong);
					
					callback.notify(Command.NEW_QUEUE, Arrays.asList(model.toArray()), gui.getCurrentZone());
				}
			}
		});

		return list;
	}

	/**
	 * Set the songs in the playlist
	 * @param songList
	 */
	public void setPlayList(List<Song> songList) {
		// Ensure table has been created
		if( model == null ) {
			return;
		}

		model.clear();

		// Add songs to the list bottom to top
		for(Song song : songList) {
			model.add(0, song);
		}

		// Set the progress bar for the playing song
		if( songList.isEmpty() == false ) {
			gui.infoPane.setProgressMaximum(songList.get(0).getDuration());
		}
	}

	/**
	 * Get the playlist currently displayed in this object,
	 * or null if the playlist doesn't exist.
	 * @return
	 */
	public Object[] getPlayList() {
		if( model != null )
			return model.toArray();
		else
			return null;
	}

	/**
	 * Class to display context menu on playlist items
	 *
	 */
	class PlayListPopUpMenu extends JPopupMenu {
		private static final long serialVersionUID = 1L;

		JMenuItem remove;
		JMenuItem playNow;

		public PlayListPopUpMenu(){			
			playNow = new JMenuItem("Play Now");
			playNow.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					// Move the selected row to the front of the playlist
					JList invoker = (JList) PlayListPopUpMenu.this.getInvoker();
					Song selectedSong = (Song) invoker.getModel().getElementAt(invoker.getSelectedIndex());
					callback.notify(Command.QUEUE_FRONT, selectedSong, gui.getCurrentZone() );
				}
			});
			add(playNow);

			remove = new JMenuItem("Remove From Playlist");
			remove.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					// Remove the selected row from the playlist
					JList invoker = (JList) PlayListPopUpMenu.this.getInvoker();
					Song selectedSong = (Song) invoker.getModel().getElementAt(invoker.getSelectedIndex());
					callback.notify(Command.QUEUE_REMOVE, selectedSong, gui.getCurrentZone() );
				}
			});
			add(remove);
		}
	}


	/**
	 * Class for rendering the individual cells in the playlist. Each
	 * cell represents a single song.
	 *
	 */
	static class PlayListCellRenderer extends JLabel implements ListCellRenderer {
		private static final long serialVersionUID = 1L;

		public Component getListCellRendererComponent(
				JList list,              // the list
				Object value,            // value to display
				int index,               // cell index
				boolean isSelected,      // is the cell selected
				boolean cellHasFocus)    // does the cell have focus
		{
			Song song = (Song) value;

			String display = song.getTitle() + "<br>"
					+ song.getArtist();

			setText("<html>" + display);

			if (isSelected) {
				setBackground(Const.highlightColor);
				setForeground(Const.bgColor);
			} else {
				setBackground(Const.bgColor);
				setForeground(Const.txtColor);
			}

			// The current playing song
			if( index == list.getModel().getSize() - 1 ) {
				Font f = list.getFont();
				setFont(f.deriveFont(Font.BOLD));
			} else {
				setFont(list.getFont());
			}

			setBorder(
					BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
			setEnabled(list.isEnabled());
			setOpaque(true);
			return this;
		}
	}

	/**
	 * Allow drag and drop to reorder list elements
	 * @author Ingo Kegel (http://stackoverflow.com/questions/7222988/java-drag-and-drop-images-in-a-list)
	 *
	 */
	static class ListTransferHandler extends TransferHandler {
		private static final long serialVersionUID = 1L;

		private static final DataFlavor DATA_FLAVOUR = new DataFlavor(PlayListCellRenderer.class, "Songs");

		private final JList playlist;
		private boolean inDrag;
		
		// Variables added by Cavyn
		private ServiceCallback callback;
		private PlayerGUI gui;

		ListTransferHandler(JList playlist, ServiceCallback callback, PlayerGUI gui) {
			this.playlist = playlist;
			this.callback = callback;
			this.gui = gui;
		}

		public int getSourceActions(JComponent c) {
			return TransferHandler.MOVE;
		}

		protected Transferable createTransferable(JComponent c) {
			inDrag = true;
			return new Transferable() {
				public DataFlavor[] getTransferDataFlavors() {
					return new DataFlavor[] {DATA_FLAVOUR};
				}

				public boolean isDataFlavorSupported(DataFlavor flavor) {
					return flavor.equals(DATA_FLAVOUR);
				}

				public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
					return playlist.getSelectedValue();
				}
			};
		}

		public boolean canImport(TransferSupport support) {
			if (!inDrag || !support.isDataFlavorSupported(DATA_FLAVOUR)) {
				return false;
			}

			JList.DropLocation dl = (JList.DropLocation)support.getDropLocation();
			if (dl.getIndex() == -1) {
				return false;
			} else {
				return true;
			}
		}

		public boolean importData(TransferSupport support) {
			if (!canImport(support)) {
				return false;
			}

			Transferable transferable = support.getTransferable();
			try {
				Object draggedImage = transferable.getTransferData(DATA_FLAVOUR);

				JList.DropLocation dl = (JList.DropLocation)support.getDropLocation();
				DefaultListModel model = (DefaultListModel)playlist.getModel();
				int dropIndex = dl.getIndex();
				if (model.indexOf(draggedImage) < dropIndex) {
					dropIndex--;
				}
				model.removeElement(draggedImage);
				model.add(dropIndex, draggedImage);
				callback.notify( Command.NEW_QUEUE, Arrays.asList(model.toArray()), gui.getCurrentZone() );
				return true;
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}

		protected void exportDone(JComponent source, Transferable data, int action) {
			super.exportDone(source, data, action);
			inDrag = false;
		}
	}
}
