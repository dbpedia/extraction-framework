package org.dbpedia.extraction.live.feeder;

/**
 * @author Claus Stadler
 *         <p/>
 *         Date: 9/20/11
 *         Time: 11:22 AM
 */
public class OntologyUpdateFeederConfig {
    private String rootPrefix = "http://dbpedia.org/ontology/";
    private String expressionPrefix = "http://dbpedia.org/ontology/expr";
    private String reifierPrefix = "http://dbpedia.org/meta/axiom";
    private String dataGraphName = "http://dbpedia.org/ontology";
    private String metaGraphName = "http://dbpedia.org/ontology/meta";
    private String baseUri = "http://mappings.dbpedia.org/";
    private String prefixesFile = "";

    private String serverName = "";
    private int    port = -1;
    private String username = "";
    private String password = "";

    private int pollInterval = 1;
    private int sleepInterval = 60;

    private boolean startNow = false;

    public boolean isStartNow() {
        return startNow;
    }

    public void setStartNow(boolean startNow) {
        this.startNow = startNow;
    }

    public String getRootPrefix() {
        return rootPrefix;
    }

    public void setRootPrefix(String rootPrefix) {
        this.rootPrefix = rootPrefix;
    }

    public String getExpressionPrefix() {
        return expressionPrefix;
    }

    public void setExpressionPrefix(String expressionPrefix) {
        this.expressionPrefix = expressionPrefix;
    }

    public String getReifierPrefix() {
        return reifierPrefix;
    }

    public void setReifierPrefix(String reifierPrefix) {
        this.reifierPrefix = reifierPrefix;
    }

    public String getDataGraphName() {
        return dataGraphName;
    }

    public void setDataGraphName(String dataGraphName) {
        this.dataGraphName = dataGraphName;
    }

    public String getMetaGraphName() {
        return metaGraphName;
    }

    public void setMetaGraphName(String metaGraphName) {
        this.metaGraphName = metaGraphName;
    }

    public String getBaseUri() {
        return baseUri;
    }

    public void setBaseUri(String baseUri) {
        this.baseUri = baseUri;
    }

    public String getPrefixesFile() {
        return prefixesFile;
    }

    public void setPrefixesFile(String prefixesFile) {
        this.prefixesFile = prefixesFile;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getPollInterval() {
        return pollInterval;
    }

    public void setPollInterval(int pollInterval) {
        this.pollInterval = pollInterval;
    }

    public int getSleepInterval() {
        return sleepInterval;
    }

    public void setSleepInterval(int sleepInterval) {
        this.sleepInterval = sleepInterval;
    }
}
