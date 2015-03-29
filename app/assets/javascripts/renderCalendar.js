$(document).ready(function () {
	
    var parentLevel = parseInt(getParameterByName("level"));
    var parentId = parseInt(getParameterByName("pxid"));
    var calType = getParameterByName("calType");
    var occurred = getParameterByName("occur");
    //occurred = $("#occurSelect").val();
    var userId = parseInt(getParameterByName("userId"));
	var date = new Date();
    var d = date.getDate();
    var m = date.getMonth();
    var y = date.getFullYear();
    var pmParentId = parseInt(getParameterByName("pmParentId"));
    var pmRoutineId = parseInt(getParameterByName("pmRoutineId"));
    var level = getParameterByName("level");
    
    jQuery("a#occurLink").click(function() {
    	occurred = $("#occurSelect").val();
    	window.open("/log/calendar?level="+parentLevel+"&pxid="+parentId+"&calType="+calType+"&occur="+occurred,"_self");
    });
    jQuery("#pmPlanForm").submit(function(event) {
    	event.preventDefault();
    	if (!jQuery('#pmPlanCalendar').is(':empty')) jQuery("#pmPlanCalendar").empty();
    	jQuery("#savePlanGroup").show();
    	var startDate = jQuery("#start").val();
    	var endDate = jQuery("#end").val();
    	var numOfPmsPerDay = jQuery("#pmPerDay").val();
    	var maintLength = jQuery("#routineTTR").val();
    	//****************************
    	//Full calendar for PM planning
    	var shCalendar = $('#pmPlanCalendar').fullCalendar({
    	    timeFormat:'H:mm{ - H:mm}',
    	    header:{
    	        left:'prev,next today',
    	        center:'title',
    	        right:'month,agendaWeek,agendaDay'
    	    },
    	    selectable:true,
    	    selectHelper:true,
    	  
    	    windowResize: function(view) {
    	        setNewHeight();
    	    },
    	    editable:true,
    	    events: {
    	        url: jsRoutes.controllers.PreventiveMaintenanceApplication.calculatePmPlanJson(level, pmParentId,pmRoutineId,startDate,endDate,numOfPmsPerDay,maintLength).url,
    	        cache : true
    	    }
    	});
    	setNewHeight();
    });
    
    //Saving planned PMs to db
    jQuery("#savePlanGroup").submit(function(event) {
    	event.preventDefault();
    	var isp = false;
    	var hiddenStart = $("#start").val();
    	var hiddenStop = $("#end").val();
    	if($("#isProject").is(':checked')) isp = true;
    	if (jQuery('#pmPlanCalendar').is(':empty')) {
    		console.log("it's empty");
    		alert("No plan selected yet");
    	} else {
    		jQuery.ajax({
                type:   'POST',
                contentType: "application/json",
                url:    jsRoutes.controllers.PreventiveMaintenanceApplication.savePlannedMaints(pmRoutineId,jQuery("#periods").val(), isp,hiddenStart,hiddenStop).url,
                data:   JSON.stringify(jQuery("#pmPlanCalendar").fullCalendar('clientEvents')),
                dataType: 'json'
            }).done(function( data ) {
            	alert(data.message);
            	if(data.message=="Planned!") $('#submitJsonCal').prop('disabled', true);
            });
    		
    	}
    });
    
    //full calendar
    var calendar = $('#calendar').fullCalendar({
        timeFormat:'H:mm{ - H:mm}',
        header:{
            left:'prev,next today',
            center:'title',
            right:'month,agendaWeek,agendaDay'
        },
        selectable:true,
        selectHelper:true,
        
        eventDrop:function(event,dayDelta,minuteDelta,allDay,revertFunc){

            if (typeof event.id == "undefined"){
                alert("This event can not be changed!");
                revertFunc();
                return false;
            }

            jQuery("#moveId").val(event.id);
            jQuery("#moveDayDelta").val(dayDelta);
            jQuery("#moveMinuteDelta").val(minuteDelta);
            jQuery("#moveAllDay").val(allDay);


            jQuery.ajax({
                type:   'POST',
                url:    jQuery("#eventFormMove").attr("action"),
                data:   jQuery("#eventFormMove").serialize(),
                statusCode:{
                    400: function(data) {
                        revertFunc();
                        alert(data.responseText);
                    }
                }
            });

        },

        eventResize: function(event,dayDelta,minuteDelta,revertFunc) {
            if (typeof event.id == "undefined"){
                alert("This event can not be changed!");
                revertFunc();
                return false;
            }

            jQuery("#resizeId").val(event.id);
            jQuery("#resizeDayDelta").val(dayDelta);
            jQuery("#resizeMinuteDelta").val(minuteDelta);

            jQuery.ajax({
                type:   'POST',
                url:    jQuery("#eventFormResize").attr("action"),
                data:   jQuery("#eventFormResize").serialize(),
                statusCode:{
                    400: function(data) {
                        revertFunc();
                        alert(data.responseText);
                    }
                }
            });
        },
        windowResize: function(view) {
            setNewHeight();
        },
        editable:true,

        events: {
            url: jsRoutes.controllers.HistoryApplication.json(parentLevel,parentId,calType,occurred).url,
            cache: true
        }
    });
    
  //full calendar for shifts
    var shCalendar = $('#shiftCalendar').fullCalendar({
        timeFormat:'H:mm{ - H:mm}',
        header:{
            left:'prev,next today',
            center:'title',
            right:'month,agendaWeek,agendaDay'
        },
        selectable:true,
        selectHelper:true,
        
        eventDrop:function(event,dayDelta,minuteDelta,allDay,revertFunc){

            if (typeof event.id == "undefined"){
                alert("This event can not be changed!");
                revertFunc();
                return false;
            }

            jQuery("#moveId").val(event.id);
            jQuery("#moveDayDelta").val(dayDelta);
            jQuery("#moveMinuteDelta").val(minuteDelta);
            jQuery("#moveAllDay").val(allDay);


            jQuery.ajax({
                type:   'POST',
                url:    jQuery("#eventFormMove").attr("action"),
                data:   jQuery("#eventFormMove").serialize(),
                statusCode:{
                    400: function(data) {
                        revertFunc();
                        alert(data.responseText);
                    }
                }
            });

        },

        eventResize: function(event,dayDelta,minuteDelta,revertFunc) {
            if (typeof event.id == "undefined"){
                alert("This event can not be changed!");
                revertFunc();
                return false;
            }

            jQuery("#resizeId").val(event.id);
            jQuery("#resizeDayDelta").val(dayDelta);
            jQuery("#resizeMinuteDelta").val(minuteDelta);
//            jQuery.post(jQuery("#eventFormResize").attr("action"), jQuery("#eventFormResize").serialize());

            jQuery.ajax({
                type:   'POST',
                url:    jQuery("#eventFormResize").attr("action"),
                data:   jQuery("#eventFormResize").serialize(),
                statusCode:{
                    400: function(data) {
                        revertFunc();
                        alert(data.responseText);
                    }
                }
            });
        },
        windowResize: function(view) {
            setNewHeight();
        },
        editable:true,

        events: {
            url: jsRoutes.controllers.ShiftApplication.json(userId).url,
            cache: true
        }
    });
    
    setNewHeight();
   
  



//setNewHeight();

});


//Useful functions
function convertDate(date){
    return(date.getDate()+"."+(date.getUTCMonth()+1)+"."+date.getUTCFullYear()+" "+date.getHours()+":"+date.getMinutes());
}

function setNewHeight() {
    newHeight = jQuery(window).height() - 120; // 60 is padding of the body tag in main.css (required for Bootstrap's header)
    $('#calendar').fullCalendar('option', 'height', newHeight);
    $('#shiftCalendar').fullCalendar('option', 'height', newHeight);
    $('#pmPlanCalendar').fullCalendar('option', 'height', newHeight);
    
}

function getParameterByName(name) {
    name = name.replace(/[\[]/, "\\[").replace(/[\]]/, "\\]");
    var regex = new RegExp("[\\?&]" + name + "=([^&#]*)"),
        results = regex.exec(location.search);
    return results == null ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
}

function getParameterByNameFromString(name, baseString) {
    name = name.replace(/[\[]/, "\\[").replace(/[\]]/, "\\]");
    var regex = new RegExp("[\\?&]" + name + "=([^&#]*)"),
        results = regex.exec(baseString);
    return results == null ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
}