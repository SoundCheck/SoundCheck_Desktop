package soundcheck.musicPlayer.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.HighlighterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soundcheck.musicPlayer.ServiceCallback;
import soundcheck.shared.Const;
import soundcheck.shared.Const.Command;
import soundcheck.shared.Song;

class SongPane {
	final static Logger logger = LoggerFactory.getLogger(SongPane.class);

	private final PlayerGUI gui;
	private ServiceCallback callback;

	// Create model for holding table data that isn't editable
	private final SoundCheckTableModel dataModel = new SoundCheckTableModel();
	private final TableRowSorter<SoundCheckTableModel> sorter = new TableRowSorter<SoundCheckTableModel>(dataModel);
	final JXTable table  = new JXTable(dataModel);

	SongPane(PlayerGUI gui, ServiceCallback callback) {
		this.gui = gui;
		this.callback = callback;
	}

	/**
	 * Add song objects from a list into the table
	 * @param songList
	 */
	void loadSongList(List<Song> songList) {
		logger.trace("Loading songs into table");

		// clear current table to prevent duplicate list, single peer display
		dataModel.clearData();

		// Populate the JTable (TableModel) with data from ArrayList
		dataModel.addAll(songList);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				dataModel.fireTableDataChanged();
			}
		});
		
	}

	/** 
	 * Update the row filter regular expression from the expression in
	 * the text box.
	 */
	void newFilter(String filterText) {
		RowFilter<SoundCheckTableModel, Object> rf = null;
		//If current expression doesn't parse, don't update.
		try {
			rf = RowFilter.regexFilter("(?i)" + filterText);
		} catch (java.util.regex.PatternSyntaxException e) {
			return;
		}
		sorter.setRowFilter(rf);
	}

	/**
	 * Add song currently selected by user to the queue. If a song is
	 * not selected, return false.
	 */
	boolean addSelectedSongToQueue() {
		int viewRow = table.getSelectedRow();
		if (viewRow >= 0) {
			int modelRow = 
					table.convertRowIndexToModel(viewRow);
			callback.notify(Command.TEARDOWN, null, gui.getCurrentZone());
			callback.notify(Command.QUEUE_FRONT, dataModel.getRow(modelRow), gui.getCurrentZone() );
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Create songTable in middle of window
	 * @return
	 */
	JXTable createSongPane() {
		table.setColumnControlVisible(true);

		Enumeration<TableColumn> col = table.getColumnModel().getColumns();
		for(; col.hasMoreElements() ;) {
			((TableColumn) col.nextElement()).setHeaderRenderer(new SoundCheckHeaderRenderer());
		}

		// Hide all column headers but first 4
		for( String column : dataModel.getHiddenColumnNames() ) {
			table.getColumnExt(column).setVisible(false);
		}

		table.setShowGrid(false, false);
		table.addHighlighter(HighlighterFactory.createAlternateStriping(Const.listColor1, Const.listColor2));
		table.setBackground(new Color(126,130,133));
		table.setForeground(Const.txtColor);

		table.setRowSorter(sorter);
		table.setDefaultRenderer(table.getColumnClass(0), new HighlightRenderer() );
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		table.addMouseListener(new MouseAdapter() {
			private SongListPopUpMenu popup = new SongListPopUpMenu();

			/**
			 * Double clicking adds song to front of queue
			 */
			@Override
			public void mouseClicked(MouseEvent evt) {
				if(evt.getClickCount() == 2) {
					addSelectedSongToQueue();
				}
			}

			/**
			 * Display pop up menu for songs
			 */
			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger()) {
					JXTable source = (JXTable)e.getSource();
					int row = source.rowAtPoint( e.getPoint() );
					int column = source.columnAtPoint( e.getPoint() );

					if (! source.isRowSelected(row)) {
						source.changeSelection(row, column, false, false);
					}

					// Only show pop up menu if mouse is over a song
					if( row >= 0) {
						popup.show(e.getComponent(), e.getX(), e.getY());
					}
				}
			}
		});

		return table;
	}

	class SongListPopUpMenu extends JPopupMenu {
		private static final long serialVersionUID = 1L;

		JMenuItem playNow;
		JMenuItem toPlayList;

		public SongListPopUpMenu(){
			toPlayList = new JMenuItem("Add to Playlist");
			toPlayList.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					// Add the selected row to the playlist
					JXTable invoker = (JXTable) SongListPopUpMenu.this.getInvoker();
					int viewRow = invoker.getSelectedRow();
					if (viewRow >= 0) {
						int modelRow = invoker.convertRowIndexToModel(viewRow);
						callback.notify(Command.QUEUE_BACK, dataModel.getRow(modelRow), gui.getCurrentZone() );
					}
				}
			});
			add(toPlayList);

			playNow = new JMenuItem("Play Now");
			playNow.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					// Add the selected row to the playlist
					JXTable invoker = (JXTable) SongListPopUpMenu.this.getInvoker();
					int viewRow = invoker.getSelectedRow();
					if (viewRow >= 0) {
						int modelRow = invoker.convertRowIndexToModel(viewRow);
						addSelectedSongToQueue();
						try {
							Thread.sleep(500);
						} catch (InterruptedException e1) {
							logger.error("",e);
						}
						callback.notify(Command.PLAY, null, gui.getCurrentZone());
					}
				}
			});
			add(playNow);
		}
	}

	static class HighlightRenderer extends DefaultTableCellRenderer {
		private static final long serialVersionUID = 1L;

		public static final DefaultTableCellRenderer DEFAULT_RENDERER = new DefaultTableCellRenderer();

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			Component c = DEFAULT_RENDERER.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			if(isSelected) {
				c.setBackground(Const.txtColor);
				c.setForeground(Const.bgColor);
			} else {

			}

			return c;
		}
	}
}

class SoundCheckHeaderRenderer extends JPanel implements TableCellRenderer {
	private static final long serialVersionUID = 1L;

	private GradientPaint gradientPaint = new GradientPaint(0, 0, Const.mnuColor1, 0,
			40, Const.mnuColor2, true);
	private JLabel label = new JLabel();

	public SoundCheckHeaderRenderer() {
		setOpaque(true);
		setLayout(new BorderLayout());
		add(label, BorderLayout.CENTER);
		label.setHorizontalAlignment(SwingConstants.CENTER);
		label.setForeground(Const.mnuTxtColor);
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocused, int arg4, int arg5) {
		label.setText(value.toString());
		label.setFont(new Font("sansserif", Font.BOLD, 14));
		return this;
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		g2.setPaint(gradientPaint);
		g2.fillRect(0, 0, getWidth(), getHeight());
	}
}
