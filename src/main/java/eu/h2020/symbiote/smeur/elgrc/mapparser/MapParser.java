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

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.h2020.symbiote.model.cim.WGS84Location;
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
	public static StreetSegmentList parser(String fileName, String fileFormat) {
		long startTime = System.currentTimeMillis();
		StreetSegmentList wayMap = null;
		
		if (fileFormat.equals("osm")) {
			log.info("Parsing Nodes of " + fileName);
			Map<String, Node> nodeMap = parseNodesOsm(fileName);
			log.info("Parsing Ways of " + fileName);
			wayMap = parseWaysOsm(fileName, nodeMap);
		}
		
		else if (fileFormat.equals("geojson")) {
			log.info("Parsing Ways of " + fileName);
			wayMap = parseWaysGeoJson(fileName); 
		}
		
		else {
			//TODO raise exception?
			log.info("File format for " + fileName + " nor supported for parsing!");
			return null;
		}
		
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
	private static Map<String, Node> parseNodesOsm(String fileName) {
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

	private static StreetSegmentList parseWaysOsm(String fileName, Map<String, Node> nodeMap) {
		StreetSegmentList wayMap = new StreetSegmentList();
		StreetSegment way = null;
		ArrayList<WGS84Location> locationAL = new ArrayList<WGS84Location>();
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
						locationAL.add(new WGS84Location(n.lon, n.lat, 100, "", new ArrayList<String>()));
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
						way.segmentData = locationAL.toArray(new WGS84Location[locationAL.size()]);
						wayMap.put(way.id, way);
					}
					locationAL = new ArrayList<WGS84Location>();
					/* if (nWays % 1000 == 0) {
						Runtime runtime = Runtime.getRuntime();
						long totalMemory = runtime.totalMemory();
						long freeMemory = runtime.freeMemory();
						long maxMemory = runtime.maxMemory();
						log.info(" --- ");
						log.info("Ways:         " + nWays);
						log.info("Total Memory: " + totalMemory);
						log.info("Free Memory:  " + freeMemory);
						log.info("Max Memory:   " + maxMemory);
					}*/
					nNodes = 0;
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
	/**
	 * Method that loads file and parses nodes into a hashmap to be referred later
	 * when parsing ways
	 * 
	 * @param fileName
	 * 			Location of the file
	 * @return
	 */
	private static StreetSegmentList parseWaysGeoJson(String fileName) {
		StreetSegmentList wayMap = new StreetSegmentList();
		FileInputStream fis = null;
		
		try {
			//Load file
			fis = new FileInputStream(fileName);
			JSONTokener tokener = new JSONTokener(fis);
			JSONObject root = new JSONObject(tokener);
			
			//Loop through ways
			JSONArray waysJson = root.getJSONArray("features");
			for (int i = 0; i < waysJson.length(); i++) {
				StreetSegment way = new StreetSegment();
				ArrayList<WGS84Location> locationAL = new ArrayList<WGS84Location>();
				
				//Get way id
				way.id = waysJson.getJSONObject(i).getJSONObject("properties").getString("id");
				
				//Loop through nodes in way and store them in list
				JSONArray nodesJson = waysJson.getJSONObject(i).getJSONObject("geometry").getJSONArray("coordinates");
				for (int j = 0; j < nodesJson.length(); j++) {
					double lon = nodesJson.getJSONArray(j).getDouble(0);
					double lat = nodesJson.getJSONArray(j).getDouble(1);
					locationAL.add(new WGS84Location(lon, lat, 100, "", new ArrayList<String>()));
				}
				
				//Save way
				way.segmentData = locationAL.toArray(new WGS84Location[locationAL.size()]);
				wayMap.put(way.id, way);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
		
		return wayMap;
	}
	
}
