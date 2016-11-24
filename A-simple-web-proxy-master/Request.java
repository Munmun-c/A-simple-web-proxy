
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.DataInputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;


public class Request 
{
   
   /**
    * Http Request method. Such as get or post.
    */
   public  String method = new String();

   //requested URL
   public String url   = new String();
   
   //HTTP Version
   public String version           = new String();

   public String userAgent         = new String();

   public String referer           = new String();

   public String ifModifiedSince   = new String();

   public String accept            = new String();

   public String authorization     = new String();
   public String contentType       = new String();
   public int    contentLength     = -1;
   public int    oldContentLength  = -1;
   public String unrecognized      = new String();
   public boolean pragmaNoCache    = false;

   static String CR ="\r\n";


/**
 * Parses a http header from a stream.
 *
 * @param in  The stream to parse. 
 * @return    true if parsing sucsessfull.
 */
public boolean parse(InputStream In)
   {
       String CR ="\r\n";

       /*
        * Read by lines
        */
       DataInputStream lines;
       StringTokenizer tz;
       BufferedReader lines2;
       try 
	   {
            lines = new DataInputStream(In);
//           lines2 = new BufferedReader(new InputStreamReader(In));
    	   lines2 = new BufferedReader(new InputStreamReader(lines, "UTF-8"));
           tz = new StringTokenizer(lines2.readLine());
           // tz = new StringTokenizer(lines.readLine());
       }
	   catch (Exception e) 
	   {
           return false;
       }

       /*
        * HTTP COMMAND LINE < <METHOD==get> <URL> <HTTP_VERSION> >
        */
       method = getToken(tz).toUpperCase();
       url    = getToken(tz);
       version= getToken(tz);
               
       while (true) 
	   {
           try 
		   {
               // tz = new StringTokenizer(lines.readLine());
           } 
		   catch (Exception e) 
		   {
               return false;
           }
           String Token = getToken(tz); 
           
           // look for termination of HTTP command
           if (0 == Token.length())
               break;
           
           if (Token.equalsIgnoreCase("USER-AGENT:"))
           {
               // line =<User-Agent: <Agent Description>>
               userAgent = getRemainder(tz);           
           }
           else if (Token.equalsIgnoreCase("ACCEPT:"))
           {
               // line=<Accept: <Type>/<Form>
               // examp: Accept image/jpeg
               accept += " " + getRemainder(tz);

           }
           else if (Token.equalsIgnoreCase("REFERER:"))
           {
               // line =<Referer: <URL>>
               referer = getRemainder(tz);

           }
           else if (Token.equalsIgnoreCase("PRAGMA:"))
           { 
               // Pragma: <no-cache>
               Token = getToken(tz);

               if (Token.equalsIgnoreCase("NO-CACHE"))
                   pragmaNoCache = true;
               else
                   unrecognized += "Pragma:" + Token + " "
                       +getRemainder(tz) +"\n";            
           }
           else if (Token.equalsIgnoreCase("AUTHORIZATION:"))
           { 
               // Authenticate: Basic UUENCODED
               authorization=  getRemainder(tz);

           }
           else if (Token.equalsIgnoreCase("IF-MODIFIED-SINCE:"))
           {
               // line =<If-Modified-Since: <http date>
               // *** Conditional GET replaces HEAD method ***
               String str = getRemainder(tz);
              int index = str.indexOf(";");
              if (index == -1) {
                   ifModifiedSince  =str;
               } else {
                   ifModifiedSince  =str.substring(0,index);
                  
                  index = str.indexOf("=");
                  if (index != -1) {
                      str = str.substring(index+1);
                      oldContentLength =Integer.parseInt(str);
                  }
              }
           }
           else if (Token.equalsIgnoreCase("CONTENT-LENGTH:"))
           {
               Token = getToken(tz);
               contentLength =Integer.parseInt(Token);
           }
           else if (Token.equalsIgnoreCase("CONTENT-TYPE:"))
           {
               contentType = getRemainder(tz);
           }
           else
           {  
               unrecognized += Token + " " + getRemainder(tz) + CR;
           }
       }
       return true;
   }
           
   /*
    * Rebuilds the header in a string
    * @returns      The header in a string.
    */
   public String toString(boolean sendUnknowen) {
       String Request; 

       if (0 == method.length())
            method = "GET";

       Request = method +" "+ url + " HTTP/1.0" + CR;

       if (0 < userAgent.length())
           Request +="User-Agent:" + userAgent + CR;

       if (0 < referer.length())
           Request+= "Referer:"+ referer  + CR;

       if (pragmaNoCache)
           Request+= "Pragma: no-cache" + CR;

       if (0 < ifModifiedSince.length())
           Request+= "If-Modified-Since: " + ifModifiedSince + CR;
           
       // ACCEPT TYPES //
       if (0 < accept.length())
           Request += "Accept: " + accept + CR;
       else 
           Request += "Accept: */"+"* \r\n";
    
       if (0 < contentType.length())
           Request += "Content-Type: " + contentType   + CR;

       if (0 < contentLength)
           Request += "Content-Length: " + contentLength + CR;
                           
       
       if (0 != authorization.length())
           Request += "Authorization: " + authorization + CR;

       if (sendUnknowen) {
           if (0 != unrecognized.length())
               Request += unrecognized;
       }   

       Request += CR;
       
       return Request;
   }


   public String toString() {
       return toString(true);
   }

   String  getToken(StringTokenizer tk){
       String str ="";
       if  (tk.hasMoreTokens())
           str =tk.nextToken();
       return str; 
   }   
   
   String  getRemainder(StringTokenizer tk){
       String str ="";
       if  (tk.hasMoreTokens())
           str =tk.nextToken();
       while (tk.hasMoreTokens()){
           str +=" " + tk.nextToken();
       }
       return str;
   }

} 
