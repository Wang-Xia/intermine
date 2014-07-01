package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.util.FormattedTextParser;
import org.intermine.util.PropertiesUtil;
import org.intermine.metadata.StringUtil;

/**
 * ID resolver for MGI genes.
 *
 * @author Fengyuan Hu
 */
public class MgiIdentifiersResolverFactory extends IdResolverFactory
{
    protected static final Logger LOG = Logger.getLogger(MgiIdentifiersResolverFactory.class);

    // data file path set in ~/.intermine/MINE.properties
    // e.g. resolver.zfin.file=/micklem/data/mgi-identifiers/current/MGI_Coordinate.rpt
    private final String propKey = "resolver.file.rootpath";
    private final String resolverFileSymbo = "mgi";
    private final String taxonId = "10090";

    private static final String NULL_STRING = "null";

    /**
     * Construct without SO term of the feature type.
     */
    public MgiIdentifiersResolverFactory() {
        this.clsCol = this.defaultClsCol;
    }

    /**
     * Construct with SO term of the feature type.
     * @param clsName the feature type to resolve
     */
    public MgiIdentifiersResolverFactory(String clsName) {
        this.clsCol = new HashSet<String>(Arrays.asList(new String[] {clsName}));
    }

    @Override
    protected void createIdResolver() {
        if (resolver != null
                && resolver.hasTaxonAndClassName(taxonId, this.clsCol
                        .iterator().next())) {
            return;
        } else {
            if (resolver == null) {
                if (clsCol.size() > 1) {
                    resolver = new IdResolver();
                } else {
                    resolver = new IdResolver(clsCol.iterator().next());
                }
            }
        }

        try {
            boolean isCachedIdResolverRestored = restoreFromFile();
            if (!isCachedIdResolverRestored || (isCachedIdResolverRestored
                    && !resolver.hasTaxonAndClassName(taxonId, this.clsCol.iterator().next()))) {
                String resolverFileRoot =
                        PropertiesUtil.getProperties().getProperty(propKey);

                if (StringUtils.isBlank(resolverFileRoot)) {
                    String message = "Resolver data file root path is not specified";
                    LOG.warn(message);
                    return;
                }

                LOG.info("Creating id resolver from data file and caching it.");
                String resolverFileName = resolverFileRoot.trim() + resolverFileSymbo;
                File f = new File(resolverFileName);
                if (f.exists()) {
                    createFromFile(f);
                    resolver.writeToFile(new File(idResolverCachedFileName));
                } else {
                    LOG.warn("Resolver file not exists: " + resolverFileName);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Populate the ID resolver from a tab delimited file
     *
     * @param f the file
     * @throws IOException if we can't read from the file
     */
    protected void createFromFile(File f) throws IOException {
        Iterator<?> lineIter = FormattedTextParser.
                parseTabDelimitedReader(new BufferedReader(new FileReader(f)));
        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();

            String type = line[9];
            if (!"Gene".equals(type)) {
                continue;
            }

            String identifier = line[0];    // MGI
            String symbol = line[6];
            String name = line[8];
            String synonymsStr = line[11];

            if (StringUtils.isEmpty(identifier)) {
                continue;
            }

            if (!NULL_STRING.equals(identifier)) {
                resolver.addMainIds(taxonId, identifier, Collections.singleton(identifier));

                if (!NULL_STRING.equals(symbol)) {
                    resolver.addMainIds(taxonId, identifier, Collections.singleton(symbol));
                }
                if (!NULL_STRING.equals(name)) {
                    resolver.addMainIds(taxonId, identifier, Collections.singleton(name));
                }

                if (synonymsStr != null && !synonymsStr.isEmpty()) {
                    resolver.addSynonyms(taxonId, identifier,
                            new HashSet<String>(Arrays.asList(StringUtil.split(synonymsStr, "|"))));
                }
            }
        }
    }
}
