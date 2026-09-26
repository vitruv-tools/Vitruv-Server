package tools.vitruv.framework.remote.modules.vsums.usecasesimpls;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.vitruv.framework.remote.modules.vsums.usecases.strategies.UseCasesStrategySelector;
import tools.vitruv.framework.remote.modules.vsums.usecases.strategies.ViewUseCasesStrategy;
import tools.vitruv.framework.remote.modules.vsums.usecases.strategies.VsumUseCasesStrategy;
import tools.vitruv.framework.remote.modules.vsums.usecasesimpls.helpers.VitruvServerIdsManager;

import java.util.UUID;

/**
 * Implementation of the UseCasesStrategySelector interface. This class determines and selects
 * the appropriate strategy for performing View and VSUM use case operations based on a given
 * UUID. It facilitates the delegation of tasks to internal or server-specific strategy
 * implementations for handling domain-specific use case logic.
 * <p>
 * The selection mechanism ensures that the correct strategy is chosen, depending on whether the
 * provided UUID is associated with a Vitruv server-managed entity or an internal entity. This
 * allows for modular and extensible management of different use case strategies.
 * <p>
 * Dependencies injected into this class are responsible for managing and executing the specific
 * logic for their respective use cases.
 */
@Service
@RequiredArgsConstructor
public class UseCasesStrategySelectorImpl implements UseCasesStrategySelector {
  private final InternalViewStrategyImpl internalViewStrategy;
  private final InternalVsumStrategyImpl internalVsumStrategy;
  private final VitruvServerStrategyImpl vitruvServerStrategy;
  private final VitruvServerIdsManager vitruvServerIdsManager;

  /**
   * {@inheritDoc}
   */
  @Override
  public VsumUseCasesStrategy getVsumStrategy(UUID id) {
    if (vitruvServerIdsManager.hasId(id)) {
      return vitruvServerStrategy;
    }

    return internalVsumStrategy;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ViewUseCasesStrategy getViewStrategy(UUID id) {
    if (vitruvServerIdsManager.hasId(id)) {
      return vitruvServerStrategy;
    }

    return internalViewStrategy;
  }
}
