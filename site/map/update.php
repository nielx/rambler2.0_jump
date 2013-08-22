<?php

require_once('config.php');

function db_connect() {
	$db = mysql_connect('localhost', DB_USER, DB_PW);

	if (!$db) {
		die ('Could not connect to database: ' . mysql_error());
	}

	if (!mysql_select_db(DB_DBASE, $db)) {
		die ('Could not select database: ' . mysql_error());
	}

	mysql_set_charset('utf8', $db);

	return $db;
}

$lat = $_GET['lat'];
$lon = $_GET['lon'];
$msg = $_GET['msg'];

if (empty($lat) || empty($lon)) {
	die('Expecting location data');
}

$db = db_connect();

$query = 'SELECT id FROM `locations` WHERE ' .
	 'lat=' . $lat . ' AND ' .
	 'lon=' . $lon; 

$result = mysql_query($query, $db);
if (!$result) {
	die ('Error inserting item: ' . mysql_error());
}

if (mysql_num_rows($result) > 0) {
	echo "location already exists";
	return;
}

$query = 'INSERT INTO `locations` SET ' .
	 'lat=' . $lat . ', ' .
	 'lon=' . $lon . ', ' .
	 'message="' . mysql_real_escape_string($msg, $db) . '"' ;

$result = mysql_query($query, $db);
if (!$result) {
	die ('Error inserting item: ' . mysql_error());
}

echo "location added";

?>
