package soundcheck.musicPlayer.interprocess;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soundcheck.shared.Const;
import soundcheck.shared.Const.Service;
import soundcheck.shared.DataPacket;

public class ServiceConnection {
	final static Logger logger = LoggerFactory.getLogger(ServiceConnection.class);

	private Socket socket = null;
	private ObjectOutputStream out = null;
	private ObjectInputStream in = null;

	public ServiceConnection() {
		if( establishConnection() == false ) {
			logger.error("Could not find service.");
		}
	}

	/**
	 * Establish a connection to the service portion of the SoundCheck
	 * application. The service must be running before this is called, and
	 * must be running on the same host.
	 * @return
	 */
	private boolean establishConnection() {
		try {
			logger.trace("Connecting to service.");

			socket = new Socket("localhost", Const.INTERFACE_PORT);
			out = new ObjectOutputStream( socket.getOutputStream() );
			in = new ObjectInputStream( socket.getInputStream() );

		} catch (UnknownHostException e) {
			logger.error("Could not find host.", e);
			return false;
		} catch (IOException e) {
			logger.error("IO Problem communicating with service.",e);
			return false;
		}
		return true;
	}

	/**
	 * Send a data packet to the service
	 * @param transportObject
	 */
	public void sendData(DataPacket transportObject) {
		try{
			out.writeObject(transportObject);
			out.flush();
		} catch(Exception e) {
			logger.error("", e);
		}
	}

	/**
	 * Receive a string over the socket connection from the service. Blocks.
	 * @return
	 */
	public DataPacket receiveData() {
		logger.trace("Waiting to receive data from service.");
		DataPacket recData = null;

		try {
			// Listen for packets until valid packet is received
			do {
				recData = (DataPacket) in.readObject();
			} while( recData.getService() != Service.INTERPROCESS);

			logger.trace("Data received from service.");

		} catch (IOException e) {
			//A SocketException generally means that the Service is no longer running.
			if(e instanceof SocketException){
				logger.error("Service connection reset, Exiting the program.");

				//TODO Indicate to the user that we are closing.

				System.exit(0);  //Close the program as it cannot function without a connection to the service.
			}

			if(e instanceof EOFException) {
				// Close and reestablish connection
				logger.warn("Closing and reestablishing service connection.");
				try {
					socket.close();
					socket = new Socket("localhost", Const.INTERFACE_PORT);
					out = new ObjectOutputStream( socket.getOutputStream() );
					in = new ObjectInputStream( socket.getInputStream() );
				} catch( IOException ioerror) {
					logger.error("",ioerror);
				}
			}
			try {
				logger.error("Error reading object from service.", e);
				socket.close();
			} catch (IOException e1) {
				logger.error("", e);
			}
		} catch (ClassNotFoundException e) {
			logger.error("", e);
		}

		return recData;
	}

	/**
	 * Check if interface connection has been made
	 * @return true if connection is established
	 */
	public boolean isConnected() {
		boolean retVal;

		try {
			retVal = socket.isConnected();
		} catch (NullPointerException e) {
			// Socket has not yet been created. Therefore, it is closed.
			retVal = false;
		}
		return retVal;
	}
}
