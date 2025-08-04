package tools.vitruv.framework.remote.server.http;

public interface InternalHttpServerManager {
    int getBoundPort();
    void start() throws Exception;
    void stop() throws Exception;
}
