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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.mail.action;

import java.util.HashMap;

import javax.mail.Message;

/**
 * The execution context of an actions pipe.
 *
 * @author Alexandre Russel
 *
 */
public class ExecutionContext extends HashMap<String, Object> {

    private static final long serialVersionUID = 1L;

    protected ExecutionContext initialContext;

    public ExecutionContext() {
    }

    public ExecutionContext(Message message) {
        put("message", message);
    }

    public ExecutionContext(Message message, ExecutionContext initialContext) {
        this(message);
        this.initialContext = initialContext;
    }

    public Message getMessage() {
        return (Message) get("message");
    }

    public ExecutionContext getInitialContext() {
        return initialContext;
    }

}