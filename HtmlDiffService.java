import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import java.io.IOException;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import java.util.Locale;
import org.outerj.daisy.diff.HtmlCleaner;
import org.outerj.daisy.diff.XslFilter;
import org.outerj.daisy.diff.html.HTMLDiffer;
import org.outerj.daisy.diff.html.HtmlSaxDiffOutput;
import org.outerj.daisy.diff.html.TextNodeComparator;
import org.outerj.daisy.diff.html.dom.DomTreeBuilder;


import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ByteArrayInputStream;
import java.util.Locale;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.outerj.daisy.diff.html.HTMLDiffer;
import org.outerj.daisy.diff.html.HtmlSaxDiffOutput;
import org.outerj.daisy.diff.html.TextNodeComparator;
import org.outerj.daisy.diff.html.dom.DomTreeBuilder;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;


public class HtmlDiffService extends AbstractHandler
{
    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response) 
        throws IOException, ServletException
    {
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);
        
        boolean htmlOut = false;
        
        try {
            String oldString = request.getParameter("old");
            String newString = request.getParameter("new");
            
            if (oldString == null || newString == null) {
                return;
            }
            
            InputStream oldStream = new ByteArrayInputStream( oldString.getBytes(  ) );
            InputStream newStream = new ByteArrayInputStream( newString.getBytes(  ) );
            
            SAXTransformerFactory tf = (SAXTransformerFactory) TransformerFactory.newInstance();
            TransformerHandler result = tf.newTransformerHandler();
            result.setResult(new StreamResult(response.getOutputStream()));
            
            String[] css = new String[]{};
            XslFilter filter = new XslFilter();
            ContentHandler postProcess = htmlOut? filter.xsl(result,
                    "org/outerj/daisy/diff/htmlheader.xsl"):result;
        
            Locale locale = Locale.getDefault();
            String prefix = "diff";
        
            HtmlCleaner cleaner = new HtmlCleaner();
            DomTreeBuilder oldHandler = new DomTreeBuilder();
            
            InputSource oldSource = new InputSource(oldStream);
            InputSource newSource = new InputSource(newStream);
            cleaner.cleanAndParse(oldSource, oldHandler);
        
            TextNodeComparator leftComparator = new TextNodeComparator(oldHandler, locale);
        
            DomTreeBuilder newHandler = new DomTreeBuilder();
            cleaner.cleanAndParse(newSource, newHandler);
            TextNodeComparator rightComparator = new TextNodeComparator(newHandler, locale);
        
            postProcess.startDocument();
            postProcess.startElement("", "diffreport", "diffreport", new AttributesImpl());
            doCSS(css, postProcess);
            postProcess.startElement("", "diff", "diff", new AttributesImpl());
            HtmlSaxDiffOutput output = new HtmlSaxDiffOutput(postProcess, prefix);
            
            HTMLDiffer differ = new HTMLDiffer(output);
            differ.diff(leftComparator, rightComparator);
            
            postProcess.endElement("", "diff", "diff");
            postProcess.endElement("", "diffreport", "diffreport");
            postProcess.endDocument();
        } catch (Throwable e) {
            e.printStackTrace();
            if (e.getCause() != null) {
                e.getCause().printStackTrace();
            }
            if (e instanceof SAXException) {
                ((SAXException) e).getException().printStackTrace();
            }
        }
    }
    private static void doCSS(String[] css, ContentHandler handler) throws SAXException {
        handler.startElement("", "css", "css",
                new AttributesImpl());
        for(String cssLink : css){
            AttributesImpl attr = new AttributesImpl();
            attr.addAttribute("", "href", "href", "CDATA", cssLink);
            attr.addAttribute("", "type", "type", "CDATA", "text/css");
            attr.addAttribute("", "rel", "rel", "CDATA", "stylesheet");
            handler.startElement("", "link", "link",
                    attr);
            handler.endElement("", "link", "link");
        }
        
        handler.endElement("", "css", "css");
        
    }
    
    public static void main(String[] args) throws Exception
    {
        Server server = new Server(8080);
        server.setHandler(new HtmlDiffService());
 
        server.start();
        server.join();
    }
    
    
}