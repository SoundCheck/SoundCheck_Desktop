package soundcheck.service.interprocess;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soundcheck.shared.Const;
import soundcheck.shared.Const.Service;
import soundcheck.shared.DataPacket;

public final class MusicPlayerConnection {
	final static Logger logger = LoggerFactory.getLogger(MusicPlayerConnection.class);

	private static ServerSocket serverSocket = null;
	private static Socket socket = null;
	private static ObjectOutputStream out = null;
	private static ObjectInputStream in = null;

	/**
	 * Start a server socket to listen for connections from the user interface
	 * @return
	 */
	public static final boolean openSocket() {
		try {
			serverSocket = new ServerSocket(Const.INTERFACE_PORT);
		} catch (IOException e) {
			logger.error("Could not listen on port {}.", Const.INTERFACE_PORT);
			return false;
		}

		try {
			logger.trace("Waiting for connection from user interface on port {}.", Const.INTERFACE_PORT);

			socket = serverSocket.accept();
			out = new ObjectOutputStream( socket.getOutputStream() );
			in = new ObjectInputStream( socket.getInputStream() );

			logger.debug("Connection to user interface established.");
		} catch (IOException e) {
			try {
				logger.error("Connection to SoundCheck user interface failed.", e);
				socket.close();
			} catch (IOException e1) {
				logger.error("", e);
			}
			return false;
		}

		return true;
	}

	/**
	 * Check if interface connection has been made
	 * @return true if connection is established
	 */
	public static final boolean isConnected() {
		boolean retVal;

		try {
			retVal = socket.isConnected();
		} catch (NullPointerException e) {
			// Socket has not yet been created. Therefore, it is closed.
			retVal = false;
		}
		return retVal;
	}

	/**
	 * Send an object to the user interface
	 * @param msg
	 */
	public synchronized static final void sendData(DataPacket packet) {
		try{
			logger.trace("Sending message to user interface.");
			out.writeObject(packet);
			out.flush();
			out.reset();
		} catch(Exception e) {
			logger.error("", e);
		}
	}

	/**
	 * Listen for data from the user interface. Blocks
	 * @return
	 */
	public static final DataPacket receiveData() {
		DataPacket recData = null;
		logger.trace("Waiting to receive from user interface.");

		try {
			// Listen for packets until valid packet is received
			do {
				recData = (DataPacket) in.readObject();
			} while( recData.getService() != Service.INTERPROCESS);
			
			logger.trace("Data received from music player.");

		} catch (IOException e) {
			try {
				logger.error("Connecting to user interface lost. Exiting program.");
				socket.close();
				serverSocket.close();
			} catch (IOException e1) {
				logger.error("Could not close sockets to user interface.", e);
			}
			System.exit(0);
		} catch (ClassNotFoundException e) {
			logger.error("", e);
		}

		return recData;
	}
}
