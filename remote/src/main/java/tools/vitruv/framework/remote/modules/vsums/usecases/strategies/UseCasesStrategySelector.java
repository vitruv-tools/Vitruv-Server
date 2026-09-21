package tools.vitruv.framework.remote.modules.vsums.usecases.strategies;

import java.util.UUID;

/**
 * This interface provides a mechanism to dynamically select and retrieve specific strategy
 * implementations for VSUM and View use cases based on a given unique identifier (UUID).
 * It enables operations that are categorized under VSUM and View management use cases,
 * delegating to the appropriate strategy implementation.
 */
public interface UseCasesStrategySelector {
    /**
     * Retrieves the VsumUseCasesStrategy implementation associated with the given UUID.
     * The strategy is used to perform operations related to VSUM use cases, such as
     * retrieving view types or deleting a VSUM.
     *
     * @param id the unique identifier of an Entity that determines which the strategy is to be retrieved.
     * @return an instance of VsumUseCasesStrategy that can be used to perform operations related to VSUMs.
     */
    VsumUseCasesStrategy getVsumStrategy(UUID id);

    /**
     * Retrieves the ViewUseCasesStrategy implementation associated with the given UUID.
     * The strategy provides operations related to managing views, such as creating view selectors,
     * opening views, committing resource sets, and closing views.
     *
     * @param id the unique identifier of an Entity that determines which the strategy is to be retrieved.
     * @return an instance of ViewUseCasesStrategy that can be used to perform operations related to views.
     */
    ViewUseCasesStrategy getViewStrategy(UUID id);
}
