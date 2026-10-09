package com.cp.friend.config;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.http.server.*;
import org.springframework.web.socket.WebSocketHandler;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class SessionHandshakeInterceptorTest {
 @Test void anonymousHandshakeIsRejectedBeforeSocketUpgrade() {
  var request=new MockHttpServletRequest();var response=new MockHttpServletResponse();
  assertFalse(new SessionHandshakeInterceptor().beforeHandshake(new ServletServerHttpRequest(request),new ServletServerHttpResponse(response),mock(WebSocketHandler.class),new HashMap<>()));
  assertEquals(401,response.getStatus());assertNull(request.getSession(false));
 }
 @Test void authenticatedHttpSessionCanUpgrade() {
  var request=new MockHttpServletRequest();request.getSession().setAttribute("userId",UUID.randomUUID());var response=new MockHttpServletResponse();
  assertTrue(new SessionHandshakeInterceptor().beforeHandshake(new ServletServerHttpRequest(request),new ServletServerHttpResponse(response),mock(WebSocketHandler.class),new HashMap<>()));
 }
}
