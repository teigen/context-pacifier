package contextpacifier;

import org.xml.sax.*;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class Filter implements javax.servlet.Filter {
    FilterConfig config;
    ServletContext context;
    Runmode runmode;
    Pattern pattern;
    ContextPacifier pacifier;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        config = filterConfig;
        context = filterConfig.getServletContext();

        String contextPath = orElse(get("contextpath"), context.getContextPath());
        boolean compile    = truthy(get("compile"));
        boolean js         = truthy(get("javascript"));
        pattern = Pattern.compile(orElse(get("pattern"), "^(?!/WEB-INF/).+\\.(?i)(html)"));

        pacifier = new ContextPacifier(contextPath, ContextPacifier.rewrite, js);

        try{
            if(compile){
                runmode = new Compile();
            } else {
                runmode = new Interpret();
            }
        } catch(IOException e){
            throw new ServletException(e);
        } catch (SAXException e) {
            throw new ServletException(e);
        }
    }

    String get(String key){
        String value = config.getInitParameter(key);
        if(value == null){
            value = System.getProperty("context-pacifier."+key);
            if(value == null){
                value = System.getenv("context-pacifier."+key);
            }
        }
        return value;
    }

    static boolean truthy(String value){
        return "true".equalsIgnoreCase(value)
            || "yes".equalsIgnoreCase(value)
            || "on".equalsIgnoreCase(value);
    }

    static String orElse(String value, String orElse){
        return (value != null) ? value : orElse;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if(request instanceof HttpServletRequest && response instanceof HttpServletResponse){
            try{
                doHttpFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
            } catch (SAXException e) {
                throw new ServletException(e);
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    void doHttpFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException, SAXException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if(runmode.isResource(path)){
            response.setContentType("text/html");
            runmode.write(path, response.getOutputStream());
        } else {
            chain.doFilter(request, response);
        }
    }

    @SuppressWarnings("unchecked")
    Set<String> getResourcePaths(String path){
        Set<String> paths = context.getResourcePaths(path);
        Set<String> resources = new HashSet<String>();
        for(String p : paths){
            if(p.endsWith("/")){
                resources.addAll(getResourcePaths(p));
            } else if(pattern.matcher(p).matches()){
                resources.add(p);
            }
        }
        return resources;
    }

    @Override
    public void destroy() {}

    interface Runmode {
        public boolean isResource(String path);
        public void write(String resource, OutputStream out) throws IOException, SAXException;
    }

    public class Compile implements Runmode {
        Map<String, byte[]> pacified = new HashMap<String, byte[]>();

        public Compile() throws ServletException, IOException, SAXException {
            for (String path : getResourcePaths("/")) {
                pacified.put(path, pacifier.pacify(context.getResourceAsStream(path)));
            }
        }

        @Override
        public boolean isResource(String path) {
            return pacified.containsKey(path);
        }

        @Override
        public void write(String resource, OutputStream out) throws IOException, SAXException {
            out.write(pacified.get(resource));
        }
    }

    public class Interpret implements Runmode {

        @Override
        public boolean isResource(String path) {
            return getResourcePaths("/").contains(path);
        }

        @Override
        public void write(String resource, OutputStream out) throws IOException, SAXException {
            pacifier.pacify(context.getResourceAsStream(resource), out);
        }
    }
}
