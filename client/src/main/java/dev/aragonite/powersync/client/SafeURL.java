package dev.aragonite.powersync.client;
import java.net.URI;import java.io.IOException;
final class SafeURL {
 static String base(String text)throws IOException {try {URI u=URI.create(text.trim());if(!"https".equals(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null||u.getRawQuery()!=null||u.getRawFragment()!=null||!(u.getPath().isEmpty()||"/".equals(u.getPath()))||u.getPort()==0||u.getPort() < -1||u.getPort()>65535)throw new IOException("Use an HTTPS server address without a path or credentials");return "https://"+u.getRawAuthority();}catch(IllegalArgumentException e){throw new IOException("Invalid HTTPS server address");}}
 static void sameOrigin(String base,String endpoint)throws IOException {try{URI b=URI.create(base),u=URI.create(endpoint);String wanted="wss".equals(u.getScheme())?"https":u.getScheme();if(!"https".equals(wanted)||!b.getRawAuthority().equals(u.getRawAuthority())||u.getUserInfo()!=null||u.getRawQuery()!=null||u.getRawFragment()!=null)throw new IOException("Endpoint is outside the enrolled server");}catch(IllegalArgumentException e){throw new IOException("Invalid server endpoint");}}
}
