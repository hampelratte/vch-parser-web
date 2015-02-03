package de.berlios.vch.parser.web;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Unbind;

import de.berlios.vch.i18n.ResourceBundleProvider;
import de.berlios.vch.net.INetworkProtocol;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.web.IWebAction;

@Component
@Provides
public class WatchWebAction implements IWebAction {

    @Requires(filter = "(instance.name=vch.web.parser)")
    private ResourceBundleProvider rbp;

    private Set<INetworkProtocol> protocols = new HashSet<INetworkProtocol>();

    @Override
    public String getUri(IWebPage page) throws UnsupportedEncodingException, URISyntaxException {
        if (page instanceof IVideoPage) {
            IVideoPage video = (IVideoPage) page;
            URI videoUri = video.getVideoUri();
            for (INetworkProtocol proto : protocols) {
                String scheme = videoUri.getScheme();
                if (proto.getSchemes().contains(scheme)) {
                    if (proto.isBridgeNeeded()) {
                        return proto.toBridgeUri(videoUri, video.getUserData()).toString();
                    }
                }
            }
            return video.getVideoUri().toString();
        } else {
            throw new IllegalArgumentException("Not a video");
        }
    }

    @Override
    public String getTitle() {
        return rbp.getResourceBundle().getString("I18N_WATCH");
    }

    @Bind(id = "protocols", aggregate = true)
    public synchronized void addProtocol(INetworkProtocol protocol) {
        protocols.add(protocol);
    }

    @Unbind(id = "protocols", aggregate = true)
    public synchronized void removeProtocol(INetworkProtocol protocol) {
        protocols.remove(protocol);
    }
}
