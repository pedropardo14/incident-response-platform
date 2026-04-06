package com.accenture.iris.functions;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for all IRIS flows.
 * Logs the error and returns a structured error response.
 */
@PreLoad(route = "v1.iris.exception", instances = 10)
public class IncidentException implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {
    private static final Logger log = LoggerFactory.getLogger(IncidentException.class);

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        Object stack = input.get("stack");
        if (stack != null) {
            var stackMap = Utility.getInstance().stackTraceToMap(stack.toString());
            log.error("IRIS flow exception: {}", stackMap);
        }

        Object status = input.get("status");
        Object message = input.get("message");

        log.error("Flow error - status={} message={}", status, message);

        Map<String, Object> error = new HashMap<>();
        error.put("type", "error");
        error.put("status", status != null ? status : 500);
        error.put("message", message != null ? message.toString() : "Internal error");
        return error;
    }
}