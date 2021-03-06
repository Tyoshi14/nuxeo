/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Benoit Delbosc
 */
package org.nuxeo.elasticsearch.test.nxql;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.storage.sql.ra.PoolingRepositoryFactory;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.api.EsResult;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@LocalDeploy("org.nuxeo.elasticsearch.core:elasticsearch-test-contrib.xml")
@RepositoryConfig(cleanup = Granularity.METHOD, repositoryFactoryClass = PoolingRepositoryFactory.class)
public class TestCompareQueryAndFetch {

    @Inject
    protected CoreSession session;

    @Inject
    protected ElasticSearchService ess;

    @Inject
    protected ElasticSearchAdmin esa;

    @Inject
    protected ElasticSearchIndexing esi;

    private String proxyPath;

    @Before
    public void initWorkingDocuments() throws Exception {
        if (!TransactionHelper.isTransactionActive()) {
            TransactionHelper.startTransaction();
        }
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2000, 1 - 1, 2, 3, 4, 5);
        cal.set(Calendar.MILLISECOND, 6);
        for (int i = 0; i < 5; i++) {
            String name = "file" + i;
            DocumentModel doc = session.createDocumentModel("/", name, "File");
            doc.setPropertyValue("dc:title", "File" + i);
            doc.setPropertyValue("dc:nature", "Nature" + i);
            doc.setPropertyValue("dc:rights", "Rights" + i % 2);
            doc.setPropertyValue("dc:issued", cal);
            doc = session.createDocument(doc);
        }
        for (int i = 5; i < 10; i++) {
            String name = "note" + i;
            DocumentModel doc = session.createDocumentModel("/", name, "Note");
            doc.setPropertyValue("dc:title", "Note" + i);
            doc.setPropertyValue("note:note", "Content" + i);
            doc.setPropertyValue("dc:nature", "Nature" + i);
            doc.setPropertyValue("dc:rights", "Rights" + i % 2);
            doc = session.createDocument(doc);
        }

        DocumentModel doc = session.createDocumentModel("/", "hidden", "HiddenFolder");
        doc.setPropertyValue("dc:title", "HiddenFolder");
        doc = session.createDocument(doc);

        DocumentModel folder = session.createDocumentModel("/", "folder", "Folder");
        folder.setPropertyValue("dc:title", "Folder");
        folder = session.createDocument(folder);

        DocumentModel file = session.getDocument(new PathRef("/file3"));
        DocumentModel proxy = session.publishDocument(file, folder);
        proxyPath = proxy.getPathAsString();

        session.followTransition(new PathRef("/file1"), "delete");
        session.followTransition(new PathRef("/note5"), "delete");

        session.checkIn(new PathRef("/file2"), VersioningOption.MINOR, "for testing");

        TransactionHelper.commitOrRollbackTransaction();

        // wait for async jobs
        WorkManager wm = Framework.getLocalService(WorkManager.class);
        Assert.assertTrue(wm.awaitCompletion(20, TimeUnit.SECONDS));
        Assert.assertEquals(0, esa.getPendingCommandCount());
        Assert.assertEquals(0, esa.getPendingWorkerCount());

        esa.refresh();
        TransactionHelper.startTransaction();
    }

    @After
    public void cleanWorkingDocuments() throws Exception {
        // prevent NXP-14686 bug that prevent cleanupSession to remove version
        session.removeDocument(new PathRef(proxyPath));
    }

    protected String getDigest(IterableQueryResult docs) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (Map<String, Serializable> doc : docs) {
            List<String> keys = new ArrayList<>(doc.keySet());
            Collections.sort(keys);
            Map<String, Serializable> sortedMap = new LinkedHashMap<>();
            for (String key : keys) {
                Serializable value = doc.get(key);
                if (value instanceof Calendar) {
                    // ISO 8601
                    value = String.format("%tFT%<tT.%<tL%<tz", (Calendar) value);
                }
                sortedMap.put(key, value);
            }
            sb.append(sortedMap.entrySet().toString());
            sb.append("\n");
        }
        return sb.toString();
    }

    protected void assertSameDocumentLists(IterableQueryResult expected, IterableQueryResult actual) throws Exception {
        Assert.assertEquals(getDigest(expected), getDigest(actual));
    }

    protected void compareESAndCore(String nxql) throws Exception {
        IterableQueryResult coreResult = session.queryAndFetch(nxql, NXQL.NXQL);
        EsResult esRes = ess.queryAndAggregate(new NxQueryBuilder(session).nxql(nxql).limit(20));
        IterableQueryResult esResult = esRes.getRows();
        assertSameDocumentLists(coreResult, esResult);
        coreResult.close();
        esResult.close();
    }

    @Test
    public void testSimpleSearchWithSort() throws Exception {
        compareESAndCore("select ecm:uuid, dc:title, dc:nature from Document order by ecm:uuid");
        compareESAndCore("select ecm:uuid, dc:title from Document where ecm:currentLifeCycleState != 'deleted' order by ecm:uuid");
        compareESAndCore("select dc:nature from File order by dc:nature");
        // TODO some timezone issues here...
        // compareESAndCore("select ecm:uuid, dc:issued from File order by ecm:uuid");
    }

}
