package tools.vitruv.remote.secserver.builder;

public class VitruvSecurityServer2Builder {
    private VitruvSecurityServer2Builder() {}

    public static ServerConnectionConfigurator createBuilder() {
        return new ServerConnectionConfigurator();
    }
}
