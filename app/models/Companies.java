package models;

import java.util.*;
import javax.persistence.*;

import com.avaje.ebean.Expr;
import com.avaje.ebean.Page;

import play.db.ebean.*;
import play.data.validation.Constraints.*;
import play.data.format.*;

@Entity
@Table(name = "companies")
public class Companies extends Model {

	private static final long serialVersionUID = -8634977374802394396L;

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	public Long id;

	@Required
	@Formats.NonEmpty
	public String name;
	
	public String description;
	
	@Enumerated(EnumType.STRING)
	public CompanyTypes companyType;
	
	public String country;
	
	public String telephone1;
	public String telephone2;
	public String telephone3;
	
	public String fax1;
	public String fax2;
	public String fax3;

	public String website;
	
	public String contactPerson;
	
	@Email
	public String email;

	@Column(columnDefinition = "TEXT")
	public String address1;
	
	@Column(columnDefinition = "TEXT")
	public String address2;
	
	@Column(columnDefinition = "TEXT")
	public String address3;

	@ManyToMany
	@JoinTable(name = "companies_companies", joinColumns = @JoinColumn(name = "head_companies_id"), inverseJoinColumns = @JoinColumn(name = "rep_companies_id"))
	public List<Companies> headCompanies = new ArrayList<Companies>();;

	@Column(columnDefinition = "TEXT")
	public String comments;

	@OneToOne
	public Blobs companyLogo;
	
	// Query creation helper
	public static Model.Finder<Long, Companies> find = new Model.Finder<Long, Companies>(
			Long.class, Companies.class);

	/**
	 * for filling select elements in html pages.
	 * 
	 * @return Map<company.id, company.name> for filling html select element
	 */
	public static Map<String, String> options() {
		LinkedHashMap<String, String> options = new LinkedHashMap<String, String>();
		for (Companies comp : Companies.find.all()) {
			options.put(comp.id.toString(), comp.name);
		}
		return options;
	}


	/**
	 * Returns a page of companies for display
	 * 
	 * @param page
	 *            Page number
	 * @param pageSize
	 *            Size of each page
	 * @param sortBy
	 *            parameter to sort the companies by, typically name
	 * @param order
	 *            order of sort
	 * @param filter
	 *            Search filter, for companies, both email and name are used for
	 *            search
	 * @return Page<Companies>: A page of Companies
	 */
	public static Page<Companies> page(int page, int pageSize, String sortBy,
			String order, String filter) {
		return find	.where().or(
					Expr.ilike("name", "%" + filter + "%"),
					Expr.ilike("email", "%" + filter + "%"))
				.orderBy(sortBy + " " + order)
				.findPagingList(pageSize)
				.setFetchAhead(false).getPage(page);
	}

}
