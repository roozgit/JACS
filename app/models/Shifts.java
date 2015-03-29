package models;


import java.util.Date;

import play.data.format.Formats;
import play.data.validation.Constraints.Required;

import javax.persistence.*;

import play.db.ebean.*;



@Entity
@Table(name = "shifts")
public class Shifts extends Model {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8094714663832769412L;
	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	public Long id;
	
	public Long sequence;
	
	@ManyToOne
	public Users user;
	
	@Required
	@Formats.DateTime(pattern="yyyy/MM/dd")
	public Date start = new Date();
	
	@Required
	@Formats.DateTime(pattern="yyyy/MM/dd")
	public Date end = new Date();
	
	public Boolean endsSameDay;
	public Boolean allDay=true;
	
	@ManyToOne
	public WorkTypes workType;
	
	// Query creation helper
		public static Model.Finder<Long, Shifts> find = new Model.Finder<Long, Shifts>(
				Long.class, Shifts.class);
		
}
