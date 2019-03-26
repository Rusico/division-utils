package division.util;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import division.fx.PropertyMap;
import division.json.JsonReader;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.TreeMap;
import javax.imageio.ImageIO;
import org.apache.commons.lang3.ArrayUtils;

public class AddressUtil {
  private static String urlGeocode   = "http://maps.googleapis.com/maps/api/geocode/json";
  private static String urlStaticMap = "http://maps.googleapis.com/maps/api/staticmap";
  
  public static String SUB_LOCALITY = "sublocality";
  public static String COUNTRY      = "country";
  public static String POSTAL_CODE  = "postal_code";
  public static String REGION       = "administrative_area_level_1";
  public static String SUB_REGION   = "administrative_area_level_2";
  public static String TOUWN        = "locality";
  public static String STREET       = "route";
  public static String HOME_NUMBER  = "street_number";
  
  private static String SYS_ADDRESS = "formatted_address";
  private static String COM_ADDRESS = "address_components";
  private static String GEOMETRY    = "geometry";
  
  private static TreeMap<String, String> imageCash = new TreeMap<>();
  
  public static Image getMap(int width, int height,String address) throws Exception {
    return getMap(width, height, 0, null, new String[]{address});
  }
  
  public static Image getMap(int width, int height,String[] addresses) throws Exception {
    return getMap(width, height, 0, null, addresses);
  }
  
  public static Image getMap(int width, int height,int zoom, String address) throws Exception {
    return getMap(width, height, zoom, null, new String[]{address});
  }
  
  public static Image getMap(int width, int height,int zoom, String[] addresses) throws Exception {
    return getMap(width, height, zoom, null, addresses);
  }
  
  public static Image getMap(int width, int height, int zoom, String center, String[] addresses) throws Exception {
    BufferedImage im = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Map<String, String> params = Maps.newHashMap();
    params.put("sensor",     "false");
    params.put("language",   "ru");
    params.put("scale",      "1");
    params.put("format",     "png");
    params.put("maptype",    "roadmap");
    if(addresses != null && addresses.length > 0)
      params.put("markers",    Joiner.on("|").join(addresses));
    if(center != null)
      params.put("center",     center);
    if(zoom > 0)
      params.put("zoom",       String.valueOf(zoom));
    if(width > 0 && height > 0)
      params.put("size",       width+"x"+height);
    
    InputStream in = null;
    try {
      String link = urlStaticMap+'?'+JsonReader.encodeParams(params);
      im = getImageFromCash(link);
      if(im == null) {
        System.out.println(link);
        im = ImageIO.read(in = new URL(link).openStream());
        imageTocash(link, im);
      }
    } catch(Exception ex) {
      throw new Exception(ex);
    } finally {
      if(in != null)
        in.close();
      in = null;
    }
    return im;
  }
  
  private static void imageTocash(String link, BufferedImage im) {
    try {
      File tmp = new File("google");
      tmp.deleteOnExit();
      if(!tmp.exists())
        tmp.mkdir();
      
      File png = new File(tmp.getName()+File.separator+System.currentTimeMillis()+".png");
      png.deleteOnExit();
      
      ImageIO.write(im, "PNG", png);
      imageCash.put(link, png.getAbsolutePath());
    }catch(Exception ex) {}
  }
  
  private static BufferedImage getImageFromCash(String link) throws IOException {
    BufferedImage im = null;
    InputStream in = null;
    if(imageCash.containsKey(link)) {
      try {
        in = new FileInputStream(imageCash.get(link));
        im = ImageIO.read(in);
      }catch(Exception ex) {}
      finally {
        if(in != null)
          in.close();
        in = null;
      }
    }
    return im;
  }
  
  private static Map<String, String> getParams(String region) {
    Map<String, String> params = Maps.newHashMap();
    params.put("sensor",     "false");
    params.put("language",   "ru");
    if(region != null) {
      params.put("region",     "ru");
      params.put("components", "country:RU|administrative_area:"+region);
    }
    return params;
  }
  
  public static String getAddress(String address) throws Exception {
    return getAddress(null, address);
  }
  
