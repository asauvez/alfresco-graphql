package fr.smile.alfresco.graphql.query;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.Date;

import graphql.schema.DataFetchingEnvironment;

public class DateQL {

	private Date date;

	public DateQL(Date date) {
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