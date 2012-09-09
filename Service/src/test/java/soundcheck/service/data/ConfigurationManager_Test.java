/**
 * 
 */
package soundcheck.service.data;

import soundcheck.service.data.ConfigurationManager;
import junit.framework.TestCase;

/**
 * 
 */
public class ConfigurationManager_Test extends TestCase {
	
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		
		ConfigurationManager.loadSettings();
		
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
		
		//cm.saveSettings();
	}

	/**
	 * Test method for {@link soundcheck.service.data.ConfigurationManager#getConfigSetting(java.lang.String)}.
	 */
	public void testGetConfigSetting() {
		try{
			String result = ConfigurationManager.getConfigSetting("LocalName");  //Valid option
			
			if(result.length() == 0){
				fail("String of length 0 returned");
			}
			
			result = ConfigurationManager.getConfigSetting("Invalid_Option");  //Invalid option
			
			if(result.length() > 0){
				fail("String with length > 0 returned for an invalid option.");
			}
		}
		catch(NullPointerException e){
			fail("Null Pointer Caught");
		}
	}

	/**
	 * Test method for {@link soundcheck.service.data.ConfigurationManager#setConfigSetting(java.lang.String, java.lang.String)}.
	 */
	public void testSetConfigSetting() {

		boolean propertySet;

		propertySet = ConfigurationManager.setConfigSetting("LocalName", "unitTest");
		
		assertEquals(propertySet, true);

		propertySet = ConfigurationManager.setConfigSetting("InvalidOption", "invalid");
		
		assertEquals(propertySet, false);
	}

	/**
	 * Test method for {@link soundcheck.service.data.ConfigurationManager#saveSettings()}.
	 */
	public void testSaveSettings() {
		//fail("Not yet implemented");
	}

	/**
	 * Test method for {@link soundcheck.service.data.ConfigurationManager#loadSettings()}.
	 */
	public void testLoadSettings() {
		//fail("Not yet implemented");
	}

}
