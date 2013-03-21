package contextpacifier;

import nu.validator.htmlparser.common.XmlViolationPolicy;
import nu.validator.htmlparser.sax.HtmlParser;
import nu.validator.htmlparser.sax.HtmlSerializer;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import java.io.*;
import java.util.*;

public class ContextPacifier {

    // http://stackoverflow.com/questions/2725156/complete-list-of-html-tag-attributes-which-have-a-url-value
    public static Map<String, Set<String>> rewrite = new HashMap<String, Set<String>>() {{
        put("a",          set("href"));
        put("applet",     set("codebase"));
        put("area",       set("href"));
        put("base",       set("href"));
        put("blockquote", set("cite"));
        put("body",       set("background"));
        put("del",        set("cite"));
        put("form",       set("action"));
        put("frame",      set("longdesc", "src"));
        put("head",       set("profile"));
        put("iframe",     set("longdesc", "src"));
        put("img",        set("longdesc", "src", "usemap"));
        put("input",      set("src", "usemap"));
        put("ins",        set("cite"));
        put("link",       set("href"));
        put("object",     set("classid", "codebase", "data", "usemap"));
        put("q",          set("cite"));
        put("script",     set("src"));
        // HTML5
        put("audio",      set("src"));
        put("button",     set("formaction"));
        put("command",    set("icon"));
        put("embed",      set("src"));
        put("html",       set("manifest"));
        put("input",      set("formaction"));
        put("source",     set("src"));
        put("video",      set("poster", "src"));
    }};

    private static Set<String> set(String...values){
        Set<String> set = new HashSet<String>();
        for(String value : values){
            set.add(value);
        }
        return set;
    }

    private final String contextPath;
    private final Map<String, Set<String>> rewrites;
    private final boolean js;

    public ContextPacifier(String contextPath){
        this(contextPath, rewrite, true);
    }

    public ContextPacifier(String contextPath, Map<String, Set<String>> rewrites, boolean js){
        this.contextPath = contextPath;
        this.rewrites = rewrites;
        this.js = js;
    }

    public void pacify(InputStream in, OutputStream out) throws IOException, SAXException {
        HtmlSerializer serializer = new ContextpathHtmlSerializer(out);
        HtmlParser parser = new HtmlParser(XmlViolationPolicy.ALLOW);
        parser.setContentHandler(serializer);
        parser.setLexicalHandler(serializer);
        parser.parse(new InputSource(in));
    }

    public byte[] pacify(InputStream in) throws IOException, SAXException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        pacify(in, out);
        return out.toByteArray();
    }

    public byte[] pacify(byte[] in) throws IOException, SAXException {
        return pacify(new ByteArrayInputStream(in));
    }

    class ContextpathHtmlSerializer extends HtmlSerializer {

        ContextpathHtmlSerializer(OutputStream out) {
            super(out);
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            AttributesImpl attributes = new AttributesImpl(atts);

            Set<String> rules = rewrites.get(localName);
            if(rules != null){
                for(int i = 0; i < attributes.getLength(); i ++){
                    if(rules.contains(attributes.getLocalName(i))){
                        attributes.setValue(i, contextPath + attributes.getValue(i));
                    }
                }
            }
            super.startElement(uri, localName, qName, attributes);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if(js && "head".equals(localName)){
                AttributesImpl attributes = new AttributesImpl();
                String script = "script";
                char[] content = ("var contextPath = '" + contextPath + "';").toCharArray();
                attributes.addAttribute("", "type", "", "", "text/javascript");
                super.startElement(uri, script, script, attributes);
                super.characters(content, 0, content.length);
                super.endElement(uri, script, script);
            }
            super.endElement(uri, localName, qName);
        }
    }
}
