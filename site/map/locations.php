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

$lastId = $_GET['last'];


if (empty($lastId) || ! is_numeric($lastId)) {
	$lastId = 0;
} else {
	$lastId = intval($lastId);
}

$db = db_connect();
$query = '';

$query = 'SELECT * FROM `locations` WHERE `id` > ' . $lastId;

$result = mysql_query($query, $db);
if (!$result) {
	die ('Error getting item: ' . mysql_error());
}

$entries = array();
while ($row = mysql_fetch_assoc($result)) {
	$entries[] = $row;
}

echo json_encode($entries);

?>
