package com.ggx.editor.editor.preview;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.scene.web.WebView;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.html.HTMLAnchorElement;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;

public class HyperlinkRedirectListener implements ChangeListener<Worker.State>,EventListener {

    private static final Logger LOGGER=Logger.getLogger(HyperlinkRedirectListener.class.getSimpleName());

    private static final String CLICK_EVENT = "click";
    private static final String ANCHOR_TAG = "a";
    private final WebView webView;

    public HyperlinkRedirectListener(WebView webView) {
        this.webView = webView;
    }

    @Override
    public void changed(ObservableValue<? extends Worker.State> observable, Worker.State oldValue, Worker.State newValue) {
        if (Worker.State.SUCCEEDED.equals(newValue)) {
            Document document = webView.getEngine().getDocument();
            NodeList anchors = document.getElementsByTagName(ANCHOR_TAG);
            for (int i = 0; i < anchors.getLength(); i++) {
                Node node = anchors.item(i);
                HTMLAnchorElement anchorElement= (HTMLAnchorElement) node;
                String href=anchorElement.getHref();
                if(!(href.startsWith("http://")
                        ||href.startsWith("https://"))){
                    continue;
                }
                EventTarget eventTarget = (EventTarget) node;
                eventTarget.addEventListener(CLICK_EVENT, this, false);
            }
        }
    }

    @Override
    public void handleEvent(Event evt) {
        HTMLAnchorElement anchorElement= (HTMLAnchorElement) evt.getCurrentTarget();
        String href=anchorElement.getHref();
        openPlatformBrowser(href);
        evt.preventDefault();
    }
    private String[] linuxBrowsers = { "epiphany", "firefox", "mozilla", "konqueror",
            "netscape", "opera", "links", "lynx" };
    private void openPlatformBrowser(String href){
        String os=System.getProperty("os.name").toLowerCase();
        if(os.contains("win")){
            if(Desktop.isDesktopSupported()){
                URI uri= null;
                try {
                    uri = new URI(href);
                    Desktop.getDesktop().browse(uri);
                } catch (URISyntaxException | IOException e) {
                    e.printStackTrace();
                }
            }
        }else if(os.contains("mac")){
            Runtime rt = Runtime.getRuntime();
            try {
                rt.exec("open " + href);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else if(os.contains("nix")||os.contains("nux")){
            Runtime rt = Runtime.getRuntime();
            StringBuilder cmd = new StringBuilder();
            cmd.append(String.format(    "%s \"%s\"", linuxBrowsers[0], href));
            // If the first didn't work, try the next browser and so on
            for (int i = 1; i < linuxBrowsers.length; i++)
                    cmd.append(String.format(" || %s \"%s\"", linuxBrowsers[i], href));

            try {
                rt.exec(new String[] { "sh", "-c", cmd.toString() });
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
