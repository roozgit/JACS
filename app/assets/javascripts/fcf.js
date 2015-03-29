$(document).ready(function () {
   
	//Corresponding failure filler
	$(window).blur(function() {
    	$(window).focus(function() {
    			ffiller();
    	});
    });

	
    //Work order number calculator
    $("select#maintenanceCategory").change(function() {
    	jQuery.ajax({
    		type:	'GET',
    		url: jsRoutes.controllers.MaintenanceApplication.calculateSerials($(this).val()).url,
    		success: function(data) {
    			//alert(JSON.stringify(data));
    				$("input#workRequestSerial").val(data.workRequestSerial);
    				$("input#workOrderSerial").val(data.workOrderSerial);
    		}
    	});
    	
    });
    
    //Section equipment selector
    $("select#maintainedSection_id").change(function() {
    	var thisParam = $(this).val();
    	$("#failureCreateLink").removeClass("disabled");
    	$("#failureCreateLink").attr("href", "/failure/new?level=5&pid="+thisParam);
    	jQuery.ajax({
            type:   'GET',
            url: jsRoutes.controllers.MaintenanceApplication.sectionEquipmentFiller($(this).val()).url,
            success: function(data) {
            	var selectOptions = '';
            	selectOptions += '<option value="" selected="selected">Select equipment</option>';
            	for(var i=0;i<data.length;i++) {
            		selectOptions += '<option value="' + data[i].id + '">' + data[i].value + '</option>';
            	}
            	$("select#maintainedEquipment_id").html(selectOptions);
            	$("select#drivenEquipment_id").html(selectOptions);
            }
    	});
    	
    	
    	//Section PM routine selector
    	jQuery.ajax({
            type:   'GET',
            url: jsRoutes.controllers.MaintenanceApplication.sectionRoutineFiller($(this).val()).url,
            success: function(data) {
            	var selectOptions = '';
            	selectOptions += '<option value="">' + '</option>';
            	for(var i=0;i<data.length;i++) {
            		selectOptions += '<option value="' + data[i].id + '">' + data[i].value + '</option>';
            	}
            	$("select#pmRoutine_id").html(selectOptions);
            }
    	});
    	ffiller();
    	
    });
    
    //Another equipment selector
    $("select#maintainedSection2_id").change(function() {
    	jQuery.ajax({
            type:   'GET',
            url: jsRoutes.controllers.MaintenanceApplication.sectionEquipmentFiller($(this).val()).url,
            success: function(data) {
            	var selectOptions = '';
            	selectOptions += '<option value="">' + '</option>';
            	for(var i=0;i<data.length;i++) {
            		selectOptions += '<option value="' + data[i].id + '">' + data[i].value + '</option>';
            	}
            	$("select#driverEquipment_id").html(selectOptions);
            }
    	});
    });
    
    
    
    //Equipment PM routine selector
    $("select#maintainedEquipment_id").change(function() {
    	var thisParam = $(this).val();
    	$("#failureCreateLink").removeClass("disabled");
    	if(thisParam.length==0) $("#failureCreateLink").addClass("disabled");
    	$("#failureCreateLink").attr("href", "/failure/new?level=6&pid="+thisParam);
    	//$("a#failureCreateLink").attr("href", "@routes.FailureApplication.createFailure(6,"+thisParam+")"));
    	//Equipment routine filler
    	jQuery.ajax({
            type:   'GET',
            url: jsRoutes.controllers.MaintenanceApplication.equipmentRoutineFiller(thisParam).url,
            success: function(data) {
            	var selectOptions = '';
            	selectOptions += '<option value="">' + '</option>';
            	for(var i=0;i<data.length;i++) {
            		selectOptions += '<option value="' + data[i].id + '">' + data[i].value + '</option>';
            	}
            	$("select#pmRoutine_id").html(selectOptions);
            	jQuery.ajax({
                    type:   'GET',
                    url: jsRoutes.controllers.MaintenanceApplication.equipmentClassRoutineFiller(thisParam).url,
                    success: function(data) {
                    	var selectOptions2 = '';
                    	selectOptions2 += '<option value="">' + '</option>';
                    	for(var i=0;i<data.length;i++) {
                    		selectOptions2 += '<option value="' + data[i].id + '">' + data[i].value + '</option>';
                    	}
                    	$("select#classLevelPMRoutine_id").html(selectOptions2);
                    }
            	});
            }
    	});
    	
    	ffiller();
    	
    });
    
    //Discipline Personnel selector
    $("select#disciplineSelector").change(function() {
    	jQuery.ajax({
            type:   'GET',
            url: jsRoutes.controllers.UserApplication.disciplineUserFiller($(this).val()).url,
            success: function(data) {
            	var selectOptions = '';
            	selectOptions += '<option value="">' + '</option>';
            	for(var i=0;i<data.length;i++) {
            		selectOptions += '<option value="' + data[i].id + '">' + data[i].value + '</option>';
            	}
            	$("select#responsiblePerson_id").html(selectOptions);
            	$("select#user_id").html(selectOptions);
            }
    	});
    });

});

//failure filler function
function ffiller() {
	seid=-1;
	eid = $("select#maintainedEquipment_id").val();
	if(eid<0 || eid.length==0 || typeof(eid)==='undefined') {
		seid=$("select#maintainedSection_id").val();
		eid=-1;
		}
	//Equipment failures filler
	jQuery.ajax({
        type:   'GET',
        url: jsRoutes.controllers.FailureApplication.failureFiller(seid,eid).url,
        success: function(data) {
        	var selectOptions = '';
        	selectOptions += '<option value="">' + '</option>';
        	for(var i=0;i<data.length;i++) {
        		selectOptions += '<option value="' + data[i].id + '">' + data[i].value + '</option>';
        	}
        	if(selectOptions == '<option value="">' + '</option>')
        		$("div#correspondingFailure_field").hide();
        	else {
        		$("div#correspondingFailure_field").show();
        		$("select#correspondingFailure").append(selectOptions);
        	}
        		
        }
	});
}

//Parameter extraction from URL functions
function getParameterByName(name) {
    name = name.replace(/[\[]/, "\\[").replace(/[\]]/, "\\]");
    var regex = new RegExp("[\\?&]" + name + "=([^&#]*)"),
        results = regex.exec(location.search);
    return results == null ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
}