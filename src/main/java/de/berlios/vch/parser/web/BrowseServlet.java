package de.berlios.vch.parser.web;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceException;
import org.osgi.service.http.HttpService;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

import de.berlios.vch.i18n.ResourceBundleProvider;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.IWebParser;
import de.berlios.vch.parser.OverviewPage;
import de.berlios.vch.parser.exceptions.NoSupportedVideoFoundException;
import de.berlios.vch.parser.service.IParserService;
import de.berlios.vch.web.IWebAction;
import de.berlios.vch.web.ResourceHttpContext;
import de.berlios.vch.web.TemplateLoader;
import de.berlios.vch.web.servlets.VchHttpServlet;

// TODO neuere Version vom jquery tree mit besserem ajax error handling probieren. Im moment wird immer nur undefined error ausgegeben
@Component
public class BrowseServlet extends VchHttpServlet {

    public static final String PATH = "/parser";

    public static final String STATIC_PATH = PATH + "/static";

    @Requires
    private IParserService parserService;

    @Requires
    private LogService logger;

    @Requires
    private TemplateLoader templateLoader;

    @Requires(filter = "(instance.name=vch.web.parser)")
    private ResourceBundleProvider rbp;

    @Requires
    private HttpService http;

    private BundleContext ctx;

    public BrowseServlet(BundleContext ctx) {
        this.ctx = ctx;
    }

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String parserId = req.getParameter("id");
        IWebParser parser = parserService.getParser(parserId);
        if (parser != null || req.getParameter("listparsers") != null) {
            if ("XMLHttpRequest".equals(req.getHeader("X-Requested-With"))) {
                try {
                    if (req.getParameter("listparsers") != null) {
                        IOverviewPage parsers = parserService.getParserOverview();
                        String response = toJSON(parsers.getPages());
                        resp.setContentType("application/json; charset=utf-8");
                        resp.getWriter().println(response);
                    } else {
                        URI vchpage = new URI(req.getParameter("uri"));
                        IWebPage parsedPage = parserService.parse(vchpage);

                        if (parsedPage != null) {
                            String response = "";
                            if (parsedPage instanceof IOverviewPage) {
                                IOverviewPage overview = (IOverviewPage) parsedPage;
                                response = toJSON(overview.getPages());
                            } else {
                                response = toJSON(parsedPage, false);
                                List<IWebAction> webActions = getWebActions();
                                Collections.sort(webActions, new Comparator<IWebAction>() {
                                    @Override
                                    public int compare(IWebAction o1, IWebAction o2) {
                                        return o1.getTitle().compareTo(o2.getTitle());
                                    }
                                });
                                String actions = actionsToJSON(webActions, parsedPage);

                                response = "{\"video\":" + response + "," + "\"actions\":" + actions + "}";

                                logger.log(LogService.LOG_INFO, webActions.size() + " web actions available");
                                logger.log(LogService.LOG_DEBUG, actions);
                            }
                            resp.setContentType("application/json; charset=utf-8");
                            resp.getWriter().println(response);
                        } else {
                            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                            resp.getWriter().print("Couldn't load page");
                        }
                    }
                } catch (NoSupportedVideoFoundException e) {
                    logger.log(LogService.LOG_WARNING, "Couldn't load page: " + e.getLocalizedMessage());
                    String msg = rbp.getResourceBundle().getString("no_supported_video_format");
                    throw new ServletException(msg, e);
                    // error(resp, HttpServletResponse.SC_PRECONDITION_FAILED, msg, true);
                } catch (Exception e) {
                    logger.log(LogService.LOG_ERROR, "Couldn't load page", e);
                    throw new ServletException(e);
                    // error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage(), true);
                }
            } else {
                logger.log(LogService.LOG_INFO, "Using " + parser.getTitle() + " parser [" + parserId + "]");
                Map<String, Object> params = new HashMap<String, Object>();
                params.put("TITLE", parser.getTitle());
                params.put("SERVLET_URI", req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort() + req.getServletPath());
                params.put("PARSER", parserId);

                // add css and javascript for the treeview and for log console
                List<String> css = new ArrayList<String>();
                css.add(BrowseServlet.STATIC_PATH + "/jstree/themes/themeroller/style.css");
                css.add(BrowseServlet.STATIC_PATH + "/parser.css");
                params.put("CSS_INCLUDES", css);
                List<String> js = new ArrayList<String>();
                js.add(BrowseServlet.STATIC_PATH + "/jstree/jquery.tree.js");
                js.add(BrowseServlet.STATIC_PATH + "/jstree/plugins/jquery.tree.themeroller.js");
                params.put("JS_INCLUDES", js);

                try {
                    IOverviewPage page = new OverviewPage();
                    page.setTitle(parser.getTitle());
                    page.setParser(parserId);
                    page.setUri(new URI("vchpage://localhost/" + parserId));
                    page.setVchUri(new URI("vchpage://localhost/" + parserId));
                    params.put("PAGE", page);
                } catch (Exception e) {
                    logger.log(LogService.LOG_ERROR, "Couldn't parse root page", e);
                    throw new ServletException("Couldn't parse root page", e);
                }

                String page = templateLoader.loadTemplate("parser.ftl", params);
                resp.getWriter().print(page);
            }
        } else {
            String msg = "Parser with id " + parserId + " is not available";
            logger.log(LogService.LOG_ERROR, msg);
            throw new ServletException(new ServiceException(msg));
            // error(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Parser with id " + parserId +
            // " is not available",
            // "XMLHttpRequest".equals(req.getHeader("X-Requested-With")));
        }
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        get(req, resp);
    }

    private String toJSON(IWebPage page, boolean isOverview) throws JSONException {
        // create the data object
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("title", page.getTitle());

        // create the attributes object
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("id", page.getVchUri());

        // set the title
        Map<String, Object> object = new HashMap<String, Object>();
        object.put("data", data);
        object.put("attributes", attributes);
        if (page instanceof IOverviewPage) {
            object.put("state", "closed");
        }

        if (page instanceof IVideoPage) {
            IVideoPage vpage = (IVideoPage) page;
            if (!isOverview && vpage.getVideoUri() != null) {
                attributes.put("vchvideo", vpage.getVideoUri().toString());
            }
            if (vpage.getUri() != null) {
                attributes.put("vchlink", vpage.getUri().toString());
            }
            if (vpage.getDescription() != null) {
                attributes.put("vchdesc", vpage.getDescription());
            }
            if (vpage.getThumbnail() != null) {
                attributes.put("vchthumb", vpage.getThumbnail().toString());
            }
            if (vpage.getPublishDate() != null) {
                attributes.put("vchpubDate", vpage.getPublishDate().getTimeInMillis());
            }
            if (vpage.getDuration() > 0) {
                attributes.put("vchduration", vpage.getDuration());
            }
            attributes.put("vchisLeaf", true);
        }
        return new JSONObject(object).toString();
    }

    private String toJSON(List<IWebPage> pages) throws JSONException {
        if (!pages.isEmpty()) {
            String json = "[";
            for (Iterator<IWebPage> iterator = pages.iterator(); iterator.hasNext();) {
                IWebPage page = iterator.next();
                json += toJSON(page, true);
                if (iterator.hasNext()) {
                    json += ", ";
                }
            }
            return json += "]";
        } else {
            return "[]";
        }
    }

    private String actionsToJSON(List<IWebAction> webActions, IWebPage page) throws UnsupportedEncodingException, URISyntaxException {
        if (!webActions.isEmpty()) {
            String json = "[";
            for (Iterator<IWebAction> iterator = webActions.iterator(); iterator.hasNext();) {
                IWebAction action = iterator.next();
                json += toJSON(action, page);
                if (iterator.hasNext()) {
                    json += ", ";
                }
            }
            return json += "]";
        } else {
            return "[]";
        }
    }

    private String toJSON(IWebAction action, IWebPage page) throws UnsupportedEncodingException, URISyntaxException {
        Map<String, Object> object = new HashMap<String, Object>();
        object.put("title", action.getTitle());
        object.put("uri", action.getUri(page));
        return new JSONObject(object).toString();
    }

    private List<IWebAction> getWebActions() {
        List<IWebAction> actions = new LinkedList<IWebAction>();

        ServiceTracker actionsTracker = new ServiceTracker(ctx, IWebAction.class.getName(), null);
        actionsTracker.open();
        Object[] services = actionsTracker.getServices();
        actionsTracker.close();

        if (services != null) {
            for (Object object : services) {
                IWebAction action = (IWebAction) object;
                actions.add(action);
            }
        }

        return actions;
    }

    @Validate
    public void start() throws Exception {
        // register the browse servlet
        http.registerServlet(PATH, this, null, null);

        // register resource context for static files
        ResourceHttpContext resourceHttpContext = new ResourceHttpContext(ctx, logger);
        http.registerResources(STATIC_PATH, "/htdocs", resourceHttpContext);
    }

    @Invalidate
    public void stop() throws Exception {
        if (http != null) {
            http.unregister(BrowseServlet.PATH);
            http.unregister(BrowseServlet.STATIC_PATH);
        }
    }
}
