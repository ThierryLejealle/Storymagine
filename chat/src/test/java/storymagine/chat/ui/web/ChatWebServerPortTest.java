package storymagine.chat.ui.web;

import org.junit.jupiter.api.Test;
import storymagine.chat.coeur.service.ChatService;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Port fallback of ChatWebServer : lets several chats run side by side. The servers started here
 * are never stopped (no stop API) ; they die with the test JVM.
 */
class ChatWebServerPortTest {

    @Test
    void usesNextFreePortWhenPreferredOneIsTaken() throws IOException {
        try (ServerSocket taken = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) {
            int preferred = taken.getLocalPort();

            int bound = new ChatWebServer(stubService(), Path.of("."), null, preferred).start();

            assertNotEquals(preferred, bound);
            assertTrue(bound > preferred, "le repli doit prendre un port superieur, pas inferieur");
        }
    }

    @Test
    void usesPreferredPortWhenFree() throws IOException {
        int preferred;
        try (ServerSocket probe = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) {
            preferred = probe.getLocalPort();
        }

        int bound = new ChatWebServer(stubService(), Path.of("."), null, preferred).start();

        assertEquals(preferred, bound);
    }

    /** Only contextWindow() is called by the constructor ; every other method is unused here. */
    private static ChatService stubService() {
        return (ChatService) Proxy.newProxyInstance(
            ChatService.class.getClassLoader(), new Class<?>[]{ChatService.class},
            (proxy, method, args) -> method.getReturnType() == int.class ? 0 : null);
    }
}
