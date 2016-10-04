/**
 * 
 */
package org.elasticsearch.synonym;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Locale;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.synonym.SolrSynonymParser;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.analysis.synonym.WordnetSynonymParser;
import org.elasticsearch.ElasticsearchIllegalArgumentException;
import org.elasticsearch.common.base.Charsets;
import org.elasticsearch.common.io.FastStringReader;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.env.Environment;

/**
 * 
 * 空白文件文件，用于做索引隔离的同义词
 * 
 * @author hailin0@yeah.net
 * @createDate 2016年9月24日
 *
 */
public class BlankSynonymFile implements SynonymFile {

    public static ESLogger logger = Loggers.getLogger("dynamic-synonym");

    private String format;

    private boolean expand;

    private Analyzer analyzer;

    private Environment env;

    /** 本地文件路径 相对于config目录 */
    private String synonymFilePath;

    public BlankSynonymFile(Environment env, Analyzer analyzer, boolean expand, String format,
            String synonymFilePath) {
        this.analyzer = analyzer;
        this.expand = expand;
        this.format = format;
        this.env = env;
        this.synonymFilePath = synonymFilePath;
    }

    @Override
    public SynonymMap reloadSynonymMap() {
        try {
            logger.info("start reload local synonym from {}.", synonymFilePath);
            Reader rulesReader = getReader();
            SynonymMap.Builder parser = null;
            if ("wordnet".equalsIgnoreCase(format)) {
                parser = new WordnetSynonymParser(true, expand, analyzer);
                ((WordnetSynonymParser) parser).parse(rulesReader);
            } else {
                parser = new SolrSynonymParser(true, expand, analyzer);
                ((SolrSynonymParser) parser).parse(rulesReader);
            }
            return parser.build();
        } catch (Exception e) {
            logger.error("reload local synonym {} error!", e, synonymFilePath);
            throw new IllegalArgumentException(
                    "could not reload local synonyms file to build synonyms", e);
        }

    }

    /**
     * 返回空白内容
     */
    @Override
    public Reader getReader() {
        Reader reader = null;
        BufferedReader br = null;
        try {
            StringBuffer sb = new StringBuffer("");
            sb.append("0-0-z,0-0-z").append(System.getProperty("line.separator"));
            reader = new FastStringReader(sb.toString());
        } catch (Exception e) {
            logger.error("get local synonym reader {} error!", e, synonymFilePath);
            throw new IllegalArgumentException("IOException while reading local synonyms file", e);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return reader;
    }

    @Override
    public boolean isNeedReloadSynonymMap() {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.elasticsearch.synonym.SynonymFile#getPath()
     */
    @Override
    public String getFile() {
        return synonymFilePath;
    }

}
