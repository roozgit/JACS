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
package security;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import play.libs.F;
import models.Blobs;
import models.SecurityRole;
import models.Users;
import models.maintenance.Maintenances;
import play.Logger;
import play.mvc.Http;
import be.objectify.deadbolt.core.DeadboltAnalyzer;
import be.objectify.deadbolt.core.models.Permission;
import be.objectify.deadbolt.core.models.Role;
import be.objectify.deadbolt.core.models.Subject;
import be.objectify.deadbolt.java.DeadboltHandler;
import be.objectify.deadbolt.java.DynamicResourceHandler;

import java.util.concurrent.TimeUnit;

/**
 * @author Steve Chaloner (steve@objectify.be)
 */
public class MyDynamicResourceHandler implements DynamicResourceHandler
{
    private static final Map<String, DynamicResourceHandler> HANDLERS = new HashMap<String, DynamicResourceHandler>();

    static
    {
	    HANDLERS.put("deleteFile",
	                 new AbstractDynamicResourceHandler()
	                 {
	                     public boolean isAllowed(final String name,
	                                              final String meta,
	                                              final DeadboltHandler deadboltHandler,
	                                              final Http.Context context)
	                     {
	                         return deadboltHandler.getSubject(context)
					.map(new F.Function<Subject, Boolean>() {
                                       @Override
                                       public Boolean apply(Subject subject) throws Throwable {
	                         boolean allowed;
	                         if (DeadboltAnalyzer.hasRole(subject, "admin"))
	                         {
	                             allowed = true;
	                         }
	                         else
	                         {
	                        	 
	                        	try {
									// a call to delete a file is currently a get request, so
	                             // the query string is used to provide info
	                             Map<String, String[]> queryStrings = context.request().queryString();
	                             String[] requestedNames = queryStrings.get("fileId");
	                             allowed = requestedNames != null
	                                       && requestedNames.length == 1
	                                       && Blobs.isBlobOwner(Long.valueOf(requestedNames[0]), subject.getIdentifier());

								} catch (Exception e) {
									Logger.info("Deadbolt error",e.fillInStackTrace());
									allowed=false;
								}
	                        }
	
	                         return allowed;
	                     }
	                 }).get(1, TimeUnit.SECONDS);
		}
	});
	    
	    HANDLERS.put("editProfile",
                new AbstractDynamicResourceHandler()
                {
                    public boolean isAllowed(final String name,
                                             final String meta,
                                             final DeadboltHandler deadboltHandler,
                                             final Http.Context context)
                    {
                        return deadboltHandler.getSubject(context)
			.map(new F.Function<Subject, Boolean>() {
                                       @Override
                                       public Boolean apply(Subject subject) throws Throwable {
                        boolean allowed;
                        if (DeadboltAnalyzer.hasRole(subject, "admin"))
                        {
                            allowed = true;
                        }
                        else
                        {
                        	try {
								// a call to edit profile is currently a get request, so
                            // the query string is used to provide info
                            Map<String, String[]> queryStrings = context.request().queryString();
                            String[] requestedNames = queryStrings.get("uid");
                            allowed = requestedNames != null
                                      && requestedNames.length == 1
                                      && Users.find.byId(Long.valueOf(requestedNames[0])).userName
                                      .equals(subject.getIdentifier());

							} catch (Exception e) {
								Logger.info("Deadbolt error",e.fillInStackTrace());
								allowed=false;
							}
                        }

                        return allowed;
                    }
	                 }).get(1, TimeUnit.SECONDS);
		}
                });
	    
	    HANDLERS.put("logger",
                new AbstractDynamicResourceHandler()
                {
			    	 public boolean isAllowed(final String name,
		                     final String meta,
		                     final DeadboltHandler deadboltHandler,
		                     final Http.Context context)
						{
						return deadboltHandler.getSubject(context)
						.map(new F.Function<Subject, Boolean>() {
                                       @Override
                                       public Boolean apply(Subject subject) throws Throwable {
						boolean allowed = false;
						if (DeadboltAnalyzer.hasRole(subject, "admin") || DeadboltAnalyzer.hasRole(subject, "planner"))
						{
						    allowed = true;
						}
						else
						{
							try {
								// a call to edit maintenance is currently a get request, so
								// the query string is used to provide info
								Map<String, String[]> queryStrings = context.request().queryString();
								String[] requestedNames = queryStrings.get("pxid");
								
								if(requestedNames.length==1) {
									for(Role sr : subject.getRoles()) {
										if(sr.getName().toLowerCase().contains("verifier") ||
												sr.getName().toLowerCase().contains("incharge") ||
												sr.getName().toLowerCase().contains("requester"))
											allowed = requestedNames.length == 1;
									}
								}
							} catch (Exception e) {
								Logger.info("Deadbolt error",e.fillInStackTrace());
								allowed=false;
							}
						}
						return allowed;
					}
	                 }).get(1, TimeUnit.SECONDS);
		}
                
                });
	    
	    HANDLERS.put("maintenancePrivilege",
                new AbstractDynamicResourceHandler()
                {
			    	 public boolean isAllowed(final String name,
		                     final String meta,
		                     final DeadboltHandler deadboltHandler,
		                     final Http.Context context)
						{
						return deadboltHandler.getSubject(context)
					.map(new F.Function<Subject, Boolean>() {
                                       @Override
                                       public Boolean apply(Subject subject) throws Throwable {
						boolean allowed;
						if (DeadboltAnalyzer.hasRole(subject, "admin") || DeadboltAnalyzer.hasRole(subject, "planner"))
						{
						    allowed = true;
						}
						else
						{
							try {
								// a call to edit maintenance is currently a get request, so
								// the query string is used to provide info
								Map<String, String[]> queryStrings = context.request().queryString();
								String[] requestedNames = queryStrings.get("maintID");
								
								if(Maintenances.find.byId(Long.valueOf(requestedNames[0])).workflowStage==null) {
									allowed = false;
								} else {
									if(Maintenances.find.byId(Long.valueOf(requestedNames[0]))
											.workflowStage.receivingRole == null) {
										allowed = false;
									} else {
										allowed = requestedNames.length == 1
												&& subject.getRoles().contains(Maintenances.find.byId(Long.valueOf(requestedNames[0]))
														.workflowStage.receivingRole);
										
									}
									
								}
								
								
								
							} catch (Exception e) {
								Logger.info("Deadbolt error",e.fillInStackTrace());
								allowed=false;
							}
						}
						return allowed;
					}
	                 }).get(1, TimeUnit.SECONDS);
		}
                
                });
	    
	    HANDLERS.put("maintenanceAuthorizers",
                new AbstractDynamicResourceHandler()
                {
			    	 public boolean isAllowed(final String name,
		                     final String meta,
		                    final DeadboltHandler deadboltHandler,
		                     final Http.Context context)
						{
						return deadboltHandler.getSubject(context)
						.map(new F.Function<Subject, Boolean>() {
                                       @Override
                                       public Boolean apply(Subject subject) throws Throwable {
						boolean allowed = false;
						if (DeadboltAnalyzer.hasRole(subject, "admin") || DeadboltAnalyzer.hasRole(subject, "planner"))
						{
						    allowed = true;
						}
						else
						{
							try {
								Map<String, String[]> queryStrings = context
										.request().queryString();
								String[] requestedNames = queryStrings
										.get("maintID");
								Users uuser = Users
										.findByUserName(play.mvc.Controller
												.session().get("userName"));
								List<SecurityRole> sr = uuser.roles;
								for (SecurityRole s : sr) {
									if (s.name.toLowerCase().contains(
											"authorizer")
											&& (!s.name.toLowerCase().contains(
													"maintenance"))
											&& (Maintenances.find
													.byId(Long
															.valueOf(requestedNames[0])).responsibleDiscipline.id == uuser.discipline.id)
											&& (SecurityRole.find
													.byId(Maintenances.find.byId(Long
															.valueOf(requestedNames[0])).workflowStage.receivingRole.id).name
													.toLowerCase()
													.contains("authorizer"))
											&& (requestedNames.length == 1)) {
										allowed = true;
										break;
									}

								}
							} catch (Exception e) {
								Logger.info("Deadbolt error",e.fillInStackTrace());
								allowed=false;
							}
						}
						return allowed;
					}
	                 }).get(1, TimeUnit.SECONDS);
		}
                
                });
	    
	    HANDLERS.put("maintenanceInCharge",
                new AbstractDynamicResourceHandler()
                {
			    	 public boolean isAllowed(final String name,
		                     final String meta,
		                     final DeadboltHandler deadboltHandler,
		                     final Http.Context context)
						{
						return deadboltHandler.getSubject(context)
						.map(new F.Function<Subject, Boolean>() {
                                       @Override
                                       public Boolean apply(Subject subject) throws Throwable {
						boolean allowed;
						if (DeadboltAnalyzer.hasRole(subject, "admin") || DeadboltAnalyzer.hasRole(subject, "planner"))
						{
						    allowed = true;
						}
						else
						{
						    try {
								Map<String, String[]> queryStrings = context.request().queryString();
								String[] requestedNames = queryStrings.get("maintID");
								
								if(Maintenances.find.byId(Long.valueOf(requestedNames[0]))
										.workflowStage.receivingRole == null ||
										Maintenances.find.byId(Long.valueOf(requestedNames[0])).responsiblePerson == null) {
									allowed = false;
								} else {
									allowed = requestedNames.length == 1
											&& subject.getRoles().contains(Maintenances.find.byId(Long.valueOf(requestedNames[0]))
													.workflowStage.receivingRole) &&
													(Maintenances.find.byId(Long.valueOf(requestedNames[0])).responsiblePerson.id==
													Users.findByUserName(play.mvc.Controller.session().get("userName")).id);
								}

							} catch (Exception e) {
								Logger.info("Deadbolt error",e.fillInStackTrace());
								allowed=false;
							}
						    						
						}
						return allowed;
					}
	                 }).get(1, TimeUnit.SECONDS);
		}
                
                });
    }
    
