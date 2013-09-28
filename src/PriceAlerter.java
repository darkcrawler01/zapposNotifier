import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.json.parsers.JSONParser;
import com.json.parsers.JsonParserFactory;




public class PriceAlerter
{

	//<emailid, productid>
	private HashMap<String, ArrayList<String>> alertDirectoryMap = new HashMap<String, ArrayList<String>>();
	private HashMap<String, ArrayList<String>> completedDirectoryMap = new HashMap<String, ArrayList<String>>();
	//product, 
	private HashMap<String, String> productDirectoryMap = new HashMap<String, String>();
	private String fileCheckSum = "";
	 
	private List<String> invalidProductIds = new ArrayList<String>();
	static Properties prop;
	private int pollInterval = 60000;
	private String fileName;
	public PriceAlerter() 
	{
		try
		{
			String temp;
			prop = new Properties();
			prop.load(new FileInputStream("config.properties"));
			fileName = prop.getProperty("customer.csv");
			if (fileName== null || fileName.length() == 0)
			{
				System.err.println(new Date() +":Error in config.properties");
				System.err.println(new Date() +":customer.dat is missing");
				System.err.println(new Date() +":Program is terminating");
				System.exit(1);
			}
			temp = prop.getProperty("poll-interval");
			if (temp == null || temp.length() == 0)
			{
				System.err.println(new Date() +":Error in config.properties");
				System.err.println(new Date() +":poll-interval is missing");
				System.err.println(new Date() +":poll interval set to default - 60 seconds");
			}
			else
			{
				if (temp.matches("[0-9]{1,25}"))
				{
					pollInterval = Integer.parseInt(temp) * 1000;
				}
				else
				{
					System.out.println(new Date() +":Error in config.properties");
					System.out.println(new Date() +":poll-interval is invalid");
					System.out.println(new Date() +":poll interval set to default - 60 seconds");
				}
			}
		}
		catch(Exception e)
		{
			System.out.println(new Date() +":Error in config.properties");
			e.printStackTrace();
			System.exit(1);
		}
	}

	
	
	List parseJSONString(String jsonString)
	{
		JsonParserFactory factory=JsonParserFactory.getInstance();
		JSONParser parser=factory.newJsonParser();

		if (jsonString == null)
		{
			return null;
		}
		try {
			Map jsonMap=parser.parseJson(jsonString);
			if (jsonMap != null && jsonMap.get("statusCode") != null 
					&& ((String)jsonMap.get("statusCode")).equalsIgnoreCase("200"))
			{
				return (List) jsonMap.get("product");
			}
			else if (jsonMap != null && jsonMap.get("statusCode") != null)
			{
				String status = (String)jsonMap.get("statusCode");
				if (status.equalsIgnoreCase("207"))
				{
					String message = (String )jsonMap.get("message");
					if (message != null)
					{
						message = message.substring(message.indexOf("[") + 1, message.indexOf("]"));
						message = message.replace(" ", "");
						invalidProductIds.addAll(Arrays.asList(message.split(",")));
						return (List) jsonMap.get("product");
					}
				}				
			}
		}
		catch(Exception e)
		{
			//error in jsonString parsing
			e.printStackTrace();
		}
		return null;
	}	

	/*
	 * assumed file format :
	 * for each entry
	 * emailid,productid
	 * productid = integer expected
	 */
	boolean parseCustomerInputFile(String  inputFile)
	{

		if (inputFile == null)
		{
			//inputFile error
			return false;
		}
		try {
			System.out.println(new Date() + inputFile + " Parsing started");
			BufferedReader br = new BufferedReader(new FileReader(inputFile));

			String line = br.readLine();
			String[] stringArray;
			ArrayList<String> temp;
			int count =1;
			while (line != null)
			{
				stringArray = line.split(",");
				if (stringArray.length == 2 && stringArray[0].matches("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}")
						&& stringArray[1].matches("[0-9]{1,10}"))
				{
					if (alertDirectoryMap.get(stringArray[0]) != null)
					{
						temp = (ArrayList<String>)alertDirectoryMap.get(stringArray[0]);						
					}
					else
					{
						temp = new ArrayList<String>();						
					}
					temp.add(stringArray[1]);
					alertDirectoryMap.put(stringArray[0], temp);
				}
				else
				{
					System.out.println(new Date() +":Error in line:" + count + " " + line);
				}
				line = br.readLine();
				count++;
			}
			System.out.println(new Date() + inputFile + " Parsing complete");
			br.close();
			
			fileCheckSum = getMD5Checksum(inputFile);
			return true;
		}
		catch(Exception e)
		{
			//error ccured during parsing
			e.printStackTrace();
			return false;
		}
	}

