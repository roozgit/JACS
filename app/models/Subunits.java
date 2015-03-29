package models;

import javax.persistence.*;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Page;

import play.data.format.Formats;
import play.data.validation.Constraints.*;
import play.db.ebean.*;
import java.util.*;

import models.equipment.*;

@Entity
@Table(name = "subunits")
public class Subunits extends Model {

	private static final long serialVersionUID = -6324555877352154471L;

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	public Long id;

	@ManyToOne
	public Equipments equipment;
	
	@Required
	@Formats.NonEmpty
	public String name;
	
	public String description;
	
	@Enumerated(EnumType.STRING)
	public SubunitTypes subunitType;

	@Column(columnDefinition = "TEXT")
	public String comments;

	@ManyToMany
	public List<Blobs> files = new ArrayList<Blobs>();	
	
	@OneToMany(mappedBy="subunit")
	public List<Components> components = new ArrayList<Components>();	

	// Query creation helper
	public static Model.Finder<Long, Subunits> find = new Model.Finder<Long, Subunits>(
			Long.class, Subunits.class);

	/**
	 * Returns all subunits for use in html select element
	 * @param parentEquipmentId Id of parent equipment
	 * @return
	 */
	public static Map<String, String> options() {
		LinkedHashMap<String, String> options = new LinkedHashMap<String, String>();
		for (Subunits su : Subunits.find.where()
					.isNotNull("id")
					.orderBy("name")
					.findList()) {
			options.put(su.id.toString(), su.name +"------EQUIPMENT: "
					+ su.equipment.name);
		}
		return options;
	}
	
	/**
	 * overrides option to return subunits belonging to one equipment
	 * @param parentEquipmentId Id of parent equipment
	 * @return
	 */
	public static Map<String, String> options(Long parentEquipmentId) {
		LinkedHashMap<String, String> options = new LinkedHashMap<String, String>();
		for (Subunits su : Subunits.find.where()
				.eq("equipment.id", parentEquipmentId)				
				.orderBy("name").
				findList()) {
			options.put(su.id.toString(), su.name+" in "+su.equipment.name);
		}
		return options;
	}

	/**
	 * Returns brother subunits of current subunit, e.g if a equipment
	 * has 10 subunits and this method gets id of subunit 1 it fills the select element with 
	 * all of current equipment's subunits and not all possible subunits
	 * @return Map<id,section name>
	 */
	public static Map<String, String> brotherSubunitsOptions(Long subunitId) {
		LinkedHashMap<String, String> options = new LinkedHashMap<String, String>();
		Equipments parentEquipment = Subunits.find.byId(subunitId).equipment; 
		for (Subunits s : parentEquipment.subunits) {
			options.put(s.id.toString(), s.name);
		}
		return options;
	}

	/**
	 * Returns a page of subunits for display
	 * 
	 * @param page
	 *            Page number
	 * @param pageSize
	 *            Size of each page
	 * @param sortBy
	 *            parameter to sort the equipments by, typically name
	 * @param order
	 *            order of sort
	 * @param filter
	 *            Search filter, for equipments, name and description
	 *            search
	 * @return Page<Sections>: A page of Subunits
	 */
	public static Page<Subunits> page(Long parentEquipmentId, int page, int pageSize, String sortBy,
			String order, String filter) {
		String whoql = 
				"(equipment.id) = :parentEquipmentId AND (" +
				"lower(name) like :filter OR " +
				"lower(description) like :filter OR " +
				"lower(subunitType) like :filter)";
		
		com.avaje.ebean.Query<Subunits> query = Ebean.createQuery(Subunits.class);
		query.where(whoql).
		setParameter("filter","%"+filter+"%").
		setParameter("parentEquipmentId",parentEquipmentId);
		
		return query.orderBy(sortBy + " " + order).
				findPagingList(pageSize)
				.setFetchAhead(false).getPage(page);
	}

}