function makePreview(data) {
    var previewDiv = document.createElement('div'); 
    
    previewDiv.style.width = "50%";
    var previewHref = document.createElement('a');
    previewHref.href = "#";
    previewDiv.appendChild(previewHref);
    previewImg = new Image();
    previewImg.src = data.fields.preview_url;
    previewImg.addEventListener('click', function(){
        alert(data.fields.parameter_string);
    });
    previewHref.appendChild(previewImg);
    
    previewDiv.appendChild(document.createElement('br'));
    date = new Date(data.fields.upload_date);
    previewDiv.appendChild(document.createTextNode(date.toLocaleString()));
    return previewDiv;
}

function callback(data) {
    for (var i = 0; i < data.length; i++) { 
        previewDiv = makePreview(data[i]);
        document.getElementById('shared').appendChild(previewDiv);
    }
}