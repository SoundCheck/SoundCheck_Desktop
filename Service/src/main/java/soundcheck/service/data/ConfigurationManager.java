package soundcheck.service.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that handles reading and saving configuration options to the config file.
 * 
 * Makes use of the Java Properties class.
 * 
 *
 */
public class ConfigurationManager {


	final static Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);

	private final static String CONFIG_FILE = "SoundCheckConfig.xml";

	private static Properties propertyManager = new Properties();


	/**
	 * Gets the value of a specified setting
	 * 
	 * @param settingName - Name of the setting to get the value for
	 * @return - The value of settingName
	 */
	public static String getConfigSetting(String settingName){

		String settingValue = propertyManager.getProperty(settingName);

		//If the settingName is not a valid config option, settingValue will be null
		if (settingValue == null) {
			logger.warn("Tried to get invalid configuration option.{}", settingName);
			settingValue = "";
		}

		return settingValue;
	}

	/**
	 * 
	 * @param settingName
	 * @param setting
	 */
	public static boolean setConfigSetting(String settingName, String settingValue){

		boolean propertySet = false;

		String currentValue = propertyManager.getProperty(settingName);

		if (currentValue == null) {
			logger.warn("Tried to set invalid configuration option.{}", settingName);
		}
		else{
			propertyManager.setProperty(settingName, settingValue);
			propertySet = true;
		}

		return propertySet;
	}

	/**
	 * Writes the changes to the XML file at CONFIG_FILE
	 */
	public static void saveSettings(){

		try {
			OutputStream fileStream = new FileOutputStream(CONFIG_FILE);			

			propertyManager.storeToXML(fileStream, null);
			
			fileStream.close();
			
		} catch (IOException e) {
			logger.error("IO Exception while saving configuration settings.", e);
		}
	}


	/**
	 * Loads the configuration file into memory so the elements can be accessed and manipulated.
	 */
	public static void loadSettings(){

		try {

			File configFile = new File(CONFIG_FILE);

			if(!configFile.exists()){
				generateDefaultConfigFile();
			}
			
			InputStream fileInput = new FileInputStream(configFile);
			
			propertyManager.loadFromXML(fileInput);
			
			fileInput.close();
			

		} catch (InvalidPropertiesFormatException e) {
			logger.error("Format of Properties file is invalid.", e);
		} catch (IOException e) {
			logger.error("IO Exception while loading configuration settings.", e);
		}
	}


	/**
	 * Creates a default configuration file
	 */
	private static void generateDefaultConfigFile() {

		Properties prop = new Properties();

		prop.setProperty("LocalName", getComputerName() );
		prop.setProperty("Library", "");
		prop.setProperty("EnabledMode", "false");
		prop.setProperty("MaxOutgoingConnections", "6");

		try {

			File defConfigFile = new File(CONFIG_FILE);

			defConfigFile.createNewFile();
			
			OutputStream fileOutput = new FileOutputStream(defConfigFile);

			prop.storeToXML(fileOutput, null);
			
			fileOutput.close();
		} 

		catch (IOException e) {
			logger.error("IO Exception while saving the default configuration file.",e);
		}			
	}

	/**
	 * Returns the name of this computer, or a predetermined
	 * string if it cannot be determined.
	 * @return
	 */
	private static String getComputerName() {
		String computerName;
		
		try {
			computerName = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e1) {
			computerName = "SoundCheck-PC";
		}

		return computerName;
	}

}
