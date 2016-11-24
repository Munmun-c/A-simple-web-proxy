
import java.io.*; 
import java.util.*;
import java.net.*;

//CACHE
class Cache 
{

	String basePath = null;
	long MinFreeSpace;// in bytes
	Hashtable<String, Date> htable; 
	Config config;

	public Cache(Config configObject)
	{
		config = configObject;
		MinFreeSpace = 15000;
		htable = new Hashtable<String, Date>();

		File cacheDir = new File("Cache");
		cacheDir.mkdirs();
		basePath = cacheDir.getAbsolutePath();
		
		// Initialize cache as empty
		int i;
		File file = new File(basePath);;

		String files[] = file.list();

		for (i=0; i<files.length; i++)
		{
			file = new File(basePath + File.separatorChar + files[i]);
			file.delete();
		}
		config.setBytesCached(0);
		config.setHits(0);
		config.setMisses(0);
	}


	// isCachable - check if URL reply should be cached
	public boolean IsCachable(String rawUrl)
	{
		return (getFileName(rawUrl) != null);
	}
		

	//
	// IsCached - Check if we have in cache what the client wants.
	//
	public boolean IsCached(String rawUrl)
	{
		// Generate filename from URL
		String filename = getFileName(rawUrl);
		if (filename == null)
			return false;

		// Search in hash table
		if (htable.get(filename) != null)
			return true;

		return false;
	}

	//called in case of cache hit
	@SuppressWarnings("finally")
	public FileInputStream getFileInputStream(String rawUrl)
	{
		FileInputStream in = null;
		try
		{
		    String filename = getFileName(rawUrl);

			// Update the hash table entry with current date as value
			htable.put(filename,new Date());

			in = new FileInputStream(filename);
		}
		catch (FileNotFoundException fnf)
		{
			try

			{
				System.out.println("File Not Found:"+getFileName(rawUrl)+" "+fnf);
			}
			catch (Exception e) 
			{}
		}
		finally
		{
			return in;
		}
	}


	//called in case of cache miss; adds new item to cache
    public FileOutputStream getFileOutputStream(String rawUrl)
	{
		FileOutputStream out = null;
		String filename;
		try
		{
		    filename = getFileName(rawUrl);

			out = new FileOutputStream(filename);
		}
		catch (IOException e)
		{}
		finally
		{
			return out;
		}
	}   

    //since multiple threads might write the cache simultaneouly, it must be synchronized
	public synchronized void AddToTable(String rawUrl)
	{
		String filename = getFileName(rawUrl);

		// Add filename to hash table with the current date as its value
		htable.put(filename,new Date());
		config.increaseFilesCached();
	}
	
	// clear the cache
    public synchronized void clean()
	{
		System.out.println("Cleaning the cache...");

		for (Enumeration<String> keys = htable.keys(); keys.hasMoreElements() ;)
		{
			String filename = (String)keys.nextElement();
			File file = new File(filename);
			long nbytes = file.length();
			boolean result = file.delete();
			if (result == true)
			{
				htable.remove(filename);
				config.decreaseFilesCached();

			}
			else
			{
			}
		}
		config.setHits(0);
		config.setMisses(0);
		System.out.println("Cache is clean.");
	}

    //get file name to be cached
	private String getFileName(String rawUrl)
	{
		String filename = basePath + File.separatorChar + rawUrl.substring(7).replace('/','@');

		if (filename.indexOf('?') != -1 || filename.indexOf("cgi-bin") != -1)
		{
			return null;
		}

		return filename;
	}
}


//			Config
// 			contains Configurable parameters of the proxy.

class Config 
{
	
//    private boolean  isFatherProxy;
    private String   fatherProxyHost;
    private int      fatherProxyPort;

	private boolean  isCaching;  // enable/disable caching
	private long 	 cacheSize;  // cache size in bytes
    
    private long     filesCached;
    private long     bytesCached;
    private long     hits;
    private long     misses;

    private final int defaultProxyPort = 80;
	private String localHost;
	private String localIP;

	private String proxyMachineNameAndPort;

	Config()
	{        
		filesCached = 0;
		bytesCached = 0;
		hits = 0;
		misses = 0;

		reset();
	}
	
	//set up the proxy for first use
	public void reset()
	{
//        isFatherProxy = true;
        fatherProxyHost = "proxy.iiit.ac.in";
        fatherProxyPort = 8080;
    	isCaching = true;
//        cleanCache = false;
	}

	public void setProxyMachineNameAndPort(String s)
	{
		proxyMachineNameAndPort = s;
	}

	public String getProxyMachineNameAndPort()
	{
		return proxyMachineNameAndPort;
	}
	
	public void setLocalHost(String host)
	{
		localHost = host;
	}

	public String getLocalHost()
	{
		return localHost;
	}
	
	public void setLocalIP(String ip)
	{
		localIP = ip;
	}
	
