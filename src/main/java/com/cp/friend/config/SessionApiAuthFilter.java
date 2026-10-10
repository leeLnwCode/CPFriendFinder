package com.cp.friend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * บังคับ session-based authentication ให้ทุก /api/** (ยกเว้น /api/auth/**)
 *
 * เดิม SecurityConfig เปิด permitAll ให้ /api/** ทั้งหมด ทำให้คนที่ไม่ได้ login
 * เรียก API ส่วนใหญ่ได้ (แม้บาง controller จะเช็คเองก็ตาม) — filter นี้ปิดช่องนั้น
 * ให้เป็น defense-in-depth ระดับเดียวกับ production system
 */
@Component
public class SessionApiAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        boolean isApi = path.equals("/api") || path.startsWith("/api/");
        boolean isAuthEndpoint = path.startsWith("/api/auth/");

        if (isApi && !isAuthEndpoint) {
            HttpSession session = request.getSession(false);
            Object userId = session != null ? session.getAttribute("userId") : null;

            if (!(userId instanceof UUID)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(
                        "{\"status\":401,\"message\":\"Authentication required\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
