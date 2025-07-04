/*
 * Copyright (c) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package de.quaddy_services.deadlinereminder.extern;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs a Jetty server on a free port, waiting for OAuth to redirect to it with
 * the verification code.
 * <p>
 * Mostly copied from oacurl by phopkins@google.com.
 * </p>
 * 
 * @author Yaniv Inbar
 */
public final class LocalServerReceiver implements VerificationCodeReceiver {
	private static final Logger LOGGER = LoggerFactory.getLogger(GoogleSync.class);

	private static final String CALLBACK_PATH = "/Callback";

	private static final Object SYNC_OBJECT = new Object();

	/** Server or {@code null} before {@link #getRedirectUri()}. */
	private Server server;

	/** Verification code or {@code null} before received. */
	private String code;

	@Override
	public String getRedirectUri() throws Exception {
		int port = getUnusedPort();
		server = new Server(port);
		for (Connector c : server.getConnectors()) {
			c.setHost("localhost");
		}
		server.addHandler(new CallbackHandler());
		server.start();
		return "http://localhost:" + port + CALLBACK_PATH;
	}

	@Override
	public String waitForCode() {
		int tempWaitSeconds = 3600;
		synchronized (SYNC_OBJECT) {
			while (code == null && tempWaitSeconds > 0) {
				try {
					SYNC_OBJECT.wait(1000);
				} catch (InterruptedException exception) {
					LOGGER.error("should not happen", exception);
					Thread.currentThread().interrupt();
					break;
				}
				tempWaitSeconds--;
			}
		}
		LOGGER.info("waitForCode=" + code);
		return code;
	}

	@Override
	public void stop() throws Exception {
		if (server != null) {
			server.stop();
			server = null;
		}
	}

	private static int getUnusedPort() throws IOException {
		Socket s = new Socket();
		s.bind(null);
		try {
			return s.getLocalPort();
		} finally {
			s.close();
		}
	}

	/**
	 * Jetty handler that takes the verifier token passed over from the OAuth
	 * provider and stashes it where {@link #waitForCode} will find it.
	 */
	class CallbackHandler extends AbstractHandler {

		@Override
		public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch)
				throws IOException {
			LOGGER.info("handle:" + target);
			if (!CALLBACK_PATH.equals(target)) {
				return;
			}
			writeLandingHtml(response);
			response.flushBuffer();
			((Request) request).setHandled(true);
			String error = request.getParameter("error");
			if (error != null) {
				LOGGER.error("Authorization failed. Error=" + error);
			}
			String tempCode = request.getParameter("code");
			LOGGER.info("Received code=" + tempCode);
			synchronized (SYNC_OBJECT) {
				code = tempCode;
				SYNC_OBJECT.notifyAll();
			}
		}

		private void writeLandingHtml(HttpServletResponse response) throws IOException {
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType("text/html");

			PrintWriter doc = response.getWriter();
			doc.println("<html>");
			doc.println("<head><title>OAuth 2.0 Authentication Token Recieved</title></head>");
			doc.println("<body>");
			doc.println("Received verification code. Closing...");
			doc.println("<script type='text/javascript'>");
			// We open "" in the same window to trigger JS ownership of it,
			// which lets
			// us then close it via JS, at least in Chrome.
			doc.println("window.setTimeout(function() {");
			doc.println("    window.open('', '_self', ''); window.close(); }, 1000);");
			doc.println("if (window.opener) { window.opener.checkToken(); }");
			doc.println("</script>");
			doc.println("</body>");
			doc.println("</HTML>");
			doc.flush();
		}
	}
}