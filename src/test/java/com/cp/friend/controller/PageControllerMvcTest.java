package com.cp.friend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class PageControllerMvcTest {

    private final PageController controller = new PageController();

    @Test
    void entry_redirectsToLogin() throws Exception {
        assertArrayEquals(new String[]{"/"}, PageController.class.getDeclaredMethod("entry").getAnnotation(GetMapping.class).value());
        assertEquals("redirect:/login", controller.entry());
    }

    @Test
    void loginPage_mapsToLoginView() throws Exception {
        Method method =
                PageController.class.getDeclaredMethod("login");

        GetMapping mapping =
                method.getAnnotation(GetMapping.class);

        assertNotNull(mapping);
        assertArrayEquals(
                new String[]{"/login"},
                mapping.value()
        );
        assertEquals("login", controller.login());
    }

    @Test
    void registerPage_mapsToRegisterView() throws Exception {
        Method method =
                PageController.class.getDeclaredMethod("register");

        GetMapping mapping =
                method.getAnnotation(GetMapping.class);

        assertNotNull(mapping);
        assertArrayEquals(
                new String[]{"/register"},
                mapping.value()
        );
        assertEquals("register", controller.register());
    }
}
