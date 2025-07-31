package tools.vitruv.framework.remote.common;

/**
 * Defines default settings for the connection between a Vitruvius server and client.
 * They are only used if no other settings are provided.
 */
public class DefaultConnectionSettings {
	/**
	 * @deprecated Due to different HTTP versions and features, a String is not suitable to represent the protocol.
	 */
	@Deprecated
	public static final String STD_PROTOCOL = "http";
	public static final String STD_HOST = "localhost";
	public static final int STD_PORT = 8080;
	
	private DefaultConnectionSettings() {}
}
