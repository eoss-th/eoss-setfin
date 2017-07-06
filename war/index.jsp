<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>SET Predictor</title>
</head>
<body>
<center>
<input type="text" id="symbol" placeholder="Enter Symbol">
<input type="button" value="Predict MA of <%= com.th.eoss.util.CalendarUtil.asOfWeek() %>" onclick="predict()"><br/>
<img id="loading" src="loading.gif">
<h1 id="result"></h1>
</center>
<script>
	var loading = document.getElementById("loading");
	var result = document.getElementById("result");
	var symbol = "";
	var loop;
	loading.hidden = true;
	
	function predict () {
		
		document.getElementById("symbol").value = document.getElementById("symbol").value.toUpperCase();		
		symbol = document.getElementById("symbol").value;
		
		var http = new XMLHttpRequest();
		var url = "YahooPredictServlet";
		var params = "symbol=" + symbol;
		http.open("POST", url, true);

		//Send the proper header information along with the request
		http.setRequestHeader("Content-type", "application/x-www-form-urlencoded");

		http.onreadystatechange = function() {//Call a function when the state changes.
		    if(http.readyState == 4 && http.status == 200) {
		    	loop = setInterval(function () {
		    		get(); }, 3000);
		    }
		}
		http.send(params);		
		loading.hidden = false;
		result.innerHTML = "";
	}
	
	function get() {
		
		var http = new XMLHttpRequest();
		var url = "YahooPredictServlet?symbol=" + symbol;
		http.open("GET", url, true);

		http.onreadystatechange = function() {//Call a function when the state changes.
		    if(http.readyState == 4 && http.status == 200) {
		       if (http.responseText!="RUNNING") {
			   		clearInterval(loop);
		    	    symbol = "";
			   		loading.hidden = true;
			   		result.innerHTML = http.responseText;
		       } 
		    }
		}
		http.send(null);		
	}
</script>
</body>
</html>