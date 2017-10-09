package eu.h2020.symbiote.smeur.elgrc.mapparser;

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
import eu.h2020.symbiote.smeur.StreetSegmentList;

public class MapParser {
	private static final Logger log = LoggerFactory.getLogger(MapParser.class);

	/**
	 * Method that parses osm file to classes to be used in Enabler
	 * 
	 * @param fileName
	 *            Location of the file
	 * @return hashmap of id : ways
	 */
	public static StreetSegmentList parser(String fileName) {
		long startTime = System.currentTimeMillis();
		log.info("Parsing Nodes of " + fileName);
		Map<String, Node> nodeMap = parseNodes(fileName);
		log.info("Parsing Ways of " + fileName);
		StreetSegmentList wayMap = parseWays(fileName, nodeMap);
		long duration = (System.currentTimeMillis() - startTime);
		log.info("Parsing finished, it took " + duration + " milliseconds");
		return wayMap;
	}

	/**
	 * Method that loads file and parses nodes into a hashmap to be referred later
	 * when parsing ways
	 * 
	 * @param fileName
	 *            Location of the file
	 * @return hashmap of id : node
	 */
	private static Map<String, Node> parseNodes(String fileName) {
		Map<String, Node> nodeMap = new HashMap<String, Node>();
		Node node = null;
		boolean hasEverything = true;
		
		/*
		 * This library parses XML files line by line, allowing not loading an entire (possibly huge) file into memory
		 */
		XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
		try {
			XMLEventReader xmlEventReader = xmlInputFactory.createXMLEventReader(new FileInputStream(fileName));
			while (xmlEventReader.hasNext()) {
				XMLEvent xmlEvent = xmlEventReader.nextEvent();
				
				// Catch start of element
				if (xmlEvent.isStartElement()) {
					StartElement startElement = xmlEvent.asStartElement();
					
					// If it is a node element, catch its properties
					if (startElement.getName().getLocalPart().equals("node")) {
						node = new Node();

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
				
				// If it is the end of an (node) element, save it
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

	private static StreetSegmentList parseWays(String fileName, Map<String, Node> nodeMap) {
		StreetSegmentList wayMap = new StreetSegmentList();
		StreetSegment way = null;
		ArrayList<Location> locationAL = new ArrayList<Location>();
		int nNodes = 0;
		
		/*
		 * This library parses XML files line by line, allowing not loading an entire (possibly huge) file into memory
		 */
		XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
		try {
			XMLEventReader xmlEventReader = xmlInputFactory.createXMLEventReader(new FileInputStream(fileName));
			while (xmlEventReader.hasNext()) {
				XMLEvent xmlEvent = xmlEventReader.nextEvent();
				
				// Catch start of element
				if (xmlEvent.isStartElement()) {
					StartElement startElement = xmlEvent.asStartElement();
					
					// If it is a way element, catch its properties
					if (startElement.getName().getLocalPart().equals("way")) {
						way = new StreetSegment();
						Attribute idAttr = startElement.getAttributeByName(new QName("id"));
						if (idAttr != null) {
							way.id = idAttr.getValue();
						}
					}

					// If propertie is a reference to a node, go fetch it to the hashmap
					else if (way != null && startElement.getName().getLocalPart().equals("nd")) {
						Attribute refAttr = startElement.getAttributeByName(new QName("ref"));
						Node n = nodeMap.get(refAttr.getValue());
						locationAL.add(new Location(n.lon, n.lat, 100, "", ""));
						nNodes += 1;
					} 
					
					// If it is a tag, see if value if the name of the way, if it is, save it
					else if (way != null && startElement.getName().getLocalPart().equals("tag")) {
						Attribute keyAttr = startElement.getAttributeByName(new QName("k"));
						if (keyAttr.getValue().equals("name")) {
							Attribute idName = startElement.getAttributeByName(new QName("v"));
							if (idName.getValue() != null) {
								way.comment = idName.getValue();
							}
						}
					}
				}

				// If it is the end of the way element, save it 
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
