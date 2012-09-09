package soundcheck.shared;

import java.io.Serializable;

public class Song implements Serializable {
	private static final long serialVersionUID = 1L;

	private String title;

	private String filePath;

	private String artist;

	private String genre;

	private int id;  //The id of the song.  Unique for each song for a given peer.

	private long duration;
	
	private String albumArtist;
	private String album;
	private String bpm;
	private String comment;
	private String composer;
	private String discNo;
	private String grouping;
	private String track;
	private String year;


	public Song(String title, String filePath, String artist, String genre, long duration, int id){
		this.title = title;
		this.filePath = filePath;
		this.artist = artist;
		this.genre = genre;
		this.setDuration(duration);
		this.id = id;
		
		// initialize remaining fields as empty strings
		this.albumArtist = "";
		this.album = "";
		this.bpm = "";
		this.comment = "";
		this.discNo = "";
		this.grouping = "";
		this.track = "";
		this.year = "";
	}

	/**
	 * Constructs a dummy song object with null fields.
	 * This constructor should only be called in an error condition.  
	 */
	public Song() {
		this(null,null,null,null,0,0);
	}

	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}
	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}
	/**
	 * @return the data
	 */
	public String getFilePath() {
		return filePath;
	}
	/**
	 * @param data the data to set
	 */
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	/**
	 * @return the artist
	 */
	public String getArtist() {
		return artist;
	}
	/**
	 * @param artist the artist to set
	 */
	public void setArtist(String artist) {
		this.artist = artist;
	}

	/**
	 * @return the genre
	 */
	public String getGenre() {
		return genre;
	}

	/**
	 * @param genre the genre to set
	 */
	public void setGenre(String genre) {
		this.genre = genre;
	}

	/**
	 * @return the duration
	 */
	public long getDuration() {
		return duration;
	}

	/**
	 * @return the duration in a nice format
	 */
	public String getDurationFormatted() {
		return Util.secToFormattedTime(duration);
	}

	/**
	 * @param duration the duration to set
	 */
	public void setDuration(long duration) {
		this.duration = duration;
	}

	/**
	 * 
	 * @return - The ID of the song
	 */
	public int getId() {
		return id;
	}
	
	/**
	 * @param id the id to be set
	 */
	public void setId( int id ) {
		this.id = id;
	}

	/**
	 * @return the albumArtist
	 */
	public String getAlbumArtist() {
		return albumArtist;
	}

	/**
	 * @param albumArtist the albumArtist to set
	 */
	public void setAlbumArtist(String albumArtist) {
		this.albumArtist = albumArtist;
	}

	/**
	 * @return the album
	 */
	public String getAlbum() {
		return album;
	}

	/**
	 * @param album the album to set
	 */
	public void setAlbum(String album) {
		this.album = album;
	}

	/**
	 * @return the bpm
	 */
	public String getBpm() {
		return bpm;
	}

	/**
	 * @param bpm the bpm to set
	 */
	public void setBpm(String bpm) {
		this.bpm = bpm;
	}

	/**
	 * @return the comment
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * @param comment the comment to set
	 */
	public void setComment(String comment) {
		this.comment = comment;
	}

	/**
	 * @return the composer
	 */
	public String getComposer() {
		return composer;
	}

	/**
	 * @param composer the composer to set
	 */
	public void setComposer(String composer) {
		this.composer = composer;
	}

	/**
	 * @return the discNo
	 */
	public String getDiscNo() {
		return discNo;
	}

	/**
	 * @param discNo the discNo to set
	 */
	public void setDiscNo(String discNo) {
		this.discNo = discNo;
	}

	/**
	 * @return the grouping
	 */
	public String getGrouping() {
		return grouping;
	}

	/**
	 * @param grouping the grouping to set
	 */
	public void setGrouping(String grouping) {
		this.grouping = grouping;
	}

	/**
	 * @return the track
	 */
	public String getTrack() {
		return track;
	}

	/**
	 * @param track the track to set
	 */
	public void setTrack(String track) {
		this.track = track;
	}

	/**
	 * @return the year
	 */
	public String getYear() {
		return year;
	}

	/**
	 * @param year the year to set
	 */
	public void setYear(String year) {
		this.year = year;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((artist == null) ? 0 : artist.hashCode());
		result = prime * result + (int) (duration ^ (duration >>> 32));
		result = prime * result + ((title == null) ? 0 : title.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object otherObject) {
		
		if ( this == otherObject) {
			return true;
		}
		
		if ( otherObject == null || !(otherObject instanceof Song )) {
			return false;
		} else {
			Song otherSong = (Song) otherObject;
			
			if( otherSong.title == null || otherSong.album == null ) {
				return false;
			}
			// return whether key attributes match
			return otherSong.title.equalsIgnoreCase(this.title) && otherSong.artist.equalsIgnoreCase(this.artist)
					&& otherSong.duration == this.duration;
		}
	}
}
