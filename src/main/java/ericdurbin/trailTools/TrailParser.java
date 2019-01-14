package ericdurbin.trailTools;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.BoundingBox;

//script to parse a shapefile and convert it to geojson
public class TrailParser {	
	private static final String COLORADO = "Colorado";
	private static final String MONTANA = "Montana";
	private static final String NEW_MEXICO = "New Mexico";
	private static final String WYOMING = "Wyoming";
	
	private static Map<String, DefaultFeatureCollection> featureMap = new HashMap<String, DefaultFeatureCollection>(){
		private static final long serialVersionUID = 810910073781854030L;
		{
			put(COLORADO, new DefaultFeatureCollection());
			put(MONTANA, new DefaultFeatureCollection());
			put(NEW_MEXICO, new DefaultFeatureCollection());
			put(WYOMING, new DefaultFeatureCollection());
		}
	};
	
	public static void main(String[] args) {
		try {
			FeatureCollection<?, ?> originalCollection = getFeatureCollectionFromFile("National_Forest_System_Trails_Feature_Layer.shp");
			FeatureIterator<?> originalIterator = originalCollection.features();
			while (originalIterator.hasNext()) {
				Feature feature = originalIterator.next();
				if(feature.getBounds() != null) {
					processFeature(feature);
				}
			}
			originalIterator.close();

			writeJSONFiles();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	private static void writeJSONFiles() throws IOException, FileNotFoundException {
		Iterator<String> mapIterator = featureMap.keySet().iterator();
		while(mapIterator.hasNext()) {
			String stateName = mapIterator.next();
			final File tmpFile = new File(stateName + "_features.json");
			if (!tmpFile.exists()) {
				tmpFile.createNewFile();
				System.out.println(tmpFile.getAbsolutePath() + " has been created");
			}
			final OutputStream output = new BufferedOutputStream(new FileOutputStream(tmpFile));
			FeatureJSON featureJSON = new FeatureJSON();
			featureJSON.writeFeatureCollection(featureMap.get(stateName), output);
			output.close();
		}
	}

	private static void processFeature(Feature feature) {
		BoundingBox colorado = new ReferencedEnvelope(-109, -102.055, 37, 41.00, feature.getBounds().getCoordinateReferenceSystem());
		BoundingBox montana = new ReferencedEnvelope(-116, -104.290283, 44.45, 49, feature.getBounds().getCoordinateReferenceSystem());
		BoundingBox northernNewMexico = new ReferencedEnvelope(-109, -102.055, 35.7, 37, feature.getBounds().getCoordinateReferenceSystem());
		BoundingBox wyoming = new ReferencedEnvelope(-111.290283, -104.2, 41.075970, 45, feature.getBounds().getCoordinateReferenceSystem());
		if(colorado.contains(feature.getBounds())) {
			featureMap.get(COLORADO).add((SimpleFeature) feature);
		} else if(montana.contains(feature.getBounds())) {
			featureMap.get(MONTANA).add((SimpleFeature) feature);
		} else if(northernNewMexico.contains(feature.getBounds())) {
			featureMap.get(NEW_MEXICO).add((SimpleFeature) feature);
		} else if(wyoming.contains(feature.getBounds())) {
			featureMap.get(WYOMING).add((SimpleFeature) feature);
		}
	}
	
	private static FeatureCollection<?, ?> getFeatureCollectionFromFile(String dbfFileName) {
		FeatureCollection<?, ?> originalCollection = new DefaultFeatureCollection();
		try {
			ClassLoader classLoader = TrailParser.class.getClassLoader();
			File shapeFile = new File(classLoader.getResource(dbfFileName).getFile() );
			FileDataStore myData = FileDataStoreFinder.getDataStore(shapeFile);
			SimpleFeatureSource source = myData.getFeatureSource();
			originalCollection = source.getFeatures();
			myData.dispose();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return originalCollection;
	}
}
