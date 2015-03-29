package models;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
//import play.data.validation.*;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import play.data.format.Formats;
import play.data.validation.Constraints.Required;
import play.db.ebean.Model;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Page;

@Entity
@SequenceGenerator(name = "disciplines_seq")
@Table(name = "disciplines")
public class Disciplines extends Model {

	private static final long serialVersionUID = 2544498622680357996L;

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	public Long id;

	@Required
	@Formats.NonEmpty
	public String name;
	
	public String telephone;

	@Column(columnDefinition = "TEXT")
	public String comments;
	
	@ManyToOne
	public Plants organizationPlant;
	
	@ManyToOne
	public Disciplines headDiscipline;
	
	@OneToMany(mappedBy="discipline")
	public List<Users> disciplinePersonnel = new ArrayList<Users>();

	// Query creation helper
	public static Model.Finder<Long, Disciplines> find = new Model.Finder<Long, Disciplines>(
			Long.class, Disciplines.class);
	
	/**
     * Check if a user is a member of this discipline
     */
    public static Boolean isMember(Long discipline, String userName) {
        return find.where()
            .eq("disciplinePersonnel.userName", userName)
            .eq("id", discipline)
            .findRowCount() > 0;
    }
	

	public static Map<String, String> options() {
		LinkedHashMap<String, String> options = new LinkedHashMap<String, String>();
		for (Disciplines d : Disciplines.find.where().isNotNull("id").orderBy("name").findList()) {
			options.put(d.id.toString(), d.name);
		}
		return options;
	}
	
	/**
	 * Returns a page of Disciplines for display
	 * 
	 * @param page
	 *            Page number
	 * @param pageSize
	 *            Size of each page
	 * @param sortBy
	 *            parameter to sort the Users by, typically userName
	 * @param order
	 *            order of sort
	 * @param filter
	 *            Search filter
	 *            search
	 * @return Page<Companies>: A page of Disciplines
	 */
	public static Page<Disciplines> page(int page, int pageSize, String sortBy,
			String order, String filter) {
		String whoql =
				"lower(name) like :filter OR " +
				"lower(organizationPlant.name) like :filter";
		
		com.avaje.ebean.Query<Disciplines> query = Ebean.createQuery(Disciplines.class);
		query.where(whoql).
		setParameter("filter","%"+filter+"%");
		return query.orderBy(sortBy + " " + order).
				findPagingList(pageSize)
				.setFetchAhead(false).getPage(page);
	}

}
