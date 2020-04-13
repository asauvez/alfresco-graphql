package fr.smile.alfresco.graphql.model;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.Date;

import graphql.schema.DataFetchingEnvironment;

public class DateQl {

	private Date date;

	public DateQl(Date date) {
		this.date = date;
	}
	
	public String getIso() {
		return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().toString();
	}
	
	public String getFormat(DataFetchingEnvironment env) {
		DateFormat dateFormat = new SimpleDateFormat(env.getArgument("format"));
		return dateFormat.format(date);
	}
}