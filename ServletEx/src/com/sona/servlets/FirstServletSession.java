package com.sona.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class FirstServletSession extends HttpServlet {
	
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
	String userName = req.getParameter("uname");
	String email = req.getParameter("email");
	
	HttpSession session = req.getSession();
	session.setAttribute("nameForSession",userName);		
	
	session.setAttribute("emailForSession",email);
	
	System.out.println("Name without session: "+userName+"and his email id is : "+ email );
	System.out.println("Name with session: "+(String)session.getAttribute("nameForSession") +" and his email id is : " +(String)session.getAttribute("emailForSession"));
	
	resp.sendRedirect("RedirectedToFirstJSP.jsp");
	
	}
}