	public String getLocalIP()
	{
		return localIP;
	}
	
	boolean getIsCaching()
	{
		return isCaching;
	}

	public synchronized long getCacheSize()
	{
		return cacheSize;
	}
    
    public String getFatherProxyHost()
    {
        return fatherProxyHost;
    }
    
    public int getFatherProxyPort()
    {
        return fatherProxyPort;
    }

    
    public synchronized void increaseFilesCached()
    {
        filesCached++;
    }

    public synchronized void decreaseFilesCached()
    {
        filesCached--;
    }

    public synchronized void setBytesCached(long number)
    {
        bytesCached = number;
    }

    public long getBytesFree()
    {
        return cacheSize - bytesCached;
    }
    
    public long getHits()
    {
        return hits;
    }
    
    public synchronized void increaseHits()
    {
        hits++;
    }

	public synchronized void setHits(long number)
	{
		hits = number;
	}
    
    public long getMisses()
    {
        return misses;
    }
    
    public synchronized void increaseMisses()
    {
        misses++;
    }

	public synchronized void setMisses(long number)
	{
		misses = number;
	}

    public double getHitRatio()
    {
        if ((hits + misses)==0)
            return 0;
        else
            return 100*hits / (hits + misses);
    }
}

// This class handles individual requests from the web browser(client)
public class Proxy extends Thread
{

	String file_to_cache;
	boolean special_char=false;
	Socket ClientSocket = null;              // Socket to client
	Socket SrvrSocket = null;                // Socket to web server
    Cache  cache = null;                     // Static cache manager object 
	String localHostName = null;             // Local machine name
	String localHostIP = null;               // Local machine IP address
	Config config = null;                    // Config object

	//constructor
	Proxy(Socket clientSocket, Cache CacheManager, Config configObject)
	{
		config = configObject;
		ClientSocket = clientSocket;     
		cache = CacheManager;
		localHostName = config.getLocalHost();
		localHostIP = config.getLocalIP();
    }


