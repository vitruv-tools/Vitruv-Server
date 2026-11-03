package tools.vitruv.framework.remote.server.rest.endpoints;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.vitruv.framework.remote.common.json.JsonMapper;
import tools.vitruv.framework.remote.server.exception.ServerHaltingException;
import tools.vitruv.framework.remote.server.http.HttpWrapper;
import tools.vitruv.framework.vsum.VirtualModel;
import tools.vitruv.framework.vsum.constraints.ConstraintDescriptor;
import tools.vitruv.framework.vsum.constraints.ConstraintSeverity;
import tools.vitruv.framework.vsum.constraints.UnknownConstraintException;

/** Validations of the {@link ValidateConstraintsEndpoint} that race with changes of constraints. */
class ValidateConstraintsEndpointTest {
  private static final String ID = "ecore::EPackage::lowercase";

  @TempDir Path folder;

  @Test
  void constraintDeletedDuringTheRequestIsNotFound() throws Exception {
    VirtualModel model = mock(VirtualModel.class);
    when(model.getConstraints())
        .thenReturn(
            List.of(
                new ConstraintDescriptor(
                    ID, null, "ecore::EPackage", null, ConstraintSeverity.CRITICAL, "test", null)));
    // deleted after the ids were checked, before the validation
    when(model.validateConstraints(anyCollection())).thenThrow(new UnknownConstraintException(ID));
    HttpWrapper request = mock(HttpWrapper.class);
    when(request.getRequestBodyAsString()).thenReturn("{\"constraintIds\": [\"" + ID + "\"]}");
    ValidateConstraintsEndpoint endpoint =
        new ValidateConstraintsEndpoint(model, new JsonMapper(folder));

    ServerHaltingException exception =
        assertThrows(ServerHaltingException.class, () -> endpoint.process(request));

    assertEquals(HTTP_NOT_FOUND, exception.getStatusCode());
  }
}
