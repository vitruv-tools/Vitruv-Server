package tools.vitruv.framework.remote.modules.vsums.usecases.strategies;

import java.util.UUID;

public interface VsumUseCasesStrategy {
    String[] getViewTypes(UUID vsumId);

    void deleteVsum(UUID vsumId);
}
