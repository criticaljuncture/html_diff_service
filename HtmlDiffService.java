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
import javax.xml.transform.OutputKeys;
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
            result.getTransformer().setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            result.setResult(new StreamResult(response.getOutputStream()));

            String[] css = new String[]{};
            XslFilter filter = new XslFilter();

            Locale locale = Locale.getDefault();
            String prefix = "diff";

            HtmlCleaner cleaner = new HtmlCleaner();
            DomTreeBuilder oldHandler = new DomTreeBuilder();

            InputSource oldSource = new InputSource(oldStream);
            oldSource.setEncoding("UTF-8");
            InputSource newSource = new InputSource(newStream);
            newSource.setEncoding("UTF-8");
            cleaner.cleanAndParse(oldSource, oldHandler);

            TextNodeComparator leftComparator = new TextNodeComparator(oldHandler, locale);

            DomTreeBuilder newHandler = new DomTreeBuilder();
            cleaner.cleanAndParse(newSource, newHandler);
            TextNodeComparator rightComparator = new TextNodeComparator(newHandler, locale);

            result.startDocument();
            HtmlSaxDiffOutput output = new HtmlSaxDiffOutput(result, prefix);

            HTMLDiffer differ = new HTMLDiffer(output);
            differ.diff(leftComparator, rightComparator);

            result.endDocument();
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

    public static void main(String[] args) throws Exception
    {
        Server server = new Server(5001);
        server.setHandler(new HtmlDiffService());

        server.start();
        server.join();
    }


}
