package tools.vitruv.framework.remote.common.rest.constraints;

/**
 * An element of the virtual model violating a constraint.
 *
 * @param uuid the UUID of the element in the virtual model, which is also its {@code _id} in the
 *     returned models; {@code null} if the element has no UUID
 * @param eClass the qualified type of the element, e.g. {@code model::Component}
 * @param resource the URI of the model containing the element, relative to the project root; equal
 *     to the {@code uri} of the model in the returned models
 * @param severity the severity of the violation
 * @param message a human-readable explanation, may be {@code null}
 */
public record ViolationDto(
    String uuid, String eClass, String resource, String severity, String message) {}
