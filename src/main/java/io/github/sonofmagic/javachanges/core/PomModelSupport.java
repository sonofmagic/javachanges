package io.github.sonofmagic.javachanges.core;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.xml.sax.SAXException;

final class PomModelSupport {
    private PomModelSupport() {
    }

    static String readRevision(Path pomPath) throws IOException {
        Document document = parse(pomPath);
        Element project = document.getDocumentElement();
        Element properties = directChild(project, "properties");
        if (properties == null) {
            throw new IllegalStateException("Cannot find <properties> in " + pomPath);
        }
        String revision = directChildText(properties, "revision");
        if (revision == null) {
            throw new IllegalStateException("Cannot find <revision> in " + pomPath);
        }
        return revision;
    }

    static void writeRevision(Path pomPath, String revision) throws IOException {
        Document document = parse(pomPath);
        Element project = document.getDocumentElement();
        Element properties = directChild(project, "properties");
        if (properties == null) {
            throw new IllegalStateException("Cannot find <properties> in " + pomPath);
        }
        Element revisionElement = directChild(properties, "revision");
        if (revisionElement == null) {
            throw new IllegalStateException("Cannot find <revision> in " + pomPath);
        }
        revisionElement.setTextContent(revision);
        write(pomPath, document);
    }

    static String readArtifactId(Path pomPath) throws IOException {
        Document document = parse(pomPath);
        Element artifactId = directChild(document.getDocumentElement(), "artifactId");
        if (artifactId == null) {
            return null;
        }
        return ReleaseTextUtils.trimToNull(artifactId.getTextContent());
    }

    static List<String> readModulePaths(Path pomPath) throws IOException {
        Document document = parse(pomPath);
        Element modules = directChild(document.getDocumentElement(), "modules");
        if (modules == null) {
            return Collections.emptyList();
        }
        List<String> values = new ArrayList<String>();
        for (Element module : directChildren(modules, "module")) {
            String value = ReleaseTextUtils.trimToNull(module.getTextContent());
            if (value != null) {
                values.add(value);
            }
        }
        return values;
    }

    private static Document parse(Path pomPath) throws IOException {
        try (InputStream inputStream = Files.newInputStream(pomPath)) {
            DocumentBuilder builder = newDocumentBuilder();
            return builder.parse(inputStream);
        } catch (ParserConfigurationException exception) {
            throw new IllegalStateException("Failed to configure XML parser for " + pomPath, exception);
        } catch (SAXException exception) {
            throw new IllegalStateException("Failed to parse pom.xml: " + pomPath, exception);
        }
    }

    private static void write(Path pomPath, Document document) throws IOException {
        try (OutputStream outputStream = Files.newOutputStream(pomPath)) {
            TransformerFactory factory = TransformerFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.transform(new DOMSource(document), new StreamResult(outputStream));
        } catch (TransformerException exception) {
            throw new IllegalStateException("Failed to write pom.xml: " + pomPath, exception);
        }
    }

    private static DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder();
    }

    private static Element directChild(Element parent, String name) {
        NodeList childNodes = parent.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element element = (Element) child;
            if (matches(element, name)) {
                return element;
            }
        }
        return null;
    }

    private static List<Element> directChildren(Element parent, String name) {
        List<Element> result = new ArrayList<Element>();
        NodeList childNodes = parent.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element element = (Element) child;
            if (matches(element, name)) {
                result.add(element);
            }
        }
        return result;
    }

    private static String directChildText(Element parent, String name) {
        Element child = directChild(parent, name);
        if (child == null) {
            return null;
        }
        return ReleaseTextUtils.trimToNull(child.getTextContent());
    }

    private static boolean matches(Element element, String name) {
        String localName = element.getLocalName();
        if (name.equals(localName)) {
            return true;
        }
        return name.equals(element.getNodeName());
    }
}
