/*
 * Copyright 2012 Steve Chaloner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package models;

import java.util.LinkedHashMap;
import java.util.Map;

import be.objectify.deadbolt.core.models.Role;
import play.db.ebean.Model;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Steve Chaloner (steve@objectify.be)
 */
@Entity
public class SecurityRole extends Model implements Role {

	private static final long serialVersionUID = 6802583707541496217L;

	@Id
    public Long id;

    public String name;
    
    public String description;

    public static final Model.Finder<Long, SecurityRole> find = new Model.Finder<Long, SecurityRole>(Long.class,
                                                                                         SecurityRole.class);

    public String getName()
    {
        return name;
    }

    public static SecurityRole findByName(String name)
    {
        return find.where()
                   .eq("name",
                       name)
                   .findUnique();
    }
    
    public static Map<String,String> options() {
    	LinkedHashMap<String,String> options = new LinkedHashMap<String,String>();
    	for(SecurityRole sr: find.all()) {
        	options.put(sr.id.toString(), sr.name);
    	}
    return options;
    }
    
}
