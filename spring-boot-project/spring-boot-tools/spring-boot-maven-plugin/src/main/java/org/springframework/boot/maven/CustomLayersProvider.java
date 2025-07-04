/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.maven;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import org.springframework.boot.loader.tools.Layer;
import org.springframework.boot.loader.tools.Library;
import org.springframework.boot.loader.tools.layer.ApplicationContentFilter;
import org.springframework.boot.loader.tools.layer.ContentFilter;
import org.springframework.boot.loader.tools.layer.ContentSelector;
import org.springframework.boot.loader.tools.layer.CustomLayers;
import org.springframework.boot.loader.tools.layer.IncludeExcludeContentSelector;
import org.springframework.boot.loader.tools.layer.LibraryContentFilter;

/**
 * Produces a {@link CustomLayers} based on the given {@link Document}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class CustomLayersProvider {

	CustomLayers getLayers(Document document) {
		validate(document);
		Element root = document.getDocumentElement();
		List<ContentSelector<String>> applicationSelectors = getApplicationSelectors(root);
		List<ContentSelector<Library>> librarySelectors = getLibrarySelectors(root);
		List<Layer> layers = getLayers(root);
		return new CustomLayers(layers, applicationSelectors, librarySelectors);
	}

	private void validate(Document document) {
		Schema schema = loadSchema();
		try {
			Validator validator = schema.newValidator();
			validator.validate(new DOMSource(document));
		}
		catch (SAXException | IOException ex) {
			throw new IllegalStateException("Invalid layers.xml configuration", ex);
		}
	}

	private Schema loadSchema() {
		try {
			SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			return factory.newSchema(getClass().getResource("layers.xsd"));
		}
		catch (SAXException ex) {
			throw new IllegalStateException("Unable to load layers XSD");
		}
	}

	private List<ContentSelector<String>> getApplicationSelectors(Element root) {
		return getSelectors(root, "application", (element) -> getSelector(element, ApplicationContentFilter::new));
	}

	private List<ContentSelector<Library>> getLibrarySelectors(Element root) {
		return getSelectors(root, "dependencies", (element) -> getLibrarySelector(element, LibraryContentFilter::new));
	}

	private List<Layer> getLayers(Element root) {
		Element layerOrder = getChildElement(root, "layerOrder");
		if (layerOrder == null) {
			return Collections.emptyList();
		}
		return getChildNodeTextContent(layerOrder, "layer").stream().map(Layer::new).toList();
	}

	private <T> List<ContentSelector<T>> getSelectors(Element root, String elementName,
			Function<Element, ContentSelector<T>> selectorFactory) {
		Element element = getChildElement(root, elementName);
		if (element == null) {
			return Collections.emptyList();
		}
		List<ContentSelector<T>> selectors = new ArrayList<>();
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Element childElement) {
				ContentSelector<T> selector = selectorFactory.apply(childElement);
				selectors.add(selector);
			}
		}
		return selectors;
	}

	private <T> ContentSelector<T> getSelector(Element element, Function<String, ContentFilter<T>> filterFactory) {
		Layer layer = new Layer(element.getAttribute("layer"));
		List<String> includes = getChildNodeTextContent(element, "include");
		List<String> excludes = getChildNodeTextContent(element, "exclude");
		return new IncludeExcludeContentSelector<>(layer, includes, excludes, filterFactory);
	}

	private ContentSelector<Library> getLibrarySelector(Element element,
			Function<String, ContentFilter<Library>> filterFactory) {
		Layer layer = new Layer(element.getAttribute("layer"));
		List<String> includes = getChildNodeTextContent(element, "include");
		List<String> excludes = getChildNodeTextContent(element, "exclude");
		Element includeModuleDependencies = getChildElement(element, "includeModuleDependencies");
		Element excludeModuleDependencies = getChildElement(element, "excludeModuleDependencies");
		List<ContentFilter<Library>> includeFilters = includes.stream()
			.map(filterFactory)
			.collect(Collectors.toCollection(ArrayList::new));
		if (includeModuleDependencies != null) {
			includeFilters.add(Library::isLocal);
		}
		List<ContentFilter<Library>> excludeFilters = excludes.stream()
			.map(filterFactory)
			.collect(Collectors.toCollection(ArrayList::new));
		if (excludeModuleDependencies != null) {
			excludeFilters.add(Library::isLocal);
		}
		return new IncludeExcludeContentSelector<>(layer, includeFilters, excludeFilters);
	}

	private List<String> getChildNodeTextContent(Element element, String tagName) {
		List<String> patterns = new ArrayList<>();
		NodeList nodes = element.getElementsByTagName(tagName);
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			if (node instanceof Element) {
				patterns.add(node.getTextContent());
			}
		}
		return patterns;
	}

	private Element getChildElement(Element element, String tagName) {
		NodeList nodes = element.getElementsByTagName(tagName);
		if (nodes.getLength() == 0) {
			return null;
		}
		if (nodes.getLength() > 1) {
			throw new IllegalStateException("Multiple '" + tagName + "' nodes found");
		}
		return (Element) nodes.item(0);
	}

}
