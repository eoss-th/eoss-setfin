package com.th.eoss.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.th.eoss.gcp.predict.YahooPredictor;

public class YahooPredictServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		PrintWriter out = resp.getWriter();
		
		String symbol = req.getParameter("symbol");
		String inputs = req.getParameter("inputs");

		try {
			
			YahooPredictor predictor = new YahooPredictor(symbol);
			out.print(predictor.predict(inputs));
			
		} catch (Exception e) {
			out.print(e.getMessage());
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		String symbol = req.getParameter("symbol");
				
		try {
			YahooPredictor predictor = new YahooPredictor(symbol);
			predictor.train();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}


}
