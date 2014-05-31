package gr.athenainnovation.imis.parsers;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import gr.athenainnovation.imis.OSMContainer.OSMNode;
import gr.athenainnovation.imis.OSMContainer.OSMWay;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeocentricCRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

/**
 * Parses OSM xml file and constructs additional nodes of the OSM map into appropriate objects with attributes.
 *
 * @author imis-nkarag
 */

public class OSMParser extends DefaultHandler {
    
    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(OSMParser.class);
    private final List<OSMNode> nodeList; //will be populated with nodes 
    private final Map<String, OSMNode> nodesWithIDs; //map containing IDs as Strings and the corresponding OSMNode objects     
    private final List<OSMWay> wayList;  //populated with ways of the OSM file
    private final String osmXmlFileName;
    private OSMNode nodeTmp; //variable to hold the node object
    private OSMWay wayTmp;   //variable to hold the way object
    private boolean inWay = false; //when parser is in a way node becomes true in order to track the parser position 
    private boolean inNode = false; //becomes true when the parser is in a simple node        

    public OSMParser(String osmXmlFileName) {
        this.osmXmlFileName = osmXmlFileName;
        nodeList = new ArrayList<>();
        wayList = new ArrayList<>();
        nodesWithIDs = new HashMap<>();

    }

    public void parseDocument() {
        // parse
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            SAXParser parser = factory.newSAXParser();
            parser.parse(osmXmlFileName, this);
        } catch (ParserConfigurationException e) {
            System.out.println("ParserConfig error " + e);
        } catch (SAXException e) {
            System.out.println("SAXException : xml not well formed " + e);
        } catch (IOException e) {
            System.out.println("IO error " + e);
        }
    LOG.info("Instances from OSM/XML file parsed!");    
    }

    @Override
    public void startElement(String s, String s1, String elementName, Attributes attributes) throws SAXException {
    
        // if current element is an OSMNode , create new node and populate with the appropriate values
        if (elementName.equalsIgnoreCase("node")) {
            nodeTmp = new OSMNode();
            nodeTmp.setID(attributes.getValue("id"));
            nodeTmp.setLang(attributes.getValue("lang"));
            nodeTmp.setAction(attributes.getValue("action"));
            nodeTmp.setVisible(attributes.getValue("visible"));

            //parse geometry
            double longitude = Double.parseDouble(attributes.getValue("lon"));
            double latitude = Double.parseDouble(attributes.getValue("lat"));

            //change from wgs84 to cartesian for later processing of the geometry   
            CoordinateReferenceSystem sourceCRS = DefaultGeographicCRS.WGS84;
            CoordinateReferenceSystem targetCRS = DefaultGeocentricCRS.CARTESIAN;

            MathTransform transform;
            Coordinate targetGeometry = null;
            Coordinate sourceCoordinate;
            try {
                transform = CRS.findMathTransform(sourceCRS, targetCRS, true);
                sourceCoordinate = new Coordinate(longitude, latitude);
                targetGeometry = JTS.transform(sourceCoordinate, null, transform);

            } catch (FactoryException | MismatchedDimensionException | TransformException ex) {
                Logger.getLogger(OSMParser.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            //create geometry object
            GeometryFactory geometryFactory = new GeometryFactory();
            Geometry geom = geometryFactory.createPoint(new Coordinate(targetGeometry));
            nodeTmp.setGeometry(geom);
            
            nodeTmp.setTimestamp(attributes.getValue("timestamp"));
            nodeTmp.setUid(attributes.getValue("uid"));
            nodeTmp.setUser(attributes.getValue("user"));
            nodeTmp.setVersion(attributes.getValue("version"));
            nodeTmp.setChangeset(attributes.getValue("changeset"));
            inNode = true;
            inWay = false;

        } else if (elementName.equalsIgnoreCase("way")) {
            wayTmp = new OSMWay();
            wayTmp.setID(attributes.getValue("id"));
            wayTmp.setLang(attributes.getValue("lang"));
            wayTmp.setAction(attributes.getValue("action"));
            wayTmp.setVisible(attributes.getValue("visible"));
            wayTmp.setTimestamp(attributes.getValue("timestamp"));
            wayTmp.setUid(attributes.getValue("uid"));
            wayTmp.setUser(attributes.getValue("user"));
            wayTmp.setVersion(attributes.getValue("version"));
            wayTmp.setChangeset(attributes.getValue("changeset"));
            inWay = true;
            inNode = false;

        } else if (elementName.equalsIgnoreCase("nd")) {
            wayTmp.addNodeReference(attributes.getValue("ref"));

        } else if (elementName.equalsIgnoreCase("tag")) {

            if (inNode) {
                //if the path is in an OSMNode set tagKey and value to the corresponding node     
                nodeTmp.setTagKeyValue(attributes.getValue("k"), attributes.getValue("v"));
            } else if (inWay) {
                //else if the path is in an OSM way set tagKey and value to the corresponding way
                wayTmp.setTagKeyValue(attributes.getValue("k"), attributes.getValue("v"));
            }
        }
        
        //add relation nodes and find a way to use them in vectors. Not supported yet
    }

    @Override
    public void endElement(String s, String s1, String element) throws SAXException {
        // if end of node element, add to appropriate list
        if (element.equals("node")) {
            nodeList.add(nodeTmp);
            nodesWithIDs.put(nodeTmp.getID(), nodeTmp);
        }
        
        if (element.equals("way")) {            
            
            //construct the Way geometry from each node of the node references
            List<String> references = wayTmp.getNodeReferences();

            for (String entry: references) {
               Geometry geometry = nodesWithIDs.get(entry).getGeometry(); //get the geometry of the node with ID=entry
               wayTmp.addNodeGeometry(geometry); //add the node geometry in this way
               
            }
            GeometryFactory geometryFactory = new GeometryFactory();
            Geometry geom = geometryFactory.buildGeometry(wayTmp.getNodeGeometries());
            
            if((wayTmp.getNumberOfNodes()>3) && 
                    wayTmp.getNodeGeometries().get(0).equals(wayTmp.getNodeGeometries().get(wayTmp.getNodeGeometries().size()-1))){
            //checks if the beginning and ending node are the same and the number of nodes are more than 3. 
            //the nodes must be more than 3, because jts does not allow a construction of a linear ring with less points.
                
               if (!((wayTmp.getTagKeyValue().containsKey("barrier")) || wayTmp.getTagKeyValue().containsKey("highway"))){
               //this is not a barrier nor a road, so construct a polygon geometry
               
               LinearRing linear = new GeometryFactory().createLinearRing(geom.getCoordinates());
               Polygon poly = new Polygon(linear, null, geometryFactory);
               wayTmp.setGeometry(poly);               
               }
               else {
               //it is either a barrier or a road, so construct a linear ring geometry 
                  LinearRing linear = new GeometryFactory().createLinearRing(geom.getCoordinates());
                  wayTmp.setGeometry(linear);  
               }
            }
            else if (wayTmp.getNumberOfNodes()>1){
            //it is an open geometry with more than one nodes, make it linestring 
                
                LineString lineString =  new GeometryFactory().createLineString(geom.getCoordinates());
                wayTmp.setGeometry(lineString);               
            }
            else{ //we assume all the rest geometries are points
            //some ways happen to have only one point. Construct a  Point.
                Point point = new GeometryFactory().createPoint(geom.getCoordinate());
                wayTmp.setGeometry(point);
            }
        wayList.add(wayTmp);
        }        
    }

    public List<OSMNode> getNodeList() {
        return this.nodeList;
    }

    public List<OSMWay> getWayList() {
        return this.wayList;
    }

    public Map<String, OSMNode> getNodesWithIDs() {
        return this.nodesWithIDs;
    }    

}