package models;

import java.util.LinkedHashMap;
import java.util.Map;

import play.data.format.Formats;
import play.data.validation.Constraints.Required;
import play.db.ebean.Model;

import javax.persistence.*;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Page;


@Entity
@Table(name = "measurement_units")
public class MeasurementUnits extends Model {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2652922314168606937L;

	@Id
	public Long id;
	
	@ManyToOne
	public Dimensions dimension;
	
	@Required
	@Formats.NonEmpty
	public String system;
	
	@Required
	@Formats.NonEmpty
	public String unitName;
	
	public String unitName2;

	@Required
	@Formats.NonEmpty
	public String unitCode;
	
	public String comments;
	
	
	// Query creation helper
		public static Model.Finder<Long, MeasurementUnits> find = new Model.Finder<Long, MeasurementUnits>(
				Long.class, MeasurementUnits.class);
	
		public static Map<String, String> options() {
			LinkedHashMap<String, String> options = new LinkedHashMap<String, String>();
			for (MeasurementUnits co : MeasurementUnits.find.all()) {
				options.put(co.id.toString(), co.dimension.name+"---"+co.unitName+"---"+co.unitCode);
			}
			return options;
		}
		
		public static Page<MeasurementUnits> page(int page, int pageSize, String sortBy, String order, String filter) {
			String whoql =
					"lower(dimension.name) like :filter OR " +
					"lower(unitName) like :filter OR " +
					"lower(unitName2) like :filter";
					
			
			com.avaje.ebean.Query<MeasurementUnits> query = Ebean.createQuery(MeasurementUnits.class);
			query.where(whoql).
			setParameter("filter","%"+filter+"%");
			
			return query.orderBy(sortBy + " " + order).
					findPagingList(pageSize)
					.setFetchAhead(false).getPage(page);
		}
}
