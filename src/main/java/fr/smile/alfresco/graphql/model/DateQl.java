package fr.smile.alfresco.graphql.model;

import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;

public class DateQl {

	private Date date;

	public DateQl(Date date) {
		this.date = date;
	}
	
	public static Optional<DateQl> of(Date date) {
		return Optional.ofNullable(new DateQl(date));
	}

	public String getIso() {
		return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().toString();
	}
}