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
 *     anguenot
 *
 * $Id: AbstractNuxeoWebService.java 28610 2008-01-09 17:13:52Z sfermigier $
 */

package org.nuxeo.ecm.platform.ws;

import javax.annotation.security.PermitAll;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.api.ws.BaseNuxeoWebService;
import org.nuxeo.ecm.platform.api.ws.session.WSRemotingSession;
import org.nuxeo.ecm.platform.api.ws.session.WSRemotingSessionManager;
import org.nuxeo.ecm.platform.api.ws.session.impl.WSRemotingSessionImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Abstract Nuxeo Web Service.
 * <p>
 * Extend this if you want to share the Web Service remoting sessions with the other Nuxeo platform web services.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public abstract class AbstractNuxeoWebService implements BaseNuxeoWebService {

    private static final long serialVersionUID = 5530614356404354863L;

    private WSRemotingSessionManager sessionsManager;

    /**
     * Returns the platform service that deals with shared Web Service remote sessions.
     *
     * @return a <code>WSRemotiungSessionManager</code> service
     */
    protected WSRemotingSessionManager getSessionsManager() {
        if (sessionsManager == null) {
            sessionsManager = Framework.getService(WSRemotingSessionManager.class);
        }
        return sessionsManager;
    }

    @PermitAll
    @WebMethod
    public String connectOnRepository(@WebParam(name = "userName") String username,
            @WebParam(name = "password") String password, @WebParam(name = "repositoryName") String repositoryName)
            throws ClientException {
        String sid = null;
        try {
            // :FIXME: won't work all the time...
            LoginContext loginContext = Framework.login();
            if (repositoryName == null) {
                RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
                repositoryName = repositoryManager.getDefaultRepositoryName();
            }
            sid = _connect(username, password, repositoryName);
            loginContext.logout();
        } catch (LoginException e) {
            throw new ClientException(e.getMessage(), e);
        }
        return sid;
    }

    @Override
    @PermitAll
    @WebMethod
    public String connect(@WebParam(name = "userName") String username, @WebParam(name = "password") String password)
            throws ClientException {
        return connectOnRepository(username, password, null);
    }

    /*
     * @PermitAll @WebMethod public String connect(String username, String password, String repository) throws
     * ClientException { return _connect(username, password, repository); }
     */

    /**
     * Internal connect method shared in between above connect() methods.
     *
     * @param username the user name.
     * @param password the user password.
     * @param repository the repository name.
     * @return a Nuxeo core session identifier.
     * @throws ClientException
     */
    private String _connect(String username, String password, String repositoryName) throws ClientException {
        // Login before doing anything.
        login(username, password);
        CoreSession session = CoreInstance.openCoreSession(repositoryName);
        String sid = session.getSessionId();
        UserManager userMgr = Framework.getService(UserManager.class);
        WSRemotingSession rs = new WSRemotingSessionImpl(session, userMgr, repositoryName, username, password);
        getSessionsManager().addSession(sid, rs);
        return sid;
    }

    @Override
    @WebMethod
    public void disconnect(@WebParam(name = "sessionId") String sid) throws ClientException {
        WSRemotingSession rs = initSession(sid);
        rs.getDocumentManager().close();
    }

    /**
     * Initializes a new user session from the credentials bound the Web Service remote session.
     *
     * @throws ClientException
     */
    protected void login(String username, String password) throws ClientException {
        try {
            Framework.login(username, password);
        } catch (LoginException e) {
            throw new ClientException("Login failed for " + username, e);
        }
    }

    /**
     * Initializes a user session.
     *
     * @param sid the session identifier.
     * @return a Web Service remoting session instance.
     * @throws ClientException
     */
    protected WSRemotingSession initSession(String sid) throws ClientException {
        WSRemotingSession rs = getSessionsManager().getSession(sid);
        if (rs == null) {
            throw new ClientException("Invalid session id: " + sid);
        }
        login(rs.getUsername(), rs.getPassword());
        return rs;
    }

}
