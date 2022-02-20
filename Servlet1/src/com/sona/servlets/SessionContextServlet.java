package com.sona.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Servlet implementation class SessionContextServlet
 */
@WebServlet(description = "Session Context Example", urlPatterns = { "/SessionContextServletPath" })
public class SessionContextServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public SessionContextServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		PrintWriter writer = response.getWriter();
		String userName = request.getParameter("fname");
		HttpSession session = request.getSession();
		if(userName != null && userName != "") {
			session.setAttribute("fNameSave", userName);
		}
		 response.getWriter().println("User Name from request is : " + userName);
		 response.getWriter().println("User Name from Session is : " + (String)session.getAttribute("fNameSave"));
	}

}
