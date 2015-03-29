/**
 * 
 */
   var parentLevel = parseInt(getParameterByName("level"));
   var parentId = parseInt(getParameterByName("pxid"));
   var calType = getParameterByName("calType")
    
$(document).ready(function() {
	//context menu
    $.contextMenu({
    	selector : '#calendar a.fc-event',
    	callback: function(key , options) {
    		var hidd = parseInt(getParameterByNameFromString("hxid",this.context.href));
    		if(key=="delete") {
    			jQuery.ajax({
                    type:   'GET',
                    url: jsRoutes.controllers.HistoryApplication.deleteH(hidd,parentLevel,parentId,calType).url,
                    success: function() {
                    	window.location.replace(
                    			'/log/calendar?level='
                    			+parentLevel+'&pxid='+parentId + '&calType='+calType);
                    }
                });
    		} 		
    	},
    	items :{
    		"delete": {name:"Delete"}
    	}
    });
});

function getParameterByName(name) {
    name = name.replace(/[\[]/, "\\[").replace(/[\]]/, "\\]");
    var regex = new RegExp("[\\?&]" + name + "=([^&#]*)"),
        results = regex.exec(location.search);
    return results == null ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
}