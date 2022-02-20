package com.sona.servlets;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Servlet implementation class LoginServlet
 */
@WebServlet(description = "Login Servlet", urlPatterns = { "/LoginServletPath" })
public class LoginServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public LoginServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		System.out.println("Inside get method");
		String userName = request.getParameter("userName");
		String password = request.getParameter("password");
		HttpSession session = request.getSession();
		session.setAttribute("userName", userName);
		if(userName.equals("Govind") || userName.equals("Sonali")){
			response.sendRedirect("Success.jsp");
		} else{
			//response.getWriter().println("Sorry Could not login");
			//response.sendRedirect("Failure.jsp");
			RequestDispatcher rd = request.getRequestDispatcher("");
			request.getRequestDispatcher("secondServletPath").forward(request, response);
		}
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		System.out.println("Inside Post method");
		String userName = request.getParameter("userName");
		String password = request.getParameter("password");
		if(userName.equals("Govind")){
			response.getWriter().println("Welcome Govind");
		} else{
			response.getWriter().println("Sorry Could not login");
		}
	}

}
