package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.*;

import java.io.File;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.BufferedReader;
import java.io.FileWriter;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;

import org.intermine.xml.full.FullRenderer;
import org.intermine.xml.full.FullParser;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemFactory;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.flymine.io.gff3.GFF3Parser;
import org.flymine.io.gff3.GFF3Record;
import org.flymine.dataconversion.GFF3Converter;

public class ChadoGFF3RecordHandlerTest extends TestCase
{

    Model tgtModel;
    ChadoGFF3RecordHandler handler;
    MockItemWriter writer = new MockItemWriter(new LinkedHashMap());
    String seqClsName = "Chromosome";
    String orgAbbrev = "DM";
    String infoSourceTitle = "FlyBase";
    GFF3Converter converter;
    String tgtNs;
    ItemFactory itemFactory;

    public void setUp() throws Exception {
        tgtModel = Model.getInstanceByName("genomic");
        handler = new ChadoGFF3RecordHandler(tgtModel);
        converter = new GFF3Converter(writer, seqClsName, orgAbbrev, infoSourceTitle, tgtModel,
                                      handler);
        tgtNs = tgtModel.getNameSpace().toString();
        itemFactory = handler.getItemFactory();
    }

    public void testParseFlyBaseId() throws Exception {
        List dbxrefs = new ArrayList(Arrays.asList(new String[] {"FlyBase:FBgn1234", "FlyBase:FBtr1234"}));
        assertEquals("FBgn1234", handler.parseFlyBaseId(dbxrefs, "FBgn").get(0));
        assertEquals("FBtr1234", handler.parseFlyBaseId(dbxrefs, "FBtr").get(0));
    }

    public void testHandleGene() throws Exception {
        String gff = "4\t.\tgene\t230506\t233418\t.\t+\t.\tID=CG1234;Name=Crk;Dbxref=FlyBase:FBan0001587,FlyBase:FBgn0024811;synonym=Crk;synonym_2nd=CRK,D-CRK,Crk,CG5678";
        BufferedReader srcReader = new BufferedReader(new StringReader(gff));

        Iterator iter = GFF3Parser.parse(srcReader);
        GFF3Record record = (GFF3Record) iter.next();

        Item feature = itemFactory.makeItem(null, tgtNs + "Gene", "");
        feature.setAttribute("identifier", "CG1234");

        handler.setFeature(feature);
        handler.process(record);

        Item expectedGene = itemFactory.makeItem(feature.getIdentifier(), tgtNs + "Gene", "");
        expectedGene.setAttribute("organismDbId", "FBgn0024811");
        expectedGene.setAttribute("identifier", "CG1234");
        expectedGene.setAttribute("name", "CG1234");

        assertEquals(7, handler.getItems().size());

        Item actualGene = null;
        iter = handler.getItems().iterator();
        while (iter.hasNext()) {
            Item item = (Item) iter.next();
            if (item.getClassName().equals(tgtNs + "Gene")) {
                actualGene = item;
                expectedGene.setIdentifier(actualGene.getIdentifier());
            }
        }
        assertEquals(expectedGene, actualGene);
    }


    public void testHandleGeneNoDbxref() throws Exception {
        String gff = "4\t.\tgene\t230506\t233418\t.\t+\t.\tID=CG1234;Dbxref=FlyBase:FBan0001587;dbxref_2nd=FlyBase:FBgn0024811";
        BufferedReader srcReader = new BufferedReader(new StringReader(gff));

        Iterator iter = GFF3Parser.parse(srcReader);
        GFF3Record record = (GFF3Record) iter.next();

        Item feature = itemFactory.makeItem(null, tgtNs + "Gene", "");
        feature.setAttribute("identifier", "CG1234");

        handler.setFeature(feature);
        handler.process(record);

        Item expectedGene = itemFactory.makeItem(feature.getIdentifier(), tgtNs + "Gene", "");
        expectedGene.setAttribute("organismDbId", "FBgn0024811");
        expectedGene.setAttribute("identifier", "CG1234");
        expectedGene.setAttribute("name", "CG1234");

        assertEquals(3, handler.getItems().size());

        Item actualGene = null;
        iter = handler.getItems().iterator();
        while (iter.hasNext()) {
            Item item = (Item) iter.next();
            if (item.getClassName().equals(tgtNs + "Gene")) {
                actualGene = item;
                expectedGene.setIdentifier(actualGene.getIdentifier());
            }
        }
        assertEquals(expectedGene, actualGene);
    }

    public void testHandleGeneNoFbgn() throws Exception {
        String gff = "4\t.\tgene\t230506\t233418\t.\t+\t.\tID=CG1234;Dbxref=FlyBase:FBan0001587";
        BufferedReader srcReader = new BufferedReader(new StringReader(gff));

        Iterator iter = GFF3Parser.parse(srcReader);
        GFF3Record record = (GFF3Record) iter.next();

        Item feature = itemFactory.makeItem(null, tgtNs + "Gene", "");
        feature.setAttribute("identifier", "CG1234");

        handler.setFeature(feature);
        handler.process(record);

        assertEquals(0, handler.getItems().size());
    }


    public void testHandleTRNA() throws Exception {
        String gff = "2L\t.\ttRNA\t1938089\t1938159\t.\t-\t.\tID=CR31667;Dbxref=FlyBase:FBan0031667,FlyBase:FBgn0051667;synonym=CR31667;synonym_2nd=CG31667";

        BufferedReader srcReader = new BufferedReader(new StringReader(gff));
        Iterator iter = GFF3Parser.parse(srcReader);
        GFF3Record record = (GFF3Record) iter.next();

        Item feature = itemFactory.makeItem(null, tgtNs + "TRNA", "");
        feature.setAttribute("identifier", "CR31667");

        handler.setFeature(feature);
        handler.process(record);

        Item expectedGene = itemFactory.makeItem(null, tgtNs + "Gene", "");
        expectedGene.setAttribute("identifier", "CG31667");
        expectedGene.setAttribute("organismDbId", "FBgn0051667");

        assertEquals(6, handler.getItems().size());

        Item actualGene = null;
        iter = handler.getItems().iterator();
        while (iter.hasNext()) {
            Item item = (Item) iter.next();
            if (item.getClassName().equals(tgtNs + "Gene")) {
                actualGene = item;
                expectedGene.setIdentifier(actualGene.getIdentifier());
            }
        }
        assertEquals(expectedGene, actualGene);



        Item expectedRelation = itemFactory.makeItem(null, tgtNs + "SimpleRelation", "");
        expectedRelation.setReference("object", expectedGene.getIdentifier());
        expectedRelation.setReference("subject", feature.getIdentifier());

        Item actualRelation = null;
        iter = handler.getItems().iterator();
        while (iter.hasNext()) {
            Item item = (Item) iter.next();
            if (item.getClassName().equals(tgtNs + "SimpleRelation")) {
                actualRelation = item;
                expectedRelation.setIdentifier(actualRelation.getIdentifier());
            }
        }
        assertEquals(expectedRelation, actualRelation);
    }


}
