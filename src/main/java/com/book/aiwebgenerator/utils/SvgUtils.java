package com.book.aiwebgenerator.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class SvgUtils {

    private static final int MAX_SVG_LENGTH = 200_000;
    private static final String SVG_NAMESPACE = "http://www.w3.org/2000/svg";
    private static final Pattern UNSAFE_VALUE = Pattern.compile(
            "(?i).*(javascript:|data:|https?:|file:|url\\((?!#[A-Za-z][A-Za-z0-9_.:-]*\\))).*"
    );

    private static final Set<String> ALLOWED_ELEMENTS = Set.of(
            "svg", "g", "path", "circle", "rect", "ellipse", "line", "polyline", "polygon",
            "defs", "lineargradient", "radialgradient", "stop", "clippath", "mask", "title", "desc"
    );

    private static final Set<String> ALLOWED_ATTRIBUTES = Set.of(
            "xmlns", "viewbox", "width", "height", "fill", "stroke", "stroke-width",
            "stroke-linecap", "stroke-linejoin", "stroke-miterlimit", "opacity", "fill-opacity",
            "stroke-opacity", "transform", "d", "cx", "cy", "r", "x", "y", "rx", "ry",
            "x1", "y1", "x2", "y2", "points", "offset", "stop-color", "stop-opacity",
            "gradientunits", "gradienttransform", "id", "clip-path", "mask", "fill-rule", "clip-rule"
    );

    private SvgUtils() {
    }

    public static String extractAndSanitizeSvg(String modelResponse) {
        if (modelResponse == null) {
            throw new IllegalArgumentException("SVG response cannot be null");
        }

        int svgStart = modelResponse.indexOf("<svg");
        int svgEnd = modelResponse.lastIndexOf("</svg>");
        if (svgStart < 0 || svgEnd < svgStart) {
            throw new IllegalArgumentException("Model response did not contain an SVG document");
        }

        String svg = modelResponse.substring(svgStart, svgEnd + "</svg>".length());
        if (svg.length() > MAX_SVG_LENGTH) {
            throw new IllegalArgumentException("SVG document is too large");
        }

        try {
            Document document = parseSecurely(svg);
            validateElement(document.getDocumentElement());
            return serialize(document);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid SVG document", e);
        }
    }

    private static Document parseSecurely(String svg) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        var documentBuilder = factory.newDocumentBuilder();
        documentBuilder.setErrorHandler(new DefaultHandler() {
            @Override
            public void error(SAXParseException e) throws SAXParseException {
                throw e;
            }

            @Override
            public void fatalError(SAXParseException e) throws SAXParseException {
                throw e;
            }
        });
        return documentBuilder.parse(new InputSource(new StringReader(svg)));
    }

    private static void validateElement(Element element) {
        String elementName = localName(element).toLowerCase(Locale.ROOT);
        if (!ALLOWED_ELEMENTS.contains(elementName)) {
            throw new IllegalArgumentException("Disallowed SVG element: " + elementName);
        }
        if ("svg".equals(elementName)) {
            String namespace = element.getNamespaceURI();
            if (!SVG_NAMESPACE.equals(namespace)) {
                throw new IllegalArgumentException("Invalid SVG namespace");
            }
            if (!element.hasAttribute("viewBox")) {
                throw new IllegalArgumentException("SVG must define a viewBox");
            }
        }

        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            String attributeName = localName(attribute).toLowerCase(Locale.ROOT);
            if (!ALLOWED_ATTRIBUTES.contains(attributeName)) {
                throw new IllegalArgumentException("Disallowed SVG attribute: " + attributeName);
            }
            if ("xmlns".equals(attributeName) && SVG_NAMESPACE.equals(attribute.getNodeValue())) {
                continue;
            }
            if (UNSAFE_VALUE.matcher(attribute.getNodeValue()).matches()) {
                throw new IllegalArgumentException("Unsafe SVG attribute value");
            }
        }

        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                validateElement((Element) child);
            }
        }
    }

    private static String localName(Node node) {
        return node.getLocalName() == null ? node.getNodeName() : node.getLocalName();
    }

    private static String serialize(Document document) throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        StringWriter output = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(output));
        return output.toString();
    }
}
