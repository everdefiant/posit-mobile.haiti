{include file="header.tpl" title=$find.name tab="projects"}
<h2>{$find.name}</h2>
<div class="find_details">
	<h3>Description:</h3>
	<div class="find_description">{$find.description}</div>
	
	<h3>Project:</h3>
	<div class="find_project">{$project.name}</div>
	
	<h3>Time Added:</h3>
	<div class="find_add_time">{$find.add_time}</div>
	
	<div class="associated_barcode">{$find.barcode_id}</div>
	<p style="text-align: center"><img src="qrcode?d={$find.barcode_id}"/></p>
	<h3>Location:</h3>
	<div class="find_location">
		Longitude: {$find.longitude}
		Latitude: {$find.latitude}
	</div>
	
	<div class="picture_loop">
	{foreach from=$images item=imageid}
	<img src="displayPicture?id={$imageid}&size=full"/>
	{/foreach}
	</div>
	
	<br />
	
	<div class="video_loop">
		{foreach from=$videos item=videoid}
			<a href="displayVideo?id={$videoid}">Click to download video</a>
		{/foreach}
	</div>
	
	<div class="audio_loop">
		{foreach from=$audios item=audioid}
			<embed type="application/x-shockwave-flash" 
				src="http://www.google.com/reader/ui/3247397568-audio-player.swf?audioUrl=displayAudio?id={$audioid}" 
				width="400" height="27" allowscriptaccess="never" quality="best" bgcolor="#ffffff" wmode="window" 
				flashvars="playerMode=embedded"
			/> <br />
		{/foreach}
	</div>
	
</div>
{include file="footer.tpl"}