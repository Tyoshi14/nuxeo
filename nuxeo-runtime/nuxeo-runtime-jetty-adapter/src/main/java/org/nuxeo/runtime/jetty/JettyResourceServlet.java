/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.runtime.jetty;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Simple resource servlet used as default servlet when EP is deployed in Jetty.
 *
 * @author Thierry Delprat
 */
public class JettyResourceServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    protected static final int BUFFER_SIZE = 1024 * 10;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String context = req.getContextPath();
        String resourceVPath = req.getRequestURI().substring(context.length());
        String resourcePath = getServletContext().getRealPath(resourceVPath);

        if (!checkAccess(resourcePath)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        File resource = new File(resourcePath);
        if (resource.exists()) {
            if (resource.isDirectory()) {
                resp.sendRedirect("index.jsp");
                // resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            sendFile(resource, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }

    }

    protected boolean checkAccess(String resourcePath) {
        // XXX
        return true;
    }

    protected void sendFile(File resource, HttpServletResponse resp) throws ServletException, IOException {
        InputStream in = null;
        try {
            OutputStream out = resp.getOutputStream();
            in = new FileInputStream(resource);
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                out.flush();
            }
        } finally {
            if (resp != null) {
                resp.flushBuffer();
            }
            if (in != null) {
                in.close();
            }
        }
    }

}
