package dev.aragonite.powersync.client;
import java.net.URI;import java.io.IOException;
final class SafeURL {
 static String base(String text)throws IOException {try {URI u=URI.create(text.trim());if(!"https".equals(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null||u.getRawQuery()!=null||u.getRawFragment()!=null||!(u.getPath().isEmpty()||"/".equals(u.getPath()))||u.getPort()==0||u.getPort() < -1||u.getPort()>65535)throw new UserError(R.string.error_use_an_https_server_address_without_a);return "https://"+u.getRawAuthority();}catch(IllegalArgumentException e){throw new UserError(R.string.error_invalid_https_server_address);}}
 static void sameOrigin(String base,String endpoint)throws IOException {try{URI b=URI.create(base),u=URI.create(endpoint);String wanted="wss".equals(u.getScheme())?"https":u.getScheme();if(!"https".equals(wanted)||!b.getRawAuthority().equals(u.getRawAuthority())||u.getUserInfo()!=null||u.getRawQuery()!=null||u.getRawFragment()!=null)throw new UserError(R.string.error_endpoint_is_outside_the_enrolled_server);}catch(IllegalArgumentException e){throw new UserError(R.string.error_invalid_server_endpoint);}}
}
