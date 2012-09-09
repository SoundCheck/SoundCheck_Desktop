package soundcheck.musicPlayer.songManagement;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.reference.GenreTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soundcheck.musicPlayer.ServiceCallback;
import soundcheck.musicPlayer.view.PlayerGUI;
import soundcheck.shared.Const.Command;
import soundcheck.shared.Song;

public class SongListManager implements Runnable {
	final static Logger logger = LoggerFactory.getLogger(SongListManager.class);
	
	private static int id = 0;
	File parentDir;
	ServiceCallback callback;
	PlayerGUI gui;
	
	public SongListManager(File parentDir, ServiceCallback callback, PlayerGUI gui) {
		this.parentDir = parentDir;
		this.callback = callback;
		this.gui = gui;
	}
	
	@Override
	public void run() {
		buildLocalSongList(parentDir, callback, gui);
	}
	
	/**
	 * Given a file, find all music files within recursive directories
	 * and build a list collection for use within SoundCheck.
	 * @param parentDir
	 * @return
	 */
	public static final void buildLocalSongList(File parentDir, ServiceCallback callback, PlayerGUI gui) {
		
		java.util.logging.Logger.getLogger("org.jaudiotagger").setLevel(java.util.logging.Level.OFF);
		
		logger.trace("Finding songs in folder {}", parentDir.getName());
		
		// Display progress in GUI
		gui.controlPane.infoStart();
		gui.controlPane.infoUpdate("Loading Songs...");

		List<Song> songList = new ArrayList<Song>();
		id = 0;

		if (!parentDir.isAbsolute()) {
			parentDir = parentDir.getAbsoluteFile();
		}

		// Recursively looks through all directories for song files
		if (parentDir.isDirectory()) {
			songList.addAll(addFolderToList(parentDir, gui));
		}

		gui.controlPane.infoEnd();
		
		logger.trace("Found {} songs", songList.size());
		
		callback.notify(Command.SET_SONGLIST, songList);
		
		// Update upon addition is unnecessary because whole songlist will be sent over network
		//callback.notify(Command.REQUEST_UPDATE, null);
	}

	/**
	 * Recursive method to search through all enclosing directories for music
	 * files.
	 * @param parentDir
	 * @return
	 */
	private static final List<Song> addFolderToList(File parentDir, PlayerGUI gui) {
		File[] childFiles = parentDir.listFiles();
		List<Song> songList = new ArrayList<Song>();

		for (File child : childFiles) {
			if (child.isFile() && child.getName().contains(".mp3")) {
				// create a song object for the file
				Song s = createSong(child.getAbsolutePath());

				// add the song to the list
				songList.add(s);
			} else if (child.isDirectory()) {
				songList.addAll(addFolderToList(child, gui));
			}
		}
		return songList;
	}
	
	/**
	 * Method to gather information from a mp3 file and create a song object.
	 * 
	 * @param filePath Location of the song
	 * @return retSong song object representing the song
	 */
	private static final Song createSong( String filePath ) {
		
		Song retSong = new Song();
		
		retSong.setFilePath( filePath );
		
		AudioFile f = null;
		try {
			// read file as audio file (this allows for expansion beyond mp3)
			f = AudioFileIO.read( new File( filePath ));
			
			// get tags associated with file
			Tag tag = f.getTag();
			
			retSong.setTitle( tag.getFirst( FieldKey.TITLE ));
			retSong.setArtist( tag.getFirst( FieldKey.ARTIST ));
			retSong.setGenre( tag.getFirst( FieldKey.GENRE ));
			retSong.setId( id++ );
			
			retSong.setAlbumArtist( tag.getFirst( FieldKey.ALBUM_ARTIST ));
			retSong.setAlbum( tag.getFirst( FieldKey.ALBUM ));
			retSong.setBpm( tag.getFirst( FieldKey.BPM ));
			retSong.setComment( tag.getFirst( FieldKey.COMMENT ));
			retSong.setComposer( tag.getFirst( FieldKey.COMPOSER ));
			retSong.setDiscNo( tag.getFirst( FieldKey.DISC_NO ));
			retSong.setGrouping( tag.getFirst(FieldKey.GROUPING ));
			retSong.setTrack( tag.getFirst( FieldKey.TRACK ));
			retSong.setYear( tag.getFirst( FieldKey.YEAR ));
			
			if( retSong.getGenre().startsWith("(") ) {	// id3v1 genre specified with number
				
				String genre = retSong.getGenre();
				
				// get the number
				genre = genre.substring(1, genre.length()-1);
				int genreNum = Integer.parseInt(genre);
				
				// retrieve genre from list
				retSong.setGenre( GenreTypes.getInstanceOf().getValueForId( genreNum ));
			}

			// get audio file header
			AudioHeader ah = f.getAudioHeader();
			// get length of song in seconds
			retSong.setDuration( ah.getTrackLength() );
			
			if( retSong.getTitle().isEmpty() ) {	// song has no title tag
				
				String title = retSong.getTitle();
				
				// make fileName the title
				title = f.getFile().getName();
				retSong.setTitle( title.substring( 0, title.length()-4 ));
			}
			
		} catch (CannotReadException e) {
			logger.warn("",e);
		} catch (IOException e) {
			logger.warn("",e);
		} catch (TagException e) {
			logger.warn("",e);
		} catch (ReadOnlyFileException e) {
			logger.warn("",e);
		} catch (InvalidAudioFrameException e) {
			logger.warn("",e);
		} catch (NumberFormatException e) {
			logger.warn("Issue reading song tags for {}", filePath, e);
		}
		
		return retSong;
	}
}