	//
	// Main work is done here:
	//
    @SuppressWarnings("deprecation")
	public void run()
	{
		URL url;
		String serverName ="";
		byte line[];
        Request request = new Request();		//parse input request to get specific fields
        Reply   reply   = new Reply();			// create reply msg in case of error from server
		FileInputStream fileInputStream = null;
		FileOutputStream fileOutputStream = null;
		boolean TakenFromCache = false;
		boolean isCachable = false;

		try
		{

			// Read HTTP Request from client
            request.parse(ClientSocket.getInputStream());
            url = new URL(request.url);
            System.out.println("Request = " + url);
            

			if (cache.IsCached(url.toString()))
			{
				// If requested page is found in the cache then read the cached file and return
				System.out.println("Found in cache!");
				config.increaseHits();
				TakenFromCache = true;

				// Get FileInputStream from Cache Manager
				fileInputStream  = cache.getFileInputStream(url.toString());
				OutputStream out = ClientSocket.getOutputStream();

				// Send the bits to client
				byte data[] = new byte[2000];
				int count;
				while (-1 < ( count  = fileInputStream.read(data)))
				{
					out.write(data,0,count);
				}
				out.flush();
				fileInputStream.close();
			}


			// page not in cache 
			else
			{
				// Open socket to web server (proxy.iiit.ac.in)
				System.out.println("URL::::"+url.toString());
				if(url.toString().contains("iiit.ac.in"))
				{
					System.out.println("Bypass proxy.iiit.ac.in");
					serverName  = url.getHost();
					System.out.println("Miss! Forwarding to server "+
						serverName + "...");
					config.increaseMisses();
					SrvrSocket = new Socket(serverName(request.url),
										    serverPort(request.url));
					request.url = serverUrl(request.url);
					
				}
				else
				{
					config.increaseMisses();
					SrvrSocket = new Socket(config.getFatherProxyHost(),
						                    config.getFatherProxyPort());
				}
				DataOutputStream srvOut = 
				   new DataOutputStream(SrvrSocket.getOutputStream());


				// Send the url to web server (proxy.iiit.ac.in)
				srvOut.writeBytes(request.toString(false));
				srvOut.flush();

				// Send data to server (needed for post method)
				for (int i =0; i < request.contentLength; i++)
				{
				   SrvrSocket.getOutputStream().write(ClientSocket.getInputStream().read());    
				}
				SrvrSocket.getOutputStream().flush(); 


				//
				// Find if reply should be cached - 
				//   First, check if caching is on.
				//
				isCachable = config.getIsCaching();
				String reasonForNotCaching = "Caching is OFF.";		//caching is on by default

				// if url contains special characters then extra encoding of url required
				if (isCachable)
				{
					isCachable = cache.IsCachable(url.toString());
					if(!isCachable)
					{
						file_to_cache=URLEncoder.encode(url.toString(),"UTF-8");
						special_char=true;
					}
					isCachable=true;
					reasonForNotCaching = "URL not cacheable";		//URL contains special characters
				}

				// check for return code in reply header
				DataInputStream  Din  = 
				   new DataInputStream(SrvrSocket.getInputStream());
				DataOutputStream Dout = 
				   new DataOutputStream(ClientSocket.getOutputStream());
				String str = Din.readLine();
				StringTokenizer s = new StringTokenizer(str);
				String retCode = s.nextToken();
				retCode = s.nextToken();
				// cache if ret code is 200
				if (isCachable)
				{
					if (!retCode.equals("200"))
					{
						isCachable = false;
                        reasonForNotCaching = "Return Code is "+retCode;
					}
				}

                if (isCachable)
				{
					System.out.println("Adding to cache..!!");
					if(!special_char)
						fileOutputStream = cache.getFileOutputStream(url.toString());
					else
						fileOutputStream = cache.getFileOutputStream(file_to_cache);
				}
				else
				{
					System.out.println("NOT Caching the reply.Reason:"
						+reasonForNotCaching);
				}
			
				// First line was read - send it to client and cache it
				String tempStr = new String(str+"\r\n");
				Dout.writeBytes(tempStr);
				if (isCachable)
				{
					// Translate reply string to bytes
					line = new byte[tempStr.length()];
					tempStr.getBytes(0,tempStr.length(),line,0);
					
					// Write bits to file
					fileOutputStream.write(line);
				}


				// Read next lines in reply header, send them to
				// client and cache them
				if (str.length() > 0)
					while (true)
					{
						str = Din.readLine();
						tempStr = new String(str+"\r\n");

						// Send bits to client
						Dout.writeBytes(tempStr);
						
						if (isCachable)
						{
							// Translate reply string to bytes
							line = new byte[tempStr.length()];
							tempStr.getBytes(0,tempStr.length(),line,0);
							
							// Write bits to file
							fileOutputStream.write(line);
						}

						if (str.length() <= 0) 
							break;
					}
				Dout.flush();
            
				//		Handle Reply
				//   (1) Send it to client.
				//   (2) Cache it. add to table
				//
				InputStream  in  = SrvrSocket.getInputStream();
				OutputStream out = ClientSocket.getOutputStream();

				byte data[] = new byte[2000];
				int count;
				while (( count  = in.read(data)) > 0)
				{
					// Send bits to client
					out.write(data,0,count);

					if (isCachable)
					{
						// Write bits to file
						line  = new byte[count];
						System.arraycopy(data,0,line,0,count);
						fileOutputStream.write(line);
					}
				}
				out.flush();
				if (isCachable)
				{
					fileOutputStream.close();
					cache.AddToTable(url.toString());
				}

			}
		}
           
        catch (UnknownHostException uhe)
		{
            // Requested Server could not be located
            System.out.println("Server Not Found.");
           
            try
			{
				// Notify client that server not found
				DataOutputStream out = 
                   new DataOutputStream(ClientSocket.getOutputStream());
                out.writeBytes(reply.formServerNotFound());				//forms reply header
                out.flush();
            }
			catch (Exception uhe2)
			{}
		}
		
		catch (Exception e)
		{
            try
			{
				if (TakenFromCache)
					fileInputStream.close();
				else if (isCachable)
					fileOutputStream.close();
				
				// Notify client that internal error accured in proxy
//				System.out.println("Server Timeout occurred!!!"+ " "+e.toString());
				DataOutputStream out = 
                   new DataOutputStream(ClientSocket.getOutputStream());
                out.writeBytes(reply.formTimeout());
                out.flush();                
				
            }
			catch (Exception uhe2)
			{}
        }
		
		finally
		{
			try
			{
				ClientSocket.getOutputStream().flush();
				ClientSocket.close();
			}
			catch (Exception e)
			{} 
		}
    }
     //parse the url to derive server name and port
    private String serverName(String str)
	{
        int i = str.indexOf("//");   
        if (i< 0) return "";  
        str = str.substring(i+2);
       
        i = str.indexOf("/");
        if (0 < i) str = str.substring(0,i);
 
        i = str.indexOf(":");
        if (0 < i) str = str.substring(0,i);
       
        return str;  
    } 
   
    private int serverPort(String str)
	{
        int i = str.indexOf("//");   
        if (i< 0) return 80;  
        str = str.substring(i+2);
       
        i = str.indexOf("/");
        if (0 < i) str = str.substring(0,i);
 
        i = str.indexOf(":");
        if (0 < i)
		{
            return Integer.parseInt(str.substring(i).trim());
        }
       
        return 80;  
    }  

    private String serverUrl(String str)
	{
        int i = str.indexOf("//");   
        if (i< 0) return str;   

        str = str.substring(i+2);
        i = str.indexOf("/");   
        if (i< 0) return str;  

        return str.substring(i);   
    }
}
