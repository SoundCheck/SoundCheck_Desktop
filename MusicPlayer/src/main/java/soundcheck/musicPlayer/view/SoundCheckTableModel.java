package soundcheck.musicPlayer.view;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import soundcheck.shared.Song;

public class SoundCheckTableModel extends AbstractTableModel {
	private static final long serialVersionUID = 1L;

	/**
	 * Immutable names of hidden columns for this model
	 */
	private final String[] hiddenColumnNames = {
			"Album Artist",
			"Album",
			"BPM",
			"Comment",
			"Composer",
			"Disc No",
			"Grouping",
			"Track",
			"Year"
	};

	/**
	 * Immutable names of the columns for this model
	 */
	private final String[] columnNames = {
			"Name",
			"Artist",
			"Time",
			"Genre",
			"Album Artist",
			"Album",
			"BPM",
			"Comment",
			"Composer",
			"Disc No",
			"Grouping",
			"Track",
			"Year"
	};

	/**
	 * The list of songs backing this model.
	 */
	private List<Song> data = new ArrayList<Song>();

	/**
	 * Returns the number of columns
	 */
	@Override
	public int getColumnCount() {
		return columnNames.length;
	}

	/**
	 * Get the name of the given column. Returns an
	 * empty string if the column can't be found.
	 */
	@Override
	public String getColumnName(int column) {
		if ( column < getColumnCount() ) {
			return columnNames[column];
		} else {
			return "";
		}
	}

	/**
	 * Get the number of rows (songs)
	 */
	@Override
	public int getRowCount() {
		return data.size();
	}

	/**
	 * Add a list of songs to this table
	 * 
	 * @param songList
	 */
	public void addAll( List<Song> songList ) {
		data.addAll( songList );
	}

	/**
	 * Add a song to this table
	 * @param song
	 */
	public void addRow(Song song) {
		data.add(song);
	}

	public Song getRow(int row) {
		return data.get(row);
	}

	public void clearData() {
		data = new ArrayList<Song>();
	}

	/**
	 * Translates the song objects backing up this table into the individual
	 * data cells.
	 */
	@Override
	public Object getValueAt(int row, int col) {
		Song song = data.get(row);
		Object retVal;

		if( song == null ) {
			retVal = "N/A";
		} else {
			switch (col) {
			case 0:
				retVal = song.getTitle() == null || song.getTitle().isEmpty() ? "N/A" : song.getTitle();
				break;
			case 1:
				retVal = song.getArtist() == null || song.getArtist().isEmpty() ? "N/A" : song.getArtist();
				break;
			case 2:
				retVal = song.getDurationFormatted();
				break;
			case 3:
				retVal = song.getGenre() == null || song.getGenre().isEmpty() ? "N/A" : song.getGenre();
				break;
			case 4:
				retVal = song.getAlbumArtist() == null || song.getAlbumArtist().isEmpty() ? "N/A" : song.getAlbumArtist();
				break;
			case 5:
				retVal = song.getAlbum() == null || song.getAlbum().isEmpty() ? "N/A" : song.getAlbum();
				break;
			case 6:
				retVal = song.getBpm() == null || song.getBpm().isEmpty() ? "N/A" : song.getBpm();
				break;
			case 7:
				retVal = song.getComment() == null || song.getComment().isEmpty() ? "N/A" : song.getComment();
				break;
			case 8:
				retVal = song.getComposer() == null || song.getComposer().isEmpty() ? "N/A" : song.getComposer();
				break;
			case 9:
				retVal = song.getDiscNo() == null || song.getDiscNo().isEmpty() ? "N/A" : song.getDiscNo();
				break;
			case 10:
				retVal = song.getGrouping() == null || song.getGrouping().isEmpty() ? "N/A" : song.getGrouping();
				break;
			case 11:
				retVal = song.getTrack() == null || song.getTrack().isEmpty() ? "N/A" : song.getTrack();
				break;
			case 12:
				retVal = song.getYear() == null || song.getYear().isEmpty() ? "N/A" : song.getYear();
				break;
			default:
				retVal = null;
				break;
			}
		}
		return retVal;
	}

	/**
	 * This table is not editable
	 */
	@Override
	public boolean isCellEditable(int row, int column) {
		//all cells false
		return false;
	}

	/**
	 * @return the hiddenColumnNames
	 */
	public String[] getHiddenColumnNames() {
		return hiddenColumnNames;
	}


}
