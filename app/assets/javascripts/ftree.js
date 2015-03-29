$(document).ready(function () {
    var sourceKey;
    var sourceId;
    var destKey;
    var destId;
    failParentId=0;
    failParentLevel=0;
    if($("#failTree").length > 0) {
    	failParentLevel = $("#failTree").attr("data-value").split("_")[0];
	    failParentId = $("#failTree").attr("data-value").split("_")[1];
    }
    //fancy tree
    $("#tree").fancytree({
    	autoCollapse: false,
    	debugLevel: 0,
    	extensions: ["persist", "contextMenu","dnd" ],
    	source: {
			url: '/tree',
			cache: false
			},
			dnd: {
				preventVoidMoves: true,
				preventRecursiveMoves: true,
				autoExpandMS: 400,
				dragStart: function(node, data) {
					sourceKey = node.key.split("_")[0];
					sourceId = node.key.split("_")[1];
					if(sourceKey==6 || sourceKey==8)
						return true;
					else return false;
			        },
		        dragEnter: function(node, data) {
		            
		            var dropNode1 = node.key.split("_")[0];
	            	if(dropNode1==5 || dropNode1==7)
	            		return ['over'];
	            	else return false;
		      
		          },
	          dragDrop: function(node, data) {
	              /** This function MUST be defined to enable dropping of items on
	               *  the tree.
	               */
	        	  destKey = node.key.split("_")[0];
	        	  destId = node.key.split("_")[1];
	        	  jQuery.ajax({
	        		  type:   'GET',
	        		  url: jsRoutes.controllers.Application.moveOrCopy(sourceKey,sourceId, destKey,destId).url,
	        		  success: function(data2) {
	                    	if(data2=="true") {
	                    		data.otherNode.moveTo(node, data.hitMode);
	                    		alert("Operation completed successfully");
	                    		window.location.reload();
	                    	}
	                    	else
	                    		alert("Error! Please consult server error log.");
	                    }
	        	  });
	              
	            }
			},
		persist: {
				expandLazy: true,
				 store: "cookie"
			},
			contextMenu: {
	            menu: {
					'registerFailure': {'name' : 'Register failure'}				  
	            },
			 actions: function(node, action, options) {
				 	var plev = parseInt(node.key.split('_')[0]);
					var peid = parseInt(node.key.split('_')[1]);
					window.location.replace('/failure/new?level='+plev+'&pid='+peid);
			 	}
			},
		postinit: function(isReloading, isError) {
			this.reactivate();
			},
		focus: function(event, data) {
	        // Auto-activate focused node after 2 seconds
	        data.node.scheduleAction("activate", 2000);
			},
		click: function(event, data) {
	        var node = data.node;
	        // Use <a> href and target attributes to load the content:
	        if( node.data.href ){
	          // Open target
	        	if(data.targetType!="expander") {
	        		window.open(node.data.href, target='_self');
	        	}
	        }
	      },
	    lazyLoad: function(event, data) {
	          var nodeKey=data.node.key;
	          var keys = nodeKey.split("_");
	          // return children or any other node source
	          data.result = {url: jsRoutes.controllers.Application.fillJsonTree(keys[0],keys[1]).url};
	        }
	});     //End of tree
    
    //fail tree
    $("#failTree").fancytree({
    	autoCollapse: false,
    	debugLevel: 0,
    	source: {
			url: '/failures/tree?parentLevel='+failParentLevel+'&parentId='+failParentId,
			cache: false
			},
		postinit: function(isReloading, isError) {
			this.reactivate();
			},
		focus: function(event, data) {
	        // Auto-activate focused node after 2 seconds
	        data.node.scheduleAction("activate", 2000);
			},
		click: function(event, data) {
	        var node = data.node;
	        // Use <a> href and target attributes to load the content:
	        if( node.data.href ){
	        	// Open target
	        	if(data.targetType!="expander") {
	        		window.open(node.data.href, target='_self');
	        	}
	        }
	      },
	    lazyLoad: function(event, data) {
          // return children or any other node source
          data.result = {url: jsRoutes.controllers.FailureApplication.fillCausalityTree(data.node.key).url};
	        }
	}); //End of fail tree
    
    //Admin tree
    $("#adminTree").fancytree({
		click: function(event, data) {
	        var node = data.node;
	        // Use <a> href and target attributes to load the content:
	        if( node.data.href ){
	          // Open target
	        		window.open(node.data.href, target='_self');
	        }
	      }
	});
    
    //Flow diagram tree
    $("#flowTree").fancytree({
    	minExpandLevel: 1, // 1: root node is not collapsible
    	source: {
			url: jsRoutes.controllers.WorkflowApplication.flowTreeJson(getParameterByName("wfType")).url,
			cache: false
			},
		lazyLoad: function(event, data) {
			var nodeKey=data.node.key;
	        var keys = nodeKey.split("_");
	        data.result = {url: jsRoutes.controllers.WorkflowApplication.fillFlowTreeJson(keys[0],keys[1],getParameterByName("wfType")).url};
	        },
	    
	        loadChildren: function(event, data) {
	            data.node.visit(function(subNode){
	                if( subNode.isUndefined()) {
	                    subNode.load();
	                }
	            });
	        }
	        
	});
    if($('#flowTree').length>0)
	    setTimeout(function() {
	    	
	        	$("#flowTree").fancytree("getRootNode").visit(function(node){
	                node.setExpanded(true);
	              
	        });
	  }, 3000);
   
  });

//Parameter extraction from URL functions
function getParameterByName(name) {
    name = name.replace(/[\[]/, "\\[").replace(/[\]]/, "\\]");
    var regex = new RegExp("[\\?&]" + name + "=([^&#]*)"),
        results = regex.exec(location.search);
    return results == null ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
}