package soundcheck.musicPlayer.songManagement;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import soundcheck.shared.Song;

public final class PlayListFileTransaction {
	final static Logger logger = LoggerFactory.getLogger(PlayListFileTransaction.class);
	
	// Suppress default constructor for noninstantiability
	private PlayListFileTransaction() {
		throw new AssertionError();
	}

	/**
	 * Writes to the specified file after formatting each song into the correct format
	 * as described in the .cejj formatting guide.
	 * @param songList
	 * @param playListFile
	 */
	public final static void savePlayList(List<Song> songList, File playListFile) {
		try {
			OutputStream fos = new FileOutputStream(playListFile);
			PrintStream ps = new PrintStream(fos);

			ps.println("<playList>");

			for (Song s : songList) {
				ps.println(
						"  <song>\n" +
								"    <name>" + stringEscape( s.getTitle() ) + "</name>\n" +
								"    <artist>" + stringEscape( s.getArtist() ) + "</artist>\n" +
								"    <length>" + s.getDuration() + "</length>\n" +
								"  </song>\n"
						);
			}
			ps.println("</playList>");
		} catch (FileNotFoundException e) {
			logger.error("Error creating playlist file.", e);
		}
	}
	
	private final static String stringEscape(String s) {
		String reg = "&(?!&#?[a-zA-Z0-9]{2,7};)";
		Pattern p = Pattern.compile(reg);
		
		String escapedString = s.replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
		// Don't take ampersands from escaped strings
		return p.matcher(escapedString).replaceAll("&amp;");
	}

	/**
	 * Load a file, determine its type, and convert it to a
	 * list of songs.
	 * @param playListFile
	 * @return
	 */
	public final static List<Song> loadPlayList(File playListFile) {
		try {
			InputStream is = new FileInputStream(playListFile);
			Scanner sc = new Scanner(is);

			String nextLine;

			// Get first line
			if( sc.hasNextLine() ) {
				nextLine = sc.nextLine();

				// File is of .cejj format
				if( nextLine.startsWith("<playList>") ) {
					return parseXML(playListFile);
				}

				// File is of .m3u format
				if( nextLine.startsWith("#EXTM3U") ) {
					List<Song> songList = new ArrayList<Song>();

					while( sc.hasNextLine() ) {
						nextLine = sc.nextLine();
						if( nextLine.startsWith("#EXTINF:")) {
							Song song = new Song();

							// Remove #EXTINF: from string
							nextLine = nextLine.substring(8);

							// Get song length
							song.setDuration(Long.parseLong( nextLine.substring(0, nextLine.indexOf(',')) ));

							// Get song title
							song.setTitle(nextLine.substring(nextLine.indexOf(',') + 1, nextLine.lastIndexOf('-') - 1));

							// Get song artist
							song.setArtist(nextLine.substring(nextLine.lastIndexOf('-') + 1));

							songList.add(song);
						}
					}
					return songList;
				}
			} else {
				// File is empty
				return new ArrayList<Song>();
			}
		} catch (FileNotFoundException e) {
			logger.warn("Could not locate specified file.",e);
		}
		return null;
	}
	
	

	/**
	 * Convert the XML file to a list of songs
	 * @param playListFile
	 */
	private final static List<Song> parseXML(File playListFile) {
		List<Song> songList = new ArrayList<Song>();

		try {
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(playListFile);

			// normalize text representation
			doc.getDocumentElement().normalize();

			NodeList listOfSongs = doc.getElementsByTagName("song");

			for(int i = 0; i < listOfSongs.getLength() ; i++){
				Song song = new Song();

				Node firstSongNode = listOfSongs.item(i);
				if(firstSongNode.getNodeType() == Node.ELEMENT_NODE){

					Element firstSongElement = (Element)firstSongNode;

					//-------
					NodeList songNameList = firstSongElement.getElementsByTagName("name");
					Element songNameElement = (Element)songNameList.item(0);

					NodeList textSongNameList = songNameElement.getChildNodes();
					song.setTitle( ((Node)textSongNameList.item(0)).getNodeValue().trim() );

					//-------
					NodeList artistList = firstSongElement.getElementsByTagName("artist");
					Element artistElement = (Element)artistList.item(0);

					NodeList textArtistList = artistElement.getChildNodes();
					song.setArtist( ((Node)textArtistList.item(0)).getNodeValue().trim() );

					//----
					NodeList lengthList = firstSongElement.getElementsByTagName("length");
					Element lengthElement = (Element)lengthList.item(0);

					NodeList textLengthList = lengthElement.getChildNodes();
					song.setDuration( Long.parseLong( ((Node)textLengthList.item(0)).getNodeValue().trim() ) );

					//------
					songList.add(song);
				}
			}

		}catch (SAXParseException err) {
			logger.warn("** Parsing error" + ", line " 
					+ err.getLineNumber () + ", uri " + err.getSystemId (), err);

		}catch (SAXException e) {
			Exception x = e.getException ();
			logger.warn( "",((x == null) ? e : x) );

		}catch (Throwable t) {
			logger.error("",t);
		}

		return songList;
	}
}
