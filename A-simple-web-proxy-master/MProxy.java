

import java.net.*;


// 
// 	Main class that creates the main socket to web browser and maintains the threads
//
public class MProxy extends Thread
{

	static ServerSocket MainSocket = null;
    static Cache cache = null;
	static Config config;

	final static int defaultDaemonPort = 1777;
	final static int maxDaemonPort = 65536;

	
	public static void main(String args[])
	{
		int daemonPort;

		switch (args.length)
		{
			case 0: daemonPort = defaultDaemonPort;
					break;

			case 1: try
					{
						daemonPort = Integer.parseInt(args[0]);
					}
					catch (NumberFormatException e)//Error
					{
						System.out.println("Error: Invalid daemon port");
						return;
					}
					if (daemonPort > maxDaemonPort)//Error
					{
						System.out.println("Error: Port out of range.");
						return;
					}
					break;

			default:System.out.println("Use: Proxy [port]");//Error
					return;
		}

					
		
		try
		{
			System.out.println("Starting MY Proxy...");
			config = new Config();
			config.setLocalHost(InetAddress.getLocalHost().getHostName());
			String tmp = InetAddress.getLocalHost().toString();
			config.setLocalIP(tmp.substring(tmp.indexOf('/')+1));
			config.setProxyMachineNameAndPort(InetAddress.getLocalHost().getHostName()+":"+daemonPort);

			cache = new Cache(config);
			System.out.println("OK");

			// Create main socket... MY-Proxy<--->Web browser
            System.out.print("Create Main Socket...");
			MainSocket = new ServerSocket(daemonPort);
			System.out.println(" port " + daemonPort + " OK");

			System.out.println("Parent Proxy "+	config.getFatherProxyHost()+":"+config.getFatherProxyPort()+" .");
			//Setup Success!!!
			System.out.println("Proxy Setup and ready!");

			// loop indefinitely till admin stops the proxy
			while (true)
			{
				// Listen on main socket
				Socket ClientSocket = MainSocket.accept();

				// Pass request to a new proxy_handler thread
				Proxy thd = new Proxy(ClientSocket,cache,config);
				thd.start();
			}
			
		}
		
		catch (Exception e)
		{}

		finally
		{
			try
			{
				MainSocket.close();	//CLOSE the socket (Myproxy---Web browser)
			}
			catch (Exception exc)
			{
				System.out.println(exc.toString());
			}
		}
	}
}


