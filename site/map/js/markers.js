$(document).ready(function() {
	var myLatLng = new google.maps.LatLng(17.74033553, 83.25067267);
	RamblerMap.init('#map', myLatLng, 11);
	RamblerMap.getMarkers();
});

var RamblerMap = {
	map: null,
	bounds: null
}

function getUrlVars() {
	var vars = {};
	var parts = window.location.href.replace(/[?&]+([^=&]+)=([^&]*)/gi, function(m,key,value) {
		vars[key] = value;
	});
	return vars;
}

RamblerMap.init = function(selector, latLng, zoom) {
	var myOptions = {
		zoom:zoom,
		center: latLng,
		mapTypeId: google.maps.MapTypeId.ROADMAP
	}
	this.map = new google.maps.Map($(selector)[0], myOptions);
	this.bounds = new google.maps.LatLngBounds();
	this.lastLocationId = 0;
	this.update = parseInt(getUrlVars()["update"]);
	if (isNaN(this.update)) {
		this.update = 0;
	}
	this.update *= 1000;
	console.log("update: " + this.update );
}

RamblerMap.getMarkers = function() {
	var theUrl = 'http://rambler.projects.simbits.nl/map/locations.php?lastId=' + RamblerMap.lastLocationId;
	console.log("request: " + theUrl);
	$.ajax({
		url: theUrl,
		dataType: 'json',
		success: function(data){
			$.each(data, function(i, item){
				// create a new LatLng point for the marker
				var lat = item.lat;
				var lng = item.lon;
				var id = item.id;
				var point = new google.maps.LatLng(parseFloat(lat),parseFloat(lng));
				
				// extend the bounds to include the new point
				RamblerMap.bounds.extend(point);
				
				var image = new google.maps.MarkerImage('images/rambler_marker.png',
					new google.maps.Size(40, 35),
					new google.maps.Point(0, 0),
					new google.maps.Point(12, 35));

				var shadow = new google.maps.MarkerImage('images/rambler_marker_shadow.png',
					new google.maps.Size(62, 35),
					new google.maps.Point(0, 0),
					new google.maps.Point(12, 35));
				
				var marker = new google.maps.Marker({
					position: point,
					map: RamblerMap.map,
					shadow: shadow,
					icon: image,
					title: 'Rambler',
				});
				
				if (item.message != '') {
					var infoWindow = new google.maps.InfoWindow();
					var html='<strong>' + item.message + '</strong>';
					google.maps.event.addListener(marker, 'click', function() {
						infoWindow.setContent(html);
						infoWindow.open(RamblerMap.map, marker);
					});
				}
				RamblerMap.map.fitBounds(RamblerMap.bounds);

				if (id > RamblerMap.lastLocationId) {
					RamblerMap.lastLocationId = id;
				}
			});
		},
		complete: function() {
			if (RamblerMap.update > 0) {
				setTimeout(RamblerMap.getMarkers, RamblerMap.update);
			}
		}
	});
}
