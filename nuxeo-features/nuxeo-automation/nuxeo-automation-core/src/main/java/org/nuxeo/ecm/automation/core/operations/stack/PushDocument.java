/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.operations.stack;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = PushDocument.ID, category = Constants.CAT_EXECUTION_STACK, label = "Push Document", description = "Push the input document on the context stack. The document can be restored later as the input using the corrresponding pop operation. Returns the input document.")
public class PushDocument {

    public static final String ID = "Document.Push";

    @Context
    protected OperationContext ctx;

    @OperationMethod
    public DocumentModel run(DocumentModel doc) throws Exception {
        ctx.push(Constants.O_DOCUMENT, doc);
        return doc;
    }

    @OperationMethod
    public DocumentRef run(DocumentRef doc) throws Exception {
        ctx.push(Constants.O_DOCUMENT, doc);
        return doc;
    }

}