package org.intermine.web;

/* 
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.SingletonResults;

import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.userprofile.Tag;
import org.intermine.model.userprofile.UserProfile;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.util.XmlBinding;

import java.io.InputStream;

import junit.framework.TestCase;

/**
 *
 * @author Kim Rutherford
 */
public class InitialiserPluginTest extends TestCase
{
    private Profile bobProfile, sallyProfile;
    private ProfileManager pm;
    private ObjectStore os, userProfileOS;
    private ObjectStoreWriter osw, userProfileOSW;
    private Integer bobId = new Integer(101);
    private Integer sallyId = new Integer(102);
    private String bobPass = "bob_pass";
    private String sallyPass = "sally_pass";

    public InitialiserPluginTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.unittest");
        os = osw.getObjectStore();

        userProfileOSW =  ObjectStoreWriterFactory.getObjectStoreWriter("osw.userprofile-test");
        userProfileOS = userProfileOSW.getObjectStore();

        pm = new NonCheckingProfileManager(os, userProfileOSW);
        XmlBinding binding = new XmlBinding(osw.getModel());
        InputStream is =
            getClass().getClassLoader().getResourceAsStream("testmodel_data.xml");
        List objects = (List) binding.unmarshal(is);

        osw.beginTransaction();
        Iterator iter = objects.iterator();
        int i = 1;
        while (iter.hasNext()) {
            InterMineObject o = (InterMineObject) iter.next();
            o.setId(new Integer(i++));
            osw.store(o);
        }
        osw.commitTransaction();

    }

    private class NonCheckingProfileManager extends ProfileManager {
        public NonCheckingProfileManager(ObjectStore os, ObjectStoreWriter userProfileOSW) {
            super(os, userProfileOSW);
        }
        
        // override to prevent the checker from objecting to
        // "org.intermine.model.testmodel.Wibble" in testCleanTags()
        protected Map makeTagCheckers(final Model model) {
            Map checkersMap = new HashMap();
            TagChecker classChecker = new TagChecker() {
                void isValid(String tagName, String objectIdentifier, String type,
                             UserProfile userProfile) {
                    // empty
                }
            };
            checkersMap.put("class", classChecker);
            return checkersMap;
        };

    }
    private void setUpUserProfiles() throws Exception {

        PathQuery query = new PathQuery(Model.getInstanceByName("testmodel"));

        // bob's details
        String bobName = "bob";

        bobProfile = new Profile(pm, bobName, bobId, bobPass,
                                 new HashMap(), new HashMap(), new HashMap());

        pm.saveProfile(bobProfile);
    }


    public void tearDown() throws Exception {
        if (osw.isInTransaction()) {
            osw.abortTransaction();
        }
        Query q = new Query();
        QueryClass qc = new QueryClass(InterMineObject.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        SingletonResults res = new SingletonResults(q, osw.getObjectStore(), osw.getObjectStore()
                                                    .getSequence());
        Iterator resIter = res.iterator();
        osw.beginTransaction();
        while (resIter.hasNext()) {
            InterMineObject o = (InterMineObject) resIter.next();
            osw.delete(o);
        }
        osw.commitTransaction();
        osw.close();

        cleanUserProfile();

    }

    private void cleanUserProfile() throws ObjectStoreException {
        if (userProfileOSW.isInTransaction()) {
            userProfileOSW.abortTransaction();
        }
        Query q = new Query();
        QueryClass qc = new QueryClass(Tag.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        SingletonResults res = new SingletonResults(q, userProfileOS,
                                                    userProfileOS.getSequence());
        Iterator resIter = res.iterator();
        userProfileOSW.beginTransaction();
        while (resIter.hasNext()) {
            InterMineObject o = (InterMineObject) resIter.next();
            userProfileOSW.delete(o);
        }

        removeUserProfile("bob");
        removeUserProfile("sally");

        userProfileOSW.commitTransaction();
        userProfileOSW.close();
    }

    private void removeUserProfile(String username) throws ObjectStoreException {
        Query q = new Query();
        QueryClass qc = new QueryClass(UserProfile.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        QueryField qf = new QueryField(qc, "username");
        SimpleConstraint sc = new SimpleConstraint(qf, ConstraintOp.EQUALS, new QueryValue(username));
        q.setConstraint(sc);
        SingletonResults res = new SingletonResults(q, userProfileOS,
                                                    userProfileOS.getSequence());
        Iterator resIter = res.iterator();
        while (resIter.hasNext()) {
            InterMineObject o = (InterMineObject) resIter.next();
            userProfileOSW.delete(o);
        }
    }
    

    public void testCleanTags() throws Exception {
        setUpUserProfiles();
        pm.addTag("test-tag1", "org.intermine.model.testmodel.Department", "class", "bob");
        pm.addTag("test-tag2", "org.intermine.model.testmodel.Department", "class", "bob");
        pm.addTag("test-tag2", "org.intermine.model.testmodel.Employee", "class", "bob");

        // test that these go away
        pm.addTag("test-tag", "org.intermine.model.testmodel.Wibble", "class", "bob");
        pm.addTag("test-tag", "org.intermine.model.testmodel.Aardvark", "class", "bob");

        InitialiserPlugin.cleanTags(pm);
        
        List tags = pm.getTags(null, null, "class", null);
        assertEquals(3, tags.size());
    }
}
