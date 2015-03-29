package models;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import models.equipment.Equipments;
import play.data.format.Formats;
import play.data.validation.Constraints.Required;
import play.db.ebean.Model;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Page;

@Entity
@Table(name = "sections")
public class Sections extends Model {

	private static final long serialVersionUID = 2838477851777966193L;

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	public Long id;

	@Required
	@Formats.NonEmpty
	public String name;
	
	public String description;
		
	@Enumerated(EnumType.STRING)
	public SectionCategories sectionCategory;

	@ManyToMany
	public List<Blobs> files = new ArrayList<Blobs>();
	
	@Column(columnDefinition = "TEXT")
	public String comments;
	
	@OneToMany(mappedBy="section")
	public List<Equipments> equipments = new ArrayList<Equipments>();
	
	@ManyToOne(cascade = CascadeType.ALL)
	public Plants plant;

	// Query creation helper
	public static Model.Finder<Long, Sections> find = new Model.Finder<Long, Sections>(
			Long.class, Sections.class);

	/**
	 * Returns list of all sections for use in html select element
	 * @return Map<id,section name>
	 */
	public static Map<String, String> options(Long parentPlantId) {
		LinkedHashMap<String, String> options = new LinkedHashMap<String, String>();
		for (Sections s : Sections.find.orderBy("name")
				.where()
				.eq("plant.id",parentPlantId)
				.findList()) {
			options.put(s.id.toString(), s.name);
		}
		return options;
	}
	
	/**
	 * Overrides options() to return brother sections of current section, e.g if a plant
	 * has 10 sections and this method gets id of section 1 it fills the select element with
	 * all sections of current plant and not all possible sections
	 * @return Map<id,section name>
	 */
	public static Map<String, String> options(Long parentPlantId, Long sectionId) {
		LinkedHashMap<String, String> options = new LinkedHashMap<String, String>();
		Plants parentPlant = Plants.find.byId(parentPlantId);
		for (Sections s : parentPlant.sections) {
			options.put(s.id.toString(), s.name);
		}
		return options;
	}
	
/**
	 * Returns a page of sections for display
	 * 
	 * @param page
	 *            Page number
	 * @param pageSize
	 *            Size of each page
	 * @param sortBy
	 *            parameter to sort the sections by, typically name
	 * @param order
	 *            order of sort
	 * @param filter
	 *            Search filter, for sections, name and description, and category are used for
	 *            search
	 * @return Page<Sections>: A page of Sections
	 */
	public static Page<Sections> page(Long parentPlantId, int page, int pageSize, String sortBy,
			String order, String filter) {
		String whoql =
				"(plant.id) = :parentPlantId AND (" +
				"lower(name) like :filter OR " +
				"lower(sectionCategory) like :filter)";
		com.avaje.ebean.Query<Sections> query = Ebean.createQuery(Sections.class);
		query.where(whoql).
			setParameter("filter","%"+filter+"%").
			setParameter("parentPlantId",parentPlantId);

		return query.orderBy(sortBy + " " + order)
				.select("name,description,sectionCategory")
				.findPagingList(pageSize)
				.setFetchAhead(false).getPage(page);
	}
	 
}