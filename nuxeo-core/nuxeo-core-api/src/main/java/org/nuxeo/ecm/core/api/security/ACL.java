/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.security;

import java.io.Serializable;
import java.util.List;

/**
 * An ACL (Access Control List) is a list of ACEs (Access Control Entry).
 * <p>
 * An ACP may contain several ACL identified by a name.
 * This is to let external modules add security rules. There are 2 default
 * ACLs:
 * <ul>
 * <li> the <code>local</code> ACL - this is the default type of ACL that may
 * be defined by an user locally to a document (using a security UI).
 * <br>
 * This is the only ACL an user can change
 * <li> the <code>inherited</code> - this is a special ACL generated by merging
 * all document parents ACL. This ACL is read only (cannot be modified locally
 * on the document since it is inherited.
 * </ul>
 *
 * ACLs that are used by external modules cannot be modified by the user
 * through the security UI. These ACLs should be modified only programatically
 * by the tool that added them.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface ACL extends List<ACE>, Serializable, Cloneable {

    String LOCAL_ACL = "local";

    String INHERITED_ACL = "inherited";

    /**
     * Gets the ACL name.
     *
     * @return the ACL name
     */
    String getName();

    /**
     * Gets the ACEs defined by this list as an array.
     *
     * @return
     */
    ACE[] getACEs();

    /**
     * Sets the ACEs defined by this ACL.
     *
     * @param aces the ACE array
     */
    void setACEs(ACE[] aces);

    /**
     * Returns a recursive copy of the ACL sharing no mutable substructure with
     * the original.
     *
     * @return a copy
     */
    Object clone();

}