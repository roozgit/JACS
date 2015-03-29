package models;


import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

import com.avaje.ebean.Page;

import models.equipment.Equipments;
import play.data.format.Formats;
import play.data.validation.Constraints.Required;
import play.db.ebean.*;

@Entity
@Table(name="datasheets")
public class Datasheet extends Model {

	private static final long serialVersionUID = 5793462631362226007L;

	@Id
	public Long id;
	
	@ManyToOne
	public Installations parentInstallation;
	
	@ManyToOne
	public Plants parentPlant;
	
	@ManyToOne
	public Sections parentSection;
	
	@ManyToOne
	public Equipments parentEquipment;
	
	@ManyToOne
	public Subunits parentSubunit;
	
	@ManyToOne
	public Components parentComponent;
	
	@ManyToOne
	public Parts parentPart;
	
	@ManyToOne
	public History parentHistory;
	
	@Required
	@Formats.NonEmpty
	public String parameter;
	
	@Required
	@Formats.NonEmpty
	public String value;
	
	public String minValue;
	
	public String maxValue;
	
	@ManyToOne
	public MeasurementUnits unit;
	
	//Query creation helper
		public static Model.Finder<Long,Datasheet> find = 
				new Model.Finder<Long,Datasheet>(Long.class,Datasheet.class);
		
		/**
		 * for filling select elements in html pages.
		 * 
		 * @return 
		 */
		public static Map<String, String> options(Integer parentLevel, Long parentId) {
			String fieldName = new String();
			switch(parentLevel) {
			case 3:
				fieldName = "parentInstallation.id";
				break;
			case 4:
				fieldName = "parentPlant.id";
				break;
			case 5:
				fieldName = "parentSection.id";
				break;
			case 6:
				fieldName = "parentEquipment.id";
				break;
			case 7:
				fieldName = "parentSubunit.id";
				break;
			case 8:
				fieldName = "parentComponent.id";
				break;
			case 9:
				fieldName = "parentPart.id";
				break;
			default:
				fieldName = "error";
				break;
					
			}
			LinkedHashMap<String, String> options = new LinkedHashMap<String, String>();
			for (Datasheet dts : Datasheet.find.where().eq(fieldName,parentId).isNull("parentHistory").findList()) {
				options.put(dts.parameter, dts.parameter);
			}
			return options;
		}
	
	/**
	 * Returns a page of datasheet for display
	 * 
	 * @param page
	 *            Page number
	 * @param pageSize
	 *            Size of each page
	 * @param sortBy
	 *            parameter to sort the datasheets by, typically parameter
	 * @param order
	 *            order of sort
	 * @param filter
	 *            Search filter,
	 *            search
	 * @return Page<Datasheet>: A page of Datasheet
	 */
	public static Page<Datasheet> page(String fieldName, Long parentId, int page, int pageSize, String sortBy,
			String order, String filter) {
		return find	.where()
				.eq(fieldName, parentId)
				.isNull("parentHistory")
				.ilike("parameter", "%" + filter + "%")
				.orderBy(sortBy + " " + order)
				.findPagingList(pageSize)
				.setFetchAhead(false).getPage(page);
	}
}