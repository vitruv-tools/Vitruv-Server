package tools.vitruv.framework.remote.common;

public final class AddressBinderUtil {
    private AddressBinderUtil() {}

    public static String getAddressForBinding(String ipOrHost) {
        return ipOrHost.equals(DefaultConnectionSettings.STD_HOST) ? ipOrHost : "0.0.0.0";
    }
}
