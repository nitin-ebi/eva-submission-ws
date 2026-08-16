package uk.ac.ebi.eva.submission.controller.swagger;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SwaggerInterceptAdapter implements HandlerInterceptor {

    @Value("${server.servlet.context-path:/}")
    private String contextPath;

    @Override
    /**
     * Redirecting to the swagger home page*/
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        String req = request.getRequestURI();

        if (req.equals(contextPath) || req.equals(contextPath + "/") ||
                req.equals(contextPath + "/v1") || req.equals(contextPath + "/v1/")) {
            response.sendRedirect(contextPath + "/swagger-ui/index.html");
            return false;
        }

        return true;
    }
}
