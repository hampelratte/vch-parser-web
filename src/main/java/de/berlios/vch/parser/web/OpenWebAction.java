package de.berlios.vch.parser.web;

import java.io.UnsupportedEncodingException;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import de.berlios.vch.i18n.ResourceBundleProvider;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.web.IWebAction;

@Component
@Provides
public class OpenWebAction implements IWebAction {

    @Requires(filter = "(instance.name=vch.web.parser)")
    private ResourceBundleProvider rbp;

    @Override
    public String getUri(IWebPage page) throws UnsupportedEncodingException {
        return page.getUri().toString();
    }

    @Override
    public String getTitle() {
        return rbp.getResourceBundle().getString("I18N_OPEN");
    }

}
