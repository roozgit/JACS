package models;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.ValidationException;

import models.maintenance.Maintenances;
import play.data.format.Formats;
import play.data.validation.Constraints.Email;
import play.data.validation.Constraints.Max;
import play.data.validation.Constraints.Min;
import play.data.validation.Constraints.Required;
import play.db.ebean.Model;
import security.PasswordHash;
import be.objectify.deadbolt.core.models.Permission;
import be.objectify.deadbolt.core.models.Role;
import be.objectify.deadbolt.core.models.Subject;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Page;

@Entity
@Table(name = "users")
public class Users extends Model implements Subject {

	private static final long serialVersionUID = 7734937457175614971L;

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	public Long id;

	@Required
	@Formats.NonEmpty
	@Column(unique=true)
	public String userName;
	
	@Formats.NonEmpty
	public String password;
	
	@ManyToOne
	public Disciplines discipline;
	
	public String firstName;
	public String lastName;
	public String personnelNumber;
	
	public String homeCity;
	public String residenceCity;
	public String address;
	public String mobilePhone;
	public String homePhone;
	public String mobilePhone2;
	public String homePhone2;
	
	@Email
	public String email;
	
	public String flightOrigin;
	
	public String camp;
	public String suite;
	public String suitePhoneNumber;
	
	@Min(7)
	@Max(17)
	public Integer organizationalGrade;
	
	public String organizationalPost;
	
	@ManyToOne
	public Companies hiringCompany;
	
	@ManyToOne
	public HiringTypes hiringType;
	
	@Enumerated(EnumType.STRING)
	public SkillLevels skill;
	
	public Boolean isOnShift=false;
	
	@ManyToMany
	public List<UserBlobs> userFiles = new ArrayList<UserBlobs>();
	
	@ManyToMany
    public List<SecurityRole> roles;

    @ManyToMany
    public List<UserPermission> permissions;
	
	@Column(columnDefinition = "TEXT")
	public String comments;

	// Query creation helper
	public static Model.Finder<Long, Users> find = new Model.Finder<Long, Users>(
			Long.class, Users.class);
	
	/**
     * Authenticate a User.
     */
    public static Users authenticate(String userName, String password) {
    	try {
    		Users u = Users.findByUserName(userName);
    		if(u==null) throw new ValidationException("User doesn't exist.");
    		if(u.password==null) throw new ValidationException("Login is disabled.");
			if(PasswordHash.validatePassword(password, u.password)) {
				return u;
			} else return null;
		} catch (NoSuchAlgorithmException | InvalidKeySpecException | ValidationException e) {
			return null;
		}
    }
    
    /**
     * find a User by its username
     * @param userName
     * @return
     */
    public static Users findByUserName(String userName) {
    	return Users.find.where().eq("userName", userName).findUnique();
    }
    
    /**
     * 
     * @return
     */
    public List<Maintenances> referredMaints() {
    	List<Maintenances> retList = new ArrayList<Maintenances>();
    	for(Maintenances m : Maintenances.find.where().isNotNull("workflowStage")
				.orderBy("requestDate")
				.findList()) {
			if(this.roles.contains(m.workflowStage.receivingRole)) {
				if(m.workflowStage.receivingRole.getName().toLowerCase()
						.contains("incharge")) {
					if(m.responsiblePerson!=null)
						if(m.responsiblePerson.id == this.id)
							retList.add(m);
				} else retList.add(m);
			}
		}
    	return retList;
    }
    /**
     * Map of id and userName of all users for use in html select element
     * @return
     */
	public static Map<String, String> options() {
		LinkedHashMap<String, String> options = new LinkedHashMap<String, String>();
		for (Users u : Users.find.orderBy("lastName").where()
				.isNotNull("userName").findList()) {
			if(u.lastName!=null)
			options.put(u.id.toString(), u.lastName+" "+ u.firstName);
		}
		return options;
	}
	
	/**
     * Map of id and userName of all users for use in html select element
     * @return
     */
	public static Map<String, String> options(Long disciplineId) {
		LinkedHashMap<String, String> options = new LinkedHashMap<String, String>();
		for (Users u : Users.find.orderBy("lastName").where()
				.eq("discipline.id", disciplineId).findList()) {
			if(u.lastName!=null)
			options.put(u.id.toString(),u.lastName+" "+ u.firstName);
		}
		return options;
	}
	
	/**
	 * 
	 * @return
	 */
	@Override
	public String getIdentifier() {
		return userName;
	}
	
	/**
	 * 
	 * @return
	 */
	@Override
	public List<? extends Permission> getPermissions() {
		return permissions;
	}
	
	/**
	 * 
	 * @return
	 */
	@Override
	public List<? extends Role> getRoles() {
        return roles;
	}
	
	/**
	 * Returns a page of Users for display
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
	 * @return Page<Companies>: A page of Users
	 */
	public static Page<Users> page(int page, int pageSize, String sortBy,
			String order, String filter) {
		String whoql =
				"lower(userName) like :filter OR " +
				"lower(firstName) like :filter2 OR " +
				"lower(lastName) like :filter2 OR " +
				"lower(personnelNumber) like :filter2 OR "+
				"lower(discipline.name) like :filter2 OR " +
				"lower(hiringCompany.name) like :filter2 OR " +
				"lower(email) like :filter";
		
		com.avaje.ebean.Query<Users> query = Ebean.createQuery(Users.class);
		query.where(whoql).
		setParameter("filter","%"+filter+"%").
		setParameter("filter2", filter+"%");
		
		return query.orderBy(sortBy + " " + order).
				findPagingList(pageSize)
				.setFetchAhead(false).getPage(page);
	}

}