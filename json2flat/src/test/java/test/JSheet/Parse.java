package test.JSheet;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.opendevl.OrderJson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;

public class Parse {
	static List<Object[]> arr = new ArrayList<Object[]>();

	static OrderJson makeOrder = new OrderJson();
	
	static String regex = "(\\[[0-9]*\\]$)";
	
	static LinkedHashSet<String> primitiveStringUnique = new LinkedHashSet<String>();
	
	static List<String> unique = null;
	
	static String tmpPath = null;
	/**
	 * just for testing
	 * @param args
	 * @throws UnsupportedEncodingException
	 * @throws FileNotFoundException
	 */
	public static void main(String[] args) throws UnsupportedEncodingException, FileNotFoundException{
		InputStream is = Parse.class.getResourceAsStream("/tmp.json") ;  
		Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
		JsonElement x = new JsonParser().parse(reader);
		
		Configuration conf = Configuration.defaultConfiguration()
											.addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL)
											.addOptions(Option.SUPPRESS_EXCEPTIONS);
		Configuration pathConf = Configuration.defaultConfiguration()
												.addOptions(Option.AS_PATH_LIST)
												.addOptions(Option.ALWAYS_RETURN_LIST);
		DocumentContext tt = JsonPath.using(conf).parse(x.toString());
		DocumentContext tt_path = JsonPath.using(pathConf).parse(x.toString());
		
		List<String> pathList = tt_path.read("$..*");
		
		LinkedHashSet<String> primitiveString = new LinkedHashSet<String>();
		Set<String> sortedString = new TreeSet<String>();
		
		//System.out.println(pathList);
		
		
		for(String o : pathList){
			// System.out.println(o);
			Object rr = tt.read(o);
			
			if(rr==null){
				primitiveString.add(o);
				
			}else{
				String dataType = rr.getClass().getSimpleName();
				if(dataType.equals("Boolean") || dataType.equals("Integer") || dataType.equals("String") || dataType.equals("Double")){
					primitiveString.add(o);
				}
			}
			sortedString.add(o);
		}
		
		//System.out.println("+=======================================+");
		
		
		Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
		
		for(String o : primitiveString){
			
			Matcher m = pattern.matcher(o);
			//System.out.println(o);
			if(m.find()){
				System.out.println(o);
				System.out.println(m.group());
				String tmp[] = o.replace("$", "").split("(\\[[0-9]\\]$)");
				tmp[0] = tmp[0].replaceAll("(\\[[0-9]\\])", "");
				primitiveStringUnique.add("/"+(tmp[0]+m.group()).replace("'][", "/").replace("[", "").replace("]", "").replace("''", "/").replace("'", ""));
			}else{
				primitiveStringUnique.add("/"+o.replace("$", "").replaceAll("(\\[[0-9]\\])", "").replace("[", "").replace("]", "").replace("''", "/").replace("'", ""));
			}
		}
		
		unique = new ArrayList<String>(primitiveStringUnique);
		
		for(String o : unique){
			System.out.println(o);
		}
		Object[] header = new Object[unique.size()];
		int i = 0;
		for(String o : unique){
			header[i] = o;
			i++;
		}
		
		arr.add(header);
		
		
		
		System.out.println("=============" + unique.size()) ;
		
