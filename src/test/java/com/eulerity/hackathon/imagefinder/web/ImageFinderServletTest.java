package com.eulerity.hackathon.imagefinder.web;

import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ImageFinderServlet} focusing on error-handling behavior for invalid input cases.
 */
public class ImageFinderServletTest {

    private HttpServletRequest request;
    private HttpServletResponse response;
    private StringWriter sw;
    private ImageFinderServlet servlet;

    /**
     * Sets up mock servlet request/response and output writer before each test.
     */
    @Before
    public void setUp() throws IOException {
        request  = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        sw       = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));
        servlet  = new ImageFinderServlet();
    }

    /**
     * Tests that a missing 'url' parameter returns HTTP 400 and a JSON error message.
     */
    @Test
    public void testDoPost_withNoUrl_returns400AndJsonError()
            throws ServletException, IOException {
        when(request.getParameter("url")).thenReturn(null);

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);

        ImageResponse expected = new ImageResponse(
            Collections.emptyList(),
            "Missing 'url' parameter."
        );
        String expectedJson = new Gson().toJson(expected);
        assertEquals(expectedJson, sw.toString().trim());
    }

    /**
     * Tests that an empty 'url' parameter returns HTTP 400 and a JSON error message.
     */
    @Test
    public void testDoPost_withEmptyUrl_returns400AndJsonError()
            throws ServletException, IOException {
        when(request.getParameter("url")).thenReturn("   ");

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);

        ImageResponse expected = new ImageResponse(
            Collections.emptyList(),
            "Missing 'url' parameter."
        );
        String expectedJson = new Gson().toJson(expected);
        assertEquals(expectedJson, sw.toString().trim());
    }

    /**
     * Tests that an unsupported URL protocol (e.g. FTP) returns HTTP 400 and a JSON error message.
     */
    @Test
    public void testDoPost_withInvalidProtocol_returns400AndJsonError()
            throws ServletException, IOException {
        when(request.getParameter("url")).thenReturn("ftp://invalid.com");

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);

        ImageResponse expected = new ImageResponse(
            Collections.emptyList(),
            "Invalid URL format."
        );
        String expectedJson = new Gson().toJson(expected);
        assertEquals(expectedJson, sw.toString().trim());
    }
}
