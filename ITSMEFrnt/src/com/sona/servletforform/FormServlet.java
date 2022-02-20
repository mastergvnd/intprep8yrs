package com.sona.servletforform;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sona.BackendDB.DBform;

public class FormServlet extends HttpServlet {
       
		protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
			
			String uName = request.getParameter("fname");
			String pwd = request.getParameter("pswrd");
			
			response.getWriter().println("The value which user entered in name is: "+ uName);
		    response.getWriter().println("The value which user entered in password is: "+ pwd);
		    
		    DBform formDB = new DBform();
		    int n = formDB.generate(uName, pwd);
		    
		    response.getWriter().println("Number of records inserted is : "+ n);
		    
	}
}