		arr.add(make2D(new Object[unique.size()], new Object[unique.size()], x, "/",true));
		//makePremObj(new Object[unique.size()], new Object[unique.size()], x, "/");
		
		
		System.out.println("\n\nNumber of Rows :: " + arr.size() + "\n\n");
		for(String o : unique){
			System.out.print(o);
			System.out.print("\t|\t");
		}
		System.out.println();
		for(Object[] o : arr){
			for(Object t : o){
				if(t==null)
					System.out.print("--");
				else
					System.out.print(t.toString());
				System.out.print("\t|\t");
			}
			System.out.println();
		}
		
		
		PrintWriter writer = new PrintWriter("/home/aptus/Desktop/json2csv.csv", "UTF-8");
		boolean comma = false;
		for(Object[] o : arr){
			comma = false;
			for(Object t : o){
				if(t==null){
					writer.print(comma == true ? "," : "");
				}
				else{
					writer.print(comma == true ? ","+t.toString() : t.toString());
				}
				if(comma == false)
					comma = true;
			}
			writer.println();
		}
		writer.close();
	}
	
	public static Object[] make2D(Object[] cur, Object[] old, JsonElement ele, String path, boolean repeat){
		if(repeat)
			cur = old.clone();
		
		
		
		boolean gotArray = false;
		
		if(ele.isJsonObject()){
			
			/* applying order to JSON.
			 * Order - 
			 * 		1) JSON premitive
			 * 		2) JSON Array
			 * 		3) JSON Object ( order of JSON Object is yet to be descided)
			 * */
			System.out.println(ele);
			//OrderJson makeOrder = new OrderJson();
			ele = makeOrder.orderJson(ele);
			
			for(Map.Entry<String, JsonElement> entry : ele.getAsJsonObject().entrySet()){
				
				if(entry.getValue().isJsonPrimitive()){
					tmpPath = path+entry.getKey();
					//System.out.println(tmpPath);
					tmpPath = tmpPath.replaceAll("(\\/\\/[0-9]+)", "/").replaceAll("\\/\\/+", "/");
					tmpPath = tmpPath.replaceAll("\\/[0-9]+\\/", "/");
					tmpPath = tmpPath.replaceAll("\\(o\\)\\/", "/");
					System.out.println(tmpPath);
					if(unique.contains(tmpPath)){
						int index = unique.indexOf(tmpPath);
						cur[index] = entry.getValue().getAsJsonPrimitive().getAsString();
					}
					tmpPath = null;
				}
				else if(entry.getValue().isJsonObject()){
					cur = (make2D(new Object[unique.size()], cur, entry.getValue().getAsJsonObject(), path + entry.getKey() + "(o)/", repeat));
				}
				else if(entry.getValue().isJsonArray()){
					cur = make2D(new Object[unique.size()], cur, entry.getValue().getAsJsonArray(), path + entry.getKey() + "//", repeat);
					
				}
			}
			
		}
		else if(ele.isJsonArray()){
			int arrIndex = 0;
			
			
			for(JsonElement tmp : ele.getAsJsonArray()){
				
				if(tmp.isJsonPrimitive()){
					tmpPath = path + arrIndex;
					tmpPath = tmpPath.replaceAll("(\\/\\/[0-9]+)", "/").replaceAll("\\/\\/+", "/");
					tmpPath = tmpPath.replaceAll("[0-9]+\\/", "");
					//tmpPath = tmpPath.replaceAll("([0-9]+\\/)", "/").replaceAll("\\/\\/+", "/");
					System.out.println(tmpPath);
					if(unique.contains(tmpPath)){
						int index = unique.indexOf(tmpPath);
						cur[index] = tmp.getAsJsonPrimitive().getAsString();
					}
					tmpPath = null;
				}
				else{					
					if(tmp.isJsonObject()){
						gotArray = isInnerArray(tmp);
						arr.add(make2D(new Object[unique.size()], cur, tmp.getAsJsonObject(), path + arrIndex + "/", repeat));
						if(gotArray){
							arr.remove(arr.size()-1);
						}
					}else if(tmp.isJsonArray()){
						make2D(new Object[unique.size()], cur, tmp.getAsJsonArray(), path + arrIndex + "//", repeat);
					}
					
					
				}
				arrIndex++;
			}
		}
		return cur;
	}
	
	
	public static boolean isInnerArray(JsonElement ele){
		int i = 0;
		for(Map.Entry<String, JsonElement> entry : ele.getAsJsonObject().entrySet()){
			if(entry.getValue().isJsonArray()){
				if(entry.getValue().getAsJsonArray().size()>0)
					return true;
			}
			System.out.println(i++);
			
		}
		return false;
	}
}
