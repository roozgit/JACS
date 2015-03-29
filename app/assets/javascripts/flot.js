/**
 * 
 */
$(document).ready(function() {
	// Flot chart vars and options
    
	
	// A custom label formatter used by several of the plots

	function labelFormatter(label, series) {
		return "<div style='font-size:8pt; text-align:center; padding:2px; color:white;'>" + label + "<br/>" + Math.round(series.percent) + "%</div>";
	}
	var data = [];
	$(".diagram-panel").click(function(){
		/*$.plot("#placeholder", data, {
			series:{
				pie: {
					show: true
				}
			},
			grid: {
		        hoverable: true,
		        clickable: true
		    }
		});*/

		// Fetch one series, adding to what we already have

		var alreadyFetched = {};
		jQuery.ajax({
			url: jsRoutes.controllers.Application.flotChartJson().url,
			type: "GET",
			dataType: "json",
			success: function(series2) {
				$.plot("#placeholder", series2, {
					series:{
						pie: {
							show: true,
				            radius: 1,
				            label: {
				                show: true,
				                radius: 1,
				                formatter: labelFormatter,
				                background: {
				                    opacity: 0.5,
				                    color: '#000'
				                }
				            }
						}
					},
					grid: {
				        hoverable: true,
				        clickable: true
				    },
				    legend : {
				    	show : true
				    }
			        
				});
			}
		});
		
		//Second dashboard chart
		var data2 = [];
		var options = {
				lines: {
					show: true
				},
				points: {
					show: true
				},
				xaxis: {
					tickDecimals: 0,
					tickSize: 1
				},
				yaxis: {
					tickDecimals: 0,
					tickSize: 2
				},
				grid: {
			        clickable: true
			    }
			};
		$.plot("#placeholder-diagram", data2, options);

		$("#placeholder-diagram").bind("plotclick", function (event, pos, item) {
			if (item) {
				alert("Work requests issued " + (31-item.datapoint[0])+" days ago ="+ item.datapoint[1]);
				//plot.highlight(item.series, item.datapoint);
			}
		});
		//var alreadyFetched = {};
		jQuery.ajax({
			url: jsRoutes.controllers.Application.flotChartJson2().url,
			type: "GET",
			dataType: "json",
			success: function(series3) {
				data2.push(series3);
				$.plot("#placeholder-diagram", data2, options);
			}
		});
		
		//Chart-3 failure mode bar chart
		var data3 = [];
		jQuery.ajax({
			url: jsRoutes.controllers.FailureApplication.flotChartFailureJson().url,
			type: "GET",
			dataType: "json",
			success: function(series4) {
				data3.push(series4);
				$.plot("#placeholder-failChart", data3, {
					series: {
						bars: {
							show: true,
							barWidth: 0.6,
							align: "center"
						}
					},
					xaxis: {
						mode: "categories",
						tickLength: 0
					}
				}
				);
			}
		});
	});
		
});
