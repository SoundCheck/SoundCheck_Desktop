package soundcheck.shared;

import java.awt.Image;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;

import javax.swing.ImageIcon;

public class Util {
	
	/**
	 * Convert time in seconds to a formatted string.
	 * @param time
	 * @return
	 */
	public static final String secToFormattedTime( long time ) {
		long hours = time / 3600;
		long minutes = (time % 3600) / 60;
		long seconds = time % 60;

		String stHour = (hours < 10 ? "0" : "") + hours;
		String stMinu = ((minutes < 10) && (hours != 0) ? "0" : "") + minutes;
		String stSec = (seconds < 10 ? "0" : "") + seconds ;

		return (hours == 0 ? "" : stHour + ":") + stMinu + ":" + stSec;
	}
	
	public Image createImage(String path, String description) {
		return createImageIcon(path, description).getImage();
	}

	/**
	 * Returns an ImageIcon, or null if the path was invalid.
	 * 
	 */
	public ImageIcon createImageIcon(String path, String description) {
		java.net.URL imgURL = getClass().getResource(path);
		if (imgURL != null) {
			return new ImageIcon(imgURL, description);
		} else {
			System.err.println("Couldn't find file: " + path);
			return null;
		}
	}

	/**
	 * Checks to see if a specific port is available.
	 *
	 * @param port the port to check for availability
	 */
	public static boolean isPortAvailable(int port) {
	    ServerSocket ss = null;
	    DatagramSocket ds = null;
	    try {
	        ss = new ServerSocket(port);
	        ss.setReuseAddress(true);
	        ds = new DatagramSocket(port);
	        ds.setReuseAddress(true);
	        return true;
	    } catch (IOException e) {
	    } finally {
	        if (ds != null) {
	            ds.close();
	        }

	        if (ss != null) {
	            try {
	                ss.close();
	            } catch (IOException e) {
	                /* should not be thrown */
	            }
	        }
	    }

	    return false;
	}
}
