function makePreview(data) {
    var previewDiv = document.createElement('div'); 
    
    previewDiv.style.width = "50%";
    previewDiv.style.float = "left";
    previewImg = new Image();
    previewImg.src = data.fields.preview_url;
    previewImg.addEventListener('click', function(){
        alert(data.fields.parameter_string);
    });
    previewDiv.appendChild(previewImg);
    
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