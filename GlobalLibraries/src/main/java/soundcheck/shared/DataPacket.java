package soundcheck.shared;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soundcheck.shared.Const.Command;
import soundcheck.shared.Const.Service;

public class DataPacket implements Serializable {
	private static final long serialVersionUID = -2574263355241733207L;
	final static Logger logger = LoggerFactory.getLogger(DataPacket.class);

	// Packet header information
	private String project = Const.PROJECT_NAME;
	private Service service;

	// Packet data
	private Command com;
	private Zone zone;
	private Object data;

	/**
	 * Default constructor
	 */
	public DataPacket() {
		this(null);
	}

	/**
	 * Constructor for header fields.
	 * @param service The part of the project this packet is intended for.
	 */
	public DataPacket(Service service) {
		this(service, null, null);
	}

	/**
	 * Constructor for header fields and data fields.
	 * @param service The part of the project this packet is intended for.
	 * @param com A command to deliver to the recipient
	 * @param data The Object to deliver to the recipient
	 */
	public DataPacket(Service service, Command com, Object data) {
		this(service, com, data, null);
	}
	
	/**
	 * Constructor for header fields and data fields.
	 * @param service The part of the project this packet is intended for.
	 * @param com A command to deliver to the recipient
	 * @param data The Object to deliver to the recipient
	 * @param zone The zone that this data will be affecting
	 */
	public DataPacket(Service service, Command com, Object data, Zone zone) {
		this.service = service;
		this.com = com;
		this.data = data;
		this.zone = zone;
	}

	/**
	 * Getter for data
	 * @return Object Class stored in this transport object.
	 */
	public Object getData() {
		return data;
	}

	/**
	 * Set data
	 * @param data
	 */
	public void setData(Object data) {
		this.data = data;
	}

	/**
	 * @return the project
	 */
	public String getProject() {
		return project;
	}

	/**
	 * @param project The project this packet is for.
	 */
	public void setProject(String project) {
		this.project = project;
	}

	/**
	 * @return the service
	 */
	public Service getService() {
		return service;
	}

	/**
	 * @param service The part of the project this packet is intended for.
	 */
	public void setService(Service service) {
		this.service = service;
	}

	/**
	 * @return the command
	 */
	public Command getCommand() {
		return com;
	}

	/**
	 * @param com A command to deliver to the recipient
	 */
	public void setCommand(Command com) {
		this.com = com;
	}

	/**
	 * @return the zone
	 */
	public Zone getZone() {
		return zone;
	}

	/**
	 * @param zone the zone to set
	 */
	public void setZone(Zone zone) {
		this.zone = zone;
	}
}
