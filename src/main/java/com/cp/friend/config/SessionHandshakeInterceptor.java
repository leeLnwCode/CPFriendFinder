package com.cp.friend.config;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.*;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
public class SessionHandshakeInterceptor implements HandshakeInterceptor {
 @Override public boolean beforeHandshake(ServerHttpRequest request,ServerHttpResponse response,WebSocketHandler handler,Map<String,Object> attributes) {
  if(request instanceof ServletServerHttpRequest servlet) {
   var session=servlet.getServletRequest().getSession(false);
   if(session!=null && session.getAttribute("userId") instanceof UUID) return true;
  }
  response.setStatusCode(HttpStatus.UNAUTHORIZED);return false;
 }
 @Override public void afterHandshake(ServerHttpRequest request,ServerHttpResponse response,WebSocketHandler handler,Exception exception) {}
}
