package tools.vitruv.remote.seccommon;

import tools.vitruv.framework.remote.common.DefaultConnectionSettings;

public final class AddressBinderUtil {
    private AddressBinderUtil() {}

    public static String getAddressForBinding(String ipOrHost) {
        return ipOrHost.equals(DefaultConnectionSettings.STD_HOST) ? ipOrHost : "0.0.0.0";
    }
}
