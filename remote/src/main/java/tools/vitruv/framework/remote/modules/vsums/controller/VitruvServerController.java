package tools.vitruv.framework.remote.modules.vsums.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.vitruv.framework.remote.modules.vsums.usecasesimpls.VitruvServerStrategyImpl;

/**
 * The VitruvServerController class provides HTTP API endpoints for retrieving information
 * about the status and availability of the Vitruv server.
 */
@RestController
@RequestMapping("/v1/vitruv-server")
@RequiredArgsConstructor
public class VitruvServerController {
  private final VitruvServerStrategyImpl vitruvServerStrategy;

  public record VitruvServerInfo(boolean isAvailable) {
  }

  /**
   * Retrieves information about the Vitruv server.
   * The method checks the availability of the server and constructs a {@code VitruvServerInfo} object
   * containing the availability status.
   *
   * @return an instance of {@code VitruvServerInfo} containing the availability status of the Vitruv server
   */
  @GetMapping
  public VitruvServerInfo getVitruvServerInfo() {
    return new VitruvServerInfo(vitruvServerStrategy.isAvailable());
  }
}
