import java.io.IOException;
import java.util.Date;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;


public class RestClient {

	public static String makeRestRequest(String parametersProductId) throws IOException{
		CloseableHttpClient httpclient = HttpClients.createDefault();


		try {

			//http://api.zappos.com/Product/id/7515478,7903281?includes=[%22styles%22]&key=52ddafbe3ee659bad97fcce7c53592916a6bfd73

			HttpGet httpget = new HttpGet("http://api.zappos.com/Product/id/" + parametersProductId +"?includes=[%22styles%22]&key=52ddafbe3ee659bad97fcce7c53592916a6bfd73");

			System.out.println(new Date() + ":executing request " + httpget.getURI());

			// Create a custom response handler
			ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

				public String handleResponse(
						final HttpResponse response) throws ClientProtocolException, IOException {
					int status = response.getStatusLine().getStatusCode();
					if (status >= 200 && status < 300) {
						HttpEntity entity = response.getEntity();
						return entity != null ? EntityUtils.toString(entity) : null;
					} else {
						throw new ClientProtocolException("Unexpected response status: " + status);
					}
				}

			};
			String responseBody = httpclient.execute(httpget, responseHandler);
			System.out.println(":----------------------------------------");
			System.out.println(new Date() + ":" + responseBody);
			System.out.println(":----------------------------------------");
			return responseBody;
		}catch(Exception e)
		{
			e.printStackTrace();
		}finally {
			httpclient.close();
		}
		
		return null;


	}
}
