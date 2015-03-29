package models;

import java.util.Date;

import play.data.format.Formats;
import play.data.validation.Constraints.Required;

import javax.persistence.*;

import play.db.ebean.*;

@Entity
@Table(name = "holidays")
public class Holidays extends Model {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8609783879681729847L;

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	public Long id;
	
	@Required
	@Formats.DateTime(pattern="yyyy/MM/dd")
	public Date holidDate;

	
	// Query creation helper
		public static Model.Finder<Long, Holidays> find = new Model.Finder<Long, Holidays>(
				Long.class, Holidays.class);
	

}
