package com.eulerity.hackathon.imagefinder.web;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Servlet filter that adds Cross-Origin Resource Sharing (CORS) headers to HTTP responses.
 * Enables requests from any origin and supports GET, POST, and OPTIONS methods.
 */
@WebFilter(
    urlPatterns = "/*",
    asyncSupported = true
)
public class CORSFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
        // No initialization needed
    }

    /**
     * Adds CORS headers to the response. Handles preflight OPTIONS requests by returning 200 OK.
     *
     * @param req the incoming ServletRequest
     * @param res the outgoing ServletResponse
     * @param chain the filter chain to continue processing
     * @throws IOException if an I/O error occurs during filtering
     * @throws ServletException if a servlet error occurs during filtering
     */
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;
        HttpServletRequest  request  = (HttpServletRequest) req;

        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        chain.doFilter(req, res);
    }

    @Override
    public void destroy() {
        // No cleanup needed
    }
}