    public boolean isAllowed(String name,
                             String meta,
                             DeadboltHandler deadboltHandler,
                             Http.Context context)
    {
        DynamicResourceHandler handler = HANDLERS.get(name);
        boolean result = false;
        if (handler == null)
        {
            Logger.error("No handler available for " + name);
        }
        else
        {
            result = handler.isAllowed(name,
                                       meta,
                                       deadboltHandler,
                                       context);
        }
        return result;
    }

public boolean checkPermission(final String permissionValue,
                                   final DeadboltHandler deadboltHandler,
                                   final Http.Context ctx)
    {
        return deadboltHandler.getSubject(ctx)
                              .map(new F.Function<Subject, Boolean>()
                              {
                                  @Override
                                  public Boolean apply(Subject subject) throws Throwable
                                  {
                                      boolean permissionOk = false;

                                      if (subject != null)
                                      {
                                          List<? extends Permission> permissions = subject.getPermissions();
                                          for (Iterator<? extends Permission> iterator = permissions.iterator(); !permissionOk && iterator.hasNext(); )
                                          {
                                              Permission permission = iterator.next();
                                              permissionOk = permission.getValue().contains(permissionValue);
                                          }
                                      }

                                      return permissionOk;
                                  }
                              }).get(1, TimeUnit.SECONDS);
    }
   
}