  public static String getAddress(String region, String address) throws Exception {
    return getAddress(getResponse(region, address));
  }
  
  public static String getAddress(double lng, double lat) throws Exception {
    return getAddress(getResponse(lng, lat));
  }
  
  public static double[] getLocation(String address) throws Exception {
    return getLocation(null, address);
  }
  
  public static double[] getLocation(String region, String address) throws Exception {
    PropertyMap response = getResponse(region, address);
    if(getStatus(response).equals("OK")) {
      PropertyMap location = response.getList("results").get(0);
      location = location.getMap(GEOMETRY);
      location = location.getMap("location");
      double lng = location.getDouble("lng");// долгота
      double lat = location.getDouble("lat");// широта
      return new double[]{lng, lat};
    }
    return null;
  }
  
  public static synchronized PropertyMap getResponse(String address) throws Exception {
    return getResponse(null, address);
  }
  
  public static synchronized PropertyMap getResponse(String region, String address) throws Exception {
    Map<String, String> params = getParams(region);
    params.put("address", address);
    return JsonReader.read(urlGeocode+'?'+JsonReader.encodeParams(params));
  }
  
  public static synchronized PropertyMap getResponse(double lng, double lat) throws Exception {
    Map<String, String> params = getParams(null);
    params.put("latlng", lat+","+lng);
    return JsonReader.read(urlGeocode+'?'+JsonReader.encodeParams(params));
  }
  
  public static String getAddressData(PropertyMap response, String type) throws IOException {
    if(getStatus(response).equals("OK")) {
      PropertyMap location = response.getList("results").get(0);
      for(PropertyMap o:location.getList(COM_ADDRESS)) {
        for(Object typeName:o.getList("types")) {
          if(type.equals(typeName))
            return o.getString("long_name");
        }
      }
    }
    return null;
  }
  
  public static String getAddress(PropertyMap response) {
    if(getStatus(response).equals("OK")) {
      PropertyMap location = response.getList("results").get(0);
      return location.getString(SYS_ADDRESS);
    }
    return null;
  }
  
  public static String getStatus(PropertyMap response) {
    return response.getString("status")==null?"STATUS IS NULL":response.getString("status");
  }
  
  public static PropertyMap[] getAddresses(String address) throws Exception {
    PropertyMap[] addresses = new PropertyMap[0];
    PropertyMap response = getResponse(address);
    if(getStatus(response) != null && getStatus(response).equals("OK")) {
      for(PropertyMap jaddress:response.getList("results")) {
        PropertyMap daddress = PropertyMap.create();
        addresses = (PropertyMap[]) ArrayUtils.add(addresses, daddress);
        daddress.setValue("title", jaddress.getString(SYS_ADDRESS));
        daddress.setValue("user-address", address);
        daddress.setValue("system-address", jaddress.getString(SYS_ADDRESS));
        
        daddress.setValue("lat", jaddress.getMap(GEOMETRY).getMap("location").getString("lat"));
        daddress.setValue("lng", jaddress.getMap(GEOMETRY).getMap("location").getString("lng"));
        
        for(PropertyMap caddress:jaddress.getList(COM_ADDRESS)) {
          String prefix = null;
          if(caddress.getList("types",String.class).contains(POSTAL_CODE))
            prefix = "post-code";
          if(caddress.getList("types",String.class).contains(COUNTRY))
            prefix = "country";
          if(caddress.getList("types",String.class).contains(REGION))
            prefix = "region";
          if(caddress.getList("types",String.class).contains(SUB_REGION))
            prefix = "sub-region";
          if(caddress.getList("types",String.class).contains(TOUWN))
            prefix = "town";
          if(caddress.getList("types",String.class).contains(SUB_LOCALITY))
            prefix = "sub-town";
          if(caddress.getList("types",String.class).contains(STREET))
            prefix = "street";
          if(caddress.getList("types",String.class).contains(HOME_NUMBER))
            prefix = "home";
          if(prefix != null) {
            daddress.setValue(prefix+"-long-name", caddress.getString("long_name"));
            daddress.setValue(prefix+"-short-name", caddress.getString("short_name"));
          }
        }
      }
    }
    return addresses;
  }
}