	//getting checksum to avoid processing the same file again
	public static String getMD5Checksum(String filename) {
		System.out.println(new Date() +":Getting MD5checksum start");
		byte[] b = new byte[0];
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
		
		
		InputStream is = new FileInputStream(filename);
			DigestInputStream dis = new DigestInputStream(is, md);
			b = md.digest();
			/* Read stream to EOF as normal... */
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			

		String result = "";

		for (int i=0; i < b.length; i++) {
			result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
		}
		System.out.println(new Date() +":Calculated checksum for " + filename + ":" + result);
		
		return result;
	}
	
	
	
	public boolean alert()
	{
		if (alertDirectoryMap.size() == 0)
			return true;
		
		List<String> emailIds = new ArrayList<String>();
		List<String> productIds = new ArrayList<String>();
		emailIds.addAll(alertDirectoryMap.keySet());
		
		for (String email : emailIds) {
			if (alertDirectoryMap.get(email) != null && ((List)alertDirectoryMap.get(email)).size() != 0)
			{
				productIds.addAll(alertDirectoryMap.get(email));
			}
		}
		
		String productids = productIds.toString().replace(" ", "");
		productids = productids.replace("[", "");
		productids = productids.replace("]", "");
		
		if (productids.length() == 0)
		{
			return true;
		}
		
		List productList;
		try
		{
			productList = parseJSONString(RestClient.makeRestRequest(productids));
			if (productList != null)
			{
				for (Map product : (List<Map>)productList) {
					if (product.get("styles") != null)
					{
						List<Map> styles = (List)product.get("styles");
						String outputHtml = "<p><a href='" + (String)product.get("defaultProductUrl") + "'>" + (String)product.get("productName")  + " (" + (String)product.get("productId")  +")</a></p><ul>";
						boolean flag = false;
						for (Map style : styles) {
							int percentOff = Integer.parseInt(((String)style.get("percentOff")).substring(0 , ((String)style.get("percentOff")).length() -1));
							if (percentOff > 20)
							{
								flag = true;
								outputHtml += "<li><a href='" + (String)style.get("productUrl") + "'>" + (String)style.get("color") + " " + (String)style.get("price") + " (" + (String)style.get("percentOff") +")</a></li>";
								/*
								 * <p><a href="<productDefaultUrl>">productName(Productid)</a></p>
								 * <ul>
								 * <li><a href="<productUrl>"> <color> <price>(<percentOff)</a></li>
								 * "percentOff":"22%","originalPrice":"$65.00","price":"$50.99","color":"Turquoise Time Boa"
								 * </ul>
								 * "productUrl":"http:\/\/www.zappos.com\/product\/7515478\/color\/392888","percentOff":"22%","originalPrice":"$65.00","price":"$50.99","color":"Turquoise Time Boa"
								 * {"brandId":"3310","brandName":"All-Clad","productId":"7903281","productName":"Hard Anodized Non-Stick 12\" Round \"Grille\"",
								 * "styles":[{"styleId":"1727586","imageUrl":"http:\/\/www.zappos.com\/images\/z\/1\/7\/2\/7\/5\/8\/1727586-p-DETAILED.jpg","productUrl":"http:\/\/www.zappos.com\/product\/7903281\/color\/3","percentOff":"48%","originalPrice":"$155.00","price":"$79.99","color":"Black"}],
								 * "defaultProductUrl":"http:\/\/www.zappos.com\/product\/7903281","defaultImageUrl":"http:\/\/www.zappos.com\/images\/z\/1\/7\/2\/7\/5\/8\/1727586-p-DETAILED.jpg"},
								 * {"brandId":"632","brandName":"Sam Edelman","productId":"7515478","productName":"Gigi","styles":[{"styleId":"1788226","imageUrl":"http:\/\/www.zappos.com\/images\/z\/1\/7\/8\/8\/2\/2\/1788226-p-DETAILED.jpg","productUrl":"http:\/\/www.zappos.com\/product\/7515478\/color\/25","percentOff":"0%","originalPrice":"$65.00","price":"$65.00","color":"Almond"},{"styleId":"1788220","imageUrl":"http:\/\/www.zappos.com\/images\/z\/1\/7\/8\/8\/2\/2\/1788220-p-DETAILED.jpg","productUrl":"http:\/\/www.zappos.com\/product\/7515478\/color\/14065","percentOff":"0%","originalPrice":"$65.00","price":"$65.00","color":"Black Boa"},{"styleId":"1413687","imageUrl":"http:\/\/www.zappos.com\/images\/z\/1\/4\/1\/3\/6\/8\/1413687-p-DETAILED.jpg","productUrl":"http:\/\/www.zappos.com\/product\/7515478\/color\/72","percentOff":"0%","originalPrice":"$65.00","price":"$65.00","color":"Black Leather"},{"styleId":"1788227","imageUrl":"http:\/\/www.zappos.com\/images\/z\/1\/7\/8\/8\/2\/2\/1788227-p-DETAILED.jpg","productUrl":"http:\/\/www.zappos.com\/product\/7515478\/color\/142953","percentOff":"0%","originalPrice":"$65.00","price":"$65.00","color":"Black Patent 2"},{"styleId":"1788223","imageUrl":"http:\/\/www.zappos.com\/images\/z\/1\/7\/8\/8\/2\/2\/1788223-p-DETAILED.jpg","productUrl":"http:\/\/www.zappos.com\/product\/7515478\/color\/340353","percentOff":"20%","originalPrice":"$65.00","price":"$51.99","color":"Black White\/New Nude Leopard\/Flamingo"},{"styleId":"2123604","imageUrl":"http:\/\/www.zappos.com\/images\/z\/2\/1\/2\/3\/6\/0\/2123604-p-DETAILED.jpg","productUrl":"http:\/\/www.zappos.com\/product\/7515478\/color\/392899","percentOff":"22%","originalPrice":"$65.00","price":"$50.99","color":"Citron Yellow\/Citron Yellow"},{"styleId":"2211904","imageUrl":"http:\/\/www.zappos.com\/images\/z\/2\/2\/1\/1\/9\/0\/2211904-p-DETAILED.jpg","productUrl":"http:\/\/www.zappos.com\/product\/7515478\/color\/406471","percentOff":"22%","originalPrice":"$65.00","price":"$50.99","color":"Denim\/Ice White\/Saddle"},{"styleId":"1164767","imageUrl":"http:\/\/www.zappos.com\/images\/z\/1\/1\/6\/4\/7\/6\/1164767-p-DETAILED.jpg","productUrl":"http:\/\/www.zappos.com\/product\/7515478\/color\/36798","percentOff":"0%","originalPrice":"$65.00","price":"$65.00","color":"Gold Boa Print"},{"styleId":"2123569","imageUrl":"http:\/\/www.zappos.com\/images\/z\/2\/1\/2\/3\/5\/6\/2123569-p-DETAILED.jpg","productUrl":"http:\/\/www.zappos.com\/product\/7515478\/color\/392886","percentOff":"20%","originalPrice":"$65.00","price":"$51.99","color":"Indigo Blue Boa"},{"styleId":"2211846","imageUrl":"http:\/\/www.zappos.com\/images\/z\/2\/2\/1\/1\/8\/4\/2211846-p-DETAILED.jpg","productUrl":"http:\/\/www.zappos.com\/product\/7515478\/color\/81596","percentOff":"20%","originalPrice":"$65.00","price":"$51.99","color":"Light Gold\/Natural"},{"styleId":"2211850","imageUrl":"http:\/\/www.zappos.com\/images\/z\/2\/2\/1\/1\/8\/5\/2211850-p-DETAILED.jpg","productUrl":"http:\/\/www.zappos.com\/product\/7515478\/color\/7646","percentOff":"20%","originalPrice":"$65.00","price":"$51.99","color":"Light Green"},{"styleId":"2123605","imageUrl":"http:\/\/www.zappos.com\/images\/z\/2\/1\/2\/3\/6\/0\/2123605-p-DETAILED.jpg","productUrl":"http:\/\/www.zappos.com\/product\/7515478\/color\/52624","percentOff":"20%","originalPrice":"$65.00","price":"$51.99","color":"Mandarin Orange"},{"styleId":"1788225","imageUrl":"http:\/\/www.zappos.com\/images\/z\/1\/7\/8\/8\/2\/2\/1788225-p-DETAILED.jpg","productUrl":"http:\/\/www.zappos.com\/product\/7515478\/color\/307257","percentOff":"0%","originalPrice":"$65.00","price":"$65.00","color":"New Nude Leopard"},{"styleId":"2236608","imageUrl":"http:\/\/www.zappos.com\/images\/z\/2\/2\/3\/6\/6\/0\/2236608-p-DETAILED.jpg","productUrl":"http:\/\/www.zappos.com\/product\/7515478\/color\/340452","percentOff":"20%","originalPrice":"$65.00","price":"$51.99","color":"Peach Melba"},{"styleId":"1788214","imageUrl":"http:\/\/www.zappos.com\/images\/z\/1\/7\/8\/8\/2\/1\/1788214-p-DETAILED.jpg","productUrl":"http:\/\/www.zappos.com\/product\/7515478\/color\/616","percentOff":"0%","originalPrice":"$65.00","price":"$65.00","color":"Saddle"},{"styleId":"1164766","imageUrl":"http:\/\/www.zappos.com\/images\/z\/1\/1\/6\/4\/7\/6\/1164766-p-DETAILED.jpg","productUrl":"http:\/\/www.zappos.com\/product\/7515478\/color\/240626","percentOff":"0%","originalPrice":"$65.00","price":"$65.00","color":"Silver Boa Print"},{"styleId":"2123568","imageUrl":"http:\/\/www.zappos.com\/images\/z\/2\/1\/2\/3\/5\/6\/2123568-p-DETAILED.jpg","productUrl":"http:\/\/www.zappos.com\/product\/7515478\/color\/392888","percentOff":"22%","originalPrice":"$65.00","price":"$50.99","color":"Turquoise Time Boa"}],"defaultProductUrl":"http:\/\/www.zappos.com\/product\/7515478","defaultImageUrl":"http:\/\/www.zappos.com\/images\/z\/1\/7\/8\/8\/2\/2\/1788226-p-DETAILED.jpg"}
								*/
							}
						}
						if (flag == true)
						{
							outputHtml += "</ul></p><hr/>";
							productDirectoryMap.put((String)product.get("productId"), outputHtml);
							
						}
					}
					
				}
				
				
				if (productDirectoryMap.size()  == 0)
				{
					return true;
				}
				
				ArrayList<String> tempList;
				HashMap<String, ArrayList<String>> tempAlertDirectoryMap = (HashMap<String, ArrayList<String>>) alertDirectoryMap.clone();
				for (String email : emailIds) {
					if (tempAlertDirectoryMap.get(email) != null && ((List)tempAlertDirectoryMap.get(email)).size() != 0)
					{
						tempAlertDirectoryMap.put(email, (ArrayList<String>)alertDirectoryMap.get(email).clone());
						String output = "<html><body>";
						boolean flag = false;
						for (String productId: (List<String>)tempAlertDirectoryMap.get(email)) 
						{
							if (productDirectoryMap.get(productId) != null 
									&& productDirectoryMap.get(productId).length() > 0)
							{
								flag = true;
								output += productDirectoryMap.get(productId);
								if (completedDirectoryMap.get(email) == null)
								{
									tempList = new ArrayList<String>();
								}
								else
								{
									tempList = completedDirectoryMap.get(productId);
								}
								tempList.add(productId);
								completedDirectoryMap.put(email, tempList);
								((List)alertDirectoryMap.get(email)).remove(productId);
							}
							else if (invalidProductIds.contains(productId))
							{
								((List)alertDirectoryMap.get(email)).remove(productId);
								System.err.println(new Date() +":removing "+productId);
								output += "<p>Product id: #" + productId + " is invalid. Please re-register with proper productId</p><hr/>";
							}

							if (((List)alertDirectoryMap.get(email)).size() == 0)
							{
								 alertDirectoryMap.remove(email);
							}
								
						}
						output += "<p>Hope this alert helps you find the offer you needed!! </p></body></html>";
						if (flag == true)
						{
							output = output.replace("\\/", "/");
							SimpleEmailSendClient.sendMail(prop.getProperty("senderId"), "Product Price Update Notification", output, email);
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return false;
			
		}
		return true;
	}
	
	public static void main(String args[]) throws Exception
	{
		PriceAlerter a = new PriceAlerter();
		a.parseCustomerInputFile(a.fileName);
		while (true)
		{
			
			a.alert();
			if (a.alertDirectoryMap.size() > 0 )
			{
				System.out.println(new Date() +":Going to idle for " + a.pollInterval/1000 + " seconds, before next attempt");
				Thread.sleep(a.pollInterval);
				System.out.println(new Date() +":Attempting again");
			}
			else
			{
				break;
			}
		}
		
		System.out.println(new Date() +":All subscribed customers have been alerted");
		System.out.println(new Date() +":Program is terminating");
		
		
	}

}