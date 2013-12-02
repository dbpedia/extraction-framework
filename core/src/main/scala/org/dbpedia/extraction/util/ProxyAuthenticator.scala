package org.dbpedia.extraction.util

import java.net.Authenticator
import java.net.PasswordAuthentication
import java.net.Authenticator.RequestorType

class ProxyAuthenticator () extends Authenticator {
    override def getPasswordAuthentication(): PasswordAuthentication = {
        if (getRequestorType() == RequestorType.PROXY) {
            val prot = getRequestingProtocol().toLowerCase()
            val host = System.getProperty(prot + ".proxyHost", "")
            val port = System.getProperty(prot + ".proxyPort", "80")
            val user = System.getProperty(prot + ".proxyUser", "")
            val password = System.getProperty(prot + ".proxyPassword", "")
            
            if (getRequestingHost().equalsIgnoreCase(host)) {
                if (Integer.parseInt(port) == getRequestingPort()) {
                     return new PasswordAuthentication(user, password.toCharArray())
                }
            }
        }
        
        return null
    }
}
