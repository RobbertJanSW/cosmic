package com.cloud.agent.resource.consoleproxy;

import com.cloud.consoleproxy.vnc.FrameBufferCanvas;
import com.cloud.consoleproxy.vnc.RfbConstants;
import com.cloud.consoleproxy.vnc.VncClient;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ConsoleProxyVncClient bridges a VNC engine with the front-end AJAX viewer
 */
public class ConsoleProxyVncClient extends ConsoleProxyClientBase {
    private static final Logger s_logger = LoggerFactory.getLogger(ConsoleProxyVncClient.class);

    private static final int SHIFT_KEY_MASK = 64;
    private static final int CTRL_KEY_MASK = 128;
    private static final int META_KEY_MASK = 256;
    private static final int ALT_KEY_MASK = 512;

    private static final int X11_KEY_SHIFT = 0xffe1;
    private static final int X11_KEY_CTRL = 0xffe3;
    private static final int X11_KEY_ALT = 0xffe9;
    private static final int X11_KEY_META = 0xffe7;

    private VncClient client;
    private Thread worker;
    private volatile boolean workerDone = false;

    private int lastModifierStates = 0;
    private int lastPointerMask = 0;

    public ConsoleProxyVncClient() {
    }

    @Override
    public boolean isHostConnected() {
        if (this.client != null) {
            return this.client.isHostConnected();
        }

        return false;
    }

    @Override
    public boolean isFrontEndAlive() {
        if (this.workerDone || System.currentTimeMillis() - getClientLastFrontEndActivityTime() > ConsoleProxy.VIEWER_LINGER_SECONDS * 1000) {
            s_logger.info("Front end has been idle for too long");
            return false;
        }
        return true;
    }

    @Override
    public void sendClientRawKeyboardEvent(final InputEventType event, final int code, final int modifiers) {
        if (this.client == null) {
            return;
        }

        updateFrontEndActivityTime();

        switch (event) {
            case KEY_DOWN:
                sendModifierEvents(modifiers);
                this.client.sendClientKeyboardEvent(RfbConstants.KEY_DOWN, code, 0);
                break;

            case KEY_UP:
                this.client.sendClientKeyboardEvent(RfbConstants.KEY_UP, code, 0);
                sendModifierEvents(0);
                break;

            case KEY_PRESS:
                break;

            default:
                assert (false);
                break;
        }
    }

    @Override
    public void sendClientMouseEvent(final InputEventType event, final int x, final int y, final int code, final int modifiers) {
        if (this.client == null) {
            return;
        }

        updateFrontEndActivityTime();

        if (event == InputEventType.MOUSE_DOWN) {
            if (code == 2) {
                this.lastPointerMask |= 4;
            } else if (code == 0) {
                this.lastPointerMask |= 1;
            }
        }

        if (event == InputEventType.MOUSE_UP) {
            if (code == 2) {
                this.lastPointerMask ^= 4;
            } else if (code == 0) {
                this.lastPointerMask ^= 1;
            }
        }

        sendModifierEvents(modifiers);
        this.client.sendClientMouseEvent(this.lastPointerMask, x, y, code, modifiers);
        if (this.lastPointerMask == 0) {
            sendModifierEvents(0);
        }
    }

    @Override
    public void initClient(final ConsoleProxyClientParam param) {
        setClientParam(param);

        this.client = new VncClient(this);
        this.worker = new Thread(new Runnable() {
            @Override
            public void run() {
                final String tunnelUrl = getClientParam().getClientTunnelUrl();
                final String tunnelSession = getClientParam().getClientTunnelSession();

                try {
                    if (tunnelUrl != null && !tunnelUrl.isEmpty() && tunnelSession != null && !tunnelSession.isEmpty()) {
                        final URI uri = new URI(tunnelUrl);
                        s_logger.info("Connect to VNC server via tunnel. url: " + tunnelUrl + ", session: " + tunnelSession);

                        ConsoleProxy.ensureRoute(uri.getHost());
                        ConsoleProxyVncClient.this.client.connectTo(
                                uri.getHost(), uri.getPort(),
                                uri.getPath() + "?" + uri.getQuery(),
                                tunnelSession, "https".equalsIgnoreCase(uri.getScheme()),
                                getClientHostPassword());
                    } else {
                        s_logger.info("Connect to VNC server directly. host: " + getClientHostAddress() + ", port: " + getClientHostPort());
                        ConsoleProxy.ensureRoute(getClientHostAddress());
                        ConsoleProxyVncClient.this.client.connectTo(getClientHostAddress(), getClientHostPort(), getClientHostPassword());
                    }
                } catch (final IOException | URISyntaxException e) {
                    s_logger.error("Unexpected exception", e);
                }

                s_logger.info("Receiver thread stopped.");
                ConsoleProxyVncClient.this.workerDone = true;
                ConsoleProxyVncClient.this.client.getClientListener().onClientClose();
            }
        });

        this.worker.setDaemon(true);
        this.worker.start();
    }

    @Override
    public void closeClient() {
        this.workerDone = true;
        if (this.client != null) {
            this.client.shutdown();
        }
    }

    @Override
    protected FrameBufferCanvas getFrameBufferCavas() {
        if (this.client != null) {
            return this.client.getFrameBufferCanvas();
        }
        return null;
    }

    @Override
    public void onFramebufferUpdate(final int x, final int y, final int w, final int h) {
        super.onFramebufferUpdate(x, y, w, h);
        this.client.requestUpdate(false);
    }

    private void sendModifierEvents(final int modifiers) {
        if ((modifiers & SHIFT_KEY_MASK) != (this.lastModifierStates & SHIFT_KEY_MASK)) {
            this.client.sendClientKeyboardEvent((modifiers & SHIFT_KEY_MASK) != 0 ? RfbConstants.KEY_DOWN : RfbConstants.KEY_UP, X11_KEY_SHIFT, 0);
        }

        if ((modifiers & CTRL_KEY_MASK) != (this.lastModifierStates & CTRL_KEY_MASK)) {
            this.client.sendClientKeyboardEvent((modifiers & CTRL_KEY_MASK) != 0 ? RfbConstants.KEY_DOWN : RfbConstants.KEY_UP, X11_KEY_CTRL, 0);
        }

        if ((modifiers & META_KEY_MASK) != (this.lastModifierStates & META_KEY_MASK)) {
            this.client.sendClientKeyboardEvent((modifiers & META_KEY_MASK) != 0 ? RfbConstants.KEY_DOWN : RfbConstants.KEY_UP, X11_KEY_META, 0);
        }

        if ((modifiers & ALT_KEY_MASK) != (this.lastModifierStates & ALT_KEY_MASK)) {
            this.client.sendClientKeyboardEvent((modifiers & ALT_KEY_MASK) != 0 ? RfbConstants.KEY_DOWN : RfbConstants.KEY_UP, X11_KEY_ALT, 0);
        }

        this.lastModifierStates = modifiers;
    }

    @Override
    public void onClientConnected() {
    }

    @Override
    public void onClientClose() {
        s_logger.info("Received client close indication. remove viewer from map.");

        ConsoleProxy.removeViewer(this);
    }
}
