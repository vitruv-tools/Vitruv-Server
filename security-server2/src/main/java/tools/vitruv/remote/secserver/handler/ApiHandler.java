package tools.vitruv.remote.secserver.handler;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.security.openid.OpenIdAuthenticator;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.Fields;

public class ApiHandler extends Handler.Abstract.NonBlocking {
    private OpenIdAuthenticator authenticator;

    private static record InternalApiReponseContent(int statusCode, String htmlContent) {}

    public ApiHandler(OpenIdAuthenticator authenticator) {
        this.authenticator = authenticator;
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback) {
        InternalApiReponseContent responseContent;
        var path = request.getHttpURI().getPath();
        
        if (path.equals(ApiPaths.OPENID_LOGIN_PATH)) {
            var session = request.getSession(false);
            responseContent = new InternalApiReponseContent(
                HttpStatus.OK_200,
                "<p>You have received the following session id: " + session.getExtendedId() + "</p>"
                + "<p>When creating a Vitruv client, please use this session id.</p>"
            );
        } else if (path.equals(ApiPaths.OPENID_LOGOUT_PATH)) {
            this.authenticator.logout(request, response);
            return false;
        } else if (path.equals(ApiPaths.OPENID_ERROR_PATH)) {
            String errorDescription = "[No error. Maybe you visited the page directly.]";
            try {
                Fields parameters = Request.getParameters(request);
                errorDescription = parameters.get(OpenIdAuthenticator.ERROR_PARAMETER).getValue();
            } catch (Exception e) {
            }
            responseContent = new InternalApiReponseContent(HttpStatus.OK_200, "<p>Error: " + errorDescription + "</p>");
        } else if (path.equals(ApiPaths.OPENID_FULL_LOGOUT_REDIRECT_PATH)) {
            responseContent = new InternalApiReponseContent(HttpStatus.OK_200, "<p>Logout successful.</p>");
        } else {
            responseContent = new InternalApiReponseContent(HttpStatus.NOT_FOUND_404, "<p>Not implemented.</p>");
        }

        response.setStatus(responseContent.statusCode());
        Content.Sink.write(response, true, "<!DOCTYPE html><html><head></head><body>" + responseContent.htmlContent() + "</body></html>", callback);
        return true;
    }
}
