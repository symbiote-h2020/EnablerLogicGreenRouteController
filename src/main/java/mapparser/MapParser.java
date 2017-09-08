package mapparser;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.h2020.symbiote.cloud.model.data.observation.Location;
import eu.h2020.symbiote.smeur.StreetSegment;
import eu.h2020.symbiote.smeur.elgrc.GreenRouteEnablerLogic;

public class MapParser {
	//TODO needs commenting and testing
	private static final Logger log = LoggerFactory.getLogger(MapParser.class);

	public static void parser(String fileName) {
		log.info("Parsing Nodes of " + fileName);
		Map<String, Node> nodeMap = parseNodes(fileName);
		log.info("Parsing Ways of " + fileName);
		Map<String, StreetSegment> wayMap = parseWays(fileName, nodeMap);
	}

	private static Map<String, Node> parseNodes(String fileName) {
		Map<String, Node> nodeMap = new HashMap<String, Node>();
		Node node = null;
		boolean hasEverything = true;
		XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
		try {
			XMLEventReader xmlEventReader = xmlInputFactory.createXMLEventReader(new FileInputStream(fileName));
			while (xmlEventReader.hasNext()) {
				XMLEvent xmlEvent = xmlEventReader.nextEvent();
				if (xmlEvent.isStartElement()) {
					StartElement startElement = xmlEvent.asStartElement();
					if (startElement.getName().getLocalPart().equals("node")) {
						node = new Node();

						// Get the 'id' attribute from Employee element
						Attribute idAttr = startElement.getAttributeByName(new QName("id"));
						if (idAttr != null) {
							node.id = idAttr.getValue();
						} else {
							hasEverything = false;
						}

						Attribute latAttr = startElement.getAttributeByName(new QName("lat"));
						if (latAttr != null) {
							node.lat = Double.parseDouble(latAttr.getValue());
						} else {
							hasEverything = false;
						}

						Attribute lonAttr = startElement.getAttributeByName(new QName("lon"));
						if (lonAttr != null) {
							node.lon = Double.parseDouble(lonAttr.getValue());
						} else {
							hasEverything = false;
						}
					}
				}
				if (xmlEvent.isEndElement()) {
					EndElement endElement = xmlEvent.asEndElement();
					if (!endElement.getName().getLocalPart().equals("node")) {
						continue;
					}
					if (node != null && endElement.getName().getLocalPart().equals("node") && hasEverything) {
						nodeMap.put(node.id, node);
					}
					node = null;
				}
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}
		return nodeMap;
	}

	private static Map<String, StreetSegment> parseWays(String fileName, Map<String, Node> nodeMap) {
		Map<String, StreetSegment> wayMap = new HashMap<String, StreetSegment>();
		StreetSegment way = null;
		ArrayList<Location> locationAL = new ArrayList<Location>();
		int nNodes = 0;
		XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
		try {
			XMLEventReader xmlEventReader = xmlInputFactory.createXMLEventReader(new FileInputStream(fileName));
			while (xmlEventReader.hasNext()) {
				XMLEvent xmlEvent = xmlEventReader.nextEvent();
				if (xmlEvent.isStartElement()) {
					StartElement startElement = xmlEvent.asStartElement();
					if (startElement.getName().getLocalPart().equals("way")) {
						way = new StreetSegment();
						Attribute idAttr = startElement.getAttributeByName(new QName("id"));
						if (idAttr != null) {
							way.id = idAttr.getValue();
						}
					}
					
					else if (way != null && startElement.getName().getLocalPart().equals("nd")) {
						Attribute refAttr = startElement.getAttributeByName(new QName("ref"));
						Node n = nodeMap.get(refAttr.getValue());
						locationAL.add(new Location(n.lon, n.lat, 100, "", ""));
						nNodes += 1;
					} else if (way != null && startElement.getName().getLocalPart().equals("tag")) {
						Attribute keyAttr = startElement.getAttributeByName(new QName("k"));
						if (keyAttr.getValue().equals("name")) {
							Attribute idName = startElement.getAttributeByName(new QName("v"));
							if (idName.getValue() != null) {
								way.comment = idName.getValue();
							}
						}
					} 
				}

				if (xmlEvent.isEndElement()) {
					EndElement endElement = xmlEvent.asEndElement();
					if (!endElement.getName().getLocalPart().equals("way")) {
						continue;
					}
					if (endElement.getName().getLocalPart().equals("way") && nNodes > 1) {
						way.segmentData = locationAL.toArray(new Location[locationAL.size()]);
						wayMap.put(way.id, way);
					}
					way = null;
				}
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}
		return wayMap;
	}

}