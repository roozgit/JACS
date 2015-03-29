@(username: String)

$(document).ready(
		function() {
			    var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket
			    		var isSecure = location.protocol === "https:";
			   
			 var chatSocket = new WS("@routes.WebSocketApplication.wsInterface(username).webSocketURL(request,false)");
			 //var chatSocket = new WS("wss:\/\/localhost:9443\/wsInterface?username="+"@username")
			 var odoc = document.title;
			    var sendMessage = function() {
			        chatSocket.send(JSON.stringify(
			            {text: $("#talk").val()}
			        ))
			        $("#talk").val('')
			    }

			    var receiveEvent = function(event) {
			        var data = JSON.parse(event.data)
			        // Handle errors
			        if(data.error) {
			            chatSocket.close()
			            $("#onError span").text(data.error)
			            $("#onError").show()
			            return
			        } else {
			            $("#onChat").show()
			        }

			        // Create the message element
			        if(data.kind!=="join" && data.kind!=="quit") {
			        $("li#wsChatter").addClass("active");
			        if(document.title.substr(-3)!="(1)") document.title= document.title + "(1)";
			        var el = $('<div class="message"><span style="font-weight:bold"></span>: <p></p></div>')
			        $("span", el).text(data.user)
			        $("p", el).text(data.message)
			        $(el).addClass(data.kind)
			        if(data.user == '@username') $("span",el).addClass('itsmetalking')
			        	$('#messages').append(el)
			        }
			        // Update the members list
			        $("#members").html('')
			        $(data.members).each(function() {
			            var li = document.createElement('li');
			            li.textContent = this;
			            $("#members").append(li);
			        })
			    }

			    var handleReturnKey = function(e) {
			        if(e.charCode == 13 || e.keyCode == 13) {
			            e.preventDefault()
			            sendMessage()
			        }
			    }

			    $("#talk").keypress(handleReturnKey)

			    chatSocket.onmessage = receiveEvent
			    
			    //Clicking handler
			    $("li#wsChatter").click(function() {
			    	$("#chatModal").modal();
			    	$("li#wsChatter").removeClass("active");
			    	document.title = odoc;
			    });
			    
			    //Clearning messages
			    $("button#clearMsgs").click(function() {
			    	$('#messages').html('');
			    });
			    
			    $(window).on('beforeunload', function(){
			    	chatSocket.close();
			    });
		});